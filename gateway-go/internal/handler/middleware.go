package handler

import (
	"context"
	"encoding/json"
	"net/http"
	"strings"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/auth"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/envelope"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/service"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

// write mirrors Spring's ResponseEntity<Response>: JSON body + mapped HTTP status.
func write(w http.ResponseWriter, resp envelope.Response) {
	w.Header().Set("Content-Type", "application/json")
	status := resp.HTTPStatus
	if status == 0 {
		status = 200
	}
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(resp)
}

func fromSvcErr(err error) envelope.Response {
	if se, ok := err.(*service.SvcError); ok {
		st := "unknownError"
		switch {
		case se.HTTPStatus == 400:
			st = "BAD_REQUEST"
		case se.HTTPStatus == 401:
			st = "UNAUTHORIZED"
		case se.HTTPStatus == 403:
			st = "FORBIDDEN"
		case se.HTTPStatus == 404:
			st = "NOT_FOUND"
		case se.HTTPStatus == 429:
			st = "TOO_MANY_REQUESTS"
		case se.HTTPStatus >= 500:
			st = "INTERNAL_SERVER_ERROR"
		}
		return envelope.Err(se.HTTPStatus, st, se.Code, se.Message)
	}
	msg := err.Error()
	if msg == "" {
		msg = "UnknownError"
	}
	return envelope.Err(500, "INTERNAL_SERVER_ERROR", "99", msg)
}

// Middleware mirrors JwtRequestFilter + SecurityConfig: login is public, everything else needs a valid non-blacklisted Bearer token.
func Middleware(db *store.Postgres, secretOf func(ctx context.Context) string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if r.URL.Path == "/gateway/user/login" || r.URL.Path == "/health" {
				next.ServeHTTP(w, r)
				return
			}
			h := r.Header.Get("Authorization")
			if h == "" || !strings.HasPrefix(h, "Bearer ") {
				write(w, envelope.Err(401, "UNAUTHORIZED", "98", "98:Unauthorized:Authorization header is missing or invalid"))
				return
			}
			token := strings.TrimPrefix(h, "Bearer ")
			secret := secretOf(r.Context())
			username, err := auth.UsernameFromToken(secret, token)
			if err != nil {
				write(w, envelope.Err(401, "UNAUTHORIZED", "98", "98:Unauthorized:Invalid JWT Token"))
				return
			}
			if db.IsBlacklisted(r.Context(), token) {
				write(w, envelope.Err(401, "UNAUTHORIZED", "98", "98:Unauthorized:Unauthorized or blacklisted JWT Token"))
				return
			}
			u, err := db.FindUser(r.Context(), username)
			if err != nil || !auth.ValidateToken(secret, token, u.Username) {
				write(w, envelope.Err(401, "UNAUTHORIZED", "98", "98:Unauthorized:Unauthorized or blacklisted JWT Token"))
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
	if u == "" {
		u = auth.UserFrom(r.Context())
	}
	return u
}
