package com.syaru.ae2mrp.mixin;

import appeng.api.crafting.IPatternDetails;
import com.syaru.ae2mrp.compat.AcoCompositeSession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.ae2vm.addon.vm.VmDemandProgram$Compiler", remap = false)
public abstract class OptionalAcoAggregateMixin {
  @Inject(method = "get", at = @At("HEAD"), cancellable = true, require = 1)
  private void ae2mrp$preserveControl(IPatternDetails pattern, CallbackInfoReturnable<Object> cir) {
    if (AcoCompositeSession.blockAggregate(pattern)) cir.setReturnValue(null);
  }
}
