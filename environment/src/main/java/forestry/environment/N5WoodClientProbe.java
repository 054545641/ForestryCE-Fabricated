package forestry.environment;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import forestry.arboriculture.client.*;
import forestry.arboriculture.features.ArboricultureEntities;
import forestry.arboriculture.wood.ForestryWoodType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.object.boat.BoatModel;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = EnvironmentProbe.ID, value = Dist.CLIENT)
public final class N5WoodClientProbe {
    private static int passes;
    private static Object lastSign;
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BoatRenderGeometry.BOAT, BoatModel::createBoatModel);
        event.registerLayerDefinition(BoatRenderGeometry.CHEST_BOAT, BoatModel::createChestBoatModel);
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ArboricultureEntities.BOAT.entityType(), context -> new ForestryBoatRenderer(context,false));
        event.registerEntityRenderer(ArboricultureEntities.CHEST_BOAT.entityType(), context -> new ForestryBoatRenderer(context,true));
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static final class Collector extends SubmitNodeStorage {
        final List<RenderType> types = new ArrayList<>();
        final List<Matrix4f> transforms = new ArrayList<>();
        final List<Runnable> animations = new ArrayList<>();
        final List<Model<?>> models = new ArrayList<>();
        @Override public <S> void submitModel(Model<? super S> model, S state, PoseStack stack, RenderType type, int light, int overlay,
                                              int color, @Nullable TextureAtlasSprite sprite, int outline, ModelFeatureRenderer.@Nullable CrumblingOverlay breaking) {
            types.add(type); transforms.add(new Matrix4f(stack.last().pose())); models.add(model);
            animations.add(() -> model.setupAnim(state));
            super.submitModel(model,state,stack,type,light,overlay,color,sprite,outline,breaking);
        }
    }
    public static void verify(Minecraft client) {
        for (var wood : ForestryWoodType.VALUES) for (boolean chest : List.of(false,true)) {
            var texture = BoatRenderGeometry.texture(wood.getSerializedName(),chest);
            check(client.getResourceManager().getResource(texture).isPresent(), "Wood entity texture exists: "+texture);
            var geometry = new BoatRenderGeometry((chest?BoatModel.createChestBoatModel():BoatModel.createBoatModel()).bakeRoot(), BoatModel.createWaterPatch().bakeRoot());
            var a = new ForestryBoatRenderState(); a.texture=texture; a.yRot=40; a.rowingTimeLeft=1; a.rowingTimeRight=2;
            var b = new ForestryBoatRenderState(); b.texture=BoatRenderGeometry.texture("teak",chest); b.isUnderWater=true; b.rowingTimeLeft=3;
            Collector collector = new Collector(); PoseStack stack = new PoseStack();
            geometry.submit(a,stack,collector); geometry.submit(b,stack,collector);
            check(collector.types.size()==3 && collector.types.get(1)==RenderTypes.waterMask(), "Water mask is omitted underwater");
            check(collector.types.getFirst()==RenderTypes.entityCutout(texture) && collector.types.get(2)==RenderTypes.entityCutout(b.texture), "Each submission retains its own wood texture");
            check(stack.last().pose().equals(new Matrix4f()), "Boat submit balances caller transform");
            check(collector.models.getFirst().root().hasChild("chest_bottom")==chest, "Chest geometry selected by renderer variant");
            collector.animations.getFirst().run(); float first=collector.models.getFirst().root().getChild("left_paddle").xRot;
            collector.animations.get(2).run(); float second=collector.models.getFirst().root().getChild("left_paddle").xRot;
            collector.animations.getFirst().run();
            check(first!=second && first==collector.models.getFirst().root().getChild("left_paddle").xRot, "Deferred rowing state does not leak between boats");
            b.isUnderWater=false; b.hurtTime=4; b.hurtDir=-1; b.damageTime=3; b.bubbleAngle=20;
            Collector wobble=new Collector(); geometry.submit(b,new PoseStack(),wobble);
            check(!wobble.transforms.getFirst().equals(collector.transforms.get(2)), "Hurt/bubble state changes the submitted pose");
        }
        int states=0;
        for (var fixture : N5WoodFixtures.SIGNS) for(var state : fixture.get().getStateDefinition().getPossibleStates()) {
            var model=client.getModelManager().getBlockStateModelSet().get(state);
            if(states==0) { check(lastSign!=model,"Reload replaces sign model"); lastSign=model; }
            List<BlockStateModelPart> parts=new ArrayList<>(); model.collectParts(RandomSource.create(42),parts);
            int quads=0;
            for(var part:parts) {
                List<net.minecraft.client.resources.model.geometry.BakedQuad> all=new ArrayList<>(part.getQuads(null));
                for(var face:Direction.values()) all.addAll(part.getQuads(face));
                quads+=all.size();
                check(all.stream().allMatch(q -> q.materialInfo().sprite().contents().name().getPath().startsWith("entity/signs/")), "Sign faces use real legacy texture atlas sprites");
            }
            check(quads>=6,"Every sign orientation/attachment/waterlogged state has a board: "+state);
            states++;
        }
        for(var wood:ForestryWoodType.VALUES) for(String suffix:List.of("boat","chest_boat","sign","hanging_sign")) {
            Identifier id=Identifier.fromNamespaceAndPath("forestry",wood.getSerializedName()+"_"+suffix);
            var item=client.getModelManager().getItemModel(id); var output=new ItemStackRenderState();
            item.update(output,ItemStack.EMPTY,client.getItemModelResolver(),ItemDisplayContext.GUI,null,null,0);
            check(!output.isEmpty() && output.getModelBoundingBox().getSize()>0,"Wood item geometry exists: "+id);
            var particle=output.pickParticleMaterial(RandomSource.create());
            check(particle!=null && !particle.sprite().contents().name().getPath().equals("missingno"),"Wood item has real texture: "+id);
        }
        LogUtils.getLogger().info("N5 wood client probe passed (pass {}): 86 boat textures/variants, deferred rowing/texture isolation, water masks/wobble, {} sign states, 172 item definitions",++passes,states);
    }
}

