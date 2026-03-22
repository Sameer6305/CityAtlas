Param(
    [string]$BackendBase = "http://localhost:8080/api",
    [string]$FrontendBase = "http://localhost:3000"
)

$ErrorActionPreference = "Stop"

function Assert-StatusCode {
    param(
        [int]$Actual,
        [int]$Expected,
        [string]$Step
    )
    if ($Actual -ne $Expected) {
        throw "[$Step] Expected HTTP $Expected but got $Actual"
    }
}

Write-Output "[SMOKE] Starting CityAtlas demo smoke flow"

$demoEmail = $env:CITYATLAS_DEMO_EMAIL
$demoPassword = $env:CITYATLAS_DEMO_PASSWORD

if ([string]::IsNullOrWhiteSpace($demoEmail) -or [string]::IsNullOrWhiteSpace($demoPassword)) {
    throw "[Auth login] Missing CITYATLAS_DEMO_EMAIL or CITYATLAS_DEMO_PASSWORD in environment"
}

# 1) Frontend health
$frontendResponse = Invoke-WebRequest -UseBasicParsing -Uri $FrontendBase -Method Get
Assert-StatusCode -Actual $frontendResponse.StatusCode -Expected 200 -Step "Frontend root"
Write-Output "[PASS] Frontend root is reachable"

# 2) Backend actuator health
$healthResponse = Invoke-WebRequest -UseBasicParsing -Uri "$BackendBase/actuator/health" -Method Get
Assert-StatusCode -Actual $healthResponse.StatusCode -Expected 200 -Step "Backend health"
Write-Output "[PASS] Backend health endpoint is UP"

# 3) Login
$loginCandidates = @(
    @{ email = $demoEmail; password = $demoPassword }
)

$loginResponse = $null
foreach ($candidate in $loginCandidates) {
    try {
        $loginBody = $candidate | ConvertTo-Json
        $attempt = Invoke-RestMethod -Uri "$BackendBase/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
        if ($attempt.token) {
            $loginResponse = $attempt
            break
        }
    } catch {
        # Try the next credential candidate
    }
}

if ($null -eq $loginResponse -or -not $loginResponse.token) {
    throw "[Auth login] Unable to authenticate with configured demo credentials"
}
Write-Output "[PASS] Auth login returned a token"

# 4) Protected city endpoint
$headers = @{ Authorization = "Bearer $($loginResponse.token)" }
$city = Invoke-RestMethod -Uri "$BackendBase/cities/london" -Method Get -Headers $headers
if (-not $city.name) {
    throw "[City endpoint] City name missing from response"
}

Write-Output "[PASS] Protected city endpoint returned data"
Write-Output ("[INFO] City=" + $city.name)
Write-Output ("[INFO] WeatherTemp=" + $city.weatherTemp)
Write-Output ("[INFO] AQI=" + $city.airQualityIndex)
Write-Output ("[INFO] BannerImagePresent=" + [bool]$city.bannerImageUrl)

Write-Output "[SMOKE] CityAtlas smoke flow completed successfully"
