package com.syaru.ae2mrp.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;

/** Provider advertisement only. It must never appear in an executable plan. */
public abstract class ControlPattern implements IPatternDetails {
  private final AEItemKey identity;
  private final CompositeDefinition definition;

  protected ControlPattern(AEItemKey identity, CompositeDefinition definition) {
    this.identity = identity;
    this.definition = definition;
  }

  public final CompositeDefinition definition() {
    return definition;
  }

  @Override
  public final AEItemKey getDefinition() {
    return identity;
  }

  @Override
  public final IInput[] getInputs() {
    return new IInput[0];
  }

  @Override
  public final boolean supportsPushInputsToExternalInventory() {
    return false;
  }

  @Override
  public final void pushInputsToExternalInventory(KeyCounter[] inputs, PatternInputSink sink) {
    throw new IllegalStateException("COMPOSITE_CONTROL_IS_NOT_EXECUTABLE");
  }

  @Override
  public final boolean equals(Object other) {
    return other instanceof ControlPattern control && identity.equals(control.identity);
  }

  @Override
  public final int hashCode() {
    return identity.hashCode();
  }
}
