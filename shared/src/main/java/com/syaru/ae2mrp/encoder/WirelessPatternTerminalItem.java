package com.syaru.ae2mrp.encoder;

import appeng.core.AEConfig;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.syaru.ae2mrp.platform.EncoderHost;
import com.syaru.ae2mrp.platform.Platform;
import com.syaru.ae2mrp.platform.WirelessData;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class WirelessPatternTerminalItem extends WirelessTerminalItem {
  public WirelessPatternTerminalItem() {
    super(AEConfig.instance().getWirelessTerminalBattery(), new Properties().stacksTo(1));
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    var player = context.getPlayer();
    if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
    EncoderHost be = Platform.terminalAt(context.getLevel(), context.getClickedPos(), -1);
    if (be == null) {
      var cable =
          appeng.api.parts.PartHelper.getPartHost(context.getLevel(), context.getClickedPos());
      if (cable != null
          && cable.selectPartWorld(context.getClickLocation()).part instanceof EncoderHost part)
        be = part;
    }
    if (be == null) return InteractionResult.PASS;
    if (!context.getLevel().isClientSide) {
      WirelessData.bind(
          context.getItemInHand(),
          GlobalPos.of(context.getLevel().dimension(), be.position()),
          be.partSide());
      player.displayClientMessage(Component.translatable("message.ae2mrp.terminal_bound"), true);
    }
    return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    var stack = player.getItemInHand(hand);
    if (level.isClientSide) return InteractionResultHolder.success(stack);
    int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
    return openRemote(player, slot)
        ? InteractionResultHolder.consume(stack)
        : InteractionResultHolder.fail(stack);
  }

  public boolean openRemote(Player player, int inventorySlot) {
    if (inventorySlot < 0 || inventorySlot >= player.getInventory().getContainerSize())
      return false;
    var stack = player.getInventory().getItem(inventorySlot);
    if (!stack.is(this) || !(player.level() instanceof ServerLevel serverLevel)) return false;
    var target = WirelessData.target(stack);
    if (target == null) return fail(player, "terminal_unbound");
    var targetLevel = serverLevel.getServer().getLevel(target.dimension());
    if (targetLevel == null || !targetLevel.hasChunkAt(target.pos()))
      return fail(player, "terminal_unavailable");
    var be = Platform.terminalAt(targetLevel, target.pos(), WirelessData.side(stack));
    if (be == null || !be.present()) return fail(player, "terminal_unavailable");
    var grid = getLinkedGrid(stack, player.level(), null);
    if (grid == null || be.terminal().grid() != grid) return fail(player, "wrong_network");
    if (!hasPower(player, .5, stack)) return fail(player, "wireless_unavailable");
    var access = WirelessData.access(this, player, inventorySlot, stack, be);
    if (!access.valid()) return fail(player, "wireless_unavailable");
    return player
        .openMenu(
            new SimpleMenuProvider(
                (id, inventory, owner) -> {
                  be.editor().readDraft();
                  var menu =
                      new EncoderMenu(
                          id,
                          inventory,
                          be.editor(),
                          ContainerLevelAccess.create(targetLevel, target.pos()),
                          be.terminal(),
                          be);
                  menu.setWirelessAccess(access);
                  return menu;
                },
                Component.translatable("item.ae2mrp.wireless_pattern_terminal")))
        .isPresent();
  }

  private boolean fail(Player player, String reason) {
    player.displayClientMessage(Component.translatable("message.ae2mrp." + reason), true);
    return false;
  }

  public boolean stillBound(Player player, int slot, ItemStack stack, EncoderHost be) {
    var target = WirelessData.target(stack);
    return player.isAlive()
        && !player.isSpectator()
        && player.getInventory().getItem(slot) == stack
        && stack.is(this)
        && target != null
        && target.dimension().equals(be.hostLevel().dimension())
        && target.pos().equals(be.position())
        && WirelessData.side(stack) == be.partSide()
        && be.present()
        && be.terminal().grid() != null
        && getLinkedGrid(stack, player.level(), null) == be.terminal().grid();
  }
}
