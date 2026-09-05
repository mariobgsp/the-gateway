package store

import (
	"context"
	"errors"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

var ErrStoreNotFound = errors.New("store not found!")

type Postgres struct{ Pool *pgxpool.Pool }

func NewPostgres(dsn string) (*Postgres, error) {
	pool, err := pgxpool.New(context.Background(), dsn)
	if err != nil {
		return nil, err
	}
	return &Postgres{Pool: pool}, nil
}

func (p *Postgres) Close() {
	if p.Pool != nil {
		p.Pool.Close()
	}
}

// Minimal ApiGateway row mirror for forward path (JSON tags match Java Jackson field names for getDetailedApi).
type ApiGateway struct {
	ID                  int64  `json:"id"`
	ApiName             string `json:"apiName"`
	ApiIdentifier       string `json:"apiIdentifier"`
	ApiHost             string `json:"apiHost"`
	ApiPath             string `json:"apiPath"`
	Method              string `json:"method"`
	Header              string `json:"header"`
	Param               string `json:"param"`
	RequireRequestBody  bool   `json:"requireRequestBody"`
	RequireRequestParam bool   `json:"requireRequestParam"`
	Status              string `json:"status"`
}

type User struct {
	ID                int64
	Username          string
	Password          string
	Email             string
	UserSessionStatus string
	UserLastLogin     time.Time
}

type StoreAccount struct {
	ID        int64
	StoreName string
	ClientID  string
	SecretKey string
}

func (p *Postgres) Prop(ctx context.Context, key string) string {
	var v string
	if err := p.Pool.QueryRow(ctx, `SELECT value FROM system_properties WHERE "key"=$1`, key).Scan(&v); err != nil {
		return ""
	}
	return v
}

func (p *Postgres) FindApiByIdentifier(ctx context.Context, id string) (*ApiGateway, error) {
	a := &ApiGateway{}
	err := p.Pool.QueryRow(ctx, `SELECT id, api_name, api_identifier, api_host, api_path, method, COALESCE(header,''), COALESCE(param,''), COALESCE(require_request_body,false), COALESCE(require_request_param,false), COALESCE(status,'') FROM api_gateway WHERE api_identifier=$1`, id).
		Scan(&a.ID, &a.ApiName, &a.ApiIdentifier, &a.ApiHost, &a.ApiPath, &a.Method, &a.Header, &a.Param, &a.RequireRequestBody, &a.RequireRequestParam, &a.Status)
	if err != nil {
		return nil, err
	}
	return a, nil
}

func (p *Postgres) ListApis(ctx context.Context) ([]ApiGateway, error) {
	rows, err := p.Pool.Query(ctx, `SELECT id, api_name, api_identifier, api_path, method, COALESCE(status,'') FROM api_gateway ORDER BY id`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []ApiGateway
	for rows.Next() {
		var a ApiGateway
		if err := rows.Scan(&a.ID, &a.ApiName, &a.ApiIdentifier, &a.ApiPath, &a.Method, &a.Status); err != nil {
			return nil, err
		}
		out = append(out, a)
	}
	return out, rows.Err()
}

func (p *Postgres) UpsertApi(ctx context.Context, a *ApiGateway) error {
	var id int64
	err := p.Pool.QueryRow(ctx, `SELECT id FROM api_gateway WHERE api_identifier=$1`, a.ApiIdentifier).Scan(&id)
	now := time.Now()
	if err != nil {
		return p.Pool.QueryRow(ctx, `INSERT INTO api_gateway (api_name, api_identifier, api_host, api_path, method, header, require_request_body, require_request_param, param, status, created_at, updated_at) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12) RETURNING id`,
			a.ApiName, a.ApiIdentifier, a.ApiHost, a.ApiPath, a.Method, a.Header, a.RequireRequestBody, a.RequireRequestParam, nullIfEmpty(a.Param), nullIfEmpty(a.Status), now, now).Scan(&a.ID)
	}
	_, err = p.Pool.Exec(ctx, `UPDATE api_gateway SET api_name=$1, api_host=$2, api_path=$3, method=$4, status=$5, header=$6, require_request_body=$7, require_request_param=$8, param=$9, updated_at=$10 WHERE id=$11`,
		a.ApiName, a.ApiHost, a.ApiPath, a.Method, a.Status, a.Header, a.RequireRequestBody, a.RequireRequestParam, nullIfEmpty(a.Param), now, id)
	a.ID = id
	return err
}

func (p *Postgres) DeleteApi(ctx context.Context, identifier string) (bool, error) {
	ct, err := p.Pool.Exec(ctx, `DELETE FROM api_gateway WHERE api_identifier=$1`, identifier)
	return ct.RowsAffected() > 0, err
}

func (p *Postgres) FindUser(ctx context.Context, username string) (*User, error) {
	u := &User{}
	var lastLogin *time.Time
	err := p.Pool.QueryRow(ctx, `SELECT id, username, password, COALESCE(email,''), COALESCE(user_session_status,''), user_last_login FROM "user" WHERE username=$1`, username).
		Scan(&u.ID, &u.Username, &u.Password, &u.Email, &u.UserSessionStatus, &lastLogin)
	if err != nil {
		return nil, err
	}
	if lastLogin != nil {
		u.UserLastLogin = *lastLogin
	}
	return u, nil
}

func (p *Postgres) SetUserSession(ctx context.Context, username, status string, login bool) error {
	if login {
		_, err := p.Pool.Exec(ctx, `UPDATE "user" SET user_session_status=$1, user_last_login=$2, updated_at=NOW() WHERE username=$3`, status, time.Now(), username)
		return err
	}
	_, err := p.Pool.Exec(ctx, `UPDATE "user" SET user_session_status=$1, updated_at=NOW() WHERE username=$2`, status, username)
	return err
}

func (p *Postgres) SaveActiveToken(ctx context.Context, token string) error {
	now := time.Now()
	_, err := p.Pool.Exec(ctx, `INSERT INTO token_log (token, status, created_at, updated_at) VALUES ($1,'ENABLED',$2,$3)`, token, now, now)
	return err
}

func (p *Postgres) BlacklistToken(ctx context.Context, token string) error {
	_, err := p.Pool.Exec(ctx, `UPDATE token_log SET status='DISABLED', updated_at=$1 WHERE id=(SELECT id FROM token_log WHERE token=$2 ORDER BY id DESC LIMIT 1)`, time.Now(), token)
	return err
}

// IsBlacklisted is fail-closed: missing row or DISABLED => true.
func (p *Postgres) IsBlacklisted(ctx context.Context, token string) bool {
	var status string
	err := p.Pool.QueryRow(ctx, `SELECT status FROM token_log WHERE token=$1 ORDER BY id DESC LIMIT 1`, token).Scan(&status)
	if err != nil {
		return true
	}
	return status == "DISABLED"
}

func (p *Postgres) UserStores(ctx context.Context, userID int64) ([]StoreAccount, error) {
	rows, err := p.Pool.Query(ctx, `SELECT s.id, s.store_name, s.client_id, s.secret_key FROM store_account s JOIN user_store_r r ON r.store_id=s.id WHERE r.user_id=$1 ORDER BY s.id`, userID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var out []StoreAccount
	for rows.Next() {
		var s StoreAccount
		if err := rows.Scan(&s.ID, &s.StoreName, &s.ClientID, &s.SecretKey); err != nil {
			return nil, err
		}
		out = append(out, s)
	}
	return out, rows.Err()
}

func (p *Postgres) OwnedStore(ctx context.Context, userID, storeID int64) (*StoreAccount, error) {
	var n int
	if err := p.Pool.QueryRow(ctx, `SELECT COUNT(*) FROM user_store_r WHERE user_id=$1 AND store_id=$2`, userID, storeID).Scan(&n); err != nil {
		return nil, err
	}
	if n == 0 {
		return nil, ErrStoreNotFound
	}
	s := &StoreAccount{}
	if err := p.Pool.QueryRow(ctx, `SELECT id, store_name, client_id, secret_key FROM store_account WHERE id=$1`, storeID).Scan(&s.ID, &s.StoreName, &s.ClientID, &s.SecretKey); err != nil {
		return nil, ErrStoreNotFound
	}
	return s, nil
}

func (p *Postgres) StoreNameExists(ctx context.Context, name string) bool {
	var n int
	_ = p.Pool.QueryRow(ctx, `SELECT COUNT(*) FROM store_account WHERE store_name=$1`, name).Scan(&n)
	return n > 0
}

func (p *Postgres) CreateStore(ctx context.Context, userID int64, name, clientID, secret string) (*StoreAccount, error) {
	var id int64
	if err := p.Pool.QueryRow(ctx, `INSERT INTO store_account (store_name, client_id, secret_key) VALUES ($1,$2,$3) RETURNING id`, name, clientID, secret).Scan(&id); err != nil {
		return nil, err
	}
	if _, err := p.Pool.Exec(ctx, `INSERT INTO user_store_r (user_id, store_id) VALUES ($1,$2)`, userID, id); err != nil {
		return nil, err
	}
	return &StoreAccount{ID: id, StoreName: name, ClientID: clientID, SecretKey: secret}, nil
}

func (p *Postgres) UpdateStore(ctx context.Context, s *StoreAccount) error {
	_, err := p.Pool.Exec(ctx, `UPDATE store_account SET store_name=$1, client_id=$2, secret_key=$3 WHERE id=$4`, s.StoreName, s.ClientID, s.SecretKey, s.ID)
	return err
}

func (p *Postgres) DeleteStore(ctx context.Context, storeID int64) error {
	if _, err := p.Pool.Exec(ctx, `DELETE FROM user_store_r WHERE store_id=$1`, storeID); err != nil {
		return err
	}
	_, err := p.Pool.Exec(ctx, `DELETE FROM store_account WHERE id=$1`, storeID)
	return err
}

func nullIfEmpty(s string) any {
	if s == "" {
		return nil
	}
	return s
}
