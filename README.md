# Minecraft Decompiled

Decompiled source from Mojang's unobfuscated JARs. No Loom, no Forge toolchains, just Vineflower and Gradle.

Starting with 1.21.11, Mojang ships unobfuscated JARs.

## Requirements

- JDK 21+

## Setup

```bash
./gradlew setup
```

Decompiles both client and server, applies patches, done.

## Build

```bash
./gradlew :server:compileJava
./gradlew :client:compileJava
```

## Run

```bash
./gradlew runServer
./gradlew runClient
```

For client, grab assets first:
```bash
./gradlew downloadAssets
```

## Structure

```
├── jars/           # vanilla jars
├── libs/           # decompiler linking libs
├── patches/        # decompiler fixes
├── mods/           # your modifications (.patch and .zip)
├── server/src/     # server source
├── client/src/     # client source
└── prism-instance/ # PrismLauncher instance template
```

## Modding

Two-layer patch system:
1. `patches/` - fixes for decompiler output (don't touch)
2. `mods/` - your changes

Edit source, test, then generate patches:
```bash
./gradlew genClientMods
./gradlew genServerMods
```

### Packing Mods

Pack changed classes into a zip for distribution:
```bash
./gradlew packClient
./gradlew packServer
```

This compares your source against the base, finds modified files, and outputs them to `mods/client.zip` or `mods/server.zip`.

### Using with PrismLauncher

Old-school jar modding is back. To use your mods with PrismLauncher:

1. **Copy the instance template:**
   ```bash
   cp -r prism-instance ~/.local/share/PrismLauncher/instances/my-modded-mc
   ```

2. **Add your mod classes:**
   ```bash
   cp mods/client.zip ~/.local/share/PrismLauncher/instances/my-modded-mc/jarmods/
   ```

3. **Restart PrismLauncher:**
   1. There should be a new instance `1.21.11-rc2 Unobfuscated`
   2. You can edit it to toggle `Unobfuscated` component to disable unobfuscated client
   3. Or you can toggle `Mods` component to disable your mods

The jarMods system merges your classes on top of the base jar at runtime.

### Instance Structure

```
prism-instance/
├── instance.cfg                    # instance name/type
├── mmc-pack.json                   # components list
├── jarmods/
│   └── client.zip                  # your compiled mod classes
└── patches/
    ├── custom.unobfuscated.json    # unobfuscated jar override
    └── custom.mods.json            # jarMods component
```

## Decompiler

Uses Vineflower with standard flags. Patches fix type inference issues, lambda captures, and other decompiler artifacts.

## Legal

Mojang owns this code. See LICENSE.
