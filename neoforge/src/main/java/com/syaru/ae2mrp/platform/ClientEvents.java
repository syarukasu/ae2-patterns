package com.syaru.ae2mrp.platform;

import appeng.api.util.AEColor;
import com.syaru.ae2mrp.MrpMod;
import com.syaru.ae2mrp.client.EncoderScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MrpMod.ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
  @SubscribeEvent
  public static void blockColors(
      net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event) {
    event.register(
        (state, level, pos, tint) -> AEColor.PURPLE.getVariantByTintIndex(tint),
        MrpMod.ENCODER.get());
  }

  @SubscribeEvent
  public static void itemColors(
      net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
    event.register(
        (stack, tint) -> AEColor.PURPLE.getVariantByTintIndex(tint), MrpMod.ENCODER_ITEM.get());
  }

  @SubscribeEvent
  public static void setup(RegisterMenuScreensEvent event) {
    event.register(MrpMod.MENU.get(), EncoderScreen::new);
  }
}
