package com.syaru.ae2mrp.test;

import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.me.helpers.MachineSource;
import appeng.server.testplots.TestPlot;
import appeng.server.testworld.PlotBuilder;
import appeng.server.testworld.PlotTestHelper;
import com.syaru.ae2mrp.core.DistributionMode;
import com.syaru.ae2mrp.encoder.EncoderState;
import com.syaru.ae2mrp.pattern.CompositeDefinition;
import com.syaru.ae2mrp.pattern.ControlPattern;
import com.syaru.ae2mrp.platform.Platform;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.world.item.ItemStack;

public class MrpPlots {
  static final AEItemKey INPUT = AEItemKey.of(AEItems.CERTUS_QUARTZ_CRYSTAL);
  static final AEItemKey OUTPUT = AEItemKey.of(AEItems.CERTUS_QUARTZ_DUST);

  @TestPlot("mrp_wireless_terminal")
  public static void wirelessTerminal(PlotBuilder p) {
    fixture(p, 10, false);
    p.block("1 0 -2", Platform.block());
    p.blockState(
        "1 0 -4",
        AEBlocks.WIRELESS_ACCESS_POINT
            .block()
            .defaultBlockState()
            .setValue(
                net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,
                net.minecraft.core.Direction.EAST));
    p.blockEntity(
        "0 0 -3",
        AEBlocks.PATTERN_PROVIDER,
        provider -> provider.getLogic().getPatternInv().addItems(TestPlatform.pattern(2, 2)));
    p.test(
            h ->
                h.startSequence()
                    .thenIdle(5)
                    .thenWaitUntil(
                        () -> {
                          var be =
                              (com.syaru.ae2mrp.platform.EncoderBlockEntity)
                                  h.getBlockEntity(new BlockPos(1, 0, -2));
                          var wap =
                              (appeng.blockentity.networking.WirelessAccessPointBlockEntity)
                                  h.getBlockEntity(new BlockPos(1, 0, -4));
                          h.check(
                              be.terminal.grid() != null
                                  && wap.isActive()
                                  && wap.getGrid() == be.terminal.grid(),
                              "Wireless fixture offline");
                        })
                    .thenExecute(
                        () -> {
                          var be =
                              (com.syaru.ae2mrp.platform.EncoderBlockEntity)
                                  h.getBlockEntity(new BlockPos(1, 0, -2));
                          var wap =
                              (appeng.blockentity.networking.WirelessAccessPointBlockEntity)
                                  h.getBlockEntity(new BlockPos(1, 0, -4));
                          var player = TestPlatform.wirelessPlayer(h.getLevel());
                          var item = com.syaru.ae2mrp.MrpMod.WIRELESS.get();
                          var stack = new ItemStack(item);
                          player.getInventory().setItem(0, stack);
                          player.setPos(
                              wap.getBlockPos().getX() + 10.5,
                              wap.getBlockPos().getY() + .5,
                              wap.getBlockPos().getZ() + .5);
                          h.check(!item.openRemote(player, 0), "Unbound item opened a terminal");
                          var target =
                              net.minecraft.core.GlobalPos.of(
                                  h.getLevel().dimension(), be.getBlockPos());
                          com.syaru.ae2mrp.platform.WirelessData.bind(stack, target);
                          var handler = appeng.api.features.GridLinkables.get(item);
                          h.check(
                              handler != null && handler.canLink(stack),
                              "Native WAP linking was not registered");
                          handler.link(
                              stack,
                              net.minecraft.core.GlobalPos.of(
                                  h.getLevel().dimension(), wap.getBlockPos()));
                          h.check(
                              !item.openRemote(player, 0), "Empty battery allowed wireless use");
                          item.injectAEPower(stack, 1000, appeng.api.config.Actionable.MODULATE);
                          var restored = TestPlatform.roundTrip(stack, h.getLevel());
                          h.check(
                              target.equals(com.syaru.ae2mrp.platform.WirelessData.target(restored))
                                  && item.getAECurrentPower(restored)
                                      == item.getAECurrentPower(stack)
                                  && item.getLinkedPosition(restored)
                                      .equals(item.getLinkedPosition(stack)),
                              "Save lost target, native link or battery");
                          h.check(
                              TestPlatform.openWireless(item, player),
                              "Native inventory entry did not open composite menu");
                          h.check(
                              player.containerMenu instanceof com.syaru.ae2mrp.encoder.EncoderMenu,
                              "Wireless opened another mod's menu");
                          var menu = (com.syaru.ae2mrp.encoder.EncoderMenu) player.containerMenu;
                          h.check(
                              menu.stillValid(player) && menu.wirelessSlot() == 0,
                              "Valid in-range remote access rejected");
                          h.check(
                              player.distanceToSqr(
                                      be.getBlockPos().getX() + .5,
                                      be.getBlockPos().getY() + .5,
                                      be.getBlockPos().getZ() + .5)
                                  > 64,
                              "Fixture did not leave ordinary block-menu range");
                          menu.clicked(
                              37, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
                          h.check(
                              menu.getCarried().isEmpty()
                                  && player.getInventory().getItem(0) == stack
                                  && menu.quickMoveStack(player, 37).isEmpty(),
                              "Wireless item escaped its locked slot");
                          var picker = menu;
                          picker.broadcastChanges();
                          h.check(
                              picker.isPlayerInventorySlotLocked(0),
                              "Native picker unlocked wireless item");
                          h.check(
                              picker.selectReference(
                                      Platform.keyTag(
                                              AEItemKey.of(TestPlatform.pattern(1, 1)),
                                              h.getLevel())
                                          .toString())
                                  && picker.selectReference(
                                      Platform.keyTag(
                                              AEItemKey.of(TestPlatform.pattern(2, 2)),
                                              h.getLevel())
                                          .toString()),
                              "Remote native picker lost source references");
                          h.check(player.containerMenu == menu, "Remote reference changed menu");
                          be.inventory.setItem(8, new ItemStack(Platform.blank()));
                          menu.clickMenuButton(player, 120);
                          h.check(
                              be.inventory.error.equals("ENCODED"),
                              "Remote encoding failed: " + be.inventory.error);
                          h.check(
                              be.terminal.registered.isEmpty(),
                              "Remote draft was advertised without registration");
                          be.terminal.registered.setItem(0, be.inventory.removeItem(9, 1));
                          h.check(
                              be.terminal.getAvailablePatterns().size() == 1,
                              "Remote registration did not reach backing provider");
                          double power = item.getAECurrentPower(stack);
                          for (int i = 0; i < 22; i++) menu.broadcastChanges();
                          h.check(
                              item.getAECurrentPower(stack) < power,
                              "Native wireless session did not consume power");
                          player.setPos(
                              wap.getBlockPos().getX() + 100,
                              wap.getBlockPos().getY(),
                              wap.getBlockPos().getZ());
                          h.check(
                              !menu.stillValid(player) && !menu.clickMenuButton(player, 120),
                              "Out-of-range operation accepted");
                          h.check(
                              !picker.stillValid(player)
                                  && !picker.selectReference(
                                      Platform.keyTag(
                                              AEItemKey.of(TestPlatform.pattern(1, 1)),
                                              h.getLevel())
                                          .toString()),
                              "Out-of-range native picker remained usable");
                          menu.broadcastChanges();
                          player.setPos(
                              wap.getBlockPos().getX() + 10.5,
                              wap.getBlockPos().getY() + .5,
                              wap.getBlockPos().getZ() + .5);
                          h.check(!menu.stillValid(player), "Closed radio lease revived");
                          h.check(
                              item.openRemote(player, 0),
                              "New wireless session could not be opened");
                          var next = (com.syaru.ae2mrp.encoder.EncoderMenu) player.containerMenu;
                          player.getInventory().setItem(0, stack.copy());
                          h.check(
                              !next.stillValid(player),
                              "Replacing handheld item preserved old lease");
                          player.getInventory().setItem(0, stack);
                          item.extractAEPower(
                              stack,
                              item.getAECurrentPower(stack),
                              appeng.api.config.Actionable.MODULATE);
                          h.check(
                              !item.openRemote(player, 0), "Drained battery opened remote menu");
                          item.injectAEPower(stack, 1000, appeng.api.config.Actionable.MODULATE);
                          handler.unlink(stack);
                          h.check(!item.openRemote(player, 0), "Unlinked item opened remote menu");
                          h.check(
                              be.terminal.registered.getItem(0).is(Platform.encoded()),
                              "Wireless disconnect lost registered control");
                        })
                    .thenSucceed())
        .maxTicks(200);
  }

  @TestPlot("mrp_network_terminal_reference_register")
  public static void networkTerminal(PlotBuilder p) {
    fixture(p, 10, false);
    p.block("1 0 -2", Platform.block());
    p.blockEntity(
        "0 0 -3",
        AEBlocks.PATTERN_PROVIDER,
        provider -> {
          provider.getLogic().getPatternInv().addItems(TestPlatform.pattern(2, 2));
          provider.getLogic().getPatternInv().addItems(TestPlatform.parentPattern());
        });
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.REPORT_MISSING_ITEMS);
              long[] expectedUsed = new long[1];
              h.startSequence()
                  .thenIdle(5)
                  .thenWaitUntil(
                      () -> {
                        var be =
                            (com.syaru.ae2mrp.platform.EncoderBlockEntity)
                                h.getBlockEntity(new BlockPos(1, 0, -2));
                        h.check(
                            be.terminal.grid() == h.getGrid(BlockPos.ZERO),
                            "Wall terminal did not connect through its capability");
                      })
                  .thenExecute(
                      () -> {
                        var be =
                            (com.syaru.ae2mrp.platform.EncoderBlockEntity)
                                h.getBlockEntity(new BlockPos(1, 0, -2));
                        var player = TestPlatform.wirelessPlayer(h.getLevel());
                        player.setPos(
                            be.getBlockPos().getX() + .5,
                            be.getBlockPos().getY() + .5,
                            be.getBlockPos().getZ() + .5);
                        var menu =
                            (com.syaru.ae2mrp.encoder.EncoderMenu)
                                be.createMenu(1, player.getInventory(), player);
                        h.check(
                            menu.stillValid(player), "Real terminal menu rejected nearby player");
                        player.containerMenu = menu;
                        var carried = TestPlatform.pattern(1, 1);
                        menu.setCarried(carried.copy());
                        var picker = menu;
                        h.check(
                            picker instanceof appeng.menu.implementations.PatternAccessTermMenu,
                            "Picker replaced the native PAT menu");
                        picker.broadcastChanges();
                        h.check(
                            !picker.selectReference("{broken") && be.inventory.getItem(0).isEmpty(),
                            "Malformed native reference changed editor state");
                        h.check(
                            picker.getSlots(appeng.menu.SlotSemantics.PLAYER_INVENTORY).size() == 27
                                && picker.stillValid(player),
                            "Native PAT inventory/lifecycle missing");
                        h.check(
                            AEItemKey.of(picker.getCarried()).equals(AEItemKey.of(carried)),
                            "Opening native PAT changed cursor ownership");
                        String first =
                            Platform.keyTag(AEItemKey.of(TestPlatform.pattern(1, 1)), h.getLevel())
                                .toString();
                        String second =
                            Platform.keyTag(AEItemKey.of(TestPlatform.pattern(2, 2)), h.getLevel())
                                .toString();
                        h.check(
                            picker.selectReference(first) && picker.selectReference(second),
                            "Native picker did not reference both mounted sources");
                        h.check(
                            be.inventory.ghost(0)
                                && be.inventory.ghost(1)
                                && !picker.outputKey.isEmpty(),
                            "Native picker selection metadata missing");
                        h.check(!picker.selectReference(first), "Duplicate source accepted");
                        h.check(
                            be.inventory.ghost(0) && be.inventory.physical(0).isEmpty(),
                            "Reference became physical ownership");
                        h.check(
                            AEItemKey.of(picker.getCarried()).equals(AEItemKey.of(carried)),
                            "Reference changed cursor");
                        picker.doAction(
                            player, appeng.helpers.InventoryAction.PICKUP_OR_SET_DOWN, 0, 0);
                        h.check(player.containerMenu == menu, "PAT changed the active menu");
                        player.containerMenu = player.inventoryMenu;
                        h.check(
                            !picker.selectReference(second),
                            "Closed picker still accepted mutations");
                        player.containerMenu = menu;
                        h.check(
                            AEItemKey.of(menu.getCarried()).equals(AEItemKey.of(carried)),
                            "Returning from native PAT changed cursor ownership");
                        menu.setCarried(ItemStack.EMPTY);
                        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.CLONE, player);
                        menu.clicked(0, 0, net.minecraft.world.inventory.ClickType.THROW, player);
                        h.check(
                            menu.getCarried().isEmpty() && be.inventory.ghost(0),
                            "Ghost pattern escaped through slot actions");
                        be.inventory.setItem(8, new ItemStack(Platform.blank()));
                        menu.clickMenuButton(player, 120);
                        h.check(
                            be.inventory.error.equals("ENCODED"),
                            "Network encode failed: " + be.inventory.error);
                        long firstBatch =
                            Platform.read(be.inventory.getItem(9), h.getLevel())
                                .details()
                                .get(0)
                                .getPrimaryOutput()
                                .amount();
                        // Q=5, 50/50 assigns 3 units to route 1, then 2; a two-unit first batch
                        // rounds to 4.
                        expectedUsed[0] = firstBatch == 1 ? 5 : 6;
                        h.check(
                            h.getGrid(BlockPos.ZERO)
                                    .getCraftingService()
                                    .getCraftingFor(OUTPUT)
                                    .size()
                                == 2,
                            "Editing or encoding advertised an unregistered control");
                        menu.quickMoveStack(player, 9);
                        var encoded =
                            player.getInventory().items.stream()
                                .filter(s -> s.is(Platform.encoded()))
                                .findFirst()
                                .orElseThrow();
                        int playerSlot = -1;
                        for (int i = 10; i < 46; i++)
                          if (menu.getSlot(i).getItem() == encoded) {
                            playerSlot = i;
                            break;
                          }
                        h.check(playerSlot >= 0, "Encoded output not returned to inventory");
                        menu.quickMoveStack(player, playerSlot);
                        h.check(
                            be.terminal.registered.getItem(0).is(Platform.encoded()),
                            "Shift-click did not register composite");
                        h.check(
                            !be.terminal.registered.canPlaceItem(
                                1, be.terminal.registered.getItem(0)),
                            "Duplicate output control accepted");
                        h.check(
                            be.terminal.registered.getContainerSize() == 54,
                            "Terminal must hold 54 registered composites");
                        be.terminal.registered.setItem(53, be.terminal.registered.removeItem(0, 1));
                        h.check(
                            !be.terminal.registered.canPlaceItem(
                                0, be.terminal.registered.getItem(53)),
                            "Duplicate output control accepted across registration rows");
                        var restored = TestPlatform.roundTripBlock(be, h.getLevel());
                        h.check(
                            AEItemKey.of(restored.terminal.registered.getItem(53))
                                    .equals(AEItemKey.of(be.terminal.registered.getItem(53)))
                                && restored.terminal.registered.getItem(0).isEmpty(),
                            "Last-row registered control lost or duplicated on save");
                        menu.quickMoveStack(
                            player, com.syaru.ae2mrp.encoder.EncoderMenu.REGISTRY_END - 1);
                        h.check(
                            be.terminal.registered.getItem(53).isEmpty(),
                            "Last-row registered control could not be retrieved");
                        var returned =
                            player.getInventory().items.stream()
                                .filter(s -> s.is(Platform.encoded()))
                                .findFirst()
                                .orElseThrow();
                        be.terminal.registered.setItem(53, returned.copy());
                        returned.setCount(0);
                        var provider =
                            (appeng.blockentity.crafting.PatternProviderBlockEntity)
                                h.getBlockEntity(new BlockPos(0, 0, -3));
                        h.check(
                            provider.getLogic().getPatternInv().getStackInSlot(0).getCount() == 1
                                && provider.getLogic().getPatternInv().getStackInSlot(1).getCount()
                                    == 1,
                            "Original provider patterns moved");
                        player.setPos(
                            be.getBlockPos().getX() + 20,
                            be.getBlockPos().getY(),
                            be.getBlockPos().getZ());
                        h.check(
                            !menu.stillValid(player) && !menu.clickMenuButton(player, 120),
                            "Remote menu mutation accepted");
                      })
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        var plan = order.get();
                        h.check(
                            !plan.simulation()
                                && plan.patternTimes().size() == 2
                                && plan.usedItems().get(INPUT) == expectedUsed[0],
                            "Terminal-registered control did not expand both mounted source"
                                + " patterns");
                        var be =
                            (com.syaru.ae2mrp.platform.EncoderBlockEntity)
                                h.getBlockEntity(new BlockPos(1, 0, -2));
                        be.terminal.registered.removeItem(53, 1);
                        h.check(
                            h
                                .getGrid(BlockPos.ZERO)
                                .getCraftingService()
                                .getCraftingFor(OUTPUT)
                                .stream()
                                .noneMatch(ControlPattern.class::isInstance),
                            "Unregistered control remains in grid");
                      })
                  .thenSucceed();
            })
        .maxTicks(300);
  }

  @TestPlot("mrp_network_terminal_stale_reference")
  public static void staleTerminal(PlotBuilder p) {
    fixture(p, 10, false);
    p.block("1 0 -2", Platform.block());
    p.blockEntity(
        "0 0 -3",
        AEBlocks.PATTERN_PROVIDER,
        provider -> provider.getLogic().getPatternInv().addItems(TestPlatform.pattern(2, 2)));
    p.test(
            h ->
                h.startSequence()
                    .thenIdle(5)
                    .thenWaitUntil(
                        () -> {
                          var be =
                              (com.syaru.ae2mrp.platform.EncoderBlockEntity)
                                  h.getBlockEntity(new BlockPos(1, 0, -2));
                          h.check(be.terminal.grid() != null, "Terminal offline");
                        })
                    .thenExecute(
                        () -> {
                          var be =
                              (com.syaru.ae2mrp.platform.EncoderBlockEntity)
                                  h.getBlockEntity(new BlockPos(1, 0, -2));
                          var player = TestPlatform.player(h.getLevel());
                          player.setPos(
                              be.getBlockPos().getX() + .5,
                              be.getBlockPos().getY() + .5,
                              be.getBlockPos().getZ() + .5);
                          var menu =
                              (com.syaru.ae2mrp.encoder.EncoderMenu)
                                  be.createMenu(2, player.getInventory(), player);
                          menu.reference(player, TestPlatform.pattern(1, 1), be.terminal.grid());
                          menu.reference(player, TestPlatform.pattern(2, 2), be.terminal.grid());
                          h.check(
                              be.inventory.ghost(0) && be.inventory.ghost(1),
                              "References not pinned");
                          var provider =
                              (appeng.blockentity.crafting.PatternProviderBlockEntity)
                                  h.getBlockEntity(new BlockPos(0, 0, -3));
                          provider.getLogic().getPatternInv().setItemDirect(0, ItemStack.EMPTY);
                          provider.getLogic().updatePatterns();
                          be.inventory.setItem(8, new ItemStack(Platform.blank()));
                          menu.clickMenuButton(player, 120);
                          h.check(
                              be.inventory.error.equals("STALE_REFERENCE")
                                  && be.inventory.physical(9).isEmpty()
                                  && be.inventory.physical(8).getCount() == 1,
                              "Removed source encoded or consumed blank");
                          var restored = TestPlatform.roundTripBlock(be, h.getLevel());
                          h.check(
                              restored.inventory.getItem(0).isEmpty()
                                  && restored.inventory.getItem(1).isEmpty(),
                              "Network ghost saved as physical item");
                        })
                    .thenSucceed())
        .maxTicks(200);
  }

  private static void fixture(PlotBuilder p, int stock, boolean composite) {
    fixture(p, stock, composite, DistributionMode.STRICT, false);
  }

  private static void fixture(
      PlotBuilder p, int stock, boolean composite, DistributionMode mode, boolean blocked) {
    fixture(p, stock, composite, mode, blocked, List.of(60, 40));
  }

  private static void fixture(
      PlotBuilder p,
      int stock,
      boolean composite,
      DistributionMode mode,
      boolean blocked,
      List<Integer> weights) {
    p.block("0 0 0", AEBlocks.CRAFTING_STORAGE_64K);
    p.cable("0 0 [-2,-1]");
    p.blockEntity(
        "0 0 -3",
        AEBlocks.PATTERN_PROVIDER,
        provider -> {
          var a = blocked ? TestPlatform.twoInputPattern() : TestPlatform.pattern(1, 1);
          var b = blocked ? TestPlatform.pattern(1, 1) : TestPlatform.pattern(2, 2);
          provider.getLogic().getPatternInv().addItems(a);
          if (composite) {
            provider.getLogic().getPatternInv().addItems(b);
            var encoded = new ItemStack(Platform.encoded());
            Platform.write(
                encoded,
                new CompositeDefinition(
                    List.of(a, b), weights, List.of(true, true), mode, 0, provider.getLevel()),
                provider.getLevel());
            provider.getLogic().getPatternInv().addItems(encoded);
          }
        });
    p.blockEntity(
        "1 0 -3",
        AEBlocks.INSCRIBER,
        i -> i.getConfigManager().putSetting(Settings.AUTO_EXPORT, YesNo.YES));
    p.cable("0 0 -4");
    p.drive(new BlockPos(0, 0, -5)).addItemCell64k().add(AEItems.CERTUS_QUARTZ_CRYSTAL, stock);
    p.creativeEnergyCell("0 -1 -5");
  }

  private static class Order {
    final PlotTestHelper h;
    final CalculationStrategy strategy;
    final long amount;
    final AEItemKey output;
    Future<ICraftingPlan> future;
    Future<ICraftingPlan> reference;

    Order(PlotTestHelper h, CalculationStrategy strategy) {
      this(h, strategy, 5);
    }

    Order(PlotTestHelper h, CalculationStrategy strategy, long amount) {
      this(h, strategy, amount, OUTPUT);
    }

    Order(PlotTestHelper h, CalculationStrategy strategy, long amount, AEItemKey output) {
      this.h = h;
      this.strategy = strategy;
      this.amount = amount;
      this.output = output;
    }

    void calculate() {
      com.syaru.ae2mrp.platform.PlannerConfig.BACKEND.set(
          com.syaru.ae2mrp.core.PlannerBackend.valueOf(
              System.getProperty("ae2mrp.test.planner", "AE2")));
      var grid = h.getGrid(BlockPos.ZERO);
      if (future == null)
        org.slf4j.LoggerFactory.getLogger("ae2mrp.fixture")
            .info(
                "MRP entry controls={} candidates={}",
                ((com.syaru.ae2mrp.pattern.CompositeServiceAccess) grid.getCraftingService())
                    .ae2mrp$hasControls(),
                grid.getCraftingService().getCraftingFor(output).stream()
                    .map(p -> p.getClass().getName())
                    .toList());
      if (future == null) {
        future =
            grid.getCraftingService()
                .beginCraftingCalculation(
                    h.getLevel(),
                    () -> new MachineSource(grid::getPivot),
                    output,
                    amount,
                    strategy);
        if (com.syaru.ae2mrp.platform.PlannerConfig.useAco()) {
          com.syaru.ae2mrp.platform.PlannerConfig.BACKEND.set(
              com.syaru.ae2mrp.core.PlannerBackend.AE2);
          try {
            reference =
                ((com.syaru.ae2mrp.pattern.CompositeServiceAccess) grid.getCraftingService())
                    .ae2mrp$tryNative(
                        h.getLevel(),
                        () -> new MachineSource(grid::getPivot),
                        output,
                        java.math.BigInteger.valueOf(amount),
                        strategy);
          } finally {
            com.syaru.ae2mrp.platform.PlannerConfig.BACKEND.set(
                com.syaru.ae2mrp.core.PlannerBackend.ACO);
          }
        }
      }
      if (!future.isDone()) throw new GameTestAssertException("Plan pending");
      if (reference != null && !reference.isDone())
        throw new GameTestAssertException("Reference pending");
      if (com.syaru.ae2mrp.platform.PlannerConfig.useAco())
        h.check(
            future.getClass().getName().startsWith("com.ae2vm.addon.nativeengine.NativeVm$"),
            "ACO option did not enter the installed native VM: " + future.getClass());
    }

    ICraftingPlan get() {
      try {
        var result = future.get();
        if (reference != null) {
          var expected = reference.get();
          h.check(
              result.multiplePaths() == expected.multiplePaths(),
              "ACO/AE2 multiplePaths differs: "
                  + result.multiplePaths()
                  + " / "
                  + expected.multiplePaths());
          h.check(
              result.finalOutput().equals(expected.finalOutput())
                  && result.simulation() == expected.simulation()
                  && result.multiplePaths() == expected.multiplePaths()
                  && result.patternTimes().equals(expected.patternTimes())
                  && counter(result.usedItems()).equals(counter(expected.usedItems()))
                  && counter(result.emittedItems()).equals(counter(expected.emittedItems()))
                  && counter(result.missingItems()).equals(counter(expected.missingItems())),
              "ACO/AE2 plan accounting differs");
          h.check(
              result.bytes() == expected.bytes(),
              "ACO/AE2 bytes differ: " + result.bytes() + " / " + expected.bytes());
        }
        return result;
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      } catch (ExecutionException e) {
        throw new IllegalStateException(e.getCause());
      }
    }

    private static java.util.Map<appeng.api.stacks.AEKey, Long> counter(
        appeng.api.stacks.KeyCounter counter) {
      var amounts = new java.util.HashMap<appeng.api.stacks.AEKey, Long>();
      for (var entry : counter)
        if (entry.getLongValue() != 0) amounts.put(entry.getKey(), entry.getLongValue());
      return amounts;
    }
  }

  @TestPlot("mrp_controlled_intermediate")
  public static void controlledIntermediate(PlotBuilder p) {
    fixture(p, 10, true);
    p.test(
            h -> {
              var order =
                  new Order(
                      h,
                      CalculationStrategy.REPORT_MISSING_ITEMS,
                      5,
                      AEItemKey.of(AEItems.SILICON));
              var small =
                  new Order(
                      h,
                      CalculationStrategy.REPORT_MISSING_ITEMS,
                      1,
                      AEItemKey.of(AEItems.SILICON));
              h.startSequence()
                  .thenIdle(5)
                  .thenExecute(
                      () -> {
                        var provider =
                            (appeng.blockentity.crafting.PatternProviderBlockEntity)
                                h.getBlockEntity(new BlockPos(0, 0, -3));
                        provider.getLogic().getPatternInv().addItems(TestPlatform.parentPattern());
                      })
                  .thenIdle(3)
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        var plan = order.get();
                        h.check(
                            !plan.simulation()
                                && plan.patternTimes().size() == 3
                                && plan.usedItems().get(INPUT) == 5,
                            "Ancestor aggregation bypassed composite allocation");
                        h.check(
                            plan.patternTimes().entrySet().stream()
                                .anyMatch(
                                    e ->
                                        e.getKey().getPrimaryOutput().what().equals(OUTPUT)
                                            && e.getKey().getPrimaryOutput().amount() == 1
                                            && e.getValue() == 3),
                            "Controlled intermediate lost its 60% share");
                      })
                  .thenWaitUntil(small::calculate)
                  .thenExecute(small::get)
                  .thenSucceed();
            })
        .maxTicks(200);
  }

  @TestPlot("mrp_strict_completion")
  public static void completion(PlotBuilder p) {
    fixture(p, 10, true);
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.REPORT_MISSING_ITEMS);
              h.startSequence()
                  .thenIdle(5)
                  .thenExecute(() -> requireAcoWhenSelected(h))
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        var plan = order.get();
                        h.check(!plan.simulation(), "Unexpected missing");
                        h.check(
                            plan.patternTimes().size() == 2, "Expected two real source patterns");
                        h.check(
                            plan.patternTimes().keySet().stream()
                                .noneMatch(ControlPattern.class::isInstance),
                            "Control escaped into execution");
                        h.check(
                            plan.patternTimes().entrySet().stream()
                                .anyMatch(
                                    e ->
                                        e.getKey().getPrimaryOutput().amount() == 1
                                            && e.getValue() == 3),
                            "Route A must execute 3 times");
                        h.check(
                            plan.patternTimes().entrySet().stream()
                                .anyMatch(
                                    e ->
                                        e.getKey().getPrimaryOutput().amount() == 2
                                            && e.getValue() == 1),
                            "Route B must execute 1 time");
                        h.check(
                            plan.usedItems().get(INPUT) == 5, "Shared stock counted incorrectly");
                        var grid = h.getGrid(BlockPos.ZERO);
                        var result =
                            grid.getCraftingService()
                                .submitJob(
                                    plan, null, null, true, new MachineSource(grid::getPivot));
                        h.check(result.successful(), "CPU refused order: " + result.errorCode());
                      })
                  .thenWaitUntil(
                      () -> {
                        var grid = h.getGrid(BlockPos.ZERO);
                        h.check(
                            grid.getCraftingService().getCpus().stream().noneMatch(c -> c.isBusy()),
                            "CPU busy");
                        var stock = grid.getStorageService().getInventory().getAvailableStacks();
                        h.check(
                            stock.get(OUTPUT) == 5 && stock.get(INPUT) == 5,
                            "Expected five real outputs and five remaining inputs");
                      })
                  .thenSucceed();
            })
        .maxTicks(1200);
  }

  private static void requireAcoWhenSelected(PlotTestHelper h) {
    try {
      Class.forName(
          "com.ae2vm.addon.nativeengine.NativeVm", false, MrpPlots.class.getClassLoader());
      return;
    } catch (ClassNotFoundException absent) {
      var previous = com.syaru.ae2mrp.platform.PlannerConfig.BACKEND.get();
      com.syaru.ae2mrp.platform.PlannerConfig.BACKEND.set(com.syaru.ae2mrp.core.PlannerBackend.ACO);
      try {
        var grid = h.getGrid(BlockPos.ZERO);
        grid.getCraftingService()
            .beginCraftingCalculation(
                h.getLevel(),
                () -> new MachineSource(grid::getPivot),
                OUTPUT,
                5,
                CalculationStrategy.REPORT_MISSING_ITEMS);
        throw new GameTestAssertException("Missing ACO silently used another backend");
      } catch (IllegalStateException expected) {
        h.check(
            "COMPOSITE_ACO_VM_UNAVAILABLE_OR_INCOMPATIBLE".equals(expected.getMessage()),
            "Wrong missing-ACO diagnostic: " + expected);
      } finally {
        com.syaru.ae2mrp.platform.PlannerConfig.BACKEND.set(previous);
      }
    }
  }

  @TestPlot("mrp_shared_stock_missing")
  public static void missing(PlotBuilder p) {
    fixture(p, 4, true);
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.REPORT_MISSING_ITEMS);
              h.startSequence()
                  .thenIdle(5)
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        var plan = order.get();
                        h.check(plan.simulation(), "Reused shared inventory");
                        h.check(
                            plan.missingItems().get(INPUT) == 1,
                            "Expected exactly one missing input");
                      })
                  .thenSucceed();
            })
        .maxTicks(200);
  }

  @TestPlot("mrp_craft_less")
  public static void craftLess(PlotBuilder p) {
    fixture(p, 4, true);
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.CRAFT_LESS);
              h.startSequence()
                  .thenIdle(5)
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        var plan = order.get();
                        h.check(
                            !plan.simulation() && plan.finalOutput().amount() == 4,
                            "CRAFT_LESS must return four");
                        h.check(
                            plan.usedItems().get(INPUT) == 4, "Partial stock accounting changed");
                      })
                  .thenSucceed();
            })
        .maxTicks(200);
  }

  @TestPlot("mrp_vanilla_unchanged")
  public static void vanilla(PlotBuilder p) {
    fixture(p, 10, false);
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.REPORT_MISSING_ITEMS);
              h.startSequence()
                  .thenIdle(5)
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        var plan = order.get();
                        h.check(
                            !plan.simulation()
                                && plan.patternTimes().size() == 1
                                && plan.usedItems().get(INPUT) == 5,
                            "Vanilla accounting changed");
                      })
                  .thenSucceed();
            })
        .maxTicks(200);
  }

  @TestPlot("mrp_encode_reedit")
  public static void encode(PlotBuilder p) {
    fixture(p, 1, false);
    p.block("1 0 -2", Platform.block());
    p.test(
            h -> {
              var be =
                  (com.syaru.ae2mrp.platform.EncoderBlockEntity)
                      h.getBlockEntity(new BlockPos(1, 0, -2));
              var state = be.inventory;
              var a = TestPlatform.pattern(1, 1);
              var b = TestPlatform.pattern(2, 2);
              state.setItem(0, a.copy());
              state.setItem(1, b.copy());
              state.setItem(8, new ItemStack(Platform.blank()));
              state.setWeight(0, 60);
              state.setWeight(1, 40);
              var player = TestPlatform.wirelessPlayer(h.getLevel());
              player.setPos(
                  be.getBlockPos().getX() + .5,
                  be.getBlockPos().getY() + .5,
                  be.getBlockPos().getZ() + .5);
              var menu =
                  (com.syaru.ae2mrp.encoder.EncoderMenu)
                      be.createMenu(1, player.getInventory(), player);
              player.containerMenu = menu;
              h.check(
                  menu.clickMenuButton(player, 122) && state.mode() == DistributionMode.ADAPTIVE,
                  "Server mode selection did not apply");
              h.check(state.encode(), "Encode failed: " + state.error);
              h.check(
                  state.physical(0).getCount() == 1 && state.physical(1).getCount() == 1,
                  "Source was consumed");
              var result = state.removeItem(9, 1);
              h.check(state.getItem(8).isEmpty(), "Blank not consumed");
              var edit = new EncoderState(h::getLevel, () -> {});
              edit.setItem(8, result);
              h.check(edit.ghost(0) && edit.ghost(1), "Saved sources not displayed");
              h.check(edit.mode() == DistributionMode.ADAPTIVE, "Saved mode not restored");
              h.check(
                  edit.removeItem(0, 1).isEmpty() && edit.physical(0).isEmpty(),
                  "Preview duplicated source");
              edit.setWeight(0, 50);
              edit.setWeight(1, 50);
              h.check(edit.encode(), "Re-edit failed");
              h.check(
                  Platform.read(edit.getItem(9), h.getLevel()).revision() == 1,
                  "Revision not incremented");
              h.succeed();
            })
        .maxTicks(100);
  }

  @TestPlot("mrp_unified_native_encoding")
  public static void unifiedNativeEncoding(PlotBuilder p) {
    fixture(p, 10, false);
    p.block("1 0 -2", Platform.block());
    p.blockEntity(
        "0 0 -3",
        AEBlocks.PATTERN_PROVIDER,
        provider -> provider.getLogic().getPatternInv().addItems(TestPlatform.pattern(2, 2)));
    p.test(
            h ->
                h.startSequence()
                    .thenIdle(5)
                    .thenWaitUntil(
                        () -> {
                          var be =
                              (com.syaru.ae2mrp.platform.EncoderBlockEntity)
                                  h.getBlockEntity(new BlockPos(1, 0, -2));
                          h.check(be.terminal.grid() != null, "Unified terminal is offline");
                        })
                    .thenExecute(
                        () -> {
                          var be =
                              (com.syaru.ae2mrp.platform.EncoderBlockEntity)
                                  h.getBlockEntity(new BlockPos(1, 0, -2));
                          var player = TestPlatform.wirelessPlayer(h.getLevel());
                          player.setPos(
                              be.getBlockPos().getX() + .5,
                              be.getBlockPos().getY() + .5,
                              be.getBlockPos().getZ() + .5);
                          var menu =
                              (com.syaru.ae2mrp.encoder.EncoderMenu)
                                  be.createMenu(1, player.getInventory(), player);
                          player.containerMenu = menu;
                          h.check(
                              menu instanceof appeng.menu.implementations.PatternAccessTermMenu,
                              "PAT implementation replaced");
                          h.check(
                              menu.nativeEncoder()
                                  instanceof appeng.menu.me.items.PatternEncodingTermMenu,
                              "Native encoder replaced");
                          h.check(
                              menu.getSlots(appeng.menu.SlotSemantics.PLAYER_INVENTORY).size() == 27
                                  && menu.getSlots(appeng.menu.SlotSemantics.PLAYER_HOTBAR).size()
                                      == 9,
                              "Player slots duplicated");
                          String first =
                              Platform.keyTag(
                                      AEItemKey.of(TestPlatform.pattern(1, 1)), h.getLevel())
                                  .toString();
                          String second =
                              Platform.keyTag(
                                      AEItemKey.of(TestPlatform.pattern(2, 2)), h.getLevel())
                                  .toString();
                          h.check(
                              menu.selectReference(first) && menu.selectReference(second),
                              "In-place PAT selection failed");
                          h.check(
                              player.containerMenu == menu
                                  && be.inventory.ghost(0)
                                  && be.inventory.ghost(1),
                              "PAT changed menus or moved source ownership");
                          be.inventory.setWeight(0, 60);
                          be.inventory.setWeight(1, 40);
                          TestPlatform.clientAction(menu, "setMode", "\"PROCESSING\"");
                          var nativeMenu = menu.nativeEncoder();
                          var input = nativeMenu.getProcessingInputSlots()[0];
                          var output = nativeMenu.getProcessingOutputSlots()[0];
                          h.check(
                              menu.getSlot(input.index) == input
                                  && menu.getSlot(output.index) == output,
                              "Native slots were copied");
                          menu.setFilter(
                              input.index,
                              appeng.api.stacks.GenericStack.wrapInItemStack(
                                  new appeng.api.stacks.GenericStack(INPUT, 3)));
                          menu.setFilter(
                              output.index,
                              appeng.api.stacks.GenericStack.wrapInItemStack(
                                  new appeng.api.stacks.GenericStack(OUTPUT, 2)));
                          be.encoding
                              .getBlankPatternInv()
                              .setItemDirect(0, AEItems.BLANK_PATTERN.stack(2));
                          TestPlatform.clientAction(menu, "encode", null);
                          var encoded = be.encoding.getEncodedPatternInv().getStackInSlot(0);
                          h.check(
                              TestPlatform.nativePatternIO(encoded, h.getLevel()),
                              "Native encode changed inputs or output");
                          h.check(
                              be.encoding.getBlankPatternInv().getStackInSlot(0).getCount() == 1,
                              "Native blank consumed incorrectly");
                          h.check(
                              be.inventory.weight(0) == 60
                                  && be.inventory.weight(1) == 40
                                  && be.inventory.ghost(0),
                              "Ordinary encoding changed composite draft");
                          var restored = TestPlatform.roundTripBlock(be, h.getLevel());
                          h.check(
                              restored.encoding.getMode()
                                      == appeng.parts.encoding.EncodingMode.PROCESSING
                                  && restored
                                          .encoding
                                          .getBlankPatternInv()
                                          .getStackInSlot(0)
                                          .getCount()
                                      == 1
                                  && AEItemKey.of(
                                          restored
                                              .encoding
                                              .getEncodedPatternInv()
                                              .getStackInSlot(0))
                                      .equals(AEItemKey.of(encoded)),
                              "Native encoder persistence failed");
                          be.encoding.getEncodedPatternInv().setItemDirect(0, ItemStack.EMPTY);
                          be.encoding.getEncodedInputInv().clear();
                          be.encoding.getEncodedOutputInv().clear();
                          TestPlatform.clientAction(menu, "setMode", "\"CRAFTING\"");
                          TestPlatform.clientAction(menu, "setSubstitution", "true");
                          var log = AEItemKey.of(net.minecraft.world.item.Items.OAK_LOG);
                          menu.setFilter(
                              nativeMenu.getCraftingGridSlots()[0].index,
                              appeng.api.stacks.GenericStack.wrapInItemStack(
                                  new appeng.api.stacks.GenericStack(log, 1)));
                          TestPlatform.clientAction(menu, "encode", null);
                          var crafted =
                              appeng.api.crafting.PatternDetailsHelper.decodePattern(
                                  be.encoding.getEncodedPatternInv().getStackInSlot(0),
                                  h.getLevel());
                          h.check(
                              crafted instanceof appeng.crafting.pattern.AECraftingPattern pattern
                                  && pattern.canSubstitute()
                                  && pattern
                                      .getPrimaryOutput()
                                      .equals(
                                          new appeng.api.stacks.GenericStack(
                                              AEItemKey.of(
                                                  net.minecraft.world.item.Items.OAK_PLANKS),
                                              4)),
                              "Native crafting recipe or substitution changed");
                          h.check(
                              be.encoding.getBlankPatternInv().getStackInSlot(0).isEmpty(),
                              "Native crafting carrier was not consumed once");
                          player.containerMenu = player.inventoryMenu;
                          TestPlatform.clientAction(menu, "clear", null);
                          h.check(
                              be.encoding
                                  .getEncodedInputInv()
                                  .getStack(0)
                                  .equals(new appeng.api.stacks.GenericStack(log, 1)),
                              "Closed menu still mutated native draft");
                        })
                    .thenSucceed())
        .maxTicks(200);
  }

  @TestPlot("mrp_invalid_configuration")
  public static void invalid(PlotBuilder p) {
    fixture(p, 1, false);
    p.test(
            h -> {
              var state = new EncoderState(h::getLevel, () -> {});
              state.setItem(0, TestPlatform.pattern(1, 1));
              state.setItem(1, TestPlatform.pattern(2, 2));
              state.setItem(8, new ItemStack(Platform.blank()));
              state.setWeight(0, 30);
              state.setWeight(1, 30);
              h.check(!state.encode() && state.error.equals("WEIGHT_TOTAL"), "Bad sum accepted");
              h.check(
                  !state.physical(8).isEmpty() && state.physical(9).isEmpty(),
                  "Failed encode changed carrier");
              state.setItem(1, state.getItem(0).copy());
              state.setWeight(0, 50);
              state.setWeight(1, 50);
              h.check(
                  !state.encode() && state.error.equals("DUPLICATE_PATTERN"), "Duplicate accepted");
              state.restoreMode("UNKNOWN");
              h.check(
                  !state.encode() && state.error.equals("INVALID_DATA"), "Unknown mode accepted");
              h.succeed();
            })
        .maxTicks(100);
  }

  @TestPlot("mrp_codec_roundtrip")
  public static void codec(PlotBuilder p) {
    fixture(p, 1, false);
    p.test(
            h -> {
              var definition =
                  new CompositeDefinition(
                      List.of(TestPlatform.pattern(1, 1), TestPlatform.pattern(2, 2)),
                      List.of(60, 40),
                      List.of(true, true),
                      7,
                      h.getLevel());
              var item = new ItemStack(Platform.encoded());
              Platform.write(item, definition, h.getLevel());
              var restored = TestPlatform.roundTrip(item, h.getLevel());
              var decoded = Platform.read(restored, h.getLevel());
              h.check(
                  decoded.revision() == 7 && decoded.routes().equals(definition.routes()),
                  "Saved data changed");
              h.check(
                  Platform.control(item, h.getLevel())
                      .equals(Platform.control(restored, h.getLevel())),
                  "Reloaded pattern identity changed");
              h.succeed();
            })
        .maxTicks(100);
  }

  @TestPlot("mrp_eight_routes")
  public static void eight(PlotBuilder p) {
    fixture(p, 1, false);
    p.test(
            h -> {
              var state = new EncoderState(h::getLevel, () -> {});
              for (int i = 0; i < 8; i++) {
                state.setItem(i, TestPlatform.pattern(i + 1, i + 1));
                state.setWeight(i, i < 4 ? 13 : 12);
              }
              state.setItem(8, new ItemStack(Platform.blank()));
              h.check(state.encode(), "Eight routes rejected: " + state.error);
              h.check(
                  Platform.read(state.getItem(9), h.getLevel()).routes().size() == 8,
                  "Routes lost");
              h.succeed();
            })
        .maxTicks(100);
  }

  @TestPlot("mrp_rounding_excess_returned")
  public static void excess(PlotBuilder p) {
    fixture(p, 10, true);
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.REPORT_MISSING_ITEMS, 3);
              h.startSequence()
                  .thenIdle(5)
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        var plan = order.get();
                        h.check(
                            plan.usedItems().get(INPUT) == 4,
                            "Batch rounding must use four real inputs");
                        var grid = h.getGrid(BlockPos.ZERO);
                        h.check(
                            grid.getCraftingService()
                                .submitJob(
                                    plan, null, null, true, new MachineSource(grid::getPivot))
                                .successful(),
                            "Rounded order refused");
                      })
                  .thenWaitUntil(
                      () -> {
                        var grid = h.getGrid(BlockPos.ZERO);
                        h.check(
                            grid.getCraftingService().getCpus().stream().noneMatch(c -> c.isBusy()),
                            "CPU busy");
                        var stock = grid.getStorageService().getInventory().getAvailableStacks();
                        h.check(
                            stock.get(OUTPUT) == 4 && stock.get(INPUT) == 6,
                            "Requested three plus one real excess must be returned");
                      })
                  .thenSucceed();
            })
        .maxTicks(1200);
  }

  @TestPlot("mrp_removed_source_fails_closed")
  public static void removed(PlotBuilder p) {
    fixture(p, 10, true);
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.REPORT_MISSING_ITEMS);
              h.startSequence()
                  .thenIdle(5)
                  .thenExecute(
                      () -> {
                        var provider =
                            (appeng.blockentity.crafting.PatternProviderBlockEntity)
                                h.getBlockEntity(new BlockPos(0, 0, -3));
                        provider.getLogic().getPatternInv().setItemDirect(1, ItemStack.EMPTY);
                      })
                  .thenIdle(3)
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        try {
                          order.future.get();
                          h.fail("Missing route silently replaced");
                        } catch (ExecutionException expected) {
                          h.check(
                              expected
                                  .getCause()
                                  .toString()
                                  .contains("SOURCE_PATTERN_NOT_ON_NETWORK"),
                              "Wrong failure: " + expected.getCause());
                        } catch (InterruptedException ex) {
                          Thread.currentThread().interrupt();
                          throw new IllegalStateException(ex);
                        }
                        h.check(
                            h.getGrid(BlockPos.ZERO)
                                    .getStorageService()
                                    .getInventory()
                                    .getAvailableStacks()
                                    .get(INPUT)
                                == 10,
                            "Failed plan mutated real inventory");
                      })
                  .thenSucceed();
            })
        .maxTicks(200);
  }

  @TestPlot("mrp_disabled_source_not_required")
  public static void disabled(PlotBuilder p) {
    fixture(p, 10, true);
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.REPORT_MISSING_ITEMS);
              h.startSequence()
                  .thenIdle(5)
                  .thenExecute(
                      () -> {
                        var provider =
                            (appeng.blockentity.crafting.PatternProviderBlockEntity)
                                h.getBlockEntity(new BlockPos(0, 0, -3));
                        var encoded = new ItemStack(Platform.encoded());
                        Platform.write(
                            encoded,
                            new CompositeDefinition(
                                List.of(
                                    TestPlatform.pattern(1, 1),
                                    TestPlatform.pattern(2, 2),
                                    TestPlatform.pattern(3, 3)),
                                List.of(60, 40, 100),
                                List.of(true, true, false),
                                1,
                                h.getLevel()),
                            h.getLevel());
                        provider.getLogic().getPatternInv().setItemDirect(2, encoded);
                      })
                  .thenIdle(3)
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        h.check(
                            !order.get().simulation() && order.get().patternTimes().size() == 2,
                            "Disabled route was required or executed");
                      })
                  .thenSucceed();
            })
        .maxTicks(200);
  }

  @TestPlot("mrp_encoder_persistence")
  public static void persistence(PlotBuilder p) {
    persistenceFixture(p);
  }

  @TestPlot("mrp_adaptive_preference")
  public static void adaptivePreference(PlotBuilder p) {
    fixture(p, 10, true, DistributionMode.ADAPTIVE, false);
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.REPORT_MISSING_ITEMS);
              h.startSequence()
                  .thenIdle(5)
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        var plan = order.get();
                        h.check(
                            !plan.simulation() && plan.missingItems().isEmpty(),
                            "Feasible mode reported missing");
                        h.check(
                            plan.patternTimes().values().stream().mapToLong(Long::longValue).sum()
                                    == 4
                                && plan.usedItems().get(INPUT) == 5,
                            "Preferred shares changed");
                      })
                  .thenSucceed();
            })
        .maxTicks(200);
  }

  @TestPlot("mrp_adaptive_completion")
  public static void adaptiveCompletion(PlotBuilder p) {
    fixture(p, 4, true, DistributionMode.ADAPTIVE, false, List.of(75, 25));
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.REPORT_MISSING_ITEMS, 4);
              h.startSequence()
                  .thenIdle(5)
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        var plan = order.get();
                        h.check(
                            !plan.simulation()
                                && plan.usedItems().get(INPUT) == 4
                                && plan.missingItems().isEmpty(),
                            "Shortfall was not reassigned");
                        h.check(
                            plan.patternTimes().size() == 1
                                && plan.patternTimes().values().iterator().next() == 4,
                            "Unfulfillable two-item batch leaked into plan");
                        var grid = h.getGrid(BlockPos.ZERO);
                        var submitted =
                            grid.getCraftingService()
                                .submitJob(
                                    plan, null, null, true, new MachineSource(grid::getPivot));
                        h.check(submitted.successful(), "Adaptive CPU submission failed");
                      })
                  .thenWaitUntil(
                      () -> {
                        var grid = h.getGrid(BlockPos.ZERO);
                        h.check(
                            grid.getCraftingService().getCpus().stream().noneMatch(c -> c.isBusy()),
                            "CPU busy");
                        var stock = grid.getStorageService().getInventory().getAvailableStacks();
                        h.check(
                            stock.get(OUTPUT) == 4 && stock.get(INPUT) == 0,
                            "Adaptive physical accounting failed");
                      })
                  .thenSucceed();
            })
        .maxTicks(1200);
  }

  @TestPlot("mrp_adaptive_failed_branch_rollback")
  public static void adaptiveRollback(PlotBuilder p) {
    fixture(p, 6, true, DistributionMode.ADAPTIVE, true);
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.REPORT_MISSING_ITEMS, 6);
              h.startSequence()
                  .thenIdle(5)
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        var plan = order.get();
                        h.check(
                            !plan.simulation()
                                && plan.missingItems().isEmpty()
                                && plan.usedItems().get(INPUT) == 6,
                            "Failed native trial consumed shared stock/Missing");
                        h.check(
                            plan.patternTimes().size() == 1
                                && plan.patternTimes().values().iterator().next() == 6,
                            "Failed route leaked");
                        h.check(
                            h.getGrid(BlockPos.ZERO)
                                    .getStorageService()
                                    .getInventory()
                                    .getAvailableStacks()
                                    .get(INPUT)
                                == 6,
                            "Planning touched real stock");
                      })
                  .thenSucceed();
            })
        .maxTicks(200);
  }

  @TestPlot("mrp_adaptive_missing")
  public static void adaptiveMissing(PlotBuilder p) {
    fixture(p, 2, true, DistributionMode.ADAPTIVE, false);
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.REPORT_MISSING_ITEMS, 5);
              h.startSequence()
                  .thenIdle(5)
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        var plan = order.get();
                        h.check(
                            plan.simulation() && plan.usedItems().get(INPUT) == 2,
                            "Shortage became success");
                        h.check(
                            plan.missingItems().get(INPUT) == 4,
                            "Missing includes discarded probe requirements or misses rounded"
                                + " inputs");
                      })
                  .thenSucceed();
            })
        .maxTicks(200);
  }

  @TestPlot("mrp_adaptive_craft_less")
  public static void adaptiveCraftLess(PlotBuilder p) {
    fixture(p, 3, true, DistributionMode.ADAPTIVE, false);
    p.test(
            h -> {
              var order = new Order(h, CalculationStrategy.CRAFT_LESS);
              h.startSequence()
                  .thenIdle(5)
                  .thenWaitUntil(order::calculate)
                  .thenExecute(
                      () -> {
                        var plan = order.get();
                        h.check(
                            !plan.simulation()
                                && plan.finalOutput().amount() == 3
                                && plan.usedItems().get(INPUT) == 3,
                            "Adaptive CRAFT_LESS accounting changed");
                      })
                  .thenSucceed();
            })
        .maxTicks(200);
  }

  private static void persistenceFixture(PlotBuilder p) {
    p.block("0 0 0", Platform.block());
    p.test(
            h -> {
              var be =
                  (com.syaru.ae2mrp.platform.EncoderBlockEntity) h.getBlockEntity(BlockPos.ZERO);
              be.inventory.setItem(0, TestPlatform.pattern(1, 1));
              be.inventory.setItem(1, TestPlatform.pattern(2, 2));
              be.inventory.setItem(8, new ItemStack(Platform.blank()));
              be.inventory.setWeight(0, 35);
              be.inventory.setWeight(1, 65);
              be.inventory.setMode(DistributionMode.ADAPTIVE);
              var restored = TestPlatform.roundTripBlock(be, h.getLevel());
              h.check(restored.inventory.mode() == DistributionMode.ADAPTIVE, "Block mode lost");
              var block = Platform.block();
              var north = block.defaultBlockState();
              var east = north.rotate(net.minecraft.world.level.block.Rotation.CLOCKWISE_90);
              var northBounds = north.getShape(h.getLevel(), be.getBlockPos()).bounds();
              var eastBounds = east.getShape(h.getLevel(), be.getBlockPos()).bounds();
              h.check(
                  northBounds.getZsize() == 3.0 / 16.0 && eastBounds.getXsize() == 3.0 / 16.0,
                  "Terminal is not thin / rotated");
              h.check(
                  east.getValue(com.syaru.ae2mrp.platform.EncoderBlock.FACING)
                      == net.minecraft.core.Direction.EAST,
                  "Wrong terminal rotation");
              h.check(
                  com.syaru.ae2mrp.MrpMod.TAB
                      .get()
                      .getIconItem()
                      .is(com.syaru.ae2mrp.MrpMod.ENCODER_ITEM.get()),
                  "Dedicated creative tab missing");
              var menu =
                  new com.syaru.ae2mrp.encoder.EncoderMenu(
                      1,
                      TestPlatform.player(h.getLevel()).getInventory(),
                      be.inventory,
                      net.minecraft.world.inventory.ContainerLevelAccess.create(
                          h.getLevel(), be.getBlockPos()));
              h.check(
                  menu.slots.size() == 100 + menu.nativeEncoder().slots.size()
                      && menu.getSlot(7).x == 230
                      && menu.getSlot(7).y == 81
                      && menu.getSlot(8).x == 14
                      && menu.getSlot(9).x == 166
                      && menu.getSlot(46).y == 160
                      && menu.getSlot(54).x == 152
                      && menu.getSlot(99).getContainerSlot() == 53,
                  "Terminal slot geometry changed");
              h.check(
                  restored.inventory.weight(0) == 35 && restored.inventory.weight(1) == 65,
                  "Block weights lost across save/load");
              h.check(
                  restored.inventory.physical(0).getCount() == 1
                      && restored.inventory.physical(8).is(Platform.blank()),
                  "Physical inventory lost");
              h.succeed();
            })
        .maxTicks(100);
  }
}
