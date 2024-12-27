package foundry.mirror.neoforge;


import foundry.mirror.MirrorMod;
import foundry.mirror.client.render.MirrorBlockEntityRenderer;
import foundry.mirror.client.render.MirrorRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = MirrorMod.MOD_ID)
public class MirrorModNeoforgeClientEvents {

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        MirrorRenderer.free();
    }

    @SubscribeEvent
    public static void onWorldTickEnd(LevelTickEvent.Post event) {
        MirrorRenderer.endClientTick();
    }
}