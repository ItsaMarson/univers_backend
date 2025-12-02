#!/bin/bash
set -e

echo "Building frontend..."

# Navigate to frontend directory
cd src/main/resources/frontend

# Detect package manager
if command -v pnpm &>/dev/null; then
        PKG_MGR="pnpm"
elif command -v npm &>/dev/null; then
        PKG_MGR="npm"
else
        echo "Error: Neither pnpm nor npm found. Please install Node.js and a package manager."
        exit 1
fi

echo "Using package manager: $PKG_MGR"

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
        echo "Installing frontend dependencies..."
        $PKG_MGR install
fi

# Build the frontend
echo "Running frontend build..."
$PKG_MGR run build

# Create target static directory if it doesn't exist
mkdir -p ../static

# Remove old static files
echo "Cleaning old static files..."
rm -rf ../static/*

# Copy built files to Spring Boot static resources
echo "Copying built files to Spring Boot static resources..."
cp -r dist/* ../static/

echo "Frontend build complete! Files copied to src/main/resources/static/"
