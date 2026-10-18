package com.syaru.ae2mrp.encoder;

/** Menu-scoped native AE2 radio/power lease; registered controls remain in the wired block. */
public interface WirelessAccess {
  int inventorySlot();

  boolean valid();

  void tick(net.minecraft.world.inventory.AbstractContainerMenu menu);
}
