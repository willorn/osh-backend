param(
    [string]$ConfigPath = ".\MCP.json",
    [switch]$ForceLogin
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-PlainTextFromSecureString {
    param([Security.SecureString]$Secure)

    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

function Get-JsonTokenFromLoginResponse {
    param($Json)

    if ($null -eq $Json) { return $null }
    if ($Json.PSObject.Properties.Name -contains "token" -and $Json.token) { return [string]$Json.token }
    if ($Json.PSObject.Properties.Name -contains "sessionID" -and $Json.sessionID) { return [string]$Json.sessionID }
    if ($Json.PSObject.Properties.Name -contains "zentaosid" -and $Json.zentaosid) { return [string]$Json.zentaosid }

    if (($Json.PSObject.Properties.Name -contains "data") -and $Json.data) {
        if ($Json.data.PSObject.Properties.Name -contains "token" -and $Json.data.token) { return [string]$Json.data.token }
        if ($Json.data.PSObject.Properties.Name -contains "sessionID" -and $Json.data.sessionID) { return [string]$Json.data.sessionID }
        if ($Json.data.PSObject.Properties.Name -contains "zentaosid" -and $Json.data.zentaosid) { return [string]$Json.data.zentaosid }
    }

    return $null
}

function Get-CookieTokenFromHeaders {
    param($Headers)

    if ($null -eq $Headers) { return $null }

    $cookieValues = @()
    if ($Headers["Set-Cookie"]) {
        if ($Headers["Set-Cookie"] -is [Array]) {
            $cookieValues = $Headers["Set-Cookie"]
        }
        else {
            $cookieValues = @([string]$Headers["Set-Cookie"])
        }
    }

    foreach ($cookie in $cookieValues) {
        if ($cookie -match "zentaosid=([^;]+)") {
            return $matches[1]
        }
    }

    return $null
}

function Get-BaseRootUrl {
    param([string]$McpUrl)

    $uri = [Uri]$McpUrl
    $path = $uri.AbsolutePath
    if (-not $path.EndsWith("/mcp")) {
        throw "Unexpected MCP url path: $path"
    }

    $rootPath = $path.Substring(0, $path.Length - 4)
    return "$($uri.Scheme)://$($uri.Authority)$rootPath"
}

function Format-JsonReadable {
    param(
        [string]$Json,
        [int]$IndentSize = 2
    )

    if ([string]::IsNullOrWhiteSpace($Json)) {
        return "{}"
    }

    $indent = 0
    $inString = $false
    $escaped = $false
    $newLine = [Environment]::NewLine
    $sb = New-Object System.Text.StringBuilder

    foreach ($char in $Json.ToCharArray()) {
        if ($escaped) {
            [void]$sb.Append($char)
            $escaped = $false
            continue
        }

        if ($char -eq '\\') {
            [void]$sb.Append($char)
            if ($inString) { $escaped = $true }
            continue
        }

        if ($char -eq '"') {
            [void]$sb.Append($char)
            $inString = -not $inString
            continue
        }

        if ($inString) {
            [void]$sb.Append($char)
            continue
        }

        switch ($char) {
            '{' {
                [void]$sb.Append($char)
                [void]$sb.Append($newLine)
                $indent++
                [void]$sb.Append(' ' * ($indent * $IndentSize))
                continue
            }
            '[' {
                [void]$sb.Append($char)
                [void]$sb.Append($newLine)
                $indent++
                [void]$sb.Append(' ' * ($indent * $IndentSize))
                continue
            }
            '}' {
                [void]$sb.Append($newLine)
                $indent = [Math]::Max(0, $indent - 1)
                [void]$sb.Append(' ' * ($indent * $IndentSize))
                [void]$sb.Append($char)
                continue
            }
            ']' {
                [void]$sb.Append($newLine)
                $indent = [Math]::Max(0, $indent - 1)
                [void]$sb.Append(' ' * ($indent * $IndentSize))
                [void]$sb.Append($char)
                continue
            }
            ',' {
                [void]$sb.Append($char)
                [void]$sb.Append($newLine)
                [void]$sb.Append(' ' * ($indent * $IndentSize))
                continue
            }
            ':' {
                [void]$sb.Append(': ')
                continue
            }
            ' ' {
                continue
            }
            "`r" {
                continue
            }
            "`n" {
                continue
            }
            default {
                [void]$sb.Append($char)
                continue
            }
        }
    }

    return $sb.ToString()
}

function Copy-ObjectPreservingOrder {
    param([object]$InputObject)

    if ($null -eq $InputObject) {
        return $null
    }

    if ($InputObject -is [string] -or $InputObject -is [ValueType]) {
        return $InputObject
    }

    if ($InputObject -is [System.Collections.IEnumerable] -and -not ($InputObject -is [System.Collections.IDictionary]) -and -not ($InputObject -is [psobject])) {
        $items = New-Object System.Collections.ArrayList
        foreach ($item in $InputObject) {
            [void]$items.Add((Copy-ObjectPreservingOrder -InputObject $item))
        }
        return ,$items.ToArray()
    }

    $ordered = [ordered]@{}
    foreach ($property in $InputObject.PSObject.Properties) {
        $ordered[$property.Name] = Copy-ObjectPreservingOrder -InputObject $property.Value
    }

    return [pscustomobject]$ordered
}

function Get-OrderedProperties {
    param(
        [object]$InputObject,
        [string[]]$PreferredOrder
    )

    $ordered = [ordered]@{}
    $propertyNames = @($InputObject.PSObject.Properties.Name)

    foreach ($name in $PreferredOrder) {
        if ($propertyNames -contains $name) {
            $ordered[$name] = $InputObject.$name
        }
    }

    foreach ($name in $propertyNames) {
        if ($PreferredOrder -notcontains $name) {
            $ordered[$name] = $InputObject.$name
        }
    }

    return [pscustomobject]$ordered
}

function Normalize-McpConfigOrder {
    param([object]$ConfigObject)

    $root = [ordered]@{}
    $rootPropertyNames = @($ConfigObject.PSObject.Properties.Name)

    if ($rootPropertyNames -contains 'mcpServers') {
        $servers = [ordered]@{}

        foreach ($serverProperty in $ConfigObject.mcpServers.PSObject.Properties) {
            if ($serverProperty.Name -eq 'zentao') {
                $serverValue = $serverProperty.Value
                $serverOrdered = Get-OrderedProperties -InputObject $serverValue -PreferredOrder @('disabled', 'type', 'url', 'timeout', 'headers', 'apiBaseUrl')

                if ($serverOrdered.PSObject.Properties.Name -contains 'headers' -and $null -ne $serverOrdered.headers) {
                    $headersOrdered = Get-OrderedProperties -InputObject $serverOrdered.headers -PreferredOrder @('token', 'Authorization')
                    $serverOrdered = Get-OrderedProperties -InputObject ([pscustomobject]@{
                        disabled = $serverOrdered.disabled
                        type = $serverOrdered.type
                        url = $serverOrdered.url
                        timeout = $serverOrdered.timeout
                        headers = $headersOrdered
                        apiBaseUrl = $serverOrdered.apiBaseUrl
                    }) -PreferredOrder @('disabled', 'type', 'url', 'timeout', 'headers', 'apiBaseUrl')
                }

                $servers[$serverProperty.Name] = Copy-ObjectPreservingOrder -InputObject $serverOrdered
            }
            else {
                $servers[$serverProperty.Name] = Copy-ObjectPreservingOrder -InputObject $serverProperty.Value
            }
        }

        $root['mcpServers'] = [pscustomobject]$servers
    }

    foreach ($name in $rootPropertyNames) {
        if ($name -ne 'mcpServers') {
            $root[$name] = Copy-ObjectPreservingOrder -InputObject $ConfigObject.$name
        }
    }

    return [pscustomobject]$root
}

function Save-JsonConfig {
    param(
        [object]$ConfigObject,
        [string]$Path
    )

    $normalizedConfig = Normalize-McpConfigOrder -ConfigObject $ConfigObject
    $compact = $normalizedConfig | ConvertTo-Json -Depth 20 -Compress
    $pretty = $null

    try {
        $pretty = $compact | node -e "let s='';process.stdin.on('data',d=>s+=d);process.stdin.on('end',()=>process.stdout.write(JSON.stringify(JSON.parse(s),null,2)));"
        if ($pretty -is [System.Array]) {
            $pretty = $pretty -join [Environment]::NewLine
        }
    }
    catch {
        # Fallback when Node.js is not available.
    }

    if ([string]::IsNullOrWhiteSpace($pretty)) {
        $pretty = Format-JsonReadable -Json $compact -IndentSize 2
    }

    $pretty = $pretty.TrimEnd("`r", "`n")
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, ($pretty + [Environment]::NewLine), $utf8)
}

function Get-ApiBaseCandidates {
    param(
        [string]$McpUrl,
        [string]$ConfiguredApiBase
    )

    $results = New-Object System.Collections.Generic.List[string]

    if (-not [string]::IsNullOrWhiteSpace($ConfiguredApiBase)) {
        $results.Add($ConfiguredApiBase.TrimEnd('/'))
    }

    $mcpBase = Get-BaseRootUrl -McpUrl $McpUrl
    $results.Add($mcpBase.TrimEnd('/'))

    $mcpUri = [Uri]$McpUrl
    $rootPath = ([Uri]$mcpBase).AbsolutePath.TrimEnd('/')
    if (-not [string]::IsNullOrWhiteSpace($rootPath)) {
        $apiUriBuilder = New-Object System.UriBuilder($mcpUri.Scheme, $mcpUri.Host, 2001)
        $apiUriBuilder.Path = $rootPath
        $results.Add($apiUriBuilder.Uri.AbsoluteUri.TrimEnd('/'))
    }

    $seen = @{}
    $unique = New-Object System.Collections.Generic.List[string]
    foreach ($item in $results) {
        if ([string]::IsNullOrWhiteSpace($item)) { continue }
        if (-not $seen.ContainsKey($item)) {
            $seen[$item] = $true
            $unique.Add($item)
        }
    }

    return $unique
}

function Test-ZentaoToken {
    param(
        [string]$ApiBase,
        [string]$Token
    )

    if ([string]::IsNullOrWhiteSpace($Token)) {
        return $false
    }

    $probeUrls = @(
        "$ApiBase/api.php/v2/users?page=1&limit=1",
        "$ApiBase/api.php/v2/user?page=1&limit=1"
    )

    foreach ($probeUrl in $probeUrls) {
        try {
            $response = Invoke-RestMethod -Method Get -Uri $probeUrl -Headers @{ token = $Token } -TimeoutSec 15
            if ($null -ne $response -and ($response.status -eq "success" -or $null -ne $response.data)) {
                return $true
            }
        }
        catch {
            # Try next probe URL.
        }
    }

    return $false
}

function Invoke-ZentaoLogin {
    param(
        [string]$ApiBase,
        [string]$BodyJson
    )

    $loginUrls = @(
        "$ApiBase/api.php/v2/users/login",
        "$ApiBase/api.php/v2/user/login"
    )

    $errors = New-Object System.Collections.Generic.List[string]
    foreach ($loginUrl in $loginUrls) {
        try {
            $response = Invoke-WebRequest -Method Post -Uri $loginUrl -ContentType "application/json" -Body $BodyJson -TimeoutSec 20
            return @{
                Response = $response
                LoginUrl = $loginUrl
            }
        }
        catch {
            $errors.Add("$loginUrl => $($_.Exception.Message)")
        }
    }

    throw "Login endpoint unavailable. Tried: $($errors -join '; ')"
}

if (-not (Test-Path -LiteralPath $ConfigPath)) {
    throw "MCP config not found: $ConfigPath"
}

$config = Get-Content -LiteralPath $ConfigPath -Raw | ConvertFrom-Json
if ($null -eq $config.mcpServers -or $null -eq $config.mcpServers.zentao) {
    throw "Invalid MCP config. Missing mcpServers.zentao"
}

$server = $config.mcpServers.zentao
if ($server.disabled -eq $true) {
    throw "mcpServers.zentao is disabled"
}

$mcpUrl = [string]$server.url
if ([string]::IsNullOrWhiteSpace($mcpUrl)) {
    throw "mcpServers.zentao.url is empty"
}

$baseRoot = Get-BaseRootUrl -McpUrl $mcpUrl
$configuredApiBase = ""
if ($server.PSObject.Properties.Name -contains "apiBaseUrl" -and $server.apiBaseUrl) {
    $configuredApiBase = [string]$server.apiBaseUrl
}

$apiBaseCandidates = Get-ApiBaseCandidates -McpUrl $mcpUrl -ConfiguredApiBase $configuredApiBase
$currentToken = [string]$server.headers.token

$tokenValid = $false
$activeApiBase = $null
if (-not $ForceLogin) {
    foreach ($apiBase in $apiBaseCandidates) {
        if (Test-ZentaoToken -ApiBase $apiBase -Token $currentToken) {
            $tokenValid = $true
            $activeApiBase = $apiBase
            break
        }
    }
}

if ($tokenValid) {
    if ($activeApiBase) {
        $server | Add-Member -NotePropertyName apiBaseUrl -NotePropertyValue $activeApiBase -Force
        Save-JsonConfig -ConfigObject $config -Path $ConfigPath
    }
    Write-Host "ZenTao MCP token is present and valid. Continue using current token." -ForegroundColor Green
    exit 0
}

Write-Host "ZenTao MCP token is empty or expired. Please enter account/password to refresh token." -ForegroundColor Yellow
$account = Read-Host "ZenTao account"
$securePassword = Read-Host "ZenTao password" -AsSecureString
$password = Get-PlainTextFromSecureString -Secure $securePassword

if ([string]::IsNullOrWhiteSpace($account) -or [string]::IsNullOrWhiteSpace($password)) {
    throw "Account/password cannot be empty"
}

$bodyObject = @{ account = $account; password = $password }
$bodyJson = $bodyObject | ConvertTo-Json -Compress

$response = $null
$loginApiBase = $null
foreach ($apiBase in $apiBaseCandidates) {
    try {
        $loginResult = Invoke-ZentaoLogin -ApiBase $apiBase -BodyJson $bodyJson
        $response = $loginResult.Response
        $loginApiBase = $apiBase
        break
    }
    catch {
        # Try next API base candidate.
    }
}

if ($null -eq $response) {
    throw "ZenTao login failed on all API base candidates: $($apiBaseCandidates -join ', ')"
}

$responseJson = $null
try {
    $responseJson = $response.Content | ConvertFrom-Json
}
catch {
    # Keep responseJson as null and try cookie parsing.
}

if ($responseJson -and $responseJson.status -and $responseJson.status -ne "success") {
    $reason = ""
    if ($responseJson.reason) { $reason = [string]$responseJson.reason }
    if (-not $reason -and $responseJson.message) { $reason = [string]$responseJson.message }
    throw "ZenTao login failed: $reason"
}

$newToken = Get-JsonTokenFromLoginResponse -Json $responseJson
if (-not $newToken) {
    $newToken = Get-CookieTokenFromHeaders -Headers $response.Headers
}
if (-not $newToken) {
    throw "Login succeeded but no token was found in response"
}

$server.headers.token = $newToken
if ($loginApiBase) {
    $server | Add-Member -NotePropertyName apiBaseUrl -NotePropertyValue $loginApiBase -Force
}
Save-JsonConfig -ConfigObject $config -Path $ConfigPath

$verifyOk = $false
if ($loginApiBase) {
    $verifyOk = Test-ZentaoToken -ApiBase $loginApiBase -Token $newToken
}
if (-not $verifyOk) {
    throw "New token was written but verification failed"
}

Write-Host "ZenTao MCP token refreshed and verified successfully." -ForegroundColor Green

