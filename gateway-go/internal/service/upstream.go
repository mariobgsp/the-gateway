package service

import (
	"bytes"
	"context"
	"encoding/json"
	"io"
	"net"
	"net/http"
	"strings"
	"time"
)

func defaultClient() *http.Client {
	return &http.Client{
		Timeout: 30 * time.Second,
		Transport: &http.Transport{
			DialContext: (&net.Dialer{Timeout: 5 * time.Second}).DialContext,
		},
	}
}

// FilterHeaders keeps only the allowlisted headers, matching names
// case-insensitively.
func FilterHeaders(config string, incoming http.Header) http.Header {
	out := http.Header{}
	allowed := splitConfig(config)
	for name, values := range incoming {
		for _, a := range allowed {
			if strings.EqualFold(a, name) {
				for _, value := range values {
					out.Add(name, value)
				}
				break
			}
		}
	}
	return out
}

func invokeUpstream(ctx context.Context, client *http.Client, url, method string, headers http.Header, body any) (any, int, error) {
	reader, err := bodyReader(body)
	if err != nil {
		return nil, 0, err
	}
	verb := strings.ToUpper(method)
	if verb == "" {
		verb = "GET"
	}
	req, err := http.NewRequestWithContext(ctx, verb, url, reader)
	if err != nil {
		return nil, 0, err
	}
	req.Header = headers
	resp, err := client.Do(req)
	if err != nil {
		return nil, 0, err
	}
	defer resp.Body.Close()
	raw, _ := io.ReadAll(resp.Body)
	return string(raw), resp.StatusCode, nil
}

// bodyReader passes text and byte payloads through untouched and JSON-encodes
// everything else, so a structured body actually reaches upstream as JSON.
func bodyReader(body any) (io.Reader, error) {
	switch v := body.(type) {
	case nil:
		return nil, nil
	case string:
		return strings.NewReader(v), nil
	case []byte:
		return bytes.NewReader(v), nil
	default:
		raw, err := json.Marshal(v)
		if err != nil {
			return nil, err
		}
		return bytes.NewReader(raw), nil
	}
}
