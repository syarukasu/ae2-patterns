package com.syaru.ae2mrp.test;

import appeng.server.testworld.GameTestPlotAdapter;
import java.io.File;
import java.util.List;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.TestFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod("ae2mrp_test")
public final class TestMod {
  public TestMod(IEventBus bus) throws Exception {
    GlobalTestReporter.replaceWith(
        new JUnitLikeTestReporter(new File(System.getProperty("ae2mrp.report"))));
    bus.addListener((RegisterGameTestsEvent e) -> e.register(Tests.class));
  }

  public static class Tests {
    @GameTestGenerator
    public List<TestFunction> tests() {
      return new GameTestPlotAdapter()
          .gameTestAdapter().stream().filter(t -> t.testName().startsWith("ae2.mrp_")).toList();
    }
  }
}
