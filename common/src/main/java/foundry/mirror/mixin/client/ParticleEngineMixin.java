package foundry.mirror.mixin.client;

import foundry.mirror.client.render.MirrorRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Unique
    private final Quaternionf mirror$orientation = new Quaternionf();

    @Inject(method = "render", at = @At("HEAD"))
    public void saveOrientation(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        if (MirrorRenderer.isRenderingMirror()) {
            this.mirror$orientation.set(camera.rotation());
            camera.rotation().conjugate();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void clearOrientation(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci) {
        if (MirrorRenderer.isRenderingMirror()) {
            camera.rotation().set(this.mirror$orientation);
        }
    }
}
