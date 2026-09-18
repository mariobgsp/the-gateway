package store

import "context"

// StoreAccount is a store identity row.
type StoreAccount struct {
	ID        int64  `json:"id"`
	StoreName string `json:"storeName"`
	ClientID  string `json:"clientId"`
	SecretKey string `json:"secretKey"`
}

func (p *Postgres) UserStores(ctx context.Context, userID int64) ([]StoreAccount, error) {
	return many[StoreAccount](ctx, p, `SELECT s.id, s.store_name, s.client_id, s.secret_key
		FROM store_account s JOIN user_store_r r ON r.store_id=s.id
		WHERE r.user_id=$1 ORDER BY s.id`, userID)
}

// OwnedStore resolves a store the user owns in one round trip. A store that
// does not exist and a store owned by someone else are indistinguishable to
// the caller, which is the point.
func (p *Postgres) OwnedStore(ctx context.Context, userID, storeID int64) (*StoreAccount, error) {
	return one[StoreAccount](ctx, p, `SELECT s.id, s.store_name, s.client_id, s.secret_key
		FROM store_account s JOIN user_store_r r ON r.store_id=s.id AND r.user_id=$1
		WHERE s.id=$2`, userID, storeID)
}

func (p *Postgres) StoreNameExists(ctx context.Context, name string) bool {
	var n int
	_ = p.Pool.QueryRow(ctx, `SELECT COUNT(*) FROM store_account WHERE store_name=$1`, name).Scan(&n)
	return n > 0
}

// CreateStore inserts the account and its ownership link together.
func (p *Postgres) CreateStore(ctx context.Context, userID int64, name, clientID, secret string) (*StoreAccount, error) {
	s := &StoreAccount{StoreName: name, ClientID: clientID, SecretKey: secret}
	err := p.Pool.QueryRow(ctx, `INSERT INTO store_account (store_name, client_id, secret_key)
		VALUES ($1,$2,$3) RETURNING id`, name, clientID, secret).Scan(&s.ID)
	if err != nil {
		return nil, err
	}
	if _, err := p.Pool.Exec(ctx, `INSERT INTO user_store_r (user_id, store_id) VALUES ($1,$2)`, userID, s.ID); err != nil {
		return nil, err
	}
	return s, nil
}

func (p *Postgres) UpdateStore(ctx context.Context, s *StoreAccount) error {
	_, err := p.Pool.Exec(ctx, `UPDATE store_account SET store_name=$1, client_id=$2, secret_key=$3
		WHERE id=$4`, s.StoreName, s.ClientID, s.SecretKey, s.ID)
	return err
}

func (p *Postgres) DeleteStore(ctx context.Context, storeID int64) error {
	if _, err := p.Pool.Exec(ctx, `DELETE FROM user_store_r WHERE store_id=$1`, storeID); err != nil {
		return err
	}
	_, err := p.Pool.Exec(ctx, `DELETE FROM store_account WHERE id=$1`, storeID)
	return err
}
