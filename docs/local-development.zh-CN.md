# Windows 本地开发：NeoForge 1.21.1

此分支使用 Minecraft 1.21.1、NeoForge 21.1.230、Gradle 9.2.1 和 Java 21。
构建脚本要求 JetBrains JDK，安装脚本固定使用 JBRSDK 21.0.11 b1163.116（Windows x64）。

## 首次配置

需要 Git、Windows 自带的 PowerShell、curl.exe 和 tar.exe，以及可访问 GitHub、JetBrains、Gradle 和各 Maven 仓库的网络。
如果尚未克隆仓库：

```powershell
git clone --branch 1.21.1-neoforge https://github.com/054545641/ForestryCE-Fabricated.git
cd ForestryCE-Fabricated
```

在仓库根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-local.ps1
.\gradlew-local.bat build
```

安装脚本下载官方 JDK，校验 SHA-512 后解压到 `.local/jdk21`，再通过项目 Wrapper 下载 Gradle。
`gradlew-local.bat` 为当前进程配置 `JAVA_HOME` 和 `GRADLE_USER_HOME`，不修改系统环境变量。
JDK、下载文件和 Gradle 缓存均在已被 Git 忽略的 `.local` 下。
首次构建需要下载依赖并生成 Minecraft 开发环境，耗时取决于网络和硬件。

## 启动客户端

```powershell
.\gradlew-local.bat runClient
```

客户端会加载 Forestry 核心、蝴蝶、农场和邮件模块，以及 Gradle 中声明的开发依赖。
游戏数据和日志位于 `run` 目录。开发客户端使用测试身份，无需输入 Microsoft 账户凭据。

其他任务：

```powershell
.\gradlew-local.bat runCoreOnlyClient
.\gradlew-local.bat runServer
.\gradlew-local.bat runGameTestServer
```

`runCoreOnlyClient` 只加载 Forestry 核心模块。服务端目录为 `run/server`；首次运行后，
请阅读 Minecraft EULA，只有同意时才自行将该目录的 `eula.txt` 改为 `eula=true`。

## 构建产物

`build/libs` 中的游戏模组为以下四个 JAR；可选模块依赖核心：

- `forestry-1.21.1-3.0.0-alpha8.jar`：核心，包含蜜蜂和树木。
- `forestrybutterflies-1.21.1-3.0.0-alpha8.jar`：蝴蝶。
- `forestryfarms-1.21.1-3.0.0-alpha8.jar`：农场。
- `forestrymail-1.21.1-3.0.0-alpha8.jar`：邮件。

`api`、`sources` 和 `javadoc` JAR 供开发使用，不放入游戏的 `mods` 目录。
开发任务默认添加 Patchouli 等依赖；自行搭建普通客户端时按模组元数据安装必需运行依赖，Patchouli 可选。

Patchouli 是可选集成：编译期 API 始终存在，但任何 Gradle 任务都可加 `-PwithPatchouli=false`
将其移出开发运行的 classpath，用于验证无 Patchouli 时的启动与手册缺失提示，例如
`.\gradlew-local.bat -PwithPatchouli=false runClient`。发行 JAR 的模组元数据同样将其标为可选。

Patchouli 93 的 NeoForge 网络通道要求双端安装状态一致：双端都有、双端都没有均可正常连接；
只装一端会在进入世界前被 Patchouli 握手拒绝，界面指出缺失的一端。
发行包的隔离实例、页面检查与 GameTest 复现命令见 [G1 验收操作说明](g1-acceptance-runbook.zh-CN.md)。

## IntelliJ IDEA

1. 打开仓库根目录并作为 Gradle 项目导入。
2. 将 Project SDK 和 Gradle JVM 设为仓库中的 `.local/jdk21`。
3. 将 Gradle user home 设为仓库中的 `.local/gradle`，复用已下载的依赖。
4. 使用项目的 Gradle Wrapper，重新加载 Gradle 项目，然后执行 `runClient`。

本地 Gradle 配置在 `.local/gradle/gradle.properties`：默认堆上限 4 GB、最多 4 个 worker。
机器内存较少时可降低这些值。若下载中断，重新执行任务；若 JDK 安装提示校验失败，
删除提示中的下载压缩包后重新运行安装脚本。

## 本机验证记录

2026-10-09 的初始环境验证基于提交 `70cc3f621`：

- `gradlew-local.bat build` 成功，四个游戏模组 JAR 及 API、源码、Javadoc JAR 已生成。
- API 边界、核心层次、JAR 分区和资源类名检查通过；原项目仍有编译及 Javadoc 警告。
- `gradlew-local.bat runClient` 已进入主菜单，显示 Minecraft 1.21.1、NeoForge 21.1.230 和 17 个模组。
- 初始环境验证没有执行独立 GameTest；当天的第一阶段补验随后完成有／无 Patchouli 各 177 项必需 GameTest，并修复基线换行及过期物品条目。

构建记录保存在 `.local/build.log`，启动记录保存在 `.local/run-client.log`，游戏日志为 `run/logs/latest.log`。

第一阶段验证对象是该提交加当前工作区改动。最终发行包、真实客户端／专用服务器矩阵、手册页面、
资源重载及上游限制均记录在 [第一阶段验收报告](phase1-acceptance-audit.zh-CN.md)。这不代表游戏全部功能或后续移植阶段已验收。
