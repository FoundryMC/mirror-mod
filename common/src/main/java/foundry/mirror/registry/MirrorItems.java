package foundry.mirror.registry;

import foundry.mirror.MirrorMod;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public final class MirrorItems {

    public static final RegistrationProvider<Item> ITEM_REGISTRY = RegistrationProvider.get(BuiltInRegistries.ITEM, MirrorMod.MOD_ID);

    private MirrorItems() {
    }

    public static void bootstrap() {
    }

    public static <T extends Item> RegistryObject<T> register(final String name, final Supplier<T> item) {
        return ITEM_REGISTRY.register(name, item);
    }
}
