package com.syaru.ae2mrp.core;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AdaptiveDistributorTest {
  private StrictDistributor.Route r(String id, int weight) {
    return new StrictDistributor.Route(id, weight, true, BigInteger.ONE);
  }

  @Test
  void preservesPreferredSharesWhenFeasible() {
    var result =
        AdaptiveDistributor.distribute(
            BigInteger.valueOf(100), List.of(r("a", 60), r("b", 40)), (i, t) -> t);
    assertEquals(List.of(BigInteger.valueOf(60), BigInteger.valueOf(40)), result.allocated());
    assertEquals(BigInteger.ZERO, result.missing());
  }

  @Test
  void redistributesShortfallAndKeepsSurvivorsWeighted() {
    var result =
        AdaptiveDistributor.distribute(
            BigInteger.valueOf(100),
            List.of(r("a", 50), r("b", 30), r("c", 20)),
            (i, t) -> i == 0 ? BigInteger.ZERO : t);
    assertEquals(
        List.of(BigInteger.ZERO, BigInteger.valueOf(60), BigInteger.valueOf(40)),
        result.allocated());
    assertEquals(BigInteger.ZERO, result.missing());
  }

  @Test
  void consumesSharedCapacityOnlyOnceAndTerminatesWhenAllAreBlocked() {
    final BigInteger[] stock = {BigInteger.valueOf(40)};
    var calls = new AtomicInteger();
    var result =
        AdaptiveDistributor.distribute(
            BigInteger.valueOf(100),
            List.of(r("a", 60), r("b", 40)),
            (i, t) -> {
              calls.incrementAndGet();
              var accepted = stock[0].min(t);
              stock[0] = stock[0].subtract(accepted);
              return accepted;
            });
    assertEquals(BigInteger.valueOf(60), result.missing());
    assertEquals(BigInteger.ZERO, stock[0]);
    assertTrue(calls.get() <= 6);
  }

  @Test
  void exactWideTargetsDoNotCauseQuantityLinearSearch() {
    var calls = new AtomicInteger();
    var amount = BigInteger.TEN.pow(64);
    var result =
        AdaptiveDistributor.distribute(
            amount,
            List.of(r("a", 60), r("b", 40)),
            (i, t) -> {
              calls.incrementAndGet();
              return i == 0 ? BigInteger.ZERO : t;
            });
    assertEquals(List.of(BigInteger.ZERO, amount), result.allocated());
    assertEquals(BigInteger.ZERO, result.missing());
    assertTrue(calls.get() <= 8);
  }

  @Test
  void disabledRoutesNeverReceiveAnAllocation() {
    var result =
        AdaptiveDistributor.distribute(
            BigInteger.TEN,
            List.of(
                r("a", 60),
                r("b", 40),
                new StrictDistributor.Route("disabled", 100, false, BigInteger.ONE)),
            (i, t) -> {
              assertNotEquals(2, i);
              return t;
            });
    assertEquals(BigInteger.ZERO, result.allocated().get(2));
  }

  @Test
  void cancellationAndInvalidResultsAreNotReinterpretedAsShortage() {
    var routes = List.of(r("a", 60), r("b", 40));
    assertThrows(
        InterruptedException.class,
        () ->
            AdaptiveDistributor.distribute(
                BigInteger.TEN,
                routes,
                (i, t) -> {
                  throw new InterruptedException();
                }));
    assertThrows(
        IllegalStateException.class,
        () ->
            AdaptiveDistributor.distribute(
                BigInteger.TEN, routes, (i, t) -> t.add(BigInteger.ONE)));
    assertThrows(IllegalArgumentException.class, () -> DistributionMode.parse("UNKNOWN"));
  }
}
