package com.syaru.ae2mrp.mixin;

import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingPlan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CraftingCalculation.class, remap = false)
public interface CalculationAccess {
  @Accessor("simRequester")
  ICraftingSimulationRequester ae2mrp$requester();

  @Invoker("handlePausing")
  void ae2mrp$pause() throws InterruptedException;

  @Invoker("runCraftAttempt")
  CraftingPlan ae2mrp$attempt(boolean simulate, long amount) throws InterruptedException;
}
