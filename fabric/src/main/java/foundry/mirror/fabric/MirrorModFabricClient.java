package foundry.mirror.fabric;

import foundry.mirror.MirrorModClient;
import foundry.mirror.client.render.MirrorPaneBlockEntityRenderer;
import foundry.mirror.client.render.MirrorItemRenderer;
import foundry.mirror.client.render.MirrorRenderer;
import foundry.mirror.registry.MirrorBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MirrorModFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MirrorModClient.initClient();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(MirrorRenderer::free));
        ClientTickEvents.END_WORLD_TICK.register(level -> MirrorRenderer.endClientTick());
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> {
            entries.addAfter(Items.BEACON, new ItemStack(MirrorBlocks.MIRROR.get()));
        });
        BlockEntityRenderers.register(MirrorBlocks.MIRROR_BE.get(), MirrorPaneBlockEntityRenderer::new);
        BuiltinItemRendererRegistry.INSTANCE.register(MirrorBlocks.MIRROR.get(), MirrorItemRenderer::render);
    }
}
