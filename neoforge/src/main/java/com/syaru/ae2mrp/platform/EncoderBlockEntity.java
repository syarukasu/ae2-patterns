package com.syaru.ae2mrp.platform;

import com.syaru.ae2mrp.MrpMod;
import com.syaru.ae2mrp.encoder.EncoderMenu;
import com.syaru.ae2mrp.encoder.EncoderState;
import com.syaru.ae2mrp.encoder.TerminalLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class EncoderBlockEntity extends BlockEntity
    implements MenuProvider,
        appeng.api.storage.IPatternAccessTermMenuHost,
        appeng.helpers.IPatternTerminalMenuHost,
        appeng.api.networking.security.IActionHost {
  private final appeng.util.ConfigManager patternAccessConfig =
      new appeng.util.ConfigManager(this::setChanged);

  {
    patternAccessConfig.registerSetting(
        appeng.api.config.Settings.TERMINAL_SHOW_PATTERN_PROVIDERS,
        appeng.api.config.ShowPatternProviders.VISIBLE);
  }

  @Override
  public appeng.api.util.IConfigManager getConfigManager() {
    return patternAccessConfig;
  }

  @Override
  public appeng.api.networking.IGridNode getActionableNode() {
    return terminal.getActionableNode();
  }

  @Override
  public appeng.api.networking.IGridNode getGridNode() {
    return terminal.getActionableNode();
  }

  @Override
  public appeng.api.storage.ILinkStatus getLinkStatus() {
    return appeng.api.storage.ILinkStatus.ofManagedNode(terminal.node());
  }

  public final EncoderState inventory = new EncoderState(this::getLevel, this::setChanged);
  public final TerminalLogic terminal = new TerminalLogic(this::getLevel, this::setChanged);
  // Keep AE2's logic-host contract separate from Minecraft's BlockEntity method names.
  public final appeng.parts.encoding.PatternEncodingLogic encoding =
      new appeng.parts.encoding.PatternEncodingLogic(
          new appeng.helpers.IPatternTerminalLogicHost() {
            @Override
            public appeng.parts.encoding.PatternEncodingLogic getLogic() {
              return encoding;
            }

            @Override
            public net.minecraft.world.level.Level getLevel() {
              return EncoderBlockEntity.this.getLevel();
            }

            @Override
            public void markForSave() {
              setChanged();
            }
          });

  @Override
  public appeng.parts.encoding.PatternEncodingLogic getLogic() {
    return encoding;
  }

  @Override
  public appeng.api.storage.MEStorage getInventory() {
    var grid = terminal.grid();
    return grid == null
        ? appeng.me.storage.NullInventory.of()
        : grid.getStorageService().getInventory();
  }

  @Override
  public ItemStack getMainMenuIcon() {
    return new ItemStack(Platform.block());
  }

  @Override
  public void returnToMainMenu(Player player, appeng.menu.ISubMenu menu) {
    player.openMenu(this);
  }

  @Override
  public void onLoad() {
    super.onLoad();
    if (!level.isClientSide)
      appeng.api.networking.GridHelper.onFirstTick(
          this,
          be -> {
            if (!be.isRemoved()) {
              be.terminal.refresh();
              be.terminal.node().create(be.level, be.worldPosition);
            }
          });
  }

  @Override
  public void setRemoved() {
    terminal.node().destroy();
    super.setRemoved();
  }

  @Override
  public void onChunkUnloaded() {
    terminal.node().destroy();
    super.onChunkUnloaded();
  }

  public EncoderBlockEntity(BlockPos pos, BlockState state) {
    super(MrpMod.ENTITY.get(), pos, state);
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("block.ae2mrp.composite_pattern_encoder");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory player, Player owner) {
    inventory.readDraft();
    return new EncoderMenu(
        id, player, inventory, ContainerLevelAccess.create(level, worldPosition), terminal);
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    var normal = new CompoundTag();
    encoding.writeToNBT(normal, registries);
    tag.put("PatternEncoding", normal);
    var settings = new CompoundTag();
    patternAccessConfig.writeToNBT(settings, registries);
    tag.put("PatternAccessSettings", settings);
    terminal.node().saveToNBT(tag);
    var controls = new ListTag();
    for (int i = 0; i < TerminalLogic.REGISTRATION_SLOTS; i++)
      if (!terminal.registered.getItem(i).isEmpty()) {
        var entry = new CompoundTag();
        entry.putInt("Slot", i);
        entry.put("Item", terminal.registered.getItem(i).save(registries));
        controls.add(entry);
      }
    tag.put("Registered", controls);
    var items = new ListTag();
    for (int i = 0; i < 10; i++)
      if (!inventory.physical(i).isEmpty()) {
        var entry = new CompoundTag();
        entry.putInt("Slot", i);
        entry.put("Item", inventory.physical(i).save(registries));
        items.add(entry);
      }
    tag.put("Items", items);
    tag.putIntArray("Weights", inventory.savedWeights());
    tag.putIntArray("Enabled", inventory.savedEnabled());
    tag.putString("DistributionMode", inventory.mode().name());
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    encoding.readFromNBT(tag.getCompound("PatternEncoding"), registries);
    patternAccessConfig.readFromNBT(tag.getCompound("PatternAccessSettings"), registries);
    terminal.node().loadFromNBT(tag);
    for (int i = 0; i < TerminalLogic.REGISTRATION_SLOTS; i++)
      terminal.registered.setItem(i, ItemStack.EMPTY);
    var controls = tag.getList("Registered", Tag.TAG_COMPOUND);
    for (int i = 0; i < controls.size(); i++) {
      var entry = controls.getCompound(i);
      int slot = entry.getInt("Slot");
      if (slot >= 0 && slot < TerminalLogic.REGISTRATION_SLOTS)
        terminal.registered.setItem(
            slot, ItemStack.parseOptional(registries, entry.getCompound("Item")));
    }
    var items = tag.getList("Items", Tag.TAG_COMPOUND);
    for (int i = 0; i < 10; i++) inventory.setItem(i, ItemStack.EMPTY);
    for (int i = 0; i < items.size(); i++) {
      var entry = items.getCompound(i);
      int slot = entry.getInt("Slot");
      if (slot >= 0 && slot < 10)
        inventory.setItem(slot, ItemStack.parseOptional(registries, entry.getCompound("Item")));
    }
    inventory.restoreSettings(tag.getIntArray("Weights"), tag.getIntArray("Enabled"));
    inventory.restoreMode(
        tag.contains("DistributionMode", Tag.TAG_STRING)
            ? tag.getString("DistributionMode")
            : "STRICT");
  }
}
