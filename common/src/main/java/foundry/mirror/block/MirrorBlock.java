package foundry.mirror.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;

public class MirrorBlock extends AbstractMirrorBlock{

    private static final AABB BOUNDING_BOX = Shapes.block().bounds();

    public MirrorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public AABB getBoundingBox(BlockState state) {
        return BOUNDING_BOX;
    }

    @Override
    public float getMirrorOffset(BlockState state) {
        return 0;
    }
}
