package main

import (
	"log"
	"net/http"
	"os"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
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

	fwd := service.NewForward(pg, &http.Client{})
	h := handler.New(fwd)

	r := chi.NewRouter()
	r.Use(middleware.RequestID, middleware.RealIP, middleware.Recoverer, middleware.Logger)
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) { w.Write([]byte(`{"status":"ok"}`)) })
	r.Route("/api/gateway", func(r chi.Router) {
		r.Get("/getApiList", h.GetApiList)
		r.Post("/getDetailedApi", h.GetDetailedApi)
		r.Post("/saveApi", h.SaveApi)
		r.Post("/deleteApi", h.DeleteApi)
		r.Get("/{path}", h.Forward)
		r.Post("/{path}", h.Forward)
		r.Put("/{path}", h.Forward)
		r.Delete("/{path}", h.Forward)
	})

	log.Printf("gateway-go listening :%s", port)
	log.Fatal(http.ListenAndServe(":"+port, r))
}
