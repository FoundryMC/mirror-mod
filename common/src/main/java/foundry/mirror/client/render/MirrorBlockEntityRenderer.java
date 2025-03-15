package foundry.mirror.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import foundry.mirror.block.MirrorBlock;
import foundry.mirror.blockentity.MirrorBlockEntity;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class MirrorBlockEntityRenderer implements BlockEntityRenderer<MirrorBlockEntity> {

    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    public MirrorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockEntityRenderDispatcher = context.getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(MirrorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof MirrorBlock mirrorBlock)) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        float mirrorOffset = mirrorBlock.getMirrorOffset(state);

        Direction facing;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            facing = state.getValue(BlockStateProperties.FACING);
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        } else {
            facing = Direction.NORTH;
        }

        MirrorRenderer.MirrorTexture mirror = MirrorRenderer.getTexture(pos, facing, mirrorOffset);
        MirrorRenderer.MirrorTexture renderMirror = MirrorRenderer.getRenderMirror();
        if (mirror == renderMirror) {
            return;
        }

        Camera camera = this.blockEntityRenderDispatcher.camera;
        Vec3 cameraPos = camera.getPosition();
        double distance = Math.sqrt(cameraPos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        int viewDistance = this.getViewDistance();

        mirror.setRenderedPos(pos.immutable());
        if (!VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
            if (!mirror.hasRendered(0)) {
                Vector3f up = camera.getUpVector();
                Vector3f look = camera.getLookVector();
                int lod = (int) Mth.clamp(distance / viewDistance * MirrorRenderer.MAX_LOD, 0, MirrorRenderer.MAX_LOD);
                MirrorRenderer.renderMirror(mirror, lod, 0, mirrorOffset, mirror.getPos(), mirror.getNormal(), cameraPos.x, cameraPos.y, cameraPos.z, up, look, MirrorRenderer.RENDER_DISTANCE, true, false);
                mirror.setRendered(0);
            }
        } else if (MirrorRenderer.getRenderLayer() < MirrorRenderer.MAX_LAYERS && renderMirror != null) {
            renderMirror.addRecursive(mirror);
        }

        RenderType renderType = VeilRenderType.get(MirrorRenderer.MIRROR_RENDER_TYPE, mirror.getTexture(MirrorRenderer.getRenderLayer()));
        if (renderType == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YN.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5, -0.5, -0.5);

        float alpha = (float) (1.0 - Mth.clamp((distance - viewDistance + 8) / 8.0, 0.0, 1.0)) * 0.7F;

        Vector3fc normal = mirror.getNormal();
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer builder = bufferSource.getBuffer(renderType);
        builder.addVertex(pose, 0, 0, mirrorOffset).setUv(1.0F, 0.0F).setColor(0.9F, 0.9F, 0.9F, alpha).setNormal(normal.x(), normal.y(), normal.z());
        builder.addVertex(pose, 1, 0, mirrorOffset).setUv(0.0F, 0.0F).setColor(0.9F, 0.9F, 0.9F, alpha).setNormal(normal.x(), normal.y(), normal.z());
        builder.addVertex(pose, 1, 1, mirrorOffset).setUv(0.0F, 1.0F).setColor(0.9F, 0.9F, 0.9F, alpha).setNormal(normal.x(), normal.y(), normal.z());
        builder.addVertex(pose, 0, 1, mirrorOffset).setUv(1.0F, 1.0F).setColor(0.9F, 0.9F, 0.9F, alpha).setNormal(normal.x(), normal.y(), normal.z());

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 54;
    }

    @Override
    public boolean shouldRender(MirrorBlockEntity blockEntity, Vec3 vec3) {
        if (MirrorRenderer.getRenderLayer() >= MirrorRenderer.MAX_LAYERS) {
            return false;
        }

        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof MirrorBlock mirrorBlock)) {
            return false;
        }

        BlockPos pos = blockEntity.getBlockPos();
        int viewDistance = this.getViewDistance();
        if (vec3.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) >= viewDistance * viewDistance) {
            return false;
        }

        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction facing = state.getValue(BlockStateProperties.FACING);
            Vec3i normal = facing.getNormal();
            if (dot(pos, normal, vec3.x, vec3.y, vec3.z) >= 0) {
                return false;
            }
        }

        AABB box = mirrorBlock.getBoundingBox(state);
        return VeilRenderSystem.getCullingFrustum().testAab(
                pos.getX() + box.minX,
                pos.getY() + box.minY,
                pos.getZ() + box.minZ,
                pos.getX() + box.maxX,
                pos.getY() + box.maxY,
                pos.getZ() + box.maxZ);
    }

    private static float dot(BlockPos pos, Vec3i normal, double x, double y, double z) {
        return (float) ((pos.getX() + 0.5 - normal.getX() * 0.5 - x) * normal.getX() + (pos.getY() + 0.5 - normal.getY() * 0.5 - y) * normal.getY() + (pos.getZ() + 0.5 - normal.getZ() * 0.5 - z) * normal.getZ());
    }
}
