package forestry.environment;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import forestry.arboriculture.client.SignEditorTexture;
import forestry.arboriculture.wood.ForestryWoodType;
import forestry.core.platform.entities.ParticleIgnition;
import forestry.core.platform.entities.ParticleSmoke;
import forestry.core.platform.gui.FilterSearchField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import org.lwjgl.glfw.GLFW;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;

@EventBusSubscriber(modid=EnvironmentProbe.ID,value=Dist.CLIENT)
public final class N5GuiParticleProbe {
    private static int passes;
    private static Object lastTexture;
    @SubscribeEvent public static void reload(AddClientReloadListenersEvent event) {
        if(Boolean.getBoolean("forestry.environment.n5Models")) SignEditorTexture.register(event,
            Arrays.stream(ForestryWoodType.VALUES).map(ForestryWoodType::getSerializedName).toList());
    }
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    public static void verify(Minecraft client) {
        try {
            for(var wood:ForestryWoodType.VALUES) for(boolean hanging:new boolean[]{false,true}) {
                var id=SignEditorTexture.editorId(wood.getSerializedName(),hanging);
                var texture=client.getTextureManager().getTexture(id);
                check(texture instanceof SignEditorTexture,"Native editor path registered: "+id);
                check(texture.getTexture().getWidth(0)==(hanging?16:24) && texture.getTexture().getHeight(0)==(hanging?16:26),"Uploaded native editor dimensions: "+id);
                try(var contents=new SignEditorTexture(wood.getSerializedName(),hanging).loadContents(client.getResourceManager())) {
                    long visible=Arrays.stream(contents.image().getPixels()).filter(pixel -> (pixel>>>24)!=0).count();
                    check(visible>120,"Editor image contains a visible board: "+id);
                }
            }
            var current=client.getTextureManager().getTexture(SignEditorTexture.editorId("larch",false));
            check(current!=lastTexture,"Reload replaces editor GPU texture");lastTexture=current;
            try(var source=new NativeImage(128,64,true)) {
                for(int y=0;y<64;y++)for(int x=0;x<128;x++)source.setPixel(x,y,0xFF000000 | x<<8 | y);
                try(var standing=SignEditorTexture.compose(source,false);var hanging=SignEditorTexture.compose(source,true)) {
                    check(standing.getWidth()==48 && standing.getHeight()==52 && hanging.getWidth()==32,"HD sheet scaling retained");
                    check(standing.getPixel(0,0)==source.getPixel(4,4) && standing.getPixel(22,24)==source.getPixel(4,32),"Board and centered post sample correct legacy UVs");
                    check(standing.getPixel(0,51)==0 && hanging.getPixel(0,0)==0,"Outside the board remains transparent");
                    check(hanging.getPixel(4,0)==source.getPixel(28,12) && hanging.getPixel(2,12)==source.getPixel(4,28),"Hanging chain/board sample correct regions");
                }
            }
            ResourceManager original=client.getResourceManager();
            Identifier target=SignEditorTexture.editorId("larch",false);
            ResourceManager override=(ResourceManager)Proxy.newProxyInstance(N5GuiParticleProbe.class.getClassLoader(),new Class<?>[]{ResourceManager.class},(proxy,method,args)-> {
                if(method.getName().equals("getResource") && target.equals(args[0]))return original.getResource(Identifier.parse("minecraft:textures/gui/signs/oak.png"));
                if(method.isDefault())return InvocationHandler.invokeDefault(proxy,method,args);
                return method.invoke(original,args);
            });
            try(var explicit=new SignEditorTexture("larch",false).loadContents(override);
                var vanilla=TextureContents.load(original,Identifier.parse("minecraft:textures/gui/signs/oak.png"))) {
                check(Arrays.equals(explicit.image().getPixels(),vanilla.image().getPixels()),"Explicit native-layout pack image overrides conversion");
            }
        } catch(java.io.IOException e) { throw new AssertionError(e); }
        var field=new FilterSearchField(client.font,10,10,160,12);
        var changes=new ArrayList<String>();field.setResponder(changes::add);field.setFocused(true);
        check(field.keyPressed(new KeyEvent(GLFW.GLFW_KEY_E,0,0)),"Inventory key belongs to focused search field");
        field.charTyped(new CharacterEvent('e'));field.charTyped(new CharacterEvent(0x8702));field.insertText(" paste");
        check(field.getValue().equals("e蜂 paste") && changes.getLast().equals(field.getValue()),"Unicode and pasted text update filtering");
        check(!field.keyPressed(new KeyEvent(GLFW.GLFW_KEY_ESCAPE,0,0)),"Escape remains available to close the screen");
        field.setVisible(false);check(!field.keyPressed(new KeyEvent(GLFW.GLFW_KEY_E,0,0)),"Hidden field does not swallow inventory keys");field.setVisible(true);
        var state=new GuiRenderState();var graphics=new GuiGraphicsExtractor(client,state,0,0);
        field.extractRenderState(graphics,0,0,0);var text=new ArrayList<Object>();state.forEachText(text::add);
        check(!text.isEmpty(),"Opaque search text is actually extracted into the GUI state");
        var ignition=new Ignition();var smoke=new Smoke();
        check(ignition.getGroup()==ParticleRenderType.NO_RENDER && smoke.getGroup()==ParticleRenderType.NO_RENDER,"Emitters use the native nonvisual group");
        for(int tick=0;tick<100;tick++){ignition.tick();smoke.tick();}
        check(!ignition.isAlive() && !smoke.isAlive() && ignition.emitted>0 && smoke.emitted>0,"Both production emitters emit visible particles then expire");
        int count=ignition.emitted;ignition.tick();check(ignition.emitted==count,"Expired emitter cannot spawn more smoke");
        LogUtils.getLogger().info("N5 GUI/particle probe passed (pass {}): 86 editor textures/reload, HD UVs/transparency/pack override, Unicode/paste/filter key routing/text extraction, both emitter lifecycles (collision-free fixtures)",++passes);
    }
    private static final class Ignition extends ParticleIgnition {
        int emitted;
        Ignition(){super(null,0,0,0);this.hasPhysics=false;this.random.setSeed(42);}
        @Override protected void emitSmoke(SimpleParticleType type){check(type==ParticleTypes.SMOKE,"Ignition smoke type");emitted++;}
    }
    private static final class Smoke extends ParticleSmoke {
        int emitted;
        Smoke(){super(null,0,0,0);this.hasPhysics=false;this.random.setSeed(42);}
        @Override protected void emitSmoke(SimpleParticleType type){check(type==ParticleTypes.LARGE_SMOKE,"Large smoke type");emitted++;}
    }
}
