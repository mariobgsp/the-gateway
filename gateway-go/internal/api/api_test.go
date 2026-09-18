package api

import (
	"encoding/json"
	"errors"
	"testing"
)

func TestErrorResponseMapsStatus(t *testing.T) {
	cases := []struct {
		fault  *Error
		status string
		code   Code
		http   int
	}{
		{BadRequest("bad"), "BAD_REQUEST", CodeBadRequest, 400},
		{UserNotFound(), "NOT_FOUND", CodeUserNotFound, 404},
		{NotFound("gone"), "NOT_FOUND", CodeNotFound, 404},
		{StoreNotFound(), "NOT_FOUND", CodeStoreNotFound, 404},
		{Unauthorized("nope"), "UNAUTHORIZED", CodeUnauthorized, 401},
		{BadCredentials(), "UNAUTHORIZED", CodeBadCredentials, 401},
		{TooManyRequests("slow down"), "TOO_MANY_REQUESTS", CodeTooManyRequests, 429},
	}
	for _, c := range cases {
		got := c.fault.Response()
		if got.Status != c.status || got.Code != c.code || got.HTTPStatus != c.http {
			t.Fatalf("%+v -> %+v", c.fault, got)
		}
		if got.Message != c.fault.Message {
			t.Fatalf("message dropped: %+v", got)
		}
	}
}

// An unmapped status still renders rather than dropping the status field.
func TestErrorResponseUnknownStatus(t *testing.T) {
	got := (&Error{HTTPStatus: 418, Code: CodeInternal, Message: "teapot"}).Response()
	if got.Status != "unknownError" || got.HTTPStatus != 418 {
		t.Fatalf("unexpected: %+v", got)
	}
}

func TestInternalKeepsMessage(t *testing.T) {
	if got := Internal(errors.New("boom")).Message; got != "boom" {
		t.Fatalf("message %q", got)
	}
	if got := Internal(nil).Message; got != "UnknownError" {
		t.Fatalf("nil error message %q", got)
	}
	if got := Internal(errors.New("")).Message; got != "UnknownError" {
		t.Fatalf("empty error message %q", got)
	}
}

// An absent list must serialize as [] so clients can map over it unconditionally.
func TestOKListRendersEmptyArray(t *testing.T) {
	var absent []string
	raw, err := json.Marshal(OKList(absent))
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	if string(raw) != `{"status":"ok","code":"00","message":"success","data":[]}` {
		t.Fatalf("absent list rendered as %s", raw)
	}
	raw, err = json.Marshal(OKStatus(201, map[string]any{"a": 1}))
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	if string(raw) != `{"status":"ok","code":"00","message":"success","data":{"a":1}}` {
		t.Fatalf("OKStatus rendered as %s", raw)
	}
}
