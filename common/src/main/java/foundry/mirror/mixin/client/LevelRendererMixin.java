package foundry.mirror.mixin.client;

import foundry.mirror.client.render.MirrorRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V"), index = 1)
    public Matrix4f modifyProjection(Matrix4f matrix) {
        return MirrorRenderer.isRenderingMirror() ? MirrorRenderer.getRenderProjection() : matrix;
    }
}
