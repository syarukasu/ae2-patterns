package com.syaru.ae2mrp.platform;

import appeng.helpers.InventoryAction;
import appeng.menu.implementations.PatternAccessTermMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public abstract class NativePatternMenu extends PatternAccessTermMenu {
  protected NativePatternMenu(
      MenuType<?> type, int id, Inventory inventory, EncoderBlockEntity host) {
    super(type, id, inventory, host, false);
  }

  @Override
  public void doAction(ServerPlayer player, InventoryAction action, int slot, long id) {
    // This picker never removes or inserts patterns in a remote provider.
  }

  @Override
  public void onServerDataSync() {
    super.onServerDataSync();
    afterNativeDataSync();
  }

  protected void afterNativeDataSync() {}
}
