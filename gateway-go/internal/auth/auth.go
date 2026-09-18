package auth

import (
	"encoding/base64"
	"errors"
	"slices"
	"strings"
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

// BearerToken extracts the token from an "Authorization: Bearer <token>"
// header. It reports false when the scheme is absent.
func BearerToken(header string) (string, bool) {
	return strings.CutPrefix(header, "Bearer ")
}

// GenerateToken issues an HS256 token with the configured lifetime in seconds.
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
	return jwt.NewWithClaims(jwt.SigningMethodHS256, c).SignedString(secret)
}

func UsernameFromToken(secretB64, token string) (string, error) {
	secret, err := base64.StdEncoding.DecodeString(secretB64)
	if err != nil {
		return "", err
	}
	c := &Claims{}
	t, err := jwt.ParseWithClaims(token, c, func(*jwt.Token) (any, error) { return secret, nil })
	if err != nil || !t.Valid {
		return "", errInvalidToken
	}
	return c.Subject, nil
}

func ValidateToken(secretB64, token, username string) bool {
	u, err := UsernameFromToken(secretB64, token)
	return err == nil && u == username
}

const (
	maxAttempts = 5
	windowSec   = 600
)

// RateLimiter throttles login failures per username, in memory.
type RateLimiter struct {
	mu       sync.Mutex
	attempts map[string][]int64
}

func NewRateLimiter() *RateLimiter { return &RateLimiter{attempts: map[string][]int64{}} }

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

// prune drops attempts that have aged out of the window.
func (l *RateLimiter) prune(username string) {
	cutoff := time.Now().Unix() - windowSec
	kept := slices.DeleteFunc(l.attempts[username], func(ts int64) bool { return ts < cutoff })
	if len(kept) == 0 {
		delete(l.attempts, username)
		return
	}
	l.attempts[username] = kept
}
