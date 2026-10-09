# G1 本地发行包验收复现

验收结果见 [第一阶段验收报告](phase1-acceptance-audit.zh-CN.md)。本工具面向 Windows、Python 3.11+ 和项目本地 JDK 21，依赖已完成本地开发环境配置及一次客户端资源准备。使用普通 NeoForge 发行启动目标，不加载开发 classes、ModKit 或 GameTest 模组。

## 准备固定发行包

在仓库根目录执行；首次安装及缺失库下载需要联网：

```powershell
.\gradlew-local.bat build
python scripts/g1-acceptance.py install
python scripts/g1-acceptance.py prepare
python scripts/g1-acceptance.py audit
```

`install` 校验固定的 NeoForge 21.1.230 安装器 SHA-256，安装到 `.local/g1-production/runtime`。Minecraft 客户端 JAR、版本清单和 assets 复用本地 NeoForm 缓存；缺少它们时先运行开发客户端准备资源。`prepare` 冻结四个游戏 JAR 及 SHA-256，建立八个独立游戏目录，保留默认第三方运行依赖。Patchouli 版本为 `1.21.1-93-NEOFORGE`。

再次构建的 JAR 若不同，`prepare` 会拒绝覆盖已有快照。使用新目录时，每条命令加相同的 `--root .local/g1-new`，并从 `install` 开始。不要在服务器或客户端仍运行时替换其文件。

## 实例矩阵

| scenario | Forestry 模块 | Patchouli | 游戏端口 | RCON 端口 |
| --- | --- | --- | --- | --- |
| core-without | 核心 | 无 | 25581 | 25591 |
| all-without | 全部四个 | 无 | 25582 | 25592 |
| core-with | 核心 | 有 | 25583 | 25593 |
| all-with | 全部四个 | 有 | 25584 | 25594 |

只有阅读并同意 Minecraft EULA 后才使用 `--accept-eula`。本次用户授权仅适用于本地验收实例。服务器绑定 `127.0.0.1`，离线测试身份为 `G1Tester`；不要将这些配置用于公网服务器。

```powershell
python scripts/g1-acceptance.py start-server --scenario all-with --accept-eula
python scripts/g1-acceptance.py start-client --scenario all-with --connect all-with
python scripts/g1-acceptance.py command --scenario all-with --command 'give G1Tester forestry:foresters_manual 1'
python scripts/g1-acceptance.py command --scenario all-with --command 'data get entity G1Tester SelectedItem'
```

每组等待服务端 `Done`，客户端进入世界后右键手册。无 Patchouli 时检查动作栏提示、物品数量和异常日志；可在关闭客户端时修改其 `options.txt` 中的 `lang:zh_cn`，再启动检查中文。

有 Patchouli 时检查正文及以下条目。命令可直接定位页面，但仍须先验证真实右键开书：

```powershell
python scripts/g1-acceptance.py command --scenario all-with --command 'open-patchouli-book G1Tester forestry:foresters_manual forestry:core/casings 0'
python scripts/g1-acceptance.py command --scenario all-with --command 'open-patchouli-book G1Tester forestry:foresters_manual forestry:core/tubes 0'
python scripts/g1-acceptance.py command --scenario all-with --command 'open-patchouli-book G1Tester forestry:foresters_manual forestry:core/machines/fabricator 0'
python scripts/g1-acceptance.py command --scenario all-with --command 'open-patchouli-book G1Tester forestry:foresters_manual forestry:farming/farm_layout 0'
python scripts/g1-acceptance.py command --scenario all-with --command reload
```

悬停木工机页面的水槽检查流体名称和容量；关闭手册后按 `F3+T` 重载客户端资源，再检查页面。中文输入法可能截获快捷键，应切到英文输入状态，并在日志确认资源重载确实完成。`F2` 截图保存在相应客户端目录的 `screenshots` 下。

非对称测试可将 `--scenario core-with --connect core-without` 或 `--scenario all-without --connect all-with` 组合使用，保存 Patchouli 通道缺失诊断。Patchouli 93 拒绝这两种连接；Forestry 不绕过其握手。

退出客户端后正常停止对应服务器：

```powershell
python scripts/g1-acceptance.py command --scenario all-with --command stop
python scripts/g1-acceptance.py audit
```

`audit.json` 记录发行包哈希、实例模组清单、世界进入记录、书籍分类完整性及配方引用。静态审计不能代替 GUI 检查。所有世界、日志、快照和截图位于被 Git 忽略的 `.local` 下。

## 自动化回归

```powershell
.\gradlew-local.bat --offline -I scripts/g1-gametest.init.gradle -PwithPatchouli=false runGameTestServer
.\gradlew-local.bat --offline -I scripts/g1-gametest.init.gradle -PwithPatchouli=true runGameTestServer
```

初始化脚本隔离两组 GameTest 目录，不覆盖 `run`。每组应通过 177 项必需测试。正常开书和自定义页面仍由真实客户端验收；有提供者时 FakePlayer 不执行真实开书。
