package com.syaru.ae2mrp.encoder;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.KeyCounter;
import com.syaru.ae2mrp.pattern.ControlPattern;
import com.syaru.ae2mrp.platform.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Grid-owned control advertisements. No original recipe is executed by this terminal. */
public final class TerminalLogic implements IInWorldGridNodeHost, IActionHost, ICraftingProvider {
  public static final int REGISTRATION_COLUMNS = 9;
  public static final int REGISTRATION_ROWS = 6;
  public static final int REGISTRATION_SLOTS = REGISTRATION_COLUMNS * REGISTRATION_ROWS;
  private final Supplier<Level> level;
  private final IManagedGridNode node;
  private List<IPatternDetails> patterns = List.of();
  public final SimpleContainer registered =
      new SimpleContainer(REGISTRATION_SLOTS) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
          if (!stack.is(Platform.encoded()) || level.get() == null) return false;
          try {
            var output = Platform.read(stack, level.get()).output();
            for (int i = 0; i < getContainerSize(); i++)
              if (i != slot
                  && !getItem(i).isEmpty()
                  && Platform.read(getItem(i), level.get()).output().equals(output)) return false;
            return true;
          } catch (IllegalArgumentException ex) {
            return false;
          }
        }
      };

  public TerminalLogic(Supplier<Level> level, Runnable dirty) {
    this(level, dirty, null);
  }

  public TerminalLogic(Supplier<Level> level, Runnable dirty, IManagedGridNode sharedNode) {
    this.level = level;
    node =
        sharedNode != null
            ? sharedNode
            : GridHelper.createManagedNode(
                    this,
                    new IGridNodeListener<TerminalLogic>() {
                      @Override
                      public void onSaveChanges(TerminalLogic owner, IGridNode gridNode) {
                        dirty.run();
                      }
                    })
                .setInWorldNode(true)
                .setTagName("GridNode");
    node.setVisualRepresentation(Platform.terminalItem())
        .setIdlePowerUsage(1)
        .setFlags(GridFlags.REQUIRE_CHANNEL)
        .addService(ICraftingProvider.class, this);
    registered.addListener(
        container -> {
          dirty.run();
          refresh();
        });
  }

  public IManagedGridNode node() {
    return node;
  }

  public IGrid grid() {
    var n = node.getNode();
    return n != null && n.isActive() ? n.getGrid() : null;
  }

  public void refresh() {
    if (level.get() == null || level.get().isClientSide) return;
    var decoded = new ArrayList<IPatternDetails>();
    for (int i = 0; i < registered.getContainerSize(); i++) {
      var stack = registered.getItem(i);
      if (!stack.isEmpty()) {
        try {
          decoded.add(Platform.control(stack, level.get()));
        } catch (IllegalArgumentException ex) {
          // Invalid saved controls are retained for retrieval, never advertised.
        }
      }
    }
    patterns = List.copyOf(decoded);
    ICraftingProvider.requestUpdate(node);
  }

  @Override
  public IGridNode getGridNode(Direction side) {
    return node.getNode();
  }

  @Override
  public IGridNode getActionableNode() {
    return node.getNode();
  }

  @Override
  public List<IPatternDetails> getAvailablePatterns() {
    return patterns;
  }

  @Override
  public boolean isBusy() {
    return true;
  }

  @Override
  public boolean pushPattern(IPatternDetails pattern, KeyCounter[] inputs) {
    throw new IllegalStateException("COMPOSITE_CONTROL_IS_NOT_EXECUTABLE");
  }

  public static boolean mounted(IGrid grid, ItemStack stack, Level level) {
    if (grid == null) return false;
    var decoded = Platform.decodeSource(stack, level);
    if (decoded == null || decoded instanceof ControlPattern) return false;
    return grid.getCraftingService().getCraftingFor(decoded.getPrimaryOutput().what()).stream()
        .anyMatch(
            pattern ->
                !(pattern instanceof ControlPattern)
                    && pattern.getDefinition().equals(decoded.getDefinition()));
  }
}
