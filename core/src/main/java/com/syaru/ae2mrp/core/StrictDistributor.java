package com.syaru.ae2mrp.core;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Output-unit apportionment, followed by per-route integer batch rounding. */
public final class StrictDistributor {
  public static final int MAX_ROUTES = 8;

  public record Route(String identity, int weight, boolean enabled, BigInteger outputPerOperation) {
    public Route {
      Objects.requireNonNull(identity);
      Objects.requireNonNull(outputPerOperation);
      if (identity.isBlank() || weight < 0 || weight > 100 || outputPerOperation.signum() <= 0)
        throw new IllegalArgumentException("INVALID_ROUTE");
    }
  }

  public record Allocation(
      int index, BigInteger target, BigInteger operations, BigInteger produced) {}

  public record Result(List<Allocation> allocations, BigInteger requested, BigInteger produced) {
    public Result {
      allocations = List.copyOf(allocations);
    }

    public BigInteger excess() {
      return produced.subtract(requested);
    }
  }

  private StrictDistributor() {}

  public static void validate(List<Route> routes) {
    if (routes.size() < 2 || routes.size() > MAX_ROUTES)
      throw new IllegalArgumentException("ROUTE_COUNT");
    var identities = new HashSet<String>();
    int total = 0, positive = 0;
    for (var route : routes) {
      if (!identities.add(route.identity()))
        throw new IllegalArgumentException("DUPLICATE_PATTERN");
      if (route.enabled()) {
        total += route.weight();
        if (route.weight() > 0) positive++;
      }
    }
    if (positive < 2) throw new IllegalArgumentException("POSITIVE_ROUTE_COUNT");
    if (total != 100) throw new IllegalArgumentException("WEIGHT_TOTAL");
  }

  public static Result distribute(BigInteger requested, List<Route> routes) {
    validate(routes);
    if (requested.signum() < 0) throw new IllegalArgumentException("NEGATIVE_REQUEST");
    var hundred = BigInteger.valueOf(100);
    var targets = new BigInteger[routes.size()];
    var remainders = new BigInteger[routes.size()];
    var order = new ArrayList<Integer>();
    BigInteger assigned = BigInteger.ZERO;
    for (int i = 0; i < routes.size(); i++) {
      var route = routes.get(i);
      var qr =
          requested
              .multiply(BigInteger.valueOf(route.enabled() ? route.weight() : 0))
              .divideAndRemainder(hundred);
      targets[i] = qr[0];
      remainders[i] = qr[1];
      assigned = assigned.add(qr[0]);
      if (route.enabled() && route.weight() > 0) order.add(i);
    }
    order.sort(
        Comparator.<Integer, BigInteger>comparing(i -> remainders[i])
            .reversed()
            .thenComparingInt(Integer::intValue));
    int extra = requested.subtract(assigned).intValueExact();
    for (int n = 0; n < extra; n++)
      targets[order.get(n)] = targets[order.get(n)].add(BigInteger.ONE);
    var allocations = new ArrayList<Allocation>();
    var produced = BigInteger.ZERO;
    for (int i = 0; i < routes.size(); i++) {
      var unit = routes.get(i).outputPerOperation();
      var qr = targets[i].divideAndRemainder(unit);
      var operations = qr[0].add(qr[1].signum() == 0 ? BigInteger.ZERO : BigInteger.ONE);
      var output = operations.multiply(unit);
      allocations.add(new Allocation(i, targets[i], operations, output));
      produced = produced.add(output);
    }
    return new Result(allocations, requested, produced);
  }
}
