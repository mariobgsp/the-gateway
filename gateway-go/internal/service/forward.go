package service

import (
	"fmt"
	"net/http"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

// ForwardRequest mirrors Java ForwardRequest
type ForwardRequest struct {
	PathName    string
	QueryParams map[string]string
	Headers     http.Header
	Body        any
}

// Forward mirrors ApiGatewayServices.processForwardApi typed seam
type Forward struct {
	db     *store.Postgres
	client *http.Client
}

func NewForward(db *store.Postgres, client *http.Client) *Forward {
	return &Forward{db: db, client: client}
}

func (f *Forward) Do(req ForwardRequest) (any, int, error) {
	// stub: lookup api_gateway by identifier (pg query omitted for spike)
	// SELECT * FROM api_gateway WHERE api_identifier=$1
	// For spike, return not found if empty
	if req.PathName == "" {
		return nil, 404, fmt.Errorf("api not found")
	}
	// ponytail: spike stub — real impl will load ApiGateway row, filter headers/params, build URL, call upstream
	return map[string]string{"ok": "spike"}, 200, nil
}
