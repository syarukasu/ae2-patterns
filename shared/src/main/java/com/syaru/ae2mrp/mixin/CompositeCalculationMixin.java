package com.syaru.ae2mrp.mixin;

import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingCalculation;
import com.syaru.ae2mrp.pattern.CompositePlanningContext;
import com.syaru.ae2mrp.pattern.CompositeServiceAccess;
import com.syaru.ae2mrp.pattern.ControlPattern;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Select native AE2 calculation before a single-producer optimizer takes ownership. */
@Mixin(value = CraftingCalculation.class, remap = false, priority = 2000)
public abstract class CompositeCalculationMixin implements CompositePlanningContext {
  @Unique private boolean ae2mrp$nativeSelected;
  @Unique private boolean ae2mrp$probing;
  @Unique private appeng.api.networking.crafting.ICraftingService ae2mrp$selectedService;

  @Override
  public boolean ae2mrp$probing() {
    return ae2mrp$probing;
  }

  @Override
  public boolean ae2mrp$setProbing(boolean probing) {
    boolean previous = ae2mrp$probing;
    ae2mrp$probing = probing;
    return previous;
  }

  @Override
  public void ae2mrp$selectNative(
      appeng.api.networking.crafting.ICraftingService service,
      Map<AEKey, ControlPattern> controls) {
    ae2mrp$nativeSelected = true;
    ae2mrp$selectedService = service;
    ae2mrp$rules.putAll(controls);
  }

  @Unique private final Map<AEKey, ControlPattern> ae2mrp$rules = new LinkedHashMap<>();

  @Override
  public void ae2mrp$observe(AEKey key, ControlPattern rule) {
    var previous = ae2mrp$rules.putIfAbsent(key, rule);
    if (previous != null && !previous.equals(rule))
      throw new IllegalStateException("COMPOSITE_CONFIGURATION_CHANGED");
  }

  @Shadow @Final private AEKey output;
  @Shadow @Final private long requestedAmount;
  @Shadow @Final private CalculationStrategy strategy;

  @Inject(method = "computePlan", at = @At("HEAD"), cancellable = true, require = 1)
  private void ae2mrp$nativeAccounting(CallbackInfoReturnable<ICraftingPlan> cir)
      throws InterruptedException {
    var access = (CalculationAccess) this;
    var node = access.ae2mrp$requester().getGridNode();
    if (node == null) return;
    var service = node.getGrid().getCraftingService();
    if (ae2mrp$selectedService != null && service != ae2mrp$selectedService)
      throw new IllegalStateException("COMPOSITE_NETWORK_CHANGED");
    if (!ae2mrp$nativeSelected
        && service instanceof CompositeServiceAccess hooks
        && !hooks.ae2mrp$hasControls()) return;
    var queue = new ArrayDeque<AEKey>();
    var visited = new HashSet<AEKey>();
    queue.add(output);
    boolean found = ae2mrp$nativeSelected;
    while (!queue.isEmpty() && !found) {
      access.ae2mrp$pause();
      var key = queue.removeFirst();
      if (!visited.add(key)) continue;
      for (var pattern : service.getCraftingFor(key)) {
        if (pattern instanceof ControlPattern) {
          found = true;
          break;
        }
        for (var input : pattern.getInputs())
          for (var possible : input.getPossibleInputs()) queue.addLast(possible.what());
      }
    }
    if (!found) return;
    var result = access.ae2mrp$attempt(false, requestedAmount);
    if (result == null && strategy == CalculationStrategy.CRAFT_LESS) {
      long successful = 0;
      for (long increment = Long.highestOneBit(requestedAmount); increment > 0; increment /= 2) {
        long test = successful + increment;
        if (test < requestedAmount) {
          var attempt = access.ae2mrp$attempt(false, test);
          if (attempt != null) {
            result = attempt;
            successful = test;
          }
        }
      }
    }
    if (result == null) result = access.ae2mrp$attempt(true, requestedAmount);
    if (result.patternTimes().keySet().stream().anyMatch(ControlPattern.class::isInstance))
      throw new IllegalStateException("COMPOSITE_CONTROL_IN_EXECUTABLE_PLAN");
    var currentNode = access.ae2mrp$requester().getGridNode();
    if (currentNode == null || currentNode.getGrid().getCraftingService() != service)
      throw new IllegalStateException("COMPOSITE_NETWORK_CHANGED");
    for (var entry : ae2mrp$rules.entrySet()) {
      var current = service.getCraftingFor(entry.getKey());
      if (!current.contains(entry.getValue()))
        throw new IllegalStateException("COMPOSITE_CONFIGURATION_CHANGED");
    }
    for (var source : result.patternTimes().keySet()) {
      if (!service.getCraftingFor(source.getPrimaryOutput().what()).contains(source))
        throw new IllegalStateException("SOURCE_PATTERN_REMOVED_DURING_PLANNING");
    }
    cir.setReturnValue(result);
  }
}
