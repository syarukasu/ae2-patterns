# AE2 Patterns 0.1.0

Status: binary publication held pending layout review of the corrected actual screen.

Initial public release. Install on both the client and the server, with Applied Energistics 2.

## Downloads

| Minecraft | Loader | Java | Tested AE2 |
|---|---|---|---|
| 1.20.1 | Forge 47.4.x | 17 | 15.4.10 |
| 1.21.1 | NeoForge 21.1.x | 21 | 19.2.17 |

Choose ONE matching runtime JAR, not a sources JAR.

## Features

- Combine 2-8 simple processing patterns producing the same primary output.
- Set output shares and optionally redistribute shortages among the registered routes.
- Reference source patterns without removing them from their Pattern Providers.
- One terminal screen with native AE2 pattern access and native pattern encoding panels.
- 54 composite registration slots, with nine visible at a time.
- Thin wall-mounted terminal and a wireless terminal using native AE2 linking, power and range.
- Japanese and English translations; 16x16 item textures.
- AE2 owns missing calculation, CPU bytes, job submission and physical execution.

## Scope and Limits

Composite sources are simple processing patterns only. Nested composites, crafting-table sources, fuzzy/substitution inputs, remaining containers, multiple outputs and dynamic patterns are not supported. Native executable quantities must fit signed long; internal BigInteger apportionment is not a promise of wide physical crafting support.

Adaptive redistribution is not an exhaustive solver. It preserves preferred shares first, then tries registered alternatives using isolated native inventory trials.

AE2 is the default backend. Optional ACO VM integration is ABI-specific, not universal compatibility with all ACO versions. An unavailable or incompatible selected backend reports an error rather than silently switching engines.

## Validation

- 12 core unit tests.
- 22 isolated AE2 GameTests per loader.
- Forge and NeoForge graphical client smoke: all four native encoding modes, pattern access, registration scrolling, shortage option, GUI scaling, close/reopen and exterior rendering.
- Resource and production-JAR linkage checks.

Post-replacement play in the full user modpack is not verified. No TPS, GC or speedup claim is made.

## 日本語

同じ主出力を持つ2-8種類の加工パターンを、指定した生産割合で組み合わせるAE2アドオンです。
「不足分を自動再分配」を有効にすると、登録した別経路で不足分を補います。
元パターンは各機械のProviderに残し、複合パターンは専用端末の54枠へ登録します。
上段のパターンアクセスと下段の通常エンコードはAE2本体の画面・処理を利用します。

対応範囲内の初回公開版です。あらゆる外部MOD・巨大数量・実パック環境での動作完了を保証するものではありません。
