package com.syaru.ae2mrp.encoder;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.parts.encoding.EncodingMode;
import com.syaru.ae2mrp.core.DistributionMode;
import com.syaru.ae2mrp.platform.EncoderBlockEntity;
import com.syaru.ae2mrp.platform.NativeEncodingMenu;
import com.syaru.ae2mrp.platform.NativePatternMenu;
import com.syaru.ae2mrp.platform.Platform;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class EncoderMenu extends NativePatternMenu {
  public static final appeng.menu.SlotSemantic[] SOURCES =
      java.util.stream.IntStream.range(0, 8)
          .mapToObj(i -> SlotSemantics.register("ae2mrp_source_" + i, false))
          .toArray(appeng.menu.SlotSemantic[]::new);
  public static final appeng.menu.SlotSemantic CARRIER =
      SlotSemantics.register("ae2mrp_carrier", false);
  public static final appeng.menu.SlotSemantic RESULT =
      SlotSemantics.register("ae2mrp_result", false);
  public static final appeng.menu.SlotSemantic[] REGISTERED =
      java.util.stream.IntStream.range(0, TerminalLogic.REGISTRATION_ROWS)
          .mapToObj(i -> SlotSemantics.register("ae2mrp_registered_" + i, false))
          .toArray(appeng.menu.SlotSemantic[]::new);
  public static final int REGISTRY_START = 46;
  public static final int REGISTRY_END = REGISTRY_START + TerminalLogic.REGISTRATION_SLOTS;
  public static final int NATIVE_START = REGISTRY_END;
  private final EncoderBlockEntity host;
  private final NativeEncodingMenu normal;

  @GuiSync(2)
  public String outputKey = "";

  @GuiSync(97)
  public EncodingMode encodingMode = EncodingMode.CRAFTING;

  @GuiSync(96)
  public boolean substitution;

  @GuiSync(95)
  public boolean fluidSubstitution = true;

  @GuiSync(94)
  public net.minecraft.resources.ResourceLocation stonecuttingRecipe;

  public static final String[] STATUS = {
    "READY",
    "ENCODED",
    "ROUTE_COUNT",
    "SIMPLE_PROCESSING_ONLY",
    "DUPLICATE_PATTERN",
    "OUTPUT_MISMATCH",
    "POSITIVE_ROUTE_COUNT",
    "WEIGHT_TOTAL",
    "CARRIER_REQUIRED",
    "INVALID_DATA",
    "NETWORK_OFFLINE",
    "STALE_REFERENCE",
    "SLOTS_FULL"
  };
  private final Container inventory;
  private final ContainerLevelAccess access;
  private final int[] weights = new int[8], enabled = new int[8];
  private int selected, status;
  private int previewMask;
  private int mode;
  private final TerminalLogic terminal;
  private int online;
  private WirelessAccess wirelessAccess;
  private int wirelessSlot = -1;

  public void setWirelessAccess(WirelessAccess access) {
    wirelessAccess = access;
    lockPlayerInventorySlot(access.inventorySlot());
  }

  public int wirelessSlot() {
    return wirelessAccess == null ? wirelessSlot : wirelessAccess.inventorySlot();
  }

  public EncoderMenu(int id, Inventory player) {
    this(id, player, new SimpleContainer(10), ContainerLevelAccess.NULL);
  }

  public EncoderMenu(int id, Inventory player, Container inventory, ContainerLevelAccess access) {
    this(id, player, inventory, access, null);
  }

  public EncoderMenu(
      int id,
      Inventory player,
      Container inventory,
      ContainerLevelAccess access,
      TerminalLogic terminal) {
    super(Platform.menu(), id, player, findHost(player, access));
    host = (EncoderBlockEntity) getTarget();
    this.inventory = inventory;
    this.access = access;
    this.terminal = terminal;
    for (int i = 0; i < 8; i++) {
      final int index = i;
      addSlot(
          new Slot(inventory, i, 14 + (i % 4) * 72, 51 + (i / 4) * 30) {
            @Override
            public boolean mayPlace(ItemStack stack) {
              return !stack.is(Platform.encoded()) && PatternDetailsHelper.isEncodedPattern(stack);
            }

            @Override
            public boolean mayPickup(Player p) {
              return !preview(index);
            }

            @Override
            public int getMaxStackSize() {
              return 1;
            }
          },
          SOURCES[i]);
    }
    addSlot(
        new Slot(inventory, 8, 14, 110) {

          @Override
          public boolean mayPlace(ItemStack stack) {
            return stack.is(Platform.blank()) || stack.is(Platform.encoded());
          }

          @Override
          public int getMaxStackSize() {
            return 1;
          }
        },
        CARRIER);
    addSlot(
        new Slot(inventory, 9, 166, 110) {

          @Override
          public boolean mayPlace(ItemStack stack) {
            return false;
          }
        },
        RESULT);
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 9; col++)
        addSlot(
            new Slot(player, col + row * 9 + 9, 75 + col * 18, 160 + row * 18),
            SlotSemantics.PLAYER_INVENTORY);
    for (int col = 0; col < 9; col++)
      addSlot(new Slot(player, col, 75 + col * 18, 218), SlotSemantics.PLAYER_HOTBAR);
    Container registered =
        terminal == null
            ? new SimpleContainer(TerminalLogic.REGISTRATION_SLOTS)
            : terminal.registered;
    for (int i = 0; i < TerminalLogic.REGISTRATION_SLOTS; i++) {
      addSlot(
          new Slot(registered, i, 8 + i % TerminalLogic.REGISTRATION_COLUMNS * 18, 160) {
            @Override
            public boolean mayPlace(ItemStack stack) {
              return stack.is(Platform.encoded())
                  && registered.canPlaceItem(getContainerSlot(), stack);
            }

            @Override
            public int getMaxStackSize() {
              return 1;
            }
          },
          REGISTERED[i / TerminalLogic.REGISTRATION_COLUMNS]);
    }
    addDataSlot(
        new DataSlot() {
          public int get() {
            return wirelessSlot();
          }

          public void set(int value) {
            wirelessSlot = value;
          }
        });
    addDataSlot(
        new DataSlot() {
          public int get() {
            return terminal == null ? online : terminal.grid() == null ? 0 : 1;
          }

          public void set(int value) {
            online = value;
          }
        });
    for (int i = 0; i < 8; i++) {
      final int slot = i;
      addDataSlot(
          new DataSlot() {
            public int get() {
              return inventory instanceof EncoderState s ? s.weight(slot) : weights[slot];
            }

            public void set(int value) {
              weights[slot] = value;
            }
          });
      addDataSlot(
          new DataSlot() {
            public int get() {
              return inventory instanceof EncoderState s
                  ? (s.enabled(slot) ? 1 : 0)
                  : enabled[slot];
            }

            public void set(int value) {
              enabled[slot] = value;
            }
          });
    }
    addDataSlot(
        new DataSlot() {
          public int get() {
            if (!(inventory instanceof EncoderState state)) return previewMask;
            int mask = 0;
            for (int i = 0; i < 8; i++) if (state.ghost(i)) mask |= 1 << i;
            return mask;
          }

          public void set(int value) {
            previewMask = value;
          }
        });
    addDataSlot(
        new DataSlot() {
          public int get() {
            return inventory instanceof EncoderState s ? statusIndex(s.error) : status;
          }

          public void set(int value) {
            status = value;
          }
        });
    addDataSlot(
        new DataSlot() {
          public int get() {
            return inventory instanceof EncoderState state ? state.mode().ordinal() : mode;
          }

          public void set(int value) {
            if (value < 0 || value >= DistributionMode.values().length)
              throw new IllegalArgumentException("INVALID_DISTRIBUTION_MODE");
            mode = value;
          }
        });
    normal = new NativeEncodingMenu(id, player, host);
    for (var slot : normal.slots) addSlot(slot, normal.getSlotSemantic(slot));
    registerClientAction("reference", String.class, this::selectReference);
    registerClientAction("encode", () -> normalAction(normal::encode));
    registerClientAction("clear", () -> normalAction(normal::clear));
    registerClientAction(
        "setMode", EncodingMode.class, value -> normalAction(() -> host.encoding.setMode(value)));
    registerClientAction(
        "setSubstitution",
        Boolean.class,
        value -> normalAction(() -> host.encoding.setSubstitution(value)));
    registerClientAction(
        "setFluidSubstitution",
        Boolean.class,
        value -> normalAction(() -> host.encoding.setFluidSubstitution(value)));
    registerClientAction(
        "setStonecuttingRecipeId",
        net.minecraft.resources.ResourceLocation.class,
        value -> normalAction(() -> host.encoding.setStonecuttingRecipeId(value)));
    registerClientAction(
        "cycleProcessingOutput", () -> normalAction(normal::cycleProcessingOutput));
    updateNormalState();
  }

  private static EncoderBlockEntity findHost(Inventory player, ContainerLevelAccess access) {
    var block = access.evaluate((level, pos) -> level.getBlockEntity(pos), null);
    if (block instanceof EncoderBlockEntity terminal) return terminal;
    if (!player.player.level().isClientSide())
      throw new IllegalArgumentException("TERMINAL_HOST_REQUIRED");
    var client =
        new EncoderBlockEntity(
            net.minecraft.core.BlockPos.ZERO, Platform.block().defaultBlockState());
    client.setLevel(player.player.level());
    return client;
  }

  public NativeEncodingMenu nativeEncoder() {
    return normal;
  }

  public boolean nativeSlot(Slot slot) {
    return slot != null
        && slot.index >= NATIVE_START
        && slot.index < NATIVE_START + normal.slots.size();
  }

  public void requestReference(ItemStack stack) {
    if (!stack.isEmpty())
      sendClientAction(
          "reference",
          Platform.keyTag(appeng.api.stacks.AEItemKey.of(stack), getPlayer().level()).toString());
  }

  public boolean selectReference(String encoded) {
    if (getPlayer().containerMenu != this || !stillValid(getPlayer())) return false;
    try {
      var key =
          Platform.readKey(net.minecraft.nbt.TagParser.parseTag(encoded), getPlayer().level());
      if (!(key instanceof appeng.api.stacks.AEItemKey pattern) || terminal == null) return false;
      boolean accepted = reference(getPlayer(), pattern.toStack(), terminal.grid());
      if (accepted) {
        var details = Platform.decodeSource(pattern.toStack(), getPlayer().level());
        outputKey =
            Platform.keyTag(details.getPrimaryOutput().what(), getPlayer().level()).toString();
      } else {
        getPlayer()
            .displayClientMessage(
                net.minecraft.network.chat.Component.translatable("status.ae2mrp." + statusKey()),
                true);
      }
      broadcastChanges();
      return accepted;
    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException | IllegalArgumentException ex) {
      return false;
    }
  }

  private void normalAction(Runnable action) {
    if (getPlayer().containerMenu != this || !stillValid(getPlayer()) || !online()) return;
    action.run();
    updateNormalState();
    broadcastChanges();
  }

  private void updateNormalState() {
    normal.refreshFromHost();
    encodingMode = normal.mode;
    substitution = normal.substitute;
    fluidSubstitution = normal.substituteFluids;
    stonecuttingRecipe = normal.stonecuttingRecipeId;
  }

  @Override
  protected void afterNativeDataSync() {
    normal.mode = encodingMode;
    normal.substitute = substitution;
    normal.substituteFluids = fluidSubstitution;
    normal.stonecuttingRecipeId = stonecuttingRecipe;
    normal.applySynchronizedState();
  }

  @Override
  public void onSlotChange(Slot slot) {
    if (normal != null && nativeSlot(slot)) normal.onSlotChange(slot);
  }

  private void refreshNativePreview() {
    if (isClientSide())
      for (var slot : normal.getSlots(SlotSemantics.CRAFTING_RESULT)) normal.onSlotChange(slot);
  }

  @Override
  public void setItem(int index, int revision, ItemStack stack) {
    super.setItem(index, revision, stack);
    refreshNativePreview();
  }

  @Override
  public void initializeContents(int revision, java.util.List<ItemStack> items, ItemStack carried) {
    super.initializeContents(revision, items, carried);
    refreshNativePreview();
  }

  @Override
  public void setFilter(int index, ItemStack stack) {
    if (!stillValid(getPlayer())
        || index < NATIVE_START
        || index >= NATIVE_START + normal.slots.size()) return;
    normal.setFilter(index - NATIVE_START, stack);
  }

  @Override
  public void doAction(
      net.minecraft.server.level.ServerPlayer player,
      appeng.helpers.InventoryAction action,
      int index,
      long remoteId) {
    if (remoteId != 0
        || getPlayer().containerMenu != this
        || !stillValid(player)
        || index < NATIVE_START
        || index >= NATIVE_START + normal.slots.size()) return;
    if (action == appeng.helpers.InventoryAction.MOVE_REGION) {
      quickMoveStack(player, index);
      return;
    }
    normal.setCarried(getCarried());
    normal.doAction(player, action, index - NATIVE_START, 0);
    setCarried(normal.getCarried());
  }

  public boolean online() {
    return terminal == null ? online != 0 : terminal.grid() != null;
  }

  @Override
  public void clicked(
      int index, int button, net.minecraft.world.inventory.ClickType type, Player player) {
    if (!stillValid(player)) return;
    if (isWirelessSlot(index)
        || type == net.minecraft.world.inventory.ClickType.SWAP && button == wirelessSlot()) return;
    if (index >= 0 && index < 8 && preview(index)) return;
    super.clicked(index, button, type, player);
  }

  public void tickAccess(AbstractContainerMenu activeMenu) {
    if (wirelessAccess != null) wirelessAccess.tick(activeMenu);
  }

  public boolean reference(Player player, ItemStack source, appeng.api.networking.IGrid grid) {
    if (!stillValid(player)
        || terminal == null
        || terminal.grid() != grid
        || !(inventory instanceof EncoderState state)) return false;
    int target = selected;
    if (!state.getItem(target).isEmpty()) {
      target = -1;
      for (int i = 0; i < 8; i++)
        if (state.getItem(i).isEmpty()) {
          target = i;
          break;
        }
    }
    if (target < 0) {
      state.error = "SLOTS_FULL";
      return false;
    }
    if (!state.reference(target, source, grid)) return false;
    selected = target;
    return true;
  }

  private static int statusIndex(String value) {
    for (int i = 0; i < STATUS.length; i++) if (STATUS[i].equals(value)) return i;
    return 9;
  }

  public int weight(int i) {
    return inventory instanceof EncoderState s ? s.weight(i) : weights[i];
  }

  public boolean preview(int i) {
    return inventory instanceof EncoderState state ? state.ghost(i) : (previewMask & (1 << i)) != 0;
  }

  public DistributionMode mode() {
    return inventory instanceof EncoderState state ? state.mode() : DistributionMode.values()[mode];
  }

  public boolean enabled(int i) {
    return inventory instanceof EncoderState s ? s.enabled(i) : enabled[i] != 0;
  }

  public String statusKey() {
    int value = inventory instanceof EncoderState state ? statusIndex(state.error) : status;
    return STATUS[Math.max(0, Math.min(value, STATUS.length - 1))].toLowerCase(
        java.util.Locale.ROOT);
  }

  @Override
  public boolean stillValid(Player player) {
    if (wirelessAccess != null) return wirelessAccess.valid();
    return stillValid(access, player, Platform.block());
  }

  private boolean isWirelessSlot(int index) {
    return wirelessSlot() >= 0
        && index >= 10
        && index < REGISTRY_START
        && slots.get(index).getContainerSlot() == wirelessSlot();
  }

  @Override
  public void broadcastChanges() {
    tickAccess(this);
    if (!isClientSide()) updateNormalState();
    super.broadcastChanges();
  }

  @Override
  public boolean clickMenuButton(Player player, int id) {
    if (!stillValid(player) || !(inventory instanceof EncoderState state)) return false;
    if (id >= 101 && id <= 108) {
      selected = id - 101;
      return true;
    }
    if (id >= 0 && id <= 100) {
      state.setWeight(selected, id);
      return true;
    }
    if (id == 109) {
      state.setEnabled(selected, !state.enabled(selected));
      return true;
    }
    if (id == 110) {
      state.clearPreview(selected);
      return true;
    }
    if (id >= 111 && id <= 114) {
      int delta =
          switch (id) {
            case 111 -> -1;
            case 112 -> 1;
            case 113 -> -5;
            default -> 5;
          };
      state.setWeight(selected, Math.max(0, Math.min(100, state.weight(selected) + delta)));
      return true;
    }
    if (id == 120) {
      if (state.validateReferences(terminal == null ? null : terminal.grid()) && state.encode()) {
        for (int i = 0; i < 8; i++) {
          var source = state.physical(i);
          if (source.isEmpty()) continue;
          var returned = source.copy();
          player.getInventory().add(returned);
          state.setItem(i, returned);
        }
      }
      broadcastChanges();
      return true;
    }
    if (id == 121 || id == 122) {
      state.setMode(id == 121 ? DistributionMode.STRICT : DistributionMode.ADAPTIVE);
      broadcastChanges();
      return true;
    }
    return false;
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    if (isWirelessSlot(index)) return ItemStack.EMPTY;
    if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
    var slot = slots.get(index);
    if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
    var item = slot.getItem();
    var original = item.copy();
    boolean moved;
    if (index < 10 || index >= REGISTRY_START) moved = moveItemStackTo(item, 10, 46, true);
    else if (appeng.core.definitions.AEItems.BLANK_PATTERN.isSameAs(item)) {
      var blank = getSlots(SlotSemantics.BLANK_PATTERN).get(0);
      moved = moveItemStackTo(item, blank.index, blank.index + 1, false);
    } else if (item.is(Platform.encoded()))
      moved = moveItemStackTo(item, REGISTRY_START, REGISTRY_END, false);
    else if (item.is(Platform.blank())) moved = moveItemStackTo(item, 8, 9, false);
    else if (PatternDetailsHelper.isEncodedPattern(item))
      moved = moveItemStackTo(item, 0, 8, false);
    else
      moved =
          index < 37 ? moveItemStackTo(item, 37, 46, false) : moveItemStackTo(item, 10, 37, false);
    if (!moved) return ItemStack.EMPTY;
    if (item.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
    else slot.setChanged();
    slot.onTake(player, item);
    return original;
  }
}
