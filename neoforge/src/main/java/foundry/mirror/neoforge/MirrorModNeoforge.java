package foundry.mirror.neoforge;


import foundry.mirror.MirrorMod;
import foundry.mirror.client.render.MirrorBlockEntityRenderer;
import foundry.mirror.registry.MirrorBlocks;
import foundry.mirror.registry.MirrorItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(MirrorMod.MOD_ID)
public class MirrorModNeoforge {

    public MirrorModNeoforge(IEventBus eventBus) {
        MirrorItems.bootstrap();
        MirrorBlocks.bootstrap();
        eventBus.addListener(this::registerBlockEntityRenderers);
    }

    private void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MirrorBlocks.MIRROR_BE.get(), MirrorBlockEntityRenderer::new);
    }
}