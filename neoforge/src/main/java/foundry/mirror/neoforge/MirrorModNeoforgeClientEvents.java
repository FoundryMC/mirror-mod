package foundry.mirror.neoforge;


import foundry.mirror.MirrorMod;
import foundry.mirror.client.render.MirrorBlockEntityRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = MirrorMod.MOD_ID)
public class MirrorModNeoforgeClientEvents {

    @SubscribeEvent
    public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        MirrorBlockEntityRenderer.free();
    }

    @SubscribeEvent
    public static void onWorldRenderEnd(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            MirrorBlockEntityRenderer.endClientTick();
        }
    }

    @SubscribeEvent
    public static void onWorldTickEnd(LevelTickEvent.Post event) {
        MirrorBlockEntityRenderer.endClientTick();
    }
}