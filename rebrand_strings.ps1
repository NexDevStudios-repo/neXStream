$files = Get-ChildItem -Path "composeApp\src", "androidApp\src" -Recurse -Filter "strings.xml"
foreach ($f in $files) {
    $content = [System.IO.File]::ReadAllText($f.FullName, [System.Text.Encoding]::UTF8)
    $newContent = $content.Replace("Nuvio Mobile", "neXStream")
    $newContent = $newContent.Replace("Nuvio TV", "neXStream TV")
    $newContent = $newContent.Replace("Nuvio Debug", "neXStream Debug")
    $newContent = $newContent.Replace("Nuvio Sync", "neXStream Sync")
    $newContent = $newContent.Replace("Nuvio Library", "neXStream Library")
    $newContent = $newContent.Replace("NuvioMedia", "Nex Dev Studios")
    $newContent = $newContent.Replace("Nuvio", "neXStream")
    if ($content -ne $newContent) {
        [System.IO.File]::WriteAllText($f.FullName, $newContent, [System.Text.Encoding]::UTF8)
        Write-Host ("Updated: " + $f.FullName)
    }
}
Write-Host "Done strings replacement."
