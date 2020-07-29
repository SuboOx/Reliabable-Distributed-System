# Powershell script for initializing the whole system
Write-Host "Starting System"
Set-Location ./out/production/ReliableDS
# Launch GFD
Write-Host "Launching GFD..."
Start-Process java -ArgumentList 'GFD', '8891'
Write-Host "GFD Launched"

# Launch LFDs
Write-Host "Launching LFD 0..."
Start-Process java -ArgumentList 'LFD', '0', '127.0.0.1', '0', '2000', '1000'
Write-Host "LFD 0 Launched"

Write-Host "Launching LFD 1..."
Start-Process java -ArgumentList 'LFD', '1', '127.0.0.1', '1', '2000', '1000'
Write-Host "LFD 1 Launched"

Write-Host "Launching LFD 2..."
Start-Process java -ArgumentList 'LFD', '2', '127.0.0.1', '2', '2000', '1000'
Write-Host "LFD 2 Launched"

# Launch servers
Write-Host "Launching Server 0..."
Start-Process java -ArgumentList 'Server', '0', '8888'
Write-Host "Server 0 Launched"

Write-Host "Launching Server 1..."
Start-Process java -ArgumentList 'Server', '1', '8889'
Write-Host "Server 1 Launched"

Write-Host "Launching Server 2..."
Start-Process java -ArgumentList 'Server', '2', '8890'
Write-Host "Server 2 Launched"

# Launch clients
Write-Host "Launching Client 0..."
Start-Process java -ArgumentList 'Client', '0', '127.0.0.1', '0', '1', '2'
Write-Host "Client 0 Launched"

Write-Host "Launching Client 0..."
Start-Process java -ArgumentList 'Client', '1', '127.0.0.1', '0', '1', '2'
Write-Host "Client 0 Launched"

Write-Host "Launching Client 0..."
Start-Process java -ArgumentList 'Client', '2', '127.0.0.1', '0', '1', '2'
Write-Host "Client 0 Launched"

Set-Location ../../..
#>