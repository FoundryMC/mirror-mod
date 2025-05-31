package foundry.mirror.neoforge;


import foundry.mirror.MirrorMod;
import foundry.mirror.client.render.MirrorRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = MirrorMod.MOD_ID)
public final class MirrorModNeoforgeClientEvents {

    private MirrorModNeoforgeClientEvents() {
    }

    @SubscribeEvent
    public static void onDisconnect(final ClientPlayerNetworkEvent.LoggingOut event) {
        MirrorRenderer.free();
    }
}