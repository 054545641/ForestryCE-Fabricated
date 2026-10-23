# Windows 本地开发：NeoForge 26.2

> 此页仅适用于 `26.2-neoforge` 分支的历史 G2 环境。
> `26.2-fabric` 已将根构建切换为 Loom，请使用 [Fabric 开发文档](fabric-development.zh-CN.md)。
> 本分支的 `gradlew-local.bat` 也已转发至 Fabric，以下 NeoForge 任务不再适用。

此分支已按调整后的范围通过路线图第二阶段 G2，见 [G2 验收报告](porting/g2-acceptance.zh-CN.md)。
四个生产模组的构建、客户端/专服、数据和保存回归通过；JEI 与 Curios 适配已恢复。
Patchouli、KubeJS、Rhino 不构成硬依赖，目标集成分别记为“因外部不可抗力无法验证（视作暂时关闭）”，不再阻塞后续阶段。
使用 `runAllJarsServer` / `runAllJarsClient` 启动四个生产模组；完整数据生成使用独立 `:runData`，旧 `gametest` 仍待迁移。
环境探针 `:environment` 使用独立 mod ID `forestry_environment`，仅验证开发工具链，
不能代替 Forestry 四模块的 G2 验收，也不能作为 Forestry 发行包。

## 固定版本

| 项目 | 固定值 |
| --- | --- |
| Minecraft | 26.2 |
| NeoForge | 26.2.0.88 |
| ModDevGradle | 2.0.148 |
| Gradle | 9.2.1，Wrapper 已固定 SHA-256 |
| 本地 JDK | Eclipse Temurin 25.0.4.1+1，Windows x64 |
| MDK 调查基准 | `NeoForgeMDKs/MDK-26.2-ModDevGradle@bc9b0583c08e952fcaba27f721ca2f7c282fcc8a` |

来源：[官方 26.2 MDK](https://github.com/NeoForgeMDKs/MDK-26.2-ModDevGradle/tree/bc9b0583c08e952fcaba27f721ca2f7c282fcc8a)、
[Temurin 固定发行](https://github.com/adoptium/temurin25-binaries/releases/tag/jdk-25.0.4.1%2B1)。
安装脚本内固定 JDK 下载 URL 与 SHA-256；Gradle 依赖校验见 `gradle/verification-metadata.xml`。
校验值只覆盖已解析的工具链及依赖；可选集成恢复时必须审核并补充对应校验值。
Minecraft 工件及资产下载另由 NeoFormRuntime 使用上游清单校验。
26.2 已去混淆，不使用旧 Parchment，也不添加旧版 remap 步骤。

## 安装与验证

需要 Git、PowerShell、curl.exe、tar.exe，以及访问 GitHub、Gradle、Mojang、NeoForge Maven 的网络。

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-local.ps1
.\gradlew-local.bat --version
.\gradlew-local.bat :environment:build checkPortingIsolation
.\gradlew-local.bat :environment:runData
.\gradlew-local.bat :environment:runGameTestServer
.\gradlew-local.bat :environment:runClient
```

探针 GameTest 实际加载服务端世界，注册测试函数并验证方块放置；不等于原 Forestry 177 项 GameTest。
自动验证客户端可加 `-PenvironmentAutoExit=true`：主菜单加载完成后正常退出。
首次运行若停在无障碍引导页，完成引导后才会进入主菜单。
探针 datagen 输出 `environment/build/generated/assets/forestry_environment/lang/en_us.json`。
其 JAR 在 `environment/build/libs`，仅用于环境测试。
开发运行使用 NeoForge 的开发启动器，其原生开发模式跳过 EULA 文件，不需要代写 `eula.txt`。
独立发行服务器仍需使用者阅读并确认 EULA。

正式模组生命周期回归：`./gradlew-local.bat runLifecycleTestServer --offline`，使用真实四模组和独立的 `lifecycleTest` 测试源集，
验证早期模板解码、实际世界数据、资源重载、创造分类构建，以及真实邮政投递/交易失败回滚；不替代其余原有业务测试。
本机入世探针 `runLifecycleTestClient` 打开 `run/26.2/lifecycle-client/saves/lifecycle-ready` 并截图退出，
需要事先准备该存档副本；`-PlifecycleClientAutoExit=false` 可保持窗口。正常交互客户端使用 `runAllJarsClient`。
创造物品栏回归：`./gradlew-local.bat runLifecycleTestClient -PlifecycleCreativeTest=true --offline`，
将上述独立副本中的测试玩家切换为创造模式，经 E 键绑定打开、关闭并重开物品栏，切换八个林业分类后截图退出。
成功日志为 `FORESTRY_CREATIVE_INVENTORY_READY`，截图位于 `run/26.2/lifecycle-client/screenshots/lifecycle-creative.png`；
此自动回归需保持默认的 `lifecycleClientAutoExit=true`。
电路板拆卸回归：`./gradlew-local.bat runLifecycleTestClient -PlifecycleSocketTest=true --offline`，
在上述独立副本中，通过真实 GUI 点击安装四个磷灰石电子管的精细电路板，验证光标持烙铁时左键、右键均可拆卸，仅主手或副手持烙铁时不能拆卸。
验证服务端和客户端的插槽、物品返还与耐久同步；成功日志为 `FORESTRY_SOCKET_CLIENT_READY`，截图为 `screenshots/socket-removal.png`。
`runLifecycleTestServer` 另覆盖背包满、烙铁耗尽、创造模式不消耗耐久及无烙铁时拒绝拆卸。
此规则对齐官方 ForestryCE `1.21.1` 分支提交 `ae51f88a6f1f3180a3d3bdddd842cf699cee03e7`：
[客户端点击](https://github.com/thedarkcolour/ForestryCE/blob/ae51f88a6f1f3180a3d3bdddd842cf699cee03e7/src/main/java/forestry/core/platform/gui/widgets/SocketWidget.java)
与[服务端处理](https://github.com/thedarkcolour/ForestryCE/blob/ae51f88a6f1f3180a3d3bdddd842cf699cee03e7/src/main/java/forestry/core/platform/network/packets/PacketSolderingIronClick.java)
均只读取 `containerMenu.getCarried()`。26.2 的背包返还范围与光标耐久同步修复不扩展此交互规则。
运行时模型引用报告写入 `.local/n2-lifecycle-item-models.json`，可交给下述产物检查器验证全部实际物品模型定义。

## 完整数据生成与产物检查

```powershell
# 四模组完整生成；冒号只运行根项目，不同时运行 environment 探针
.\gradlew-local.bat :runData --offline
python scripts/check-datagen-output.py src/generated --item-model-report .local/n2-lifecycle-item-models.json
```

生成到 `src/generated/resources{,_farms,_mail,_butterflies}`；特殊物品选择、染色、流体容器、告示牌几何和图集均由生成器直接输出，
不再需要旧 `migrate-n5-*.cjs` 的后处理。`--item-model-report` 可省略；使用前先运行一次 `runLifecycleTestServer` 获得实际注册报告。
检查器核对 JSON/原料格式、物种数量、配方产量、流体容器原料，以及 Forestry 模型/材质引用（包含图集别名和手写装备层纹理）。

建议先生成到隔离目录并与当前产物比较：

```powershell
.\gradlew-local.bat :runData '-PdatagenOutput=.local/datagen-review' --offline
python scripts/check-datagen-output.py .local/datagen-review --baseline src/generated
```

PowerShell 中请给包含点号的 `-PdatagenOutput=...` 整个参数加引号。
`--baseline` 会检查共同配方中的物品 ID/数量未回退；检查器不会用旧生成目录填补新产物的缺失引用。
重复 `:runData` 应显示 `written: 0`、`removed stale: 0`。生成器会清除不再输出的旧文件，手写资源应放在各模组的 `src/<模块>/resources`。
完整 `build` 不隐式执行 datagen；修改生成规则后需先运行上述命令，再构建和进行服务器/客户端回归。

## 本地 Jade（不随林业发行）

使用 [Jade 26.2.10 / NeoForge](https://modrinth.com/mod/jade/version/GBES6etT)。
安装脚本固定官方 Modrinth 下载地址并校验 SHA-512；JAR 仅缓存于 `.local/validation-mods`，
复制到忽略的运行目录，不进入 `implementation`、`runtimeOnly`、Jar-in-Jar、模组依赖声明或发行 JAR。

```powershell
.\scripts\install-local-jade.ps1
.\gradlew-local.bat runAllJarsClient --offline
```

默认安装到 `run/26.2/boot-all-client/mods` 和 `run/26.2/boot-all/mods`，本机已安装，正常启动命令不变。
下载缓存存在后可加 `-Offline` 重装；其他本地实例用 `-RunNames` 指定，例如：
`.\scripts\install-local-jade.ps1 -Offline -RunNames postal-server,postal-client`。
脚本遇到另一版 Jade 会停止，避免同时加载两个版本；本轮试用的 26.2.9 已改为 `.jar.disabled` 保留。
26.2.10 修复了试用 26.2.9 时出现的剪刀采集信息错误。
这是独立 Jade 的通用观察提示；没有将 Jade API 或专用进度提供器并入林业本体。

## 真实邮政与独立客户端验收

仅在 `run/26.2/postal-server` / `postal-client` 两个独立实例运行；不会改动正常交互客户端的存档。
专服默认只监听 `127.0.0.1:25572`，使用离线测试玩家 `PostalTest`；不要把此测试配置用于公网服务器。
测试只注册在 `lifecycleTest` 源集，四个发行 JAR 不包含测试命令或自动启动逻辑。

```powershell
# 实际投递/交易、保存退出，然后用另一个进程读取同一世界并验证内容及 dirty 回调
.\gradlew-local.bat runPostalTestServer -PpostalPhase=seed --offline
.\gradlew-local.bat runPostalTestServer -PpostalPhase=verify --offline
```

`seed` 只重置该验收世界中的专用测试收件地址；世界保存在 `postal-server/postal-acceptance`。
`seed` 与 `verify` 必须成对运行，中间不要执行 `network`：联网场景会改变同一个世界中的邮资、信箱和交易记录，
其重启检查使用 `reconnect` 及独立网络快照。邮政专服任务检查本次成功标记，断言未完成会令 Gradle 失败。
用于跨进程核对的快照为 `postal-server/postal-expected.nbt`；邮政实际数据仍由生产 SavedData 写入世界目录。

联网验收先安装 Jade 到两个测试实例，然后在两个终端分别启动：

```powershell
# 终端 A；等待 FORESTRY_POSTAL_NETWORK_LISTENING
.\gradlew-local.bat runPostalTestServer -PpostalPhase=network --offline
# 终端 B
.\gradlew-local.bat runPostalTestClient --offline
```

客户端检查真实回滚不发通知、投递提示数量、邮箱内容、主手及副手打开交易回信。
信件重开回归会先输入有效贸易站名称并确认预览，关闭后由服务端改变报价，再原样重开同一封未发送信件，
确认无需编辑收件人名称或切换收件类型，就能收到并显示新的商品数量和交易价格。
收到 `FORESTRY_POSTAL_CLIENT_READY` 才算通过；单独 `BUILD SUCCESSFUL` 不代表完成了所有客户端断言。
客户端验证后退出，专服在测试玩家离线时保存停止。随后在两个终端重启以验证保存重连提示：

```powershell
# 终端 A；等待 FORESTRY_POSTAL_NETWORK_RELOADED
.\gradlew-local.bat runPostalTestServer -PpostalPhase=reconnect --offline
# 终端 B
.\gradlew-local.bat runPostalTestClient -PpostalReconnect=true --offline
```

截图写入 `run/26.2/postal-client/screenshots`，包括收件提示、邮箱、交易回信、副手回信和重开后的交易预览。
这些测试直接使用生产物品、Capability、邮政注册表与协议；尚不覆盖手工编辑/配置的每个 GUI 控件或跨维度投递规则。

## N2 数据与存档专项验收

测试使用 `run/26.2/n2-server`、`n2-peer` 与 `n2-client`，两台专服仅监听 `127.0.0.1:25573/25574`。
所有测试命令和自动退出逻辑均在 `lifecycleTest` 源集，不进入发行 JAR。

```powershell
# 实际数据包修改/撤销重载，保存蜜蜂、树木、蝴蝶的玩家与公共繁殖记录
.\gradlew-local.bat runN2TestServer -Pn2Phase=seed --offline
# 在另一个 JVM 中读取同一世界，核对六份真实 SavedData
.\gradlew-local.bat runN2TestServer -Pn2Phase=verify --offline
# 保存带杂交个体的库存、背包、机器、树木、育幼、实体与已组装多方块
.\gradlew-local.bat runN2TestServer -Pn2Phase=world-seed --offline
# 另一 JVM 从实际区块/实体文件读回并重建控制器
.\gradlew-local.bat runN2TestServer -Pn2Phase=world-verify --offline
```

联网验收先成功执行 `world-seed`，再在三个终端启动：

```powershell
# 终端 A；等待 FORESTRY_N2_NETWORK_LISTENING
.\gradlew-local.bat runN2TestServer -Pn2Phase=network --offline
# 终端 B；等待同样的监听标记，数据包和繁殖记录独立于 A
.\gradlew-local.bat runN2PeerServer --offline
# 终端 C；实际登录、机器菜单/实体同步、重载、重连及 A→B→A 切换
.\gradlew-local.bat runN2TestClient --offline
```

成功标记为 `FORESTRY_N2_SAVE_READY`、`FORESTRY_N2_RESTART_READY`、`FORESTRY_N2_WORLD_READY`、`FORESTRY_N2_CLIENT_READY`。
重载/同步场景覆盖三类物种、等位基因、花型、突变、新增机器配方与全部 10 类 DataMap；
跨服检查使用同一个客户端进程，不由测试代码清理客户端缓存。
这些新任务在断言未完成时会令 Gradle 失败；联网专服最多等待约五分钟，成功后随测试客户端离线停止。
测试数据包保留在独立世界中供复查，成功结束时不再选用；中断后再次运行 `seed` 或 `network` 会先撤销旧测试包。
`verify` 必须在成功的 `seed` 后运行，`world-verify` 必须在成功的 `world-seed` 后运行。
联网测试会打开蒸馏器菜单并自动退出，请勿操作测试客户端。正常游玩实例不受影响。
验收范围与未完成项见 [N2 接续记录](porting/n2-status.zh-CN.md)。

## N4 核心玩法验收

N4 已通过整体验收，范围和证据见 [N4 验收报告](porting/n4-acceptance.zh-CN.md)。
47 项旧玩法测试已迁入 `src/lifecycleTest/java/forestry/gametest`，另新增 18 项 N4 场景；
与原有数据、邮政和机器回归合计 95 项，使用真实四模组运行：

```powershell
.\gradlew-local.bat :runLifecycleTestServer --offline
# 两个独立 JVM：保存玩法中途状态，再从真实区块读取并继续工作
.\gradlew-local.bat :runN4TestServer -Pn4Phase=seed --offline
.\gradlew-local.bat :runN4TestServer -Pn4Phase=verify --offline
```

专服使用独立目录 `run/26.2/n4-server`，仅监听 `127.0.0.1:25576`。
保存树苗、已授粉且育幼的树叶、茧、繁殖中的蜂箱及装有背包的箱子；成功标记为 `FORESTRY_N4_WORLD_READY`。
`verify` 比对后会继续生长与繁殖，因此每次完整重验从 `seed` 开始。任务检查本次 `.passed` 文件，断言失败不能被零退出码掩盖。
这些测试不进入生产 JAR；正常游玩世界不参与测试。原 `src/test` 中其余旧测试仍待迁移，标准 `test` 和旧 `runGameTestServer` 不代表本套验收。

## N5 真实客户端验收

覆盖范围及证据见 [N5 验收报告](porting/n5-acceptance.zh-CN.md)。
使用独立的 `run/26.2/n5-client`；第一次运行会从 `run/26.2/lifecycle-client/saves/lifecycle-ready`
复制独立测试世界，缺少该基线时明确失败。不要在这些路径放置游玩存档：夹具会重置测试玩家库存、
繁殖记录和固定区域内的方块/实体。运行期间请勿操作测试窗口。

```powershell
# 默认 OpenGL；42 个入口、全部注册菜单、物品模型和实景，资源重载后再次执行
.\gradlew-local.bat :runN5TestClient --offline
# 在支持 Vulkan 的显卡上执行相同流程，并断言实际后端没有降级
.\gradlew-local.bat :runN5TestClient -Pn5Graphics=vulkan --offline
# 仅针对物品资源修改的定向复验；不替代完整 N5 验收
.\gradlew-local.bat :runN5TestClient -Pn5Graphics=opengl -Pn5ModelsOnly=true --offline
# 补充机器几何、木材、GUI/粒子与邮件资源边界探针
.\gradlew-local.bat :environment:runClient -PenvironmentN5Models=true -PenvironmentMailAssets=true -PenvironmentAutoExit=true --offline
```

每次自动设定测试实例的语言/缩放、视距和图形后端，关闭 FML 提前加载窗口以兼容 Vulkan；
不修改正常实例设置。两轮分别使用中文/缩放 2、英文/缩放 1，并检验原生输入及服务端同步。
成功日志为 `FORESTRY_N5_CLIENT_READY`，任务同时要求本次 `n5-client.passed`，进程返回零本身不代表通过。
仅模型模式使用 `FORESTRY_N5_MODELS_READY`，完成文件也会写明 `models_only=true`。
截图在 `run/26.2/n5-client/screenshots/n5-*.png`，同名截图会覆盖；比较后端时请先分别归档。
N5 自动验收类只属于 `lifecycleTest`，不进入四个生产 JAR。

## N6 发行 JAR 与外部集成回归

结果及范围见 [N6 验收记录](porting/n6-acceptance.zh-CN.md)。`runAllJars*` 是生产源集的开发启动，
验证发行 JAR 请使用独立 `n6Test` 探针：

```powershell
.\gradlew-local.bat :build :checkN6Release :checkN6PublicationMetadata --offline
# 核心始终存在；掩码 0..7 分别叠加 蝴蝶=1、农场=2、邮件=4
.\gradlew-local.bat :runN6ReleaseServer -Pn6Modules=0 -Pn6Integrations=none --offline
.\gradlew-local.bat :runN6ReleaseClient -Pn6Modules=7 -Pn6Integrations=all -Pn6Graphics=opengl --offline
.\gradlew-local.bat :runN6ReleaseClient -Pn6Modules=7 -Pn6Integrations=all -Pn6Graphics=vulkan --offline
python scripts/n6-network-matrix.py
```

`n6Integrations` 接受 `none`、`jei`、`curios`、`all`，与普通开发任务的 `withJei` / `withCurios` 分开。
实例位于 `run/26.2/n6-{server|client}-<掩码>-<集成>[-network]`；任务管理其中的 `mods` JAR、选项及测试完成标记，
不要在这些实例放入其他模组或游玩存档。客户端首次从独立 `lifecycle-ready` 副本复制世界，然后重置测试玩家和机器。
探针断言 Forestry 从真实 JAR 加载、生产运行没有 ModKit/旧测试类，并要求本次 `n6.passed`；
日志中 `FORESTRY_N6_CLIENT_READY` / `FORESTRY_N6_SERVER_READY` 表示专项断言完成。
截图同名覆盖，比较后端前应归档。六组联网测试保存结果到 `.local/n6-network-results.json`；
Curios 16 双端安装不一致时，上游通道协商会拒绝连接，测试核对拒绝项仅属于 `curios`。

上述任务仍使用 NeoForge 开发启动器。普通服务器单独使用官方安装器生成的
`.local/n6-production/runtime` 和 `scripts/n6-production-acceptance.py`，
`prepare` 冻结四个 JAR、记录 SHA-256 并准备十个实例，不写入 EULA 同意。
只有使用者同意 [Minecraft EULA](https://www.minecraft.net/eula) 并授权这组实例后，才执行 `run --accept-eula`。
该流程已在使用者明确授权这十个本地实例后完成，二十轮原生生产启动、真实 `/reload`、保存及重启全部通过。
`python scripts/n6-production-acceptance.py audit` 要求完整二十轮结果，复核实际模组/数据集合、运行模式与产物 SHA-256，
输出独立 `.local/n6-production/audit.json`。已知 Windows 性能计数器及上游原生网络探测诊断见验收记录。

## 隔离目录与 IDE

| 用途 | 26.2 | 保留的 1.21.1 |
| --- | --- | --- |
| JDK | `.local/jdk25` | `.local/jdk21` |
| Gradle user home | `.local/gradle-26.2` | `.local/gradle` |
| 本地启动器项目缓存 | `.local/project-cache-26.2` | `.gradle` |
| Forestry 构建输出 | `build/26.2` | `build` 内既有旧输出 |
| 游戏运行目录 | `run/26.2/<任务>` | `run` 内既有旧实例 |
| 探针运行目录 | `run/26.2/environment/<任务>` | 无 |

`gradlew-local.bat` 仅修改当前进程环境变量，不修改系统 Java。
`1.21.1-neoforge` 分支及本地 JDK 21 保持原状，切回该分支时使用其自身脚本。
不要把旧 `build/libs` 的 1.21.1 JAR 当作 26.2 产物。

IDEA 导入根 `settings.gradle`；Project SDK 和 Gradle JVM 选择 `.local/jdk25`，
Gradle user home 选择 `.local/gradle-26.2`，使用项目 Wrapper。
命令行通过启动器固定项目缓存；IDE 若需要完全分离项目缓存，在 Gradle 参数中同样设置
`--project-cache-dir <仓库绝对路径>/.local/project-cache-26.2`。
先运行 `environment` 的任务检查环境，再开展根项目的 API 移植。

## 源集与功能缺口

G2 已通过；验收范围、上游补查及三项暂时关闭后的恢复步骤见 [G2 验收记录](porting/g2-acceptance.zh-CN.md)。
运行 `python scripts/check-g2-upstream.py` 可生成带查询时间和 SHA-256 的官方渠道快照；
返回 0 仅表示查询完整，不代表找到了兼容依赖或 G2 已通过。网络/格式错误会明确失败，不当作“没有发布物”。
`checkG2OptionalIsolation` 已加入 `build`，检查 20 个类路径的传递依赖和最终四包的字节码、资源、服务及元数据；
报告写入 `build/26.2/reports/g2/optional-isolation.json`。未来恢复适配器时应同步调整当前禁用项的门禁并补验缺失/安装两条路径。

四个发行模块仍是 `main`、`butterflies`、`farms`、`mail`，mod ID 保持不变。
JEI、Curios 保持独立源集并始终进入对应生产 JAR；开发运行时开关只决定是否安装外部模组，不改变发行包内容。
Patchouli/KubeJS 适配器与 ModKit 不进入默认生产编译图。
`checkPortingIsolation` 检查生产源码的直接 API 引用及依赖声明；
`checkJarPartition` 继续负责四 JAR 的归属、开发代码泄漏及语言文件校验。

| 能力 | 源集 / 当前状态 | 恢复条件 |
| --- | --- | --- |
| JEI | `mainJei`、`butterfliesJei`、`farmsJei`、`mailJei`；始终打包 | 固定 `30.39.0.233`；`-PwithJei=true` 将外部 JEI 加入开发运行时；发行包已验证分类、布局、子类型、真实按钮填充和重载 |
| Curios | `curios`；始终打包；原生头部装备路径保留 | 固定 `16.0.0+26.2`；`-PwithCurios=true` 将外部 Curios 加入开发运行时；已验证功能槽/装饰槽、玩家模型、高亮和重载 |
| Patchouli | `patchouli`；未配置目标 API；源码及服务提供者保留 | 确认 26.2 NeoForge 发布物、版本约束与校验值，再接回适配器及书籍资源 |
| KubeJS / Rhino | `kubejs`；未配置目标 API；默认资源不含旧插件入口 | 确认 26.2 依赖图，再恢复入口资源、Forestry 插件服务及脚本回归 |
| ModKit | 仅 `datagen`；固定上游提交 `8e87f1ea8aaad70b3ff1bbe3e604c535751b3401` | 完整生成已恢复，四根输出保留；2×2 多产出配方显式构造以绕过该提交的数量丢失问题；普通游戏启动不加载此源集 |
| Ponder / Architectury | 移出默认依赖图 | 当前未发现直接 Java 引用；未来有实际使用需求再评估 |
| 旧 Access Transformer | 保存在 `docs/porting/accesstransformer-1.21.1.cfg`；目标文件仅恢复已验证的结构池三个字段 | 按 26.2 字段、方法逐项恢复，保持 AT 校验开启 |
| Forestry 数据 / 模型 / GUI | N2–N6 及调整范围后的 G2 已验收，完整 datagen、双后端 GUI/模型/实景及集成/发行回归通过 | 三项不可验证的外部集成已暂时关闭；取得目标依赖后重新打开相应项，不阻塞后续移植 |
| Forestry GameTest | 原 `src/test` 文件由 `gametest` 源集承载，普通客户端及标准 `test` 不加载 | 迁移至 26.2 测试注册及数据 API，再运行业务回归 |

不存在目标 API 的 Patchouli/KubeJS 开关或编译任务会明确失败，不使用旧版 JAR 或空实现填补。
手册初始化通过 Forestry 自有服务接口发现实现；缺失适配器时沿用缺失手册服务。
Curios 通过独立服务提供者加入额外装备检查；无 Curios 时该提供者安全返回缺失。
未恢复的 Patchouli 书籍入口、页面及 KubeJS 服务入口均不进入生产资源，源码仍保留用于后续恢复。
当前 `datagen` 汇集四模块生成器，保留原文件位置，未保持旧生成器之间的编译边界；
发行内容源集之间仍只依赖核心。可在后续生成器迁移中进一步按模块拆分。

## 后续移植入口

```powershell
# 暴露真实的 26.2 API 编译问题，不跳过旧源码
.\gradlew-local.bat :compileJava
# N1 模块依赖回归；独立于尚未完成迁移的 Minecraft 代码
.\gradlew-local.bat testModuleLoadOrder --offline
# 展开完整诊断，避免默认 400 个错误的上限掩盖问题
.\gradlew-local.bat :compileJava -PportingMaxErrors=10000
# 普通游戏与完整 datagen 已可运行；旧 GameTest 源集仍需迁移
.\gradlew-local.bat runServer
.\gradlew-local.bat runClient
.\gradlew-local.bat :runData
.\gradlew-local.bat runGameTestServer
.\gradlew-local.bat build
```

`:runData` 采用 26.2 MDK 的 `clientData()`，使用独立 datagen 源集并同时生成各模块数据与客户端资源。
生成器入口 mod ID 为 `forestrydatagen`，ModKit 与合并的生成器服务清单只在开发运行中加载。
标准 Gradle `test` 当前没有 JUnit 测试；已迁移的 95 项真实业务测试使用 `runLifecycleTestServer`，旧 `runGameTestServer` 不代表本轮迁移已完成的测试集。
四 JAR 打包、资源合并与检查任务保留，输出在 `build/26.2/libs`。
本分支发布标签保持 alpha；路线图 G2 已按用户调整后的范围通过，三项外部暂时关闭不表示集成功能已经恢复。

本次验证结果及日志位置见 [26.2 环境验证记录](phase2-environment-audit.zh-CN.md)。
后续注册与启动改动、验证边界及剩余阻碍见 [N1 接续记录](porting/n1-status.zh-CN.md)。

数据加载、配方同步与持久化的接续进展见 [N2 接续记录](porting/n2-status.zh-CN.md)。
可运行 `gradlew-local.bat testN2Data --offline` 验证独立的生产数据工具；
`gradlew-local.bat :environment:runGameTestServer --offline` 还会运行三个 N2 数据测试。
这些测试不代替真实 Forestry 专用服务器、物种/机器和完整存档的验收。
