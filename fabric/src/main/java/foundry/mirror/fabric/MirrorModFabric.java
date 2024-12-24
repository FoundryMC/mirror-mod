package foundry.mirror.fabric;

import foundry.mirror.MirrorMod;
import net.fabricmc.api.ModInitializer;

public class MirrorModFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        MirrorMod.init();
    }
}
