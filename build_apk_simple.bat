@echo off

REM 简单的Android APK构建脚本

echo 正在构建Android APK...
echo.

REM 检查Gradle包装器是否存在
if not exist "gradlew.bat" (
    echo 错误: 未找到gradlew.bat文件，请确保在项目根目录下运行此脚本
    pause
    exit /b 1
)

REM 执行构建命令
gradlew.bat clean assembleDebug

if %errorlevel% neq 0 (
    echo.
    echo 构建失败！
    pause
    exit /b 1
)

echo.
echo 构建完成！
echo 正在查找APK文件...
echo.

REM 搜索APK文件
dir /s /b *.apk | find /i "debug" | find /v "intermediates" | find /v "generated"

echo.
echo 构建脚本执行完毕！
pause