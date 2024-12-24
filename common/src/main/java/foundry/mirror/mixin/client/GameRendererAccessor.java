package foundry.mirror.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Accessor
    int getConfusionAnimationTick();

    @Invoker
    void invokeBobHurt(PoseStack poseStack, float partialTicks);

    @Invoker
    void invokeBobView(PoseStack poseStack, float partialTicks);
}
