package com.syaru.ae2mrp.client;

import appeng.client.gui.WidgetContainer;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.style.StyleManager;
import appeng.menu.SlotSemantics;
import com.syaru.ae2mrp.mixin.EncodingScreenAccess;
import com.syaru.ae2mrp.mixin.EncodingWidgetsAccess;
import com.syaru.ae2mrp.platform.NativeEncodingMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Embeds AE2's actual encoding panels, tabs, buttons, tooltips and slot layout. */
final class NativeEncodingPanel extends PatternEncodingTermScreen<NativeEncodingMenu> {
  private final EncoderScreen owner;

  NativeEncodingPanel(NativeEncodingMenu menu, Inventory inventory, EncoderScreen owner) {
    super(
        menu,
        inventory,
        Component.empty(),
        StyleManager.loadStyleDoc("/screens/terminals/pattern_encoding_terminal.json"));
    this.owner = owner;
    var access = (EncodingWidgetsAccess) (Object) widgets;
    access.ae2mrp$widgets().keySet().removeIf(id -> !encodingWidget(id));
    access.ae2mrp$composites().keySet().removeIf(id -> !encodingWidget(id));
  }

  private static boolean encodingWidget(String id) {
    return id.startsWith("modePanel")
        || id.startsWith("modeTabButton")
        || id.startsWith("crafting")
        || id.startsWith("processing")
        || id.startsWith("smithing")
        || id.startsWith("stonecutting")
        || id.equals("encodePattern");
  }

  void mergeInto(WidgetContainer parent) {
    var source = (EncodingWidgetsAccess) (Object) widgets;
    var target = (EncodingWidgetsAccess) (Object) parent;
    target.ae2mrp$widgets().putAll(source.ae2mrp$widgets());
    target.ae2mrp$composites().putAll(source.ae2mrp$composites());
  }

  void attach(Rect2i bounds, WidgetContainer parent) {
    minecraft = Minecraft.getInstance();
    font = minecraft.font;
    width = owner.width;
    height = owner.height;
    leftPos = bounds.getX();
    topPos = bounds.getY();
    imageWidth = bounds.getWidth();
    imageHeight = bounds.getHeight();
    children().clear();
    renderables.clear();
    // Native panels retain this container; share the owner's already resolved tooltip areas.
    var areas = ((EncodingWidgetsAccess) (Object) widgets).ae2mrp$tooltips();
    areas.clear();
    areas.putAll(((EncodingWidgetsAccess) (Object) parent).ae2mrp$tooltips());
    for (var semantic : getStyle().getSlots().keySet()) {
      var key = SlotSemantics.get(semantic);
      if (!menu.getSlots(key).isEmpty()) {
        setSlotsHidden(key, false);
        repositionSlots(key);
      }
    }
    prepare();
  }

  void prepare() {
    menu.setCarried(owner.getMenu().getCarried());
    var access = (EncodingScreenAccess) (Object) this;
    access
        .ae2mrp$panels()
        .forEach(
            (mode, panel) -> {
              boolean selected = mode == menu.getMode();
              access.ae2mrp$tabs().get(mode).setSelected(selected);
              panel.setVisible(selected);
            });
  }

  void drawNativeTooltip(GuiGraphics graphics, Slot slot, int x, int y) {
    hoveredSlot = slot;
    super.renderTooltip(graphics, x, y);
  }

  boolean openAmount(double x, double y, int button) {
    return minecraft.options.keyPickItem.matchesMouse(button) && super.mouseClicked(x, y, button);
  }

  @Override
  public void init() {
    // Native amount/recipe sub-screens return to this adapter; keep the composite screen active.
    minecraft.setScreen(owner);
  }
}
