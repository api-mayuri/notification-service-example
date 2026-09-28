@REM Maven Wrapper (only-script) для Windows: скачивает Maven и запускает его.
@echo off
setlocal
set "PROPS=%~dp0.mvn\wrapper\maven-wrapper.properties"
for /f "usebackq tokens=1,* delims==" %%A in ("%PROPS%") do if "%%A"=="distributionUrl" set "DIST_URL=%%B"
if "%DIST_URL%"=="" (echo mvnw: distributionUrl не задан & exit /b 1)
if "%MAVEN_USER_HOME%"=="" set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
for %%F in ("%DIST_URL%") do set "DIST_NAME=%%~nF"
set "DIST_DIR=%DIST_NAME:-bin=%"
set "INSTALL_ROOT=%MAVEN_USER_HOME%\wrapper\dists\%DIST_NAME%"
set "MAVEN_HOME=%INSTALL_ROOT%\%DIST_DIR%"
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%INSTALL_ROOT%" mkdir "%INSTALL_ROOT%"
  powershell -NoProfile -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%INSTALL_ROOT%\maven.zip'; Expand-Archive -Path '%INSTALL_ROOT%\maven.zip' -DestinationPath '%INSTALL_ROOT%' -Force; Remove-Item '%INSTALL_ROOT%\maven.zip'"
  if errorlevel 1 (echo mvnw: не удалось скачать Maven & exit /b 1)
)
"%MAVEN_HOME%\bin\mvn.cmd" %*
