[cmdletbinding(SupportsShouldProcess=$true)]
Param(
	[Parameter(Mandatory)]
	[string]$url, 
	
	[Parameter(Mandatory)]
	[string]$path, 
	
	[Parameter(Mandatory)]
	[string]$file_name
)

If ($url -eq "" -or $path -eq "" -or $file_name -eq "") {
	Write-Output "User error: Must specifiy url, save path and file name"
	Write-Output "download_file <url> <path to dir> <desired file name>"
}

$fullPath = join-path $path $file_name
Write-Host "Downloading $($url) to $($file_name)..." -ForegroundColor Green
if ($PSCmdlet.ShouldProcess($fullPath, "Download $url")) {
	[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]'Tls,Tls11,Tls12'	
	$wc = New-Object System.Net.WebClient
	$wc.Headers.Add("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.2; .NET CLR 1.0.3705;)")
	$wc.DownloadFile($url, $fullPath)
}
