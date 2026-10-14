package forestry.environment;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import forestry.mail.client.StampTintSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;

/** Verifies production mail assets in a real resource reload, without claiming mail gameplay coverage. */
public final class MailClientAssetProbe {
    public static void verify(Minecraft client) {
        var converter = new FileToIdConverter("items", ".json");
        var definitions = converter.listMatchingResources(client.getResourceManager());
        int items = 0;
        int stamps = 0;
        for (var entry : definitions.entrySet()) {
            if (!entry.getKey().getNamespace().equals("forestry")) continue;
            String name = converter.fileToId(entry.getKey()).getPath();
            if (!(name.startsWith("letter_") || name.startsWith("stamp_") || java.util.Set.of("catalog", "mailbox", "trade_station").contains(name))) continue;
            try (var reader = entry.getValue().openAsReader()) {
                var definition = ClientItem.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
                if (!(definition.model() instanceof CuboidItemModelWrapper.Unbaked model)) {
                    throw new AssertionError("Unexpected mail item model: " + entry.getKey());
                }
                if (name.matches("stamp_[0-9]+n")) {
                    if (model.tints().size() != 2 || !model.tints().get(0).equals(new StampTintSource(0))
                        || !model.tints().get(1).equals(new StampTintSource(1))) {
                        throw new AssertionError("Invalid stamp tint layers: " + entry.getKey());
                    }
                    stamps++;
                } else {
                    if (model.tints().size() != 5) throw new AssertionError("Invalid generic tint layer count: " + entry.getKey());
                    for (int layer = 0; layer < 5; layer++) {
                        if (!model.tints().get(layer).equals(new forestry.core.platform.models.ForestryItemTintSource(layer))) {
                            throw new AssertionError("Invalid generic tint layer: " + entry.getKey() + "/" + layer);
                        }
                    }
                }
                if (!(client.getModelManager().getItemModel(converter.fileToId(entry.getKey())) instanceof CuboidItemModelWrapper)) {
                    throw new AssertionError("Mail model failed to bake: " + entry.getKey());
                }
                items++;
            } catch (java.io.IOException exception) {
                throw new java.io.UncheckedIOException(exception);
            }
        }
        if (items != 23 || stamps != 7) throw new AssertionError("Mail assets: " + items + " items / " + stamps + " stamps");
        var guiAtlas = client.getAtlasManager().getAtlasOrThrow(AtlasIds.GUI);
        for (String name : new String[] {"blocked", "blocked_2", "bee", "cocoon", "container", "liquid", "locked"}) {
            var id = Identifier.fromNamespaceAndPath("forestry", "container/slots/" + name);
            if (!guiAtlas.getSprite(id).contents().name().equals(id)) {
                throw new AssertionError("Missing slot sprite " + id);
            }
        }
        var forestryAtlas = new forestry.core.platform.render.ForestrySpriteUploader();
        for (String carrier : new String[] {"player", "trader"}) {
            var id = Identifier.fromNamespaceAndPath("forestry", "mail/carrier." + carrier);
            if (!forestryAtlas.getSprite(id).contents().name().equals(id)) throw new AssertionError("Missing carrier sprite " + id);
        }
        com.mojang.logging.LogUtils.getLogger().info("Mail client asset probe passed: 23 baked item definitions, 7 two-layer stamp tints, 7 GUI slot sprites, 2 carrier sprites through the production atlas adapter");
    }
}
