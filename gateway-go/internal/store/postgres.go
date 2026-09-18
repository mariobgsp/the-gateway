package store

import (
	"context"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Postgres owns the connection pool the rest of this package queries through.
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

// one runs a single-row query and maps the row onto T by column position, so
// T's exported fields must match the SELECT list in order and count.
func one[T any](ctx context.Context, p *Postgres, sql string, args ...any) (*T, error) {
	rows, err := p.Pool.Query(ctx, sql, args...)
	if err != nil {
		return nil, err
	}
	return pgx.CollectExactlyOneRow(rows, pgx.RowToAddrOfStructByPos[T])
}

// many runs a multi-row query and maps each row onto T by column position.
func many[T any](ctx context.Context, p *Postgres, sql string, args ...any) ([]T, error) {
	rows, err := p.Pool.Query(ctx, sql, args...)
	if err != nil {
		return nil, err
	}
	return pgx.CollectRows(rows, pgx.RowToStructByPos[T])
}

// Prop reads a system_properties value. An absent key reads as "".
func (p *Postgres) Prop(ctx context.Context, key string) string {
	var v string
	if err := p.Pool.QueryRow(ctx, `SELECT value FROM system_properties WHERE "key"=$1`, key).Scan(&v); err != nil {
		return ""
	}
	return v
}
