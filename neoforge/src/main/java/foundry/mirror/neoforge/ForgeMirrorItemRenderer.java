package foundry.mirror.neoforge;

import com.mojang.blaze3d.vertex.PoseStack;
import foundry.mirror.client.render.MirrorItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ForgeMirrorItemRenderer extends BlockEntityWithoutLevelRenderer {

    public ForgeMirrorItemRenderer() {
        super(null, null);
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack, @NotNull ItemDisplayContext displayContext, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        MirrorItemRenderer.render(stack, displayContext, poseStack, buffer, packedLight, packedOverlay);
    }
}
