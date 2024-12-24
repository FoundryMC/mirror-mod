package foundry.mirror.mixin.client;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import foundry.mirror.client.render.MirrorBlockEntityRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void saveOrientation(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci, @Share("orientation") LocalRef<Quaternionf> orientation) {
        if (MirrorBlockEntityRenderer.isRenderingMirror()) {
            orientation.set(new Quaternionf(camera.rotation()));
            camera.rotation().set(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void clearOrientation(LightTexture lightTexture, Camera camera, float partialTick, CallbackInfo ci, @Share("orientation") LocalRef<Quaternionf> orientation) {
        if (MirrorBlockEntityRenderer.isRenderingMirror()) {
            camera.rotation().set(orientation.get());
        }
    }
}
