package forestry.core.platform.compat.jade;

import forestry.api.apiculture.IBeeHousing;
import forestry.api.core.IError;
import forestry.api.core.IErrorLogicSource;
import forestry.api.core.genetics.IFruitBearer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum ForestryJadeData implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    public static final Identifier ID = Identifier.fromNamespaceAndPath("forestry", "status");

    @Override
    public Identifier getUid() {
        return ID;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        append(data, accessor.getBlockEntity());
    }

    public static void append(CompoundTag data, BlockEntity entity) {
        if (entity == null || !BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("forestry")) return;
        CompoundTag status = new CompoundTag();
        if (entity instanceof IErrorLogicSource source) {
            ListTag errors = new ListTag();
            source.getErrorLogic().getErrors().stream().map(IError::getDescriptionTranslationKey).sorted().limit(32)
                .forEach(key -> errors.add(StringTag.valueOf(key)));
            status.put("errors", errors);
        }
        if (entity instanceof IBeeHousing housing) {
            status.putInt("bee_progress", housing.getBeekeepingLogic().getBeeProgressPercent());
        }
        if (entity instanceof IFruitBearer fruit && fruit.hasFruit()) {
            status.putInt("ripeness", Math.clamp(Math.round(fruit.getRipeness() * 100), 0, 100));
        }
        if (!status.isEmpty()) data.put("forestry:status", status);
    }
}
