package handler

import (
	"encoding/json"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/service"
)

type Handler struct{ fwd *service.Forward }

func New(fwd *service.Forward) *Handler { return &Handler{fwd: fwd} }

func (h *Handler) GetApiList(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]any{"code": "00", "data": []any{}})
}
func (h *Handler) GetDetailedApi(w http.ResponseWriter, r *http.Request) {
	path := chi.URLParam(r, "path")
	json.NewEncoder(w).Encode(map[string]any{"code": "00", "data": map[string]string{"path": path}})
}
func (h *Handler) SaveApi(w http.ResponseWriter, r *http.Request)   { w.Write([]byte(`{"code":"00"}`)) }
func (h *Handler) DeleteApi(w http.ResponseWriter, r *http.Request) { w.Write([]byte(`{"code":"00"}`)) }
func (h *Handler) Forward(w http.ResponseWriter, r *http.Request) {
	path := chi.URLParam(r, "path")
	// ponytail: spike — delegate to service with path (real impl will parse query/headers)
	if _, _, err := h.fwd.Do(service.ForwardRequest{PathName: path, Headers: r.Header}); err != nil {
		http.Error(w, err.Error(), http.StatusNotFound)
		return
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]any{"code": "00", "data": map[string]string{"ok": "forward spike"}})
}
