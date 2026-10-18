package com.syaru.ae2mrp.mixin;

import appeng.api.networking.crafting.ICraftingProvider;
import appeng.me.service.CraftingService;
import com.syaru.ae2mrp.pattern.CompositeServiceAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingService.class, remap = false)
public abstract class GlobalControlProviderMixin {
  @Inject(
      method = {"addGlobalCraftingProvider", "refreshGlobalCraftingProvider"},
      at = @At("RETURN"),
      require = 1)
  private void ae2mrp$globalChanged(ICraftingProvider provider, CallbackInfo ci) {
    ((CompositeServiceAccess) this).ae2mrp$updateProvider(provider, provider);
  }

  @Inject(method = "removeGlobalCraftingProvider", at = @At("RETURN"), require = 1)
  private void ae2mrp$globalRemoved(ICraftingProvider provider, CallbackInfo ci) {
    ((CompositeServiceAccess) this).ae2mrp$updateProvider(provider, null);
  }
}
