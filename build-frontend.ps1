# Windows PowerShell Build Script
# More robust than batch file, with better error handling

Write-Host "Building frontend..." -ForegroundColor Green

# Navigate to frontend directory
Push-Location src\main\resources\frontend

try {
    # Detect package manager
    $pkgMgr = $null
    
    if (Get-Command pnpm -ErrorAction SilentlyContinue) {
        $pkgMgr = "pnpm"
    }
    elseif (Get-Command npm -ErrorAction SilentlyContinue) {
        $pkgMgr = "npm"
    }
    else {
        Write-Host "Error: Neither pnpm nor npm found. Please install Node.js and a package manager." -ForegroundColor Red
        exit 1
    }
    
    Write-Host "Using package manager: $pkgMgr" -ForegroundColor Cyan
    
    # Install dependencies if node_modules doesn't exist
    if (-not (Test-Path "node_modules")) {
        Write-Host "Installing frontend dependencies..." -ForegroundColor Yellow
        & $pkgMgr install
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to install dependencies"
        }
    }
    
    # Build the frontend
    Write-Host "Running frontend build..." -ForegroundColor Yellow
    & $pkgMgr run build
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed"
    }
    
    # Create target static directory if it doesn't exist
    $staticDir = "..\static"
    if (-not (Test-Path $staticDir)) {
        New-Item -ItemType Directory -Path $staticDir | Out-Null
    }
    
    # Remove old static files
    Write-Host "Cleaning old static files..." -ForegroundColor Yellow
    Get-ChildItem $staticDir -Recurse | Remove-Item -Force -Recurse
    
    # Copy built files to Spring Boot static resources
    Write-Host "Copying built files to Spring Boot static resources..." -ForegroundColor Yellow
    Copy-Item -Path "dist\*" -Destination $staticDir -Recurse -Force
    
    Write-Host "`nFrontend build complete! Files copied to src\main\resources\static\" -ForegroundColor Green
}
catch {
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
}
finally {
    # Return to original directory
    Pop-Location
}
