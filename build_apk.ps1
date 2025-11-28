<#
.SYNOPSIS
Android APK自动构建脚本

.DESCRIPTION
该脚本用于自动构建Android APK文件，支持清理构建、构建Debug版本等功能

.AUTHOR
AI Assistant

.DATE
$(Get-Date)
#>

Write-Host "=========================================" -ForegroundColor Green
Write-Host "Android APK自动构建脚本" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""

# 检查Java环境
Write-Host "正在检查Java环境..." -ForegroundColor Cyan
try {
    $javaVersion = java -version 2>&1
    Write-Host $javaVersion
    Write-Host "Java环境检测成功！" -ForegroundColor Green
} catch {
    Write-Host "错误: 未检测到Java环境，请确保已安装JDK并配置了JAVA_HOME环境变量" -ForegroundColor Red
    pause
    exit 1
}

Write-Host ""

# 使用当前目录的gradlew.bat文件
$GradlewPath = ".\gradlew.bat"

# 检查Gradle包装器是否存在
Write-Host "正在检查Gradle包装器..." -ForegroundColor Cyan
if (-not (Test-Path -Path $GradlewPath)) {
    Write-Host "错误: 未找到Gradle包装器(gradlew.bat)，请确保当前目录是Android项目根目录" -ForegroundColor Red
    pause
    exit 1
}
Write-Host "Gradle包装器检测成功！" -ForegroundColor Green

# 清理之前的构建
Write-Host ""
Write-Host "正在清理之前的构建..." -ForegroundColor Cyan
& cmd /c $GradlewPath clean
if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 清理构建失败" -ForegroundColor Red
    pause
    exit 1
}
Write-Host "清理完成！" -ForegroundColor Green

# 构建Debug版本APK
Write-Host ""
Write-Host "正在构建Debug版本APK..." -ForegroundColor Cyan
Write-Host "这可能需要几分钟时间，请耐心等待..." -ForegroundColor Yellow
Write-Host ""
& cmd /c $GradlewPath assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "错误: 构建APK失败" -ForegroundColor Red
    pause
    exit 1
}

Write-Host ""
Write-Host "=========================================" -ForegroundColor Green
Write-Host "构建完成！" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Green
Write-Host ""

# 搜索生成的APK文件
Write-Host "正在搜索生成的APK文件..." -ForegroundColor Cyan
$ApkFiles = Get-ChildItem -Path . -Recurse -Filter "*.apk" | Where-Object {$_.FullName -notlike "*\build\intermediates\*"} | Where-Object {$_.FullName -notlike "*\build\generated\*"}

if ($ApkFiles.Count -gt 0) {
    Write-Host "已生成APK文件:" -ForegroundColor Cyan
    Write-Host "-----------------------------------------" -ForegroundColor Cyan
    foreach ($File in $ApkFiles) {
        Write-Host $File.FullName -ForegroundColor Yellow
    }
    Write-Host "-----------------------------------------" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "您可以使用以下命令安装第一个APK到连接的设备:" -ForegroundColor Cyan
    Write-Host "adb install $($ApkFiles[0].FullName)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "构建成功！" -ForegroundColor Green
} else {
    Write-Host "警告: 未找到生成的APK文件" -ForegroundColor Yellow
    Write-Host "请检查构建日志以获取详细信息" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "您可以尝试手动运行以下命令查看更详细的构建日志:" -ForegroundColor Cyan
    Write-Host "$GradlewPath assembleDebug --stacktrace" -ForegroundColor Yellow
}

Write-Host ""
pause