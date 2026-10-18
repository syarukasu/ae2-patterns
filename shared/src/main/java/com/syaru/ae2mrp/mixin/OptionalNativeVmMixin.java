package com.syaru.ae2mrp.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.stacks.AEKey;
import com.syaru.ae2mrp.pattern.CompositeServiceAccess;
import java.math.BigInteger;
import java.util.concurrent.Future;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native VM takes ownership at service HEAD, earlier than CraftingCalculation exists. */
@Pseudo
@Mixin(targets = "com.ae2vm.addon.nativeengine.NativeVm", remap = false)
public abstract class OptionalNativeVmMixin {
  @Inject(method = "calculate", at = @At("HEAD"), cancellable = true, require = 1)
  private static void ae2mrp$routeControl(
      IGrid grid,
      Level level,
      ICraftingSimulationRequester requester,
      AEKey output,
      BigInteger amount,
      CalculationStrategy strategy,
      CallbackInfoReturnable<Future<ICraftingPlan>> cir) {
    if (com.syaru.ae2mrp.platform.PlannerConfig.useAco()) return;
    if (grid.getCraftingService() instanceof CompositeServiceAccess service) {
      var selected = service.ae2mrp$tryNative(level, requester, output, amount, strategy);
      if (selected != null) cir.setReturnValue(selected);
    }
  }
}
