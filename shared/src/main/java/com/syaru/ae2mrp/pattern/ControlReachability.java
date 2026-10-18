package com.syaru.ae2mrp.pattern;

import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEKey;
import java.util.ArrayDeque;
import java.util.HashSet;

/** Called only on grids with a currently mounted control document. */
public final class ControlReachability {
  private ControlReachability() {}

  public static java.util.Map<AEKey, ControlPattern> collect(ICraftingService service, AEKey root) {
    var controls = new java.util.LinkedHashMap<AEKey, ControlPattern>();
    var queue = new ArrayDeque<AEKey>();
    var visited = new HashSet<AEKey>();
    queue.add(root);
    while (!queue.isEmpty()) {
      var key = queue.removeFirst();
      if (!visited.add(key)) continue;
      for (var pattern : service.getCraftingFor(key)) {
        if (pattern instanceof ControlPattern control) {
          var previous = controls.putIfAbsent(key, control);
          if (previous != null && !previous.equals(control))
            throw new IllegalStateException("AMBIGUOUS_COMPOSITE_PATTERN");
        }
        for (var input : pattern.getInputs())
          for (var candidate : input.getPossibleInputs()) queue.addLast(candidate.what());
      }
    }
    return java.util.Collections.unmodifiableMap(controls);
  }
}
