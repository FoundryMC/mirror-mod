package foundry.mirror.blockentity;

import foundry.mirror.registry.MirrorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MirrorBlockEntity extends BlockEntity {

    public MirrorBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(MirrorBlocks.MIRROR_BE.get(), blockPos, blockState);
    }
}
