package foundry.mirror.registry;

import foundry.mirror.MirrorMod;
import foundry.mirror.block.MirrorBlock;
import foundry.mirror.block.MirrorPaneBlock;
import foundry.mirror.blockentity.MirrorBlockEntity;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

public class MirrorBlocks {

    public static final RegistrationProvider<Block> BLOCK_REGISTRY = RegistrationProvider.get(BuiltInRegistries.BLOCK, MirrorMod.MOD_ID);
    public static final RegistrationProvider<BlockEntityType<?>> BLOCK_ENTITY_REGISTRY = RegistrationProvider.get(BuiltInRegistries.BLOCK_ENTITY_TYPE, MirrorMod.MOD_ID);

    public static final RegistryObject<MirrorBlock> MIRROR = register("mirror_pane", () -> new MirrorPaneBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK)
                    .noOcclusion()
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)),
            new Item.Properties());
    public static final RegistryObject<BlockEntityType<MirrorBlockEntity>> MIRROR_BE = registerBlockEntity("mirror", () -> BlockEntityType.Builder.of(MirrorBlockEntity::new, MIRROR.get()));

    public static void bootstrap() {
    }

    public static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block, Item.Properties properties) {
        RegistryObject<T> object = register(name, block);
        MirrorItems.register(name, () -> new BlockItem(object.get(), properties));
        return object;
    }

    public static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block) {
        return BLOCK_REGISTRY.register(name, block);
    }

    public static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> registerBlockEntity(String name, Supplier<BlockEntityType.Builder<T>> builder) {
        return BLOCK_ENTITY_REGISTRY.register(name, () -> builder.get().build(null));
    }
}
