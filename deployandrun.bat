@echo off
setlocal

:start
echo Compiling...
call gradlew.bat deployNodes
echo gradle returned %errorlevel%

if "%errorlevel%"=="0" (
	echo Running the nodes...
	call build\nodes\runnodes.bat
	echo Nodes are running, do you want to kill them?
	pause
	echo Kill nodes?
	call taskkill /im java.exe /F
	echo Ready to run again.
	pause
) else (
	call taskkill /im java.exe /F
	echo taskkill returned %errorlevel%
	echo Build failed, ready to rebuild?
	pause
	goto :start
)
goto :start