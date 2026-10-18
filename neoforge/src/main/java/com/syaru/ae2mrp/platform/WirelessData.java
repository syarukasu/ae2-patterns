package com.syaru.ae2mrp.platform;

import com.syaru.ae2mrp.encoder.WirelessAccess;
import com.syaru.ae2mrp.encoder.WirelessPatternTerminalItem;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class WirelessData {
  private static final String TARGET = "ae2mrp_terminal";

  public static void bind(ItemStack stack, GlobalPos target) {
    var tag =
        stack
            .getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY)
            .copyTag();
    tag.put(TARGET, GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, target).result().orElseThrow());
    stack.set(
        net.minecraft.core.component.DataComponents.CUSTOM_DATA,
        net.minecraft.world.item.component.CustomData.of(tag));
  }

  public static GlobalPos target(ItemStack stack) {
    var tag =
        stack
            .getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY)
            .copyTag();
    return tag == null || !tag.contains(TARGET)
        ? null
        : GlobalPos.CODEC.parse(NbtOps.INSTANCE, tag.get(TARGET)).result().orElse(null);
  }

  public static WirelessAccess access(
      WirelessPatternTerminalItem item,
      Player player,
      int slot,
      ItemStack stack,
      EncoderBlockEntity be) {
    var grid = be.terminal.grid();
    var radio = new Radio(item, player, slot);
    return new WirelessAccess() {
      private boolean active = true;

      public int inventorySlot() {
        return slot;
      }

      public boolean valid() {
        return active
            && item.stillBound(player, slot, stack, be)
            && be.terminal.grid() == grid
            && radio.connected();
      }

      public void tick(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        if (!valid()) {
          active = false;
          return;
        }
        radio.tick();
        active = radio.getLinkStatus().connected();
      }
    };
  }

  private static final class Radio
      extends appeng.helpers.WirelessTerminalMenuHost<WirelessPatternTerminalItem> {
    Radio(WirelessPatternTerminalItem item, Player player, int slot) {
      super(
          item,
          player,
          appeng.menu.locator.MenuLocators.forInventorySlot(slot),
          (p, sub) -> item.openRemote(p, slot));
    }

    boolean connected() {
      updateConnectedAccessPoint();
      updateLinkStatus();
      return isValid() && getLinkStatus().connected();
    }
  }
}
