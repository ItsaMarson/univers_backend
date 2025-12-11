// Page setup
#set page(
  margin: (x: 1.5cm, y: 2cm),
  numbering: "1",
)

// Typography
#set text(font: "Geist", size: 11pt)
#set par(justify: true, leading: 0.65em)

// Headings
#show heading.where(level: 1): it => block(
  text(size: 24pt, weight: "bold", fill: rgb("#1a1a1a"), it.body),
  spacing: 1.5em,
)
#show heading.where(level: 2): it => block(
  above: 1.5em,
  below: 1em,
  text(size: 16pt, weight: "bold", fill: rgb("#2a2a2a"), it.body),
)
#show heading.where(level: 3): it => block(
  above: 1.2em,
  below: 0.8em,
  text(size: 13pt, weight: "semibold", fill: rgb("#3a3a3a"), it.body),
)
#show heading.where(level: 4): it => block(
  above: 1em,
  below: 0.6em,
  text(size: 11.5pt, weight: "semibold", fill: rgb("#4a4a4a"), it.body),
)

// Code blocks
#show raw.where(block: true): it => block(
  fill: rgb("#f5f5f5"),
  inset: 10pt,
  radius: 4pt,
  width: 100%,
  stroke: 0.5pt + rgb("#e0e0e0"),
  text(size: 9.5pt, it),
)
#show raw.where(block: false): it => box(
  fill: rgb("#f5f5f5"),
  inset: (x: 4pt, y: 2pt),
  outset: (y: 2pt),
  radius: 2pt,
  text(size: 10pt, it),
)

// Lists
#set list(indent: 1em, spacing: 0.8em)
#set enum(indent: 1em, spacing: 0.8em)

// Tables
#set table(stroke: 0.5pt, fill: (c, r) => if r == 0 { rgb("#e8e8e8") })

// Links
#show link: it => text(fill: rgb("#0066cc"), it)

= UniVERS

#v(0.5em)

#box(
  width: 100%,
  fill: rgb("#f8f9fa"),
  inset: 15pt,
  radius: 4pt,
  stroke: 0.5pt + rgb("#dee2e6"),
)[
  *UniVERS* is a *centralized, automated reservation system* with an integrated approval workflow. It enables seamless *venue and equipment reservations* with *real-time availability tracking*, approval hierarchies, and automated notifications.
]

#v(1em)

== 📌 Prerequisites

#box(
  width: 100%,
  fill: rgb("#fff3cd"),
  inset: 12pt,
  radius: 4pt,
  stroke: 0.5pt + rgb("#ffc107"),
)[
  - Java 21
  - MySQL server
  - Maven
  - Node.js and pnpm (for frontend build)
  - MinIO server (for object storage)
]

#v(0.5em)

== 🚀 Getting Started

=== 1. Clone Repository

```bash
git clone --recurse-submodules https://github.com/ItsaMarson/univers_backend
cd univers_backend
git checkout deploy
git submodule update --init --recursive
```

#v(0.5em)

=== 2. Setup Database and Storage

==== Option A: Using Docker (Linux/Windows with Docker)

If you have Docker installed, you can start *MySQL* and *MinIO* services automatically:

*Linux/macOS:*
```bash
docker-compose up -d
```

*Windows (PowerShell/CMD):*
```bash
docker-compose up -d
```

#box(
  width: 100%,
  fill: rgb("#d1ecf1"),
  inset: 10pt,
  radius: 3pt,
  stroke: 0.5pt + rgb("#0c5460"),
)[
  _Note: This will set up MySQL and MinIO. Skip to step 3._
]

==== Option B: Manual Setup (Windows/Linux without Docker)

If not using Docker, install and configure MySQL and MinIO manually:

*MySQL Setup:*
```sql
CREATE DATABASE univers CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

*MinIO Setup:*

+ Download MinIO server:
  - *Linux/macOS:*
    ```bash
    wget https://dl.min.io/server/minio/release/linux-amd64/minio
    chmod +x minio
    ```
  - *Windows:* Download from https://dl.minio.io/server/minio/release/windows-amd64/minio.exe

+ Start MinIO server:
  - *Linux/macOS:*
    ```bash
    MINIO_ROOT_USER=minioadmin MINIO_ROOT_PASSWORD=minioadmin ./minio server ./minio-data --console-address ":9001"
    ```
  - *Windows (PowerShell):*
    ```powershell
    $env:MINIO_ROOT_USER="minioadmin"$env:MINIO_ROOT_PASSWORD="minioadmin"
    .\minio.exe server .\minio-data --console-address ":9001"
    ```
  - *Windows (Command Prompt):*
    ```cmd
    set MINIO_ROOT_USER=minioadmin
    set MINIO_ROOT_PASSWORD=minioadmin
    minio.exe server .\minio-data --console-address ":9001"
    ```

+ Access MinIO Console:
  - Open browser to `http://localhost:9001`
  - Login with username: `minioadmin`, password: `minioadmin`
  - Create a bucket named *`univers`* (or as configured in your .env)

+ Note your MinIO credentials for the *`.env`* file:
  - `MINIO_ENDPOINT=http://127.0.0.1:9000`
  - `MINIO_ACCESS_KEY=minioadmin`
  - `MINIO_SECRET_KEY=minioadmin`


=== 3. Configure Environment Variables

Create a *`.env`* file in the project root with the following variables:

```bash
#Database
MYSQL_DB_ENDPOINT=jdbc:mysql://localhost:3306/univers
MYSQL_DB_USERNAME=root
MYSQL_DB_PASSWORD=your_password

#JWT
JWT_SECRET_KEY=your_secret_key

#Email (Mailjet)
MAILJET_API_KEY=your_mailjet_api_key
MAILJET_API_SECRET=your_mailjet_api_secret
MAILJET_SENDER_EMAIL=noreply@example.com
MAILJET_TEMPLATE_ID=your_template_id
MAILJET_TEMPLATE_ID_FORGOT_PASSWORD=your_forgot_password_template_id

#MinIO Storage
MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_ACCESS_KEY=your_minio_access_key
MINIO_SECRET_KEY=your_minio_secret_key

#CORS (only needed for dev with separate frontend server)
CORS_ALLOWED_ORIGIN=http://localhost:5173
```


=== 4. Build Frontend (First Time and After Frontend Changes)

*Linux/macOS:*
```bash
chmod +x build-frontend.sh
./build-frontend.sh
```

*Windows (PowerShell):*
```powershell
.\build-frontend.ps1
```

*Windows (Command Prompt):*
```cmd
build-frontend.bat
```

_This script will install frontend dependencies, build the frontend, and copy built files to `src/main/resources/static/`._

#v(0.5em)

=== 5. Build and Run Backend

*All platforms (Linux/Windows/macOS):*
```bash
mvn spring-boot:run
```
Or using Maven wrapper:

*Linux/macOS:*
```bash
./mvnw spring-boot:run
```

*Windows:*
```cmd
mvnw.cmd spring-boot:run
```

#v(1em)
#pagebreak()

== 📂 Project API Structure

#grid(
  columns: (1fr, 1fr),
  column-gutter: 1em,
  row-gutter: 0.8em,
  box(
    width: 100%,
    fill: rgb("#e7f3ff"),
    inset: 12pt,
    radius: 4pt,
    stroke: 0.5pt + rgb("#0066cc"),
  )[
    *Backend API* \
    All endpoints under `/api` prefix:
    - Auth: `/api/auth/*`
    - Users: `/api/users/*`, `/api/admin/users/*`
    - Events: `/api/events/*`
    - Venues: `/api/venues/*`
    - Equipment: `/api/equipments/*`
    - Departments: `/api/departments/*`
  ],
  box(
    width: 100%,
    fill: rgb("#f0e7ff"),
    inset: 12pt,
    radius: 4pt,
    stroke: 0.5pt + rgb("#6610f2"),
  )[
    *Frontend* \
    Served at root `/` \
    With client-side routing support
  ],
)

#v(0.5em)

== 🔧 Development

=== Frontend Development (with Hot Reload)
During frontend development, you can run the frontend dev server separately:

```bash
cd src/main/resources/frontend
pnpm install
pnpm run dev
```

#box(
  width: 100%,
  fill: rgb("#d1ecf1"),
  inset: 10pt,
  radius: 3pt,
  stroke: 0.5pt + rgb("#0c5460"),
)[
  _Set `VITE_API_BASE_URL=http://localhost:8080/api` in your frontend `.env` file._
]

=== Backend Only
```bash
mvn spring-boot:run
```

=== Code Formatting
```bash
#Check code formatting
mvn spotless:check

#Auto-format code
mvn spotless:apply
```

#v(1em)
#pagebreak()

== 🌐 Deployment

=== Production Build

1. Build the frontend:

*Linux/macOS:*
```bash
./build-frontend.sh
```

*Windows (PowerShell):*
```powershell
.\build-frontend.ps1
```

*Windows (Command Prompt):*
```cmd
build-frontend.bat
```

2. Package the application:

*All platforms:*
```bash
mvn clean package -DskipTests
```
Or using Maven wrapper:

*Linux/macOS:*
```bash
./mvnw clean package -DskipTests
```

*Windows:*
```cmd
mvnw.cmd clean package -DskipTests
```

3. Run the JAR:

*All platforms:*
```bash
java -jar target/univers_backend-0.0.1-SNAPSHOT.jar
```

#box(
  width: 100%,
  fill: rgb("#d4edda"),
  inset: 12pt,
  radius: 4pt,
  stroke: 0.5pt + rgb("#28a745"),
)[
  *The application will serve:*
  - Frontend at `http://localhost:8080/`
  - API at `http://localhost:8080/api/`
]

=== Environment Variables for Production

#box(
  width: 100%,
  fill: rgb("#fff3cd"),
  inset: 12pt,
  radius: 4pt,
  stroke: 0.5pt + rgb("#ffc107"),
)[
  *Required environment variables for production:*
  - `MYSQL_DB_ENDPOINT`
  - `MYSQL_DB_USERNAME`
  - `MYSQL_DB_PASSWORD`
  - `JWT_SECRET_KEY` _(use a strong secret!)_
  - `MAILJET_*` variables
  - `MINIO_*` variables
  - `CORS_ALLOWED_ORIGIN` _(if needed)_
]

#v(0.5em)

== 📝 API Documentation

#box(
  width: 100%,
  fill: rgb("#e7f3ff"),
  inset: 12pt,
  radius: 4pt,
  stroke: 0.5pt + rgb("#0066cc"),
)[
  Once the application is running, access the OpenAPI documentation at: \
  *Swagger UI:* #link("http://localhost:8080/swagger-ui.html")
]

