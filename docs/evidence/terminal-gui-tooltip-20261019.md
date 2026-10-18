# Alpha.11 native terminal GUI initialization

## Observed failure

Prism syarukasutime1.2.46 latest.log, 2026-10-19 01:11:46 JST:

```
java.lang.IllegalArgumentException: No tooltip with id 'processing-primary-output' is defined
at appeng.client.gui.WidgetContainer.setTooltipAreaEnabled(WidgetContainer.java:345)
at appeng.client.gui.me.items.ProcessingEncodingPanel.updateTooltipVisibility(ProcessingEncodingPanel.java:81)
at appeng.client.gui.me.items.ProcessingEncodingPanel.setVisible(ProcessingEncodingPanel.java:108)
at com.syaru.ae2mrp.client.NativeEncodingPanel.lambda$prepare$2(NativeEncodingPanel.java:80)
at com.syaru.ae2mrp.client.NativeEncodingPanel.attach(NativeEncodingPanel.java:68)
at com.syaru.ae2mrp.client.EncoderScreen.positionRegistrationRow(EncoderScreen.java:154)
```

The screen never opened; the client task failed and the integrated connection
was subsequently closed. This is not a block-model failure or an inventory-HUD
conflict. The earlier getLevel linkage failure is a separate alpha.10 fix.

## Cause and change

Native panel objects were moved into the outer WidgetContainer, but still held
their constructor-supplied inner container. Only the outer container had its
style populated, leaving the inner tooltip-area table empty. setVisible(false)
also updates processing tooltip visibility, so opening any mode could fail.

attach now binds the outer screen's resolved tooltip-area objects into the native
panel container before prepare. Both references observe the SAME enabled flags
and coordinates, including after resize and registration scrolling. No fallback
or ignored exception is used. The native panels and actions remain AE2-owned.

The English PAT title was shortened to avoid overlapping the search box.
Placed-terminal geometry now directly composes AE2's native frame and lit
pattern-encoding display. Native UVs and tint indices are not reimplemented.

## Graphical regression fixture

The optional Forge clientSmoke source set starts an actual LWJGL Minecraft client,
loads only a copy of the generated GameTest world and opens a real server menu.
It checks all four native modes, shared tooltip-area identity, live mounted
provider patterns, exactly nine visible registrations, row six, checkbox sync,
GUI scaling, mouse clicks, close and reopen. It also checks at least 14 native
baked quads, non-missing sprites and z bounds 13/16 through 1 for the wall model.

Screenshots and a PASS/FAIL report are produced by the client itself. An absent
or failed report fails the Gradle task. Tests are not included in the mod JAR.

Early fixture failures were not accepted: the smoke mod lacked pack.mcmeta;
the copied world's player could drown; starting before position synchronization
put the terminal out of reach; losing window focus paused the integrated server.
The fixture now has pack metadata and uses a fixed server-side creative flying
position with pause-on-lost-focus disabled only in the isolated test client.
Production access rules and test assertions were not relaxed.

This does not establish NeoForge graphical-client acceptance, large-pack runtime
compatibility, performance improvements or all recipe types. Those claims are
not inferred from a Forge GUI fixture or server GameTests.
