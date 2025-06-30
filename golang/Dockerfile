FROM ghcr.io/joinself/go-build AS builder

WORKDIR /wd

COPY go.mod go.sum .
RUN go mod download

COPY . .

RUN go build -o app


FROM debian:bookworm-20250610-slim

COPY --from=builder /wd/app /

CMD ["/app"]
