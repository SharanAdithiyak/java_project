@echo off
cd /d "%~dp0"
echo Payment Console Application
echo ========================

echo.
echo Choose an option:
echo 1. Payment Console - Select from Menu
echo 2. View Saved Transactions
echo 3. Test Data Storage
echo 4. Compile All Java Files
echo 5. Exit (Console)
echo 6. Start Web Server (E-commerce Frontend)
echo.

set /p choice="Enter your choice (1-6): "

if "%choice%"=="1" goto run_console
if "%choice%"=="2" goto view_tx
if "%choice%"=="3" goto test_store
if "%choice%"=="4" goto compile_only
if "%choice%"=="5" goto exit_app
if "%choice%"=="6" goto start_server

echo Invalid choice. Please try again.
pause
goto :eof

:run_console
echo.
echo Starting Menu-Driven Payment Console...
if not exist "src\MenuDrivenPaymentConsole.class" call :compile
pushd src
java MenuDrivenPaymentConsole
popd
pause
goto :eof

:view_tx
echo.
echo Opening Simple Transaction Viewer...
if not exist "src\SimpleFileBasedViewer.class" call :compile
pushd src
java SimpleFileBasedViewer
popd
pause
goto :eof

:test_store
echo.
echo Testing Simple Data Storage...
if not exist "src\SimpleFileBasedTest.class" call :compile
pushd src
java SimpleFileBasedTest
popd
pause
goto :eof

:compile_only
echo.
echo Compiling Java files...
call :compile
pause
goto :eof

:exit_app
echo Goodbye!
exit

:start_server
echo.
echo Starting Web Server on http://localhost:8080 ...
if not exist "src\EcommerceServer.class" call :compile
start "EcommerceServer" cmd /k "pushd src && java EcommerceServer"
timeout /t 2 >nul
start http://localhost:8080/
goto :eof

:compile
echo.
echo Compiling Java files...
pushd src
javac *.java
popd
echo Compilation completed.
exit /b