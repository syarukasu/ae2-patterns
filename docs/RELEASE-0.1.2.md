# AE2 Patterns 0.1.2

Icon packaging update for both loaders. Includes the corrected terminal layout from 0.1.1.

- Display the existing encoded composite pattern icon in the mod list.
- Use nearest-neighbor filtering for the 16x16 icon (`logoBlur=false`).
- No changes to crafting, distribution, saved data, registry IDs or the native AE2 encoder.

Install the matching runtime JAR on BOTH client and server, together with Applied Energistics 2.

| Minecraft | Loader | Java | Tested AE2 |
|---|---|---|---|
| 1.20.1 | Forge 47.4.x | 17 | 15.4.10 |
| 1.21.1 | NeoForge 21.1.x | 21 | 19.2.17 |

Composite sources remain limited to 2-8 simple processing patterns with the same primary output. Native executable quantities must fit signed long. AE2 owns inventory simulation, missing ingredients, CPU bytes, submission and execution.

Full user-modpack acceptance remains unverified. No TPS, GC or speedup claim is made.

## 日本語

MOD一覧のアイコンに、エンコード済み複合パターンの画像を設定しました。
両版のJARに16x16画像と表示設定を同梱しています。0.1.1の画面修正は引き継ぎ、クラフト処理や保存形式は変更していません。
