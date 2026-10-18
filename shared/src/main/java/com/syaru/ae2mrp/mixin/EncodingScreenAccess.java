package com.syaru.ae2mrp.mixin;

import appeng.client.gui.me.items.EncodingModePanel;
import appeng.client.gui.me.items.PatternEncodingTermScreen;
import appeng.client.gui.widgets.TabButton;
import appeng.parts.encoding.EncodingMode;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PatternEncodingTermScreen.class, remap = false)
public interface EncodingScreenAccess {
  @Accessor("modePanels")
  Map<EncodingMode, EncodingModePanel> ae2mrp$panels();

  @Accessor("modeTabButtons")
  Map<EncodingMode, TabButton> ae2mrp$tabs();
}
