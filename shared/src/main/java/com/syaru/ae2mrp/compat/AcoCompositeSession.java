package com.syaru.ae2mrp.compat;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.syaru.ae2mrp.core.AdaptiveDistributor;
import com.syaru.ae2mrp.core.DistributionMode;
import com.syaru.ae2mrp.core.StrictDistributor;
import com.syaru.ae2mrp.pattern.ControlPattern;
import java.math.BigInteger;
import java.util.*;

/** One captured order. Only detached VM patterns are read during quantity calculation. */
public final class AcoCompositeSession {
  public record Rule(
      DistributionMode mode, List<StrictDistributor.Route> routes, List<AEItemKey> sources) {
    public Rule {
      routes = List.copyOf(routes);
      sources = List.copyOf(sources);
    }

    public static Rule capture(ControlPattern control) {
      var definition = control.definition();
      return new Rule(
          definition.mode(),
          definition.routes(),
          definition.details().stream().map(IPatternDetails::getDefinition).toList());
    }
  }

  private static final ThreadLocal<AcoCompositeSession> ACTIVE = new ThreadLocal<>();
  private final Map<AEKey, Rule> rules;
  private final Map<AEKey, List<IPatternDetails>> candidates;
  private final Set<IPatternDetails> blocked = Collections.newSetFromMap(new IdentityHashMap<>());
  private int expansions;

  public AcoCompositeSession(Map<AEKey, Rule> rules, Map<AEKey, List<IPatternDetails>> candidates) {
    this.rules = Map.copyOf(rules);
    this.candidates = Map.copyOf(candidates);
    // Do not let a compiled aggregate erase a controlled dependency. Unrelated
    // ingredient subgraphs retain ACO's normal aggregate/bytecode optimizations.
    var parents = new HashMap<AEKey, Set<AEKey>>();
    candidates.forEach(
        (output, patterns) -> {
          for (var pattern : patterns)
            for (var slot : pattern.getInputs())
              for (var input : slot.getPossibleInputs())
                parents.computeIfAbsent(input.what(), ignored -> new HashSet<>()).add(output);
        });
    var pending = new ArrayDeque<>(rules.keySet());
    var seen = new HashSet<AEKey>();
    while (!pending.isEmpty()) {
      var key = pending.removeFirst();
      if (!seen.add(key)) continue;
      blocked.addAll(candidates.getOrDefault(key, List.of()));
      pending.addAll(parents.getOrDefault(key, Set.of()));
    }
  }

  public static boolean blockAggregate(IPatternDetails pattern) {
    var session = ACTIVE.get();
    return session != null && session.blocked.contains(pattern);
  }

  public static boolean multiplePaths(Object engine, boolean original) {
    return ACTIVE.get() == null ? original : AcoVmAccess.multiplePaths(engine);
  }

  public static boolean controlledChoices(List<?> choices) {
    var session = ACTIVE.get();
    return session != null
        && !choices.isEmpty()
        && session.rules.containsKey(AcoVmAccess.key(AcoVmAccess.parent(choices.get(0))));
  }

  public Object execute(Object vm, Object code, Object inventory, boolean less) {
    var previous = ACTIVE.get();
    ACTIVE.set(this);
    try {
      return AcoVmAccess.execute(vm, code, inventory, less);
    } finally {
      if (previous == null) ACTIVE.remove();
      else ACTIVE.set(previous);
    }
  }

  public int expansions() {
    return expansions;
  }

  public static boolean expand(Object engine, Object node, BigInteger demand, Object state) {
    var session = ACTIVE.get();
    if (session == null) return false;
    var rule = session.rules.get(AcoVmAccess.key(node));
    if (rule == null) return false;
    session.distribute(engine, node, demand, state, rule);
    session.expansions++;
    return true;
  }

  private IPatternDetails source(AEKey output, Rule rule, int index) {
    var identity = rule.sources().get(index);
    return candidates.getOrDefault(output, List.of()).stream()
        .filter(pattern -> pattern.getDefinition().equals(identity))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("SOURCE_PATTERN_NOT_ON_NETWORK: " + identity));
  }

  private void distribute(Object engine, Object node, BigInteger demand, Object state, Rule rule) {
    var key = AcoVmAccess.key(node);
    var processes = new ArrayList<Object>();
    AcoVmAccess.processes(node, processes);
    if (rule.mode() == DistributionMode.STRICT) {
      strict(engine, node, demand, state, rule, processes);
      return;
    }
    // Match native ADAPTIVE's mounted-source check, including tiny rounded shares.
    for (int i = 0; i < rule.routes().size(); i++)
      if (rule.routes().get(i).enabled() && rule.routes().get(i).weight() > 0) source(key, rule, i);
    var result =
        AdaptiveDistributor.distribute(
            demand,
            rule.routes(),
            (index, target) -> {
              var pattern = source(key, rule, index);
              var accepted =
                  capacity(
                      engine,
                      node,
                      state,
                      pattern,
                      target,
                      rule.routes().get(index).outputPerOperation());
              if (accepted == null) return BigInteger.ZERO;
              AcoVmAccess.commit(accepted.state(), state);
              processes.add(accepted.process());
              return accepted.target();
            });
    var remaining = result.missing();
    remaining = remaining.subtract(AcoVmAccess.extract(state, key, remaining));
    if (remaining.signum() > 0) {
      // A failed real attempt raises the VM's own Missing from the original
      // source. Only final simulation may add hypothetical missing ingredients.
      strict(engine, node, remaining, state, rule, processes);
    }
  }

  private void strict(
      Object engine,
      Object node,
      BigInteger demand,
      Object state,
      Rule rule,
      List<Object> processes) {
    for (var allocation : StrictDistributor.distribute(demand, rule.routes()).allocations()) {
      if (allocation.operations().signum() == 0) continue;
      AcoVmAccess.checkpoint(engine);
      var pattern = source(AcoVmAccess.key(node), rule, allocation.index());
      var process = AcoVmAccess.process(engine, pattern, node);
      processes.add(process);
      AcoVmAccess.run(engine, process, allocation.operations(), state, false);
      extract(state, AcoVmAccess.key(node), allocation.target());
    }
  }

  private record Trial(Object process, Object state, BigInteger target) {}

  private Trial capacity(
      Object engine,
      Object node,
      Object state,
      IPatternDetails pattern,
      BigInteger target,
      BigInteger unit) {
    var maximum = ceilDiv(target, unit);
    var full = trial(engine, node, state, pattern, target, maximum, unit);
    if (full != null) return full;
    Trial best = null;
    var low = BigInteger.ZERO;
    var high = maximum.subtract(BigInteger.ONE);
    while (low.compareTo(high) < 0) {
      AcoVmAccess.checkpoint(engine);
      var middle = low.add(high).add(BigInteger.ONE).shiftRight(1);
      var result = trial(engine, node, state, pattern, target, middle, unit);
      if (result == null) high = middle.subtract(BigInteger.ONE);
      else {
        low = middle;
        best = result;
      }
    }
    return best;
  }

  private Trial trial(
      Object engine,
      Object node,
      Object state,
      IPatternDetails pattern,
      BigInteger target,
      BigInteger operations,
      BigInteger unit) {
    var child = AcoVmAccess.child(engine, state);
    var process = AcoVmAccess.process(engine, pattern, node);
    if (!AcoVmAccess.run(engine, process, operations, child, true)) return null;
    var accepted = target.min(operations.multiply(unit));
    extract(child, AcoVmAccess.key(node), accepted);
    return new Trial(process, child, accepted);
  }

  private static BigInteger ceilDiv(BigInteger value, BigInteger divisor) {
    return value.subtract(BigInteger.ONE).divide(divisor).add(BigInteger.ONE);
  }

  private static void extract(Object state, AEKey key, BigInteger target) {
    if (!AcoVmAccess.extract(state, key, target).equals(target))
      throw new IllegalStateException("COMPOSITE_OUTPUT_ACCOUNTING_MISMATCH");
  }
}
