package foundry.mirror.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import foundry.mirror.block.AbstractMirrorBlock;
import foundry.mirror.blockentity.MirrorBlockEntity;
import foundry.mirror.registry.MirrorBlocks;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3fc;

public class MirrorBlockEntityRenderer implements BlockEntityRenderer<MirrorBlockEntity> {

    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    public MirrorBlockEntityRenderer(final BlockEntityRendererProvider.Context context) {
        this.blockEntityRenderDispatcher = context.getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(final MirrorBlockEntity blockEntity, final float partialTick, final PoseStack poseStack, final MultiBufferSource bufferSource, final int packedLight, final int packedOverlay) {
        final BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof final AbstractMirrorBlock mirrorBlock)) {
            return;
        }

        final BlockPos pos = blockEntity.getBlockPos();
        final Camera camera = this.blockEntityRenderDispatcher.camera;
        final Vec3 cameraPos = camera.getPosition();
        final double distance = Math.sqrt(cameraPos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        final int viewDistance = this.getViewDistance();

        if (state.is(MirrorBlocks.MIRROR_PANE.get())) {
            final float mirrorOffset = mirrorBlock.getMirrorOffset(state);

            final Direction facing;
            if (state.hasProperty(BlockStateProperties.FACING)) {
                facing = state.getValue(BlockStateProperties.FACING);
            } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            } else {
                facing = Direction.NORTH;
            }

            final MirrorRenderer.MirrorTexture mirror = MirrorRenderer.getTexture(pos, facing, mirrorOffset);
            mirror.setRenderedPos(pos, true);

            final RenderType renderType = mirror.getRenderType();
            if (renderType == null) {
                return;
            }

            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YN.rotationDegrees(facing.toYRot()));
            poseStack.translate(-0.5, -0.5, -0.5);

            final float alpha = (float) (1.0 - Mth.clamp((distance - viewDistance + 8) / 8.0, 0.0, 1.0)) * 0.7F;

            final Vector3fc normal = mirror.getNormal();
            final Matrix4f pose = poseStack.last().pose();
            final VertexConsumer builder = bufferSource.getBuffer(renderType);
            builder.addVertex(pose, 0, 0, mirrorOffset).setUv(1.0F, 0.0F).setColor(0.9F, 0.9F, 0.9F, alpha).setNormal(normal.x(), normal.y(), normal.z());
            builder.addVertex(pose, 1, 0, mirrorOffset).setUv(0.0F, 0.0F).setColor(0.9F, 0.9F, 0.9F, alpha).setNormal(normal.x(), normal.y(), normal.z());
            builder.addVertex(pose, 1, 1, mirrorOffset).setUv(0.0F, 1.0F).setColor(0.9F, 0.9F, 0.9F, alpha).setNormal(normal.x(), normal.y(), normal.z());
            builder.addVertex(pose, 0, 1, mirrorOffset).setUv(1.0F, 1.0F).setColor(0.9F, 0.9F, 0.9F, alpha).setNormal(normal.x(), normal.y(), normal.z());

            poseStack.popPose();
        } else if (state.is(MirrorBlocks.MIRROR.get())) {
            final BlockPos.MutableBlockPos offset = new BlockPos.MutableBlockPos();
            final Level level = blockEntity.getLevel();

            for (final Direction facing : Direction.values()) {
                if (level != null && !Block.shouldRenderFace(state, level, pos, facing, offset.setWithOffset(pos, facing))) {
                    continue;
                }

                final float dot = dot(pos, facing.getOpposite().getNormal(), cameraPos.x, cameraPos.y, cameraPos.z);
                if (dot <= 0) {
                    continue;
                }

                final MirrorRenderer.MirrorTexture mirror = MirrorRenderer.getTexture(pos, facing, 1);
                mirror.setRenderedPos(pos, true);

                final RenderType renderType = mirror.getRenderType();
                if (renderType == null) {
                    return;
                }

                poseStack.pushPose();
                poseStack.translate(0.5, 0.5, 0.5);
                poseStack.mulPose(facing.getRotation());
                poseStack.translate(-0.5, -0.5, -0.5);

                final float alpha = (float) (1.0 - Mth.clamp((distance - viewDistance + 8) / 8.0, 0.0, 1.0)) * 0.7F;

                final Vector3fc normal = mirror.getNormal();
                final Matrix4f pose = poseStack.last().pose();
                final VertexConsumer builder = bufferSource.getBuffer(renderType);
                builder.addVertex(pose, 0, 1, 0).setUv(1.0F, 0.0F).setColor(0.9F, 0.9F, 0.9F, alpha).setNormal(normal.x(), normal.y(), normal.z());
                builder.addVertex(pose, 0, 1, 1).setUv(0.0F, 0.0F).setColor(0.9F, 0.9F, 0.9F, alpha).setNormal(normal.x(), normal.y(), normal.z());
                builder.addVertex(pose, 1, 1, 1).setUv(0.0F, 1.0F).setColor(0.9F, 0.9F, 0.9F, alpha).setNormal(normal.x(), normal.y(), normal.z());
                builder.addVertex(pose, 1, 1, 0).setUv(1.0F, 1.0F).setColor(0.9F, 0.9F, 0.9F, alpha).setNormal(normal.x(), normal.y(), normal.z());

                poseStack.popPose();
            }
        }
    }

    @Override
    public int getViewDistance() {
        return 22;
    }

    @Override
    public boolean shouldRender(final MirrorBlockEntity blockEntity, final Vec3 cameraPos) {
        if (VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
            return false;
        }

        final BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof final AbstractMirrorBlock mirrorBlock)) {
            return false;
        }

        final BlockPos pos = blockEntity.getBlockPos();
        final int viewDistance = this.getViewDistance();
        if (cameraPos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) >= viewDistance * viewDistance) {
            return false;
        }

        final Direction facing;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            facing = state.getValue(BlockStateProperties.FACING);
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else {
            facing = null;
        }

        if (facing != null) {
            final Vec3i normal = facing.getNormal();
            if (dot(pos, normal, cameraPos.x, cameraPos.y, cameraPos.z) >= 0) {
                return false;
            }
        }

        final AABB box = mirrorBlock.getBoundingBox(state);
        return VeilRenderSystem.getCullingFrustum().testAab(
                pos.getX() + box.minX,
                pos.getY() + box.minY,
                pos.getZ() + box.minZ,
                pos.getX() + box.maxX,
                pos.getY() + box.maxY,
                pos.getZ() + box.maxZ);
    }

    private static float dot(final BlockPos pos, final Vec3i normal, final double x, final double y, final double z) {
        final float dx = (float) (pos.getX() + 0.5 - normal.getX() * 0.5 - x);
        final float dy = (float) (pos.getY() + 0.5 - normal.getY() * 0.5 - y);
        final float dz = (float) (pos.getZ() + 0.5 - normal.getZ() * 0.5 - z);
        final float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return (dx * normal.getX() + dy * normal.getY() + dz * normal.getZ()) / length;
    }
}
