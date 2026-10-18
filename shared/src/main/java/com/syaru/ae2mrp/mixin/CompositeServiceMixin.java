package com.syaru.ae2mrp.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.CraftingCalculation;
import appeng.me.service.CraftingService;
import com.syaru.ae2mrp.pattern.CompositePlanningContext;
import com.syaru.ae2mrp.pattern.CompositeServiceAccess;
import com.syaru.ae2mrp.pattern.ControlPattern;
import com.syaru.ae2mrp.pattern.ControlReachability;
import com.syaru.ae2mrp.platform.Platform;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A grid-owned presence index, published at native provider registration boundaries. */
@Mixin(value = CraftingService.class, remap = false, priority = 3000)
public abstract class CompositeServiceMixin implements CompositeServiceAccess {
  @Shadow @Final private IGrid grid;
  @Shadow @Final private static ExecutorService CRAFTING_POOL;

  @Unique
  private final Set<Object> ae2mrp$controlOwners =
      Collections.newSetFromMap(new IdentityHashMap<>());

  @Unique private volatile boolean ae2mrp$controls;

  @Override
  public boolean ae2mrp$hasControls() {
    return ae2mrp$controls;
  }

  @Override
  public void ae2mrp$updateProvider(Object owner, ICraftingProvider provider) {
    boolean contains =
        provider != null
            && provider.getAvailablePatterns().stream().anyMatch(ControlPattern.class::isInstance);
    if (contains) ae2mrp$controlOwners.add(owner);
    else ae2mrp$controlOwners.remove(owner);
    ae2mrp$controls = !ae2mrp$controlOwners.isEmpty();
  }

  @Inject(method = "addNode", at = @At("RETURN"), require = 1)
  private void ae2mrp$added(IGridNode node, CompoundTag saved, CallbackInfo ci) {
    ae2mrp$updateProvider(node, node.getService(ICraftingProvider.class));
  }

  @Inject(method = "refreshNodeCraftingProvider", at = @At("RETURN"), require = 1)
  private void ae2mrp$refreshed(IGridNode node, CallbackInfo ci) {
    ae2mrp$updateProvider(node, node.getService(ICraftingProvider.class));
  }

  @Inject(method = "removeNode", at = @At("RETURN"), require = 1)
  private void ae2mrp$removed(IGridNode node, CallbackInfo ci) {
    ae2mrp$updateProvider(node, null);
  }

  @Inject(method = "beginCraftingCalculation", at = @At("HEAD"), cancellable = true, require = 1)
  private void ae2mrp$selectNative(
      Level level,
      ICraftingSimulationRequester requester,
      AEKey output,
      long amount,
      CalculationStrategy strategy,
      CallbackInfoReturnable<Future<ICraftingPlan>> cir) {
    var selected =
        ae2mrp$tryNative(level, requester, output, java.math.BigInteger.valueOf(amount), strategy);
    if (selected != null) cir.setReturnValue(selected);
  }

  @Override
  public Future<ICraftingPlan> ae2mrp$tryNative(
      Level level,
      ICraftingSimulationRequester requester,
      AEKey output,
      java.math.BigInteger amount,
      CalculationStrategy strategy) {
    if (!ae2mrp$controls
        || level == null
        || level.isClientSide
        || requester == null
        || output == null
        || amount.signum() <= 0) return null;
    if (level.getServer() == null || !level.getServer().isSameThread())
      throw new IllegalStateException("COMPOSITE_REQUEST_MUST_START_ON_SERVER_THREAD");
    var controls = ControlReachability.collect((CraftingService) (Object) this, output);
    if (controls.isEmpty()) return null;
    for (var key : controls.keySet())
      if (((CraftingService) (Object) this).canEmitFor(key))
        throw new IllegalArgumentException("COMPOSITE_EMITTER_CONFLICT");
    if (com.syaru.ae2mrp.platform.PlannerConfig.useAco()) {
      com.syaru.ae2mrp.compat.AcoVmAccess.requireAvailable();
      return null;
    }
    if (amount.bitLength() > 63)
      throw new IllegalArgumentException("COMPOSITE_WIDE_PLANNING_UNSUPPORTED");
    var job =
        new CraftingCalculation(
            level, grid, requester, new GenericStack(output, amount.longValueExact()), strategy);
    ((CompositePlanningContext) job).ae2mrp$selectNative((CraftingService) (Object) this, controls);
    return CRAFTING_POOL.submit(job::run);
  }

  @Inject(method = "submitJob", at = @At("HEAD"), require = 1)
  private void ae2mrp$rejectExecutableControl(
      ICraftingPlan plan,
      ICraftingRequester requester,
      ICraftingCPU cpu,
      boolean power,
      IActionSource source,
      CallbackInfoReturnable<ICraftingSubmitResult> cir) {
    if (plan.patternTimes().keySet().stream()
        .anyMatch(
            p -> p instanceof ControlPattern || p.getDefinition().getItem() == Platform.encoded()))
      throw new IllegalArgumentException("COMPOSITE_CONTROL_IN_EXECUTABLE_PLAN");
  }
}
