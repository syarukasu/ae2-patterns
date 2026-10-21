package com.syaru.ae2mrp.platform;

import appeng.api.parts.IPartItem;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import com.syaru.ae2mrp.encoder.EncoderMenu;
import com.syaru.ae2mrp.encoder.EncoderState;
import com.syaru.ae2mrp.encoder.TerminalLogic;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Native AE2 placement, face models and collision boxes; only editor behavior is extended. */
public final class EncoderPart extends PatternEncodingTerminalPart implements EncoderHost {
  private final EncoderState editor = new EncoderState(this::getLevel, this::changed);
  private final TerminalLogic terminal =
      new TerminalLogic(this::getLevel, this::changed, getMainNode());

  public EncoderPart(IPartItem<?> item) {
    super(item);
    getConfigManager()
        .registerSetting(
            appeng.api.config.Settings.TERMINAL_SHOW_PATTERN_PROVIDERS,
            appeng.api.config.ShowPatternProviders.VISIBLE);
  }

  private void changed() {
    if (getHost() != null) getHost().markForSave();
  }

  @Override
  public Level getLevel() {
    return getBlockEntity() == null ? null : super.getLevel();
  }

  @Override
  public Level hostLevel() {
    return getLevel();
  }

  @Override
  public EncoderState editor() {
    return editor;
  }

  @Override
  public TerminalLogic terminal() {
    return terminal;
  }

  @Override
  public BlockPos position() {
    return getBlockEntity().getBlockPos();
  }

  @Override
  public int partSide() {
    return getSide().ordinal();
  }

  @Override
  public boolean present() {
    return getHost() != null
        && getLevel() != null
        && getLevel().hasChunkAt(position())
        && !getBlockEntity().isRemoved()
        && getHost().getPart(getSide()) == this;
  }

  @Override
  public void addToWorld() {
    terminal.refresh();
    super.addToWorld();
  }

  @Override
  public MenuType<?> getMenuType(Player player) {
    return Platform.menu();
  }

  private void open(Player player) {
    if (!player.level().isClientSide)
      player.openMenu(
          new SimpleMenuProvider(
              (id, inv, owner) -> {
                editor.readDraft();
                return new EncoderMenu(
                    id,
                    inv,
                    editor,
                    ContainerLevelAccess.create(getLevel(), position()),
                    terminal,
                    this);
              },
              getName()));
  }

  @Override
  public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
    open(player);
    return true;
  }

  @Override
  public void returnToMainMenu(Player player, appeng.menu.ISubMenu menu) {
    open(player);
  }

  @Override
  public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
    super.addAdditionalDrops(drops, wrenched);
    for (int i = 0; i < 10; i++)
      if (!editor.physical(i).isEmpty()) drops.add(editor.physical(i).copy());
    for (int i = 0; i < TerminalLogic.REGISTRATION_SLOTS; i++)
      if (!terminal.registered.getItem(i).isEmpty())
        drops.add(terminal.registered.getItem(i).copy());
  }

  @Override
  public void clearContent() {
    super.clearContent();
    editor.clearContent();
    terminal.registered.clearContent();
  }

  @Override
  public void writeToNBT(CompoundTag data) {
    super.writeToNBT(data);
    var tag = new CompoundTag();
    var items = new ListTag();
    for (int i = 0; i < 10; i++)
      if (!editor.physical(i).isEmpty()) {
        var entry = new CompoundTag();
        entry.putInt("Slot", i);
        entry.put("Item", editor.physical(i).save(new CompoundTag()));
        items.add(entry);
      }
    tag.put("Items", items);
    var registered = new ListTag();
    for (int i = 0; i < TerminalLogic.REGISTRATION_SLOTS; i++)
      if (!terminal.registered.getItem(i).isEmpty()) {
        var entry = new CompoundTag();
        entry.putInt("Slot", i);
        entry.put("Item", terminal.registered.getItem(i).save(new CompoundTag()));
        registered.add(entry);
      }
    tag.put("Registered", registered);
    tag.putIntArray("Weights", editor.savedWeights());
    tag.putIntArray("Enabled", editor.savedEnabled());
    tag.putString("DistributionMode", editor.mode().name());
    data.put("CompositeEditor", tag);
  }

  @Override
  public void readFromNBT(CompoundTag data) {
    super.readFromNBT(data);
    if (!data.contains("CompositeEditor", Tag.TAG_COMPOUND)) return;
    var tag = data.getCompound("CompositeEditor");
    editor.clearContent();
    terminal.registered.clearContent();
    var items = tag.getList("Items", Tag.TAG_COMPOUND);
    for (int i = 0; i < items.size(); i++) {
      var entry = items.getCompound(i);
      int slot = entry.getInt("Slot");
      if (slot >= 0 && slot < 10) editor.setItem(slot, ItemStack.of(entry.getCompound("Item")));
    }
    var registered = tag.getList("Registered", Tag.TAG_COMPOUND);
    for (int i = 0; i < registered.size(); i++) {
      var entry = registered.getCompound(i);
      int slot = entry.getInt("Slot");
      if (slot >= 0 && slot < TerminalLogic.REGISTRATION_SLOTS)
        terminal.registered.setItem(slot, ItemStack.of(entry.getCompound("Item")));
    }
    editor.restoreSettings(tag.getIntArray("Weights"), tag.getIntArray("Enabled"));
    editor.restoreMode(tag.getString("DistributionMode"));
  }
}
