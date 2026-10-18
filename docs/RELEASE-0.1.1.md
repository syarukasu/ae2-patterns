# AE2 Patterns 0.1.1

First public binary release with the corrected terminal layout. The previously submitted 0.1.0 files remain archived.

Install the matching runtime JAR on BOTH client and server, together with Applied Energistics 2.

| Minecraft | Loader | Java | Tested AE2 |
|---|---|---|---|
| 1.20.1 | Forge 47.4.x | 17 | 15.4.10 |
| 1.21.1 | NeoForge 21.1.x | 21 | 19.2.17 |

## Features

- Combine 2-8 simple processing patterns with the same primary output and configurable output shares.
- Optionally redistribute shortages among registered routes; otherwise preserve the configured shares.
- Reference source patterns without taking them out of their machine Pattern Providers.
- Native AE2 pattern access above composite editing and native AE2 pattern encoding below, in one screen.
- 54 registration slots, with nine visible at a time.
- Thin wall-mounted terminal, wireless access, and Japanese/English translations.

## Layout Corrections

- Preserve the ordinary encoder's actual AE2 panels, icons, mode tabs, original style positions and operations.
- Keep registration navigation inside the composite section instead of overlapping the native encoder.
- Match compact button drawing to its click area.
- Reserve the full native switch width and move reference-clear buttons beside the share fields.
- Keep the screen scale positive when the game window is minimized.

## Scope

Composite sources must be simple processing patterns. Crafting-table sources, nested composites, fuzzy/substitution inputs, remaining containers, multiple outputs and dynamic patterns are not supported. Native executable quantities must fit signed long. Internal BigInteger apportionment does not promise wide physical crafting. Adaptive redistribution is not an exhaustive solver.

AE2 owns inventory simulation, missing ingredients, CPU bytes, job submission and execution. The default backend is AE2. Optional ACO VM integration is restricted to the documented compatible ABI, not all ACO versions.

## Validation

12 core unit tests, 22 required isolated GameTests on each loader, both builds, resource audit and packaged-linkage checks. Actual graphical smoke on both loaders covers four encoding modes, original native icon positions, reference previews, control/slot overlap, registration navigation, checkbox sync, scaling and close/reopen.

Full user-modpack acceptance remains unverified. No TPS, GC or speedup claim is made.

## 日本語

複合パターンを指定割合で分配するAE2アドオンの初回配布版です。
通常エンコードはAE2本体の画面・アイコン・配置・処理をそのまま利用し、複合編集欄のボタン重なりを修正しました。
登録は54枠、9枠ずつ表示。素材不足時の自動再分配とワイヤレスターミナルに対応します。
Forge 1.20.1とNeoForge 1.21.1の両版を用意しています。
