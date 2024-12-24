package foundry.mirror.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import foundry.mirror.MirrorMod;
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
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.NativeResource;

import java.lang.Math;
import java.nio.IntBuffer;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.glGenerateMipmap;

public class MirrorBlockEntityRenderer implements BlockEntityRenderer<MirrorBlockEntity> {

    private static final ResourceLocation MIRROR_RENDER_TYPE = MirrorMod.path("mirror");
    private static final ResourceLocation MIRROR_FBO = MirrorMod.path("mirror");
    private static final float RENDER_DISTANCE = 4.0F;
    private static final int MIPMAP_LEVELS = 4;
    private static final int MAX_LAYERS = 1;

    private static final Matrix4f RENDER_MODELVIEW = new Matrix4f();
    private static final Matrix4f RENDER_PROJECTION = new Matrix4f();
    private static final Matrix4f INVERSE_RENDER_PROJECTION = new Matrix4f();
    private static final Vector4f OBLIQUE_PLANE = new Vector4f();
    private static final Quaternionf VIEW = new Quaternionf();

    private static final ObjectSet<BlockPos> RENDER_POSITIONS = new ObjectArraySet<>();
    private static final Long2ObjectMap<MirrorTexture> TEXTURES = new Long2ObjectArrayMap<>();

    private static MirrorTexture renderMirror;
    private static BlockPos renderPos;
    private static int renderLayer;

    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;

    public MirrorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockEntityRenderDispatcher = context.getBlockEntityRenderDispatcher();
    }

    private static long getKey(BlockPos pos, Direction face) {
        Vec3i normal = face.getNormal();
        return (long) face.getAxis().ordinal() << 60 | ((long) pos.getX() * normal.getX() + (long) pos.getY() * normal.getY() + (long) pos.getZ() * normal.getZ());
    }

    @Override
    public void render(MirrorBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FramebufferManager framebufferManager = VeilRenderSystem.renderer().getFramebufferManager();
        AdvancedFbo fbo = framebufferManager.getFramebuffer(MIRROR_FBO);
        if (fbo == null) {
            return;
        }

        BlockPos pos = blockEntity.getBlockPos();
        Direction facing = blockEntity.getBlockState().getValue(MirrorBlock.FACING);

        boolean renderingPerspective = VeilLevelPerspectiveRenderer.isRenderingPerspective();
        MirrorTexture mirror = TEXTURES.computeIfAbsent(getKey(pos, facing), unused -> new MirrorTexture(new Vector3f(pos.getX(), pos.getY(), pos.getZ()), new Vector3f(facing.getStepX(), facing.getStepY(), facing.getStepZ())));
        if (mirror == renderMirror) {
            return;
        }

        mirror.positions.add(pos);
        if (!renderingPerspective) {
            if (!mirror.hasRendered(0)) {
                Camera camera = this.blockEntityRenderDispatcher.camera;
                Vec3 cameraPos = camera.getPosition();
                Vector3f up = camera.getUpVector();
                Vector3f look = camera.getLookVector();
                renderLayer = 1;
                renderMirror(mirror, 0, cameraPos.x, cameraPos.y, cameraPos.z, up, look, true, false);
                renderLayer = 0;
                mirror.setRendered(0);
            }
        } else if (renderLayer < MAX_LAYERS) {
            renderMirror.visibleMirrors.add(mirror);
        }

        RENDER_POSITIONS.add(pos.immutable());
        RenderType renderType = VeilRenderType.get(MIRROR_RENDER_TYPE);
        if (renderType == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YN.rotationDegrees(facing.toYRot()));
        poseStack.translate(-0.5, -0.5, -0.5);

        Vector3f normal = mirror.normal;
        Matrix4f pose = poseStack.last().pose();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
        builder.addVertex(pose, 0, 0, 0.125F).setUv(1.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 0.7F).setNormal(normal.x, normal.y, normal.z);
        builder.addVertex(pose, 1, 0, 0.125F).setUv(0.0F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 0.7F).setNormal(normal.x, normal.y, normal.z);
        builder.addVertex(pose, 1, 1, 0.125F).setUv(0.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, 0.7F).setNormal(normal.x, normal.y, normal.z);
        builder.addVertex(pose, 0, 1, 0.125F).setUv(1.0F, 1.0F).setColor(1.0F, 1.0F, 1.0F, 0.7F).setNormal(normal.x, normal.y, normal.z);

        RenderSystem.setShaderColor(0.9F, 0.9F, 0.9F, 1.0F);
        RenderSystem.setShaderTexture(0, mirror.textureIds[renderLayer]);
        renderType.setupRenderState();
        BufferUploader.drawWithShader(builder.buildOrThrow());
        renderType.clearRenderState();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }

    private static void renderMirror(MirrorTexture mirror, int layer, double x, double y, double z, Vector3fc up, Vector3fc dir, boolean render, boolean recurse) {
        AdvancedFbo fbo = VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(MIRROR_FBO);
        if (fbo == null) {
            return;
        }

        Vector3fc pos = mirror.pos;
        Vector3fc normal = mirror.normal;
        Minecraft client = Minecraft.getInstance();

        Vector3d renderPos = new Vector3d(pos.x() + 0.5 - normal.x() * 0.36, pos.y() + 0.5 - normal.y() * 0.36, pos.z() + 0.5 - normal.z() * 0.36);
        Vector3f offset = new Vector3f((float) (x - renderPos.x), (float) (y - renderPos.y), (float) (z - renderPos.z));
        Vector4f plane = new Vector4f(normal.x(), normal.y(), normal.z(), -offset.dot(normal.x(), normal.y(), normal.z()));

        Window window = client.getWindow();
        float aspect = (float) window.getWidth() / window.getHeight();
        float fov = RenderSystem.getProjectionMatrix().perspectiveFov();
        RENDER_PROJECTION.setPerspective(fov, aspect, 0.3F, RENDER_DISTANCE * 64);
        RENDER_PROJECTION.mul(applyInverseBob());

        offset.reflect(normal);
        renderPos.add(offset);
        Vector3f mirrorDir = dir.reflect(normal, new Vector3f());
        Vector3f mirrorUp = up.reflect(normal, new Vector3f());

        Quaternionf look = VIEW.identity().lookAlong(mirrorDir, mirrorUp);
        look.transform(plane);

        calculateObliqueMatrix(plane);

        if (render) {
            renderMirror = mirror;
            VeilLevelPerspectiveRenderer.render(fbo, RENDER_MODELVIEW, RENDER_PROJECTION, renderPos, look, RENDER_DISTANCE, client.getTimer());
            renderMirror = null;
            mirror.copy(fbo, layer);
//            Veil.withImGui(() -> {
//                if (ImGui.begin("Mirror Test")) {
//                    if (ImGui.beginChild("tex")) {
//                        ImGui.text("Mirror Layer: " + layer);
//                        float ratio = (float) fbo.getHeight() / fbo.getWidth();
//                        float width = ImGui.getContentRegionAvailX();
//                        int tex = VeilImGuiUtil.renderArea((int) width, (int) (width * ratio), fbo::resolveToAdvancedFbo);
//                        ImGui.image(tex, (int) width, (int) (width * ratio), 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.1F);
//                    }
//                    ImGui.endChild();
//                }
//                ImGui.end();
//            });
//            if (layer == 0) {
//                AdvancedFbo main = AdvancedFbo.getMainFramebuffer();
//                main.bind(false);
//                main.clear();
//                fbo.resolveToAdvancedFbo(main);
//            }
        }

        if (!recurse || layer + 1 >= MAX_LAYERS) {
            return;
        }
        for (MirrorTexture child : mirror.visibleMirrors) {
            renderMirror(child, layer + 1, renderPos.x, renderPos.y, renderPos.z, mirrorUp, mirrorDir, true, false);
        }
    }

    private static Matrix4fc applyInverseBob() {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        GameRenderer gameRenderer = minecraft.gameRenderer;
        GameRendererAccessor accessor = (GameRendererAccessor) gameRenderer;

        PoseStack poseStack = new PoseStack();
        Matrix4f matrix4f = poseStack.last().pose();
        matrix4f.scale(-1, 1, 1);
        accessor.invokeBobHurt(poseStack, partialTicks);
        if (minecraft.options.bobView().get()) {
            accessor.invokeBobView(poseStack, partialTicks);
        }

        float h = minecraft.options.screenEffectScale().get().floatValue();
        float i = Mth.lerp(partialTicks, minecraft.player.oSpinningEffectIntensity, minecraft.player.spinningEffectIntensity) * h * h;
        if (i > 0.0F) {
            int j = minecraft.player.hasEffect(MobEffects.CONFUSION) ? 7 : 20;
            float k = 5.0F / (i * i + 5.0F) - i * 0.04F;
            k *= k;
            Vector3f vector3f = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
            float l = ((float) accessor.getConfusionAnimationTick() + partialTicks) * (float) j * (float) (Math.PI / 180.0);
            matrix4f.rotate(l, vector3f);
            matrix4f.scale(1.0F / k, 1.0F, 1.0F);
            matrix4f.rotate(-l, vector3f);
        }
        matrix4f.scale(-1, 1, 1);

        return matrix4f;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }

    @Override
    public boolean shouldRender(MirrorBlockEntity blockEntity, Vec3 vec3) {
        if (renderLayer >= MAX_LAYERS) {
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

    private static void calculateObliqueMatrix(Vector4fc c) {
        Vector4f q = RENDER_PROJECTION.invert(INVERSE_RENDER_PROJECTION).transform(
                Math.signum(c.x()),
                Math.signum(c.y()),
                1.0f,
                1.0f,
                OBLIQUE_PLANE);
        float dot = c.dot(q);
        RENDER_PROJECTION.m02(c.x() * 2.0F / dot - RENDER_PROJECTION.m03()).m12(c.y() * 2.0F / dot - RENDER_PROJECTION.m13()).m22(c.z() * 2.0F / dot - RENDER_PROJECTION.m23()).m32(c.w() * 2.0F / dot - RENDER_PROJECTION.m33());
    }

    public static void levelRenderEnd() {
        if (VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
            return;
        }

        Camera camera = Minecraft.getInstance().getBlockEntityRenderDispatcher().camera;
        Vec3 cameraPos = camera.getPosition();
        Vector3f up = camera.getUpVector();
        Vector3f look = camera.getLookVector();

        for (MirrorTexture mirror : TEXTURES.values()) {
            renderMirror(mirror, 0, cameraPos.x, cameraPos.y, cameraPos.z, up, look, false, true);
            mirror.reset();
        }

        renderLayer = 0;
    }

    public static void endClientTick() {
        ObjectIterator<Long2ObjectMap.Entry<MirrorTexture>> iterator = TEXTURES.long2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, MirrorTexture> entry = iterator.next();
            MirrorTexture mirror = entry.getValue();
            if (mirror.positions.removeIf(p -> !RENDER_POSITIONS.contains(p)) && mirror.positions.isEmpty()) {
                iterator.remove();
                mirror.free();
            }
        }
    }

    public static void free() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ObjectCollection<MirrorTexture> values = TEXTURES.values();
            IntBuffer textures = stack.mallocInt(values.size() * MAX_LAYERS);
            values.forEach(texture -> textures.put(texture.textureIds));
            textures.flip();
            glDeleteTextures(textures);
        }
        TEXTURES.clear();
    }

    public static boolean isRenderingMirror() {
        return renderMirror != null;
    }

    private static class MirrorTexture implements NativeResource {

        private final ObjectSet<BlockPos> positions;
        private final BitSet renderedLayers;
        private final int[] textureIds;
        private final Vector3f pos;
        private final Vector3f normal;
        private final Set<MirrorTexture> visibleMirrors;

        private int width;
        private int height;

        private MirrorTexture(Vector3f pos, Vector3f normal) {
            this.positions = new ObjectArraySet<>();
            this.renderedLayers = new BitSet(MAX_LAYERS);
            this.textureIds = new int[MAX_LAYERS];
            this.pos = pos;
            this.normal = normal;
            this.visibleMirrors = new HashSet<>();
            this.width = -1;
            this.height = -1;

            glGenTextures(this.textureIds);
            for (int i = 0; i < MAX_LAYERS; i++) {
                RenderSystem.bindTexture(this.textureIds[i]);
                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR);
                GlStateManager._texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            }
        }

        public void copy(AdvancedFbo fbo, int layer) {
            int width = fbo.getWidth();
            int height = fbo.getHeight();
            if (this.width != width || this.height != height) {
                this.width = width;
                this.height = height;
                for (int i = 0; i < MAX_LAYERS; i++) {
                    TextureUtil.prepareImage(NativeImage.InternalGlFormat.RGBA, this.textureIds[i], MIPMAP_LEVELS, width, height);
                }
            }

            fbo.bindRead();
            RenderSystem.bindTexture(this.textureIds[layer]);
            glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
            AdvancedFbo.unbind();
            glGenerateMipmap(GL_TEXTURE_2D);
        }

        @Override
        public void free() {
            glDeleteTextures(this.textureIds);
        }

        public boolean hasRendered(int layer) {
            return this.renderedLayers.get(layer);
        }

        public void reset() {
            this.renderedLayers.clear();
            this.visibleMirrors.clear();
        }

        public void setRendered(int layer) {
            this.renderedLayers.set(layer);
        }
    }
}
