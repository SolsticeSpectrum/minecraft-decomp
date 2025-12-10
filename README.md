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

## IntelliJ IDEA

Run `runClient`/`runServer` once before opening.

Hot reload requires JetBrains Runtime in Project Structure, IDEA must run under it too:
```bash
export IDEA_JDK="/usr/lib/jvm/java-21-jetbrains"
```

## Structure

```
├── jars/              # vanilla jars
├── libs/              # decompiler linking libs
├── patches/           # decompiler fixes
├── mods/
│   ├── client/        # client patches (mymod.patch)
│   └── server/        # server patches (mymod.patch)
├── server/src/        # server source
├── client/src/        # client source
└── prism-instance/    # PrismLauncher instance template
```

## Modding

Two-layer patch system:
1. `patches/` - fixes for decompiler output (don't touch)
2. `mods/client/` and `mods/server/` - your named patches

Edit source, test, then generate a named patch:
```bash
./gradlew modGen -Pargs=client,mymod
./gradlew modGen -Pargs=server,mymod
```

This saves to `mods/client/mymod.patch` or `mods/server/mymod.patch`.

### Applying Mods

Apply named patches to the source:
```bash
./gradlew modApply -Pargs=client,mod1,mod2,mod3
./gradlew modApply -Pargs=server,mod1,mod2,mod3
```

Patches are applied in order. If a patch fails:
- Already applied? Use `modRevert` first
- Conflicting mod? Revert the other mod first

### Reverting Mods

Reverse patches (in reverse order):
```bash
./gradlew modRevert -Pargs=client,mod1,mod2,mod3
./gradlew modRevert -Pargs=server,mod1,mod2,mod3
```

If revert fails, the patch was likely already reverted or never applied.

### Packing Mods

Pack changed classes into a zip for distribution:
```bash
./gradlew modPackClient
./gradlew modPackServer
```

Compiles current source and outputs changed classes to `mods/client.zip` or `mods/server.zip`.

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
   1. There should be a new instance `My Modded Minecraft`
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
