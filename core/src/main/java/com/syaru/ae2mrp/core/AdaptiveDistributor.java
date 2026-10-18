package com.syaru.ae2mrp.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Preferred shares first, then weighted water-filling on the same evolving inventory. */
public final class AdaptiveDistributor {
  @FunctionalInterface
  public interface Allocator<E extends Exception> {
    BigInteger allocate(int route, BigInteger target) throws E;
  }

  public record Result(List<BigInteger> allocated, BigInteger missing) {
    public Result {
      allocated = List.copyOf(allocated);
    }
  }

  private AdaptiveDistributor() {}

  public static <E extends Exception> Result distribute(
      BigInteger requested, List<StrictDistributor.Route> routes, Allocator<E> allocator) throws E {
    var preferred = StrictDistributor.distribute(requested, routes);
    var assigned = new BigInteger[routes.size()];
    Arrays.fill(assigned, BigInteger.ZERO);
    var available = new boolean[routes.size()];
    var remaining = requested;
    for (var allocation : preferred.allocations()) {
      int i = allocation.index();
      available[i] = routes.get(i).enabled() && routes.get(i).weight() > 0;
      var accepted = allocate(allocator, i, allocation.target());
      assigned[i] = accepted;
      remaining = remaining.subtract(accepted);
    }
    // Each incomplete round removes a route; a complete round finishes the demand.
    while (remaining.signum() > 0) {
      var shares = shares(remaining, routes, available);
      if (shares == null) break;
      for (int i = 0; i < shares.length; i++) {
        if (shares[i].signum() == 0) continue;
        var accepted = allocate(allocator, i, shares[i]);
        assigned[i] = assigned[i].add(accepted);
        remaining = remaining.subtract(accepted);
        if (accepted.compareTo(shares[i]) < 0) available[i] = false;
      }
    }
    return new Result(List.of(assigned), remaining);
  }

  private static <E extends Exception> BigInteger allocate(
      Allocator<E> allocator, int route, BigInteger target) throws E {
    if (target.signum() == 0) return BigInteger.ZERO;
    var accepted = allocator.allocate(route, target);
    if (accepted == null || accepted.signum() < 0 || accepted.compareTo(target) > 0)
      throw new IllegalStateException("ADAPTIVE_ALLOCATION_OUT_OF_RANGE");
    return accepted;
  }

  private static BigInteger[] shares(
      BigInteger amount, List<StrictDistributor.Route> routes, boolean[] available) {
    int totalWeight = 0;
    for (int i = 0; i < routes.size(); i++) if (available[i]) totalWeight += routes.get(i).weight();
    if (totalWeight == 0) return null;
    var denominator = BigInteger.valueOf(totalWeight);
    var result = new BigInteger[routes.size()];
    var remainder = new BigInteger[routes.size()];
    var order = new ArrayList<Integer>();
    var assigned = BigInteger.ZERO;
    for (int i = 0; i < routes.size(); i++) {
      var qr =
          amount
              .multiply(BigInteger.valueOf(available[i] ? routes.get(i).weight() : 0))
              .divideAndRemainder(denominator);
      result[i] = qr[0];
      remainder[i] = qr[1];
      assigned = assigned.add(qr[0]);
      if (available[i]) order.add(i);
    }
    order.sort(
        Comparator.<Integer, BigInteger>comparing(i -> remainder[i])
            .reversed()
            .thenComparingInt(Integer::intValue));
    int extra = amount.subtract(assigned).intValueExact();
    for (int i = 0; i < extra; i++) result[order.get(i)] = result[order.get(i)].add(BigInteger.ONE);
    return result;
  }
}
