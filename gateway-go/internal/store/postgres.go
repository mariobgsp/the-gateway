package store

import (
	"context"

	"github.com/jackc/pgx/v5/pgxpool"
)

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

// Minimal ApiGateway row mirror for forward path
type ApiGateway struct {
	ID                  int64
	ApiName             string
	ApiIdentifier       string
	ApiHost             string
	ApiPath             string
	Method              string
	Header              string
	Param               string
	RequireRequestBody  bool
	RequireRequestParam bool
	Status              string
}
