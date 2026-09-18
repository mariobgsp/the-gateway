package service

import (
	"context"
	"errors"
	"strings"

	"golang.org/x/crypto/bcrypt"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/api"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/auth"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

type LoginResult struct {
	LoginMessage  string `json:"loginMessage"`
	AccessToken   string `json:"accessToken"`
	TokenLifetime string `json:"tokenLifetime"`
}

const tokenLifetimeDefault = "3600"

// Login verifies credentials, throttles repeat failures, and issues a token.
func Login(ctx context.Context, db *store.Postgres, limiter *auth.RateLimiter, username, password string) (*LoginResult, error) {
	username = strings.TrimSpace(username)
	if username == "" || strings.TrimSpace(password) == "" {
		return nil, api.BadRequest("username and password are required")
	}
	if limiter.IsBlocked(username) {
		return nil, api.TooManyRequests("too many login attempts, please try again later")
	}
	u, err := db.FindUser(ctx, username)
	if err != nil {
		limiter.Failure(username)
		return nil, api.BadCredentials()
	}
	if bcrypt.CompareHashAndPassword([]byte(u.Password), []byte(password)) != nil {
		limiter.Failure(username)
		return nil, api.BadCredentials()
	}

	secret := db.Prop(ctx, "TOKEN_SECRET_KEY")
	lifetime := db.Prop(ctx, "TOKEN_EXPIRATION")
	token, err := auth.GenerateToken(secret, u.Username, lifetime)
	if err != nil {
		return nil, api.Internal(err)
	}
	if err := db.SetUserSession(ctx, u.Username, "ACTIVE", true); err != nil {
		return nil, err
	}
	limiter.Success(username)
	if err := db.SaveActiveToken(ctx, token); err != nil {
		return nil, err
	}
	if lifetime == "" {
		lifetime = tokenLifetimeDefault
	}
	return &LoginResult{LoginMessage: "success login!", AccessToken: token, TokenLifetime: lifetime}, nil
}

func Logout(ctx context.Context, db *store.Postgres, secretB64, authHeader string) (map[string]string, error) {
	token, ok := auth.BearerToken(authHeader)
	if !ok {
		return nil, api.BadRequest("Authorization header should not be empty")
	}
	username, err := auth.UsernameFromToken(secretB64, token)
	if err != nil {
		return nil, api.Internal(errors.New("Invalid JWT Token"))
	}
	if err := db.BlacklistToken(ctx, token); err != nil {
		return nil, err
	}
	if _, err := db.FindUser(ctx, username); err != nil {
		return nil, api.UserNotFound()
	}
	if err := db.SetUserSession(ctx, username, "INACTIVE", false); err != nil {
		return nil, err
	}
	return map[string]string{"logoutMessage": "success logout!"}, nil
}
