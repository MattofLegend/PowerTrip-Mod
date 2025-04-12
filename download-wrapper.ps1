# Download Gradle wrapper JAR directly
$wrapperUrl = "https://github.com/gradle/gradle/raw/master/gradle/wrapper/gradle-wrapper.jar"
$outputFile = "gradle/wrapper/gradle-wrapper.jar"

Write-Host "Downloading Gradle wrapper JAR..." -ForegroundColor Yellow
Invoke-WebRequest -Uri $wrapperUrl -OutFile $outputFile

Write-Host "Download complete!" -ForegroundColor Green
