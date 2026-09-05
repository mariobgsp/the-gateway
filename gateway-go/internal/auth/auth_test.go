package auth

import (
	"testing"
	"time"
)

const testSecret = "3VZ6lDdJZ/4k8dQ5HgO7w4XwZ8tTrHAlf4A5KJb/VZ8=" // same shape as data.sql seed

func TestTokenRoundtrip(t *testing.T) {
	tok, err := GenerateToken(testSecret, "ario_test", "3600")
	if err != nil {
		t.Fatalf("generate: %v", err)
	}
	u, err := UsernameFromToken(testSecret, tok)
	if err != nil {
		t.Fatalf("parse: %v", err)
	}
	if u != "ario_test" {
		t.Fatalf("sub %q", u)
	}
	if !ValidateToken(testSecret, tok, "ario_test") || ValidateToken(testSecret, tok, "other") {
		t.Fatal("validate mismatch")
	}
	// tampered token must fail (parity: JwtRequestFilter 401/98)
	bad := tok[:len(tok)-2] + "xx"
	if _, err := UsernameFromToken(testSecret, bad); err == nil {
		t.Fatal("tampered token accepted")
	}
}

func TestTokenExpiry(t *testing.T) {
	tok, err := GenerateToken(testSecret, "u", "1")
	if err != nil {
		t.Fatalf("generate: %v", err)
	}
	time.Sleep(1100 * time.Millisecond)
	if _, err := UsernameFromToken(testSecret, tok); err == nil {
		t.Fatal("expired token accepted")
	}
}

func TestRateLimiter(t *testing.T) {
	l := NewRateLimiter()
	if l.IsBlocked("u") {
		t.Fatal("fresh user blocked")
	}
	for i := 0; i < 5; i++ {
		l.Failure("u")
	}
	if !l.IsBlocked("u") {
		t.Fatal("expected block after 5 fails (parity: 429/05)")
	}
	l.Success("u")
	if l.IsBlocked("u") {
		t.Fatal("success should clear (parity: registerSuccess)")
	}
}
