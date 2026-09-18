package service

import (
	"context"
	"strings"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/api"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

var allowedMethods = map[string]bool{"GET": true, "POST": true, "PUT": true, "DELETE": true, "PATCH": true}

type SaveApiInput struct {
	ApiIdentifier       string
	Name                string
	Host                string
	Path                string
	Method              string
	Status              string
	Header              string
	RequireRequestBody  *bool
	RequireRequestParam *bool
	Param               string
}

func ListGateways(ctx context.Context, db *store.Postgres) ([]store.ApiListItem, error) {
	return db.ListApis(ctx)
}

func GetApiDetail(ctx context.Context, db *store.Postgres, identifier string) (*store.ApiGateway, error) {
	if strings.TrimSpace(identifier) == "" {
		return nil, api.BadRequest("api_identifier is required")
	}
	row, err := db.FindApiByIdentifier(ctx, identifier)
	if err != nil {
		return nil, api.NotFound("api not found!")
	}
	return row, nil
}

func SaveApi(ctx context.Context, db *store.Postgres, in SaveApiInput) error {
	if err := ValidateSaveApi(in); err != nil {
		return err
	}
	status := in.Status
	if status == "" {
		status = "created"
	}
	return db.UpsertApi(ctx, &store.ApiGateway{
		ApiIdentifier:       in.ApiIdentifier,
		ApiName:             in.Name,
		ApiHost:             in.Host,
		ApiPath:             in.Path,
		Method:              in.Method,
		Status:              status,
		Header:              in.Header,
		Param:               in.Param,
		RequireRequestBody:  in.RequireRequestBody != nil && *in.RequireRequestBody,
		RequireRequestParam: in.RequireRequestParam != nil && *in.RequireRequestParam,
	})
}

func DeleteApi(ctx context.Context, db *store.Postgres, identifier string) error {
	if strings.TrimSpace(identifier) == "" {
		return api.BadRequest("api_identifier is required")
	}
	removed, err := db.DeleteApi(ctx, identifier)
	if err != nil {
		return err
	}
	if !removed {
		return api.NotFound("api not found!")
	}
	return nil
}

// ValidateSaveApi rejects a configuration the forwarder could not use. The
// host must be a credential-free http(s) URL; the path must be rooted.
func ValidateSaveApi(in SaveApiInput) error {
	if strings.TrimSpace(in.ApiIdentifier) == "" {
		return api.BadRequest("api_identifier is required")
	}
	if strings.TrimSpace(in.Name) == "" {
		return api.BadRequest("api name is required")
	}
	if strings.TrimSpace(in.Host) == "" {
		return api.BadRequest("api host is required")
	}
	if strings.TrimSpace(in.Path) == "" {
		return api.BadRequest("api path is required")
	}
	if strings.TrimSpace(in.Method) == "" {
		return api.BadRequest("api method is required")
	}
	if len(in.ApiIdentifier) > 255 || len(in.Name) > 255 || len(in.Host) > 255 || len(in.Path) > 255 {
		return api.BadRequest("field length exceeds maximum allowed")
	}
	if !allowedMethods[strings.ToUpper(in.Method)] {
		return api.BadRequest("unsupported http method")
	}
	if !strings.HasPrefix(in.Path, "/") {
		return api.BadRequest("api path must start with /")
	}
	host := strings.ToLower(in.Host)
	if !(strings.HasPrefix(host, "https://") || strings.HasPrefix(host, "http://")) || strings.Contains(host, "@") {
		return api.BadRequest("api host must be a valid http(s) url without embedded credentials")
	}
	return nil
}
