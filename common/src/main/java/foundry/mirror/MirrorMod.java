package foundry.mirror;

import foundry.mirror.registry.MirrorBlocks;
import foundry.mirror.registry.MirrorItems;
import net.minecraft.resources.ResourceLocation;

public class MirrorMod {

    public static final String MOD_ID = "mirror";

    public static void init() {
        MirrorItems.bootstrap();
        MirrorBlocks.bootstrap();
    }

    public static ResourceLocation path(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}