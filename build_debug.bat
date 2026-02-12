@echo off
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Using JAVA_HOME: %JAVA_HOME%
echo.

call gradlew.bat clean assembleDebug

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo BUILD FAILED!
    echo Check the error message above.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo BUILD SUCCESSFUL!
echo You can now run the app in Android Studio.
pause
