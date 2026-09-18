package service

import (
	"io"
	"net/http"
	"strings"
	"testing"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/api"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

func TestForward_Allowlist(t *testing.T) {
	// URL must contain allowed limit, drop evil
	url, err := BuildForwardURL(&store.ApiGateway{ApiHost: "https://h", ApiPath: "/p", RequireRequestParam: true, Param: "limit;size"}, map[string]string{"limit": "5", "evil": "injected"})
	if err != nil {
		t.Fatalf("unexpected err: %v", err)
	}
	if !strings.Contains(url, "limit=5") || strings.Contains(url, "evil") {
		t.Fatalf("bad url %q", url)
	}
	// headers: allowlist keeps x-api-key, drops Authorization
	h := FilterHeaders("x-api-key;Content-Type", http.Header{"x-api-key": []string{"secret"}, "Authorization": []string{"Bearer x"}})
	if h.Get("x-api-key") != "secret" || h.Get("Authorization") != "" {
		t.Fatalf("bad headers %v", h)
	}
}

func TestForward_RequireParam(t *testing.T) {
	cfg := &store.ApiGateway{ApiHost: "https://h", ApiPath: "/p", RequireRequestParam: true, Param: "limit"}
	if _, err := BuildForwardURL(cfg, map[string]string{}); err == nil {
		t.Fatal("expected emptyRequestParam error")
	} else if fault, ok := err.(*api.Error); !ok || fault.Code != api.CodeBadRequest {
		t.Fatalf("wrong err %v", err)
	}
}

func TestForward_TolerantDecode(t *testing.T) {
	name, q := ParseForwardPath("gateway-catapi?a=1&c&b=2&b=3")
	if name != "gateway-catapi" || q["a"] != "1" || q["c"] != "" || q["b"] != "3" {
		t.Fatalf("tolerant decode failed: %q %v", name, q)
	}
}

func TestJavaURLEncode(t *testing.T) {
	if JavaURLEncode("a b") != "a+b" || JavaURLEncode("~") != "%7E" || JavaURLEncode("a+b") != "a%2Bb" {
		t.Fatalf("java encode mismatch: %q %q %q", JavaURLEncode("a b"), JavaURLEncode("~"), JavaURLEncode("a+b"))
	}
}

func TestBuildForwardURL_AppendsToExistingQuery(t *testing.T) {
	cfg := &store.ApiGateway{ApiHost: "https://h", ApiPath: "/p?fixed=1", RequireRequestParam: true, Param: "limit"}
	url, err := BuildForwardURL(cfg, map[string]string{"limit": "5"})
	if err != nil {
		t.Fatalf("unexpected err: %v", err)
	}
	if url != "https://h/p?fixed=1&limit=5" {
		t.Fatalf("bad url %q", url)
	}
}

// A structured body must reach upstream as JSON, not as Go's %v formatting.
func TestBodyReader_JSONEncodesStructuredBodies(t *testing.T) {
	cases := map[string]any{
		`{"a":1}`:              map[string]any{"a": 1},
		`["x"]`:                []any{"x"},
		`text`:                 "text",
		``:                     []byte{},
		`{"nested":{"k":"v"}}`: map[string]any{"nested": map[string]any{"k": "v"}},
	}
	for want, in := range cases {
		rdr, err := bodyReader(in)
		if err != nil {
			t.Fatalf("bodyReader(%v): %v", in, err)
		}
		got, _ := io.ReadAll(rdr)
		if string(got) != want {
			t.Fatalf("bodyReader(%#v) = %q, want %q", in, got, want)
		}
	}
	if rdr, err := bodyReader(nil); err != nil || rdr != nil {
		t.Fatalf("nil body should yield a nil reader, got %v %v", rdr, err)
	}
}
