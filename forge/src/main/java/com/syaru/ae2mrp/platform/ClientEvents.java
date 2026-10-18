package com.syaru.ae2mrp.platform;

import appeng.api.util.AEColor;
import com.syaru.ae2mrp.MrpMod;
import com.syaru.ae2mrp.client.EncoderScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
    modid = MrpMod.ID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT)
public final class ClientEvents {
  @SubscribeEvent
  public static void blockColors(
      net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event) {
    event.register(
        (state, level, pos, tint) -> AEColor.PURPLE.getVariantByTintIndex(tint),
        MrpMod.ENCODER.get());
  }

  @SubscribeEvent
  public static void itemColors(
      net.minecraftforge.client.event.RegisterColorHandlersEvent.Item event) {
    event.register(
        (stack, tint) -> AEColor.PURPLE.getVariantByTintIndex(tint), MrpMod.ENCODER_ITEM.get());
  }

  @SubscribeEvent
  public static void setup(FMLClientSetupEvent event) {
    event.enqueueWork(
        () -> {
          MenuScreens.register(MrpMod.MENU.get(), EncoderScreen::new);
        });
  }
}
