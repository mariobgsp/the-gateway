package service

import (
	"net/http"
	"testing"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

func TestForward_Allowlist(t *testing.T) {
	// URL must contain allowed limit, drop evil
	url, err := BuildForwardURL(&store.ApiGateway{ApiHost: "https://h", ApiPath: "/p", RequireRequestParam: true, Param: "limit;size"}, map[string]string{"limit": "5", "evil": "injected"})
	if err != nil {
		t.Fatalf("unexpected err: %v", err)
	}
	if got := url; !contains(got, "limit=5") || contains(got, "evil") {
		t.Fatalf("bad url %q", got)
	}
	// headers: allowlist keeps x-api-key, drops Authorization
	h := FilterHeaders("x-api-key;Content-Type", http.Header{"x-api-key": []string{"secret"}, "Authorization": []string{"Bearer x"}})
	if h.Get("x-api-key") != "secret" || h.Get("Authorization") != "" {
		t.Fatalf("bad headers %v", h)
	}
}

func TestForward_RequireParam(t *testing.T) {
	api := &store.ApiGateway{ApiHost: "https://h", ApiPath: "/p", RequireRequestParam: true, Param: "limit"}
	if _, err := BuildForwardURL(api, map[string]string{}); err == nil {
		t.Fatal("expected emptyRequestParam error")
	} else if se, ok := err.(*SvcError); !ok || se.Code != "04" {
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

func contains(s, sub string) bool {
	return len(s) >= len(sub) && (func() bool {
		for i := 0; i+len(sub) <= len(s); i++ {
			if s[i:i+len(sub)] == sub {
				return true
			}
		}
		return false
	})()
}
