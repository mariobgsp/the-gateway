package envelope

// Response mirrors Java Response<T> envelope. httpStatus stays server-side.
type Response struct {
	Status       string `json:"status,omitempty"`
	Code         string `json:"code,omitempty"`
	Message      string `json:"message,omitempty"`
	Data         any    `json:"data,omitempty"`
	ErrorMessage string `json:"errorMessage,omitempty"`
	HTTPStatus   int    `json:"-"`
}

func Ok(data any) Response {
	return Response{Status: "ok", Code: "00", Message: "success", Data: data, HTTPStatus: 200}
}

func OkMsg(msg string) Response {
	return Response{Status: "ok", Code: "00", Message: msg, HTTPStatus: 200}
}

func OkData(msg string, data any) Response {
	return Response{Status: "ok", Code: "00", Message: msg, Data: data, HTTPStatus: 200}
}

// Err mirrors CommonUtil.applyError: CommonException -> its status/code, else 500/99.
func Err(httpStatus int, status, code, message string) Response {
	if code == "" {
		code = "99"
	}
	if message == "" {
		message = "UnknownError"
	}
	return Response{Status: status, Code: code, Message: message, HTTPStatus: httpStatus}
}
