# OfflineSkinInjector (OSI)

[**中文**](README.ZH.md) · English

![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)
![Minecraft: 1.7.10 to 26.x](https://img.shields.io/badge/Minecraft-1.7.10%20to%2026.x-62b47a)
![Java: 8+](https://img.shields.io/badge/Java-8%2B-orange)

Modify the authlib implementation at runtime to customize the appearance of skins, capes and elytra in client. Works across versions and mod loaders.

**THIS IS AN INJECTOR, NOT RESOURCE PACK**: it modifies the game's behavior at runtime, rather than simply replacing the player's textures. Not all versions supports this.

## Features

- **Skin and cape** — set a 64x64 or 64x32 skin, plus an optional 64x32 cape.
- **Elytra reuse** — with a cape configured, the client automatically textures elytra with the cape.
- **Automatic model detection** — classic (4px) vs slim (3px) is inferred from the arm UV layout, with an explicit override.
- **Local player or everyone** — replace only yourself (`self`, default) or every profile your client renders (`all`).
- **Version-aware conversion** — 64x32 for 1.7.x, 64x64 for 1.8+, converted automatically.

## Requirements

- Minecraft: Java Edition **1.7.10~latest**.
- The JVM that runs the game (Java 8 or newer). The agent itself is Java 8 bytecode.
- `offlineskininjector.jar`.

## Quick start

### 1. Build the jar

From the repository root, with JDK 17 installed:

```bat
gradlew.bat :app:shadowJar
```

On macOS / Linux:

```bash
./gradlew :app:shadowJar
```

The jar is written to `app/build/libs/offlineskininjector.jar`.

### 2. Prepare the textures

- **Skin** — a PNG that is exactly **64x64** (modern) or **64x32** (legacy).
- **Cape** — optional, a PNG that is exactly **64x32**. The client also reuses this texture for elytra.

### 3. Add the JVM arguments

Put these in the launcher's **JVM arguments** field, not in the game/window arguments:

```
-javaagent:\path\to\offlineskininjector.jar
-Dofflineskininjector.skin=\path\to\skin.png
-Dofflineskininjector.cape=\path\to\cape.png
-Dofflineskininjector.mode=self
-Dofflineskininjector.model=auto
```

- Official launcher: Edit profile → More options → JVM arguments.
- HMCL: instance settings → Java → JVM arguments.
- PCL2: Settings → Java → JVM arguments.

The cape and the last two options are optional.

### 4. Start the game and check the log

You should see:

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

If it says `disabled: ...`, see [Troubleshooting](#troubleshooting).

## JVM arguments

| args | Default | Description |
|---|---|---|
| `-Dofflineskininjector.skin` | — (required) | Path to the skin PNG. Must be 64x64 or 64x32. |
| `-Dofflineskininjector.cape` | none | Path to the cape PNG. Must be 64x32. If the file is missing or invalid, only the cape is disabled. |
| `-Dofflineskininjector.mode` | `self` | `self` replaces only the local player. `all` replaces every profile the client renders, including other players. |
| `-Dofflineskininjector.model` | `auto` | `auto`, `classic` or `slim`. `auto` infers 4px/3px arms from a 64x64 skin's UV layout; a 64x32 skin is always classic. |
| `-Dofflineskininjector.format` | `auto` | Layout of the PNG served to the client: `auto`, `64x64` or `64x32`. |
| `-Dofflineskininjector.uuid` | auto | Local player UUID, 32 hex chars with or without dashes. Auto-detected from the launcher's `--uuid`. |
| `-Dofflineskininjector.name` | auto | Local player name. Auto-detected from `--username`; used only when the UUID is unknown. |
| `-Dofflineskininjector.debug` | `false` | Set to `true` to print stack traces when the agent fails. |

> When neither UUID nor name can be detected, `mode=self` does nothing. Set `-Dofflineskininjector.uuid=` / `.name=`, or use `mode=all`.

## Skin formats

### Skins

| Client | 64x32 input | 64x64 input |
|---|---|---|
| 1.7.x and older | served as-is | cropped to 64x32 (top half) |
| 1.8+ | served as 64x64, with the left arm/leg mirrored from the right | served as-is |

`format=64x64` or `format=64x32` forces fixed format; `auto` picks it from the launcher's `--version`, `--assetIndex`, or the `versions/<id>` entry in the class path.

Notes:

- A 64x32 skin is always **classic** (4-pixel arms).
- A 64x64 skin is detected by `model=auto`: classic arm faces fill the whole arm block, while slim (3-pixel) arms leave a 2-pixel unused strip at the right edge of each arm block. Transparent unused strips (or four solid-black strips) mean slim.
- If a slim skin fills its unused strips with opaque colour, detection falls back to classic; set `-Dofflineskininjector.model=slim` explicitly.
- 1.7.x has no slim model, so a 64x64 slim skin logs a warning there as well.
- HD skins (128x128, 256x256, …) are rejected.

### Capes

- Cape textures use the classic **64x32** layout. HD capes are not supported.
- The cape is optional.
- Broken or missing cape files disable the cape only; the skin keeps working.
- Elytra needs no extra setup, client will automatically apply the cape texture.
- Using different images for the cape and the elytra is not supported.

## Supported versions

| Minecraft | authlib | Hooked methods | Verification |
|---|---|---|---|
| 1.6.2 and older | none | — | not supported |
| 1.7.10 | 1.5.21 | `getTextures` | runtime test + call-site scan |
| 1.8.3 | 1.5.21 | `getTextures` | decompilation |
| 1.12.2 / 1.13 / 1.13.2 | 1.5.25 | `getTextures` | call-site scan |
| 1.16.5 | 2.1.28 | `getTextures` | runtime test |
| 1.19.2 | 3.11.49 | `getTextures` | runtime test |
| 1.19.3 – 1.21.10 | new generation | packed | expected, same authlib generation |
| 1.21.11 | 7.0.61 | `getPackedTextures` + `unpackTextures` | runtime test |
| 26.3 | 10.0.77 | `getPackedTextures` + `unpackTextures` | runtime test |

Versions inside the same authlib generation use the same entry points and are expected to work even when they are not listed individually.

## How it works

```
-javaagent:offlineskininjector.jar
        │
     premain
        ├─ read -Dofflineskininjector.* and validate the PNGs
        ├─ resolve model=auto (4px/3px arm layout for 64x64)
        ├─ convert the skin to 64x64 / 64x32, then hash it
        ├─ start a loopback HTTP server
        │     /skin/<sha1>.png, /cape/<sha1>.png
        └─ install an ASM transformer on authlib
```

1. The agent starts before the game window and starts a tiny HTTP server bound to `127.0.0.1` on a random port. The skin and cape are published under URLs whose file names are their SHA-1 hashes.
2. The transformer injects an early return into the concrete authlib session-service implementations:
   - authlib 1.5–3.x: `getTextures(GameProfile, boolean)` returns our `Map<Type, MinecraftProfileTexture>`.
   - authlib 7.x–10.x: `getPackedTextures(GameProfile)` returns a `Property` with our payload, and `unpackTextures(Property)` returns our `MinecraftProfileTextures`, marked `SIGNED`. The early return also bypasses authlib's localhost domain whitelist and the `secure` filter.
3. The vanilla client downloads the PNG through its normal `SkinManager` path.
4. Every authlib type is accessed reflectively, and ASM is shaded and relocated inside the agent, so Forge/launchwrapper's older ASM cannot clash.

## Troubleshooting

| Symptom | What to check |
|---|---|
| `disabled: -Dofflineskininjector.skin=<file.png> is required` | The property is missing or misspelled. It must be a JVM argument with the `-D` prefix. |
| `skin file not found` | Wrong path, or the path contains spaces without quotes. |
| `skin must be 64x64 or 64x32` | Export the skin at a supported size; HD skins are rejected. |
| The agent initializes but the skin does not change | In `self` mode, the player could not be identified. Check `uuid = ...` in the log; set `-Dofflineskininjector.uuid=` or use `mode=all`. |
| The old skin still shows after replacing the PNG | Restart the game. If it persists, delete the cached files under `.minecraft/assets/skins` (modern) or `.minecraft/skins` (1.7.x). |
| Skin works, cape does not | The cape must be 64x32. Look for `cape disabled` in the log. |
| Elytra shows the default gray wings | The cape is inactive or cape display is disabled, so the client falls back to the vanilla elytra texture. Check `cape` / `cape url` in the log. |
| Slim arms are misaligned | Check whether the log's `model` says `slim (auto, 3px arms)`. If detection failed, add `-Dofflineskininjector.model=slim`. A 64x32 skin is classic-only, and 1.7.x has no slim model at all. |
| Other players see my normal skin | Expected — the change is client-side. `mode=all` changes how other players look to you, not how you look to them. |
| The game fails to start or throws `VerifyError` | Check for a skin mod that also rewrites authlib (for example CustomSkinLoader). Use one at a time, and run with `-Dofflineskininjector.debug=true` when reporting. |
| Nothing happens on 1.6.2 or older | 1.6.2 has no authlib, so there is nothing to hook. It is not supported. |
| Local HTTP fails behind a proxy | Add `-Dhttp.nonProxyHosts=localhost\|127.0.0.1` (quote the value if the launcher requires it). |

## Development

The build is a standard Gradle multi-project build (the agent lives in `app`). It requires JDK 17 to build, targets Java 8 bytecode, and uses Gradle 8.14 via the wrapper.

```
app/src/main/java/github/naivg/offlineskininjector/
  Premain.java             agent entry point and startup sequence
  Config.java              system properties and launcher arguments
  SourceTexture.java       PNG decoding and size validation
  SkinConverter.java       64x64 <-> 64x32 conversion and alpha rules
  SkinModel.java           AUTO / CLASSIC / SLIM model enum
  SkinModelDetector.java   4px / 3px arm-layout detection
  ClientVersion.java       --version / --assetIndex / class path detection
  TextureSet.java          texture URLs and the base64 payload
  TextureHttpServer.java   loopback HTTP server
  AuthlibTransformer.java  ASM early-return injection
  Hooks.java               authlib-facing hooks (reflection only)
  Reflect.java             reflection helpers
app/src/test/java/...      unit tests
tools/                     development and verification utilities
```

Run the unit tests with:

```bat
gradlew.bat :app:test
```

On macOS / Linux, use `./gradlew :app:test`.

The tests cover configuration parsing, PNG validation, format conversion, model detection, client version detection, and the textures payload.

## Limitations

- Client-side only; other players do not receive your custom skin.
- The elytra automatically reuses the cape texture; a separate elytra texture is not supported.
- No HD skins or capes.
- No hot reload; restart the client after changing a PNG.
- Skin mods that hook authlib themselves may conflict. Use one at a time.
- 1.6.2 and older are not supported.

## License

GNU General Public License v3.0. See [LICENSE](LICENSE).

If you have used this project's code or integrated it into your launcher, please be sure to mention it in `README` or `About`.

Minecraft is a trademark of Mojang Studios. This project is unofficial and is not affiliated with Mojang or Microsoft.
