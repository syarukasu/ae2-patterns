package com.syaru.ae2mrp.pattern;

import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEKey;
import com.syaru.ae2mrp.core.DistributionMode;
import com.syaru.ae2mrp.core.StrictDistributor;
import com.syaru.ae2mrp.platform.Platform;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A copied, validated draft. No live provider or inventory ownership. */
public final class CompositeDefinition {
  private record InputIdentity(
      List<appeng.api.stacks.GenericStack> alternatives, long multiplier) {}

  private record RecipeIdentity(
      Class<?> kind,
      List<InputIdentity> inputs,
      List<appeng.api.stacks.GenericStack> outputs,
      AEKey nativeDefinition) {}

  public static final int SCHEMA = 1;
  private final List<ItemStack> patterns;
  private final List<IPatternDetails> details;
  private final List<StrictDistributor.Route> routes;
  private final AEKey output;
  private final long revision;
  private final DistributionMode mode;

  public CompositeDefinition(
      List<ItemStack> patterns,
      List<Integer> weights,
      List<Boolean> enabled,
      long revision,
      Level level) {
    this(patterns, weights, enabled, DistributionMode.STRICT, revision, level);
  }

  public CompositeDefinition(
      List<ItemStack> patterns,
      List<Integer> weights,
      List<Boolean> enabled,
      DistributionMode mode,
      long revision,
      Level level) {
    this.mode = java.util.Objects.requireNonNull(mode);
    if (patterns.size() < 2
        || patterns.size() > 8
        || patterns.size() != weights.size()
        || patterns.size() != enabled.size()
        || revision < 0) throw new IllegalArgumentException("ROUTE_COUNT");
    var copies = new ArrayList<ItemStack>();
    var decoded = new ArrayList<IPatternDetails>();
    var routes = new ArrayList<StrictDistributor.Route>();
    var identities = new HashSet<Object>();
    AEKey common = null;
    for (int i = 0; i < patterns.size(); i++) {
      var stack = patterns.get(i).copyWithCount(1);
      var pattern = Platform.decodeSource(stack, level);
      if (!Platform.isSupportedSource(pattern))
        throw new IllegalArgumentException("SOURCE_PATTERN_UNSUPPORTED");
      var inputIdentity = new ArrayList<InputIdentity>();
      for (var input : pattern.getInputs())
        inputIdentity.add(
            new InputIdentity(List.of(input.getPossibleInputs()), input.getMultiplier()));
      if (!identities.add(
          new RecipeIdentity(
              pattern.getClass(),
              List.copyOf(inputIdentity),
              Platform.outputs(pattern),
              pattern.getClass() == appeng.crafting.pattern.AEProcessingPattern.class
                  ? null
                  : pattern.getDefinition())))
        throw new IllegalArgumentException("DUPLICATE_PATTERN");
      var out = pattern.getPrimaryOutput();
      if (common != null && !common.equals(out.what()))
        throw new IllegalArgumentException("OUTPUT_MISMATCH");
      common = out.what();
      copies.add(stack);
      decoded.add(pattern);
      routes.add(
          new StrictDistributor.Route(
              Integer.toString(i),
              weights.get(i),
              enabled.get(i),
              BigInteger.valueOf(out.amount())));
    }
    StrictDistributor.validate(routes);
    this.patterns = List.copyOf(copies);
    this.details = List.copyOf(decoded);
    this.routes = List.copyOf(routes);
    this.output = common;
    this.revision = revision;
  }

  public List<ItemStack> patternCopies() {
    return patterns.stream().map(ItemStack::copy).toList();
  }

  public List<IPatternDetails> details() {
    return details;
  }

  public List<StrictDistributor.Route> routes() {
    return routes;
  }

  public AEKey output() {
    return output;
  }

  public long revision() {
    return revision;
  }

  public DistributionMode mode() {
    return mode;
  }
}
