package com.syaru.ae2mrp.pattern;

import appeng.api.stacks.AEKey;

/** Per-calculation rule identity, never a process-wide cache. */
public interface CompositePlanningContext {
  void ae2mrp$selectNative(
      appeng.api.networking.crafting.ICraftingService service,
      java.util.Map<AEKey, ControlPattern> controls);

  void ae2mrp$observe(AEKey output, ControlPattern rule);

  boolean ae2mrp$probing();

  boolean ae2mrp$setProbing(boolean probing);
}
