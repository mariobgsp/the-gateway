package store

import (
	"context"
	"time"
)

// ApiGateway is a full gateway configuration row. Only the fields the forwarder
// needs are carried; JSON tags match the getDetailedApi response shape.
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

// ApiListItem is the projection the list endpoint returns.
type ApiListItem struct {
	ID            int64  `json:"id"`
	ApiName       string `json:"apiName"`
	ApiIdentifier string `json:"apiIdentifier"`
	ApiPath       string `json:"apiPath"`
	Method        string `json:"method"`
	Status        string `json:"status"`
}

func (p *Postgres) FindApiByIdentifier(ctx context.Context, identifier string) (*ApiGateway, error) {
	return one[ApiGateway](ctx, p, `SELECT id, api_name, api_identifier, api_host, api_path, method,
		COALESCE(header,''), COALESCE(param,''),
		COALESCE(require_request_body,false), COALESCE(require_request_param,false), COALESCE(status,'')
		FROM api_gateway WHERE api_identifier=$1`, identifier)
}

func (p *Postgres) ListApis(ctx context.Context) ([]ApiListItem, error) {
	return many[ApiListItem](ctx, p, `SELECT id, api_name, api_identifier, api_path, method, COALESCE(status,'')
		FROM api_gateway ORDER BY id`)
}

// UpsertApi inserts or updates by api_identifier in a single statement, so a
// concurrent creator cannot slip between a probe and a write.
func (p *Postgres) UpsertApi(ctx context.Context, a *ApiGateway) error {
	now := time.Now()
	return p.Pool.QueryRow(ctx, `INSERT INTO api_gateway
		(api_name, api_identifier, api_host, api_path, method, header,
		 require_request_body, require_request_param, param, status, created_at, updated_at)
		VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)
		ON CONFLICT (api_identifier) DO UPDATE SET
			api_name=EXCLUDED.api_name, api_host=EXCLUDED.api_host, api_path=EXCLUDED.api_path,
			method=EXCLUDED.method, header=EXCLUDED.header, status=EXCLUDED.status,
			require_request_body=EXCLUDED.require_request_body,
			require_request_param=EXCLUDED.require_request_param, param=EXCLUDED.param,
			updated_at=EXCLUDED.updated_at
		RETURNING id`,
		a.ApiName, a.ApiIdentifier, a.ApiHost, a.ApiPath, a.Method, a.Header,
		a.RequireRequestBody, a.RequireRequestParam, nullIfEmpty(a.Param), nullIfEmpty(a.Status),
		now, now).Scan(&a.ID)
}

// DeleteApi reports whether a row was removed.
func (p *Postgres) DeleteApi(ctx context.Context, identifier string) (bool, error) {
	ct, err := p.Pool.Exec(ctx, `DELETE FROM api_gateway WHERE api_identifier=$1`, identifier)
	return ct.RowsAffected() > 0, err
}

func nullIfEmpty(s string) any {
	if s == "" {
		return nil
	}
	return s
}
