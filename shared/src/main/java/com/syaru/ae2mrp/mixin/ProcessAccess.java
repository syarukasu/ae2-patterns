package com.syaru.ae2mrp.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.CraftingTreeProcess;
import appeng.crafting.inv.CraftingSimulationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CraftingTreeProcess.class, remap = false)
public interface ProcessAccess {
  @Invoker("request")
  void ae2mrp$request(CraftingSimulationState inventory, long times)
      throws CraftBranchFailure, InterruptedException;

  @Invoker("notRecursive")
  boolean ae2mrp$notRecursive(IPatternDetails pattern);
}
