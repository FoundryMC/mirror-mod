package foundry.mirror.fabric.mixin.client;

import foundry.mirror.client.render.MirrorItemRenderer;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBakery.class)
public abstract class ModelBakerMixin {

    @Shadow
    protected abstract void registerModelAndLoadDependencies(ModelResourceLocation modelLocation, UnbakedModel model);

    @Shadow
    abstract UnbakedModel getModel(ResourceLocation modelLocation);

    @Inject(method = "<init>", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=special"))
    public void registerSpecial(CallbackInfo ci) {
        this.registerModelAndLoadDependencies(MirrorItemRenderer.MODEL_LOCATION, this.getModel(MirrorItemRenderer.MODEL_LOCATION.id()));
    }
}
