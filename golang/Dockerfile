FROM golang:1.24.2-bullseye

WORKDIR /app

# Install system dependencies
RUN apt-get update && \
    apt-get install -y curl gpg && \
    mkdir -m 0755 -p /etc/apt/keyrings && \
    curl -fsSL https://europe-apt.pkg.dev/doc/repo-signing-key.gpg | gpg --dearmor -o /etc/apt/keyrings/self.gpg && \
    echo "deb [signed-by=/etc/apt/keyrings/self.gpg] https://europe-apt.pkg.dev/projects/principal-oxide-204416 apt main" > /etc/apt/sources.list.d/self.list && \
    apt-get update && \
    apt-get install -y self-sdk=0.84.0 && \
    rm -rf /var/lib/apt/lists/* /tmp/*

# Copy go mod files
COPY go.mod go.sum ./
RUN go mod download

# Copy the rest of the application
COPY . .

# Set LD_LIBRARY_PATH for runtime
ENV LD_LIBRARY_PATH="/usr/lib"

# Expose port
EXPOSE 8080

# Run the application
CMD ["go", "run", "./..."]
