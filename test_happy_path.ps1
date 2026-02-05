
$ErrorActionPreference = "Stop"

function Rest-Call {
    param (
        [string]$Url,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [string]$Body = $null
    )
    Write-Host "Calling $Method $Url"
    try {
        if ($Body) {
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Headers $Headers -Body $Body -ContentType "application/json"
        }
        else {
            $response = Invoke-RestMethod -Uri $Url -Method $Method -Headers $Headers -ContentType "application/json"
        }
        return $response
    }
    catch {
        Write-Host "Error calling $Url"
        Write-Host $_.Exception.Message
        if ($_.Exception.Response) {
            # Read the error stream
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "Response Body: $responseBody"
        }
        return $null
    }
}

# 1. Register
$registerBody = @{
    email    = "happyuser@example.com"
    password = "password123"
    name     = "Happy User"
    role     = "USER"
} | ConvertTo-Json

Write-Host "`n--- Step 1: Register ---"
Rest-Call -Url "http://localhost:8080/auth/register" -Method "POST" -Body $registerBody

# 2. Login
$loginBody = @{
    email    = "happyuser@example.com"
    password = "password123"
} | ConvertTo-Json

Write-Host "`n--- Step 2: Login ---"
$loginResponse = Rest-Call -Url "http://localhost:8080/auth/login" -Method "POST" -Body $loginBody

if (-not $loginResponse) {
    Write-Error "Login failed. Exiting."
}

$token = $loginResponse.token
Write-Host "Token received: $token"
$authHeader = @{ Authorization = "Bearer $token" }

# 3. Get Products
Write-Host "`n--- Step 3: Get Products ---"
$products = Rest-Call -Url "http://localhost:8082/inventory/products" -Method "GET" -Headers $authHeader

if (-not $products -or $products.Count -eq 0) {
    Write-Warning "No products found. Cannot proceed with order creation using existing products."
    # Attempt to create a product if we can (likely need ADMIN) - Skipping for now, assuming seed data or empty list handling
}
else {
    Write-Host "Found $($products.Count) products."
    $product = $products[0]
    Write-Host "Selected Product: $($product | ConvertTo-Json -Depth 1)"

    # 4. Create Order
    Write-Host "`n--- Step 4: Create Order ---"
    $orderItem = @{
        productId   = $product.id
        productName = $product.productName
        unitPrice   = $product.price
        quantity    = 1
    }
    $orderBody = @{
        items = @($orderItem)
    } | ConvertTo-Json -Depth 3

    $orderResponse = Rest-Call -Url "http://localhost:8081/orders" -Method "POST" -Headers $authHeader -Body $orderBody

    if ($orderResponse) {
        $orderId = $orderResponse.id
        Write-Host "Order Created with ID: $orderId"
        Write-Host "Status: $($orderResponse.status)"

        # 5. Verify Order
        Write-Host "`n--- Step 5: Verify Order ---"
        $getOrderResponse = Rest-Call -Url "http://localhost:8081/orders/$orderId" -Method "GET" -Headers $authHeader
        Write-Host "Retrieved Order: $($getOrderResponse | ConvertTo-Json -Depth 3)"
    }
}
