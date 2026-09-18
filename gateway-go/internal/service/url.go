package service

import (
	"fmt"
	"strings"

	"github.com/mariobgsp/the-gateway/gateway-go/internal/api"
	"github.com/mariobgsp/the-gateway/gateway-go/internal/store"
)

// splitConfig splits a ";"-separated allowlist, trimming and dropping blanks.
func splitConfig(config string) []string {
	var out []string
	for _, s := range strings.Split(config, ";") {
		if t := strings.TrimSpace(s); t != "" {
			out = append(out, t)
		}
	}
	return out
}

// ParseForwardPath splits an identifier from its query string the tolerant way:
// "?foo" yields "", the last duplicate key wins, and nothing is decoded twice
// because the transport already decoded it.
func ParseForwardPath(path string) (string, map[string]string) {
	name, qs, _ := strings.Cut(path, "?")
	params := map[string]string{}
	for _, pair := range strings.Split(qs, "&") {
		if pair == "" {
			continue
		}
		k, v, _ := strings.Cut(pair, "=")
		params[k] = v
	}
	return name, params
}

// BuildForwardURL joins host and path, and appends the allowlisted query
// parameters. It is the only place query parameters are encoded.
func BuildForwardURL(cfg *store.ApiGateway, params map[string]string) (string, error) {
	url := cfg.ApiHost + cfg.ApiPath
	if !cfg.RequireRequestParam {
		return url, nil
	}
	if len(params) == 0 {
		return "", api.BadRequest("Bad request: emptyRequestParam")
	}
	allowed := map[string]bool{}
	for _, name := range splitConfig(cfg.Param) {
		allowed[name] = true
	}
	var q strings.Builder
	for k, v := range params {
		if !allowed[k] {
			continue
		}
		if q.Len() > 0 {
			q.WriteByte('&')
		}
		q.WriteString(JavaURLEncode(k))
		q.WriteByte('=')
		q.WriteString(JavaURLEncode(v))
	}
	if q.Len() == 0 {
		return url, nil
	}
	separator := "?"
	if strings.Contains(url, "?") {
		separator = "&"
	}
	return url + separator + q.String(), nil
}

// JavaURLEncode replicates URLEncoder.encode(s, UTF-8): unreserved characters
// pass through, a space becomes "+", everything else becomes %XX uppercase.
func JavaURLEncode(s string) string {
	var b strings.Builder
	for i := 0; i < len(s); i++ {
		c := s[i]
		if isUnreserved(c) {
			b.WriteByte(c)
		} else if c == ' ' {
			b.WriteByte('+')
		} else {
			fmt.Fprintf(&b, "%%%02X", c)
		}
	}
	return b.String()
}

func isUnreserved(c byte) bool {
	return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' ||
		c == '.' || c == '-' || c == '*' || c == '_'
}
