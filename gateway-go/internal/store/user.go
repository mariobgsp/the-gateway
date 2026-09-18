package store

import "context"

// User carries only the credential columns the auth path reads. The remaining
// user columns are never referenced anywhere.
type User struct {
	ID       int64
	Username string
	Password string
}

func (p *Postgres) FindUser(ctx context.Context, username string) (*User, error) {
	return one[User](ctx, p, `SELECT id, username, password FROM "user" WHERE username=$1`, username)
}

// SetUserSession flips the session status. touchLogin also stamps
// user_last_login, which only a successful login should do.
func (p *Postgres) SetUserSession(ctx context.Context, username, status string, touchLogin bool) error {
	_, err := p.Pool.Exec(ctx, `UPDATE "user" SET user_session_status=$1,
		user_last_login=CASE WHEN $3 THEN NOW() ELSE user_last_login END,
		updated_at=NOW() WHERE username=$2`, status, username, touchLogin)
	return err
}

func (p *Postgres) SaveActiveToken(ctx context.Context, token string) error {
	_, err := p.Pool.Exec(ctx, `INSERT INTO token_log (token, status, created_at, updated_at)
		VALUES ($1,'ENABLED',NOW(),NOW())`, token)
	return err
}

func (p *Postgres) BlacklistToken(ctx context.Context, token string) error {
	_, err := p.Pool.Exec(ctx, `UPDATE token_log SET status='DISABLED', updated_at=NOW()
		WHERE id=(SELECT id FROM token_log WHERE token=$1 ORDER BY id DESC LIMIT 1)`, token)
	return err
}

// IsBlacklisted fails closed: an absent row and a DISABLED row both read as
// blacklisted, so a token_log gap can never let a revoked token through.
func (p *Postgres) IsBlacklisted(ctx context.Context, token string) bool {
	var status string
	if err := p.Pool.QueryRow(ctx, `SELECT status FROM token_log
		WHERE token=$1 ORDER BY id DESC LIMIT 1`, token).Scan(&status); err != nil {
		return true
	}
	return status == "DISABLED"
}
