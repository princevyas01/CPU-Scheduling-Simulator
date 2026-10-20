$files = git status -uall --porcelain | ForEach-Object { $_.Substring(3).Trim() }
Write-Output "Found $($files.Count) files to commit and push."
foreach ($f in $files) {
    if (Test-Path $f) {
        git add $f
        $msg = "Add or update $f"
        git commit -m $msg
        git push origin main
        Write-Output "Pushed: $f"
    }
}
