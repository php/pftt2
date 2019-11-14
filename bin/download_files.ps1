Param([string]$url, [string]$path, [string]$file_name)

If ($url -eq "" -or $path -eq "" -or $file_name -eq "") {
	Write-Output "User error: Must specifiy url, save path and file name"
	Write-Output "download_file <url> <path to dir> <desired file name>"
}

Import-Module BitsTransfer

Write-Host "Downloading" $file_name"..." -ForegroundColor Green
	Start-BitsTransfer -Source $url -Destination $path