package service

import (
	"testing"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/api"
)

func TestValidateSaveApi(t *testing.T) {
	good := SaveApiInput{ApiIdentifier: "id1", Name: "n", Host: "https://example.com", Path: "/p", Method: "GET"}
	if err := ValidateSaveApi(good); err != nil {
		t.Fatalf("good input rejected: %v", err)
	}
	cases := []struct {
		name   string
		mutate func(*SaveApiInput)
		msg    string
	}{
		{"identifier", func(i *SaveApiInput) { i.ApiIdentifier = "" }, "api_identifier is required"},
		{"name", func(i *SaveApiInput) { i.Name = "" }, "api name is required"},
		{"host", func(i *SaveApiInput) { i.Host = "" }, "api host is required"},
		{"path", func(i *SaveApiInput) { i.Path = "" }, "api path is required"},
		{"method", func(i *SaveApiInput) { i.Method = "" }, "api method is required"},
		{"method-unsupported", func(i *SaveApiInput) { i.Method = "OPTIONS" }, "unsupported http method"},
		{"path-slash", func(i *SaveApiInput) { i.Path = "p" }, "api path must start with /"},
		{"host-scheme", func(i *SaveApiInput) { i.Host = "ftp://example.com" }, "api host must be a valid http(s) url without embedded credentials"},
		{"host-creds", func(i *SaveApiInput) { i.Host = "https://user@example.com" }, "api host must be a valid http(s) url without embedded credentials"},
	}
	for _, c := range cases {
		in := good
		c.mutate(&in)
		fault, ok := ValidateSaveApi(in).(*api.Error)
		if !ok {
			t.Fatalf("%s: expected *api.Error, got %v", c.name, fault)
		}
		if fault.HTTPStatus != 400 || fault.Code != api.CodeBadRequest {
			t.Fatalf("%s: wrong status/code: %+v", c.name, fault)
		}
		if fault.Message != c.msg {
			t.Fatalf("%s: message %q != %q", c.name, fault.Message, c.msg)
		}
	}
	// PATCH allowed per allowedMethods (BFF + service agree)
	patched := good
	patched.Method = "patch"
	if err := ValidateSaveApi(patched); err != nil {
		t.Fatalf("PATCH should be allowed: %v", err)
	}
}
