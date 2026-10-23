/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package forestry.core.platform.client.render;

import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * A dynamic fluid container model, capable of re-texturing itself at runtime to match the contained fluid.
 * <p>
 * Composed of a base layer, a fluid layer (applied with a mask) and a cover layer (optionally applied with a mask).
 * The entire model may optionally be flipped if the fluid is gaseous, and the fluid layer may glow if light-emitting.
 */
@SuppressWarnings("deprecation")
public class FluidContainerModel implements ItemModel {
    private static final ModelDebugName DEBUG_NAME = () -> "FluidContainerModel";
    private static final RenderType RENDER_TYPE_CUTOUT_UNLIT_BLOCK = Sheets.cutoutBlockItemSheet();
    private static final RenderType RENDER_TYPE_CUTOUT_UNLIT_ITEM = Sheets.cutoutItemSheet();
    private static final RenderType RENDER_TYPE_TRANSLUCENT_UNLIT_BLOCK = Sheets.translucentBlockItemSheet();
    private static final RenderType RENDER_TYPE_TRANSLUCENT_UNLIT_ITEM = Sheets.translucentItemSheet();

    private final Unbaked unbakedModel;
    private final BakingContext bakingContext;
    private final Matrix4fc transformation;
    private final ItemTransforms itemTransforms;
    private final Map<Fluid, ItemModel> cache = new IdentityHashMap<>(); // contains all the baked models since they'll never change

    private FluidContainerModel(Unbaked unbakedModel, BakingContext bakingContext, Matrix4fc transformation) {
        this.unbakedModel = unbakedModel;
        this.bakingContext = bakingContext;
        this.transformation = transformation;
        // Source ItemTransforms from the base item model
        var baseItemModel = bakingContext.blockModelBaker().getModel(Identifier.withDefaultNamespace("item/generated"));
        this.itemTransforms = baseItemModel.getTopTransforms();
    }

    private ItemModel bakeModelForFluid(Fluid fluid) {
        ModelBaker baker = bakingContext.blockModelBaker();
        MaterialBaker materials = baker.materials();
        FluidModel fluidModel = Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(fluid.defaultFluidState());

        Material particleLocation = unbakedModel.textures.particle.orElse(null);
        Material baseLocation = unbakedModel.textures.base.orElse(null);
        Material fluidMaskLocation = unbakedModel.textures.fluid.orElse(null);
        Material coverLocation = unbakedModel.textures.cover.orElse(null);

        Material.Baked baseSprite = baseLocation != null ? materials.get(baseLocation, DEBUG_NAME) : null;
        Material.Baked fluidSprite = fluid != Fluids.EMPTY ? fluidModel.stillMaterial() : null;
        Material.Baked coverSprite = (coverLocation != null && (!unbakedModel.coverIsMask || baseLocation != null)) ? materials.get(coverLocation, DEBUG_NAME) : null;

        Material.Baked particleSprite = particleLocation != null ? materials.get(particleLocation, DEBUG_NAME) : null;

        if (particleSprite == null) particleSprite = fluidSprite;
        if (particleSprite == null) particleSprite = baseSprite;
        if (particleSprite == null && !unbakedModel.coverIsMask) particleSprite = coverSprite;

        // If the fluid is lighter than air, rotate 180deg to turn it upside down
        ModelState state = BlockModelRotation.IDENTITY;
        if (unbakedModel.flipGas && fluid != Fluids.EMPTY && net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.isLighterThanAir(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid))) {
            state = new ModelState() { @Override public Transformation transformation() { return new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null); } };
        }

        List<ItemModel> subModels = new ArrayList<>();
        ModelRenderProperties renderProperties = new ModelRenderProperties(false, particleSprite, itemTransforms);

        if (baseLocation != null) {
            // Base texture
            QuadCollection quads = FlatItemGeometry.bake(baker, BakedQuad.MaterialInfo.of(baseSprite, baseSprite.sprite().transparency(), -1, false, 0), state, 0, 0, 16, 16, 0);
            subModels.add(new CuboidItemModelWrapper(List.of(), quads, renderProperties, transformation));
        }

        if (fluidMaskLocation != null && fluidSprite != null) {
            // Fluid layer
            boolean opaque = unbakedModel.forceOpaqueFluid;
            boolean emissive = unbakedModel.applyFluidLuminosity && net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes.getLuminance(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid)) > 0;
            ModelBaker.Interner interner = baker.interner();
            BakedQuad.MaterialInfo fluidInfo = interner.materialInfo(new BakedQuad.MaterialInfo(
                    fluidSprite.sprite(), ChunkSectionLayer.SOLID, computeFluidItemRenderType(fluidSprite, opaque, emissive), 0, false, emissive ? Level.MAX_BRIGHTNESS : 0));
            var mask = materials.get(fluidMaskLocation, DEBUG_NAME).sprite().contents();
            QuadCollection quads = FlatItemGeometry.masked(baker, fluidInfo, state, mask, unbakedModel.bucketMask, 0.001f);

            subModels.add(new CuboidItemModelWrapper(List.of(new forestry.core.platform.models.ForestryItemTintSource(1)), quads, renderProperties, transformation));
        }

        if (coverSprite != null) {
            Material.Baked sprite = unbakedModel.coverIsMask ? baseSprite : coverSprite;
            // Cover/overlay
            var material = BakedQuad.MaterialInfo.of(sprite, sprite.sprite().transparency(), -1, false, 0);
            QuadCollection quads = unbakedModel.coverIsMask
                ? FlatItemGeometry.masked(baker, material, state, coverSprite.sprite().contents(), false, 0.002f)
                : FlatItemGeometry.bake(baker, material, state, 0, 0, 16, 16, 0.002f);
            subModels.add(new CuboidItemModelWrapper(List.of(), quads, renderProperties, transformation));
        }

        return new CompositeModel(subModels);
    }

    // Force all fluids into cutout to avoid blending issues with block atlas sprites being rendered in front of item atlas sprites (unless the model opts out of it)
    private static RenderType computeFluidItemRenderType(Material.Baked material, boolean forceOpaque, boolean emissive) {
        boolean translucent = !forceOpaque && (material.forceTranslucent() || material.sprite().transparency().hasTranslucent());
        if (material.sprite().atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)) {
            if (translucent) {
                return emissive ? RENDER_TYPE_TRANSLUCENT_UNLIT_BLOCK : Sheets.translucentBlockItemSheet();
            } else {
                return emissive ? RENDER_TYPE_CUTOUT_UNLIT_BLOCK : Sheets.cutoutBlockItemSheet();
            }
        } else {
            if (translucent) {
                return emissive ? RENDER_TYPE_TRANSLUCENT_UNLIT_ITEM : Sheets.translucentItemSheet();
            } else {
                return emissive ? RENDER_TYPE_CUTOUT_UNLIT_ITEM : Sheets.cutoutItemSheet();
            }
        }
    }

    @Override
    public void update(ItemStackRenderState renderState, ItemStack stack, ItemModelResolver modelResolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        var fluidStack = forestry.core.platform.fluids.FluidHelper.getFluidContained(stack);
        var fluid = fluidStack.isEmpty() ? unbakedModel.fluid : fluidStack.getFluid();

        cache.computeIfAbsent(fluid, this::bakeModelForFluid)
                .update(renderState, stack, modelResolver, displayContext, level, owner, seed);
    }

    public record Textures(
            Optional<Material> particle,
            Optional<Material> base,
            Optional<Material> fluid,
            Optional<Material> cover) {
        public static final Codec<Textures> CODEC = RecordCodecBuilder.<Textures>create(
                instance -> instance
                        .group(
                                Material.CODEC.optionalFieldOf("particle").forGetter(Textures::particle),
                                Material.CODEC.optionalFieldOf("base").forGetter(Textures::base),
                                Material.CODEC.optionalFieldOf("fluid").forGetter(Textures::fluid),
                                Material.CODEC.optionalFieldOf("cover").forGetter(Textures::cover))
                        .apply(instance, Textures::new))
                .validate(textures -> {
                    if (textures.particle.isPresent() || textures.base.isPresent() || textures.fluid.isPresent() || textures.cover.isPresent()) {
                        return DataResult.success(textures);
                    }
                    return DataResult.error(() -> "Dynamic fluid container model requires at least one particle, base, fluid or cover texture.");
                });
    }

    /// @param forceOpaqueFluid Whether the fluid should always be rendered opaque regardless of its actual translucency. Disabling this may cause issues with translucent fluids
    ///                         on top of a base texture stitched to the item atlas
    public record Unbaked(Textures textures, Fluid fluid, boolean flipGas, boolean coverIsMask, boolean applyFluidLuminosity, boolean forceOpaqueFluid, boolean bucketMask) implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance
                        .group(
                                Textures.CODEC.fieldOf("textures").forGetter(Unbaked::textures),
                                BuiltInRegistries.FLUID.byNameCodec().fieldOf("fluid").forGetter(Unbaked::fluid),
                                Codec.BOOL.optionalFieldOf("flip_gas", false).forGetter(Unbaked::flipGas),
                                Codec.BOOL.optionalFieldOf("cover_is_mask", true).forGetter(Unbaked::coverIsMask),
                                Codec.BOOL.optionalFieldOf("apply_fluid_luminosity", true).forGetter(Unbaked::applyFluidLuminosity),
                                Codec.BOOL.optionalFieldOf("force_opaque_fluid", true).forGetter(Unbaked::forceOpaqueFluid),
                                Codec.BOOL.optionalFieldOf("bucket_mask", false).forGetter(Unbaked::bucketMask))
                        .apply(instance, Unbaked::new));

        public Unbaked(Textures textures, Fluid fluid, boolean flipGas, boolean coverIsMask, boolean applyFluidLuminosity) {
            this(textures, fluid, flipGas, coverIsMask, applyFluidLuminosity, true, false);
        }

        @Override
        public MapCodec<? extends ItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(BakingContext bakingContext, Matrix4fc transformation) {
            return new FluidContainerModel(this, bakingContext, transformation);
        }

        @Override
        public void resolveDependencies(Resolver resolver) {
            //No dependencies
        }
    }
}
