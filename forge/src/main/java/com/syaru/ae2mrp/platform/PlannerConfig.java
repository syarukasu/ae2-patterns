package com.syaru.ae2mrp.platform;

import com.syaru.ae2mrp.core.PlannerBackend;
import net.minecraftforge.common.ForgeConfigSpec;

public final class PlannerConfig {
  public static final ForgeConfigSpec SPEC;
  public static final ForgeConfigSpec.EnumValue<PlannerBackend> BACKEND;

  static {
    var builder = new ForgeConfigSpec.Builder();
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
