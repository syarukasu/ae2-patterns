package com.syaru.ae2mrp.encoder;

import appeng.core.AEConfig;
import appeng.items.tools.powered.WirelessTerminalItem;
import com.syaru.ae2mrp.platform.EncoderBlockEntity;
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
    if (player == null
        || !player.isShiftKeyDown()
        || !(context.getLevel().getBlockEntity(context.getClickedPos())
            instanceof EncoderBlockEntity be)) return InteractionResult.PASS;
    if (!context.getLevel().isClientSide) {
      WirelessData.bind(
          context.getItemInHand(), GlobalPos.of(context.getLevel().dimension(), be.getBlockPos()));
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
    if (targetLevel == null
        || !targetLevel.hasChunkAt(target.pos())
        || !(targetLevel.getBlockEntity(target.pos()) instanceof EncoderBlockEntity be))
      return fail(player, "terminal_unavailable");
    var grid = getLinkedGrid(stack, player.level(), null);
    if (grid == null || be.terminal.grid() != grid) return fail(player, "wrong_network");
    if (!hasPower(player, .5, stack)) return fail(player, "wireless_unavailable");
    var access = WirelessData.access(this, player, inventorySlot, stack, be);
    if (!access.valid()) return fail(player, "wireless_unavailable");
    return player
        .openMenu(
            new SimpleMenuProvider(
                (id, inventory, owner) -> {
                  be.inventory.readDraft();
                  var menu =
                      new EncoderMenu(
                          id,
                          inventory,
                          be.inventory,
                          ContainerLevelAccess.create(targetLevel, target.pos()),
                          be.terminal);
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

  public boolean stillBound(Player player, int slot, ItemStack stack, EncoderBlockEntity be) {
    var target = WirelessData.target(stack);
    return player.isAlive()
        && !player.isSpectator()
        && player.getInventory().getItem(slot) == stack
        && stack.is(this)
        && target != null
        && target.dimension().equals(be.getLevel().dimension())
        && target.pos().equals(be.getBlockPos())
        && !be.isRemoved()
        && be.getLevel().hasChunkAt(be.getBlockPos())
        && be.getLevel().getBlockEntity(be.getBlockPos()) == be
        && be.terminal.grid() != null
        && getLinkedGrid(stack, player.level(), null) == be.terminal.grid();
  }
}
