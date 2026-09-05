package service

import (
	"context"
	"strings"

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

type GatewayListItem struct {
	ID            int64  `json:"id"`
	ApiName       string `json:"apiName"`
	ApiIdentifier string `json:"apiIdentifier"`
	ApiPath       string `json:"apiPath"`
	Method        string `json:"method"`
	Status        string `json:"status"`
}

func ListGateways(ctx context.Context, db *store.Postgres) ([]GatewayListItem, error) {
	rows, err := db.ListApis(ctx)
	if err != nil {
		return nil, err
	}
	out := make([]GatewayListItem, 0, len(rows))
	for _, a := range rows {
		out = append(out, GatewayListItem{ID: a.ID, ApiName: a.ApiName, ApiIdentifier: a.ApiIdentifier, ApiPath: a.ApiPath, Method: a.Method, Status: a.Status})
	}
	return out, nil
}

func GetApiDetail(ctx context.Context, db *store.Postgres, identifier string) (*store.ApiGateway, error) {
	if strings.TrimSpace(identifier) == "" {
		return nil, &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:api_identifier is required"}
	}
	api, err := db.FindApiByIdentifier(ctx, identifier)
	if err != nil {
		return nil, &SvcError{HTTPStatus: 404, Code: "02", Message: "02:NotFound:api not found!"}
	}
	return api, nil
}

func SaveApi(ctx context.Context, db *store.Postgres, in SaveApiInput) error {
	if err := ValidateSaveApi(in); err != nil {
		return err
	}
	status := in.Status
	if status == "" {
		// keep existing default for new rows; updates preserve input status as-is (may be "")
		status = "created"
	}
	a := &store.ApiGateway{
		ApiIdentifier: in.ApiIdentifier, ApiName: in.Name, ApiHost: in.Host, ApiPath: in.Path,
		Method: in.Method, Status: in.Status, Header: in.Header, Param: in.Param,
	}
	if a.Status == "" && in.Status == "" {
		a.Status = status
	}
	if in.RequireRequestBody != nil {
		a.RequireRequestBody = *in.RequireRequestBody
	}
	if in.RequireRequestParam != nil {
		a.RequireRequestParam = *in.RequireRequestParam
	}
	return db.UpsertApi(ctx, a)
}

func DeleteApi(ctx context.Context, db *store.Postgres, identifier string) error {
	if strings.TrimSpace(identifier) == "" {
		return &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:api_identifier is required"}
	}
	ok, err := db.DeleteApi(ctx, identifier)
	if err != nil {
		return err
	}
	if !ok {
		return &SvcError{HTTPStatus: 404, Code: "02", Message: "02:NotFound:api not found!"}
	}
	return nil
}

// ValidateSaveApi mirrors ApiGatewayServices.validateSaveApi messages exactly.
func ValidateSaveApi(in SaveApiInput) error {
	bad := func(msg string) error {
		return &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:" + msg}
	}
	if strings.TrimSpace(in.ApiIdentifier) == "" {
		return bad("api_identifier is required")
	}
	if strings.TrimSpace(in.Name) == "" {
		return bad("api name is required")
	}
	if strings.TrimSpace(in.Host) == "" {
		return bad("api host is required")
	}
	if strings.TrimSpace(in.Path) == "" {
		return bad("api path is required")
	}
	if strings.TrimSpace(in.Method) == "" {
		return bad("api method is required")
	}
	if len(in.ApiIdentifier) > 255 || len(in.Name) > 255 || len(in.Host) > 255 || len(in.Path) > 255 {
		return bad("field length exceeds maximum allowed")
	}
	if !allowedMethods[strings.ToUpper(in.Method)] {
		return bad("unsupported http method")
	}
	if !strings.HasPrefix(in.Path, "/") {
		return bad("api path must start with /")
	}
	h := strings.ToLower(in.Host)
	if !(strings.HasPrefix(h, "https://") || strings.HasPrefix(h, "http://")) || strings.Contains(h, "@") {
		return bad("api host must be a valid http(s) url without embedded credentials")
	}
	return nil
}
