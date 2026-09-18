package handler

import (
	"encoding/json"
	"io"
	"net/http"
	"net/url"

	"github.com/go-chi/chi/v5"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/api"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/service"
)

type saveApiRequest struct {
	ApiIdentifier       string `json:"apiIdentifier"`
	Name                string `json:"name"`
	Host                string `json:"host"`
	Path                string `json:"path"`
	Method              string `json:"method"`
	Status              string `json:"status"`
	Header              string `json:"header"`
	RequireRequestBody  *bool  `json:"requireRequestBody"`
	RequireRequestParam *bool  `json:"requireRequestParam"`
	Param               string `json:"param"`
}

// Forward mirrors the Java controller: the path variable holds the identifier
// and may arrive with a %3F-encoded query string.
func (h *Handler) Forward(w http.ResponseWriter, r *http.Request) {
	raw := chi.URLParam(r, "path")
	if raw == "" {
		raw = chi.URLParam(r, "*")
	}
	// chi can hand back the raw segment; unescape once so a %3F-encoded query
	// matches the decoded path variable the Java controller saw.
	if unescaped, err := url.PathUnescape(raw); err == nil {
		raw = unescaped
	}
	name, params := service.ParseForwardPath(raw)
	// The real query string wins, last value per key.
	for k, values := range r.URL.Query() {
		params[k] = values[len(values)-1]
	}

	res, err := h.Fwd.Do(r.Context(), service.ForwardRequest{
		PathName:    name,
		QueryParams: params,
		Headers:     r.Header,
		Body:        readBody(r),
	})
	if err != nil {
		write(w, errorResponse(err))
		return
	}
	write(w, api.OKStatus(res.StatusCode, res.Body))
}

// readBody prefers JSON and falls back to the raw string, so a non-JSON
// payload still reaches upstream intact.
func readBody(r *http.Request) any {
	raw, _ := io.ReadAll(r.Body)
	if len(raw) == 0 {
		return nil
	}
	var v any
	if err := json.Unmarshal(raw, &v); err != nil {
		return string(raw)
	}
	return v
}

func (h *Handler) GetApiList(w http.ResponseWriter, r *http.Request) {
	items, err := service.ListGateways(r.Context(), h.DB)
	if err != nil {
		write(w, errorResponse(err))
		return
	}
	write(w, api.OKList(items))
}

func (h *Handler) GetDetailedApi(w http.ResponseWriter, r *http.Request) {
	detail, err := service.GetApiDetail(r.Context(), h.DB, queryParam(r, "api_identifier"))
	if err != nil {
		write(w, errorResponse(err))
		return
	}
	write(w, api.OK(detail))
}

func (h *Handler) SaveApi(w http.ResponseWriter, r *http.Request) {
	in, ok := readJSON[saveApiRequest](w, r, "request body is required")
	if !ok {
		return
	}
	err := service.SaveApi(r.Context(), h.DB, service.SaveApiInput{
		ApiIdentifier:       in.ApiIdentifier,
		Name:                in.Name,
		Host:                in.Host,
		Path:                in.Path,
		Method:              in.Method,
		Status:              in.Status,
		Header:              in.Header,
		RequireRequestBody:  in.RequireRequestBody,
		RequireRequestParam: in.RequireRequestParam,
		Param:               in.Param,
	})
	if err != nil {
		write(w, errorResponse(err))
		return
	}
	write(w, api.OKMsg("api saved successfully!"))
}

func (h *Handler) DeleteApi(w http.ResponseWriter, r *http.Request) {
	if err := service.DeleteApi(r.Context(), h.DB, queryParam(r, "api_identifier")); err != nil {
		write(w, errorResponse(err))
		return
	}
	write(w, api.OKMsg("api deleted successfully!"))
}
