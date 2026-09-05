package main

import (
	"context"
	"log"
	"net/http"
	"os"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/auth"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/handler"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/service"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}
	dsn := os.Getenv("DATABASE_URL")
	if dsn == "" {
		dsn = "postgres://microservices:password@postgres:5432/gateway?sslmode=disable"
	}
	pg, err := store.NewPostgres(dsn)
	if err != nil {
		log.Fatalf("db: %v", err)
	}
	defer pg.Close()

	limiter := auth.NewRateLimiter()
	fwd := service.NewForward(pg, nil)
	h := handler.New(pg, fwd, limiter)
	secretOf := func(ctx context.Context) string { return secretCache(ctx, pg) }

	r := chi.NewRouter()
	r.Use(middleware.RequestID, middleware.RealIP, middleware.Recoverer, middleware.Logger)
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Write([]byte(`{"status":"ok"}`))
	})
	// public login (mirrors permitAll) + authenticated routes
	r.Post("/gateway/user/login", h.Login)
	r.With(handler.Middleware(pg, secretOf)).Route("/gateway/user", func(r chi.Router) {
		r.Post("/logout", h.Logout)
		r.Get("/testAuth", h.TestAuth)
	})
	r.With(handler.Middleware(pg, secretOf)).Route("/api/gateway", func(r chi.Router) {
		r.Get("/getApiList", h.GetApiList)
		r.Post("/getDetailedApi", h.GetDetailedApi)
		r.Post("/saveApi", h.SaveApi)
		r.Post("/deleteApi", h.DeleteApi)
		// forwarders last (avoid shadowing); PATCH included per ALLOWED_METHODS
		r.Get("/{path}", h.Forward)
		r.Post("/{path}", h.Forward)
		r.Put("/{path}", h.Forward)
		r.Patch("/{path}", h.Forward)
		r.Delete("/{path}", h.Forward)
	})
	r.With(handler.Middleware(pg, secretOf)).Route("/api/store", func(r chi.Router) {
		r.Get("/getList", h.ListStores)
		r.Post("/getDetail", h.GetStoreDetail)
		r.Post("/save", h.SaveStore)
		r.Post("/delete", h.DeleteStore)
		r.Post("/regenerateSecret", h.RegenerateSecret)
	})

	log.Printf("gateway-go listening :%s", port)
	log.Fatal(http.ListenAndServe(":"+port, r))
}

// secretCache reads per-request in middleware via secretOf; kept as func for test stubbing.
func secretCache(ctx context.Context, pg *store.Postgres) string {
	// ponytail: no in-process cache — SystemPropertiesServices.getProps hits DB each call; parity over perf
	return pg.Prop(ctx, "TOKEN_SECRET_KEY")
}
