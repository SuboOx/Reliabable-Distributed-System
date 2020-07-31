# Powershell script for initializing the whole system
Write-Host "Starting System"
Set-Location ./out/production/ReliableDS

# Launch replica manager
Write-Host "Launching RM..."
Start-Process java -ArgumentList 'ReplicaManager', 'passive'
Write-Host "RM Launched"

Start-Sleep -Seconds 3

# Launch GFD
Write-Host "Launching GFD..."
Start-Process java -ArgumentList 'GFD', '8891'
Write-Host "GFD Launched"

Start-Sleep -Seconds 3

# Launch LFDs
Write-Host "Launching LFD 0..."
Start-Process java -ArgumentList 'LFD', '0', '127.0.0.1', '0', '5000', '2000'
Write-Host "LFD 0 Launched"

Start-Sleep -Seconds 3

Write-Host "Launching LFD 1..."
Start-Process java -ArgumentList 'LFD', '1', '127.0.0.1', '1', '5000', '2000'
Write-Host "LFD 1 Launched"

Start-Sleep -Seconds 3

Write-Host "Launching LFD 2..."
Start-Process java -ArgumentList 'LFD', '2', '127.0.0.1', '2', '5000', '2000'
Write-Host "LFD 2 Launched"

Start-Sleep -Seconds 3

# Launch servers
Write-Host "Launching Server 0..."
Start-Process java -ArgumentList 'Server', '0', '8888', 'primary', '10000'
Write-Host "Server 0 Launched"

Start-Sleep -Seconds 3

Write-Host "Launching Server 1..."
Start-Process java -ArgumentList 'Server', '1', '8889', 'backup', '10000'
Write-Host "Server 1 Launched"

Start-Sleep -Seconds 3

Write-Host "Launching Server 2..."
Start-Process java -ArgumentList 'Server', '2', '8890', 'backup', '10000'
Write-Host "Server 2 Launched"

Start-Sleep -Seconds 3

# Launch clients
Write-Host "Launching Client 0..."
Start-Process java -ArgumentList 'Client', '0', '127.0.0.1', '0', '1', '2'
Write-Host "Client 0 Launched"

Start-Sleep -Seconds 3

Write-Host "Launching Client 0..."
Start-Process java -ArgumentList 'Client', '1', '127.0.0.1', '0', '1', '2'
Write-Host "Client 0 Launched"

Start-Sleep -Seconds 3

Write-Host "Launching Client 0..."
Start-Process java -ArgumentList 'Client', '2', '127.0.0.1', '0', '1', '2'
Write-Host "Client 0 Launched"

Set-Location ../../..
#>