#!/usr/bin/env bash
# Installs the Docker Compose plugin.

set -euo pipefail

VERSION=v2.29.7
PLUGIN_DIR=/usr/local/lib/docker/cli-plugins

if docker compose version >/dev/null 2>&1; then
    echo "Docker Compose is already installed: $(docker compose version)"
    exit 0
fi

sudo mkdir -p "$PLUGIN_DIR"
# uname -m gives x86_64 or aarch64, which match the names of the release downloads
sudo curl -fSL "https://github.com/docker/compose/releases/download/$VERSION/docker-compose-linux-$(uname -m)" \
    -o "$PLUGIN_DIR/docker-compose"
sudo chmod +x "$PLUGIN_DIR/docker-compose"

docker compose version
