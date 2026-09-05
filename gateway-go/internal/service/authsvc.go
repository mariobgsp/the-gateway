package service

import (
	"context"
	"strings"
	"time"

	"golang.org/x/crypto/bcrypt"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/auth"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

type LoginResult struct {
	LoginMessage  string `json:"loginMessage"`
	AccessToken   string `json:"accessToken"`
	TokenLifetime string `json:"tokenLifetime"`
}

func Login(ctx context.Context, db *store.Postgres, limiter *auth.RateLimiter, username, password string) (*LoginResult, error) {
	username = strings.TrimSpace(username)
	if username == "" || strings.TrimSpace(password) == "" {
		return nil, &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:username and password are required"}
	}
	if limiter.IsBlocked(username) {
		return nil, &SvcError{HTTPStatus: 429, Code: "05", Message: "05:TooManyRequests:too many login attempts, please try again later"}
	}
	u, err := db.FindUser(ctx, username)
	if err != nil {
		limiter.Failure(username)
		return nil, &SvcError{HTTPStatus: 401, Code: "06", Message: "06:Unauthorized:invalid username or password"}
	}
	if err := bcrypt.CompareHashAndPassword([]byte(u.Password), []byte(password)); err != nil {
		limiter.Failure(username)
		return nil, &SvcError{HTTPStatus: 401, Code: "06", Message: "06:Unauthorized:invalid username or password"}
	}
	secret := db.Prop(ctx, "TOKEN_SECRET_KEY")
	exp := db.Prop(ctx, "TOKEN_EXPIRATION")
	token, err := auth.GenerateToken(secret, u.Username, exp)
	if err != nil {
		return nil, &SvcError{HTTPStatus: 500, Code: "99", Message: "99:internalServerError:" + err.Error()}
	}
	_ = time.Now()
	if err := db.SetUserSession(ctx, u.Username, "ACTIVE", true); err != nil {
		return nil, err
	}
	limiter.Success(username)
	if err := db.SaveActiveToken(ctx, token); err != nil {
		return nil, err
	}
	if exp == "" {
		exp = "3600"
	}
	return &LoginResult{LoginMessage: "success login!", AccessToken: token, TokenLifetime: exp}, nil
}

func Logout(ctx context.Context, db *store.Postgres, secretB64, authHeader string) (map[string]string, error) {
	if authHeader == "" || !strings.HasPrefix(authHeader, "Bearer ") {
		return nil, &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:Authorization header should not be empty"}
	}
	token := strings.TrimPrefix(authHeader, "Bearer ")
	username, err := auth.UsernameFromToken(secretB64, token)
	if err != nil {
		return nil, &SvcError{HTTPStatus: 500, Code: "99", Message: "99:internalServerError:Invalid JWT Token"}
	}
	if err := db.BlacklistToken(ctx, token); err != nil {
		return nil, err
	}
	if _, err := db.FindUser(ctx, username); err != nil {
		return nil, &SvcError{HTTPStatus: 404, Code: "01", Message: "user not found!"}
	}
	if err := db.SetUserSession(ctx, username, "INACTIVE", false); err != nil {
		return nil, err
	}
	return map[string]string{"logoutMessage": "success logout!"}, nil
}
