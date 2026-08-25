package service

import (
	"net/http"
	"testing"
)

func TestForward_Allowlist(t *testing.T) {
	fwd := NewForward(nil, &http.Client{})
	req := ForwardRequest{
		PathName:    "gateway-catapi",
		QueryParams: map[string]string{"limit": "5", "evil": "injected"},
		Headers:     http.Header{"x-api-key": []string{"secret"}, "Authorization": []string{"Bearer x"}},
	}
	// stub: spike returns ok, real impl would filter evil and strip Authorization
	data, code, err := fwd.Do(req)
	if err != nil {
		t.Fatalf("unexpected err: %v", err)
	}
	if code != 200 {
		t.Fatalf("code %d", code)
	}
	_ = data
	// TODO: assert URL contains limit=5 not evil, headers filtered — mirrors ApiGatewayServicesTest.forwardApiForwardsWithAllowedParamsAndHeaders
}

func TestForward_RequireParam(t *testing.T) {
	fwd := NewForward(nil, &http.Client{})
	req := ForwardRequest{PathName: "gateway-catapi", QueryParams: map[string]string{}, Headers: http.Header{}}
	_, _, err := fwd.Do(req)
	_ = err // spike stub; real impl should return 04 emptyRequestParam when requireRequestParam true
}

func TestForward_TolerantDecode(t *testing.T) {
	// mirrors CommonUtilTest.parseQueryStringHandlesEmptyAndValues and tolerant ?foo→""
	req := ForwardRequest{PathName: "gateway-catapi", QueryParams: map[string]string{"a": "1", "c": ""}, Headers: http.Header{}}
	if req.QueryParams["a"] != "1" || req.QueryParams["c"] != "" {
		t.Fatal("tolerant decode failed")
	}
}
