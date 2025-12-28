# Quick database setup script for CityAtlas
# Run this after PostgreSQL is installed

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  CityAtlas - PostgreSQL Database Setup" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Get PostgreSQL password securely
$DB_PASS = Read-Host -Prompt "Enter your PostgreSQL password" -AsSecureString
$PlainPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($DB_PASS)
)

$env:PGPASSWORD = $PlainPassword
$psqlPath = "C:\Program Files\PostgreSQL\16\bin\psql.exe"

# Check if psql exists
if (-not (Test-Path $psqlPath)) {
    Write-Host "ERROR: PostgreSQL not found at: $psqlPath" -ForegroundColor Red
    Write-Host "Please install PostgreSQL 16 or update the path in this script." -ForegroundColor Yellow
    exit 1
}

Write-Host "Step 1: Testing PostgreSQL connection..." -ForegroundColor Yellow
$testResult = & $psqlPath -h localhost -p 5434 -U postgres -d postgres -c "SELECT 1;" 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Could not connect to PostgreSQL" -ForegroundColor Red
    Write-Host "Please check:" -ForegroundColor Yellow
    Write-Host "  1. PostgreSQL service is running (Check Services.msc)" -ForegroundColor Yellow
    Write-Host "  2. Password is correct" -ForegroundColor Yellow
    Write-Host "  3. Port is 5434 (check postgresql.conf)" -ForegroundColor Yellow
    exit 1
}

Write-Host "✓ Connection successful!" -ForegroundColor Green
Write-Host ""

Write-Host "Step 2: Checking if cityatlas database exists..." -ForegroundColor Yellow
$dbCheck = & $psqlPath -h localhost -p 5434 -U postgres -d postgres -t -c "SELECT 1 FROM pg_database WHERE datname='cityatlas';" 2>&1

if ($dbCheck -match "1") {
    Write-Host "✓ Database 'cityatlas' already exists!" -ForegroundColor Green
} else {
    Write-Host "Creating 'cityatlas' database..." -ForegroundColor Yellow
    $createResult = & $psqlPath -h localhost -p 5434 -U postgres -d postgres -c "CREATE DATABASE cityatlas;" 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Database 'cityatlas' created successfully!" -ForegroundColor Green
    } else {
        Write-Host "ERROR: Failed to create database" -ForegroundColor Red
        Write-Host $createResult
        exit 1
    }
}

Write-Host ""
Write-Host "Step 3: Verifying database..." -ForegroundColor Yellow
$verifyResult = & $psqlPath -h localhost -p 5434 -U postgres -d cityatlas -c "SELECT current_database(), current_user, version();" 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Database verification successful!" -ForegroundColor Green
} else {
    Write-Host "WARNING: Could not verify database" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==================================================" -ForegroundColor Green
Write-Host "  PostgreSQL Setup Complete!" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Configuration Summary:" -ForegroundColor Cyan
Write-Host "  • Database: cityatlas" -ForegroundColor White
Write-Host "  • Host: localhost" -ForegroundColor White
Write-Host "  • Port: 5434" -ForegroundColor White
Write-Host "  • User: postgres" -ForegroundColor White
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Cyan
Write-Host "  1. Set DB_PASSWORD environment variable (optional):" -ForegroundColor White
Write-Host "     [System.Environment]::SetEnvironmentVariable('DB_PASSWORD', '$PlainPassword', 'User')" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. Start the backend:" -ForegroundColor White
Write-Host "     cd backend" -ForegroundColor Gray
Write-Host "     ./mvnw spring-boot:run" -ForegroundColor Gray
Write-Host ""
Write-Host "  3. Hibernate will automatically create all tables on first run!" -ForegroundColor Yellow
Write-Host ""

# Clear password from environment
$env:PGPASSWORD = $null
