package main

import (
	"context"
	"log"
	"net/http"
	"os"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/auth"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/handler"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/service"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

const (
	defaultPort = "8080"
	defaultDSN  = "postgres://microservices:password@postgres:5432/gateway?sslmode=disable"
)

func main() {
	port := envOr("PORT", defaultPort)
	pg, err := store.NewPostgres(envOr("DATABASE_URL", defaultDSN))
	if err != nil {
		log.Fatalf("db: %v", err)
	}
	defer pg.Close()

	h := handler.New(pg, service.NewForward(pg, nil), auth.NewRateLimiter())
	// Read per request rather than caching: parity with the Java lookup, which
	// hit the database on every call.
	secretOf := func(ctx context.Context) string { return pg.Prop(ctx, "TOKEN_SECRET_KEY") }

	log.Printf("gateway-go listening :%s", port)
	log.Fatal(http.ListenAndServe(":"+port, handler.Router(h, secretOf)))
}

func envOr(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
