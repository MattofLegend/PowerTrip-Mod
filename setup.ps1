# PowerTrip Mod Setup Script
Write-Host "Setting up PowerTrip Mod development environment..." -ForegroundColor Green

# Check Java installation
try {
    $javaVersion = java -version 2>&1 | Select-String -Pattern "version" | ForEach-Object { $_.ToString() }
    Write-Host "Found Java: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "Java is not installed or not in PATH. Please install JDK 17 or newer." -ForegroundColor Red
    Write-Host "Download from: https://adoptium.net/" -ForegroundColor Yellow
    exit 1
}

# Download Gradle binary
$gradleVersion = "8.5"
$gradleZip = "gradle-$gradleVersion-bin.zip"
$gradleUrl = "https://services.gradle.org/distributions/$gradleZip"
$gradleDir = "gradle-$gradleVersion"

Write-Host "Downloading Gradle $gradleVersion..." -ForegroundColor Yellow
Invoke-WebRequest -Uri $gradleUrl -OutFile $gradleZip

Write-Host "Extracting Gradle..." -ForegroundColor Yellow
Expand-Archive -Path $gradleZip -DestinationPath "." -Force

# Create wrapper files
Write-Host "Setting up Gradle Wrapper..." -ForegroundColor Yellow
& "$gradleDir\bin\gradle.bat" wrapper

# Clean up
Write-Host "Cleaning up temporary files..." -ForegroundColor Yellow
Remove-Item $gradleZip -Force
Remove-Item $gradleDir -Recurse -Force

Write-Host "Setup complete! You can now run 'gradlew.bat genSources' to generate Minecraft sources." -ForegroundColor Green
Write-Host "After that, run 'gradlew.bat runClient' to test your mod." -ForegroundColor Green
