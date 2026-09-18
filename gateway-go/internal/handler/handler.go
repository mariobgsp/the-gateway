package handler

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"strconv"

	"github.com/go-chi/chi/v5"
	chimw "github.com/go-chi/chi/v5/middleware"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/api"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/auth"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/service"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

type Handler struct {
	DB      *store.Postgres
	Fwd     *service.Forward
	Limiter *auth.RateLimiter
}

func New(db *store.Postgres, fwd *service.Forward, limiter *auth.RateLimiter) *Handler {
	return &Handler{DB: db, Fwd: fwd, Limiter: limiter}
}

var forwardMethods = []string{"GET", "POST", "PUT", "PATCH", "DELETE"}

// Router wires the gateway's surface. Health and login are the only routes
// reachable without a token; they are registered outside the guarded group so
// the middleware never has to know which paths are public.
func Router(h *Handler, secretOf func(ctx context.Context) string) http.Handler {
	r := chi.NewRouter()
	r.Use(chimw.RequestID, chimw.RealIP, chimw.Recoverer, chimw.Logger)

	r.Get("/health", h.Health)
	r.Post("/gateway/user/login", h.Login)

	r.Group(func(r chi.Router) {
		r.Use(Middleware(h.DB, secretOf))

		r.Post("/gateway/user/logout", h.Logout)
		r.Get("/gateway/user/testAuth", h.TestAuth)

		r.Route("/api/gateway", func(r chi.Router) {
			r.Get("/getApiList", h.GetApiList)
			r.Post("/getDetailedApi", h.GetDetailedApi)
			r.Post("/saveApi", h.SaveApi)
			r.Post("/deleteApi", h.DeleteApi)
			// Forwarders registered last so the literal routes above win.
			for _, method := range forwardMethods {
				r.Method(method, "/{path}", http.HandlerFunc(h.Forward))
			}
		})

		r.Route("/api/store", func(r chi.Router) {
			r.Get("/getList", h.ListStores)
			r.Post("/getDetail", h.GetStoreDetail)
			r.Post("/save", h.SaveStore)
			r.Post("/delete", h.DeleteStore)
			r.Post("/regenerateSecret", h.RegenerateSecret)
		})
	})
	return r
}

// Health answers the liveness and readiness probes. The body is a fixed
// contract; leave it alone.
func (h *Handler) Health(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	_, _ = w.Write([]byte(`{"status":"ok"}`))
}

// write renders an envelope with its mapped HTTP status.
func write(w http.ResponseWriter, resp api.Response) {
	w.Header().Set("Content-Type", "application/json")
	status := resp.HTTPStatus
	if status == 0 {
		status = 200
	}
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(resp)
}

// readJSON decodes the request body, answering 400 with msg and returning
// false when the body is missing or malformed.
func readJSON[T any](w http.ResponseWriter, r *http.Request, msg string) (*T, bool) {
	var in T
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		write(w, api.BadRequest(msg).Response())
		return nil, false
	}
	return &in, true
}

// errorResponse renders any error the service layer produced.
func errorResponse(err error) api.Response {
	var fault *api.Error
	if errors.As(err, &fault) {
		return fault.Response()
	}
	return api.Internal(err).Response()
}

// queryParam reads key from the query string, falling back to the parsed form
// so both query and form-encoded callers work.
func queryParam(r *http.Request, key string) string {
	if v := r.URL.Query().Get(key); v != "" {
		return v
	}
	_ = r.ParseForm()
	return r.Form.Get(key)
}

func parseID(r *http.Request, key string) int64 {
	n, _ := strconv.ParseInt(queryParam(r, key), 10, 64)
	return n
}
