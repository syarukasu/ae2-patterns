# Terminal-open production linkage failure

Source: Prism syarukasutime1.2.46 minecraft/logs/latest.log,
2026-10-18 22:16:40 JST, lines 11630-11640.

```
java.lang.AbstractMethodError: Receiver class com.syaru.ae2mrp.platform.EncoderBlockEntity does not define or inherit an implementation of the resolved method 'abstract net.minecraft.world.level.Level getLevel()' of interface appeng.helpers.IPatternTerminalLogicHost.
at appeng.parts.encoding.PatternEncodingLogic.fixCraftingRecipes(PatternEncodingLogic.java:283)
at appeng.parts.encoding.PatternEncodingLogic.setMode(PatternEncodingLogic.java:177)
at appeng.menu.me.items.PatternEncodingTermMenu.onServerDataSync(PatternEncodingTermMenu.java:485)
at com.syaru.ae2mrp.platform.NativeEncodingMenu.applySynchronizedState(NativeEncodingMenu.java:31)
at com.syaru.ae2mrp.platform.NativeEncodingMenu.refreshFromHost(NativeEncodingMenu.java:27)
at com.syaru.ae2mrp.platform.NativeEncodingMenu.<init>(NativeEncodingMenu.java:13)
at com.syaru.ae2mrp.encoder.EncoderMenu.<init>(EncoderMenu.java:261)
```

The old Forge JAR implements IPatternTerminalLogicHost directly on the Minecraft
BlockEntity subclass, but declares no getLevel implementation. Development
Minecraft provides the inherited getLevel name; production Minecraft provides
m_58904_ instead. AE2 invokes the interface's unmapped getLevel name.

The new adapter extends Object, explicitly implements all three AE2 interface
methods, and delegates getLevel to the block. javap on the reobfuscated alpha.10
JAR confirms its public getLevel method invokes EncoderBlockEntity.m_58904_.
markForSave similarly delegates to m_6596_. No catch, retry or empty return added.

The packaged-linkage guard rejects the original alpha.9 JAR and accepts both
alpha.10 loader JARs. This verifies class shape and remapped bytecode, not a
graphical-client smoke test. The later TooManyRecipeViewers logout exception is
not the first cause of the terminal-open failure and was not modified here.

Validation: clean build, 12 core tests, 22/22 required Forge GameTests and
22/22 required NeoForge GameTests passed. Final packaged metadata is alpha.10.

User-authorized deployment: installed alpha.10 in Prism syarukasutime1.2.46.
Both old JARs (alpha.1 and alpha.9) were moved outside mods to:
`.codex-backups/ae2mrp-20261018-222419` under that instance's minecraft directory.
Exactly one matching addon JAR remains. Installed Forge SHA-256:
`B204044B0760074AF2CBD95636B432E166C84F990CB4B466A3446A801CD3C3E4`.
No world/config changes or post-replacement client launch.
