// Package api owns the gateway's wire contract: the response envelope every
// handler returns, the fault codes carried in it, and the one place a fault is
// turned into a response.
package api

// Code is the envelope code carried by every response.
type Code string

const (
	CodeOK              Code = "00"
	CodeUserNotFound    Code = "01"
	CodeNotFound        Code = "02"
	CodeStoreNotFound   Code = "03"
	CodeBadRequest      Code = "04"
	CodeTooManyRequests Code = "05"
	CodeBadCredentials  Code = "06"
	CodeUnauthorized    Code = "98"
	CodeInternal        Code = "99"
)

// Response is the envelope every endpoint returns. HTTPStatus is server-side.
type Response struct {
	Status       string `json:"status,omitempty"`
	Code         Code   `json:"code,omitempty"`
	Message      string `json:"message,omitempty"`
	Data         any    `json:"data,omitempty"`
	ErrorMessage string `json:"errorMessage,omitempty"`
	HTTPStatus   int    `json:"-"`
}

// OK answers with data.
func OK(data any) Response {
	return Response{Status: "ok", Code: CodeOK, Message: "success", Data: data, HTTPStatus: 200}
}

// OKMsg answers with a message and no data.
func OKMsg(msg string) Response {
	return Response{Status: "ok", Code: CodeOK, Message: msg, HTTPStatus: 200}
}

// OKStatus carries an upstream status code through the envelope.
func OKStatus(httpStatus int, data any) Response {
	return Response{Status: "ok", Code: CodeOK, Message: "success", Data: data, HTTPStatus: httpStatus}
}

// OKList answers a list endpoint. An absent result renders as [] rather than
// being dropped from the envelope by omitempty.
func OKList[T any](items []T) Response {
	if items == nil {
		items = []T{}
	}
	return OK(items)
}

// Error is a fault together with the HTTP status and envelope code it maps to.
type Error struct {
	HTTPStatus int
	Code       Code
	Message    string
}

func (e *Error) Error() string { return e.Message }

// Response renders the fault as an envelope.
func (e *Error) Response() Response {
	status, ok := statusName[e.HTTPStatus]
	if !ok {
		status = "unknownError"
	}
	return Response{Status: status, Code: e.Code, Message: e.Message, HTTPStatus: e.HTTPStatus}
}

func BadRequest(msg string) *Error {
	return &Error{HTTPStatus: 400, Code: CodeBadRequest, Message: msg}
}

func UserNotFound() *Error {
	return &Error{HTTPStatus: 404, Code: CodeUserNotFound, Message: "user not found!"}
}

func NotFound(msg string) *Error {
	return &Error{HTTPStatus: 404, Code: CodeNotFound, Message: msg}
}

func StoreNotFound() *Error {
	return &Error{HTTPStatus: 404, Code: CodeStoreNotFound, Message: "store not found!"}
}

func Unauthorized(msg string) *Error {
	return &Error{HTTPStatus: 401, Code: CodeUnauthorized, Message: msg}
}

func BadCredentials() *Error {
	return &Error{HTTPStatus: 401, Code: CodeBadCredentials, Message: "invalid username or password"}
}

func TooManyRequests(msg string) *Error {
	return &Error{HTTPStatus: 429, Code: CodeTooManyRequests, Message: msg}
}

// Internal wraps an unexpected failure. A nil or empty error still yields a
// readable message.
func Internal(err error) *Error {
	msg := "UnknownError"
	if err != nil && err.Error() != "" {
		msg = err.Error()
	}
	return &Error{HTTPStatus: 500, Code: CodeInternal, Message: msg}
}

var statusName = map[int]string{
	400: "BAD_REQUEST",
	401: "UNAUTHORIZED",
	403: "FORBIDDEN",
	404: "NOT_FOUND",
	429: "TOO_MANY_REQUESTS",
	500: "INTERNAL_SERVER_ERROR",
}
