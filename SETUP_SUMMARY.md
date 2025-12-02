# Quick Setup Summary

## What Changed

Your Spring Boot application now serves both the backend API and frontend from a single server:

### Before
- Frontend: `http://localhost:5173` (separate Vite dev server)
- Backend: `http://localhost:8080/auth/*`, `http://localhost:8080/events/*`, etc.

### After
- Everything: `http://localhost:8080`
  - Frontend: `http://localhost:8080/` (root and all routes)
  - API: `http://localhost:8080/api/*` (all API endpoints)
  - WebSocket: `http://localhost:8080/ws` (unchanged)

## Files Modified

1. **`src/main/java/com/univers/univers_backend/config/WebConfig.java`** (NEW)
   - Serves static frontend files
   - Handles client-side routing (forwards to index.html)

2. **`src/main/java/com/univers/univers_backend/config/SecurityConfig.java`**
   - Allows public access to static files
   - Updated API paths to use `/api` prefix

3. **`src/main/resources/application.properties`**
   - Added: `server.servlet.context-path=/api`

4. **`src/main/resources/frontend/src/lib/auth.ts`**
   - Changed default API_BASE_URL from `http://localhost:8080` to `/api`

5. **`src/main/resources/frontend/vite.config.ts`**
   - Added explicit build output directory configuration

6. **`src/main/resources/frontend/src/hooks/use-websocket-notifications.ts`**
   - Updated WebSocket URL logic to work with new structure

7. **`build-frontend.sh`** (NEW)
   - Automates frontend build and copy to Spring Boot resources

8. **`.gitignore`**
   - Added `/src/main/resources/static/` (ignored, generated on build)

9. **`README.md`** (UPDATED)
   - Comprehensive setup and deployment instructions

10. **`DEPLOYMENT.md`** (NEW)
    - Detailed integration guide and architecture documentation

## Quick Start

### First Time Setup
```bash
# 1. Build the frontend
./build-frontend.sh

# 2. Start Spring Boot
mvn spring-boot:run

# 3. Visit http://localhost:8080
```

### Development Workflow

**Option A: Integrated (Production-like)**
```bash
./build-frontend.sh    # Rebuild frontend when needed
mvn spring-boot:run    # Run everything together
# Visit: http://localhost:8080
```

**Option B: Separate (Hot Reload)**
```bash
# Terminal 1: Backend
mvn spring-boot:run

# Terminal 2: Frontend
cd src/main/resources/frontend
pnpm run dev
# Visit: http://localhost:5173
# (needs VITE_API_BASE_URL=http://localhost:8080/api in frontend .env)
```

## API Path Changes

All your API endpoints are now under `/api`:

| Old Path | New Path |
|----------|----------|
| `/auth/login` | `/api/auth/login` |
| `/events` | `/api/events` |
| `/venues` | `/api/venues` |
| `/equipments` | `/api/equipments` |
| `/admin/users` | `/api/admin/users` |

**The frontend automatically uses the correct paths**, so no manual updates needed in API calls.

## Testing

1. **Build and test locally:**
   ```bash
   ./build-frontend.sh
   mvn clean package
   java -jar target/univers_backend-0.0.1-SNAPSHOT.jar
   ```

2. **Open browser:**
   - Frontend: `http://localhost:8080/`
   - Login page: `http://localhost:8080/login`
   - API health: `http://localhost:8080/api/auth/me`

3. **Verify WebSocket:**
   - Login to the application
   - Check browser console for WebSocket connection logs
   - Should see: "STOMP: connected" or similar

## Deployment Checklist

- [ ] Run `./build-frontend.sh`
- [ ] Run `mvn clean package`
- [ ] Set environment variables (see README.md)
- [ ] Deploy JAR file
- [ ] Test: visit `https://yourdomain.com/`
- [ ] Test: visit `https://yourdomain.com/api/auth/me`

## Troubleshooting

**Frontend shows 404:**
- Run `./build-frontend.sh` again
- Check that `src/main/resources/static/index.html` exists

**API calls fail:**
- Check if API paths include `/api` prefix
- Check browser Network tab for actual request URLs

**WebSocket won't connect:**
- Verify `/ws/**` is allowed in SecurityConfig
- Check browser console for connection errors

**"Failed to fetch" errors:**
- In production: Check CORS settings
- In dev: Make sure backend is running

## Next Steps

1. Test the application with `./build-frontend.sh && mvn spring-boot:run`
2. Review `DEPLOYMENT.md` for detailed architecture info
3. Update any external API documentation to reflect new `/api` prefix
4. Update CI/CD pipelines to run `build-frontend.sh` before packaging

## Need Help?

See detailed documentation in:
- `README.md` - Setup and getting started
- `DEPLOYMENT.md` - Architecture and deployment guide
- `AGENTS.md` - Developer coding guidelines
