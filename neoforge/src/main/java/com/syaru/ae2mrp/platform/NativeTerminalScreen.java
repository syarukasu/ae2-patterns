package com.syaru.ae2mrp.platform;

import appeng.client.gui.me.patternaccess.PatternAccessTermScreen;
import appeng.client.gui.style.StyleManager;
import com.syaru.ae2mrp.encoder.EncoderMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public abstract class NativeTerminalScreen extends PatternAccessTermScreen<EncoderMenu> {
  protected double uiScale = 1;

  protected NativeTerminalScreen(EncoderMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title, StyleManager.loadStyleDoc("/screens/ae2mrp/terminal.json"));
  }

  @Override
  public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
    if (scrollRegistration(x / uiScale, y / uiScale, vertical)) return true;
    return super.mouseScrolled(x / uiScale, y / uiScale, horizontal, vertical);
  }

  protected abstract boolean scrollRegistration(double x, double y, double delta);
}
