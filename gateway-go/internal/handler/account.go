package handler

import (
	"net/http"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/api"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/service"
)

type saveStoreRequest struct {
	StoreID   *int64 `json:"storeId"`
	StoreName string `json:"storeName"`
	ClientID  string `json:"clientId"`
}

func (h *Handler) ListStores(w http.ResponseWriter, r *http.Request) {
	items, err := service.ListStores(r.Context(), h.DB, currentUser(r))
	if err != nil {
		write(w, errorResponse(err))
		return
	}
	write(w, api.OKList(items))
}

func (h *Handler) GetStoreDetail(w http.ResponseWriter, r *http.Request) {
	item, err := service.GetStoreDetail(r.Context(), h.DB, currentUser(r), parseID(r, "store_id"))
	if err != nil {
		write(w, errorResponse(err))
		return
	}
	write(w, api.OK(item))
}

func (h *Handler) SaveStore(w http.ResponseWriter, r *http.Request) {
	in, ok := readJSON[saveStoreRequest](w, r, "request body is required")
	if !ok {
		return
	}
	item, err := service.SaveStore(r.Context(), h.DB, currentUser(r), in.StoreID, in.StoreName, in.ClientID)
	if err != nil {
		write(w, errorResponse(err))
		return
	}
	write(w, api.OK(item))
}

func (h *Handler) DeleteStore(w http.ResponseWriter, r *http.Request) {
	if err := service.DeleteStore(r.Context(), h.DB, currentUser(r), parseID(r, "store_id")); err != nil {
		write(w, errorResponse(err))
		return
	}
	write(w, api.OKMsg("store deleted successfully!"))
}

func (h *Handler) RegenerateSecret(w http.ResponseWriter, r *http.Request) {
	item, err := service.RegenerateSecret(r.Context(), h.DB, currentUser(r), parseID(r, "store_id"))
	if err != nil {
		write(w, errorResponse(err))
		return
	}
	write(w, api.OK(item))
}
