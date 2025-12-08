package com.leclowndu93150.stackeddimensions.item;

import com.leclowndu93150.stackeddimensions.block.DimensionalPipeBlock;
import com.leclowndu93150.stackeddimensions.block.DimensionalPipeBlockEntity;
import com.leclowndu93150.stackeddimensions.block.PortalBlock;
import com.leclowndu93150.stackeddimensions.config.PortalConfig;
import com.leclowndu93150.stackeddimensions.config.PortalConfigLoader;
import com.leclowndu93150.stackeddimensions.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class DimensionalPipeItem extends Item {

    public DimensionalPipeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof PortalBlock)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ServerLevel serverLevel = (ServerLevel) level;

        boolean isCeiling = state.getValue(PortalBlock.CEILING);
        Direction facing = isCeiling ? Direction.DOWN : Direction.UP;

        TargetInfo target = findTargetPosition(serverLevel, pos, state);

        if (target == null) {
            return InteractionResult.FAIL;
        }

        if (target.isDynamic) {
            placeLocalPipeOnly(serverLevel, pos, facing);
        } else {
            placePipePair(serverLevel, pos, facing, target);
        }

        context.getItemInHand().shrink(1);

        level.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

        return InteractionResult.SUCCESS;
    }

    private void placeLocalPipeOnly(ServerLevel level, BlockPos pos, Direction facing) {
        BlockState pipeState = ModBlocks.DIMENSIONAL_PIPE.get().defaultBlockState()
                .setValue(DimensionalPipeBlock.FACING, facing);
        level.setBlock(pos, pipeState, 3);
    }

    private void placePipePair(ServerLevel sourceLevel, BlockPos sourcePos, Direction sourceFacing, TargetInfo target) {
        BlockState sourcePipeState = ModBlocks.DIMENSIONAL_PIPE.get().defaultBlockState()
                .setValue(DimensionalPipeBlock.FACING, sourceFacing);
        sourceLevel.setBlock(sourcePos, sourcePipeState, 3);

        Direction targetFacing = sourceFacing.getOpposite();
        BlockState targetPipeState = ModBlocks.DIMENSIONAL_PIPE.get().defaultBlockState()
                .setValue(DimensionalPipeBlock.FACING, targetFacing);
        target.level.setBlock(target.pos, targetPipeState, 3);

        if (sourceLevel.getBlockEntity(sourcePos) instanceof DimensionalPipeBlockEntity sourcePipe &&
                target.level.getBlockEntity(target.pos) instanceof DimensionalPipeBlockEntity targetPipe) {
            sourcePipe.link(targetPipe);
            targetPipe.link(sourcePipe);
        }
    }

    private TargetInfo findTargetPosition(ServerLevel level, BlockPos pos, BlockState portalState) {
        PortalConfig config = PortalConfigLoader.load();
        String currentDim = level.dimension().location().toString();

        PortalConfig.PortalDefinition portal = config.getPortalForDimension(currentDim);
        if (portal == null || !portal.enabled) return null;

        ResourceLocation targetDimRL = new ResourceLocation(portal.targetDimension);
        ResourceKey<Level> targetDimKey = ResourceKey.create(Registries.DIMENSION, targetDimRL);
        ServerLevel targetLevel = level.getServer().getLevel(targetDimKey);
        if (targetLevel == null) return null;

        PortalConfig.PortalDefinition targetPortal = config.getPortalBySourceAndTarget(
                portal.targetDimension, currentDim);
        if (targetPortal == null) return null;

        PortalBlock.PortalLayer layer = portalState.getValue(PortalBlock.LAYER);
        int offset = switch (layer) {
            case BOTTOM -> 0;
            case MIDDLE -> 1;
            case TOP -> 2;
        };

        int targetY;
        if (targetPortal.portalType == PortalConfig.PortalDefinition.PortalType.CEILING) {
            targetY = targetPortal.bedrockYLevel - offset;
        } else {
            targetY = targetPortal.bedrockYLevel + offset;
        }

        BlockPos targetPos = new BlockPos(pos.getX(), targetY, pos.getZ());

        BlockState targetState = targetLevel.getBlockState(targetPos);
        if (!(targetState.getBlock() instanceof PortalBlock)) {
            return null;
        }

        return new TargetInfo(targetLevel, targetPos, targetPortal.dynamicLoading);
    }

    private record TargetInfo(ServerLevel level, BlockPos pos, boolean isDynamic) {}
}
