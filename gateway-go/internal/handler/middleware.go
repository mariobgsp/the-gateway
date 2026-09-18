package handler

import (
	"context"
	"net/http"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/api"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/auth"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

const (
	unauthorizedHeader = "Authorization header is missing or invalid"
	unauthorizedToken  = "Unauthorized or blacklisted JWT Token"
)

// Middleware requires a valid, non-blacklisted Bearer token. It mirrors the
// Java filter, minus the public-path list: Router already registers health and
// login outside this middleware, so the check would be duplicated knowledge.
func Middleware(db *store.Postgres, secretOf func(ctx context.Context) string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			token, ok := auth.BearerToken(r.Header.Get("Authorization"))
			if !ok {
				write(w, api.Unauthorized(unauthorizedHeader).Response())
				return
			}
			secret := secretOf(r.Context())
			username, err := auth.UsernameFromToken(secret, token)
			if err != nil {
				write(w, api.Unauthorized("Invalid JWT Token").Response())
				return
			}
			if db.IsBlacklisted(r.Context(), token) {
				write(w, api.Unauthorized(unauthorizedToken).Response())
				return
			}
			u, err := db.FindUser(r.Context(), username)
			if err != nil || !auth.ValidateToken(secret, token, u.Username) {
				write(w, api.Unauthorized(unauthorizedToken).Response())
				return
			}
			next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), userCtxKey, u.Username)))
		})
	}
}

type ctxKey string

const userCtxKey ctxKey = "username"

func currentUser(r *http.Request) string {
	u, _ := r.Context().Value(userCtxKey).(string)
	return u
}
