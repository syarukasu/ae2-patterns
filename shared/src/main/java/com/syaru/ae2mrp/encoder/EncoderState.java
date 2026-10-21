package com.syaru.ae2mrp.encoder;

import com.syaru.ae2mrp.core.DistributionMode;
import com.syaru.ae2mrp.pattern.CompositeDefinition;
import com.syaru.ae2mrp.platform.Platform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Real source slots and non-transferable editing previews have separate ownership. */
public final class EncoderState extends SimpleContainer {
  private final Supplier<Level> level;
  private final Runnable changed;
  private final ItemStack[] previews = new ItemStack[8];
  private final appeng.api.networking.IGrid[] referenceGrids = new appeng.api.networking.IGrid[8];
  private final int[] weights = new int[8];
  private final boolean[] enabled = new boolean[8];
  private boolean loading;
  private boolean malformedSettings;
  private DistributionMode mode = DistributionMode.STRICT;
  public String error = "READY";

  public EncoderState(Supplier<Level> level, Runnable changed) {
    super(10);
    this.level = level;
    this.changed = changed;
    Arrays.fill(previews, ItemStack.EMPTY);
    Arrays.fill(enabled, true);
    weights[0] = weights[1] = 50;
  }

  public boolean ghost(int slot) {
    return slot < 8 && super.getItem(slot).isEmpty() && !previews[slot].isEmpty();
  }

  public ItemStack physical(int slot) {
    return super.getItem(slot);
  }

  @Override
  public ItemStack getItem(int slot) {
    return ghost(slot) ? previews[slot] : super.getItem(slot);
  }

  @Override
  public ItemStack removeItem(int slot, int amount) {
    if (ghost(slot)) return ItemStack.EMPTY;
    return super.removeItem(slot, amount);
  }

  @Override
  public ItemStack removeItemNoUpdate(int slot) {
    if (ghost(slot)) return ItemStack.EMPTY;
    return super.removeItemNoUpdate(slot);
  }

  @Override
  public void setItem(int slot, ItemStack stack) {
    if (slot < 8) {
      previews[slot] = ItemStack.EMPTY;
      referenceGrids[slot] = null;
    }
    super.setItem(slot, stack);
    if (slot == 8 && !loading) readDraft();
  }

  @Override
  public void setChanged() {
    super.setChanged();
    if (changed != null) changed.run();
  }

  public int weight(int slot) {
    return weights[slot];
  }

  public DistributionMode mode() {
    return mode;
  }

  public void setMode(DistributionMode mode) {
    this.mode = java.util.Objects.requireNonNull(mode);
    setChanged();
  }

  public void restoreMode(String name) {
    try {
      mode = DistributionMode.parse(name);
    } catch (IllegalArgumentException ex) {
      malformedSettings = true;
      error = "INVALID_DATA";
    }
  }

  public boolean enabled(int slot) {
    return enabled[slot];
  }

  public void setWeight(int slot, int value) {
    if (slot >= 0 && slot < 8 && value >= 0 && value <= 100) {
      weights[slot] = value;
      setChanged();
    }
  }

  public void setEnabled(int slot, boolean value) {
    if (slot >= 0 && slot < 8) {
      enabled[slot] = value;
      setChanged();
    }
  }

  public void readDraft() {
    if (level.get() == null || !getItem(8).is(Platform.encoded())) return;
    Arrays.fill(previews, ItemStack.EMPTY);
    Arrays.fill(referenceGrids, null);
    try {
      var definition = Platform.read(getItem(8), level.get());
      mode = definition.mode();
      var sources = definition.patternCopies();
      for (int i = 0; i < sources.size(); i++) {
        previews[i] = sources.get(i);
        weights[i] = definition.routes().get(i).weight();
        enabled[i] = definition.routes().get(i).enabled();
      }
      error = "READY";
    } catch (IllegalArgumentException ex) {
      error = "INVALID_DATA";
    }
  }

  public boolean encode() {
    if (level.get() == null || level.get().isClientSide || !getItem(9).isEmpty()) return false;
    if (malformedSettings) {
      error = "INVALID_DATA";
      return false;
    }
    var carrier = physical(8);
    if (!carrier.is(Platform.blank()) && !carrier.is(Platform.encoded())) {
      error = "CARRIER_REQUIRED";
      return false;
    }
    try {
      var patterns = new ArrayList<ItemStack>();
      var proportions = new ArrayList<Integer>();
      var active = new ArrayList<Boolean>();
      for (int i = 0; i < 8; i++)
        if (!getItem(i).isEmpty()) {
          patterns.add(getItem(i).copyWithCount(1));
          proportions.add(weights[i]);
          active.add(enabled[i]);
        }
      long revision =
          carrier.is(Platform.encoded())
              ? Math.addExact(Platform.read(carrier, level.get()).revision(), 1)
              : 0;
      var definition =
          new CompositeDefinition(patterns, proportions, active, mode, revision, level.get());
      var result = new ItemStack(Platform.encoded());
      Platform.write(result, definition, level.get());
      loading = true;
      super.removeItem(8, 1);
      super.setItem(9, result);
      Arrays.fill(previews, ItemStack.EMPTY);
      Arrays.fill(referenceGrids, null);
      loading = false;
      error = "ENCODED";
      setChanged();
      return true;
    } catch (IllegalArgumentException ex) {
      error = ex.getMessage();
      return false;
    }
  }

  public int[] savedWeights() {
    return weights.clone();
  }

  public int[] savedEnabled() {
    return java.util.stream.IntStream.range(0, 8).map(i -> enabled[i] ? 1 : 0).toArray();
  }

  public void restoreSettings(int[] w, int[] e) {
    malformedSettings = w.length != 8 || e.length != 8;
    for (int i = 0; i < Math.min(8, Math.min(w.length, e.length)); i++)
      if (w[i] < 0 || w[i] > 100 || (e[i] != 0 && e[i] != 1)) malformedSettings = true;
    if (malformedSettings) {
      error = "INVALID_DATA";
      return;
    }
    for (int i = 0; i < 8; i++) {
      weights[i] = w[i];
      enabled[i] = e[i] == 1;
    }
  }

  public void clearPreview(int slot) {
    if (slot >= 0 && slot < 8) {
      previews[slot] = ItemStack.EMPTY;
      referenceGrids[slot] = null;
      setChanged();
    }
  }

  public boolean reference(int slot, ItemStack source, appeng.api.networking.IGrid grid) {
    if (slot < 0 || slot >= 8 || !physical(slot).isEmpty()) return false;
    if (!TerminalLogic.mounted(grid, source, level.get())) {
      error = "STALE_REFERENCE";
      return false;
    }
    var details = Platform.decodeSource(source, level.get());
    if (!Platform.isSupportedSource(details)) {
      error = "SOURCE_PATTERN_UNSUPPORTED";
      return false;
    }
    for (int i = 0; i < 8; i++) {
      if (i == slot || getItem(i).isEmpty()) continue;
      var other = Platform.decodeSource(getItem(i), level.get());
      if (other != null
          && !other.getPrimaryOutput().what().equals(details.getPrimaryOutput().what())) {
        error = "OUTPUT_MISMATCH";
        return false;
      }
      if (appeng.api.stacks.AEItemKey.of(getItem(i))
          .equals(appeng.api.stacks.AEItemKey.of(source))) {
        error = "DUPLICATE_PATTERN";
        return false;
      }
    }
    previews[slot] = source.copyWithCount(1);
    referenceGrids[slot] = grid;
    error = "READY";
    setChanged();
    return true;
  }

  public boolean validateReferences(appeng.api.networking.IGrid grid) {
    if (grid == null)
      for (int i = 0; i < 8; i++) {
        if (ghost(i) && referenceGrids[i] != null) {
          error = "NETWORK_OFFLINE";
          return false;
        }
      }
    for (int i = 0; i < 8; i++)
      if (ghost(i)
          && referenceGrids[i] != null
          && (referenceGrids[i] != grid
              || !TerminalLogic.mounted(grid, previews[i], level.get()))) {
        error = "STALE_REFERENCE";
        return false;
      }
    return true;
  }
}
