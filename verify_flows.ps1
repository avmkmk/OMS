# verify_flows.ps1
# Script to verify OMS flows and report on failure behavior

$IAM_URL = "http://localhost:8080"
$ORDER_URL = "http://localhost:8081"
$INVENTORY_URL = "http://localhost:8082"
$PAYMENT_URL = "http://localhost:8083"

function Write-Header($msg) {
    Write-Host "`n====================================================" -ForegroundColor Cyan
    Write-Host " $msg" -ForegroundColor Cyan
    Write-Host "====================================================`n" -ForegroundColor Cyan
}

function Invoke-ApiRequest {
    param($Method, $Uri, $Headers, $Body)
    try {
        if ($Body) {
            $resp = Invoke-WebRequest -Method $Method -Uri $Uri -Headers $Headers -Body $Body -ContentType "application/json" -ErrorAction Stop -UseBasicParsing
        }
        else {
            $resp = Invoke-WebRequest -Method $Method -Uri $Uri -Headers $Headers -ErrorAction Stop -UseBasicParsing
        }
        return $resp
    }
    catch {
        return $_.Exception.Response
    }
}

Write-Header "STEP 1: USER REGISTRATION & LOGIN"

$regBody = @{
    email    = "tester@example.com"
    password = "password123"
    name     = "Tester"
    role     = "USER"
} | ConvertTo-Json

Write-Host "Registering user..."
$regResp = Invoke-ApiRequest -Method Post -Uri "$IAM_URL/auth/register" -Body $regBody
Write-Host "Status: $($regResp.StatusCode)"
Write-Host "Body: $($regResp.Content)"

$loginBody = @{
    email    = "tester@example.com"
    password = "password123"
} | ConvertTo-Json

Write-Host "`nLogging in..."
$loginResp = Invoke-ApiRequest -Method Post -Uri "$IAM_URL/auth/login" -Body $loginBody
$token = ($loginResp.Content | ConvertFrom-Json).token
Write-Host "Token received: $($token.Substring(0, 10))..."

$authHeader = @{ Authorization = "Bearer $token" }

Write-Header "STEP 2: PREPARING INVENTORY"
# Add a product
$invBody = @{
    productName = "Integration Test Product"
    price       = 99.99
    quantity    = 10
    status      = "ACTIVE"
} | ConvertTo-Json

$invResp = Invoke-ApiRequest -Method Post -Uri "$INVENTORY_URL/inventory/products" -Headers $authHeader -Body $invBody
$productId = ($invResp.Content | ConvertFrom-Json).productId
Write-Host "Created Product ID: $productId"

Write-Header "STEP 3: HAPPY FLOW (Order Creation)"
$orderBody = @{
    items = @(
        @{ productId = $productId; quantity = 2 }
    )
} | ConvertTo-Json

$orderResp = Invoke-ApiRequest -Method Post -Uri "$ORDER_URL/orders" -Headers $authHeader -Body $orderBody
Write-Host "Status: $($orderResp.StatusCode)"
Write-Host "Body: $($orderResp.Content)"

Write-Header "STEP 4: FAILURE CASE - INSUFFICIENT STOCK"
$failOrderBody = @{
    items = @(
        @{ productId = $productId; quantity = 100 }
    )
} | ConvertTo-Json

$failResp = Invoke-ApiRequest -Method Post -Uri "$ORDER_URL/orders" -Headers $authHeader -Body $failOrderBody
Write-Host "Status: $($failResp.StatusCode)"
Write-Host "Body: $($failResp.Content)"

Write-Header "STEP 5: FAILURE CASE - INVALID PRODUCT"
$invalidOrderBody = @{
    items = @(
        @{ productId = 9999; quantity = 1 }
    )
} | ConvertTo-Json

$invalidResp = Invoke-ApiRequest -Method Post -Uri "$ORDER_URL/orders" -Headers $authHeader -Body $invalidOrderBody

Write-Host "Status: $($invalidResp.StatusCode)"
Write-Host "Body: $($invalidResp.Content)"

Write-Header "SUMMARY OF CURRENT FAILURE BEHAVIOR"
Write-Host "1. Insufficient Stock: $($failResp.StatusCode) - $($failResp.Content)"
Write-Host "2. Invalid Product:   $($invalidResp.StatusCode) - $($invalidResp.Content)"
