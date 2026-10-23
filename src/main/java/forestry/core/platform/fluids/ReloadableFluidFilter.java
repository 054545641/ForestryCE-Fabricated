package forestry.core.platform.fluids;
import net.minecraft.resources.Identifier;
import java.util.Set;
import java.util.function.Supplier;
/** Resolve from the current logical side on every access, including datapack reloads. */
public class ReloadableFluidFilter implements Supplier<Set<Identifier>> {
    private final Supplier<Set<Identifier>> filterGetter;
    public ReloadableFluidFilter(Supplier<Set<Identifier>> filterGetter) { this.filterGetter = filterGetter; }
    @Override public Set<Identifier> get() { return Set.copyOf(filterGetter.get()); }
}
