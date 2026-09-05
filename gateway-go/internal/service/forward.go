package service

import (
	"context"
	"fmt"
	"io"
	"net"
	"net/http"
	"strings"
	"time"

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
	Lookup func(ctx context.Context, identifier string) (*store.ApiGateway, error)
	Client *http.Client
}

func NewForward(db *store.Postgres, client *http.Client) *Forward {
	if client == nil {
		client = defaultClient()
	}
	f := &Forward{Client: client}
	if db != nil {
		f.Lookup = db.FindApiByIdentifier
	}
	return f
}

func defaultClient() *http.Client {
	return &http.Client{
		Timeout: 30 * time.Second,
		Transport: &http.Transport{
			DialContext: (&net.Dialer{Timeout: 5 * time.Second}).DialContext,
		},
	}
}

type ForwardResult struct {
	Body       any
	StatusCode int
}

// Do mirrors processForwardApi: load config, build URL, filter headers, require body, call upstream.
func (f *Forward) Do(ctx context.Context, req ForwardRequest) (*ForwardResult, error) {
	if f.Lookup == nil {
		return nil, &SvcError{HTTPStatus: 404, Code: "02", Message: "02:NotFound:API Gateway Configuration Not Found"}
	}
	api, err := f.Lookup(ctx, req.PathName)
	if err != nil {
		return nil, &SvcError{HTTPStatus: 404, Code: "02", Message: "02:NotFound:API Gateway Configuration Not Found"}
	}
	url, err := BuildForwardURL(api, req.QueryParams)
	if err != nil {
		return nil, err
	}
	headers := FilterHeaders(api.Header, req.Headers)
	if api.RequireRequestBody && req.Body == nil {
		return nil, &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:Bad request: emptyRequestBody"}
	}
	body, status, err := invokeUpstream(ctx, f.Client, url, api.Method, headers, req.Body)
	if err != nil {
		return nil, &SvcError{HTTPStatus: 500, Code: "99", Message: "99:internalServerError:" + err.Error()}
	}
	return &ForwardResult{Body: body, StatusCode: status}, nil
}

// SplitConfig mirrors CommonUtil.splitConfig: ";"-split, trim, drop empty.
func SplitConfig(config string) []string {
	if strings.TrimSpace(config) == "" {
		return nil
	}
	var out []string
	for _, s := range strings.Split(config, ";") {
		if t := strings.TrimSpace(s); t != "" {
			out = append(out, t)
		}
	}
	return out
}

// ParseQuery mirrors toForwardRequest tolerant parse: split "?" once, "&"-split, "=" once, "?foo"->"", last-wins, skip empty pairs, no decoding.
func ParseForwardPath(path string) (string, map[string]string) {
	name, qs, _ := strings.Cut(path, "?")
	params := map[string]string{}
	if qs == "" {
		return name, params
	}
	for _, pair := range strings.Split(qs, "&") {
		if pair == "" {
			continue
		}
		k, v, _ := strings.Cut(pair, "=")
		params[k] = v // ponytail: last-wins, no second decode (container already decoded)
	}
	return name, params
}

// FilterHeaders mirrors ApiGatewayServices.filterHeaders: case-insensitive allowlist, copy all values.
func FilterHeaders(config string, incoming http.Header) http.Header {
	out := http.Header{}
	if len(incoming) == 0 {
		return out
	}
	allowed := SplitConfig(config)
	for k, vals := range incoming {
		for _, a := range allowed {
			if strings.EqualFold(a, k) {
				for _, v := range vals {
					out.Add(k, v)
				}
				break
			}
		}
	}
	return out
}

// BuildForwardURL mirrors buildForwardUrl: host+path, requireRequestParam gate, allowlist, Java-URLEncoder, ? vs &.
func BuildForwardURL(api *store.ApiGateway, params map[string]string) (string, error) {
	url := api.ApiHost + api.ApiPath
	if !api.RequireRequestParam {
		return url, nil
	}
	if len(params) == 0 {
		return "", &SvcError{HTTPStatus: 400, Code: "04", Message: "04:BadRequest:Bad request: emptyRequestParam"}
	}
	allowed := SplitConfig(api.Param)
	allowedSet := map[string]bool{}
	for _, a := range allowed {
		allowedSet[a] = true
	}
	var q strings.Builder
	for k, v := range params {
		if !allowedSet[k] {
			continue
		}
		if q.Len() > 0 {
			q.WriteByte('&')
		}
		q.WriteString(JavaURLEncode(k))
		q.WriteByte('=')
		q.WriteString(JavaURLEncode(v))
	}
	if q.Len() > 0 {
		if strings.Contains(url, "?") {
			url += "&"
		} else {
			url += "?"
		}
		url += q.String()
	}
	return url, nil
}

// JavaURLEncode replicates URLEncoder.encode(s, UTF-8): [0-9a-zA-Z.-*_+] kept except space->"+", rest %XX uppercase.
func JavaURLEncode(s string) string {
	var b strings.Builder
	for i := 0; i < len(s); i++ {
		c := s[i]
		if c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '.' || c == '-' || c == '*' || c == '_' {
			b.WriteByte(c)
		} else if c == ' ' {
			b.WriteByte('+')
		} else {
			fmt.Fprintf(&b, "%%%02X", c)
		}
	}
	return b.String()
}

func invokeUpstream(ctx context.Context, client *http.Client, url, method string, headers http.Header, body any) (any, int, error) {
	var rdr io.Reader
	if body != nil {
		switch v := body.(type) {
		case string:
			rdr = strings.NewReader(v)
		case []byte:
			rdr = strings.NewReader(string(v))
		default:
			rdr = strings.NewReader(toJSON(body))
		}
	}
	m := strings.ToUpper(method)
	if m == "" {
		m = "GET"
	}
	req, err := http.NewRequestWithContext(ctx, m, url, rdr)
	if err != nil {
		return nil, 0, err
	}
	for k, vals := range headers {
		for _, v := range vals {
			req.Header.Add(k, v)
		}
	}
	resp, err := client.Do(req)
	if err != nil {
		return nil, 0, err
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	return string(raw), resp.StatusCode, nil
}

func toJSON(v any) string {
	switch t := v.(type) {
	case string:
		return t
	default:
		// ponytail: upstream body passthrough — minimal marshal, envelope handles display
		return fmt.Sprintf("%v", v)
	}
}

// SvcError mirrors CommonException: http status + code + message.
type SvcError struct {
	HTTPStatus int
	Code       string
	Message    string
}

func (e *SvcError) Error() string { return e.Message }
