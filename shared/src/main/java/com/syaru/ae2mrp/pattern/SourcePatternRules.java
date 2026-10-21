package com.syaru.ae2mrp.pattern;

import appeng.api.crafting.IPatternDetails;
import com.syaru.ae2mrp.platform.Platform;

/** Validate advertised quantities without replacing native matching/return callbacks. */
public final class SourcePatternRules {
  private SourcePatternRules() {}

  public static boolean supported(IPatternDetails pattern) {
    if (pattern == null || pattern instanceof ControlPattern || pattern.getDefinition() == null)
      return false;
    var outputs = Platform.outputs(pattern);
    var inputs = pattern.getInputs();
    if (outputs.isEmpty() || inputs == null) return false;
    for (var output : outputs)
      if (output == null || output.what() == null || output.amount() <= 0) return false;
    for (var input : inputs) {
      if (input == null || input.getMultiplier() <= 0) return false;
      var alternatives = input.getPossibleInputs();
      if (alternatives == null || alternatives.length == 0) return false;
      for (var possible : alternatives)
        if (possible == null || possible.what() == null || possible.amount() <= 0) return false;
    }
    return pattern.getPrimaryOutput() != null && pattern.getPrimaryOutput().amount() > 0;
  }
}
