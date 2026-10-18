package com.syaru.ae2mrp.mixin;

import appeng.client.gui.me.patternaccess.PatternAccessTermScreen;
import appeng.client.gui.widgets.AETextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = PatternAccessTermScreen.class, remap = false)
public interface PatternAccessScreenAccess {
  @Accessor("searchField")
  AETextField ae2mrp$searchField();

  @Accessor("visibleRows")
  void ae2mrp$visibleRows(int rows);

  @Invoker("resetScrollbar")
  void ae2mrp$resetScrollbar();
}
