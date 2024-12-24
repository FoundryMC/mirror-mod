package foundry.mirror.registry;

import foundry.mirror.MirrorMod;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.function.Supplier;

public class MirrorItems {

    public static final RegistrationProvider<Item> ITEM_REGISTRY = RegistrationProvider.get(BuiltInRegistries.ITEM, MirrorMod.MOD_ID);

    public static void bootstrap() {
    }

    public static <T extends Item> RegistryObject<T> register(String name, Supplier<T> item) {
        return ITEM_REGISTRY.register(name, item);
    }
}
