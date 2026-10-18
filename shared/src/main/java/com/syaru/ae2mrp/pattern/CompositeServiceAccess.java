package com.syaru.ae2mrp.pattern;

import appeng.api.networking.crafting.ICraftingProvider;

public interface CompositeServiceAccess {
  java.util.concurrent.Future<appeng.api.networking.crafting.ICraftingPlan> ae2mrp$tryNative(
      net.minecraft.world.level.Level level,
      appeng.api.networking.crafting.ICraftingSimulationRequester requester,
      appeng.api.stacks.AEKey output,
      java.math.BigInteger amount,
      appeng.api.networking.crafting.CalculationStrategy strategy);

  boolean ae2mrp$hasControls();

  void ae2mrp$updateProvider(Object owner, ICraftingProvider provider);
}
