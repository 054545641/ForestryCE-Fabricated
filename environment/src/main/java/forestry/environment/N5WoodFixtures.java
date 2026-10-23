package forestry.environment;

import forestry.arboriculture.wood.ForestryWoodType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Native sign blocks used to bake every production sign blockstate without bootstrapping Forestry modules. */
public final class N5WoodFixtures {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, EnvironmentProbe.ID);
    public static final List<Supplier<? extends Block>> SIGNS = new ArrayList<>();
    static {
        for (var wood : ForestryWoodType.VALUES) for (String kind : List.of("sign","wall_sign","hanging_sign","wall_hanging_sign")) {
            SIGNS.add(BLOCKS.register(wood.getSerializedName()+"_"+kind, id -> {
                var properties = BlockBehaviour.Properties.of().noCollision().setId(ResourceKey.create(Registries.BLOCK,id));
                return switch (kind) {
                    case "sign" -> new StandingSignBlock(WoodType.OAK, properties);
                    case "wall_sign" -> new WallSignBlock(WoodType.OAK, properties);
                    case "hanging_sign" -> new CeilingHangingSignBlock(WoodType.OAK, properties);
                    default -> new WallHangingSignBlock(WoodType.OAK, properties);
                };
            }));
        }
    }
}
