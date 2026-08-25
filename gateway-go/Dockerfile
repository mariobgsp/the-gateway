FROM golang:1.22-alpine AS build
WORKDIR /app
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /out/gateway-go ./cmd/server

FROM scratch
COPY --from=build /out/gateway-go /gateway-go
EXPOSE 8080
ENTRYPOINT ["/gateway-go"]
