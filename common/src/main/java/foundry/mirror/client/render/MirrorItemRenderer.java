package foundry.mirror.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.mirror.MirrorMod;
import foundry.veil.api.client.render.VeilLevelPerspectiveRenderer;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class MirrorItemRenderer {

    public static final ModelResourceLocation MODEL_LOCATION = new ModelResourceLocation(MirrorMod.path("item/mirror_pane_model"), "standalone");
    public static final boolean RENDER_REFLECTION = false;

    public static void render(ItemStack stack, ItemDisplayContext mode, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        RenderType rendertype = ItemBlockRenderTypes.getRenderType(stack, true);
        VertexConsumer vertexconsumer = ItemRenderer.getFoilBufferDirect(bufferSource, rendertype, true, stack.hasFoil());

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel model = modelManager.getModel(MODEL_LOCATION);
        BakedModel override = model.getOverrides().resolve(model, stack, null, null, 0);
        itemRenderer.renderModelLists(override == null ? modelManager.getMissingModel() : override, stack, packedLight, packedOverlay, poseStack, vertexconsumer);

        if (!RENDER_REFLECTION || !mode.firstPerson() || VeilLevelPerspectiveRenderer.isRenderingPerspective()) {
            return;
        }

        boolean leftHand = mode == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        MirrorRenderer.MirrorTexture mirror = MirrorRenderer.getTexture(leftHand);
        MirrorRenderer.MirrorTexture renderMirror = MirrorRenderer.getRenderMirror();
        if (mirror == renderMirror) {
            return;
        }

        RenderType renderType = VeilRenderType.get(MirrorRenderer.MIRROR_RENDER_TYPE, mirror.getTexture(0));
        if (renderType == null) {
            return;
        }
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f look = new Vector3f(0, 0, 1);
        Quaternionfc orientation = camera.rotation();
        Vector3d pos = new Vector3d(cameraPos.x, cameraPos.y, cameraPos.z).add(orientation.transform(new Vector3f(leftHand ? -0.2F : 0.2F, 0.0F, -1.5F)));

        up.rotateX((float) (Math.PI / 4));
        look.rotateZ((float) (Math.PI / 4));
        poseStack.pushPose();
        poseStack.setIdentity();
        model.getTransforms().getTransform(mode).apply(leftHand, poseStack);
        Matrix4f matrix4f = poseStack.last().pose().invert();
        matrix4f.transformDirection(up);
        matrix4f.transformDirection(look);
        up.rotate(orientation);
        look.rotate(orientation);
        poseStack.popPose();

        MirrorRenderer.renderMirror(mirror, 0, 0, 0, new Vector3f(), new Vector3f(), pos.x, pos.y, pos.z, up, look, 2.0F, true, false);

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer builder = bufferSource.getBuffer(renderType);
        builder.addVertex(pose, 0, 0, 0.875F).setUv(0.0F, 1.0F).setColor(0.9F, 0.9F, 0.9F, 0.7F).setNormal(0, 0, -1);
        builder.addVertex(pose, 0, 1, 0.875F).setUv(0.0F, 0.0F).setColor(0.9F, 0.9F, 0.9F, 0.7F).setNormal(0, 0, -1);
        builder.addVertex(pose, 1, 1, 0.875F).setUv(1.0F, 0.0F).setColor(0.9F, 0.9F, 0.9F, 0.7F).setNormal(0, 0, -1);
        builder.addVertex(pose, 1, 0, 0.875F).setUv(1.0F, 1.0F).setColor(0.9F, 0.9F, 0.9F, 0.7F).setNormal(0, 0, -1);
        if (bufferSource instanceof MultiBufferSource.BufferSource source) {
            VeilRenderSystem.endLastBatch(source, renderType);
        }
    }
}
