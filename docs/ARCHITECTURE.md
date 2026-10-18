# AE2 Patterns (v0.2 specification implementation)

## Ownership

The 0.1.0 single-screen revision supersedes the alpha.9 screen transition below.
EncoderMenu now extends the native PAT menu. EncoderScreen extends its native PAT
screen, preserving native provider packets, filtering and scrolling. The native
PatternEncodingTermMenu contributes the SAME physical/config slot objects, and
PatternEncodingTermScreen contributes its ACTUAL mode panels, tabs and controls
to the parent's WidgetContainer. Only share editing/registration is addon UI.
Native panels retain their original WidgetContainer reference. After the parent
populates its style, the adapter binds the SAME resolved tooltip-area objects
into that retained container, before any panel visibility update. Resize and
registration-row changes rebind the newly resolved areas. No dummy tooltip,
missing-area catch or second tooltip layout is introduced.
There is one active menu, one player inventory, one carried stack and one packet
owner. Native encoder operations are guarded and routed through that active menu.
The embedded menu never publishes its own synchronization or ME storage updates.
Its preview callbacks must not recursively enter the parent's synchronization.
The previous picker menu, return screen and transition commands are removed.
Normal PatternEncodingLogic is saved separately; only its two physical pattern
stacks are dropped. Config slots and composite references never become drops.
PatternEncodingLogic receives an explicit IPatternTerminalLogicHost adapter,
not the BlockEntity itself. Minecraft's inherited getLevel method is remapped
on Forge, while AE2's interface method is not; inheriting the former cannot
implement the latter in a production JAR. The adapter's explicit interface
methods delegate to the block and retain the appropriate namespace on each side.
Registration has 54 physical slots (six semantic groups of nine), all synchronized
in one menu. Only the visible row is positioned for interaction. Changing rows
is client-local and cannot move items or change provider advertisements. All 54
remain eligible for persistence, quick-move, duplicate checks and block drops.
The native encoder slot prefix starts at 100, after the 54 registration slots;
ordinary encoder actions use that constant rather than the previous prefix 55.
The shortage checkbox selects ADAPTIVE when checked and STRICT when unchecked.
The placed block composes AE2's monitor_base and pattern_encoding_terminal_on
models through the platform's native composite loader. The only geometry change
is a 13/16-block translation to the supporting wall, matching existing bounds.
There are no locally recreated terminal faces, UVs or light layers. Inventory
rendering already inherits AE2's pattern_encoding_terminal item model.

Historical UI (alpha.9): PatternPickerScreen extends native PatternAccessTermScreen;
PatternPickerMenu extends native PatternAccessTermMenu through a loader adapter.
AE2 owns provider grouping, search, sizing, scrollbar, full and incremental
provider packets. The alpha.6/alpha.8 standalone browser described below is
historical and has been removed, including its slots and search network channel.

| Class | Responsibility |
|---|---|
| StrictDistributor | Pure BigInteger output apportionment; no Minecraft or inventory access |
| AdaptiveDistributor | Preferred shares, weighted redistribution and bounded route removal; native allocation callback owns capacity |
| AdaptiveTrial | Successful isolated native process/inventory diff; committed once by the planner |
| CompositeDefinition | Copied source data; common primary AEKey; simple-processing and semantic-duplicate validation |
| Platform (per loader) | AE2 API differences, strict NBT/Data Component codec, control construction |
| ControlPattern | Provider advertisement for a control document, never a process |
| EncoderState | Physical inventory versus non-transferable editing previews; encode atomically on server |
| EncoderMenu | One native PAT menu, reference validation, 54 registrations, native encoding actions and carried-stack ownership |
| NativePatternMenu | Loader adapter for native PAT sync, reference-only selection and server access checks |
| NativeEncodingMenu / NativeEncodingPanel | Native AE2 encoding logic, same slot objects and actual native GUI panels; no separate inventory publication |
| TerminalLogic | Managed AE2 grid node, 54 physical registration slots and control-only ICraftingProvider; no machine execution |
| EncoderScreen | Native PAT and native encoder in one screen, 4x2 share editor, shortage checkbox and scrollable registration row |
| EncoderBlock/EncoderBlockEntity | Four-facing thin wall terminal, block lifecycle and physical inventory persistence; drop no editing previews |
| CompositeCalculationMixin | Preserve native AE2 attempts, CRAFT_LESS, missing simulation; exclude single-producer optimizers before ownership |
| CompositeServiceMixin | Grid-owned control-provider presence index, native request gateway and rejection of executable control documents |
| GlobalControlProviderMixin (NeoForge) | Mirror native global-provider add/refresh/remove into the same grid-owned presence index |
| OptionalNativeVmMixin | Route composite orders at the optional VM public entry before that engine owns them; no VM/ACO source edits |
| PlannerConfig / PlannerBackend | Server-owned AE2 (default) or ACO backend selection for new composite orders |
| OptionalAcoContextMixin | Pin control rules with the VM snapshot, scope arithmetic and revalidate used bindings in the VM live-read window |
| OptionalAcoExecutionMixin / OptionalAcoAggregateMixin | Preserve controlled demand boundaries inside VM execution and prevent aggregate compilation from bypassing them |
| AcoCompositeSession / AcoVmAccess | STRICT/ADAPTIVE VM state adapter and cached, shape-checked access to the optional private VM ABI |
| ControlReachability | One request-time reachable-key walk only when the grid has mounted controls; pin control identities |
| CompositeNodeMixin | After native stock subtraction, expand STRICT shares into native source processes on the SAME simulation inventory |
| CompositePlanningContext | Per-calculation control identity and change rejection, no global cache |
| CalculationAccess/ProcessAccess | Narrow invokers for native AE2 operations |
| ControlProcessGuardMixin | Fail if a control advertisement reaches an ordinary crafting process |
| MrpPlots | Shared real-network integration fixtures, not a replacement crafting simulator |

## Distribution invariant

Percentages describe output units, never recipe invocations. For demand Q and
weight w, start at floor(Q*w/100). Assign remaining units in descending fractional
remainder order, ties by registration order. The integer targets sum EXACTLY to Q.
For each route, operations=ceil(target/outputPerOperation). The native process
creates that many batches. Only the target is removed from the native simulation;
unavoidable batch excess remains there and is returned by the ordinary AE2 CPU.
STRICT has no rounding normalization, material-dependent fallback or candidate race.
Explicit ADAPTIVE allocation is documented in docs/ADAPTIVE.md; it does not change
STRICT or external CPU/storage ownership.

Allocation is O(routes log routes), routes<=8, independent of requested amount.
BigInteger is used for allocation so intermediate multiplication cannot wrap.
The initial native adapter is LONG ONLY: operations, total output and input
multiplication must fit signed long, otherwise explicit failure. It does not
claim ACO wide-sidecar support or silently saturate to Long.MAX_VALUE.

## AE2 connection

PatternDetailsHelper registers a decoder, so ordinary Pattern Providers can store
the composite. The control advertises its primary output but has no executable
inputs and refuses external input pushing. It is NOT a processing-pattern
subclass. Original processing patterns must remain mounted on the same network.

The native CraftingTreeNode.request hook is immediately before buildChildPatterns,
AFTER stock extraction and emitter handling. For each allocated route it creates
a native CraftingTreeProcess and requests the integer batch count on the same
CraftingSimulationState. Its real patterns, used inventory, missing inputs,
tree bytes and batch bytes flow through AE2's existing accounting.

The service owns an identity set of currently registered control providers.
Native add/remove/refresh callbacks update it, including NeoForge global
providers. No tick polling or static world references exist. With no controls,
request overhead is one presence check, not an extra graph traversal. On a grid
with controls, one server-side reachable-key walk pins applicable definitions;
orders unrelated to those controls continue into the configured existing planner.

With the default AE2 backend, applicable orders create a native CraftingCalculation and use AE2's EXISTING
CRAFTING_POOL. Native runCraftAttempt, CRAFT_LESS search and missing simulation
remain in use. No new pool, custom Future or worker wait is introduced.

An actual co-installation test found that the installed ACO VM intercepted the
request before CraftingCalculation existed, so a computePlan hook and mixin
priority alone did NOT protect ratios. OptionalNativeVmMixin therefore routes
applicable requests at NativeVm.calculate's explicit public entry. The optional
target is @Pseudo; it is absent without ACO/VM, and its known method signature
is required when installed. Wide composite requests fail explicitly, never
silently narrowing into the native long planner. The executable submission guard
also rejects encoded control definitions even if wrapped by another planner.

No ACO source is edited or required. Current Forge ACO VM co-installation passed
the same twelve fixtures after this fix. Other ACO/VM versions and NeoForge
co-installation still require their own runtime checks; this is not universal
future compatibility evidence. The optional ACO backend below has separately
recorded Forge and NeoForge refs.8 fixture evidence.

## Optional ACO backend (alpha.5)

The server's generated `ae2mrp-server.toml` selects `plannerBackend = "AE2"` (default)
or `"ACO"`. The common enum is independent of loaders; each loader registers a
server config spec. No client preference changes the server's planner.

ACO selection lets the actual installed NativeVm.calculate own the Future,
stock capture, existing worker scheduling, CRAFT_LESS and result materialization.
The adapter does not submit separate jobs per route or merge independently
planned branch inventories. There is no production shadow/reference calculation.

At the server-side VM prepare boundary, pin immutable output shares, source
identities and control identities. After VM pattern capture, retain detached
candidate lists. Compute reverse dependencies once; aggregate compilation is
unavailable only for controlled outputs and their ancestors. Other ingredient
subgraphs keep the existing VM aggregate and bytecode paths.

Inside the VM craft boundary (after stock/emitter handling), execute selected
real source processes against the same VM state. STRICT preserves allocations.
ADAPTIVE trials use VM child states, disable hypothetical Missing in try/finally,
commit only accepted child diffs, and retain only committed process trees.
The VM's ordinary byte and tree accounting, missing simulation and CRAFT_LESS
remain active. Control advertisements never become executable patterns.
Because replacing a control's candidate list invalidates the VM's sticky
multiple-paths flag, result construction derives that flag from the retained
process tree, matching AE2's hasMultiplePaths semantics. This metadata pass is
worker-owned and never reads live providers or inventories.

A thread-local scope exists only around the original VM execute call and is
restored in finally, including failure/cancel. It adds no process-wide plan
cache or completed-Future table and retains no order context after execute.
Final source/control validation
uses the VM's existing exclusive live-read interval; a composite session requests
that scheduling path even when its arithmetic needs no live callbacks.

There is no suitable public VM control-policy SPI in the inspected artifact.
Therefore three optional @Pseudo mixins and one cached reflection layout bind
the known refs.8 VM ABI; no VM classes are bundled or edited. Required targets
use require=1. Unsupported ABI/absent VM fails explicitly, never by allowing an
ordinary single-producer result to ignore configured shares. This is a pinned
integration, not a universal compatibility contract.

ACO arithmetic remains BigInteger through demands, operations and child trials;
ACO retains its own result/sidecar limits and consumers. Native AE2 mode remains
checked-long only. Wide physical completion and third-party CPU acceptance are
not established by the long-range parity fixtures.

Pinned control identities, current grid/service identity and mounted real
patterns are checked again before returning a plan. Changed/removed controls are
rejected explicitly, not relabeled as missing ingredients or ignored in favor
of an ordinary producer. Native pause/lifecycle remains. The presence set is
bounded by attached providers, invalidated by their native lifecycle and owned
only by that grid service; it contains no plans or completed Futures.

## Save and editing ownership

Forge 1.20.1: ae2mrp item NBT. NeoForge 1.21.1: registered persistent,
network-synchronized composite_configuration Data Component. Both encode schema,
primary output, full source item data, source identities, ordered weights and
enabled flags, STRICT mode and monotonic configurationRevision. Unknown schemas
and mismatched identities fail; no speculative legacy migration exists.

Physical source slots are never consumed by encoding. Re-editing creates preview
copies only: they cannot be picked up or dropped on breaking. Only one blank or
encoded carrier is consumed after validation; one encoded output is produced.
The old encoded carrier is untouched until commit. Slot actions and weights use
the vanilla validated menu protocol; no custom packet mutates arbitrary positions.

## Network terminal (alpha.6)

The thin terminal exposes an in-world grid node through each loader's AE2
capability. Initialization is scheduled on the first server tick; removal and
chunk unload destroy the node. No block ticker is installed.

The menu retains only one 30-pattern page and a grid identity, not a full copied
network index. Opening, explicit refresh, paging and source selection enumerate
the current crafting service. Selecting a source pins its exact definition and
original grid in a non-transferable reference, then searches the SAME primary
AEKey in AE2's candidate order. No name matching, recipe-manager search or
per-tick full scan occurs. A reference selection cannot modify cursor contents
or extract the original provider item; creative clone, throw and shift extraction
are blocked as well. Before encoding, referenced definitions must still be
mounted in the pinned, currently connected active grid.

Editing and encoding do not advertise controls. Only the 54 physical
registration slots publish controls through ICraftingProvider; registration
changes refresh that native provider. Duplicate primary-output controls in the
same terminal are rejected. Existing external Provider registration remains
compatible. Controls expand to the original live mounted source patterns; this
terminal never accepts execution inputs. Changing drafts does not rewrite a
registered pattern or an accepted job.

Physical editor sources are returned to player inventory after successful menu
encoding. Inventory overflow stays in the source slot; it is not dropped.
Network references are not persisted or dropped. Registered physical controls
are separately saved and dropped once on block removal. Ghost copies are never
part of a physical drop container. Both GUI tabs fit the same native 312x240
surface, including the smallest normal Minecraft GUI width.

## AE2 styling and wireless access (alpha.7)

EncoderScreen references AE2's native background texture, SLOT_BACKGROUND,
horizontal TabButton, native IconButton sprites and the 1.21.1 text palette.
The 312x240 slot geometry and menu commands remain unchanged. No runtime copy
of AE2 artwork, planner change or menu-inventory migration is introduced.

WirelessPatternTerminalItem extends AE2 WirelessTerminalItem. The standard
GridLinkables handler owns WAP linking and the inherited battery owns charging.
A separate GlobalPos pins the wired composite terminal; it is not a second
network connection or a provider owned by a handheld item.

WirelessData is loader-specific: Forge NBT and NeoForge CustomData preserve
AE2's link and battery state. Each menu owns one WirelessAccess lease. Forge
uses WirelessTerminalMenuHost.rangeCheck/onBroadcastChanges, NeoForge uses its
current updateConnectedAccessPoint/updateLinkStatus/tick. No power/range formula
is reimplemented. Both pin the original active grid, bound block instance and
the exact handheld stack/slot. No force loading or rebinding an old lease occurs.

Only the distance check to the wired block is replaced by this validated radio
lease; all original menu commands, source checks and slot accounting still run.
The hosting handheld slot is synchronized and locked against pickup, shift and
hotbar/offhand swaps. Battery exhaustion, out-of-range, item replacement or an
unavailable/moved target invalidates the menu. Registration remains in the wired
terminal; closing the handheld never deletes accepted jobs or registered items.
No remote world/storage mutation or external CPU ownership is introduced.

## Search and UI wording (alpha.8)

The browser reuses AE2 AETextField and the pattern-access terminal's ScreenStyle.
It does not inherit the native provider-editing menu: candidate clicks still
create non-transferable source references and cannot remove provider patterns.
The encoder slots and controls are hidden in browser view; the 30 candidate
slots move below the search field. Menu slot indices and saved data do not change.

Client-localized registered item names and registry IDs are searched on the
client. The request contains a registry-sized matching-item bitset, not recipes
or inventory mutations. Forge and NeoForge handlers run on the server thread,
validate the current menu id and wired/wireless access, and intersect it with
the server's actual craftables before pagination. Exact primary-AEKey filtering,
candidate order and pre-encode source validation remain unchanged. The filter
is menu-local; no polling, persistent index, provider packet interception or
planner change is added. Item names mean registered base-item names, not custom
component names, provider names, ingredient text or native PAT search operators.

STRICT/ADAPTIVE remain the persisted values. Only their visible labels change
to actions on material shortage. Empty-slot labels become tooltips; the generic
idle/ready text is suppressed. The encode button remains a native arrow.

## Native pattern-access picker (alpha.9)

The wired block implements the platform's native pattern-access host contract.
NativePatternMenu disables remote provider inventory actions (including NeoForge
bulk insertion). PatternPickerScreen overrides PatternSlot clicks only, retaining
AE2's native screen, style, row grouping, search and scrolling. Player quick-move
into providers is disabled. No AE2 classes, packets or source are overwritten.

Selection uses AEBaseMenu's existing client-action protocol and a complete
AEItemKey codec. The server accepts it only in the current valid picker and
against the originally pinned live grid; EncoderState revalidates mounted source,
duplicate identity, primary AEKey and supported processing semantics. Only a
non-transferable reference is created. Accepted output identity is synchronized
through native GuiSync; the client puts its localized display name into the
native search field through one client-only field accessor. Search remains AE2's
search, not a custom authoritative recipe filter.

The editor now has 55 slots, including the unchanged source/carrier/output/player
and registration indices. Opening/returning switches menus without returning or
duplicating the carried stack. A picker retains one editor context, not a global
session map. Reopening preserves the same inventory, selected slot and wireless
lease. Wireless ticking accepts AbstractContainerMenu so the same native radio
lease runs while the picker is visible. Invalid range, battery, target or grid
prevents selection and return. Closed pickers reject mutation. Closing the picker
does not drop references or remove registered composites.

## Deliberate limits

- Strict native AEProcessingPattern only, one item output, fixed exact inputs.
- No nested composites, substitutions, crafting-grid patterns, return containers,
  dynamic outputs, mid-job reallocation or external CPU ownership.
- Emitting a controlled output is rejected as COMPOSITE_EMITTER_CONFLICT instead
  of allowing the native emitter shortcut to silently ignore the configured ratio.
- Input/output amounts outside native long range fail; pure BigInteger allocation
  tests do not prove wide native planning.
- Reachability preflight visits each reachable key once; no additional global
  cache/invalidation system. This is a recipe-control addon, not an optimizer.
- Original item images are retained under artwork/original, outside game resources.
  Game textures are deterministic 16x16 nearest-neighbor exports; no AI editing.
  Encoder block uses referenced AE2 textures. GUI visual acceptance is separate
  from GameTests.

Primary code references:
[AE2 upstream](https://github.com/AppliedEnergistics/Applied-Energistics-2),
[ModDevGradle isolated sources and runs](https://github.com/neoforged/ModDevGradle).
