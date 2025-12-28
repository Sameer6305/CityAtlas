# ============================================
# PostgreSQL Password Reset Script
# ============================================
# This script resets the postgres user password by temporarily
# modifying pg_hba.conf to allow trust authentication
# ============================================

Write-Host "=== PostgreSQL Password Reset Script ===" -ForegroundColor Cyan
Write-Host ""

# PostgreSQL paths
$pgPath = "C:\Program Files\PostgreSQL\16"
$pgDataPath = "$pgPath\data"
$pgHbaConf = "$pgDataPath\pg_hba.conf"
$pgHbaBackup = "$pgDataPath\pg_hba.conf.backup"

# Check if running as Administrator
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "ERROR: This script must be run as Administrator!" -ForegroundColor Red
    Write-Host "Right-click PowerShell and select 'Run as Administrator', then run this script again." -ForegroundColor Yellow
    exit 1
}

Write-Host "Step 1: Backing up pg_hba.conf..." -ForegroundColor Yellow
Copy-Item $pgHbaConf $pgHbaBackup -Force
Write-Host "✓ Backup created: $pgHbaBackup" -ForegroundColor Green
Write-Host ""

Write-Host "Step 2: Modifying pg_hba.conf to allow trust authentication..." -ForegroundColor Yellow
# Read current content
$content = Get-Content $pgHbaConf

# Create new content with trust authentication for localhost
$newContent = @"
# TYPE  DATABASE        USER            ADDRESS                 METHOD
# Temporary trust authentication for password reset
host    all             all             127.0.0.1/32            trust
host    all             all             ::1/128                 trust

# Original rules (commented out temporarily)
"@

foreach ($line in $content) {
    $newContent += "`n# $line"
}

# Write new content
Set-Content -Path $pgHbaConf -Value $newContent
Write-Host "✓ pg_hba.conf modified" -ForegroundColor Green
Write-Host ""

Write-Host "Step 3: Restarting PostgreSQL service..." -ForegroundColor Yellow
Restart-Service postgresql-x64-16
Start-Sleep -Seconds 5
Write-Host "✓ PostgreSQL restarted" -ForegroundColor Green
Write-Host ""

Write-Host "Step 4: Resetting postgres user password to 'pranav'..." -ForegroundColor Yellow
$env:PGPASSWORD = ""  # No password needed with trust authentication
psql -U postgres -h localhost -p 5434 -d postgres -c "ALTER USER postgres WITH PASSWORD 'pranav';"

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Password reset successful!" -ForegroundColor Green
} else {
    Write-Host "✗ Password reset failed!" -ForegroundColor Red
    Write-Host "Restoring original pg_hba.conf..." -ForegroundColor Yellow
    Copy-Item $pgHbaBackup $pgHbaConf -Force
    Restart-Service postgresql-x64-16
    exit 1
}
Write-Host ""

Write-Host "Step 5: Restoring original pg_hba.conf..." -ForegroundColor Yellow
Copy-Item $pgHbaBackup $pgHbaConf -Force
Write-Host "✓ Original configuration restored" -ForegroundColor Green
Write-Host ""

Write-Host "Step 6: Restarting PostgreSQL service with original settings..." -ForegroundColor Yellow
Restart-Service postgresql-x64-16
Start-Sleep -Seconds 5
Write-Host "✓ PostgreSQL restarted" -ForegroundColor Green
Write-Host ""

Write-Host "Step 7: Testing new password..." -ForegroundColor Yellow
$env:PGPASSWORD = "pranav"
psql -U postgres -h localhost -p 5434 -d cityatlas -c "SELECT 'Password reset successful! postgres user can now connect.' as status;"

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "==================================" -ForegroundColor Green
    Write-Host "  PASSWORD RESET SUCCESSFUL!" -ForegroundColor Green
    Write-Host "==================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "New credentials:" -ForegroundColor Cyan
    Write-Host "  Username: postgres" -ForegroundColor White
    Write-Host "  Password: pranav" -ForegroundColor White
    Write-Host "  Database: cityatlas" -ForegroundColor White
    Write-Host "  Port: 5434" -ForegroundColor White
    Write-Host ""
    Write-Host "You can now start the backend with:" -ForegroundColor Cyan
    Write-Host "  cd backend" -ForegroundColor White
    Write-Host "  ./mvnw spring-boot:run" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "✗ Password test failed!" -ForegroundColor Red
    Write-Host "Please check PostgreSQL logs for errors." -ForegroundColor Yellow
}
