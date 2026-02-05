
$ErrorActionPreference = "Stop"

# Helper for login
function Get-AuthToken {
    $loginBody = @{
        email    = "happyuser@example.com"
        password = "password123"
    } | ConvertTo-Json
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method "POST" -Body $loginBody -ContentType "application/json"
        return $response.token
    }
    catch {
        Write-Error "Setup failed: Could not login."
    }
}

$token = Get-AuthToken
$authHeader = @{ Authorization = "Bearer $token" }
Write-Host "Setup: Logged in."

# --- Test 1: Invalid Login ---
Write-Host "`n--- Test 1: Invalid Login ---"
$invalidLogin = @{
    email    = "happyuser@example.com"
    password = "WRONG_PASSWORD"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method POST -Body $invalidLogin -ContentType "application/json"
    Write-Error "FAILURE: Invalid Login succeeded unexpectedly."
}
catch {
    Write-Host "SUCCESS: Caught expected error: $($_.Exception.Message)"
    if ($_.Exception.Response.StatusCode -eq [System.Net.HttpStatusCode]::Unauthorized) {
        Write-Host "Verified Status Code: 401 Unauthorized"
    }
    else {
        Write-Warning "Status Code was $($_.Exception.Response.StatusCode), expected 401."
    }
}

# --- Test 2: Product Not Found ---
Write-Host "`n--- Test 2: Product Not Found ---"
$badProductOrder = @{
    items = @(
        @{
            productId   = 999999
            productName = "NonExistent Product"
            unitPrice   = 100.00
            quantity    = 1
        }
    )
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "http://localhost:8081/orders" -Method POST -Headers $authHeader -Body $badProductOrder -ContentType "application/json"
    Write-Error "FAILURE: Order with invalid product succeeded unexpectedly."
}
catch {
    Write-Host "SUCCESS: Caught expected error: $($_.Exception.Message)"
    # Doc says 400
    if ($_.Exception.Response.StatusCode -eq [System.Net.HttpStatusCode]::BadRequest) {
        Write-Host "Verified Status Code: 400 Bad Request"
    }
    else {
        Write-Warning "Status Code was $($_.Exception.Response.StatusCode), expected 400."
    }
}

# --- Test 3: Insufficient Stock ---
Write-Host "`n--- Test 3: Insufficient Stock ---"
# Utilizing Product 101 (MacBook Pro) which we saw has 50 units. Requesting 1000.
$hugeOrder = @{
    items = @(
        @{
            productId   = 101
            productName = "MacBook Pro"
            unitPrice   = 1200.00
            quantity    = 1000
        }
    )
} | ConvertTo-Json -Depth 3

try {
    Invoke-RestMethod -Uri "http://localhost:8081/orders" -Method POST -Headers $authHeader -Body $hugeOrder -ContentType "application/json"
    Write-Error "FAILURE: Insufficient stock order succeeded unexpectedly."
}
catch {
    Write-Host "SUCCESS: Caught expected error: $($_.Exception.Message)"
    # Doc says 409
    if ($_.Exception.Response.StatusCode -eq [System.Net.HttpStatusCode]::Conflict) {
        Write-Host "Verified Status Code: 409 Conflict"
    }
    else {
        Write-Warning "Status Code was $($_.Exception.Response.StatusCode), expected 409."
    }
}
