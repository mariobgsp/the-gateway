package handler

import (
	"net/http"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/api"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/service"
)

type loginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

// Login is public: it is registered outside the guarded router group.
func (h *Handler) Login(w http.ResponseWriter, r *http.Request) {
	in, ok := readJSON[loginRequest](w, r, "username and password are required")
	if !ok {
		return
	}
	res, err := service.Login(r.Context(), h.DB, h.Limiter, in.Username, in.Password)
	if err != nil {
		write(w, errorResponse(err))
		return
	}
	write(w, api.OK(res))
}

func (h *Handler) Logout(w http.ResponseWriter, r *http.Request) {
	secret := h.DB.Prop(r.Context(), "TOKEN_SECRET_KEY")
	res, err := service.Logout(r.Context(), h.DB, secret, r.Header.Get("Authorization"))
	if err != nil {
		write(w, errorResponse(err))
		return
	}
	write(w, api.OK(res))
}

func (h *Handler) TestAuth(w http.ResponseWriter, _ *http.Request) {
	write(w, api.OKMsg("success authentication!"))
}
