# PostgreSQL Local Setup Guide for CityAtlas

## Current Status
✅ PostgreSQL 16 is installed and running
⚠️ Running on **non-standard port: 5434** (instead of 5432)
⚠️ Password authentication required

## Database Setup Steps

### 1. Set PostgreSQL Password Environment Variable

Open PowerShell as **Administrator** and run:

```powershell
# Option A: Set for current session only
$env:DB_PASSWORD = "your_postgres_password"

# Option B: Set permanently (recommended for development)
[System.Environment]::SetEnvironmentVariable('DB_PASSWORD', 'your_postgres_password', 'User')
```

Replace `your_postgres_password` with your actual PostgreSQL password.

### 2. Create CityAtlas Database

Run the following command:

```powershell
# Using psql command line
$env:PGPASSWORD = 'your_postgres_password'
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -h localhost -p 5434 -U postgres -d postgres -c "CREATE DATABASE cityatlas;"
```

**OR** use pgAdmin GUI:
1. Open pgAdmin
2. Right-click on "Databases"
3. Select "Create" → "Database..."
4. Name: `cityatlas`
5. Click "Save"

### 3. Verify Database Creation

```powershell
$env:PGPASSWORD = 'your_postgres_password'
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -h localhost -p 5434 -U postgres -d postgres -c "\l"
```

You should see `cityatlas` in the list.

### 4. Start Backend

Once the database is created, start the backend:

```powershell
cd backend
./mvnw spring-boot:run
```

## Configuration Applied

The `application.properties` has been configured with:

- **Database URL:** `jdbc:postgresql://localhost:5434/cityatlas`
- **Username:** `postgres` (reads from `$env:DB_USERNAME` if set)
- **Password:** Reads from `$env:DB_PASSWORD` (defaults to "password" for local dev)
- **JPA DDL Mode:** `update` (auto-creates tables)
- **SQL Logging:** `false` (cleaner console output)

## Environment Variables (Security)

For production or if you want to avoid hardcoding:

```powershell
# Windows (PowerShell - User level)
[System.Environment]::SetEnvironmentVariable('DB_URL', 'jdbc:postgresql://localhost:5434/cityatlas', 'User')
[System.Environment]::SetEnvironmentVariable('DB_USERNAME', 'postgres', 'User')
[System.Environment]::SetEnvironmentVariable('DB_PASSWORD', 'your_password', 'User')

# Restart your terminal after setting environment variables
```

## Troubleshooting

### Port Conflict
Your PostgreSQL is running on port **5434** (not standard 5432). This has been configured in `application.properties`.

If you need to change the port:
1. Edit `C:\Program Files\PostgreSQL\16\data\postgresql.conf`
2. Change `port = 5434` to `port = 5432`
3. Restart PostgreSQL service:
   ```powershell
   Restart-Service -Name "postgresql-x64-16"
   ```
4. Update `application.properties` to use port 5432

### Password Issues
- PostgreSQL requires authentication
- Default username: `postgres`
- Password: Set during PostgreSQL installation
- If you forgot: Use pgAdmin to reset or reinstall PostgreSQL

### Connection Test
```powershell
$env:PGPASSWORD = 'your_password'
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -h localhost -p 5434 -U postgres -d cityatlas -c "SELECT now();"
```

## What Happens When Backend Starts

With `spring.jpa.hibernate.ddl-auto=update`:

1. ✅ Hibernate will automatically create all tables
2. ✅ Tables created based on JPA entities:
   - `city`
   - `metrics`
   - `city_section`
   - `ai_summary`
   - `analytics_event`
3. ✅ Indexes will be created automatically
4. ✅ Relationships will be established

## Next Steps

1. Set your PostgreSQL password as environment variable
2. Create the `cityatlas` database
3. Run `./mvnw spring-boot:run` from the backend directory
4. Check logs for "Started BackendApplication"
5. Test endpoints: `http://localhost:8080/api/health`

## Quick Start Script

Save this as `setup-db.ps1`:

```powershell
# Quick database setup script
$DB_PASS = Read-Host -Prompt "Enter your PostgreSQL password" -AsSecureString
$env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($DB_PASS)
)

Write-Host "Creating cityatlas database..." -ForegroundColor Cyan
& "C:\Program Files\PostgreSQL\16\bin\psql.exe" -h localhost -p 5434 -U postgres -d postgres -c "CREATE DATABASE cityatlas;"

Write-Host "Database setup complete!" -ForegroundColor Green
Write-Host "Now run: cd backend && ./mvnw spring-boot:run" -ForegroundColor Yellow
```

Run with: `.\setup-db.ps1`
