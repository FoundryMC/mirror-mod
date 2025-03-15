package foundry.mirror.block;

import foundry.mirror.blockentity.MirrorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public abstract class MirrorBlock extends Block implements EntityBlock {

    public MirrorBlock(Properties properties) {
        super(properties);
    }

    public abstract AABB getBoundingBox(BlockState state);

    public abstract float getMirrorOffset(BlockState state);

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MirrorBlockEntity(pos, state);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos) {
        return blockState.getFluidState().isEmpty();
    }
}
