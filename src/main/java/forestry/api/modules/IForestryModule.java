package forestry.api.modules;

import net.minecraft.resources.Identifier;
import java.util.List;

/**
 * Fabric module entrypoint, declared under "forestry:module" in the owning fabric.mod.json.
 * This is a source-breaking replacement of the NeoForge event-bus API. See docs/porting/f1-status.zh-CN.md.
 */
public interface IForestryModule {
    Identifier getId();
    String getOwnerModId();
    default List<Identifier> getModuleDependencies() { return List.of(); }
    default List<String> getModDependencies() { return List.of(); }
    void register();
}
