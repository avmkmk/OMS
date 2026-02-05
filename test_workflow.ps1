# test_workflow.ps1

$BaseUrlIam = "http://localhost:8080"
$BaseUrlOrder = "http://localhost:8081"
$BaseUrlInventory = "http://localhost:8082"
$BaseUrlPayment = "http://localhost:8083"

Write-Host "Waiting 15 seconds for services to stabilize..."
Start-Sleep -Seconds 15

# Try to clear previous data if needed (optional, just to be clean)
# But here we just use a new user email each time or depend on unique email failure which we catch

$Email = "testuser_$(Get-Random)@example.com"

Write-Host "--- 1. Registering and Logging in ---" -ForegroundColor Cyan
$RegBody = @{
    email    = $Email
    password = "password123"
} | ConvertTo-Json

try {
    Invoke-RestMethod -Uri "$BaseUrlIam/auth/register" -Method Post -Body $RegBody -ContentType "application/json" | Out-Null
}
catch {
    Write-Host "Registration failed (likely user exists): $($_.Exception.Message)" -ForegroundColor Gray
}

$LoginResponse = Invoke-RestMethod -Uri "$BaseUrlIam/auth/login" -Method Post -Body $RegBody -ContentType "application/json"
$Token = $LoginResponse.token
Write-Host "Logged in. Token acquired." -ForegroundColor Green

$Headers = @{
    Authorization  = "Bearer $Token"
    "Content-Type" = "application/json"
}

Write-Host "`n--- 2. Adding Product to Inventory ---" -ForegroundColor Cyan
$ProductBody = @{
    productName = "Laptop"
    price       = 1200
    quantity    = 10
} | ConvertTo-Json

$ProductResponse = Invoke-RestMethod -Uri "$BaseUrlInventory/inventory/products" -Method Post -Body $ProductBody -Headers $Headers
$ProductId = $ProductResponse.id
Write-Host "Product added. ID: $ProductId" -ForegroundColor Green

Write-Host "`n--- 3. Creating Order ---" -ForegroundColor Cyan
$OrderBody = @{
    items = @(
        @{
            productId   = $ProductId
            productName = "Laptop" 
            unitPrice   = 1200
            quantity    = 1
        }
    )
} | ConvertTo-Json

$OrderResponse = Invoke-RestMethod -Uri "$BaseUrlOrder/orders" -Method Post -Body $OrderBody -Headers $Headers
$OrderId = $OrderResponse.id
Write-Host "Order created. ID: $OrderId. Initial Status: $($OrderResponse.status)" -ForegroundColor Green

Write-Host "`n--- 4. Waiting for Inventory Reservation (Async) ---" -ForegroundColor Cyan
Start-Sleep -Seconds 10
$OrderCheck = Invoke-RestMethod -Uri "$BaseUrlOrder/orders/$OrderId" -Method Get -Headers $Headers
Write-Host "Current Order Status: $($OrderCheck.status)" -ForegroundColor Yellow

if ($OrderCheck.status -ne "AWAITING_PAYMENT") {
    Write-Host "Warning: Order is not in AWAITING_PAYMENT status. It might still be processing or failed." -ForegroundColor Red
}

Write-Host "`n--- 5. Triggering Manual Payment ---" -ForegroundColor Cyan
$PaymentBody = @{
    orderId  = $OrderId
    amount   = 1200
    currency = "USD"
} | ConvertTo-Json

$PaymentResponse = Invoke-RestMethod -Uri "$BaseUrlPayment/payments" -Method Post -Body $PaymentBody -Headers $Headers
Write-Host "Payment processed. Status: $($PaymentResponse.status)" -ForegroundColor Green

Write-Host "`n--- 6. Waiting for Order Completion (Async) ---" -ForegroundColor Cyan
Start-Sleep -Seconds 10
$OrderFinal = Invoke-RestMethod -Uri "$BaseUrlOrder/orders/$OrderId" -Method Get -Headers $Headers
Write-Host "Final Order Status: $($OrderFinal.status)" -ForegroundColor Green

if ($OrderFinal.status -eq "COMPLETED") {
    Write-Host "SUCCESS: End-to-end async workflow verified!" -ForegroundColor Cyan
}
else {
    Write-Host "FAILURE: Order did not reach COMPLETED status." -ForegroundColor Red
}

Write-Host "`n--- 7. Checking Notification Service Logs ---" -ForegroundColor Cyan
docker logs notification_service --tail 20
