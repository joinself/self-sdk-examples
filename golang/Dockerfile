FROM golang:1.25.7-trixie AS builder

WORKDIR /wd

RUN <<EOF
  mkdir -m 0755 -p /etc/apt/keyrings
  curl -fsSL https://europe-apt.pkg.dev/doc/repo-signing-key.gpg | gpg --dearmor -o /etc/apt/keyrings/self.gpg
  echo "deb [signed-by=/etc/apt/keyrings/self.gpg] https://europe-apt.pkg.dev/projects/principal-oxide-204416 apt main" > /etc/apt/sources.list.d/self.list
  apt-get update
  apt-get install -y self-sdk=0.85.0-48
EOF

COPY go.mod go.sum .
RUN go mod download

COPY . .

RUN go build -o app


FROM debian:bookworm-20250610-slim

COPY --from=builder /wd/app /

CMD ["/app"]
