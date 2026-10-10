$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\jbr"
$env:PATH = "$env:JAVA_HOME\bin;" + $env:PATH
Set-Location "C:\Users\Dell\.gemini\antigravity\scratch\apexcare-hospital-management\backend"
& "C:\apache-maven-3.9.16\bin\mvn.cmd" spring-boot:run
