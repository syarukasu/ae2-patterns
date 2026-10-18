package com.syaru.ae2mrp.mixin;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.syaru.ae2mrp.compat.AcoCompositeSession;
import com.syaru.ae2mrp.compat.AcoVmAccess;
import com.syaru.ae2mrp.pattern.ControlPattern;
import com.syaru.ae2mrp.pattern.ControlReachability;
import com.syaru.ae2mrp.platform.PlannerConfig;
import java.util.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Capture controls beside ACO's snapshot, then scope its existing VM execution. */
@Pseudo
@Mixin(targets = "com.ae2vm.addon.nativeengine.NativeVmPlanningContext", remap = false)
public abstract class OptionalAcoContextMixin {
  @Shadow @Final private IGrid grid;
  @Shadow @Final private Level level;
  @Shadow @Final private Map<AEKey, List<IPatternDetails>> programsByKey;
  @Shadow @Final private Map<String, IPatternDetails> bindings;
  @Unique private ICraftingService ae2mrp$service;
  @Unique private Map<AEKey, ControlPattern> ae2mrp$controls = Map.of();
  @Unique private Map<AEKey, AcoCompositeSession.Rule> ae2mrp$rules = Map.of();
  @Unique private AcoCompositeSession ae2mrp$session;
  @Unique private Map<AEItemKey, IPatternDetails> ae2mrp$originals = Map.of();

  @Inject(method = "prepare", at = @At("HEAD"), require = 1)
  private void ae2mrp$capture(AEKey root, CallbackInfo ci) {
    if (!PlannerConfig.useAco()) return;
    if (level.getServer() == null || !level.getServer().isSameThread())
      throw new IllegalStateException("COMPOSITE_REQUEST_MUST_START_ON_SERVER_THREAD");
    ae2mrp$service = grid.getCraftingService();
    if (ae2mrp$service instanceof com.syaru.ae2mrp.pattern.CompositeServiceAccess hooks
        && !hooks.ae2mrp$hasControls()) return;
    ae2mrp$controls = ControlReachability.collect(ae2mrp$service, root);
    var rules = new LinkedHashMap<AEKey, AcoCompositeSession.Rule>();
    ae2mrp$controls.forEach(
        (key, control) -> {
          if (ae2mrp$service.canEmitFor(key))
            throw new IllegalArgumentException("COMPOSITE_EMITTER_CONFLICT");
          rules.put(key, AcoCompositeSession.Rule.capture(control));
        });
    ae2mrp$rules = Map.copyOf(rules);
  }

  @Inject(method = "prepare", at = @At("RETURN"), require = 1)
  private void ae2mrp$captured(AEKey root, CallbackInfo ci) {
    if (!ae2mrp$rules.isEmpty()) {
      AcoVmAccess.requireAvailable();
      ae2mrp$session = new AcoCompositeSession(ae2mrp$rules, programsByKey);
      var originals = new HashMap<AEItemKey, IPatternDetails>();
      bindings.values().forEach(pattern -> originals.put(pattern.getDefinition(), pattern));
      ae2mrp$originals = Map.copyOf(originals);
    }
  }

  @Inject(method = "requiresLiveAccess", at = @At("HEAD"), cancellable = true, require = 1)
  private void ae2mrp$finalValidationWindow(CallbackInfoReturnable<Boolean> cir) {
    if (ae2mrp$session != null) cir.setReturnValue(true);
  }

  @Coerce
  @Redirect(
      method = "plan",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/ae2vm/addon/vm/CraftingVM;execute(Lcom/ae2vm/addon/vm/CraftingBytecode;Lcom/ae2vm/addon/vm/VmSimulationState;Z)Lcom/ae2vm/addon/vm/VmCraftingPlan;"),
      require = 1)
  private Object ae2mrp$execute(
      @Coerce Object vm, @Coerce Object code, @Coerce Object inventory, boolean less) {
    if (ae2mrp$session == null) return AcoVmAccess.execute(vm, code, inventory, less);
    var result = ae2mrp$session.execute(vm, code, inventory, less);
    var used =
        AcoVmAccess.times(result).keySet().stream().map(IPatternDetails::getDefinition).toList();
    AcoVmAccess.validate(
        this,
        () -> {
          if (grid.getCraftingService() != ae2mrp$service)
            throw new IllegalStateException("COMPOSITE_NETWORK_CHANGED");
          ae2mrp$controls.forEach(
              (key, control) -> {
                if (!ae2mrp$service.getCraftingFor(key).contains(control))
                  throw new IllegalStateException("COMPOSITE_CONFIGURATION_CHANGED");
              });
          for (var identity : used) {
            var source = ae2mrp$originals.get(identity);
            if (source == null || source instanceof ControlPattern)
              throw new IllegalStateException("COMPOSITE_CONTROL_IN_EXECUTABLE_PLAN");
            if (!ae2mrp$service.getCraftingFor(source.getPrimaryOutput().what()).contains(source))
              throw new IllegalStateException("SOURCE_PATTERN_REMOVED_DURING_PLANNING");
          }
        });
    org.slf4j.LoggerFactory.getLogger("ae2mrp.planner")
        .debug(
            "route=ACO_VM controlledKeys={} expansions={}",
            ae2mrp$rules.size(),
            ae2mrp$session.expansions());
    return result;
  }
}
