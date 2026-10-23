package forestry.core.platform.fluids;

import forestry.core.platform.registration.RegistrationBatch;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.*;

/** Non-placeable milk retains the previous registry ID and vanilla drinkable bucket. */
public final class MilkFluid extends EmptyFluid {
    public static final Identifier ID = Identifier.withDefaultNamespace("milk");
    public static void declare() {
        if (!BuiltInRegistries.FLUID.containsKey(ID)) RegistrationBatch.create(Registries.FLUID, "minecraft").register("milk", MilkFluid::new);
    }
    public static Fluid fluid() { return BuiltInRegistries.FLUID.getValue(ID); }
    public static void initializeTransfer() {
        FluidStorage.combinedItemApiProvider(Items.MILK_BUCKET).register(context ->
            new FullItemFluidStorage(context, Items.BUCKET, FluidVariant.of(fluid()), FluidConstants.BUCKET));
        FluidStorage.combinedItemApiProvider(Items.BUCKET).register(context ->
            new EmptyItemFluidStorage(context, Items.MILK_BUCKET, fluid(), FluidConstants.BUCKET));
        FluidVariantAttributes.register(fluid(), new FluidVariantAttributeHandler() {
            @Override public net.minecraft.network.chat.Component getName(FluidVariant variant) {
                return net.minecraft.network.chat.Component.translatable("item.minecraft.milk_bucket");
            }
        });
    }
    @Override public Item getBucket() { return Items.MILK_BUCKET; }
    @Override protected boolean isEmpty() { return false; }
    @Override public boolean isSource(FluidState state) { return true; }
    @Override public int getAmount(FluidState state) { return 8; }
}
