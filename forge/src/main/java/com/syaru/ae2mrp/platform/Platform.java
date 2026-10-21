package com.syaru.ae2mrp.platform;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import com.syaru.ae2mrp.MrpMod;
import com.syaru.ae2mrp.core.DistributionMode;
import com.syaru.ae2mrp.encoder.EncoderMenu;
import com.syaru.ae2mrp.pattern.CompositeDefinition;
import com.syaru.ae2mrp.pattern.ControlPattern;
import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class Platform {
  public static CompoundTag keyTag(AEKey key, Level level) {
    return key.toTagGeneric();
  }

  public static AEKey readKey(CompoundTag tag, Level level) {
    return AEKey.fromTagGeneric(tag);
  }

  private Platform() {}

  public static Item blank() {
    return MrpMod.BLANK.get();
  }

  public static Item encoded() {
    return MrpMod.ENCODED.get();
  }

  public static Block block() {
    return MrpMod.ENCODER.get();
  }

  public static Item terminalItem() {
    return MrpMod.ENCODER_ITEM.get();
  }

  public static java.util.List<GenericStack> outputs(IPatternDetails pattern) {
    return java.util.List.of(pattern.getOutputs());
  }

  public static EncoderHost terminalAt(Level level, net.minecraft.core.BlockPos pos, int side) {
    if (side == -1) return level.getBlockEntity(pos) instanceof EncoderHost host ? host : null;
    if (side < 0 || side > 5) return null;
    return appeng.api.parts.PartHelper.getPart(
                level, pos, net.minecraft.core.Direction.from3DDataValue(side))
            instanceof EncoderHost host
        ? host
        : null;
  }

  public static MenuType<EncoderMenu> menu() {
    return MrpMod.MENU.get();
  }

  public static IPatternDetails decodeSource(ItemStack stack, Level level) {
    if (stack.is(encoded()) || stack.isEmpty()) return null;
    return PatternDetailsHelper.decodePattern(stack, level, false);
  }

  public static boolean isSupportedSource(IPatternDetails p) {
    return com.syaru.ae2mrp.pattern.SourcePatternRules.supported(p);
  }

  public static void write(ItemStack result, CompositeDefinition definition, Level level) {
    var tag = new CompoundTag();
    tag.putInt("schemaVersion", CompositeDefinition.SCHEMA);
    tag.putString("distributionMode", definition.mode().name());
    tag.putLong("configurationRevision", definition.revision());
    tag.put("primaryOutput", definition.output().toTagGeneric());
    var entries = new ListTag();
    var patterns = definition.patternCopies();
    for (int i = 0; i < patterns.size(); i++) {
      var entry = new CompoundTag();
      entry.put("pattern", patterns.get(i).save(new CompoundTag()));
      entry.put("patternIdentity", definition.details().get(i).getDefinition().toTagGeneric());
      entry.putInt("weight", definition.routes().get(i).weight());
      entry.putBoolean("enabled", definition.routes().get(i).enabled());
      entries.add(entry);
    }
    tag.put("patterns", entries);
    result.getOrCreateTag().put("ae2mrp", tag);
  }

  public static CompositeDefinition read(ItemStack stack, Level level) {
    if (!stack.is(encoded())
        || stack.getTag() == null
        || !stack.getTag().contains("ae2mrp", Tag.TAG_COMPOUND))
      throw new IllegalArgumentException("INVALID_DATA");
    var tag = stack.getTag().getCompound("ae2mrp");
    if (tag.getInt("schemaVersion") != CompositeDefinition.SCHEMA
        || !tag.contains("configurationRevision", Tag.TAG_LONG))
      throw new IllegalArgumentException("INVALID_DATA");
    var mode = DistributionMode.parse(tag.getString("distributionMode"));
    var entries = tag.getList("patterns", Tag.TAG_COMPOUND);
    if (entries.size() < 2 || entries.size() > 8) throw new IllegalArgumentException("ROUTE_COUNT");
    var patterns = new ArrayList<ItemStack>();
    var weights = new ArrayList<Integer>();
    var enabled = new ArrayList<Boolean>();
    for (int i = 0; i < entries.size(); i++) {
      var entry = entries.getCompound(i);
      if (!entry.contains("weight", Tag.TAG_INT) || !entry.contains("enabled", Tag.TAG_BYTE))
        throw new IllegalArgumentException("INVALID_DATA");
      patterns.add(ItemStack.of(entry.getCompound("pattern")));
      weights.add(entry.getInt("weight"));
      enabled.add(entry.getBoolean("enabled"));
      var decoded = decodeSource(patterns.get(i), level);
      if (decoded == null
          || !decoded
              .getDefinition()
              .equals(AEKey.fromTagGeneric(entry.getCompound("patternIdentity"))))
        throw new IllegalArgumentException("INVALID_DATA");
    }
    var definition =
        new CompositeDefinition(
            patterns, weights, enabled, mode, tag.getLong("configurationRevision"), level);
    if (!definition.output().equals(AEKey.fromTagGeneric(tag.getCompound("primaryOutput"))))
      throw new IllegalArgumentException("OUTPUT_MISMATCH");
    return definition;
  }

  public static ControlPattern control(ItemStack stack, Level level) {
    var definition = read(stack, level);
    var outputs = new GenericStack[] {new GenericStack(definition.output(), 1)};
    return new ControlPattern(AEItemKey.of(stack), definition) {
      @Override
      public GenericStack[] getOutputs() {
        return outputs;
      }
    };
  }
}
