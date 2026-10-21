package com.syaru.ae2mrp;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import com.syaru.ae2mrp.encoder.EncoderMenu;
import com.syaru.ae2mrp.platform.EncoderBlock;
import com.syaru.ae2mrp.platform.EncoderBlockEntity;
import com.syaru.ae2mrp.platform.Platform;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(MrpMod.ID)
public final class MrpMod {
  public static final String ID = "ae2mrp";
  private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
  private static final DeferredRegister<Block> BLOCKS =
      DeferredRegister.create(Registries.BLOCK, ID);
  private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
  private static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, ID);
  private static final DeferredRegister<CreativeModeTab> TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
  private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
      DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ID);
  public static final Supplier<Item> BLANK =
      ITEMS.register("blank_composite_pattern", () -> new Item(new Item.Properties()));
  public static final Supplier<com.syaru.ae2mrp.platform.WirelessItem> WIRELESS =
      ITEMS.register("wireless_pattern_terminal", com.syaru.ae2mrp.platform.WirelessItem::new);
  public static final Supplier<Item> ENCODED =
      ITEMS.register(
          "encoded_composite_pattern", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final Supplier<EncoderBlock> ENCODER =
      BLOCKS.register("composite_pattern_encoder", EncoderBlock::new);
  public static final Supplier<Item> ENCODER_ITEM =
      ITEMS.register(
          "composite_pattern_encoder",
          () ->
              new appeng.items.parts.PartItem<>(
                  new Item.Properties(),
                  com.syaru.ae2mrp.platform.EncoderPart.class,
                  com.syaru.ae2mrp.platform.EncoderPart::new));
  public static final Supplier<BlockEntityType<EncoderBlockEntity>> ENTITY =
      ENTITIES.register(
          "composite_pattern_encoder",
          () -> BlockEntityType.Builder.of(EncoderBlockEntity::new, ENCODER.get()).build(null));
  public static final Supplier<MenuType<EncoderMenu>> MENU =
      MENUS.register(
          "composite_pattern_encoder",
          () -> new MenuType<>(EncoderMenu::new, FeatureFlags.DEFAULT_FLAGS));
  public static final Supplier<CreativeModeTab> TAB =
      TABS.register(
          "patterns",
          () ->
              CreativeModeTab.builder()
                  .title(Component.translatable("itemGroup.ae2mrp.patterns"))
                  .icon(() -> new ItemStack(ENCODER_ITEM.get()))
                  .displayItems(
                      (parameters, output) -> {
                        output.accept(ENCODER_ITEM.get());
                        output.accept(WIRELESS.get());
                        output.accept(BLANK.get());
                        output.accept(ENCODED.get());
                      })
                  .build());
  public static final Supplier<DataComponentType<CompoundTag>> CONFIGURATION =
      COMPONENTS.register(
          "composite_configuration",
          () ->
              DataComponentType.<CompoundTag>builder()
                  .persistent(CompoundTag.CODEC)
                  .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                  .build());

  public MrpMod(IEventBus bus, net.neoforged.fml.ModContainer container) {
    container.registerConfig(
        net.neoforged.fml.config.ModConfig.Type.SERVER,
        com.syaru.ae2mrp.platform.PlannerConfig.SPEC,
        "ae2mrp-server.toml");
    ITEMS.register(bus);
    BLOCKS.register(bus);
    ENTITIES.register(bus);
    MENUS.register(bus);
    COMPONENTS.register(bus);
    TABS.register(bus);
    bus.addListener(
        (net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
            event.enqueueWork(
                () ->
                    appeng.api.features.GridLinkables.register(
                        WIRELESS.get(),
                        appeng.items.tools.powered.WirelessTerminalItem.LINKABLE_HANDLER)));
    bus.addListener(
        (net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) ->
            event.registerBlockEntity(
                appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ENTITY.get(),
                (be, context) -> be.terminal));
    bus.addListener(
        (net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
            event.enqueueWork(
                () ->
                    PatternDetailsHelper.registerDecoder(
                        new IPatternDetailsDecoder() {
                          public boolean isEncodedPattern(ItemStack stack) {
                            return stack.is(ENCODED.get());
                          }

                          public IPatternDetails decodePattern(AEItemKey key, Level level) {
                            var stack = key.toStack();
                            if (!stack.is(ENCODED.get())) return null;
                            try {
                              return Platform.control(stack, level);
                            } catch (IllegalArgumentException ex) {
                              return null;
                            }
                          }
                        })));
  }
}
