# OfflineSkinInjector (OSI)

[**English**](README.md) · 中文

![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)
![Minecraft: 1.7.10 to 26.x](https://img.shields.io/badge/Minecraft-1.7.10%20to%2026.x-62b47a)
![Java: 8+](https://img.shields.io/badge/Java-8%2B-orange)

通过运行时修改 authlib 实现修改客户端皮肤、披风和鞘翅的显示。可跨版本、跨 Mod 加载器工作。

**这是个注入器，不是资源包**：它会在运行时修改游戏的行为，而不是简单替换玩家的贴图。并非所有版本都支持此功能。

## 特性

- **皮肤 + 披风**：设置 64x64 或 64x32 皮肤，可选 64x32 披风。
- **鞘翅自动复用披风**：配置披风后穿鞘翅，客户端会自动用披风材质渲染鞘翅。
- **模型自动识别**：按手臂 UV 布局自动判断 classic（4px）/ slim（3px），也可显式覆盖。
- **只换自己 / 换所有人**：`self`（默认）只替换本地玩家，`all` 替换客户端渲染到的所有玩家。
- **按版本自动转换**：1.7.x 发 64x32，1.8+ 发 64x64，自动镜像/裁剪。

## 环境要求

- Minecraft Java 版 **1.7.10 ~ 最新**。
- 能运行游戏的 JVM（Java 8+）；agent 本身是 Java 8 字节码。
- `offlineskininjector.jar`。

## 快速开始

### 1. 构建 jar

在仓库根目录执行（需要 JDK 17）：

```bat
gradlew.bat :app:shadowJar
```

macOS / Linux：

```bash
./gradlew :app:shadowJar
```

产物：`app/build/libs/offlineskininjector.jar`。

### 2. 准备图片

- **皮肤**：PNG，必须是 **64x64**（现代）或 **64x32**（旧版）。
- **披风**：可选，PNG，必须是 **64x32**。同一张图也会被客户端自动用于鞘翅。

### 3. 添加 JVM 参数

在启动器的 **JVM 参数** 里填写，不要填到游戏参数/窗口参数里：

```
-javaagent:\path\to\offlineskininjector.jar
-Dofflineskininjector.skin=\path\to\skin.png
-Dofflineskininjector.cape=\path\to\cape.png
-Dofflineskininjector.mode=self
-Dofflineskininjector.model=auto
```

- 官方启动器：编辑配置 → 更多选项 → JVM 参数。
- HMCL：实例设置 → Java → JVM 参数。
- PCL2：设置 → Java → JVM 参数。

披风和后两项都是可选的。

### 4. 启动游戏并查看日志

正常时能看到：

```
[OfflineSkinInjector] Successfully initialize injector.
[OfflineSkinInjector]   skin     = C:\path\to\skin.png
[OfflineSkinInjector]   source   = 64x64
[OfflineSkinInjector]   format   = 64x64 (auto, client 1.21.11)
[OfflineSkinInjector]   url      = http://127.0.0.1:62841/skin/8afa....png
[OfflineSkinInjector]   cape     = <none>
[OfflineSkinInjector]   mode     = self
[OfflineSkinInjector]   model    = classic (auto, 4px arms)
[OfflineSkinInjector]   uuid     = ...
[OfflineSkinInjector]   name     = ...
```

如果显示 `disabled: ...`，见[常见问题](#常见问题)。

## JVM 参数

| 参数 | 默认值 | 说明 |
|---|---|---|
| `-Dofflineskininjector.skin` | —（必填） | 皮肤 PNG 路径，必须是 64x64 或 64x32。 |
| `-Dofflineskininjector.cape` | 无 | 披风 PNG 路径，必须是 64x32。文件缺失或损坏时只禁用披风。 |
| `-Dofflineskininjector.mode` | `self` | `self` 只替换本地玩家；`all` 替换客户端渲染到的所有玩家。 |
| `-Dofflineskininjector.model` | `auto` | `auto` / `classic` / `slim`。auto 从 64x64 皮肤的手臂 UV 布局识别 4px/3px；64x32 恒为 classic。 |
| `-Dofflineskininjector.format` | `auto` | 发给客户端的 PNG 布局：`auto`、`64x64`、`64x32`。 |
| `-Dofflineskininjector.uuid` | 自动 | 本地玩家 UUID，32 位十六进制，可带横线。自动从启动器的 `--uuid` 读取。 |
| `-Dofflineskininjector.name` | 自动 | 本地玩家名。自动从 `--username` 读取；只在没有 UUID 时用于匹配。 |
| `-Dofflineskininjector.debug` | `false` | 设为 `true` 时打印失败堆栈。 |

> 如果既取不到 UUID 也取不到名字，`self` 模式不会生效。可以显式设置 `-Dofflineskininjector.uuid=` / `.name=`，或者改用 `mode=all`。

## 皮肤格式

### 皮肤

| 客户端 | 输入 64x32 | 输入 64x64 |
|---|---|---|
| 1.7.x 及更早 | 原样发送 | 裁成 64x32（保留上半部分） |
| 1.8+ | 补成 64x64，左臂/左腿镜像右臂/右腿 | 原样发送 |

`format=64x64` / `format=64x32` 可以强制固定格式；`auto` 会从启动器的 `--version`、`--assetIndex` 或 classpath 里的 `versions/<id>` 判断客户端版本。

注意：

- 64x32 皮肤一定是 **classic**（4 像素手臂）。
- 64x64 皮肤默认由 `model=auto` 识别：classic 的手臂面铺满手臂块，slim（3 像素）会在
  每个手臂块右侧留下 2 像素未使用区；未使用区透明（或四个区全黑）即判 slim。
- 如果 slim 皮肤把未使用区填成了不透明颜色，识别会回落 classic；用
  `-Dofflineskininjector.model=slim` 显式指定。
- 1.7.x 没有 slim 模型，64x64 slim 皮肤在 1.7.x 上会错位并打警告。
- 不支持 HD 皮肤（128x128、256x256 等）。

### 披风

- 披风固定 **64x32** 布局，不支持 HD。
- 披风是可选的。
- 披风文件损坏/缺失只会禁用披风，皮肤照常生效。
- 鞘翅不需要单独设置，会自动使用披风材质。
- 不支持让披风和鞘翅各用一张图。

## 支持的版本

| Minecraft | authlib | 钩子 | 验证情况 |
|---|---|---|---|
| 1.6.2 及更早 | 无 | — | 不支持 |
| 1.7.10 | 1.5.21 | `getTextures` | 运行时测试 + 调用点扫描 |
| 1.8.3 | 1.5.21 | `getTextures` | 反编译 |
| 1.12.2 / 1.13 / 1.13.2 | 1.5.25 | `getTextures` | 调用点扫描 |
| 1.16.5 | 2.1.28 | `getTextures` | 运行时测试 |
| 1.19.2 | 3.11.49 | `getTextures` | 运行时测试 |
| 1.19.3 – 1.21.10 | 新世代 | packed | 预期可用，同一 authlib 世代 |
| 1.21.11 | 7.0.61 | `getPackedTextures` + `unpackTextures` | 运行时测试 |
| 26.3 | 10.0.77 | `getPackedTextures` + `unpackTextures` | 运行时测试 |

同一 authlib 世代内的版本使用相同的入口方法，即使没有逐版本列出也应当可用。

## 工作原理

```
-javaagent:offlineskininjector.jar
        │
     premain
        ├─ 读取 -Dofflineskininjector.*，校验 PNG
        ├─ 解析 model=auto（64x64 按手臂 UV 判定 4px/3px）
        ├─ 按版本把皮肤转成 64x64 / 64x32，算 SHA-1
        ├─ 启动只监听 127.0.0.1 的 HTTP 服务
        │     /skin/<sha1>.png、/cape/<sha1>.png
        └─ 给 authlib 安装 ASM transformer
```

1. agent 在游戏窗口出现前启动一个只绑定 `127.0.0.1` 随机端口的迷你 HTTP 服务，把皮肤和披风发布成带内容 SHA-1 的 URL。
2. transformer 在 authlib 会话服务具体类的入口注入早退：
   - authlib 1.5–3.x：`getTextures(GameProfile, boolean)` 直接返回我们的 `Map<Type, MinecraftProfileTexture>`。
   - authlib 7.x–10.x：`getPackedTextures(GameProfile)` 返回我们的 `Property`；`unpackTextures(Property)` 返回构造好的 `MinecraftProfileTextures`，状态标成 `SIGNED`。这同时绕过了 authlib 的 localhost 域名白名单和 `secure` 过滤。
3. 原版客户端仍走自己的 `SkinManager` 下载/缓存管线，我们只提供一个 URL。
4. 所有 authlib 类型都通过反射访问；ASM 被 shade 并 relocate 到 agent 内部，避免和 Forge / launchwrapper 自带的老 ASM 冲突。

## 常见问题

| 现象 | 排查 |
|---|---|
| `disabled: -Dofflineskininjector.skin=<file.png> is required` | 属性没写或拼错；它必须是带 `-D` 的 JVM 参数。 |
| `skin file not found` | 路径不对，或含空格但没有加引号。 |
| `skin must be 64x64 or 64x32` | 换受支持的尺寸；HD 皮肤会被拒绝。 |
| agent 初始化成功，但皮肤没变 | `self` 模式没识别出本地玩家。看日志里的 `uuid = ...`；设置 `-Dofflineskininjector.uuid=` 或改用 `mode=all`。 |
| 换了 PNG 还是旧皮肤 | 重启游戏。仍不行就删除 `.minecraft/assets/skins`（现代版本）或 `.minecraft/skins`（1.7.x）下的缓存。 |
| 皮肤生效，披风不显示 | 披风必须是 64x32；看日志里有没有 `cape disabled`。 |
| 穿鞘翅显示原版灰色翅膀 | 披风没生效或客户端关闭了披风显示时，鞘翅会回退到默认纹理；先按上一条检查日志里的 `cape` / `cape url`。 |
| slim 手臂错位 | 先看日志的 `model` 是否识别成 slim（`auto, 3px arms`）。识别失败时显式加 `-Dofflineskininjector.model=slim`；64x32 皮肤只能是 classic，1.7.x 根本没有 slim 模型。 |
| 别的玩家看我还是原皮肤 | 正常——这是客户端改动。`mode=all` 只改变你看到别人时的样子，不改变别人看到你的样子。 |
| 游戏起不来或报 `VerifyError` | 检查有没有同样改写 authlib 的皮肤 Mod（如 CustomSkinLoader），两者二选一；反馈时加 `-Dofflineskininjector.debug=true`。 |
| 1.6.2 及更早完全没反应 | 1.6.2 没有 authlib，没有可钩的目标，不支持。 |
| 代理环境下本地 HTTP 失败 | 加 `-Dhttp.nonProxyHosts=localhost\|127.0.0.1`（启动器要求时给值加引号）。 |

## 开发

标准 Gradle 多项目构建（agent 在 `app` 模块）。构建需要 JDK 17，产物是 Java 8 字节码，Gradle 8.14 由 wrapper 提供。

```
app/src/main/java/github/naivg/offlineskininjector/
  Premain.java             agent 入口与启动流程
  Config.java              系统属性与启动器参数解析
  SourceTexture.java       PNG 解码与尺寸校验
  SkinConverter.java       64x64 <-> 64x32 转换与 alpha 规则
  SkinModel.java           AUTO / CLASSIC / SLIM 模型枚举
  SkinModelDetector.java   4px / 3px 手臂布局自动识别
  ClientVersion.java       --version / --assetIndex / classpath 识别
  TextureSet.java          纹理 URL 与 base64 payload
  TextureHttpServer.java   回环 HTTP 服务
  AuthlibTransformer.java  ASM 早退注入
  Hooks.java               面向 authlib 的钩子（纯反射）
  Reflect.java             反射辅助
app/src/test/java/...      单元测试
tools/                     开发与验证工具
```

运行单元测试：

```bat
gradlew.bat :app:test
```

macOS / Linux 用 `./gradlew :app:test`。

覆盖配置解析、PNG 校验、格式转换、模型自动识别、客户端版本识别和 textures payload。

## 限制

- 只影响客户端；别的玩家看不到你的自定义皮肤。
- 鞘翅自动复用披风材质；不能单独指定鞘翅贴图。
- 不支持 HD 皮肤/披风。
- 不支持热更新，换图后要重启客户端。
- 与同样钩 authlib 的皮肤 Mod 可能冲突，二选一。
- 不支持 1.6.2 及更早版本。

## 许可证

GNU General Public License v3.0，见 [LICENSE](LICENSE)。

如果你使用了本项目的代码或在启动器中集成了本项目，请务必在 `README` 或 `关于` 中提及。

Minecraft 是 Mojang Studios 的商标。本项目为非官方项目，与 Mojang / Microsoft 无关。
