package com.syaru.ae2mrp.platform;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class EncoderBlock extends BaseEntityBlock {
  public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
  private static final VoxelShape NORTH = Block.box(2, 2, 13, 14, 14, 16);
  private static final VoxelShape SOUTH = Block.box(2, 2, 0, 14, 14, 3);
  private static final VoxelShape EAST = Block.box(0, 2, 2, 3, 14, 14);
  private static final VoxelShape WEST = Block.box(13, 2, 2, 16, 14, 14);
  private static final MapCodec<EncoderBlock> CODEC = simpleCodec(EncoderBlock::new);

  public EncoderBlock() {
    this(BlockBehaviour.Properties.of().strength(3.5f).noOcclusion());
  }

  private EncoderBlock(BlockBehaviour.Properties properties) {
    super(properties);
    registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
  }

  @Override
  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  @Override
  public RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    Direction face = context.getClickedFace();
    return defaultBlockState()
        .setValue(
            FACING,
            face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite());
  }

  @Override
  public VoxelShape getShape(
      BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
    return switch (state.getValue(FACING)) {
      case NORTH -> NORTH;
      case SOUTH -> SOUTH;
      case EAST -> EAST;
      case WEST -> WEST;
      default -> throw new IllegalStateException("Non-horizontal terminal facing");
    };
  }

  @Override
  public BlockState rotate(BlockState state, Rotation rotation) {
    return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
  }

  @Override
  public BlockState mirror(BlockState state, Mirror mirror) {
    return rotate(state, mirror.getRotation(state.getValue(FACING)));
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new EncoderBlockEntity(pos, state);
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (!level.isClientSide && level.getBlockEntity(pos) instanceof EncoderBlockEntity be)
      player.openMenu(be);
    return InteractionResult.sidedSuccess(level.isClientSide);
  }

  @Override
  public void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
    if (!state.is(replacement.getBlock())
        && level.getBlockEntity(pos) instanceof EncoderBlockEntity be) {
      var real = new net.minecraft.world.SimpleContainer(12);
      for (int i = 0; i < 10; i++) real.setItem(i, be.inventory.physical(i).copy());
      real.setItem(10, be.encoding.getBlankPatternInv().getStackInSlot(0).copy());
      real.setItem(11, be.encoding.getEncodedPatternInv().getStackInSlot(0).copy());
      Containers.dropContents(level, pos, real);
      Containers.dropContents(level, pos, be.terminal.registered);
    }
    super.onRemove(state, level, pos, replacement, moving);
  }
}
