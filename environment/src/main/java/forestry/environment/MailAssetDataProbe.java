package forestry.environment;

import forestry.mail.data.MailItemDefinitionProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;

/** Runs the production definition writer against the checked-in mail model inventory. */
public final class MailAssetDataProbe {
    public static MailItemDefinitionProvider provider(Path root) throws java.io.IOException {
        var items = new LinkedHashMap<Identifier, Boolean>();
        for (String source : new String[] {"src/mail/resources", "src/generated/resources_mail"}) {
            try (var models = Files.list(root.resolve(source + "/assets/forestry/models/item"))) {
                models.filter(path -> path.toString().endsWith(".json")).sorted().forEach(path -> {
                    String name = path.getFileName().toString().replace(".json", "");
                    items.put(Identifier.fromNamespaceAndPath("forestry", name), name.startsWith("stamp_"));
                });
            }
        }
        // stamp_collector is a block item, not one of the two-layer postage stamps.
        items.put(Identifier.fromNamespaceAndPath("forestry", "stamp_collector"), false);
        if (items.size() != 23 || items.values().stream().filter(Boolean::booleanValue).count() != 7) {
            throw new AssertionError("Unexpected mail model inventory: " + items);
        }
        return new MailItemDefinitionProvider(new PackOutput(root.resolve("src/generated/resources_mail")), items);
    }
}
