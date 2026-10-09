# 第一阶段（Patchouli 解耦）验收报告

日期：2026-10-09。依据：[移植路线图第 4 节、G1](porting-roadmap.zh-CN.md)，并已读取引用聊天“配置 NeoForge 1.21.1 本地环境”。对象是 `1.21.1-neoforge` 分支 HEAD `70cc3f62135ffc7b0784481727284338aeda5e38` 加当前工作区改动。

**结论：第一阶段 G1 通过。** 初次审计发现的测试失败及实际页面错误均已修复。同一套最终发行 JAR 的客户端／专用服务器矩阵通过；Patchouli 双端安装状态必须一致的上游握手限制已实测并记录，按路线图不要求绕过。

## 修复与原因

1. 基因基线只存在 CRLF／LF 差异。`GenomeBaselineTest` 对预期文本归一化换行，不重写基因数据。
2. 创造模式基线还包含 `forestry:pulsating_dust`。已核实历史提交 `b365ffb21a61105e94b057bf3fff8c164fee2046` 明确移除该物品，因此仅删除该过期行，不盲目刷新整份基线。
3. 加强无提供者手册 GameTest：断言动作栏使用可翻译键、每次调用恰好一个提示、零次成功声音调用、数量仍为 1。有提供者的 FakePlayer 路径仍跳过 GUI，真实开书由客户端检查。
4. 真实页面暴露三个过期配方 ID：改为 `thermionic_fabricator`、`apatite_from_apatite_block`、`bronze_ingot_from_bronze_block`。发行包内共 67 个 Forestry 书籍配方引用均可解析。
5. 核心单装实测出现整本书 `Loading error`：邮件条目引用了不在核心包的邮件分类。将 3 个邮件条目、农场总览条目及 2 个农场分类从核心资源移至所属 `mail`／`farms` 源集。核心单装重测已能右键开书并显示木工机页面；全装内容保留。

## 构建与自动化回归

环境：JetBrains JDK 21、Gradle 9.2.1、Minecraft 1.21.1、NeoForge 21.1.230、Patchouli `1.21.1-93-NEOFORGE`。

| 检查 | 结果 | 证据 |
| --- | --- | --- |
| 最终 `gradlew-local.bat --offline build` | 通过，1 分 22 秒；增量构建 | `.local/g1-final-partition-build.log` |
| 无 Patchouli 全量 GameTest | 177／177 必需测试通过 | `.local/g1-fixed-tests-false.log` |
| 有 Patchouli 全量 GameTest | 177／177 必需测试通过 | `.local/g1-fixed-tests-true.log` |
| Patchouli 字节码边界 | 2,137 类通过，仅兼容包可直接引用 API | 最终 build 日志 |
| API、核心层次、发行分区、资源类名 | 通过；19 个资源类名可解析 | 最终 build 日志 |
| 发行包审计 | OPTIONAL、兼容范围保留；无捆绑 Patchouli API／实现；书籍分类与配方完整 | `.local/g1-production/audit.json` |
| `git diff --check` | 通过 | 最终工作区检查 |

GameTest 运行于修复 Java 测试后，随后书籍 JSON 修复及资源搬迁由最终构建、发行包审计和真实客户端复验覆盖。标准 `build` 不等于 GameTest。原有 Javadoc／Gradle 弃用警告仍存在，platform → content 的 28 个引用仍按原规则报告，不能据此认为平台层已完全解耦。

## 最终发行实例矩阵

八个游戏目录相互独立，使用官方安装器创建的普通 NeoForge `forgeclient`／`forgeserver` 运行环境。加载冻结的发行 JAR，没有开发 classes、ModKit 或 GameTest 模组。仅测试 Patchouli 开关，保留 Architectury、Rhino、KubeJS、Curios、Ponder、JEI 和 Ponder 所需 Flywheel 运行依赖。

| 场景 | 专用服务器 | 客户端进入世界 | 手册 |
| --- | --- | --- | --- |
| 核心，无 Patchouli | 通过 | 通过 | 中文动作栏提示正常，数量 1 |
| 四模块，无 Patchouli | 通过 | 通过 | 英文动作栏提示正常，数量 1 |
| 核心，有 Patchouli | 通过 | 通过 | 右键开书、正文、木工机页正常，数量 1 |
| 四模块，有 Patchouli | 通过 | 通过 | 右键开书、指定页面和重载均通过，数量 1 |

所有实例只用于本地验收，服务器绑定 `127.0.0.1`。用户已同意仅在这些实例设置 `eula=true`。测试身份 `G1Tester` 无账户凭据。

最终全装实例于 17:28 完成服务端 `/reload` 和客户端 `F3+T`，日志确认实际资源重载。重载后检查正文、木工机硬化／浸渍机壳、热电子制造机锡／铜电子管、流体组件（Water，5000／10000 mB）、农场布局图、修复后的机器合成配方和邮件信件页，均正常显示，无书籍编译、配方缺失或 Patchouli 类缺失异常。

提示次数和无成功声音由 GameTest 精确断言；真实客户端确认中英文本地化显示及物品数量，没有将截图当作音频检测。成功开书路径保留原声音调用。本地超平坦测试配置首次建世界记录了原版 `No key layers in MapLike[{}]` 后回退默认层，四组随后均 `Done` 并成功进服；该记录不属于手册错误，报告不声称日志完全无警告或错误。

最终截图与数量记录位于 `.local/g1-production`：

| 内容 | 证据 |
| --- | --- |
| 核心单装首页 | `client-core-with/screenshots/2026-10-09_17.24.09.png` |
| 中文／英文缺失提示 | `manual-missing-zh-final.png`、`manual-missing-en-final.png` |
| 全装首页 | `client-all-with/screenshots/2026-10-09_17.28.19.png` |
| 重载后木工机与流体 | `client-all-with/screenshots/2026-10-09_17.29.29.png` |
| 重载后电子管配方 | `client-all-with/screenshots/2026-10-09_17.29.48.png` |
| 重载后农场布局 | `client-all-with/screenshots/2026-10-09_17.30.06.png` |
| 修复后的机器配方 | `client-all-with/screenshots/2026-10-09_17.30.21.png` |
| 邮件信件页 | `client-all-with/screenshots/2026-10-09_17.31.02.png` |
| 四组手册数量 | `manual-count-{scenario}.txt`，均为 1 |

## 非对称安装的上游限制

两个方向均已在真实发行客户端复现进入世界前的连接拒绝：

- 仅客户端安装 Patchouli：诊断指出服务器缺少客户端要求的 `patchouli:open_book`／`patchouli:reload_books` 通道。
- 仅服务器安装 Patchouli：诊断指出客户端缺少服务器要求的相同通道。

本地 Patchouli 93 字节码显示注册载荷时没有调用 `PayloadRegistrar.optional()`；NeoForge 默认将这些通道视为必需。该诊断来自 Patchouli 自身握手，符合路线图允许记录上游限制的条件，不是 Forestry 右键手册后崩溃。Forestry 不承诺绕过该握手，也没有用自定义协商掩盖限制。非对称测试在最后一次纯资源归属修复前完成，网络代码与 Patchouli 版本未变。

证据：`.local/g1-production/client-only-patchouli.log`、`client-only-patchouli.png`、`server-only-patchouli.log`，以及 `before-partition-fix/client-all-without/screenshots/2026-10-09_16.27.21.png`。

## 最终冻结产物

目录：`.local/g1-production/artifacts`；版本均为 `1.21.1-3.0.0-alpha8`。每次启动及审计均验证实例 JAR 哈希与冻结值一致。

| JAR 前缀 | SHA-256 |
| --- | --- |
| forestry | `b907052a9a0953202af1dcae5629413863411a3a0efb1be0060305bb1cd0d05d` |
| forestrybutterflies | `4771db417da7190ab622cd6e7860fc1edde6de4b31427a8f787e44ac575375df` |
| forestryfarms | `6350b5a50eaee756f25902203894546bf273e1f7f03852861d9529e0efbebd46` |
| forestrymail | `ec497bf4f2f71b366287646e5afc0bc2173f5f6d4016863a62414db635afa026` |

## 复现与证据范围

[G1 验收操作说明](g1-acceptance-runbook.zh-CN.md)包含构建、两组 GameTest、隔离生产实例、页面定位、重载及停服命令。`scripts/g1-acceptance.py` 提供安装、冻结、启动、RCON 和静态审计；现有 Computer Use 已能操作游戏，因此无需另建 MCP 服务。

最终日志位于 `.local/g1-production/{client,server}-{scenario}/logs/latest.log`，截图位于对应客户端的 `screenshots`。`before-partition-fix` 保存上一轮证据；`*-before-book-fix` 保存配方修复前的诊断，不能当作最终通过日志。所有 `.local` 产物均不随 Git 提交。

验收结束后已正常退出四个客户端，并通过 `stop` 保存和关闭四个专用服务器。

本报告只验收 G1 及列出的回归，不代表全部机器、所有书籍条目、多人性能或后续 NeoForge 26.2／Fabric 阶段已经完成。发布配置的可选依赖和 NeoForge／Java 21 标签已检查，本次没有发布远端版本。
