package com.syaru.ae2mrp.platform;

import com.syaru.ae2mrp.encoder.WirelessPatternTerminalItem;
import net.minecraft.world.entity.player.Player;

public final class WirelessItem extends WirelessPatternTerminalItem {
  @Override
  public boolean openFromInventory(Player player, appeng.menu.locator.ItemMenuHostLocator locator) {
    var slot = locator.getPlayerInventorySlot();
    return slot != null && openRemote(player, slot);
  }
}
