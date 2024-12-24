package foundry.mirror.fabric;

import foundry.mirror.client.render.MirrorBlockEntityRenderer;
import foundry.mirror.registry.MirrorBlocks;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import foundry.veil.fabric.event.FabricVeilRenderLevelStageEvent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MirrorModFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(MirrorBlockEntityRenderer::free));
        ClientTickEvents.END_WORLD_TICK.register(level -> MirrorBlockEntityRenderer.endClientTick());
        FabricVeilRenderLevelStageEvent.EVENT.register((stage, levelRenderer, bufferSource, matrixStack, frustumMatrix, projectionMatrix, renderTick, deltaTracker, camera, frustum) -> {
            if (stage == VeilRenderLevelStageEvent.Stage.AFTER_LEVEL) {
                MirrorBlockEntityRenderer.levelRenderEnd();
            }
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.addAfter(Items.BEACON, new ItemStack(MirrorBlocks.MIRROR.get()));
        });
        BlockEntityRenderers.register(MirrorBlocks.MIRROR_BE.get(), MirrorBlockEntityRenderer::new);
    }
}
