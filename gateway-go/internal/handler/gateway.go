package handler

import (
	"encoding/json"
	"io"
	"net/http"
	"net/url"
	"strconv"

	"github.com/go-chi/chi/v5"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/auth"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/envelope"
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

// Forward mirrors GatewayController.forwardApi: path holds the identifier; query may arrive as %3F-encoded "?".
func (h *Handler) Forward(w http.ResponseWriter, r *http.Request) {
	raw := chi.URLParam(r, "path")
	if raw == "" {
		raw = chi.URLParam(r, "*")
	}
	// ponytail: chi may return the raw segment; unescape once so %3F-query matches Java's decoded @PathVariable (no-op if already decoded)
	if un, err := url.PathUnescape(raw); err == nil {
		raw = un
	}
	name, params := service.ParseForwardPath(raw)
	// merge real query string (tolerant, last-wins) — mirrors container-decoded @PathVariable + query
	for k, vs := range r.URL.Query() {
		if len(vs) > 0 {
			params[k] = vs[len(vs)-1]
		} else {
			params[k] = ""
		}
	}
	var body any
	if r.Body != nil {
		rawBody, _ := io.ReadAll(r.Body)
		if len(rawBody) > 0 {
			var v any
			if err := json.Unmarshal(rawBody, &v); err != nil {
				body = string(rawBody)
			} else {
				body = v
			}
		}
	}
	res, err := h.Fwd.Do(r.Context(), service.ForwardRequest{PathName: name, QueryParams: params, Headers: r.Header, Body: body})
	if err != nil {
		write(w, fromSvcErr(err))
		return
	}
	write(w, envelope.Response{Status: "ok", Code: "00", Message: "success", Data: res.Body, HTTPStatus: res.StatusCode})
}

func (h *Handler) GetApiList(w http.ResponseWriter, r *http.Request) {
	items, err := service.ListGateways(r.Context(), h.DB)
	if err != nil {
		write(w, fromSvcErr(err))
		return
	}
	if items == nil {
		items = []service.GatewayListItem{}
	}
	write(w, envelope.Ok(items))
}

func (h *Handler) GetDetailedApi(w http.ResponseWriter, r *http.Request) {
	id := r.URL.Query().Get("api_identifier")
	if id == "" {
		_ = r.ParseForm()
		id = r.Form.Get("api_identifier")
	}
	api, err := service.GetApiDetail(r.Context(), h.DB, id)
	if err != nil {
		write(w, fromSvcErr(err))
		return
	}
	write(w, envelope.Ok(api))
}

func (h *Handler) SaveApi(w http.ResponseWriter, r *http.Request) {
	var in struct {
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
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		write(w, envelope.Err(400, "BAD_REQUEST", "04", "04:BadRequest:request body is required"))
		return
	}
	err := service.SaveApi(r.Context(), h.DB, service.SaveApiInput{
		ApiIdentifier: in.ApiIdentifier, Name: in.Name, Host: in.Host, Path: in.Path,
		Method: in.Method, Status: in.Status, Header: in.Header,
		RequireRequestBody: in.RequireRequestBody, RequireRequestParam: in.RequireRequestParam, Param: in.Param,
	})
	if err != nil {
		write(w, fromSvcErr(err))
		return
	}
	write(w, envelope.OkMsg("api saved successfully!"))
}

func (h *Handler) DeleteApi(w http.ResponseWriter, r *http.Request) {
	id := r.URL.Query().Get("api_identifier")
	if id == "" {
		_ = r.ParseForm()
		id = r.Form.Get("api_identifier")
	}
	if err := service.DeleteApi(r.Context(), h.DB, id); err != nil {
		write(w, fromSvcErr(err))
		return
	}
	write(w, envelope.OkMsg("api deleted successfully!"))
}

// Login is public (mirrors permitAll /gateway/user/login).
func (h *Handler) Login(w http.ResponseWriter, r *http.Request) {
	var in struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		write(w, envelope.Err(400, "BAD_REQUEST", "04", "04:BadRequest:username and password are required"))
		return
	}
	res, err := service.Login(r.Context(), h.DB, h.Limiter, in.Username, in.Password)
	if err != nil {
		write(w, fromSvcErr(err))
		return
	}
	write(w, envelope.Ok(res))
}

func (h *Handler) Logout(w http.ResponseWriter, r *http.Request) {
	secret := h.DB.Prop(r.Context(), "TOKEN_SECRET_KEY")
	res, err := service.Logout(r.Context(), h.DB, secret, r.Header.Get("Authorization"))
	if err != nil {
		write(w, fromSvcErr(err))
		return
	}
	write(w, envelope.Ok(res))
}

func (h *Handler) TestAuth(w http.ResponseWriter, _ *http.Request) {
	write(w, envelope.OkMsg("success authentication!"))
}

func (h *Handler) ListStores(w http.ResponseWriter, r *http.Request) {
	items, err := service.ListStores(r.Context(), h.DB, currentUser(r))
	if err != nil {
		write(w, fromSvcErr(err))
		return
	}
	if items == nil {
		items = []service.StoreItem{}
	}
	write(w, envelope.Ok(items))
}

func (h *Handler) GetStoreDetail(w http.ResponseWriter, r *http.Request) {
	id := parseID(r, "store_id")
	item, err := service.GetStoreDetail(r.Context(), h.DB, currentUser(r), id)
	if err != nil {
		write(w, fromSvcErr(err))
		return
	}
	write(w, envelope.Ok(item))
}

func (h *Handler) SaveStore(w http.ResponseWriter, r *http.Request) {
	var in struct {
		StoreID   *int64 `json:"storeId"`
		StoreName string `json:"storeName"`
		ClientID  string `json:"clientId"`
	}
	if err := json.NewDecoder(r.Body).Decode(&in); err != nil {
		write(w, envelope.Err(400, "BAD_REQUEST", "04", "04:BadRequest:request body is required"))
		return
	}
	item, err := service.SaveStore(r.Context(), h.DB, currentUser(r), in.StoreID, in.StoreName, in.ClientID)
	if err != nil {
		write(w, fromSvcErr(err))
		return
	}
	write(w, envelope.Ok(item))
}

func (h *Handler) DeleteStore(w http.ResponseWriter, r *http.Request) {
	if err := service.DeleteStore(r.Context(), h.DB, currentUser(r), parseID(r, "store_id")); err != nil {
		write(w, fromSvcErr(err))
		return
	}
	write(w, envelope.OkMsg("store deleted successfully!"))
}

func (h *Handler) RegenerateSecret(w http.ResponseWriter, r *http.Request) {
	item, err := service.RegenerateSecret(r.Context(), h.DB, currentUser(r), parseID(r, "store_id"))
	if err != nil {
		write(w, fromSvcErr(err))
		return
	}
	write(w, envelope.Ok(item))
}

func parseID(r *http.Request, key string) int64 {
	s := r.URL.Query().Get(key)
	if s == "" {
		_ = r.ParseForm()
		s = r.Form.Get(key)
	}
	if s == "" {
		return 0
	}
	n, _ := strconv.ParseInt(s, 10, 64)
	return n
}
