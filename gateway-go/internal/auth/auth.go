package auth

import (
	"context"
	"encoding/base64"
	"errors"
	"sync"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/google/uuid"
)

// Claims mirrors JJWT HS256: sub, jti, iat, exp.
type Claims struct {
	jwt.RegisteredClaims
}

var errInvalidToken = errors.New("Invalid JWT Token")

func GenerateToken(secretB64, username, expSec string) (string, error) {
	secret, err := base64.StdEncoding.DecodeString(secretB64)
	if err != nil {
		return "", err
	}
	exp := 3600
	if d, err := time.ParseDuration(expSec + "s"); err == nil {
		exp = int(d.Seconds())
	}
	now := time.Now()
	c := Claims{RegisteredClaims: jwt.RegisteredClaims{
		Subject:   username,
		ID:        uuid.NewString(),
		IssuedAt:  jwt.NewNumericDate(now),
		ExpiresAt: jwt.NewNumericDate(now.Add(time.Duration(exp) * time.Second)),
	}}
	t := jwt.NewWithClaims(jwt.SigningMethodHS256, c)
	return t.SignedString(secret)
}

func UsernameFromToken(secretB64, token string) (string, error) {
	secret, err := base64.StdEncoding.DecodeString(secretB64)
	if err != nil {
		return "", err
	}
	c := &Claims{}
	t, err := jwt.ParseWithClaims(token, c, func(t *jwt.Token) (any, error) { return secret, nil })
	if err != nil || !t.Valid {
		return "", errInvalidToken
	}
	return c.Subject, nil
}

func ValidateToken(secretB64, token, username string) bool {
	u, err := UsernameFromToken(secretB64, token)
	if err != nil {
		return false
	}
	return u == username
}

// RateLimiter mirrors LoginAttemptService: 5 fails / 600s window per username, in-memory.
type RateLimiter struct {
	mu       sync.Mutex
	attempts map[string][]int64
}

func NewRateLimiter() *RateLimiter { return &RateLimiter{attempts: map[string][]int64{}} }

const maxAttempts = 5
const windowSec = 600

func (l *RateLimiter) IsBlocked(username string) bool {
	l.mu.Lock()
	defer l.mu.Unlock()
	l.prune(username)
	return len(l.attempts[username]) >= maxAttempts
}

func (l *RateLimiter) Failure(username string) {
	l.mu.Lock()
	defer l.mu.Unlock()
	l.attempts[username] = append(l.attempts[username], time.Now().Unix())
}

func (l *RateLimiter) Success(username string) {
	l.mu.Lock()
	defer l.mu.Unlock()
	delete(l.attempts, username)
}

func (l *RateLimiter) prune(username string) {
	cutoff := time.Now().Unix() - windowSec
	kept := l.attempts[username][:0]
	for _, ts := range l.attempts[username] {
		if ts >= cutoff {
			kept = append(kept, ts)
		}
	}
	if len(kept) == 0 {
		delete(l.attempts, username)
		return
	}
	l.attempts[username] = kept
}

type ctxKey string

const userKey ctxKey = "username"

func WithUser(ctx context.Context, username string) context.Context {
	return context.WithValue(ctx, userKey, username)
}

func UserFrom(ctx context.Context) string {
	u, _ := ctx.Value(userKey).(string)
	return u
}
