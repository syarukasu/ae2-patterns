package com.syaru.ae2mrp.test;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.GenericStack;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class TestPlatform {
  public static boolean nativePatternIO(ItemStack encoded, Level level) {
    if (!(PatternDetailsHelper.decodePattern(encoded, level)
        instanceof appeng.crafting.pattern.AEProcessingPattern details)) return false;
    return details.getSparseInputs().get(0).equals(new GenericStack(MrpPlots.INPUT, 3))
        && details.getPrimaryOutput().equals(new GenericStack(MrpPlots.OUTPUT, 2))
        && details.getSparseInputs().stream().filter(java.util.Objects::nonNull).count() == 1
        && details.getOutputs().size() == 1;
  }

  public static void clientAction(
      com.syaru.ae2mrp.encoder.EncoderMenu menu, String name, String json) {
    menu.receiveClientAction(name, json);
  }

  public static PlayerForWireless wirelessPlayer(Level level) {
    return new PlayerForWireless((net.minecraft.server.level.ServerLevel) level);
  }

  public static boolean openWireless(
      com.syaru.ae2mrp.platform.WirelessItem item,
      net.minecraft.world.entity.player.Player player) {
    return item.openFromInventory(player, appeng.menu.locator.MenuLocators.forInventorySlot(0));
  }

  public static final class PlayerForWireless
      extends net.neoforged.neoforge.common.util.FakePlayer {
    PlayerForWireless(net.minecraft.server.level.ServerLevel level) {
      super(
          level,
          new com.mojang.authlib.GameProfile(
              java.util.UUID.fromString("07d377e8-f56a-4986-985a-0537c6abc3f6"), "MRPWireless"));
    }

    @Override
    public java.util.OptionalInt openMenu(net.minecraft.world.MenuProvider provider) {
      containerMenu = provider.createMenu(1, getInventory(), this);
      return java.util.OptionalInt.of(1);
    }
  }

  public static net.minecraft.world.entity.player.Player player(Level level) {
    return net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(
        (net.minecraft.server.level.ServerLevel) level);
  }

  public static com.syaru.ae2mrp.platform.EncoderBlockEntity roundTripBlock(
      com.syaru.ae2mrp.platform.EncoderBlockEntity be, Level level) {
    var restored =
        new com.syaru.ae2mrp.platform.EncoderBlockEntity(be.getBlockPos(), be.getBlockState());
    restored.setLevel(level);
    restored.loadWithComponents(
        be.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
    return restored;
  }

  public static ItemStack pattern(long input, long output) {
    return PatternDetailsHelper.encodeProcessingPattern(
        List.of(new GenericStack(MrpPlots.INPUT, input)),
        List.of(new GenericStack(MrpPlots.OUTPUT, output)));
  }

  public static ItemStack twoInputPattern() {
    return PatternDetailsHelper.encodeProcessingPattern(
        List.of(
            new GenericStack(MrpPlots.INPUT, 1),
            new GenericStack(
                appeng.api.stacks.AEItemKey.of(appeng.core.definitions.AEItems.FLUIX_CRYSTAL), 1)),
        List.of(new GenericStack(MrpPlots.OUTPUT, 1)));
  }

  public static ItemStack parentPattern() {
    return PatternDetailsHelper.encodeProcessingPattern(
        List.of(new GenericStack(MrpPlots.OUTPUT, 1)),
        List.of(
            new GenericStack(
                appeng.api.stacks.AEItemKey.of(appeng.core.definitions.AEItems.SILICON), 1)));
  }

  public static ItemStack roundTrip(ItemStack stack, Level level) {
    return ItemStack.parseOptional(
        level.registryAccess(), (CompoundTag) stack.save(level.registryAccess()));
  }
}
