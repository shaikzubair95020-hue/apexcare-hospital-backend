$res = curl.exe -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{\"email\":\"admin@apexcare.com\",\"password\":\"Admin@123\"}'
Write-Host "Raw login response:" $res

$auth = $res | ConvertFrom-Json
$token = $auth.token
Write-Host "Extracted token:" $token

$dash = curl.exe -s -X GET http://localhost:8080/api/dashboard/admin -H "Authorization: Bearer $token"
Write-Host "Admin Dashboard response:" $dash
