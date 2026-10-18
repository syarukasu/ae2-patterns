package com.syaru.ae2mrp.test;

import appeng.server.testplots.TestPlots;
import appeng.server.testworld.GameTestPlotAdapter;
import java.io.File;
import java.util.List;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("ae2mrp_test")
public final class TestMod {
  public TestMod() throws Exception {
    GlobalTestReporter.replaceWith(
        new JUnitLikeTestReporter(new File(System.getProperty("ae2mrp.report"))));
    TestPlots.addPlotClass(MrpPlots.class);
    FMLJavaModLoadingContext.get()
        .getModEventBus()
        .addListener((RegisterGameTestsEvent e) -> e.register(Tests.class));
  }

  public static class Tests {
    @GameTestGenerator
    public List<TestFunction> tests() {
      return new GameTestPlotAdapter()
          .gameTestAdapter().stream().filter(t -> t.getTestName().startsWith("ae2.mrp_")).toList();
    }
  }
}
