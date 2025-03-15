package foundry.mirror.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import foundry.mirror.MirrorMod;
import foundry.mirror.mixin.client.GameRendererAccessor;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
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

import java.lang.Math;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL30C.glGenerateMipmap;

public class MirrorRenderer {

    public static final float RENDER_DISTANCE = 4.0F;
    public static final int MAX_LOD = 2;
    public static final int MIPMAP_LEVELS = 4;
    public static final int MAX_LAYERS = 1;

    public static final ResourceLocation MIRROR_RENDER_TYPE = MirrorMod.path("mirror");
    public static final ResourceLocation[] MIRROR_FBO = IntStream.rangeClosed(0, MAX_LOD + 1)
            .mapToObj(i -> MirrorMod.path("mirror" + i))
            .toArray(ResourceLocation[]::new);

    private static final Matrix4f RENDER_MODELVIEW = new Matrix4f();
    private static final Matrix4f RENDER_PROJECTION = new Matrix4f();
    private static final Matrix4f RENDER_OBLIQUE_PROJECTION = new Matrix4f();
    private static final Matrix4f INVERSE_RENDER_PROJECTION = new Matrix4f();
    private static final Vector4f OBLIQUE_PLANE = new Vector4f();
    private static final Quaternionf VIEW = new Quaternionf();

    private static final ObjectSet<BlockPos> RENDER_POSITIONS = new ObjectArraySet<>();
    private static final Long2ObjectMap<MirrorTexture> TEXTURES = new Long2ObjectArrayMap<>();

    private static MirrorTexture renderMirror;
    private static int renderLayer;

    private static long getKey(BlockPos pos, Direction face, float mirrorOffset) {
        Vec3i normal = face.getNormal();
        return (long) face.getAxis().ordinal() << 62 | (long)(mirrorOffset * 16.0) << 57 | (0x1FFFFFFFFFFFFFFL & (((long) pos.getX() * normal.getX() + (long) pos.getY() * normal.getY() + (long) pos.getZ() * normal.getZ())));
    }

    public static MirrorTexture getTexture(BlockPos pos, Direction facing, float mirrorOffset) {
        return TEXTURES.computeIfAbsent(getKey(pos, facing, mirrorOffset), unused -> new MirrorTexture(new Vector3f(pos.getX(), pos.getY(), pos.getZ()), new Vector3f(facing.getStepX(), facing.getStepY(), facing.getStepZ()), mirrorOffset));
    }

    public static MirrorTexture getTexture(boolean leftHand) {
        return TEXTURES.computeIfAbsent(3L << 62 | (leftHand ? 1 : 0), unused -> new MirrorTexture());
    }

    public static void renderMirror(MirrorTexture mirror, int lod, int layer, float mirrorOffset, Vector3fc mirrorPos, Vector3fc mirrorNormal, double cameraX, double cameraY, double cameraZ, Vector3fc up, Vector3fc dir, float renderDistance, boolean render, boolean recurse) {
        AdvancedFbo fbo = VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(MIRROR_FBO[lod]);
        if (fbo == null) {
            return;
        }

        Minecraft client = Minecraft.getInstance();

        Vector3d renderPos = new Vector3d(mirrorPos.x() + 0.5 - mirrorNormal.x() * (0.5 - mirrorOffset-0.01), mirrorPos.y() + 0.5 - mirrorNormal.y() * (0.5 - mirrorOffset-0.01), mirrorPos.z() + 0.5 - mirrorNormal.z() * (0.5 - mirrorOffset-0.01));
        Vector3f offset = new Vector3f((float) (cameraX - renderPos.x), (float) (cameraY - renderPos.y), (float) (cameraZ - renderPos.z));

        Window window = client.getWindow();
        float aspect = (float) window.getWidth() / window.getHeight();
        float fov = RenderSystem.getProjectionMatrix().perspectiveFov();
        RENDER_PROJECTION.setPerspective(fov, aspect, 0.3F, renderDistance * 64);
        RENDER_PROJECTION.mul(applyInverseBob());

        offset.reflect(mirrorNormal);
        renderPos.add(offset);
        Vector3f mirrorDir = dir.reflect(mirrorNormal, new Vector3f());
        Vector3f mirrorUp = up.reflect(mirrorNormal, new Vector3f());

        if (render) {
            Quaternionf look = VIEW.identity().lookAlong(mirrorDir, mirrorUp);
            Vector4f plane = new Vector4f(mirrorNormal.x(), mirrorNormal.y(), mirrorNormal.z(), offset.dot(mirrorNormal.x(), mirrorNormal.y(), mirrorNormal.z()));
            look.transform(plane);
            calculateObliqueMatrix(plane);

            renderMirror = mirror;
            renderLayer = layer + 1;
            VeilLevelPerspectiveRenderer.render(fbo, RENDER_MODELVIEW, RENDER_OBLIQUE_PROJECTION, renderPos, look, renderDistance, client.getTimer(), false);
            renderLayer = layer;
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
            renderMirror(child, lod, layer + 1, mirrorOffset, child.pos, child.normal, renderPos.x, renderPos.y, renderPos.z, mirrorUp, mirrorDir, renderDistance, true, false);
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

    private static void calculateObliqueMatrix(Vector4fc c) {
        Vector4f q = RENDER_PROJECTION.invert(INVERSE_RENDER_PROJECTION).transform(
                Math.signum(c.x()),
                Math.signum(c.y()),
                1.0f,
                1.0f,
                OBLIQUE_PLANE);
        float dot = c.dot(q);
        RENDER_OBLIQUE_PROJECTION.set(RENDER_PROJECTION);
        RENDER_OBLIQUE_PROJECTION.m02(c.x() * 2.0F / dot - RENDER_PROJECTION.m03()).m12(c.y() * 2.0F / dot - RENDER_PROJECTION.m13()).m22(c.z() * 2.0F / dot - RENDER_PROJECTION.m23()).m32(c.w() * 2.0F / dot - RENDER_PROJECTION.m33());
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
            if (mirror.visibleMirrors != null) {
                renderMirror(mirror, 2, 0, mirror.mirrorOffset, mirror.pos, mirror.normal, cameraPos.x, cameraPos.y, cameraPos.z, up, look, RENDER_DISTANCE, false, true);
                mirror.reset();
            }
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
        RENDER_POSITIONS.clear();
    }

    public static void free() {
        for (MirrorTexture mirror : TEXTURES.values()) {
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

    public static int getRenderLayer() {
        return renderLayer;
    }

    public static class MirrorTexture implements NativeResource {

        private final ObjectSet<BlockPos> positions;
        private final BitSet renderedLayers;
        private final TextureWrapper[] textures;
        private final Vector3f pos;
        private final Vector3f normal;
        private final float mirrorOffset;
        private final Set<MirrorTexture> visibleMirrors;

        private int width;
        private int height;

        private MirrorTexture() {
            this.positions = new ObjectArraySet<>();
            this.renderedLayers = new BitSet(1);
            this.textures = new TextureWrapper[1];
            this.pos = null;
            this.normal = null;
            this.mirrorOffset = 0;
            this.visibleMirrors = null;
            this.width = -1;
            this.height = -1;

            TextureWrapper texture = new TextureWrapper(glGenTextures());
            texture.setFilter(false, true);
            this.textures[0] = texture;
            Minecraft.getInstance().getTextureManager().register(texture.getName(), texture);
        }

        private MirrorTexture(Vector3f pos, Vector3f normal, float mirrorOffset) {
            this.positions = new ObjectArraySet<>();
            this.renderedLayers = new BitSet(MAX_LAYERS);
            this.textures = new TextureWrapper[MAX_LAYERS];
            this.pos = pos;
            this.normal = normal;
            this.mirrorOffset = mirrorOffset;
            this.visibleMirrors = new HashSet<>();
            this.width = -1;
            this.height = -1;

            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            int[] textureIds = new int[MAX_LAYERS];
            glGenTextures(textureIds);
            for (int i = 0; i < MAX_LAYERS; i++) {
                TextureWrapper texture = new TextureWrapper(textureIds[i]);
                texture.setFilter(false, true);
                this.textures[i] = texture;
                textureManager.register(texture.getName(), texture);
            }
        }

        public void copy(AdvancedFbo fbo, int layer) {
            int width = fbo.getWidth();
            int height = fbo.getHeight();
            if (this.width != width || this.height != height) {
                this.width = width;
                this.height = height;
                for (int i = 0; i < MAX_LAYERS; i++) {
                    TextureUtil.prepareImage(NativeImage.InternalGlFormat.RGBA, this.textures[layer].getId(), MIPMAP_LEVELS, width, height);
                }
            }

            fbo.bindRead();
            this.textures[layer].bind();
            glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
            AdvancedFbo.unbind();
            glGenerateMipmap(GL_TEXTURE_2D);
        }

        public void reset() {
            this.renderedLayers.clear();
            this.visibleMirrors.clear();
        }

        public void addRecursive(MirrorTexture mirror) {
            this.visibleMirrors.add(mirror);
        }

        @Override
        public void free() {
            TextureManager textureManager = Minecraft.getInstance().getTextureManager();
            for (TextureWrapper texture : this.textures) {
                textureManager.release(texture.getName());
            }
        }

        public boolean hasRendered(int layer) {
            return this.renderedLayers.get(layer);
        }

        public ResourceLocation getTexture(int layer) {
            return this.textures[layer].getName();
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

        public void setRenderedPos(BlockPos pos) {
            this.positions.add(pos);
            RENDER_POSITIONS.add(pos);
        }

        public void setRendered(int layer) {
            this.renderedLayers.set(layer);
        }
    }

    private static class TextureWrapper extends AbstractTexture {

        private final ResourceLocation name;
        private final int id;

        private TextureWrapper(int id) {
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
        public void load(ResourceManager resourceManager) {
        }
    }
}
