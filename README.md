# UniVERS - BackEnd

# UniVERS 🎓📅

**UniVERS** is a centralized, automated reservation system with an integrated approval workflow. It enables seamless venue and equipment reservations with real-time availability tracking, approval hierarchies, and automated notifications.

## 📌 Prerequisites
- Java 21
- MySQL server
- Maven
- Node.js and pnpm (for frontend build)

## 🚀 Getting Started

### 1. Clone Repository
```bash
git clone --recurse-submodules https://github.com/ItsaMarson/univers_backend
cd univers_backend
```

### 2. Setup Database

#### Option A: Using Docker (Linux/Windows with Docker)
If you have Docker installed, you can start the MySQL service automatically:

**Linux/macOS:**
```bash
docker-compose up -d
```

**Windows (PowerShell/CMD):**
```bash
docker-compose up -d
```

This will set up MySQL. Skip to step 3.

#### Option B: Manual Setup (Windows/Linux without Docker)
If not using Docker, install and configure MySQL manually:

**MySQL Setup:**
```sql
CREATE DATABASE univers CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Configure Environment Variables

Create a `.env` file in the project root with the following variables:

```bash
# Database
MYSQL_DB_ENDPOINT=jdbc:mysql://localhost:3306/univers
MYSQL_DB_USERNAME=root
MYSQL_DB_PASSWORD=your_password

# JWT
JWT_SECRET_KEY=your_secret_key

# Email (Mailjet)
MAILJET_API_KEY=your_mailjet_api_key
MAILJET_API_SECRET=your_mailjet_api_secret
MAILJET_SENDER_EMAIL=noreply@example.com
MAILJET_TEMPLATE_ID=your_template_id
MAILJET_TEMPLATE_ID_FORGOT_PASSWORD=your_forgot_password_template_id

# Storage (Local Filesystem)
STORAGE_LOCATION=./storage

# CORS (only needed for dev with separate frontend server)
CORS_ALLOWED_ORIGIN=http://localhost:5173
```


### 4. Build Frontend (First Time and After Frontend Changes)

**Linux/macOS:**
```bash
chmod +x build-frontend.sh
./build-frontend.sh
```

**Windows (PowerShell):**
```powershell
.\build-frontend.ps1
```

**Windows (Command Prompt):**
```cmd
build-frontend.bat
```

This script will:
- Install frontend dependencies
- Build the frontend
- Copy built files to `src/main/resources/static/`

### 5. Build and Run Backend

**All platforms (Linux/Windows/macOS):**
```bash
mvn spring-boot:run
```

Or using Maven wrapper:

**Linux/macOS:**
```bash
./mvnw spring-boot:run
```

**Windows:**
```cmd
mvnw.cmd spring-boot:run
```

## 📂 Project Structure

- **Backend API**: All endpoints are under `/api` prefix
  - Auth: `/api/auth/*`
  - Users: `/api/users/*`, `/api/admin/users/*`
  - Events: `/api/events/*`
  - Venues: `/api/venues/*`
  - Equipment: `/api/equipments/*`
  - Departments: `/api/departments/*`
  - Files: `/api/files/*` (Local storage retrieval)
  
- **Frontend**: Served at root `/` with client-side routing support

## 🔧 Development

### Frontend Development (with Hot Reload)
During frontend development, you can run the frontend dev server separately:

```bash
cd src/main/resources/frontend
pnpm install
pnpm run dev
```

Set `VITE_API_BASE_URL=http://localhost:8080/api` in your frontend `.env` file.

### Backend Only
```bash
mvn spring-boot:run
```

### Code Formatting
```bash
# Check code formatting
mvn spotless:check

# Auto-format code
mvn spotless:apply
```

### Run Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn -Dtest=YourTestClass test
```

## 🌐 Deployment

### Production Build

1. Build the frontend:

**Linux/macOS:**
```bash
./build-frontend.sh
```

**Windows (PowerShell):**
```powershell
.\build-frontend.ps1
```

**Windows (Command Prompt):**
```cmd
build-frontend.bat
```

2. Package the application:

**All platforms:**
```bash
mvn clean package -DskipTests
```

Or using Maven wrapper:

**Linux/macOS:**
```bash
./mvnw clean package -DskipTests
```

**Windows:**
```cmd
mvnw.cmd clean package -DskipTests
```

3. Run the JAR:

**All platforms:**
```bash
java -jar target/univers_backend-0.0.1-SNAPSHOT.jar
```

The application will serve:
- Frontend at `http://localhost:8080/`
- API at `http://localhost:8080/api/`

### Environment Variables for Production

Set the following environment variables in production:
- `MYSQL_DB_ENDPOINT`
- `MYSQL_DB_USERNAME`
- `MYSQL_DB_PASSWORD`
- `JWT_SECRET_KEY` (use a strong secret!)
- `MAILJET_*` variables
- `STORAGE_LOCATION`
- `CORS_ALLOWED_ORIGIN` (if needed)

## 📝 API Documentation

Once the application is running, access the OpenAPI documentation at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`

## 🤝 Contributing

1. Create a feature branch
2. Make your changes
3. Run `mvn spotless:apply` to format code
4. Submit a pull request

## 🚀 Happy Coding! 🎉
