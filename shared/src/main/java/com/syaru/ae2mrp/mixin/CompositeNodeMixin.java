package com.syaru.ae2mrp.mixin;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.CraftBranchFailure;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingTreeNode;
import appeng.crafting.CraftingTreeProcess;
import appeng.crafting.inv.ChildCraftingSimulationState;
import appeng.crafting.inv.CraftingSimulationState;
import com.syaru.ae2mrp.core.AdaptiveDistributor;
import com.syaru.ae2mrp.core.DistributionMode;
import com.syaru.ae2mrp.core.StrictDistributor;
import com.syaru.ae2mrp.pattern.AdaptiveTrial;
import com.syaru.ae2mrp.pattern.CompositeDefinition;
import com.syaru.ae2mrp.pattern.CompositePlanningContext;
import com.syaru.ae2mrp.pattern.ControlPattern;
import java.math.BigInteger;
import java.util.ArrayList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftingTreeNode.class, remap = false)
public abstract class CompositeNodeMixin {
  @Shadow @Final private CraftingCalculation job;
  @Shadow @Final private AEKey what;
  @Shadow @Final private long amount;
  @Shadow @Final private CraftingTreeProcess parent;
  @Shadow private ArrayList<CraftingTreeProcess> nodes;

  // A capacity probe must fail on missing ingredients, even during the final
  // native Missing preview. Its discarded child inventory must not publish Missing.
  @Redirect(
      method = "request",
      at = @At(value = "INVOKE", target = "Lappeng/crafting/CraftingCalculation;isSimulation()Z"),
      require = 1)
  private boolean ae2mrp$probeDoesNotInventInputs(CraftingCalculation calculation) {
    return !((CompositePlanningContext) calculation).ae2mrp$probing() && calculation.isSimulation();
  }

  // AE2 already consumed existing stock and handled emitters at this exact point.
  @Inject(
      method = "request",
      at =
          @At(value = "INVOKE", target = "Lappeng/crafting/CraftingTreeNode;buildChildPatterns()V"),
      cancellable = true,
      require = 1)
  private void ae2mrp$expand(
      CraftingSimulationState inv, long requested, KeyCounter containers, CallbackInfo ci)
      throws CraftBranchFailure, InterruptedException {
    var gridNode = ((CalculationAccess) job).ae2mrp$requester().getGridNode();
    if (gridNode == null) return;
    var service = gridNode.getGrid().getCraftingService();
    var candidates = service.getCraftingFor(what);
    ControlPattern control = null;
    for (var candidate : candidates)
      if (candidate instanceof ControlPattern found) {
        if (control != null && !control.equals(found))
          throw new IllegalStateException("AMBIGUOUS_COMPOSITE_PATTERN");
        control = found;
      }
    if (control == null) return;
    ((CompositePlanningContext) job).ae2mrp$observe(what, control);
    var definition = control.definition();
    var total = BigInteger.valueOf(amount).multiply(BigInteger.valueOf(requested));
    if (definition.mode() == DistributionMode.ADAPTIVE) {
      ae2mrp$adaptive(inv, total.longValueExact(), definition, candidates, service);
      ci.cancel();
      return;
    }
    var split = StrictDistributor.distribute(total, definition.routes());
    split.produced().longValueExact();
    // Preflight the entire split before mutating even the simulation inventory.
    var processes = new ArrayList<CraftingTreeProcess>();
    var counts = new ArrayList<Long>();
    for (var allocation : split.allocations()) {
      var configured = definition.routes().get(allocation.index());
      if (!configured.enabled()
          || configured.weight() == 0
          || allocation.operations().signum() == 0) {
        processes.add(null);
        counts.add(0L);
        continue;
      }
      IPatternDetails original = definition.details().get(allocation.index());
      var actual =
          candidates.stream()
              .filter(
                  p ->
                      !(p instanceof ControlPattern)
                          && p.getDefinition().equals(original.getDefinition()))
              .findFirst();
      if (actual.isEmpty())
        throw new IllegalStateException(
            "SOURCE_PATTERN_NOT_ON_NETWORK: " + original.getDefinition());
      if (parent != null && !((ProcessAccess) parent).ae2mrp$notRecursive(actual.get()))
        throw new IllegalStateException("COMPOSITE_RECURSION: " + what);
      long operations = allocation.operations().longValueExact();
      ae2mrp$checkNativeAmounts(actual.get(), operations);
      processes.add(
          new CraftingTreeProcess(service, job, actual.get(), (CraftingTreeNode) (Object) this));
      counts.add(operations);
      allocation.produced().longValueExact();
    }
    long remaining = total.longValueExact();
    nodes = new ArrayList<>();
    for (var process : processes) if (process != null) nodes.add(process);
    for (int i = 0; i < processes.size(); i++) {
      if (counts.get(i) == 0) continue;
      ((ProcessAccess) processes.get(i)).ae2mrp$request(inv, counts.get(i));
      long target = split.allocations().get(i).target().longValueExact();
      long extracted = inv.extract(what, target, Actionable.MODULATE);
      if (extracted != target)
        throw new IllegalStateException("COMPOSITE_OUTPUT_ACCOUNTING_MISMATCH");
      remaining -= extracted;
    }
    if (remaining != 0) throw new IllegalStateException("COMPOSITE_DEMAND_NOT_CONSERVED");
    ci.cancel();
  }

  @Unique
  private void ae2mrp$adaptive(
      CraftingSimulationState inv,
      long demand,
      CompositeDefinition definition,
      java.util.Collection<IPatternDetails> candidates,
      ICraftingService service)
      throws CraftBranchFailure, InterruptedException {
    var originals = new ArrayList<IPatternDetails>();
    for (int i = 0; i < definition.routes().size(); i++) {
      var route = definition.routes().get(i);
      if (!route.enabled() || route.weight() == 0) {
        originals.add(null);
        continue;
      }
      var configured = definition.details().get(i);
      var original =
          candidates.stream()
              .filter(
                  p ->
                      !(p instanceof ControlPattern)
                          && p.getDefinition().equals(configured.getDefinition()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "SOURCE_PATTERN_NOT_ON_NETWORK: " + configured.getDefinition()));
      if (parent != null && !((ProcessAccess) parent).ae2mrp$notRecursive(original))
        throw new IllegalStateException("COMPOSITE_RECURSION: " + what);
      originals.add(original);
    }
    nodes = new ArrayList<>();
    final long[] produced = {0};
    var allocation =
        AdaptiveDistributor.distribute(
            BigInteger.valueOf(demand),
            definition.routes(),
            (index, target) -> {
              var original = originals.get(index);
              var accepted = ae2mrp$capacity(inv, target.longValueExact(), original, service);
              if (accepted == null) return BigInteger.ZERO;
              produced[0] = Math.addExact(produced[0], accepted.produced());
              accepted.inventory().applyDiff(inv);
              nodes.add(accepted.process());
              return BigInteger.valueOf(accepted.target());
            });
    long missing = allocation.missing().longValueExact();
    // Rounding excess from real accepted batches may cover an otherwise missing share.
    missing -= inv.extract(what, missing, Actionable.MODULATE);
    if (missing == 0) return;
    if (!job.isSimulation() || ((CompositePlanningContext) job).ae2mrp$probing())
      throw new CraftBranchFailure(what, missing);
    // Only the final native simulation may record hypothetical ingredients.
    // Failed capacity probes above never enter this path.
    var remainder = StrictDistributor.distribute(BigInteger.valueOf(missing), definition.routes());
    produced[0] = Math.addExact(produced[0], remainder.produced().longValueExact());
    for (var share : remainder.allocations()) {
      if (share.operations().signum() == 0) continue;
      var original = originals.get(share.index());
      long times = share.operations().longValueExact();
      ae2mrp$checkNativeAmounts(original, times);
      var process =
          new CraftingTreeProcess(service, job, original, (CraftingTreeNode) (Object) this);
      ((ProcessAccess) process).ae2mrp$request(inv, times);
      long expected = share.target().longValueExact();
      if (inv.extract(what, expected, Actionable.MODULATE) != expected)
        throw new IllegalStateException("COMPOSITE_OUTPUT_ACCOUNTING_MISMATCH");
      nodes.add(process);
    }
  }

  @Unique
  private AdaptiveTrial ae2mrp$capacity(
      CraftingSimulationState inv, long target, IPatternDetails original, ICraftingService service)
      throws InterruptedException {
    long unit = original.getPrimaryOutput().amount();
    long maximum = Math.addExact((target - 1) / unit, 1);
    var full = ae2mrp$trial(inv, target, maximum, original, service);
    if (full != null) return full;
    AdaptiveTrial best = null;
    long low = 0, high = maximum - 1;
    while (low < high) {
      long distance = high - low;
      long middle = low + distance / 2 + distance % 2;
      var trial = ae2mrp$trial(inv, target, middle, original, service);
      if (trial == null) high = middle - 1;
      else {
        low = middle;
        best = trial;
      }
    }
    return best;
  }

  @Unique
  private AdaptiveTrial ae2mrp$trial(
      CraftingSimulationState inv,
      long target,
      long operations,
      IPatternDetails original,
      ICraftingService service)
      throws InterruptedException {
    long output = ae2mrp$checkNativeAmounts(original, operations);
    var child = new ChildCraftingSimulationState(inv);
    var process = new CraftingTreeProcess(service, job, original, (CraftingTreeNode) (Object) this);
    var context = (CompositePlanningContext) job;
    boolean previous = context.ae2mrp$setProbing(true);
    try {
      ((ProcessAccess) process).ae2mrp$request(child, operations);
      long provided = Math.min(target, output);
      if (child.extract(what, provided, Actionable.MODULATE) != provided)
        throw new IllegalStateException("COMPOSITE_OUTPUT_ACCOUNTING_MISMATCH");
      return new AdaptiveTrial(process, child, provided, output);
    } catch (CraftBranchFailure unavailable) {
      return null;
    } finally {
      context.ae2mrp$setProbing(previous);
    }
  }

  @Unique
  private static long ae2mrp$checkNativeAmounts(IPatternDetails original, long operations) {
    for (var output : original.getOutputs()) Math.multiplyExact(output.amount(), operations);
    for (var input : original.getInputs())
      for (var possible : input.getPossibleInputs())
        Math.multiplyExact(
            Math.multiplyExact(possible.amount(), input.getMultiplier()), operations);
    return Math.multiplyExact(original.getPrimaryOutput().amount(), operations);
  }
}
