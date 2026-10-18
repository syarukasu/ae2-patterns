package com.syaru.ae2mrp.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingService;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.CraftingTreeProcess;
import com.syaru.ae2mrp.pattern.ControlPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingTreeProcess.class, remap = false)
public class ControlProcessGuardMixin {
  @Inject(method = "<init>", at = @At("RETURN"), require = 1)
  private void ae2mrp$rejectControl(
      ICraftingService service,
      CraftingCalculation job,
      IPatternDetails details,
      CraftingTreeNode parent,
      CallbackInfo ci) {
    if (details instanceof ControlPattern)
      throw new IllegalStateException("COMPOSITE_CONTROL_IS_NOT_A_RECIPE");
  }
}
