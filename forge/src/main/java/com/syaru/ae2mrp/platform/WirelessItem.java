package com.syaru.ae2mrp.platform;

import com.syaru.ae2mrp.encoder.WirelessPatternTerminalItem;
import net.minecraft.world.entity.player.Player;

public final class WirelessItem extends WirelessPatternTerminalItem {
  @Override
  public boolean openFromInventory(Player player, int slot) {
    return openRemote(player, slot);
  }
}
