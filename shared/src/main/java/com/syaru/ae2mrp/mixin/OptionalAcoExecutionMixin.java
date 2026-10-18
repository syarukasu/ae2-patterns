package com.syaru.ae2mrp.mixin;

import com.syaru.ae2mrp.compat.AcoCompositeSession;
import java.math.BigInteger;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.ae2vm.addon.vm.VmInstructionExecution", remap = false)
public abstract class OptionalAcoExecutionMixin {
  @ModifyArg(
      method = {"attempt", "materializeDirect"},
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/ae2vm/addon/vm/VmCraftingPlan;<init>(Lcom/ae2vm/addon/vm/VmStack;Ljava/math/BigInteger;ZZLcom/ae2vm/addon/vm/VmKeyCounter;Lcom/ae2vm/addon/vm/VmKeyCounter;Lcom/ae2vm/addon/vm/VmKeyCounter;Ljava/util/Map;)V"),
      index = 3,
      require = 1)
  private boolean ae2mrp$retainedPaths(boolean original) {
    return AcoCompositeSession.multiplePaths(this, original);
  }

  @Inject(method = "craft", at = @At("HEAD"), cancellable = true, require = 1)
  private void ae2mrp$distribute(
      @Coerce Object node,
      BigInteger demand,
      @Coerce Object state,
      List<?> choices,
      CallbackInfoReturnable<BigInteger> cir) {
    if (AcoCompositeSession.expand(this, node, demand, state)) cir.setReturnValue(BigInteger.ZERO);
  }

  // A controlled set is one policy, not competing AE2 producers. Preserve the
  // final simulation flag; discarded ADAPTIVE probes switch it themselves.
  @Redirect(
      method = "request",
      at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"),
      slice =
          @Slice(
              from =
                  @At(
                      value = "INVOKE",
                      target =
                          "Lcom/ae2vm/addon/vm/VmInstructionExecution$Node;processes()Ljava/util/List;")),
      require = 1)
  private int ae2mrp$onePolicy(List<?> choices) {
    return AcoCompositeSession.controlledChoices(choices) ? 1 : choices.size();
  }
}
