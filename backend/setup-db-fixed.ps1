# Quick database setup script for CityAtlas
# Run this after PostgreSQL is installed

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  CityAtlas - PostgreSQL Database Setup" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Get PostgreSQL password securely
$securePassword = Read-Host "Enter PostgreSQL password for 'postgres' user" -AsSecureString
$PGPASSWORD = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
)

# Set environment variable for psql
$env:PGPASSWORD = $PGPASSWORD

Write-Host ""
Write-Host "Testing PostgreSQL connection..." -ForegroundColor Yellow

# Test connection
$testResult = psql -h localhost -p 5434 -U postgres -d postgres -c "SELECT 1;" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Cannot connect to PostgreSQL!" -ForegroundColor Red
    Write-Host "Error: $testResult" -ForegroundColor Red
    Write-Host ""
    Write-Host "Please verify:" -ForegroundColor Yellow
    Write-Host "  1. PostgreSQL service is running" -ForegroundColor Yellow
    Write-Host "  2. Port 5434 is correct" -ForegroundColor Yellow
    Write-Host "  3. Password is correct" -ForegroundColor Yellow
    exit 1
}

Write-Host "[OK] Connected to PostgreSQL successfully!" -ForegroundColor Green
Write-Host ""

# Check if database exists
Write-Host "Checking if 'cityatlas' database exists..." -ForegroundColor Yellow
$dbCheck = psql -h localhost -p 5434 -U postgres -d postgres -t -c "SELECT 1 FROM pg_database WHERE datname='cityatlas';" 2>&1

if ($dbCheck -match "1") {
    Write-Host "[OK] Database 'cityatlas' already exists!" -ForegroundColor Green
} else {
    Write-Host "Creating 'cityatlas' database..." -ForegroundColor Yellow
    $createResult = psql -h localhost -p 5434 -U postgres -d postgres -c "CREATE DATABASE cityatlas;" 2>&1
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Failed to create database!" -ForegroundColor Red
        Write-Host "Error: $createResult" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "[OK] Database 'cityatlas' created successfully!" -ForegroundColor Green
}

Write-Host ""
Write-Host "Verifying database access..." -ForegroundColor Yellow
$verifyResult = psql -h localhost -p 5434 -U postgres -d cityatlas -c "SELECT current_database(), current_user, version();" 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Cannot access cityatlas database!" -ForegroundColor Red
    Write-Host "Error: $verifyResult" -ForegroundColor Red
    exit 1
} else {
    Write-Host "[OK] Database verification successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host "  Setup Complete!" -ForegroundColor Green
    Write-Host "==================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Database: cityatlas" -ForegroundColor White
    Write-Host "Host: localhost" -ForegroundColor White
    Write-Host "Port: 5434" -ForegroundColor White
    Write-Host "User: postgres" -ForegroundColor White
    Write-Host ""
    Write-Host "You can now start the backend with:" -ForegroundColor Yellow
    Write-Host "  ./mvnw spring-boot:run" -ForegroundColor Cyan
    Write-Host ""
}

# Clear password from environment
$env:PGPASSWORD = $null
