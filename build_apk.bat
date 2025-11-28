@echo off
setlocal enabledelayedexpansion

REM Android APK自动构建脚本
REM 作者: AI Assistant
REM 日期: %date%

REM 设置项目根目录
set PROJECT_DIR=%~dp0

REM 设置构建输出目录
set OUTPUT_DIR=%PROJECT_DIR%app\build\outputs\apk\debug

REM 检查Java环境
echo 正在检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误: 未检测到Java环境，请确保已安装JDK并配置了JAVA_HOME环境变量
    pause
    exit /b 1
)

REM 显示Java版本
java -version
echo.

REM 检查Gradle包装器是否存在
if not exist "%PROJECT_DIR%gradlew.bat" (
    echo 错误: 未找到Gradle包装器(gradlew.bat)，请确保当前目录是Android项目根目录
    pause
    exit /b 1
)

echo 正在构建Android APK...
echo =========================================

REM 清理之前的构建
echo 正在清理之前的构建...
call %PROJECT_DIR%gradlew.bat clean
if %errorlevel% neq 0 (
    echo 错误: 清理构建失败
    pause
    exit /b 1
)
echo 清理完成！
echo.

REM 构建Debug版本APK
echo 正在构建Debug版本APK...
echo 这可能需要几分钟时间，请耐心等待...
echo.
call %PROJECT_DIR%gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo.
    echo 错误: 构建APK失败
    pause
    exit /b 1
)

echo.
echo 构建成功！
echo =========================================
echo.

REM 检查APK文件是否生成
if exist "%OUTPUT_DIR%\*.apk" (
    echo 已生成APK文件:
    echo -----------------------------------------
    dir /b "%OUTPUT_DIR%\*.apk"
    echo -----------------------------------------
    echo APK文件位置: %OUTPUT_DIR%
    echo.
    echo 您可以使用以下命令安装APK到连接的设备:
    echo adb install %OUTPUT_DIR%\*.apk
    echo.
    echo 构建完成！
) else (
    echo 警告: 未找到生成的APK文件
    echo 请检查构建日志以获取详细信息
)

pause
endlocal