package com.syaru.ae2mrp.client;

import appeng.api.stacks.AEItemKey;
import appeng.client.gui.Icon;
import appeng.client.gui.me.patternaccess.PatternSlot;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.AECheckbox;
import com.syaru.ae2mrp.core.DistributionMode;
import com.syaru.ae2mrp.encoder.EncoderMenu;
import com.syaru.ae2mrp.encoder.TerminalLogic;
import com.syaru.ae2mrp.mixin.EncodingWidgetsAccess;
import com.syaru.ae2mrp.mixin.PatternAccessScreenAccess;
import com.syaru.ae2mrp.platform.NativeTerminalScreen;
import com.syaru.ae2mrp.platform.Platform;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Native PAT above native pattern encoding; only the middle share editor is addon UI. */
public final class EncoderScreen extends NativeTerminalScreen {
  private static final int MIDDLE_HEIGHT = 144, NATIVE_FOOTER_HEIGHT = 180;
  private static final int TAIL_HEIGHT = MIDDLE_HEIGHT + NATIVE_FOOTER_HEIGHT;
  private static final int MIN_HEIGHT = 17 + 2 * 18 + TAIL_HEIGHT + 12;
  private static final int INK = 0xff413f54;
  private final EditBox[] weights = new EditBox[8];
  private final AECheckbox[] toggles = new AECheckbox[8];
  private final Button[] clears = new Button[8];
  private final AEItemKey[] cached = new AEItemKey[8];
  private final ItemStack[] outputs = new ItemStack[8];
  private NativeEncodingPanel encoding;
  private AECheckbox redistribute;
  private Button encode, previousRow, nextRow;
  private int registrationRow;
  private int middleY;
  private String searchedOutput = "";

  public EncoderScreen(EncoderMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    java.util.Arrays.fill(outputs, ItemStack.EMPTY);
    encoding = new NativeEncodingPanel(menu.nativeEncoder(), inventory, this);
    encoding.mergeInto(widgets);
  }

  @Override
  public void init() {
    int realWidth = minecraft.getWindow().getGuiScaledWidth();
    int realHeight = minecraft.getWindow().getGuiScaledHeight();
    uiScale = Math.min(1.0, (realHeight - 8.0) / MIN_HEIGHT);
    width = (int) Math.round(realWidth / uiScale);
    height = (int) Math.round(realHeight / uiScale);
    super.init();
    int rows = Math.max(2, Math.min(6, (height - TAIL_HEIGHT - 29) / 18));
    var access = (PatternAccessScreenAccess) (Object) this;
    access.ae2mrp$visibleRows(rows);
    imageHeight = 17 + rows * 18 + TAIL_HEIGHT;
    leftPos = (width - imageWidth) / 2;
    topPos = (height - imageHeight) / 2;
    middleY = 17 + rows * 18;
    clearWidgets();
    ((EncodingWidgetsAccess) (Object) widgets)
        .ae2mrp$populate(
            this::addRenderableWidget, new Rect2i(leftPos, topPos, imageWidth, imageHeight), this);
    access.ae2mrp$resetScrollbar();
    for (int i = 0; i < 8; i++) {
      final int route = i;
      int x = leftPos + 10 + i % 4 * 44, y = topPos + middleY + 16 + i / 4 * 35;
      var field = new EditBox(font, x, y + 18, 29, 12, Component.translatable("gui.ae2mrp.weight"));
      field.setMaxLength(3);
      field.setFilter(s -> s.isEmpty() || s.matches("[0-9]{1,3}") && Integer.parseInt(s) <= 100);
      field.setValue(Integer.toString(menu.weight(i)));
      field.setResponder(
          value -> {
            if (field.isFocused() && !value.isEmpty()) command(route, Integer.parseInt(value));
          });
      weights[i] = addRenderableWidget(field);
      toggles[i] =
          addRenderableWidget(new AECheckbox(x + 18, y + 1, 14, 14, getStyle(), Component.empty()));
      toggles[i].setChangeListener(() -> command(route, 109));
      toggles[i].setTooltip(Tooltip.create(Component.translatable("gui.ae2mrp.toggle")));
      clears[i] =
          addRenderableWidget(
              new NativeIconButton(
                  x + 34, y, Icon.CLEAR, "clear_preview", () -> command(route, 110)));
    }
    redistribute =
        addRenderableWidget(
            new AECheckbox(
                leftPos + 8,
                topPos + middleY + 88,
                180,
                14,
                getStyle(),
                Component.translatable("gui.ae2mrp.adaptive")));
    redistribute.setChangeListener(() -> command(-1, redistribute.isSelected() ? 122 : 121));
    previousRow =
        addRenderableWidget(
            new NativeIconButton(
                leftPos + 175,
                topPos + middleY + 126,
                Icon.ARROW_UP,
                "previous_registration_row",
                () -> changeRegistrationRow(-1)));
    nextRow =
        addRenderableWidget(
            new NativeIconButton(
                leftPos + 175,
                topPos + middleY + 135,
                Icon.ARROW_DOWN,
                "next_registration_row",
                () -> changeRegistrationRow(1)));
    var encodeArrow =
        new NativeIconButton(
            leftPos + 34,
            topPos + middleY + 108,
            Icon.ARROW_RIGHT,
            "encode",
            () -> command(-1, 120));
    encodeArrow.setHalfSize(false);
    encode = encodeArrow;
    addRenderableWidget(encode);
    positionCompositeSlots();
    refresh();
  }

  private void command(int route, int action) {
    if (route >= 0) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 101 + route);
    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
  }

  private void positionCompositeSlots() {
    for (var source : EncoderMenu.SOURCES) repositionSlots(source);
    repositionSlots(EncoderMenu.CARRIER);
    repositionSlots(EncoderMenu.RESULT);
    positionRegistrationRow();
    repositionSlots(appeng.menu.SlotSemantics.PLAYER_INVENTORY);
    repositionSlots(appeng.menu.SlotSemantics.PLAYER_HOTBAR);
  }

  private void positionRegistrationRow() {
    for (int row = 0; row < EncoderMenu.REGISTERED.length; row++)
      setSlotsHidden(EncoderMenu.REGISTERED[row], row != registrationRow);
    repositionSlots(EncoderMenu.REGISTERED[registrationRow]);
    previousRow.active = registrationRow > 0;
    nextRow.active = registrationRow < TerminalLogic.REGISTRATION_ROWS - 1;
    // Unhiding a semantic repositions all outer slots; restore the native mode's slot visibility.
    encoding.attach(new Rect2i(leftPos, topPos, imageWidth, imageHeight), widgets);
  }

  private void changeRegistrationRow(int delta) {
    int row = Math.max(0, Math.min(TerminalLogic.REGISTRATION_ROWS - 1, registrationRow + delta));
    if (row != registrationRow) {
      registrationRow = row;
      positionRegistrationRow();
    }
  }

  @Override
  protected boolean scrollRegistration(double x, double y, double delta) {
    if (x < leftPos
        || x >= leftPos + imageWidth
        || y < topPos + middleY + 124
        || y >= topPos + middleY + MIDDLE_HEIGHT) return false;
    if (delta != 0) changeRegistrationRow(delta > 0 ? -1 : 1);
    return true;
  }

  private int total() {
    int sum = 0;
    for (int i = 0; i < 8; i++)
      if (menu.enabled(i) && menu.getSlot(i).hasItem()) sum += menu.weight(i);
    return sum;
  }

  private void refresh() {
    for (int i = 0; i < 8; i++) {
      boolean present = menu.getSlot(i).hasItem();
      weights[i].active = present && menu.enabled(i);
      if (!weights[i].isFocused()) weights[i].setValue(Integer.toString(menu.weight(i)));
      toggles[i].visible = present;
      toggles[i].setSelected(menu.enabled(i));
      clears[i].visible = present && menu.preview(i);
      var stack = menu.getSlot(i).getItem();
      var key = AEItemKey.of(stack);
      if (!Objects.equals(key, cached[i])) {
        cached[i] = key;
        outputs[i] = ItemStack.EMPTY;
        try {
          var details = Platform.decodeSource(stack, minecraft.level);
          if (details != null && details.getPrimaryOutput().what() instanceof AEItemKey output)
            outputs[i] = output.toStack();
        } catch (IllegalArgumentException invalid) {
          outputs[i] = ItemStack.EMPTY;
        }
      }
    }
    redistribute.setSelected(menu.mode() == DistributionMode.ADAPTIVE);
    redistribute.setTooltip(
        Tooltip.create(
            Component.translatable(
                redistribute.isSelected()
                    ? "gui.ae2mrp.adaptive_hint"
                    : "gui.ae2mrp.strict_hint")));
    String ready = readiness();
    encode.active = ready.equals("ready");
    encode.setTooltip(
        Tooltip.create(
            Component.translatable(
                ready.equals("ready") ? "gui.ae2mrp.encode" : "status.ae2mrp." + ready)));
  }

  private String readiness() {
    if (menu.getSlot(9).hasItem()) return "output_occupied";
    int routes = 0, positive = 0;
    for (int i = 0; i < 8; i++)
      if (menu.getSlot(i).hasItem()) {
        routes++;
        if (menu.enabled(i) && menu.weight(i) > 0) positive++;
      }
    if (routes < 2) return "route_count";
    if (positive < 2) return "positive_route_count";
    if (total() != 100) return "weight_total";
    var carrier = menu.getSlot(8).getItem();
    return carrier.is(Platform.blank()) || carrier.is(Platform.encoded())
        ? "ready"
        : "carrier_required";
  }

  @Override
  public void updateBeforeRender() {
    super.updateBeforeRender();
    if (encoding == null) return;
    refresh();
    encoding.prepare();
    if (!searchedOutput.equals(menu.outputKey)) {
      searchedOutput = menu.outputKey;
      if (!searchedOutput.isEmpty()) {
        try {
          var key =
              Platform.readKey(
                  net.minecraft.nbt.TagParser.parseTag(searchedOutput), minecraft.level);
          if (key != null)
            ((PatternAccessScreenAccess) (Object) this)
                .ae2mrp$searchField()
                .setValue(key.getDisplayName().getString());
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException invalid) {
          throw new IllegalStateException("INVALID_PATTERN_ACCESS_OUTPUT", invalid);
        }
      }
    }
  }

  @Override
  public void drawBG(GuiGraphics g, int x, int y, int mouseX, int mouseY, float partial) {
    super.drawBG(g, x, y, mouseX, mouseY, partial);
    // The new share section is the only resized background; native sections render themselves.
    var background = Blitter.texture("guis/background.png");
    background.src(1, 1, 254, 254).dest(x + 1, y + middleY, 193, MIDDLE_HEIGHT).blit(g);
    background.src(0, 1, 1, 254).dest(x, y + middleY, 1, MIDDLE_HEIGHT).blit(g);
    background.src(255, 1, 1, 254).dest(x + 194, y + middleY, 1, MIDDLE_HEIGHT).blit(g);
    Blitter.texture("guis/pattern.png")
        .src(0, 71, 195, NATIVE_FOOTER_HEIGHT)
        .dest(x, y + imageHeight - NATIVE_FOOTER_HEIGHT)
        .blit(g);
    for (int i = 0; i < 10; i++) drawSlot(g, x, y, menu.getSlot(i));
    int first = EncoderMenu.REGISTRY_START + registrationRow * TerminalLogic.REGISTRATION_COLUMNS;
    for (int i = first; i < first + TerminalLogic.REGISTRATION_COLUMNS; i++)
      drawSlot(g, x, y, menu.getSlot(i));
  }

  private static void drawSlot(GuiGraphics g, int x, int y, Slot slot) {
    Icon.SLOT_BACKGROUND.getBlitter().dest(x + slot.x - 1, y + slot.y - 1, 18, 18).blit(g);
  }

  @Override
  public void drawFG(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
    super.drawFG(g, x, y, mouseX, mouseY);
    g.drawString(font, Component.translatable("gui.ae2mrp.registered"), 8, middleY + 4, INK, false);
    g.drawString(
        font, Component.translatable("gui.ae2mrp.total", total()), 83, middleY + 112, INK, false);
    for (int i = 0; i < 8; i++)
      g.drawString(font, "%", 41 + i % 4 * 44, middleY + 36 + i / 4 * 35, INK, false);
    g.drawString(
        font,
        (registrationRow + 1) + "/" + TerminalLogic.REGISTRATION_ROWS,
        174,
        middleY + 114,
        INK,
        false);
    for (var output : outputs)
      if (!output.isEmpty()) {
        g.renderFakeItem(output, 174, middleY + 1);
        break;
      }
  }

  @Override
  protected void slotClicked(Slot slot, int index, int button, ClickType type) {
    if (slot instanceof PatternSlot) {
      if (type == ClickType.PICKUP && button == 0) menu.requestReference(slot.getItem());
      return;
    }
    super.slotClicked(slot, index, button, type);
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
    g.pose().pushPose();
    g.pose().scale((float) uiScale, (float) uiScale, 1);
    super.render(g, (int) (mouseX / uiScale), (int) (mouseY / uiScale), partial);
    g.pose().popPose();
  }

  @Override
  protected void renderTooltip(GuiGraphics g, int x, int y) {
    if (encoding != null && menu.nativeSlot(hoveredSlot))
      encoding.drawNativeTooltip(g, hoveredSlot, x, y);
    else super.renderTooltip(g, x, y);
  }

  @Override
  public boolean mouseClicked(double x, double y, int button) {
    x /= uiScale;
    y /= uiScale;
    if (y >= topPos + imageHeight - NATIVE_FOOTER_HEIGHT && encoding.openAmount(x, y, button))
      return true;
    return super.mouseClicked(x, y, button);
  }

  @Override
  public boolean mouseReleased(double x, double y, int button) {
    return super.mouseReleased(x / uiScale, y / uiScale, button);
  }

  @Override
  public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
    return super.mouseDragged(x / uiScale, y / uiScale, button, dx / uiScale, dy / uiScale);
  }

  private static final class NativeIconButton extends appeng.client.gui.widgets.IconButton {
    private final Icon icon;

    NativeIconButton(int x, int y, Icon icon, String name, Runnable action) {
      super(button -> action.run());
      this.icon = icon;
      setX(x);
      setY(y);
      setHalfSize(true);
      setMessage(Component.translatable("gui.ae2mrp." + name));
      setTooltip(Tooltip.create(getMessage()));
    }

    @Override
    protected Icon getIcon() {
      return icon;
    }
  }
}
