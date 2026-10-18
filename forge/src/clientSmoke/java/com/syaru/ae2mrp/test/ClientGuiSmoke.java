package com.syaru.ae2mrp.test;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.widgets.AECheckbox;
import appeng.core.definitions.AEBlocks;
import appeng.parts.encoding.EncodingMode;
import com.syaru.ae2mrp.client.EncoderScreen;
import com.syaru.ae2mrp.core.DistributionMode;
import com.syaru.ae2mrp.encoder.EncoderMenu;
import com.syaru.ae2mrp.mixin.EncodingScreenAccess;
import com.syaru.ae2mrp.mixin.EncodingWidgetsAccess;
import com.syaru.ae2mrp.platform.EncoderBlockEntity;
import com.syaru.ae2mrp.platform.Platform;
import java.lang.reflect.Field;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

/** Runs only in the isolated -PguiSmoke development client; never included in the mod JAR. */
@Mod("ae2mrp_gui_smoke")
public final class ClientGuiSmoke {
  private final long deadline = System.nanoTime() + 240_000_000_000L;
  private int phase, ticks;
  private BlockPos terminal;
  private volatile RuntimeException serverFailure;
  private boolean finished;
  private boolean worldRequested;

  public ClientGuiSmoke() {
    MinecraftForge.EVENT_BUS.addListener(this::tick);
  }

  private void tick(TickEvent.ClientTickEvent event) {
    if (event.phase != TickEvent.Phase.END || finished) return;
    var mc = Minecraft.getInstance();
    try {
      if (serverFailure != null) throw serverFailure;
      if (System.nanoTime() > deadline)
        throw new IllegalStateException("GUI smoke timed out at phase " + phase);
      if (mc.screen
          instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen intro) {
        intro.onClose();
        return;
      }
      if (!worldRequested && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
        worldRequested = true;
        new net.minecraft.client.gui.screens.worldselection.WorldOpenFlows(mc, mc.getLevelSource())
            .loadLevel(mc.screen, "gui-smoke");
        return;
      }
      if (mc.level == null || mc.player == null || mc.getSingleplayerServer() == null) return;
      ticks++;
      if (phase == 0) {
        mc.options.guiScale().set(2);
        mc.options.pauseOnLostFocus = false;
        mc.resizeDisplay();
        terminal = new BlockPos(20, 80, 22);
        onServer(
            () -> {
              var player =
                  mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
              var level = player.serverLevel();
              player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
              player.getAbilities().flying = true;
              player.onUpdateAbilities();
              player.teleportTo(20.5, 80, 20.5);
              level.setBlockAndUpdate(
                  terminal.below(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
              level.setBlockAndUpdate(terminal, Platform.block().defaultBlockState());
              level.setBlockAndUpdate(
                  terminal.south(),
                  net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
              level.setBlockAndUpdate(
                  terminal.east(), AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());
              var provider =
                  (appeng.blockentity.crafting.PatternProviderBlockEntity)
                      level.getBlockEntity(terminal.east());
              provider.getLogic().getPatternInv().addItems(processing(Items.COBBLESTONE));
              provider.getLogic().getPatternInv().addItems(processing(Items.GRAVEL));
              var be = (EncoderBlockEntity) level.getBlockEntity(terminal);
              be.inventory.setItem(0, processing(Items.COBBLESTONE));
              be.inventory.setItem(1, processing(Items.GRAVEL));
              be.inventory.setItem(8, new ItemStack(Platform.blank()));
            });
        next();
        return;
      }
      if (ticks < 30) return;
      if (phase == 1 || phase == 11) {
        onServer(
            () -> {
              var player =
                  mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
              System.out.println(
                  "MRP_GUI_SMOKE open at " + player.position() + " target=" + terminal);
              player.openMenu((EncoderBlockEntity) player.serverLevel().getBlockEntity(terminal));
              require(
                  player.containerMenu instanceof EncoderMenu
                      && player.containerMenu.stillValid(player),
                  "Server did not retain a valid terminal menu");
            });
        next();
        return;
      }
      if (phase == 10) {
        require(mc.screen == null, "Terminal did not close");
        capture("terminal-exterior");
        next();
        return;
      }
      require(mc.screen instanceof EncoderScreen, "Expected terminal screen, got " + mc.screen);
      var screen = (EncoderScreen) mc.screen;
      var menu = screen.getMenu();
      System.out.println(
          "MRP_GUI_SMOKE phase="
              + phase
              + " scale="
              + mc.getWindow().getGuiScale()
              + " uiScale="
              + field(screen, "uiScale")
              + " screen="
              + screen.width
              + "x"
              + screen.height);
      var adapter = (AEBaseScreen<?>) field(screen, "encoding");
      var panels = (EncodingScreenAccess) adapter;
      var parentAreas = ((EncodingWidgetsAccess) field(screen, "widgets")).ae2mrp$tooltips();
      var nativeAreas = ((EncodingWidgetsAccess) field(adapter, "widgets")).ae2mrp$tooltips();
      require(
          parentAreas.get("processing-primary-output") != null
              && parentAreas.get("processing-primary-output")
                  == nativeAreas.get("processing-primary-output"),
          "Native tooltip areas do not belong to the visible screen");
      long visible =
          menu.slots.subList(EncoderMenu.REGISTRY_START, EncoderMenu.REGISTRY_END).stream()
              .filter(slot -> slot.x >= 0 && slot.x < 195 && slot.y >= 0)
              .count();
      require(visible == 9, "Expected exactly nine visible registration slots, got " + visible);
      switch (phase) {
        case 2 -> {
          require(
              menu.slots.stream()
                      .filter(
                          slot ->
                              slot instanceof appeng.client.gui.me.patternaccess.PatternSlot
                                  && slot.hasItem())
                      .count()
                  == 2,
              "Native PAT did not display both mounted provider patterns");
          var state = Platform.block().defaultBlockState();
          var model = mc.getBlockRenderer().getBlockModel(state);
          var quads = model.getQuads(state, null, net.minecraft.util.RandomSource.create());
          require(quads.size() >= 14, "Native terminal frame/display model did not bake");
          float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
          for (var quad : quads) {
            var data = quad.getVertices();
            for (int i = 2; i < data.length; i += 8) {
              float z = Float.intBitsToFloat(data[i]);
              min = Math.min(min, z);
              max = Math.max(max, z);
            }
            require(
                !quad.getSprite().contents().name().getPath().equals("missingno"),
                "Native terminal texture missing");
          }
          require(
              Math.abs(min - 13f / 16) < 0.0001f && Math.abs(max - 1) < 0.0001f,
              "Native terminal model is not aligned to wall bounds: " + min + ".." + max);
          capture("crafting");
          click(screen, panels.ae2mrp$tabs().get(EncodingMode.PROCESSING));
        }
        case 3 -> {
          require(
              menu.nativeEncoder().getMode() == EncodingMode.PROCESSING, "Processing tab failed");
          capture("processing");
          click(screen, panels.ae2mrp$tabs().get(EncodingMode.SMITHING_TABLE));
        }
        case 4 -> {
          require(
              menu.nativeEncoder().getMode() == EncodingMode.SMITHING_TABLE, "Smithing tab failed");
          capture("smithing");
          click(screen, panels.ae2mrp$tabs().get(EncodingMode.STONECUTTING));
        }
        case 5 -> {
          require(
              menu.nativeEncoder().getMode() == EncodingMode.STONECUTTING,
              "Stonecutting tab failed");
          capture("stonecutting");
          for (int i = 0; i < 5; i++) click(screen, (Button) field(screen, "nextRow"));
        }
        case 6 -> {
          require(
              (int) field(screen, "registrationRow") == 5, "Last registration row not selected");
          capture("registration-last-row");
          click(screen, (AECheckbox) field(screen, "redistribute"));
        }
        case 7 -> {
          require(
              menu.mode() == DistributionMode.ADAPTIVE, "Adaptive checkbox did not synchronize");
          mc.options.guiScale().set(3);
          mc.resizeDisplay();
        }
        case 8 -> {
          require((int) field(screen, "registrationRow") == 5, "Resize lost registration row");
          require(menu.mode() == DistributionMode.ADAPTIVE, "Resize lost shortage mode");
          capture("scaled");
          click(screen, panels.ae2mrp$tabs().get(EncodingMode.CRAFTING));
        }
        case 9 -> {
          require(
              menu.nativeEncoder().getMode() == EncodingMode.CRAFTING,
              "Crafting tab did not restore");
          capture("crafting-restored");
          mc.player.closeContainer();
          mc.player.setYRot(0);
          mc.player.setXRot(26);
        }
        case 12 -> {
          capture("reopened");
          finish(
              "PASS: native screen open, four modes, shared tooltip areas, nine visible slots, six"
                  + " registration rows, mouse clicks, mounted provider patterns, native model"
                  + " bounds/textures, checkbox sync, resize, close and reopen");
          return;
        }
        default -> throw new IllegalStateException("Unexpected phase " + phase);
      }
      next();
    } catch (Exception | AssertionError | LinkageError error) {
      error.printStackTrace();
      finish("FAIL at phase " + phase + ": " + error);
    }
  }

  private static ItemStack processing(net.minecraft.world.item.Item input) {
    return PatternDetailsHelper.encodeProcessingPattern(
        new GenericStack[] {new GenericStack(AEItemKey.of(input), 1)},
        new GenericStack[] {new GenericStack(AEItemKey.of(Items.STONE), 1)});
  }

  private void onServer(Runnable task) {
    Minecraft.getInstance()
        .getSingleplayerServer()
        .execute(
            () -> {
              try {
                task.run();
              } catch (RuntimeException error) {
                serverFailure = error;
              }
            });
  }

  private void next() {
    phase++;
    ticks = 0;
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
  }

  private static void click(
      EncoderScreen screen, net.minecraft.client.gui.components.AbstractWidget widget)
      throws ReflectiveOperationException {
    double scale = (double) field(screen, "uiScale");
    double x = (widget.getX() + widget.getWidth() / 2.0) * scale;
    double y = (widget.getY() + widget.getHeight() / 2.0) * scale;
    require(
        screen.mouseClicked(x, y, 0),
        "Mouse click not handled: " + widget.getMessage().getString());
    screen.mouseReleased(x, y, 0);
  }

  private static Object field(Object target, String name) throws ReflectiveOperationException {
    Class<?> type = target.getClass();
    while (type != null) {
      try {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
      } catch (NoSuchFieldException absent) {
        type = type.getSuperclass();
      }
    }
    throw new NoSuchFieldException(name);
  }

  private static void capture(String name) throws java.io.IOException {
    var mc = Minecraft.getInstance();
    var directory = mc.gameDirectory.toPath().resolve("screenshots");
    Files.createDirectories(directory);
    try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
      image.writeToFile(directory.resolve(name + ".png"));
    }
    System.out.println("MRP_GUI_SMOKE captured " + name);
  }

  private void finish(String result) {
    finished = true;
    var mc = Minecraft.getInstance();
    try {
      Files.writeString(mc.gameDirectory.toPath().resolve("gui-smoke-result.txt"), result);
    } catch (java.io.IOException error) {
      throw new IllegalStateException("Cannot save GUI smoke report", error);
    }
    System.out.println("MRP_GUI_SMOKE " + result);
    mc.stop();
  }
}
