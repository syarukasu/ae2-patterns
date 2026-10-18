package com.syaru.ae2mrp.core;

public enum DistributionMode {
  STRICT,
  ADAPTIVE;

  public static DistributionMode parse(String name) {
    for (var mode : values()) if (mode.name().equals(name)) return mode;
    throw new IllegalArgumentException("INVALID_DATA");
  }
}
