package foundry.mirror.fabric;

import foundry.mirror.MirrorMod;
import foundry.mirror.registry.MirrorBlocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

public class MirrorModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        MirrorMod.init();

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(entries -> entries.addAfter(Items.TINTED_GLASS, MirrorBlocks.MIRROR.get(), MirrorBlocks.MIRROR_PANE.get()));
    }
}
