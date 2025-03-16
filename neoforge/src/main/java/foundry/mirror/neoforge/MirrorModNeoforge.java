package foundry.mirror.neoforge;


import foundry.mirror.MirrorMod;
import foundry.mirror.client.render.MirrorBlockEntityRenderer;
import foundry.mirror.registry.MirrorBlocks;
import foundry.mirror.registry.MirrorItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(MirrorMod.MOD_ID)
public class MirrorModNeoforge {

    public MirrorModNeoforge(IEventBus eventBus) {
        MirrorItems.bootstrap();
        MirrorBlocks.bootstrap();
        eventBus.addListener(this::registerBlockEntityRenderers);
        eventBus.addListener(this::fillCreativeTab);
    }

    private void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(MirrorBlocks.MIRROR_BE.get(), MirrorBlockEntityRenderer::new);
    }

    private void fillCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) {
            ItemStack insert = new ItemStack(Items.BEACON);
            event.insertAfter(insert, new ItemStack(MirrorBlocks.MIRROR_PANE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(insert, new ItemStack(MirrorBlocks.MIRROR.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}