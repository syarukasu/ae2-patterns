package com.syaru.ae2mrp.platform;

import com.syaru.ae2mrp.core.PlannerBackend;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class PlannerConfig {
  public static final ModConfigSpec SPEC;
  public static final ModConfigSpec.EnumValue<PlannerBackend> BACKEND;

  static {
    var builder = new ModConfigSpec.Builder();
    BACKEND =
        builder
            .comment("Composite planning backend. ACO requires its supported native VM.")
            .defineEnum("plannerBackend", PlannerBackend.AE2);
    SPEC = builder.build();
  }

  private PlannerConfig() {}

  public static boolean useAco() {
    return BACKEND.get() == PlannerBackend.ACO;
  }
}
