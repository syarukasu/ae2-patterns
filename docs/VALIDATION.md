# Validation, 2026-10-19

## 0.1.0 public release

- Release metadata is 0.1.0 on both loaders. Clean build with --no-build-cache,
  12 core tests, 22/22 required GameTests on each loader and Forge GUI smoke passed.
- Added the equivalent isolated NeoForge GUI fixture. Its boot code uses the
  1.21.1 world-opening/processing APIs and a separate package; the run explicitly
  loads only the product and GUI fixture, not the dedicated-server GameTest mod.
  NeoForge graphical smoke passed all four native modes, mounted pattern access,
  54-slot row navigation, checkbox sync, scaling, close/reopen and model checks.
  Actual screenshots were inspected: evidence/gui-0.1.0-neoforge/.
- Resource audit and packaged-linkage checks passed for both release JARs.
- Forge SHA-256: 080ECCD61FDBA0FF0D925A14DF782202C31985C7CC0BA865BBDC593F254ABD4E.
- NeoForge SHA-256: F5C229F42B1D5BEAEC63FE9962BB35F24369E900F9D8456588A0847FD273A091.
- Both GUI runs use mapped development classes. The linkage guard checks packaged
  production bytecode separately; full live-modpack play remains unverified.
- No live pack, server, launch script or saved user world was changed for this release.

## Alpha.11 native GUI and terminal model

- Fixed the confirmed client initialization failure: the retained native panel
  container now shares the visible screen's resolved tooltip areas before mode
  visibility is applied. No absent-tooltip exception is suppressed.
- Removed custom terminal geometry. Forge/NeoForge native composite models
  reference AE2 monitor_base and pattern_encoding_terminal_on directly, with
  only the wall-position translation. English title/search overlap was removed.
- Final command passed with --no-build-cache: clean, 12 core tests, both loader
  builds, 22/22 required GameTests per loader, then the isolated Forge GUI client.
- Graphical fixture passed actual mouse-coordinate clicks, all four modes,
  live provider pattern display, six registration rows with nine visible slots,
  shared tooltip identity, checkbox/server sync, scale 2 to 3, close and reopen.
  Native baked-model bounds and non-missing sprites passed; the actual exterior
  screenshot was visually inspected. Final screenshots/report: evidence/gui-alpha.11/.
- Resource audit: 20 JSON files and 46 matching bilingual keys. Packaged linkage
  guard passed both JARs. git diff --check passed.
- Client syarukasutime1.2.46 was updated to only alpha.11 at the user's request.
  Alpha.10 backup: minecraft/.codex-backups/ae2mrp-20261019-013538.
  Forge SHA-256: C9945328E6E3D281F02F49978B1D01FFAC15BFFBDEFBF4834CF06EB2115FE04D.
  NeoForge SHA-256: 2C1DDD6E65C9B9EA26ACCA1AACFA452DDA4799BEFB41FA788D1DF0EF142810B8.
- The graphical run used mapped Forge development classes, not the large live
  pack. NeoForge graphical rendering and live-pack post-replacement interaction
  remain unverified. No user world, config, other mod or server was changed.

## Alpha.10 terminal-open linkage fix

- The live client log identifies AbstractMethodError at
  IPatternTerminalLogicHost.getLevel during NativeEncodingMenu construction.
  EncoderBlockEntity inherited a Minecraft method whose production name differs
  from AE2's unmapped interface method. This was missed by mapped GameTests.
- Both loaders now supply a separate explicit logic-host adapter. The production
  Forge bytecode keeps public getLevel and calls m_58904_ on the block, as required.
  The new packaged-linkage guard rejects the old alpha.9 and passes alpha.10.
- Clean builds, 12 core tests and 22/22 required GameTests per loader passed.
  Final metadata-only rebuilds and both packaged-linkage checks also passed.
  Resource audit passed (19 JSON, 45 bilingual keys); git diff --check passed.
- User-authorized client replacement: syarukasutime1.2.46 now has only alpha.10.
  Old alpha.1 and alpha.9 were backed up outside mods. Installed Forge SHA-256:
  B204044B0760074AF2CBD95636B432E166C84F990CB4B466A3446A801CD3C3E4.
  No config, world or other mod was changed. No client launch or real GUI
  interaction was performed after replacement; that acceptance remains pending.

## Unreleased unified native terminal

- Both loader builds and 22/22 required GameTests per loader passed after the
  54-slot registration and shortage-checkbox changes; core tests passed.
  The registration fixture exercises slot 53 persistence, retrieval, cross-row
  duplicate rejection and control expansion from the last row.
  Resource audit passed: 19 JSON files, 45 matching bilingual keys and existing
  texture hashes. Final git diff --check passed.
- Offline preview checks exercised checkbox state, six registration rows,
  wheel/boundary controls, native-mode selection, provider search and encoding.
  No horizontal overflow at 320/360/736px and no script errors were observed.
  This is not a real Minecraft rendering or mouse-input acceptance result.
- One active PAT menu with native encoder slot objects and native encoder actions.
- Native encoding panels, tabs and controls are composed into the parent widget
  container, not reimplemented as an imitation grid or an arrow-only form.
- The new shared fixture verifies same-menu reference selection, a single player
  inventory, ordinary processing and crafting encoding, substitution, exact
  blank consumption, separate persistence and closed-menu rejection.
- Previous null-player fixtures now construct a real fake player and wired host,
  because the native AE2 menus require that context. No assertions are disabled.
- The old return-tab/picker transition tests now assert in-place selection,
  unchanged cursor, reference ownership, radio power/range and closed-menu checks.
- Real graphical-client rendering, IME, scaled mouse/keyboard and native amount
  sub-screen return remain separate acceptance requirements. Server GameTests
  and offline previews do not establish them. Existing dist artifacts are unchanged.

## Alpha.9 native pattern-access screen

- Actual PatternAccessTermScreen/PatternAccessTermMenu subclasses now own the
  browser. No custom 30-slot page, registry bitset search or standalone search
  packet/channel remains. Native provider grouping, search, scroll, style and
  full/incremental updates are reused, not merely imitated with AE2 assets.
- Existing terminal/wireless fixtures exercise real native menu construction,
  broadcastChanges, two mounted source references, exact cursor preservation
  across menu switches, duplicate refusal, output hint/count synchronization,
  remote item locking, return, and closed-picker mutation refusal. The original
  removed-source, ghost ownership and real crafting completion tests remain.
- The first test run exposed an old fixture click on removed slot 56; it was
  migrated to the reference-selection path. Assertions were retained; production
  code was not relaxed to accept nonexistent slots.
- Both 21-case native AE2 suites, 12 core cases, both clean builds and final
  builds passed. Resource audit: 18 JSON files and 45 bilingual keys. Saved
  final reports are *-native-pat-ae2.xml. Browser search, editor return and
  encoding/registration passed at 320/360/736px without overflow or script errors.
  A server-only native menu test still does
  not prove real client rendering, native client packet delivery or IME behavior.

## Alpha.8 search and clearer controls

- Native AETextField and pattern-access ScreenStyle are reused. This is a
  reference-only browser, not the native menu that edits providers. Search
  supports client-localized registered item names and item IDs across all
  candidate pages, intersected with the existing exact primary-output filter.
- Removed permanent Input/Result headings and generic idle text. Shortage
  actions are Keep shares / Use alternatives, with explicit hover descriptions.
  Encoder controls/slots are hidden in browser view; all slot indices persist.
- Expanded the existing real-network terminal fixture to cover wrong menu ids,
  oversized filters, no-match clearing, matching-output recipes, search reset,
  pagination bounds and non-overlapping hidden editor slots. No tests disabled.
- Both clean builds, 12 core cases and each loader's 21-case AE2 suite passed.
  Browser search, mode selection, encoding and registration were exercised at
  320, 360 and 736px without page overflow or script exceptions.
- Real multiplayer packet delivery, native client rendering/IME and arbitrary
  resource packs are not accepted by browser or server-only tests. Registered
  base-item name/ID matching is not full native PAT ingredient/provider search.

## Alpha.7 AE2 styling and wireless terminal

- The GUI references the actual 1.21.1 AE2 19.2.17 background, slot, tab and icon
  assets and its #413f54/#878fa5 text palette. No opaque dark title strip or
  custom card backgrounds remain. Forge references the corresponding installed
  15.4.10 assets rather than bundling copies. Slot geometry, allocator, schema,
  server menu commands and existing source/provider behavior are unchanged.
- Added ae2mrp:wireless_pattern_terminal, its creative-tab entry, JP/EN name,
  native AE2 item-model reference and survival recipe. Native WAP linking is
  registered through GridLinkables; battery and range are delegated to AE2.
  A separate target GlobalPos selects the wired composite terminal to edit.
- Added one focused real-network fixture, mrp_wireless_terminal. Includes an
  actual WAP and wired terminal, native battery inject/extract, native linking,
  target/link/battery codec round-trip, the real inventory-open item entry,
  remote encoding outside the ordinary block-menu radius, publishing only after
  registration, locked item slots, native power drain, range loss, invalidated
  lease not reviving, replaced item, drained battery and native unlink rejection.
- The fake player overrides only openMenu's network-handshake boundary, constructing
  the actual production menu with the supplied MenuProvider. It is not a real
  client session, real packet/UI acceptance or physical Charger-block completion.
  Charger support was also traced to the native IAEItemPowerStorage path; no
  extra capability or charging formula was invented.
- Initial WAP fixture wiring was invalid: a Drive's front cannot connect to the
  WAP's back. The fixture now attaches the WAP to the existing cable with explicit
  EAST facing. No production retry, delay or wireless range relaxation was added.
- Core JUnit: 12 cases. Both loaders' clean build --no-build-cache and isolated
  21-case native AE2 suites passed; existing tests were retained. Final XML
  reports are stored separately from alpha.6 under *-wireless-ae2.xml.
- The pinned optional refs.8 ACO backend also passed all 21 fixtures per loader;
  reports are retained as *-wireless-aco.xml. This does not imply compatibility
  with other ACO/VM ABIs or live wide-order CPU acceptance.
- Resource audit checks 18 JSON files, 44 matching bilingual keys and native
  GUI/model/texture references in both AE2 JARs, as well as the unchanged custom
  16x16 icons. The shipped JARs do not contain extracted AE2 sprites or test code.
- The updated browser preview uses the real 19.2.17 sprite coordinates, not the
  older Forge toolbar coordinates. Local interactive search, shares, ADAPTIVE,
  encoding, registration and item names/IDs remain visible. Browser screenshots
  are under gui-ae2-1211-*.png; they are not Minecraft screenshots.
- Remaining runtime acceptance: real multiplayer packet/drag interaction, physical
  Charger use, offhand interaction, chunk unload/reload and client rendering.
  Curios/external slots and mixed crafting-grid/processing composites are not
  supported by this revision. No live pack, server launcher or world was edited.

## Alpha.6 network terminal

- ME-capable thin terminal on both loaders, with native in-world grid capability,
  first-tick node creation, node teardown, persisted nine-slot control registration
  and a control-only crafting provider. No block ticker or external recipe edit.
- Two tabs in the native 312x240 surface: share editor and 30-entry network pattern
  browser. Selecting one real mounted source copies a non-transferable reference
  and filters candidates by exact primary AEKey, preserving AE2 candidate order.
- Encoding revalidates pinned source/grid identities. Unregistered drafts do not
  advertise controls. Original Provider items and carried items remain untouched.
  Ghosts cannot be cloned, thrown, shifted out, saved or dropped as physical items.
- New real-network fixtures: mrp_network_terminal_reference_register and
  mrp_network_terminal_stale_reference. Cover capability connection, menu clicks,
  automatic same-output search, ghost ownership, registration shift-click and
  unregister, duplicate control rejection, save/load, remote-menu rejection,
  two-source actual planning and stale-reference refusal without blank consumption.
- The initial regression run caught a real draft bug: inserting a blank carrier
  cleared network references before checking whether it contained saved data.
  readDraft now checks the carrier before clearing previews. No retry/fallback
  was added. The menu-coordinate test was updated for the deliberately added
  39 slots and the new route row positions, retaining exact geometry assertions.
- AE2 candidate iteration differs between the two loader fixtures. Q=5 at 50/50
  assigns 3 to the first route: a two-output first route legitimately uses 6 raw
  items rather than 5 after batch rounding. The test derives its exact expectation
  from the retained source order; production does not reorder candidates to fit it.
- Core: 12 JUnit tests, zero failures/errors/skips. clean build with
  --no-build-cache passed for Forge and NeoForge, followed by the final rebuild.
- Final native AE2 suites: 20 GameTests per loader. Optional pinned refs.8 ACO
  backend suites: the same 20 per loader. All reports have zero failures/errors/skips:
  forge-terminal-ae2.xml, neoforge-terminal-ae2.xml, forge-terminal-aco.xml and
  neoforge-terminal-aco.xml under docs/evidence.
- Resource audit: 15 JSON files, 36 matching bilingual keys, unchanged 16x16
  pattern textures and all referenced external textures present in both AE2 JARs.
- Browser-only preview includes item names, verified registry IDs and actual
  16px game textures. Terminal front layers use the product's AEColor.PURPLE
  multipliers. Search, weight editing, mode change, encode and registration are
  local preview interactions, not Minecraft GUI acceptance. No graphical client
  or live pack/server was started or modified. Native rendering, real dragging,
  arbitrary modded providers and chunk reload remain separate acceptance work.
- Crafting-grid patterns and mixed crafting/processing composites remain outside
  the current source contract. This terminal change does not claim to add them.

The older sections below preserve earlier-version evidence and limitations.

Initial implementation version: 0.1.0-alpha.1. This is not a production-release
or full live-pack acceptance claim.

## Evidence classes

- Shared pure core: six JUnit tests, including 1,000 small-quantity allocations,
  exact 10^64 allocation, output-unit versus operation-unit allocation, invalid
  sums, duplicates, disabled routes and deterministic ties.
- Forge 1.20.1 / Java 17 / Forge 47.4.18 / AE2 15.4.10: twelve isolated real
  GameTests. Includes native Pattern Provider, storage cell, standard 64K CPU,
  physical Inscriber and exact post-completion network counts.
- NeoForge 1.21.1 / Java 21 / NeoForge 21.1.247 / AE2 19.2.17: the same twelve
  real GameTests. Only the test registration and item/block serialization differ.
- Forge current ACO co-installation: the same twelve fixtures passed with
  aco2.0.0-rc.7-vm-refs.8-local_1.20.1.jar, SHA-256
  DCD10CB901E10C25BB412EC9CFDEE454925179C8005B16C42A92BFB1DD0F36E5.
  This is a local fixture, not an ACO release or compatibility promise for all versions.
- Resource audit checks both AE2 JARs for every referenced block texture,
  parses all resource JSON, compares English/Japanese keys and placeholders,
  and records the game texture hashes and verifies their 16x16 dimensions.

## Twelve runtime fixtures

| Fixture | Assertion |
|---|---|
| mrp_strict_completion | 5 outputs: 3 one-output operations and 1 two-output operation; real machine output 5, raw stock decreases by 5 |
| mrp_rounding_excess_returned | 3 requested, 4 physically produced; all 4 return, stock decreases by 4 |
| mrp_shared_stock_missing | Two routes share 4 inputs against demand 5; exactly 1 missing, no inventory double spending |
| mrp_craft_less | Demand 5 with stock 4 produces an ordinary feasible plan for 4 |
| mrp_vanilla_unchanged | No composite: native/configured planner uses one original pattern and exactly 5 inputs |
| mrp_encode_reedit | Originals retained; one blank consumed; non-transferable previews; revision increments on re-edit |
| mrp_invalid_configuration | Non-100% sum and duplicate patterns rejected; no carrier consumption |
| mrp_codec_roundtrip | Item NBT/Data Component retains routes, revision and pattern identity |
| mrp_eight_routes | All eight distinct patterns and weights survive encoding |
| mrp_removed_source_fails_closed | Missing mounted source fails as SOURCE_PATTERN_NOT_ON_NETWORK; no automatic reassignment or real stock mutation |
| mrp_disabled_source_not_required | Disabled third source need not be mounted and never executes |
| mrp_encoder_persistence | BlockEntity save/load retains physical items and exact settings |

XML reports are retained under docs/evidence. A clean final build is recorded
separately from the GameTest runtime results. Deprecation warnings from loader
client registration APIs remain; no tests are disabled to silence them.

Executed successfully: clean build with --no-build-cache; final rebuild after
the compact GUI/server-side delta-button adjustment; six core tests; both sets
of twelve GameTests. The ACO fixture also passed all twelve after the VM gateway
fix. An expected SOURCE_PATTERN_NOT_ON_NETWORK exception is deliberately logged
by the removed-source negative test and is not a swallowed runtime failure.

## Reproducing

```powershell
./gradlew.bat clean :core:test :forge:build :neoforge:build --no-build-cache
./gradlew.bat :forge:runGameTestServer :neoforge:runGameTestServer
./gradlew.bat :forge:runGameTestServer -PacoJar=C:/path/to/aco-fixture.jar
./tools/verify-resources.ps1 -ForgeAe2Jar C:/path/to/ae2-forge.jar -NeoAe2Jar C:/path/to/ae2-neoforge.jar
git diff --check
```

The GameTest tasks remove the previous report before launching and reject absent
reports, anything other than twelve cases, failures, errors or skips. They do not
equate a zero launcher exit code with having executed tests.

## Superseded alpha.1 texture export (2026-10-18)

The user approved nearest-neighbor downsampling to 64x64. Both 1254x1254 source
PNGs are preserved unchanged under artwork/original, outside packaged resources.
tools/export-item-icons.ps1 exports center-sampled nearest-neighbor RGBA pixels,
then reloads each PNG and verifies every pixel against its original source sample.
No image generation, smoothing, alpha blending or changes to the Downloads
originals are involved. This 64x64 export is superseded by alpha.2 below.

Original SHA-256:
- blank: DDA258B67542D61DF95794D4BAA1A0F508AF186E42F1EEF99B7002D4D72A2617
- encoded: 18B39C1144DD34B344C1299D53F33414273C2C471247EBC65F518AB346012CD3

64x64 game texture SHA-256:
- blank: 010B1F368BBE821D0CB713C40B8D3487C569EC1C465AFD2A3C5182E1E0BB6B5C
- encoded: D4E392AD93358553CDE05609D196D51E370A1B7D8D8672753AAC8CCFD30F9B00

After export, both loader builds and all twelve GameTests per loader passed again.
Core tests remained up-to-date (not re-executed for the PNG-only change).
The four distribution JARs were refreshed; embedded PNG hashes match the exports
and original artwork is excluded. dist/manifest.json records the final JAR hashes.

## Alpha.2 terminal and GUI revision (2026-10-18)

- Display name: AE2 Patterns. Mod ID ae2mrp, all registry IDs, save schema and
  recipe-allocation semantics are unchanged.
- A dedicated creative tab contains the terminal and both pattern items.
- The terminal is 12x12 pixels on its face and 3 pixels deep, with four horizontal
  facings, placement/rotation/mirror support and referenced AE2 terminal textures.
  Screen layers and inventory icon use the same AE2 purple tint mapping.
- The 312x240 GUI labels source patterns / output shares, input and result.
  Empty routes hide inactive controls. Numeric shares and +/- steppers remain;
  checkboxes enable routes; the trash action appears only for saved previews.
  Preview flags synchronize through a DataSlot and prevent client-side pickup
  prediction as well as server-side extraction.
- Both game icons are now 16x16 nearest-neighbor exports. Original artwork is
  unchanged. Pixel verification includes the full RGBA value at all 256 pixels.
  blank SHA-256: 50D6E03CFB6604AD775C2EAA1E7E0B155BBFF6E570A93ABA0DFB292E668D72C3
  encoded SHA-256: 1318F06838729C00A9C9F89AFF05D92311430A9627C12CC3BCEAB538D70F105D
- gui-offline-preview.png and gui-offline-mobile.png are BROWSER layout previews,
  not Minecraft screenshots or native client acceptance. Browser checks cover
  110% rejection, returning to 100%, the local encode interaction, image decode
  at 16x16 and no horizontal overflow at 360px.
- No graphical Minecraft client was started. An attempted development visual
  run failed during Gradle configuration before game startup; that unused
  fixture was removed. Runtime rendering and real mouse/keyboard interaction
  remain unverified; see the acceptance limits below.
- Both loader builds and all twelve GameTests per loader passed for alpha.2.
  The persistence fixture also verifies the 3-pixel-deep rotated shape, dedicated
  creative-tab icon and matching terminal menu slot coordinates. Core tests were
  up-to-date; the arithmetic implementation was not changed.

## Alpha.3 adaptive allocation and UI (2026-10-18)

- Added explicit ADAPTIVE mode, with encoding, item codec, draft re-edit, native
  block persistence and server-owned menu synchronization. Existing STRICT
  allocation remains unchanged.
- The header's example output-name text is replaced by the actual primary item
  icon and hover tooltip. The mode label is now a mouse/keyboard selector.
  Encoding uses AE2's native IconButton and ARROW_RIGHT sprite, not a text button.
- Pure-core JUnit: 12 cases, no failures/errors/skips (six existing STRICT plus
  six new ADAPTIVE cases). Native suites: 17 cases each for Forge AE2-only,
  NeoForge AE2-only, and Forge with the recorded ACO VM fixture.
- New native fixtures verify preferred shares, reallocation followed by real
  four-item Inscriber completion, partially consumed failed-branch rollback,
  rounded Missing without failed-probe leakage, and CRAFT_LESS.
- The first physical test's 60/40 expectation was wrong: largest remainder gives
  2/2 units for a four-item request. Its fixture was corrected to 75/25 (3/1 units)
  so it actually requires redistribution. The strict arithmetic was not changed.
- Full algorithms, scoped probe isolation and global-optimization limitations are
  documented in ADAPTIVE.md. Browser images remain offline previews, not native
  rendering acceptance. No graphical game client or active pack was modified.

## Alpha.4 text layout correction (2026-10-18)

- Removed one-line status truncation. Status messages wrap within a reserved
  two-line area above the inventory; unusually tall translations scale to fit
  without dropping text. Input/result slots and route rows were moved to make
  that space without increasing the 312x240 screen.
- Widened the percentage input for three digits, separated the percent sign,
  moved saved-preview removal to the upper control row and widened the mode
  selector. Its popup stays above the selector.
- Offline browser checks covered all 11 status strings in both languages at
  widths 736, 360 and 320 (66 cases), with 100/0 percentage inputs. No horizontal
  page overflow, input clipping or status/inventory overlap was detected.
  The mobile image also exercises a deliberately longer Japanese instruction.
- Both final loader builds succeeded. All 17 isolated GameTests per loader
  passed, including the updated menu coordinate assertion. The 12 core tests
  were up-to-date, not re-executed. Resource audit and diff whitespace check
  passed. The ACO fixture was not re-run for this layout-only revision.
- These browser images are not evidence of native Minecraft font rendering.
  No active server/client pack was modified or graphical Minecraft started.

## Alpha.5 optional ACO backend (2026-10-18)

- Server config `plannerBackend` selects AE2 (default) or ACO. No saved pattern
  schema, source order, ratio policy, CPU submission or physical storage code
  was changed. The active packs, ACO sources and VM sources were not edited.
- ACO mode actually returns the installed NativeVm order Future; fixtures assert
  this rather than accepting a silent native-route substitution. The native
  backend is additionally calculated as a reference ONLY in test code.
- For successful composite calculations, fixtures compare output, simulation,
  multiplePaths, all original pattern counts, used/emitted/missing counters and
  exact CPU bytes against the native backend. This includes STRICT, ADAPTIVE,
  rounded output, Missing and CRAFT_LESS. The new controlled-intermediate fixture
  verifies ancestor graph aggregation cannot bypass distribution.
- Adding a one-unit order to that fixture initially produced a real parity
  failure (forge-aco-small-before-fix.xml): the VM retained multiplePaths=true
  from the unexpanded candidate list although allocation retained only one path.
  Result construction now derives the flag from the retained VM tree, as AE2
  does. The same fixture, without weakening its assertions, passes for both
  one-unit and five-unit orders on both loaders.
- The no-ACO runtime also explicitly selects ACO once and requires the precise
  unavailable/incompatible diagnostic, then restores native mode. There is no
  silent backend fallback. Removed-source failures remain expected negative cases.
- Pure-core tests: 12 passed. Runtime matrix: 18 required cases each for Forge
  AE2-only, NeoForge AE2-only, Forge AE2 mode with ACO installed, Forge ACO mode,
  and NeoForge ACO mode. No failure/error/skip in these reports.
- Forge ACO fixture: rc.7-vm-refs.8-local with native VM 0.3.12-vm-refs.8;
  SHA-256 DCD10CB901E10C25BB412EC9CFDEE454925179C8005B16C42A92BFB1DD0F36E5.
- NeoForge fixtures are LOCAL TEST ARTIFACTS, not installation recommendations:
  ACO snapshot-refs.8 SHA-256
  53BF77E053D92D21EFA3FF5415CEDBEFEF8E88D1820FF455388986AC1715D2B4;
  VM 0.3.12-vm-refs.8-neoforge SHA-256
  18541528C0ADE895368912542E3206CBA52667BCC5FD986A687AEA90B587D9EB.
- Final build and report paths are under each loader's build directory; saved
  ACO mode reports are forge-aco-option.xml and neoforge-aco-option.xml.
  The existing forge-aco-vm.xml now records the 18-case default AE2-mode co-installation.
- Configuration generation was observed at Forge world/serverconfig and at the
  NeoForge isolated GameTest config directory. No graphical client was launched.
- These are long-range planning/parity and real ordinary-CPU completion fixtures,
  NOT acceptance of wide physical completion, every third-party CPU, future VM
  ABIs, or a performance claim. The pinned private adapter remains an explicit
  compatibility boundary. No ACO or VM classes are packaged inside this addon.

## Not yet accepted

- GUI visual inspection, mouse/keyboard interaction in a real player client and
  server/client join handshake. The layout is 312x240 GUI pixels so its geometry
  fits Minecraft's minimum scaled viewport, but that is not a screenshot test.
- Full process restart and real-world chunk unload/reload, as distinct from the
  native item/BlockEntity serialization exercised above.
- Every cycle/alternate producer topology, dynamic providers and third-party
  CPU integration. Unsupported encoded source classes are rejected initially.
- ACO co-installation on NeoForge or every historic ACO version.
- BigInteger/wide native plans: deliberately unsupported. BigInteger core tests
  do not constitute native wide crafting acceptance.
- TPS, GC, latency, rendering or allocation improvement. This addon implements
  recipe allocation, not an optimizer, and no performance percentage is claimed.

The isolated validation steps did not modify active packs, configurations,
launchers or worlds. The separately authorized alpha.10 client JAR replacement
is recorded above. Only isolated development GameTest servers were started.
