package com.syaru.ae2mrp.mixin;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.WidgetContainer;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = WidgetContainer.class, remap = false)
public interface EncodingWidgetsAccess {
  @Accessor("widgets")
  Map<String, AbstractWidget> ae2mrp$widgets();

  @Accessor("compositeWidgets")
  Map<String, ICompositeWidget> ae2mrp$composites();

  @Accessor("tooltips")
  Map<String, Object> ae2mrp$tooltips();

  @Invoker("populateScreen")
  void ae2mrp$populate(Consumer<AbstractWidget> consumer, Rect2i bounds, AEBaseScreen<?> screen);
}
