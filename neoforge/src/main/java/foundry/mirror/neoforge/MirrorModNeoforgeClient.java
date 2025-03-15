package foundry.mirror.neoforge;


import foundry.mirror.MirrorMod;
import foundry.mirror.MirrorModClient;
import foundry.mirror.client.render.MirrorItemRenderer;
import foundry.mirror.registry.MirrorBlocks;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jetbrains.annotations.NotNull;

@Mod(value = MirrorMod.MOD_ID, dist = Dist.CLIENT)
public class MirrorModNeoforgeClient {

    public MirrorModNeoforgeClient(IEventBus bus) {
        MirrorModClient.initClient();
        bus.addListener(this::registerExtensions);
        bus.addListener(this::onModelLoad);
    }

    private void registerExtensions(RegisterClientExtensionsEvent event) {
        ForgeMirrorItemRenderer renderer = new ForgeMirrorItemRenderer();
        event.registerItem(new IClientItemExtensions() {
            @Override
            public @NotNull BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        }, MirrorBlocks.MIRROR_PANE.get().asItem());
    }

    private void onModelLoad(ModelEvent.RegisterAdditional event) {
        event.register(MirrorItemRenderer.MODEL_LOCATION);
    }
}