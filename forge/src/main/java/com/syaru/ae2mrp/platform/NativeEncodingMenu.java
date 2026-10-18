package com.syaru.ae2mrp.platform;

import appeng.menu.me.items.PatternEncodingTermMenu;
import net.minecraft.world.entity.player.Inventory;

/** Native encoder slots and operations; the enclosing menu owns all network synchronization. */
public final class NativeEncodingMenu extends PatternEncodingTermMenu {
  private final EncoderBlockEntity host;

  public NativeEncodingMenu(int id, Inventory inventory, EncoderBlockEntity host) {
    super(Platform.menu(), id, inventory, host, false);
    this.host = host;
    refreshFromHost();
  }

  @Override
  public void broadcastChanges() {
    // Native recipe preview calls this while assembling its temporary grid.
    // Only the enclosing menu may synchronize, not a nested preview callback.
  }

  public void refreshFromHost() {
    mode = host.encoding.getMode();
    substitute = host.encoding.isSubstitution();
    substituteFluids = host.encoding.isFluidSubstitution();
    stonecuttingRecipeId = host.encoding.getStonecuttingRecipeId();
    if (isClientSide()) applySynchronizedState();
  }

  public void applySynchronizedState() {
    super.onServerDataSync();
  }
}
