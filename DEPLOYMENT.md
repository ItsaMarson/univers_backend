# Spring Boot + Frontend Integration Guide

## Overview

This project serves a Vite-based React frontend alongside a Spring Boot REST API:

- **Frontend**: Served at root (`/`) with client-side routing support
- **REST API**: All endpoints under `/api` prefix
- **WebSocket**: Available at `/ws` (not under `/api`)

## Architecture

### URL Structure

```
https://example.com/                    → Frontend (index.html + assets)
https://example.com/login               → Frontend route (client-side)
https://example.com/events              → Frontend route (client-side)
https://example.com/api/auth/login      → Backend REST API
https://example.com/api/events          → Backend REST API
https://example.com/ws                  → WebSocket endpoint
```

### How It Works

1. **Static Resources**: Frontend build output is placed in `src/main/resources/static/`
2. **API Context Path**: All Spring controllers are prefixed with `/api` via `server.servlet.context-path=/api`
3. **Client-Side Routing**: The `WebConfig` class forwards all non-API, non-static requests to `index.html`
4. **Security**: `SecurityConfig` allows public access to static assets and `/ws`, while protecting API endpoints

## Configuration Files

### Backend Configuration

#### `application.properties`
```properties
server.servlet.context-path=/api
```
This adds `/api` prefix to all REST controllers.

#### `WebConfig.java`
Handles:
- Serving static assets from `classpath:/static/`
- Forwarding all non-API routes to `index.html` for client-side routing
- Caching strategy for assets

#### `SecurityConfig.java`
Configures:
- Public access to: `/`, `/assets/**`, static files, `/ws/**`, and public API endpoints
- Authentication for all `/api/**` endpoints
- SUPER_ADMIN role restriction for `/api/admin/**`

### Frontend Configuration

#### `src/main/resources/frontend/src/lib/auth.ts`
```typescript
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api";
```
Uses relative path `/api` by default, allowing the frontend to call the backend API.

#### `src/main/resources/frontend/vite.config.ts`
```typescript
build: {
  outDir: "dist",
  emptyOutDir: true,
}
```
Builds frontend to `dist/` directory.

## Build Process

### Development

1. **Frontend Only** (with hot reload):
   ```bash
   cd src/main/resources/frontend
   pnpm install
   pnpm run dev
   ```
   Frontend runs on `http://localhost:5173`
   Set `VITE_API_BASE_URL=http://localhost:8080/api` in frontend `.env`

2. **Backend Only**:
   ```bash
   mvn spring-boot:run
   ```
   Backend runs on `http://localhost:8080`

### Production

1. **Build Frontend**:
   ```bash
   ./build-frontend.sh
   ```
   This script:
   - Installs dependencies (if needed)
   - Builds frontend: `pnpm run build`
   - Copies `dist/*` to `src/main/resources/static/`

2. **Package Application**:
   ```bash
   mvn clean package
   ```
   Creates a single JAR with both frontend and backend.

3. **Run**:
   ```bash
   java -jar target/univers_backend-0.0.1-SNAPSHOT.jar
   ```
   Everything served from `http://localhost:8080`

## Directory Structure

```
univers_backend/
├── src/main/
│   ├── java/
│   │   └── com/univers/univers_backend/
│   │       ├── config/
│   │       │   ├── SecurityConfig.java    # Security + CORS
│   │       │   ├── WebConfig.java         # Static resources + routing
│   │       │   └── WebSocketConfig.java   # WebSocket setup
│   │       └── Controller/                # REST controllers (all under /api)
│   └── resources/
│       ├── static/                        # Built frontend (ignored in git)
│       │   ├── index.html
│       │   └── assets/
│       ├── frontend/                      # Frontend source (git submodule)
│       │   ├── src/
│       │   ├── package.json
│       │   └── vite.config.ts
│       └── application.properties
└── build-frontend.sh                      # Frontend build script
```

## Environment Variables

### Backend (`.env` or environment)
- `MYSQL_DB_*`: Database connection
- `JWT_SECRET_KEY`: JWT signing key
- `MAILJET_*`: Email service
- `MINIO_*`: File storage
- `CORS_ALLOWED_ORIGIN`: Only needed if running frontend separately in dev

### Frontend (Development only)
- `VITE_API_BASE_URL`: API endpoint (default: `/api`)

## Common Tasks

### Update Frontend
```bash
cd src/main/resources/frontend
git pull origin main
cd ../../../..
./build-frontend.sh
```

### Deploy Updates
```bash
# Update frontend
./build-frontend.sh

# Build backend with new frontend
mvn clean package

# Deploy JAR
java -jar target/univers_backend-0.0.1-SNAPSHOT.jar
```

### Troubleshooting

**Problem**: Frontend routes return 404
- **Solution**: Make sure `WebConfig` is properly configured and `static/` folder contains `index.html`

**Problem**: API calls fail with CORS errors
- **Solution**: In production, `CORS_ALLOWED_ORIGIN` should not be needed. Check `SecurityConfig`.

**Problem**: WebSocket connection fails
- **Solution**: Verify `/ws/**` is permitted in `SecurityConfig` and not using `/api/ws`

**Problem**: Changes to frontend not reflected
- **Solution**: Run `./build-frontend.sh` again and restart Spring Boot

## Best Practices

1. **Always run `./build-frontend.sh`** before building the JAR for production
2. **Don't commit** `src/main/resources/static/` (it's in `.gitignore`)
3. **Use environment variables** for sensitive configuration
4. **Test the production build** locally before deploying:
   ```bash
   ./build-frontend.sh
   mvn clean package
   java -jar target/*.jar
   # Visit http://localhost:8080
   ```

## WebSocket Considerations

The WebSocket endpoint (`/ws`) is **outside** the `/api` context path because:
1. WebSocket is not a REST API
2. Clients need a predictable endpoint that doesn't change with API versioning
3. Simpler configuration and routing

Frontend automatically constructs the correct WebSocket URL based on the current host and protocol.
