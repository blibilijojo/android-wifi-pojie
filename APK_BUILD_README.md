# Android APK 自动构建指南

本项目提供了自动构建Android APK的脚本，支持Windows系统，无需手动执行复杂的构建命令。

## 目录

- [支持的系统](#支持的系统)
- [前置条件](#前置条件)
- [使用方法](#使用方法)
  - [使用批处理脚本 (推荐)](#使用批处理脚本-推荐)
  - [使用PowerShell脚本](#使用powershell脚本)
- [构建流程](#构建流程)
- [构建结果](#构建结果)
- [常见问题](#常见问题)

## 支持的系统

- Windows 7/8/10/11

## 前置条件

在运行构建脚本之前，请确保您的系统满足以下要求：

1. **JDK 安装**：已安装 Java Development Kit (JDK) 8 或更高版本
   - 下载地址：[Oracle JDK](https://www.oracle.com/java/technologies/downloads/) 或 [OpenJDK](https://adoptium.net/)
   - 配置 `JAVA_HOME` 环境变量，指向 JDK 安装目录
   - 将 `%JAVA_HOME%\bin` 添加到 `Path` 环境变量

2. **Android SDK**：已安装 Android SDK (通过 Android Studio 或独立安装)
   - 配置 `ANDROID_HOME` 环境变量，指向 Android SDK 安装目录
   - 将 `%ANDROID_HOME%\platform-tools` 和 `%ANDROID_HOME%\tools` 添加到 `Path` 环境变量

3. **设备驱动**：如果需要安装到物理设备，已安装相应的 USB 驱动

## 使用方法

### 使用批处理脚本 (推荐)

1. 打开文件资源管理器，导航到项目根目录
2. 双击运行 `build_apk.bat` 文件
3. 等待构建过程完成

### 使用 PowerShell 脚本

1. 打开 PowerShell 终端
2. 导航到项目根目录：
   ```powershell
   cd e:\py\android-wifi-pojie
   ```
3. 运行 PowerShell 脚本：
   ```powershell
   .\build_apk.ps1
   ```

## 构建流程

1. **检查环境**：验证 Java 环境是否正确安装
2. **清理构建**：删除之前的构建文件，确保干净构建
3. **构建 APK**：执行 Gradle 构建命令，生成 Debug 版本 APK
4. **输出结果**：显示构建结果和 APK 文件位置

## 构建结果

构建成功后，APK 文件将生成在以下目录：

```
e:\py\android-wifi-pojie\app\build\outputs\apk\debug
```

您可以使用以下命令将 APK 安装到连接的设备：

```bash
adb install e:\py\android-wifi-pojie\app\build\outputs\apk\debug\*.apk
```

## 常见问题

### Q: 构建失败，提示 "未检测到Java环境"

**A**: 请确保已正确安装 JDK 并配置了环境变量。可以通过以下命令验证：

```bash
java -version
```

如果仍有问题，请检查 `JAVA_HOME` 环境变量是否正确配置。

### Q: 构建失败，提示 "未找到Gradle包装器"

**A**: 请确保您在项目根目录下运行脚本。Gradle 包装器文件 (`gradlew.bat`) 应该位于项目根目录。

### Q: 构建失败，提示 "Could not find Android SDK"

**A**: 请确保已正确安装 Android SDK 并配置了 `ANDROID_HOME` 环境变量。

### Q: 构建成功，但找不到 APK 文件

**A**: 请检查构建日志，确认是否有警告或错误信息。如果构建过程中出现问题，APK 文件可能不会生成。

### Q: 安装 APK 失败，提示 "INSTALL_FAILED_TEST_ONLY"

**A**: 这是因为 Debug 版本 APK 默认带有 `testOnly` 标志。您可以使用以下命令安装：

```bash
adb install -t e:\py\android-wifi-pojie\app\build\outputs\apk\debug\*.apk
```

## 构建类型

当前脚本只构建 Debug 版本 APK。如果需要构建 Release 版本，请修改脚本中的构建命令：

- Debug 版本：`assembleDebug`
- Release 版本：`assembleRelease`

## 联系信息

如果您在使用过程中遇到问题，可以通过以下方式获取帮助：

- 查看项目文档
- 检查构建日志中的详细错误信息
- 在项目 GitHub 仓库提交 Issue

---

**注意**：本脚本仅用于开发和测试目的。在发布应用之前，请确保进行充分的测试和签名。