package service

import (
	"context"
	"net/http"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/api"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

// ForwardRequest is the typed seam between the HTTP layer and the forwarder.
type ForwardRequest struct {
	PathName    string
	QueryParams map[string]string
	Headers     http.Header
	Body        any
}

type ForwardResult struct {
	Body       any
	StatusCode int
}

// Forward resolves an ApiGateway configuration and dispatches to its upstream.
type Forward struct {
	Lookup func(ctx context.Context, identifier string) (*store.ApiGateway, error)
	Client *http.Client
}

const configNotFound = "API Gateway Configuration Not Found"

func NewForward(db *store.Postgres, client *http.Client) *Forward {
	f := &Forward{Client: client}
	if f.Client == nil {
		f.Client = defaultClient()
	}
	if db != nil {
		f.Lookup = db.FindApiByIdentifier
	}
	return f
}

// Do loads the configuration, builds the URL, filters headers, enforces the
// required-body gate, then calls upstream.
func (f *Forward) Do(ctx context.Context, req ForwardRequest) (*ForwardResult, error) {
	cfg, err := f.config(ctx, req.PathName)
	if err != nil {
		return nil, err
	}
	url, err := BuildForwardURL(cfg, req.QueryParams)
	if err != nil {
		return nil, err
	}
	if cfg.RequireRequestBody && req.Body == nil {
		return nil, api.BadRequest("Bad request: emptyRequestBody")
	}
	body, status, err := invokeUpstream(ctx, f.Client, url, cfg.Method, FilterHeaders(cfg.Header, req.Headers), req.Body)
	if err != nil {
		return nil, api.Internal(err)
	}
	return &ForwardResult{Body: body, StatusCode: status}, nil
}

// config resolves the gateway configuration, reporting a missing one and an
// unusable lookup the same way.
func (f *Forward) config(ctx context.Context, identifier string) (*store.ApiGateway, error) {
	if f.Lookup == nil {
		return nil, api.NotFound(configNotFound)
	}
	cfg, err := f.Lookup(ctx, identifier)
	if err != nil {
		return nil, api.NotFound(configNotFound)
	}
	return cfg, nil
}
