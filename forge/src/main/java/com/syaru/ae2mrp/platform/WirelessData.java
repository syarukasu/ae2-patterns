package com.syaru.ae2mrp.platform;

import com.syaru.ae2mrp.encoder.WirelessAccess;
import com.syaru.ae2mrp.encoder.WirelessPatternTerminalItem;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class WirelessData {
  private static final String TARGET = "ae2mrp_terminal";
  private static final String SIDE = "ae2mrp_terminal_side";

  public static void bind(ItemStack stack, GlobalPos target) {
    bind(stack, target, -1);
  }

  public static void bind(ItemStack stack, GlobalPos target, int side) {
    if (side < -1 || side > 5) throw new IllegalArgumentException("INVALID_TERMINAL_SIDE");
    var tag = stack.getOrCreateTag();
    tag.put(TARGET, GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, target).result().orElseThrow());
    tag.putInt(SIDE, side);
  }

  public static int side(ItemStack stack) {
    var tag = stack.getTag();
    return tag == null || !tag.contains(SIDE)
        ? -1
        : tag.contains(SIDE, net.minecraft.nbt.Tag.TAG_INT) ? tag.getInt(SIDE) : -2;
  }

  public static GlobalPos target(ItemStack stack) {
    var tag = stack.getTag();
    return tag == null || !tag.contains(TARGET)
        ? null
        : GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get(TARGET)).result().orElse(null);
  }

  public static WirelessAccess access(
      WirelessPatternTerminalItem item, Player player, int slot, ItemStack stack, EncoderHost be) {
    var grid = be.terminal().grid();
    var radio =
        new appeng.helpers.WirelessTerminalMenuHost(
            player, slot, stack, (p, sub) -> item.openRemote(p, slot));
    return new WirelessAccess() {
      private boolean active = true;

      public int inventorySlot() {
        return slot;
      }

      public boolean valid() {
        return active
            && item.stillBound(player, slot, stack, be)
            && be.terminal().grid() == grid
            && radio.rangeCheck()
            && (player.isCreative() || item.hasPower(player, .5, stack));
      }

      public void tick(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        if (!valid()) {
          active = false;
          return;
        }
        active = radio.onBroadcastChanges(menu);
      }
    };
  }
}
