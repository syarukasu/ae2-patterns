package com.syaru.ae2mrp;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetailsDecoder;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEItemKey;
import com.syaru.ae2mrp.encoder.EncoderMenu;
import com.syaru.ae2mrp.platform.EncoderBlock;
import com.syaru.ae2mrp.platform.EncoderBlockEntity;
import com.syaru.ae2mrp.platform.Platform;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(MrpMod.ID)
public final class MrpMod {
  public static final String ID = "ae2mrp";
  private static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(ForgeRegistries.ITEMS, ID);
  private static final DeferredRegister<Block> BLOCKS =
      DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
  private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
      DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
  private static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);
  private static final DeferredRegister<CreativeModeTab> TABS =
      DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
  public static final RegistryObject<Item> BLANK =
      ITEMS.register("blank_composite_pattern", () -> new Item(new Item.Properties()));
  public static final RegistryObject<com.syaru.ae2mrp.platform.WirelessItem> WIRELESS =
      ITEMS.register("wireless_pattern_terminal", com.syaru.ae2mrp.platform.WirelessItem::new);
  public static final RegistryObject<Item> ENCODED =
      ITEMS.register(
          "encoded_composite_pattern", () -> new Item(new Item.Properties().stacksTo(1)));
  public static final RegistryObject<EncoderBlock> ENCODER =
      BLOCKS.register("composite_pattern_encoder", EncoderBlock::new);
  public static final RegistryObject<Item> ENCODER_ITEM =
      ITEMS.register(
          "composite_pattern_encoder",
          () ->
              new appeng.items.parts.PartItem<>(
                  new Item.Properties(),
                  com.syaru.ae2mrp.platform.EncoderPart.class,
                  com.syaru.ae2mrp.platform.EncoderPart::new));
  public static final RegistryObject<BlockEntityType<EncoderBlockEntity>> ENTITY =
      ENTITIES.register(
          "composite_pattern_encoder",
          () -> BlockEntityType.Builder.of(EncoderBlockEntity::new, ENCODER.get()).build(null));
  public static final RegistryObject<MenuType<EncoderMenu>> MENU =
      MENUS.register(
          "composite_pattern_encoder",
          () -> new MenuType<>(EncoderMenu::new, FeatureFlags.DEFAULT_FLAGS));
  public static final RegistryObject<CreativeModeTab> TAB =
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

  public MrpMod() {
    net.minecraftforge.fml.ModLoadingContext.get()
        .registerConfig(
            net.minecraftforge.fml.config.ModConfig.Type.SERVER,
            com.syaru.ae2mrp.platform.PlannerConfig.SPEC,
            "ae2mrp-server.toml");
    var bus = FMLJavaModLoadingContext.get().getModEventBus();
    ITEMS.register(bus);
    BLOCKS.register(bus);
    ENTITIES.register(bus);
    MENUS.register(bus);
    TABS.register(bus);
    bus.addListener(
        (net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
            event.enqueueWork(
                () ->
                    appeng.api.features.GridLinkables.register(
                        WIRELESS.get(),
                        appeng.items.tools.powered.WirelessTerminalItem.LINKABLE_HANDLER)));
    bus.addListener(
        (net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event) ->
            event.enqueueWork(
                () ->
                    PatternDetailsHelper.registerDecoder(
                        new IPatternDetailsDecoder() {
                          public boolean isEncodedPattern(ItemStack stack) {
                            return stack.is(ENCODED.get());
                          }

                          public IPatternDetails decodePattern(AEItemKey key, Level level) {
                            return decodePattern(key.toStack(), level, false);
                          }

                          public IPatternDetails decodePattern(
                              ItemStack stack, Level level, boolean recovery) {
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
