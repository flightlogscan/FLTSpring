#!/bin/bash

# Prevent command history logging
set +o history

# Exit on any error
set -e

# Configuration
ENV_FILE="/etc/flightlogscan.env"
PORT=8080
TIMEOUT=60  # Max seconds to wait for health check
INTERVAL=5  # Seconds between health check retries

# Function to log messages
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

if ! sudo test -f "$ENV_FILE"; then
    log "ERROR: Environment file $ENV_FILE not found"
    exit 1
fi

if ! sudo test -r "$ENV_FILE"; then
    log "ERROR: Environment file $ENV_FILE is not readable"
    exit 1
fi

IMAGE_TAG=${IMAGE_TAG:-latest}
log "Pulling image flightlogscanner/flightlogscan:$IMAGE_TAG from Docker Hub..."
sudo docker pull flightlogscanner/flightlogscan:$IMAGE_TAG

log "Removing existing container if present..."
sudo docker rm -f flightlogscan 2>/dev/null || true

log "Checking port $PORT availability..."
if sudo lsof -i :$PORT; then
    log "ERROR: Another process is using port $PORT"
    exit 1
fi

log "Ensuring host log directory /var/log/FLTSpring exists..."
sudo mkdir -p /var/log/FLTSpring
# Ensure the user inside the container can write to the mounted volume.
sudo chmod 777 /var/log/FLTSpring

log "Starting new container with image tag $IMAGE_TAG..."
sudo docker run -d \
    --platform linux/amd64 \
    --env-file "$ENV_FILE" \
    --name flightlogscan \
    --restart unless-stopped \
    -p $PORT:8080 \
    -v /var/log/FLTSpring:/var/log/FLTSpring \
    flightlogscanner/flightlogscan:$IMAGE_TAG

log "Starting health check (timeout: ${TIMEOUT}s)..."
start_time=$(date +%s)
timeout=$TIMEOUT

while true; do
    if curl -sf --connect-timeout 2 http://localhost:$PORT/api/ping >/dev/null; then
        log "Health check passed!"
        break
    fi

    sleep $INTERVAL
    elapsed=$(( $(date +%s) - start_time ))

    if [ $elapsed -ge $TIMEOUT ]; then
        log "ERROR: Health check failed after ${TIMEOUT}s"
        log "Showing container logs:"
        sudo docker logs flightlogscan --tail 50
        exit 1
    fi
    log "Waiting for service... (${elapsed}s elapsed)"
done

log "Pruning unused and old Docker images..."
sudo docker image prune -a -f --filter "until=24h" --filter "label=repository=flightlogscanner/flightlogscan"

set -o history

log "Deployment completed successfully"
