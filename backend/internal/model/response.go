package model

type ApiResponse struct {
	Data  interface{} `json:"data"`
	Error string      `json:"error"`
}

func DataResponse(data interface{}) ApiResponse {
	return ApiResponse{Data: data, Error: ""}
}

func ErrorResponse(msg string) ApiResponse {
	return ApiResponse{Data: nil, Error: msg}
}
