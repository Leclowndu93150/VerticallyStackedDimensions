package com.leclowndu93150.stackeddimensions.block;

import com.leclowndu93150.stackeddimensions.Stackeddimensions;
import com.leclowndu93150.stackeddimensions.config.PortalConfig;
import com.leclowndu93150.stackeddimensions.config.PortalConfigLoader;
import com.leclowndu93150.stackeddimensions.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.world.ForgeChunkManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DimensionalPipeBlockEntity extends BlockEntity {

    private static final byte ITEM = 1, FLUID = 2;

    private ResourceKey<Level> linkedDimension;
    private BlockPos linkedPos;
    private DimensionalPipeBlockEntity cachedLink;
    private boolean needsLinkUpdate = true;
    private byte hasCap;
    private boolean chunksForceLoaded = false;

    public DimensionalPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIMENSIONAL_PIPE.get(), pos, state);
    }

    public Direction getSide() {
        return getBlockState().getValue(DimensionalPipeBlock.FACING);
    }

    public void link(DimensionalPipeBlockEntity other) {
        if (other == null) {
            this.linkedDimension = null;
            this.linkedPos = null;
            this.cachedLink = null;
        } else {
            this.linkedDimension = other.level.dimension();
            this.linkedPos = other.worldPosition;
            this.cachedLink = other;
        }
        this.needsLinkUpdate = false;
        setChanged();
    }

    @Nullable
    public DimensionalPipeBlockEntity getLinkedPipe() {
        if (level == null || level.isClientSide()) return null;

        if (needsLinkUpdate) {
            updateLink();
        }

        if (cachedLink != null && cachedLink.isRemoved()) {
            cachedLink = null;
        }

        if (cachedLink == null && linkedDimension != null && linkedPos != null) {
            ServerLevel targetLevel = ((ServerLevel) level).getServer().getLevel(linkedDimension);
            if (targetLevel != null) {
                targetLevel.getChunk(linkedPos);
                BlockEntity be = targetLevel.getBlockEntity(linkedPos);
                if (be instanceof DimensionalPipeBlockEntity pipe) {
                    cachedLink = pipe;
                }
            }
        }

        return cachedLink;
    }

    private void updateLink() {
        needsLinkUpdate = false;
        if (level == null || level.isClientSide()) return;

        PortalConfig config = PortalConfigLoader.load();
        String currentDim = level.dimension().location().toString();
        PortalConfig.PortalDefinition portal = config.getPortalForDimension(currentDim);

        if (portal == null || !portal.enabled) return;

        ResourceLocation targetDimRL = new ResourceLocation(portal.targetDimension);
        ResourceKey<Level> targetDimKey = ResourceKey.create(Registries.DIMENSION, targetDimRL);
        ServerLevel targetLevel = ((ServerLevel) level).getServer().getLevel(targetDimKey);
        if (targetLevel == null) return;

        PortalConfig.PortalDefinition targetPortal = config.getPortalBySourceAndTarget(
                portal.targetDimension, currentDim);
        if (targetPortal == null) return;

        Direction facing = getSide();
        boolean isCeiling = facing == Direction.DOWN;
        int baseY = portal.bedrockYLevel;
        int offset = isCeiling ? (baseY - worldPosition.getY()) : (worldPosition.getY() - baseY);

        int targetY;
        if (targetPortal.portalType == PortalConfig.PortalDefinition.PortalType.CEILING) {
            targetY = targetPortal.bedrockYLevel - offset;
        } else {
            targetY = targetPortal.bedrockYLevel + offset;
        }

        BlockPos targetPos = new BlockPos(worldPosition.getX(), targetY, worldPosition.getZ());

        if (targetLevel.isLoaded(targetPos)) {
            BlockEntity be = targetLevel.getBlockEntity(targetPos);
            if (be instanceof DimensionalPipeBlockEntity pipe) {
                linkedDimension = targetDimKey;
                linkedPos = targetPos;
                cachedLink = pipe;
                if (pipe.cachedLink != this) {
                    pipe.link(this);
                }
            }
        }
    }

    private BlockEntity getConnectedTile() {
        if (level == null) return null;
        return level.getBlockEntity(worldPosition.relative(getSide()));
    }

    private void forceLoadChunks() {
        if (chunksForceLoaded || level == null || level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        ChunkPos thisChunk = new ChunkPos(worldPosition);
        ForgeChunkManager.forceChunk(serverLevel, Stackeddimensions.MODID, worldPosition, thisChunk.x, thisChunk.z, true, true);

        if (linkedDimension != null && linkedPos != null) {
            ServerLevel targetLevel = serverLevel.getServer().getLevel(linkedDimension);
            if (targetLevel != null) {
                ChunkPos linkedChunk = new ChunkPos(linkedPos);
                ForgeChunkManager.forceChunk(targetLevel, Stackeddimensions.MODID, worldPosition, linkedChunk.x, linkedChunk.z, true, true);
            }
        }

        chunksForceLoaded = true;
    }

    private void unforceLoadChunks() {
        if (!chunksForceLoaded || level == null || level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        ChunkPos thisChunk = new ChunkPos(worldPosition);
        ForgeChunkManager.forceChunk(serverLevel, Stackeddimensions.MODID, worldPosition, thisChunk.x, thisChunk.z, false, true);

        if (linkedDimension != null && linkedPos != null) {
            ServerLevel targetLevel = serverLevel.getServer().getLevel(linkedDimension);
            if (targetLevel != null) {
                ChunkPos linkedChunk = new ChunkPos(linkedPos);
                ForgeChunkManager.forceChunk(targetLevel, Stackeddimensions.MODID, worldPosition, linkedChunk.x, linkedChunk.z, false, true);
            }
        }

        chunksForceLoaded = false;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        Direction facing = getSide();
        if (side != facing) {
            return super.getCapability(cap, side);
        }

        byte type = cap == ForgeCapabilities.ITEM_HANDLER ? ITEM :
                cap == ForgeCapabilities.FLUID_HANDLER ? FLUID : 0;

        DimensionalPipeBlockEntity linked = getLinkedPipe();
        if (linked != null && linked.level != null && !linked.isRemoved()) {
            BlockEntity adjacentBE = linked.getConnectedTile();
            if (adjacentBE != null) {
                LazyOptional<T> result = adjacentBE.getCapability(cap, linked.getSide().getOpposite());
                if (result.isPresent()) {
                    hasCap |= type;
                    forceLoadChunks();
                    return result;
                } else {
                    hasCap &= ~type;
                }
            } else {
                hasCap &= ~type;
            }
        }

        return super.getCapability(cap, side);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DimensionalPipeBlockEntity blockEntity) {
        if (blockEntity.needsLinkUpdate) {
            blockEntity.updateLink();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        needsLinkUpdate = true;
    }

    @Override
    public void setRemoved() {
        unforceLoadChunks();
        if (cachedLink != null && cachedLink.cachedLink == this) {
            cachedLink.cachedLink = null;
            cachedLink.needsLinkUpdate = true;
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (linkedDimension != null) {
            tag.putString("LinkedDim", linkedDimension.location().toString());
        }
        if (linkedPos != null) {
            tag.putInt("LinkedX", linkedPos.getX());
            tag.putInt("LinkedY", linkedPos.getY());
            tag.putInt("LinkedZ", linkedPos.getZ());
        }
        tag.putByte("HasCap", hasCap);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("LinkedDim")) {
            linkedDimension = ResourceKey.create(Registries.DIMENSION,
                    new ResourceLocation(tag.getString("LinkedDim")));
        }
        if (tag.contains("LinkedX")) {
            linkedPos = new BlockPos(
                    tag.getInt("LinkedX"),
                    tag.getInt("LinkedY"),
                    tag.getInt("LinkedZ"));
        }
        hasCap = tag.getByte("HasCap");
        needsLinkUpdate = true;
    }
}
