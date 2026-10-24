# Fabric 26.2 本地开发（F2 构建）

`26.2-fabric` 的根工程已使用 Loom。IDE 导入根目录 `settings.gradle`，不再导入临时 `fabric-environment/`。
F2 已接回领域注册表、基因与组件、物品/流体/能量传输、机器、养蜂、树木、蝴蝶、农场、背包、分拣、工作台及邮政平台。
F2 已完成，逐项结果见 [F2 验收报告](porting/f2-acceptance.zh-CN.md)和[接续记录](porting/f2-status.zh-CN.md)。完整客户端业务界面、世界生成和专属集成继续由 F3 完成。

## 安装与启动

在仓库根目录打开 PowerShell：

```powershell
# 首次准备：安装/检查本地 Java 25，构建四包并检查隔离
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-fabric.ps1

# 开发客户端：默认加载四模块、Jade、JEI
.\gradlew-fabric.bat runClient

# 普通独立服务器；在控制台输入 stop 保存并退出
.\gradlew-fabric.bat runServer

# 依赖与游戏资产已下载后可离线使用
.\gradlew-fabric.bat runClient --offline

# 构建四个 F2 JAR
.\gradlew-fabric.bat build
```

客户端首次启动可能显示无障碍引导。创建新的创造模式测试世界，在创造栏或 JEI 搜索 `@forestry`。
JEI 悬停物品按 `R` 查看配方、`U` 查看用途；Jade 看向近处方块显示名称和来源。
可用例子：`/give @s forestry:apatite`、`/setblock ~ ~ ~3 forestry:apatite_ore`，9 个磷灰石合成压块。
手册物品保留原降级提示；当前无 Patchouli 手册界面。

普通独立服务器首次使用需要使用者阅读并接受 [Minecraft EULA](https://aka.ms/MinecraftEULA)。
构建和安装脚本不自动接受。此次开发者已明确同意，仅本机 `server-0`、`server-7` 已写入接受声明。
本机验证配置绑定 `127.0.0.1`，核心服务器端口 `25580`，四模块服务器端口 `25587`，保留 `online-mode=true`。
F2 最终专服使用 `f2-development-world`；原 F1 世界文件仍保留。更改是否加载 Jade 时，建议使用单独测试世界，避免旧世界保存的 Jade 游戏规则在卸载模组后无法解析。
新克隆未附带这些忽略目录中的运行配置，会使用游戏首次生成的默认配置；如需多人连接请自行配置地址与身份认证。
开发客户端使用离线测试身份，本轮独立服务器验证为启动、载入世界和保存退出，不包含正版账号联网登录。

## 模块及工具开关

`-PforestryModules=0..7` 只控制开发运行时安装组合，构建仍输出四个 JAR。
核心总是安装；蝴蝶为 1、农场为 2、邮件为 4，相加选择扩展。

| 值 | 核心之外安装的扩展 |
| --- | --- |
| 0 | 无 |
| 1 | 蝴蝶 |
| 2 | 农场 |
| 3 | 蝴蝶、农场 |
| 4 | 邮件 |
| 5 | 蝴蝶、邮件 |
| 6 | 农场、邮件 |
| 7（默认） | 三个扩展 |

```powershell
.\gradlew-fabric.bat runClient -PforestryModules=0
.\gradlew-fabric.bat runServer -PforestryModules=4
.\gradlew-fabric.bat runClient -PwithJade=false -PwithJei=false
```

Jade、JEI 仅在开发运行时加载，不是四个 JAR 的硬依赖，也不会内嵌。
不需复制到 `mods/`；不要把 NeoForge 版本的工具或旧 Forestry JAR 放入 Fabric 实例。
`gradlew-local.bat` 现转发到 `gradlew-fabric.bat`，NeoForge 命令只适用于 `26.2-neoforge` 分支。

## 工具链及目录

| 项目 | 固定值 |
| --- | --- |
| Minecraft / Java | 26.2 / Temurin 25.0.4.1+1 |
| Gradle / Loom | 9.5.1 / `net.fabricmc.fabric-loom` 1.17.21 |
| Fabric Loader / API | 0.19.5 / 0.161.0+26.2 |
| Team Reborn Energy | 核心 JAR 内嵌官方 5.0.0，运行时接受兼容 5.x；1 FE = 1 E，无需 Tech Reborn |
| Jade | 26.2.11，Modrinth ID `ue8CO97w` |
| JEI / Mezz Config | 30.42.0.236 / 0.6.8（配置库上游标记 beta） |

版本位于根 `gradle.properties` 和 `build.gradle`。已去混淆的 26.2 不配置 Yarn/Parchment/remap。
Wrapper 固定 SHA-256，依赖校验使用根 `gradle/verification-metadata.xml`；不要绕过校验来修复下载失败。
开发依赖原始来源及哈希见 [依赖记录](porting/fabric-dependencies.json)。
接口以锁定依赖为准，工具链选择依据 [Fabric 26.2 官方说明](https://fabricmc.net/2026/06/15/262.html)。

| 用途 | 路径 |
| --- | --- |
| Java / Gradle 缓存 | `.local/jdk25` / `.local/gradle-fabric-26.2` |
| 项目缓存 | `.local/project-cache-fabric-26.2-root` |
| 四包输出 | `build/fabric-26.2/libs/` |
| 客户端 / 专服 | `run/fabric-26.2/client-<mask>/` / `server-<mask>/` |
| 服务端 / 客户端 GameTest | `run/fabric-26.2/f2-gametest-<mask>/` / `f2-client-test-<mask>/` |
| 实际资源子集 / 资源报告 | `build/fabric-26.2/generated/f2-resources/` / 其中的 `resource-report.json` |
| 包检查及待迁移源码清单 | `build/fabric-26.2/reports/f2/isolation.json` |
| 组合测试日志 | `.local/f2-validation/` |

IDE Gradle JVM 选择 `.local/jdk25`，Gradle User Home 设置为 `.local/gradle-fabric-26.2`。
运行根工程的 `runClient` / `runServer`；需要源码时运行 `gradlew-fabric.bat genSources`。
Linux/macOS 安装 Java 25 后使用 `bash gradlew <task>`。本轮图形和专服实测平台为 Windows。

## 验证与后续开发

```powershell
# 构建、静态检查、8 种真实服务端测试组合
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-fabric-f2.ps1 -Offline

# 可同时加入核心/全装图形测试和普通专服测试；专服必须先自行接受 EULA
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-fabric-f2.ps1 -Clients -DedicatedServers -Persistence -Offline

# 单项测试
.\gradlew-fabric.bat runGameTest -PforestryModules=7 -PwithJade=false -PwithJei=false
.\gradlew-fabric.bat runClientGameTest
.\gradlew-fabric.bat checkFabricF2

# 验证实际发行包的 Energy 嵌套发现、去重和版本选择，不启动游戏
.\scripts\test-energy-bundling.ps1
```

Energy 校验使用当前 Fabric Loader 的真实发现/解析器，覆盖客户端及服务端、无其他 API 提供者、
仅引用 API 的消费者、同版本重复内嵌/外置和不兼容主版本拒绝。较新/较旧版本使用合成元数据验证选择规则，
不代表验证未来 API 的二进制兼容性。可用 `-CompanionJar <包1>,<包2>` 加入本机实际模组包检查共存；
运行时能量传输、方向、限额及事务回滚由 `F2TransferTests` 的 GameTest 覆盖。
依赖解析探针的实例、输入包和结果只保存在忽略目录 `.local/energy-bundling/`，不放入 CI 上传的 `build/reports/`。

只检查普通主菜单或专服启动时可分别添加 `-PclientAutoExit=true`、`-PserverAutoExit=true`。
正常交互不传这些开关。`build` 包含四 JAR 隔离检查和当前选定组合的服务端 GameTest；完整组合、图形和普通专服由上面的验证脚本显式执行。
服务端 GameTest 每次重建专属 `f2-gametest-<mask>/world`，避免开关 Jade 后遗留游戏规则影响测试；不会清理普通客户端或专服存档。
测试源码位于 `src/fabricTest`，不会进入生产包。CI 位于 `.github/workflows/build.yml`，覆盖静态构建及八组合服务端测试。

`gradle/fabric-f2-sources.json` 明确启用源码，`fabric-f2-content.json` 明确启用注册内容。
原 NeoForge 源码、资源及测试保留为迁移参考，不在正式 Fabric 编译路径中。
资源准备任务只输出可用子集，记录所有延后配方；完整 Fabric datagen、客户端业务界面与发行实例验证继续由 F3 处理。现有 F2 回归覆盖实际服务端消费者，不宣称已通过 G3 或支持任意旧世界直接升级。

F2 最终基线见 [f2-baseline.json](porting/f2-baseline.json)：四包哈希、八组合结果、两种图形客户端、跨 JVM 保存恢复和两种普通专服均已验证。
验证脚本将本次选定检查记录到 `.local/f2-validation/results.json`；验收时合并并核对的完整历史证据另存为 `final-results.json`。

### BC / TR 原生服务器能量联调

2026-10-24 使用当前四个发行 JAR，在非开发模式的 Minecraft 26.2 / Fabric Loader 0.19.5 专服中完成以下检查。
外部模组为 BCRefabricated `26.9.17+mc26.2`、Tech Reborn `6.1.1`（内含 RebornCore `6.1.1`）。
每组都是新建的隔离世界，只在初始化时预充有限能量，之后由真实机器和线缆运行 300 个游戏 tick。

| 安装组合（均含 Forestry 和 Fabric API） | Forestry → 对方 | 对方 → Forestry | 已加载 Energy API |
| --- | --- | --- | --- |
| 仅 BC | 4000 E 全部到达 | 4000 E 全部到达 | 1 份 5.0.0 |
| 仅 TR | 4096 E 全部到达 | 4096 E 全部到达 | 1 份 5.0.0 |
| BC + TR | BC 4000 E、TR 4096 E 均全部到达 | BC 4000 E、TR 4096 E 均全部到达 | 1 份 5.0.0 |

BC 正向为泥炭引擎 → 木/石 RF 管 → BC Energy Engine；反向为 MJ Dynamo → 木/石 RF 管 → Forestry 离心机。
BC 反向初始使用等价 4000 E 的 MJ，按该包默认配置转换（1 E = 100000 microMJ），不是把 MJ 与 E 当作 1:1。
TR 两个方向均通过两段绝缘铜线，TR 端使用低压储能块；Forestry 端分别为泥炭引擎和离心机。
八条实际传输线路全部通过，最终源和线缆余量为零，没有能量丢失或增加，Forestry 保持 1 FE = 1 E。

三个实例的 `mods/` 均没有独立 Energy JAR。传输前后都断言恰好一个 `team_reborn_energy` ModContainer、
一个 `EnergyStorage.class` 类资源，并记录嵌套来源和实际类加载位置；此次三组均由 Forestry 的嵌套 API 提供。
TR 和 RebornCore 自带的同版本 API 没有重复加载，三个服务器均正常保存并退出。
这是原生专服业务测试，不包含图形客户端界面验证。

复跑先准备发行包和原生运行库，再传入本机模组包和已有的 EULA 接受文件：

```powershell
.\gradlew-fabric.bat stageFabricDistribution -PwithJade=false -PwithJei=false
python scripts/test-fabric-release.py runtime
python scripts/test-energy-interop.py `
  --runtime-manifest .local/f3-production/runtime/manifest.json `
  --bc '实际路径/BCRefabricated-26.9.17+mc26.2.jar' `
  --tr '实际路径/TechReborn-6.1.1.jar' `
  --eula-from '实际路径/eula.txt'
```

脚本按发行校验报告验证四包哈希，测试探针不会进入发行 JAR。结果、输入哈希、启动参数和完整日志保存于
`.local/energy-interop/runs/<run>/`，最新路径见 `.local/energy-interop/latest-run.txt`。
此次通过记录为 `1792824936540373100/results.json`；早先沙箱回环连接受限的失败尝试单独保留，不计为模组故障。

## F2 数据与配置

流体内部单位为 Fabric droplets：1 mB = 81 droplets，1 桶 = 81000 droplets；新保存格式使用 schema 2，旧无标记流体量按 mB 转换。
胶囊和罐子只接收整桶，排空时消耗容器；吸管与可用罐体保留精确余数。所有这些操作均参与 Fabric 事务。

全局模板为 `config/forestry/server.json`，每个世界使用 `serverconfig/forestry-server.json`；客户端偏好在 `config/forestry/client.json`。
首次缺少 JSON 时可迁移原 Forestry 生成的 `forestry-server.toml` / `forestry-client.toml` 中的布尔与数值配置；保留 TOML 原文件，非法或未知键会报告错误。
连接时服务器配置、领域定义和解析后的 DataMap 会同步到客户端；断线后清理连接状态，不覆盖集成服务器数据。

重启持久化专项须按顺序执行，使用独立测试目录，不能把 verify 当作 seed：

```powershell
.\gradlew-fabric.bat runGameTest -PpersistencePhase=seed --offline
.\gradlew-fabric.bat runGameTest -PpersistencePhase=verify --offline
```

专项保存于 `run/fabric-26.2/f2-persistence-7/world`；seed 会重建该测试世界，verify 保留并读取它。
