package foundry.mirror.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import foundry.mirror.block.MirrorBlock;
import foundry.mirror.blockentity.MirrorBlockEntity;
import foundry.mirror.mixin.client.GameRendererAccessor;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.VeilRenderer;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.FramebufferManager;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class MirrorBlockEntityRenderer implements BlockEntityRenderer<MirrorBlockEntity> {

    public static final float RENDER_DISTANCE = 4.0F;

    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    public MirrorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockEntityRenderDispatcher = context.getBlockEntityRenderDispatcher();
    }

    @Override
    public void render(MirrorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FramebufferManager framebufferManager = VeilRenderSystem.renderer().getFramebufferManager();
        AdvancedFbo fbo = framebufferManager.getFramebuffer(MirrorRenderer.MIRROR_FBO);
        if (fbo == null) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        Direction facing = blockEntity.getBlockState().getValue(MirrorBlock.FACING);

        MirrorRenderer.MirrorTexture mirror = MirrorRenderer.getTexture(pos, facing);
        MirrorRenderer.MirrorTexture renderMirror = MirrorRenderer.getRenderMirror();
        if (mirror == renderMirror) {
            return;
        }

        mirror.setRenderedPos(pos.immutable());
        if (!VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
            if (!mirror.hasRendered(0)) {
                Camera camera = this.blockEntityRenderDispatcher.camera;
                Vec3 cameraPos = camera.getPosition();
                Vector3f up = camera.getUpVector();
                Vector3f look = camera.getLookVector();
                MirrorRenderer.renderMirror(mirror, 0, mirror.getPos(), mirror.getNormal(), cameraPos.x, cameraPos.y, cameraPos.z, up, look, RENDER_DISTANCE, true, false);
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

        Vector3fc normal = mirror.getNormal();
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer builder = bufferSource.getBuffer(renderType);
        builder.addVertex(pose, 0, 0, 0.125F).setUv(1.0F, 0.0F).setColor(0.9F, 0.9F, 0.9F, 0.7F).setNormal(normal.x(), normal.y(), normal.z());
        builder.addVertex(pose, 1, 0, 0.125F).setUv(0.0F, 0.0F).setColor(0.9F, 0.9F, 0.9F, 0.7F).setNormal(normal.x(), normal.y(), normal.z());
        builder.addVertex(pose, 1, 1, 0.125F).setUv(0.0F, 1.0F).setColor(0.9F, 0.9F, 0.9F, 0.7F).setNormal(normal.x(), normal.y(), normal.z());
        builder.addVertex(pose, 0, 1, 0.125F).setUv(1.0F, 1.0F).setColor(0.9F, 0.9F, 0.9F, 0.7F).setNormal(normal.x(), normal.y(), normal.z());

        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public boolean shouldRender(MirrorBlockEntity blockEntity, Vec3 vec3) {
        if (MirrorRenderer.getRenderLayer() >= MirrorRenderer.MAX_LAYERS) {
            return false;
        }

        BlockPos pos = blockEntity.getBlockPos();
        int viewDistance = this.getViewDistance();
        if (vec3.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) >= viewDistance * viewDistance) {
            return false;
        }

        CullFrustum frustum = VeilRenderer.getCullingFrustum();
        Direction facing = blockEntity.getBlockState().getValue(MirrorBlock.FACING);
        Vec3i normal = facing.getNormal();
        if (dot(pos, normal, vec3.x, vec3.y, vec3.z) >= 0) {
            return false;
        }

        AABB box = MirrorBlock.BOUNDING_BOXES[facing.get2DDataValue()];
        return frustum.testAab(
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
