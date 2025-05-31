package foundry.mirror.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import foundry.mirror.MirrorMod;
import foundry.mirror.mixin.client.GameRendererAccessor;
import foundry.veil.Veil;
import foundry.veil.api.client.imgui.VeilImGuiUtil;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import foundry.veil.api.compat.IrisCompat;
import imgui.ImGui;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.system.NativeResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Math;
import java.util.Map;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.glGenerateMipmap;

public final class MirrorRenderer {

    public static final float RENDER_DISTANCE = 4.0F;
    public static final int MIPMAP_LEVELS = 4;

    public static final ResourceLocation MIRROR_RENDER_TYPE = MirrorMod.path("mirror");
    public static final ResourceLocation SCREEN_SPACE_MIRROR_RENDER_TYPE = MirrorMod.path("mirror_screenspace");
    public static final ResourceLocation MIRROR_FBO = MirrorMod.path("mirror");

    private static final Logger LOGGER = LoggerFactory.getLogger(MirrorRenderer.class);

    private static final Matrix4f RENDER_MODELVIEW = new Matrix4f();
    private static final Matrix4f RENDER_PROJECTION = new Matrix4f();
    private static final Matrix4f RENDER_OBLIQUE_PROJECTION = new Matrix4f();
    private static final Matrix4f INVERSE_RENDER_PROJECTION = new Matrix4f();
    private static final Vector4f OBLIQUE_PLANE = new Vector4f();
    private static final Quaternionf VIEW = new Quaternionf();

    private static final Long2ObjectMap<MirrorTexture> TEXTURES = new Long2ObjectLinkedOpenHashMap<>();
    private static final Quaternionf CAMERA_ORIENTATION = new Quaternionf();

    private static MirrorTexture renderMirror;

    private MirrorRenderer() {
    }

    private static long getKey(final BlockPos pos, final Direction face, final float mirrorOffset) {
        final Vec3i normal = face.getNormal();
        return (long) face.getAxis().ordinal() << 62 | (long) (mirrorOffset * 16.0) << 57 | (0x1FFFFFFFFFFFFFFL & (((long) pos.getX() * normal.getX() + (long) pos.getY() * normal.getY() + (long) pos.getZ() * normal.getZ())));
    }

    public static MirrorTexture getTexture(final BlockPos pos, final Direction facing, final float mirrorOffset) {
        return TEXTURES.computeIfAbsent(getKey(pos, facing, mirrorOffset), unused -> new MirrorTexture(new Vector3f(pos.getX(), pos.getY(), pos.getZ()), new Vector3f(facing.getStepX(), facing.getStepY(), facing.getStepZ()), mirrorOffset));
    }

    public static MirrorTexture getTexture(final boolean leftHand) {
        return TEXTURES.computeIfAbsent(3L << 62 | (leftHand ? 1 : 0), unused -> new MirrorTexture());
    }

    private static void renderMirror(final MirrorTexture mirror, final double cameraX, final double cameraY, final double cameraZ, final Vector3fc up, final Vector3fc dir, final float renderDistance, final DeltaTracker deltaTracker) {
        final AdvancedFbo fbo = VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(MIRROR_FBO);
        if (fbo == null) {
            return;
        }

        final float mirrorOffset = mirror.mirrorOffset;
        final Vector3fc mirrorPos = mirror.pos;
        final Vector3fc mirrorNormal = mirror.normal;

        final Vector3d renderPos = new Vector3d(mirrorPos.x() + 0.5 - mirrorNormal.x() * (0.5 - mirrorOffset - 0.01), mirrorPos.y() + 0.5 - mirrorNormal.y() * (0.5 - mirrorOffset - 0.01), mirrorPos.z() + 0.5 - mirrorNormal.z() * (0.5 - mirrorOffset - 0.01));
        final Vector3f offset = new Vector3f((float) (cameraX - renderPos.x), (float) (cameraY - renderPos.y), (float) (cameraZ - renderPos.z));

        final float aspect = (float) fbo.getWidth() / fbo.getHeight();
        final float fov = RenderSystem.getProjectionMatrix().perspectiveFov();
        RENDER_PROJECTION.setPerspective(fov, aspect, 0.3F, renderDistance * 64);
        RENDER_PROJECTION.mul(applyInverseBob());

        offset.reflect(mirrorNormal);
        renderPos.add(offset);
        final Vector3f mirrorDir = dir.reflect(mirrorNormal, new Vector3f());
        final Vector3f mirrorUp = up.reflect(mirrorNormal, new Vector3f());

        final Quaternionf look = VIEW.identity().lookAlong(mirrorDir, mirrorUp);
        final Vector4f plane = new Vector4f(mirrorNormal.x(), mirrorNormal.y(), mirrorNormal.z(), offset.dot(mirrorNormal.x(), mirrorNormal.y(), mirrorNormal.z()));
        look.transform(plane);
        calculateObliqueMatrix(plane);

        renderMirror = mirror;

        final Quaternionf rotation = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
        CAMERA_ORIENTATION.set(rotation.x, -rotation.y, -rotation.z, rotation.w);
        if (mirrorNormal.maxComponent() == 1) {
            CAMERA_ORIENTATION.rotateLocalZ((float) Math.PI);
        } else if (mirrorNormal.maxComponent() == 2) {
            CAMERA_ORIENTATION.rotateLocalY((float) Math.PI);
        }

        try {
            VeilLevelPerspectiveRenderer.render(fbo, null, RENDER_MODELVIEW, RENDER_OBLIQUE_PROJECTION, renderPos, look, renderDistance, deltaTracker, false);
        } catch (final ReportedException e) {
            // Let crash reports go through
            throw e;
        } catch (final Throwable t) {
            LOGGER.error("Error drawing mirror at: {}", mirror.pos, t);
        }
        renderMirror = null;
        mirror.copy(fbo);
        Veil.withImGui(() -> {
            if (ImGui.begin("Mirror Test")) {
                if (ImGui.beginChild("tex")) {
                    final float ratio = (float) fbo.getHeight() / fbo.getWidth();
                    final float width = ImGui.getContentRegionAvailX();
                    final int tex = VeilImGuiUtil.renderArea((int) width, (int) (width * ratio), fbo::resolveToAdvancedFbo);
                    ImGui.image(tex, (int) width, (int) (width * ratio), 0, 1, 1, 0, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.1F);
                }
                ImGui.endChild();
            }
            ImGui.end();
        });
//        final AdvancedFbo main = AdvancedFbo.getMainFramebuffer();
//        main.bind(false);
//        main.clear();
//        fbo.resolveToAdvancedFbo(main);
    }

    private static Matrix4fc applyInverseBob() {
        final Minecraft minecraft = Minecraft.getInstance();
        final float partialTicks = minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        final GameRenderer gameRenderer = minecraft.gameRenderer;
        final GameRendererAccessor accessor = (GameRendererAccessor) gameRenderer;

        final PoseStack poseStack = new PoseStack();
        final Matrix4f matrix4f = poseStack.last().pose();
        matrix4f.scale(-1, 1, 1);
        accessor.invokeBobHurt(poseStack, partialTicks);
        if (minecraft.options.bobView().get()) {
            accessor.invokeBobView(poseStack, partialTicks);
        }

        final float h = minecraft.options.screenEffectScale().get().floatValue();
        final float i = Mth.lerp(partialTicks, minecraft.player.oSpinningEffectIntensity, minecraft.player.spinningEffectIntensity) * h * h;
        if (i > 0.0F) {
            final int j = minecraft.player.hasEffect(MobEffects.CONFUSION) ? 7 : 20;
            float k = 5.0F / (i * i + 5.0F) - i * 0.04F;
            k *= k;
            final Vector3f vector3f = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
            final float l = ((float) accessor.getConfusionAnimationTick() + partialTicks) * (float) j * (float) (Math.PI / 180.0);
            matrix4f.rotate(l, vector3f);
            matrix4f.scale(1.0F / k, 1.0F, 1.0F);
            matrix4f.rotate(-l, vector3f);
        }
        matrix4f.scale(-1, 1, 1);

        return matrix4f;
    }

    private static void calculateObliqueMatrix(final Vector4fc c) {
        final Vector4f q = RENDER_PROJECTION.invert(INVERSE_RENDER_PROJECTION).transform(
                Math.signum(c.x()),
                Math.signum(c.y()),
                1.0f,
                1.0f,
                OBLIQUE_PLANE);
        final float dot = c.dot(q);
        RENDER_OBLIQUE_PROJECTION.set(RENDER_PROJECTION);
        RENDER_OBLIQUE_PROJECTION.m02(c.x() * 2.0F / dot - RENDER_PROJECTION.m03()).m12(c.y() * 2.0F / dot - RENDER_PROJECTION.m13()).m22(c.z() * 2.0F / dot - RENDER_PROJECTION.m23()).m32(c.w() * 2.0F / dot - RENDER_PROJECTION.m33());
    }

    /**
     * Renders all mirrors visible last frame.
     */
    public static void renderMirrors(final Camera camera, final DeltaTracker deltaTracker) {
        if (IrisCompat.isLoaded() && IrisCompat.INSTANCE.areShadersLoaded()) {
            return;
        }

        final Vec3 cameraPos = camera.getPosition();
        final Vector3f up = camera.getUpVector();
        final Vector3f look = camera.getLookVector();

        for (final MirrorTexture texture : TEXTURES.values()) {
            if (!texture.isScreenSpace()) {
                renderMirror(texture, cameraPos.x, cameraPos.y, cameraPos.z, up, look, MirrorRenderer.RENDER_DISTANCE, deltaTracker);
            }
        }
    }

    /**
     * Deletes unused mirror textures this frame
     */
    public static void freeMirrors() {
        if (isRenderingMirror()) {
            return;
        }

        final ObjectIterator<Long2ObjectMap.Entry<MirrorTexture>> iterator = TEXTURES.long2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Long, MirrorTexture> entry = iterator.next();
            final MirrorTexture mirror = entry.getValue();
            if (mirror.positions.isEmpty()) {
                iterator.remove();
                mirror.free();
                continue;
            }

            mirror.reset();
        }
    }

    public static void free() {
        for (final MirrorTexture mirror : TEXTURES.values()) {
            mirror.free();
        }
        TEXTURES.clear();
    }

    public static Matrix4f getRenderProjection() {
        return RENDER_PROJECTION;
    }

    public static @Nullable MirrorTexture getRenderMirror() {
        return renderMirror;
    }

    public static boolean isRenderingMirror() {
        return renderMirror != null;
    }

    public static Quaternionfc getCameraOrientation() {
        return CAMERA_ORIENTATION;
    }

    public static class MirrorTexture implements NativeResource {

        private final ObjectSet<BlockPos> positions;
        private final TextureWrapper texture;
        private final Vector3f pos;
        private final Vector3f normal;
        private final float mirrorOffset;

        private int width;
        private int height;
        private boolean screenSpace;

        private MirrorTexture() {
            this.positions = new ObjectArraySet<>();
            this.pos = null;
            this.normal = null;
            this.mirrorOffset = 0;
            this.width = -1;
            this.height = -1;
            this.screenSpace = false;

            this.texture = new TextureWrapper(glGenTextures());
            this.texture.setFilter(false, true);
            Minecraft.getInstance().getTextureManager().register(this.texture.getName(), this.texture);
        }

        private MirrorTexture(final Vector3f pos, final Vector3f normal, final float mirrorOffset) {
            this.positions = new ObjectArraySet<>();
            this.pos = pos;
            this.normal = normal;
            this.mirrorOffset = mirrorOffset;
            this.width = -1;
            this.height = -1;

            this.texture = new TextureWrapper(VeilRenderSystem.createTextures(GL_TEXTURE_2D));
            this.texture.setFilter(false, true);
            Minecraft.getInstance().getTextureManager().register(this.texture.getName(), this.texture);
        }

        public void copy(final AdvancedFbo fbo) {
            final int width = fbo.getWidth();
            final int height = fbo.getHeight();
            if (this.width != width || this.height != height) {
                this.width = width;
                this.height = height;
                TextureUtil.prepareImage(NativeImage.InternalGlFormat.RGBA, this.texture.getId(), MIPMAP_LEVELS, width, height);
            }

            fbo.bindRead();
            this.texture.bind();
            glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
            AdvancedFbo.unbind();
            glGenerateMipmap(GL_TEXTURE_2D);
        }

        @Override
        public void free() {
            Minecraft.getInstance().getTextureManager().release(this.texture.getName());
        }

        public ResourceLocation getTexture() {
            return this.texture.getName();
        }

        public Vector3fc getPos() {
            return this.pos;
        }

        public Vector3fc getNormal() {
            return this.normal;
        }

        public float getMirrorOffset() {
            return this.mirrorOffset;
        }

        public @Nullable RenderType getRenderType() {
            // TODO screen space
            if (false && this.screenSpace) {
                return VeilRenderType.get(SCREEN_SPACE_MIRROR_RENDER_TYPE);
            } else {
                return VeilRenderType.get(MIRROR_RENDER_TYPE, this.texture.name);
            }
        }

        public void setRenderedPos(final BlockPos pos, final boolean screenSpace) {
            this.positions.add(pos);
            this.screenSpace |= screenSpace;
        }

        public void reset() {
            this.positions.clear();
            this.screenSpace = false;
        }

        public boolean isScreenSpace() {
            return this.screenSpace;
        }
    }

    private static class TextureWrapper extends AbstractTexture {

        private final ResourceLocation name;
        private final int id;

        private TextureWrapper(final int id) {
            this.name = MirrorMod.path("mirror/dynamic_" + id);
            this.id = id;
        }

        public ResourceLocation getName() {
            return this.name;
        }

        @Override
        public int getId() {
            return this.id;
        }

        @Override
        public void releaseId() {
            glDeleteTextures(this.id);
        }

        @Override
        public void load(final ResourceManager resourceManager) {
        }
    }
}
