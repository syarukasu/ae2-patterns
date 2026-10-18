package com.syaru.ae2mrp.core;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

class StrictDistributorTest {
  private StrictDistributor.Route r(String id, int w, long output) {
    return new StrictDistributor.Route(id, w, true, BigInteger.valueOf(output));
  }

  @Test
  void apportionsOutputNotOperations() {
    var result =
        StrictDistributor.distribute(
            BigInteger.valueOf(100_000), List.of(r("essence", 60, 16), r("honey", 40, 3)));
    assertEquals(BigInteger.valueOf(3750), result.allocations().get(0).operations());
    assertEquals(BigInteger.valueOf(13334), result.allocations().get(1).operations());
    assertEquals(BigInteger.valueOf(2), result.excess());
  }

  @Test
  void largestRemainderTiesUseRegistrationOrder() {
    var result =
        StrictDistributor.distribute(BigInteger.valueOf(3), List.of(r("a", 50, 1), r("b", 50, 1)));
    assertEquals(BigInteger.TWO, result.allocations().get(0).target());
    assertEquals(BigInteger.ONE, result.allocations().get(1).target());
  }

  @Test
  void exactBeyondLongAtAnyPosition() {
    var amount = BigInteger.TEN.pow(64);
    var result = StrictDistributor.distribute(amount, List.of(r("a", 60, 16), r("b", 40, 7)));
    assertEquals(
        amount,
        result.allocations().stream()
            .map(StrictDistributor.Allocation::target)
            .reduce(BigInteger.ZERO, BigInteger::add));
    assertTrue(result.produced().compareTo(amount) >= 0);
    assertTrue(result.excess().compareTo(BigInteger.valueOf(23)) < 0);
  }

  @Test
  void rejectsInvalidAndDuplicateConfiguration() {
    assertThrows(
        IllegalArgumentException.class,
        () -> StrictDistributor.validate(List.of(r("a", 30, 1), r("b", 30, 1))));
    assertThrows(
        IllegalArgumentException.class,
        () -> StrictDistributor.validate(List.of(r("a", 50, 1), r("a", 50, 1))));
    assertThrows(
        IllegalArgumentException.class,
        () -> StrictDistributor.validate(List.of(r("a", 100, 1), r("b", 0, 1))));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            StrictDistributor.distribute(
                BigInteger.valueOf(-1), List.of(r("a", 50, 1), r("b", 50, 1))));
  }

  @Test
  void disabledRouteHasNoDemand() {
    var result =
        StrictDistributor.distribute(
            BigInteger.valueOf(99),
            List.of(
                r("a", 50, 1),
                r("b", 50, 1),
                new StrictDistributor.Route("c", 100, false, BigInteger.ONE)));
    assertEquals(BigInteger.ZERO, result.allocations().get(2).operations());
  }

  @Test
  void allSmallQuantitiesConserveTargetsAndBoundExcess() {
    for (int q = 0; q < 1000; q++) {
      var result =
          StrictDistributor.distribute(
              BigInteger.valueOf(q), List.of(r("a", 33, 3), r("b", 33, 7), r("c", 34, 11)));
      assertEquals(
          BigInteger.valueOf(q),
          result.allocations().stream()
              .map(StrictDistributor.Allocation::target)
              .reduce(BigInteger.ZERO, BigInteger::add));
      assertTrue(result.excess().signum() >= 0);
      assertTrue(result.excess().compareTo(BigInteger.valueOf(21)) < 0);
    }
  }
}
