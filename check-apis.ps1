param(
    [string]$BackendBase = "http://localhost:8080/api"
)

$pass = 0; $warn = 0; $fail = 0
function Show($label, $status, $note = "") {
    if     ($status -eq "OK")   { Write-Host "  [OK]   $label $note" -ForegroundColor Green;  $script:pass++ }
    elseif ($status -eq "WARN") { Write-Host "  [WARN] $label $note" -ForegroundColor Yellow; $script:warn++ }
    else                        { Write-Host "  [FAIL] $label $note" -ForegroundColor Red;    $script:fail++ }
}

function Get-Safe($url, $hdrs = @{}, $timeoutSec = 10) {
    try {
        $r = Invoke-WebRequest -Uri $url -Headers $hdrs -UseBasicParsing -TimeoutSec $timeoutSec -MaximumRedirection 5 -ErrorAction Stop
        return @{ code = [int]$r.StatusCode; body = $r.Content }
    } catch [System.Net.WebException] {
        $resp = $_.Exception.Response
        if ($resp -ne $null) {
            $code = [int]$resp.StatusCode
            return @{ code = $code; body = $_.Exception.Message }
        }
        return @{ code = 0; body = $_.Exception.Message }
    } catch {
        return @{ code = 0; body = $_.Exception.Message }
    }
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  CityAtlas API Health Check" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ---- Backend ----
Write-Host "[ Backend ]" -ForegroundColor Cyan
$r = Get-Safe "$BackendBase/health"
if ($r.code -eq 200) { Show "Backend /health" "OK" "(HTTP $($r.code))" }
elseif ($r.code -eq 0) { Show "Backend /health" "FAIL" "(not running - start with: cd backend && ./mvnw spring-boot:run)" }
else { Show "Backend /health" "WARN" "(HTTP $($r.code) - may be starting up)" }

# ---- External APIs (direct) ----
Write-Host ""
Write-Host "[ External APIs - direct ]" -ForegroundColor Cyan

# World Bank
$r = Get-Safe "https://api.worldbank.org/v2/country/US/indicator/SP.POP.TOTL?format=json&mrv=1"
if ($r.code -eq 200) { Show "World Bank (no key)" "OK" }
else { Show "World Bank" "FAIL" "(HTTP $($r.code))" }

# REST Countries
$r = Get-Safe "https://restcountries.com/v3.1/name/france?fields=name,capital,population"
if ($r.code -eq 200) { Show "REST Countries (no key)" "OK" }
else { Show "REST Countries" "FAIL" "(HTTP $($r.code))" }

# GeoDB - HTTPS is required (HTTP returns 308)
$r = Get-Safe "https://geodb-free-service.wirefreethought.com/v1/geo/cities?limit=1&offset=0&sort=-population"
if ($r.code -eq 200)        { Show "GeoDB (no key, HTTPS)" "OK" }
elseif ($r.code -eq 308 -or $r.code -eq 301) { Show "GeoDB" "FAIL" "(HTTP $($r.code) - redirect, use HTTPS!)" }
else                        { Show "GeoDB" "WARN" "(HTTP $($r.code) - may be rate limited)" }

# OpenWeatherMap
$owmKey = ""
$secretsFile = ".\backend\src\main\resources\application-secrets.properties"
if (Test-Path $secretsFile) {
    $line = Get-Content $secretsFile | Where-Object { $_ -match "^cityatlas\.external\.openweather\.api-key=" }
    if ($line) { $owmKey = $line.Split("=",2)[1].Trim() }
}
if ($owmKey -and $owmKey -ne "your_openweather_api_key_here") {
    $r = Get-Safe "https://api.openweathermap.org/data/2.5/weather?q=London&appid=$owmKey"
    if ($r.code -eq 200)   { Show "OpenWeatherMap (key set)" "OK" }
    elseif ($r.code -eq 401) { Show "OpenWeatherMap" "FAIL" "(key invalid - check OPENWEATHER_API_KEY)" }
    else { Show "OpenWeatherMap" "WARN" "(HTTP $($r.code))" }
} else {
    Show "OpenWeatherMap" "WARN" "(key not configured - set openweather.api-key in application-secrets.properties)"
}

# Open-Meteo Air Quality (replaces OpenAQ — no key required)
$r = Get-Safe "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=51.5&longitude=-0.12&current=pm2_5,european_aqi"
if ($r.code -eq 200) { Show "Open-Meteo AQ (no key)" "OK" }
else { Show "Open-Meteo AQ" "WARN" "(HTTP $($r.code))" }

# Unsplash
$unsplashKey = ""
if (Test-Path $secretsFile) {
    $line = Get-Content $secretsFile | Where-Object { $_ -match "^cityatlas\.external\.unsplash\.access-key=" }
    if ($line) { $unsplashKey = $line.Split("=",2)[1].Trim() }
}
if ($unsplashKey -and $unsplashKey -ne "your_unsplash_access_key_here") {
    $r = Get-Safe "https://api.unsplash.com/photos/random?query=london&client_id=$unsplashKey"
    if ($r.code -eq 200)   { Show "Unsplash (key set)" "OK" }
    elseif ($r.code -eq 403) { Show "Unsplash" "FAIL" "(key invalid or rate limited)" }
    else { Show "Unsplash" "WARN" "(HTTP $($r.code))" }
} else {
    Show "Unsplash" "WARN" "(key not configured)"
}

# ---- Backend endpoints (when running) ----
Write-Host ""
Write-Host "[ Backend city endpoints ]" -ForegroundColor Cyan
$city = "london"
$r = Get-Safe "$BackendBase/cities/$city" @{} 45
if ($r.code -eq 200) {
    $hasNull = $r.body -match '"population":null|"gdp":null|"aqi":null'
    if ($hasNull) { Show "GET /cities/$city" "WARN" "(200 OK but some fields are null)" }
    else          { Show "GET /cities/$city" "OK" }
} elseif ($r.code -eq 0) {
    Show "GET /cities/$city" "WARN" "(backend not running)"
} else {
    Show "GET /cities/$city" "FAIL" "(HTTP $($r.code))"
}

$r = Get-Safe "$BackendBase/cities/$city/analytics"
if ($r.code -eq 200) {
    $hasNull = $r.body -match '"population":null|"gdp":null'
    if ($hasNull) { Show "GET /cities/$city/analytics" "WARN" "(200 OK but some fields are null)" }
    else          { Show "GET /cities/$city/analytics" "OK" }
} elseif ($r.code -eq 0) {
    Show "GET /cities/$city/analytics" "WARN" "(backend not running)"
} else {
    Show "GET /cities/$city/analytics" "FAIL" "(HTTP $($r.code))"
}

# ---- Summary ----
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
$total = $pass + $warn + $fail
Write-Host "  PASS: $pass  WARN: $warn  FAIL: $fail  (of $total checks)" -ForegroundColor $(if ($fail -gt 0) { "Red" } elseif ($warn -gt 0) { "Yellow" } else { "Green" })
if ($fail -eq 0 -and $warn -eq 0) { Write-Host "  All systems go!" -ForegroundColor Green }
elseif ($fail -eq 0) { Write-Host "  No critical failures. Review warnings above." -ForegroundColor Yellow }
else { Write-Host "  $fail critical issue(s) need attention." -ForegroundColor Red }
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
