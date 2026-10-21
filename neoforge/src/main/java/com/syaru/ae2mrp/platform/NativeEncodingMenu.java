package com.syaru.ae2mrp.platform;

import appeng.menu.me.items.PatternEncodingTermMenu;
import net.minecraft.world.entity.player.Inventory;

/** Native encoder slots and operations; the enclosing menu owns all network synchronization. */
public final class NativeEncodingMenu extends PatternEncodingTermMenu {
  private final EncoderHost host;

  public NativeEncodingMenu(int id, Inventory inventory, EncoderHost host) {
    super(Platform.menu(), id, inventory, host, false);
    this.host = host;
    refreshFromHost();
  }

  @Override
  protected boolean hideViewCells() {
    // The enclosing PAT menu has no view-cell slots, regardless of the physical host.
    return true;
  }

  @Override
  public void broadcastChanges() {
    // Native recipe preview calls this while assembling its temporary grid.
    // Only the enclosing menu may synchronize, not a nested preview callback.
  }

  public void refreshFromHost() {
    mode = host.getLogic().getMode();
    substitute = host.getLogic().isSubstitution();
    substituteFluids = host.getLogic().isFluidSubstitution();
    stonecuttingRecipeId = host.getLogic().getStonecuttingRecipeId();
    if (isClientSide()) applySynchronizedState();
  }

  public void applySynchronizedState() {
    super.onServerDataSync(it.unimi.dsi.fastutil.shorts.ShortSets.EMPTY_SET);
  }
}
