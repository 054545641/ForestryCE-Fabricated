package forestry.core.engine.circuits;

import com.google.common.collect.*;
import forestry.api.*;
import forestry.api.core.circuits.*;
import forestry.apiimpl.ForestryApiImpl;
import forestry.core.content.energy.circuits.CircuitEngineUpgrade;
import forestry.core.content.machines.circuits.CircuitMachineUpgrade;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;

/** Original core circuit values, with lazy native stack templates for the registration phase. */
public final class FabricCircuits {
    private FabricCircuits() {}
    public static void initialize(java.util.List<forestry.api.plugin.IForestryPlugin> plugins) {
        ICircuitLayout machine = new CircuitLayout(ForestryCircuitLayouts.MACHINE_UPGRADE, ForestryCircuitSocketTypes.MACHINE);
        ICircuitLayout engine = new CircuitLayout(ForestryCircuitLayouts.ENGINE_UPGRADE, ForestryCircuitSocketTypes.ENGINE);
        var layouts = new java.util.LinkedHashMap<String, ICircuitLayout>(); layouts.put(machine.getId(), machine); layouts.put(engine.getId(), engine);
        var holders = ImmutableMultimap.<ICircuitLayout, CircuitHolder>builder();
        var circuits = ImmutableMap.<String, ICircuit>builder();
        add(holders, circuits, machine, "emerald", new CircuitMachineUpgrade("machine.speed.boost.1", 0.125f, 0.05f, 1f));
        add(holders, circuits, machine, "blaze", new CircuitMachineUpgrade("machine.speed.boost.2", 0.250f, 0.10f, 1f));
        add(holders, circuits, machine, "gold", new CircuitMachineUpgrade("machine.efficiency.1", 0, -0.10f, 1f));
        add(holders, circuits, machine, "amber", new CircuitMachineUpgrade("machine.fortune.1", 0, 0.05f, 1.25f));
        add(holders, circuits, engine, "gold", new CircuitEngineUpgrade("engine.speed.boost", 1, -0.15f, 0));
        add(holders, circuits, engine, "copper", new CircuitEngineUpgrade("engine.speed.choke", -5, 0.3f, 1));
        add(holders, circuits, engine, "lapis", new CircuitEngineUpgrade("engine.efficiency.1", 0, 0.1f, 0));
        add(holders, circuits, engine, "diamond", new CircuitEngineUpgrade("engine.efficiency.2", 0, 0.2f, 0));
        add(holders, circuits, engine, "apatite", new CircuitEngineUpgrade("engine.biogas.overclock", 0, -0.2f, 2));
        var extra = new forestry.apiimpl.plugin.CircuitRegistration();
        plugins.forEach(plugin -> plugin.registerCircuits(extra));
        for (var layout : extra.getLayouts()) if (layouts.putIfAbsent(layout.getId(), layout) != null) throw new IllegalArgumentException("Duplicate circuit layout " + layout.getId());
        for (var holder : extra.getCircuits()) {
            var layout = java.util.Objects.requireNonNull(layouts.get(holder.layoutId()), "Unknown circuit layout");
            var circuit = holder.circuit();
            Registry.register(ForestryRegistries.CIRCUIT, ForestryConstants.forestry(circuit.getId().substring("forestry.".length())), circuit);
            holders.put(layout, holder); circuits.put(circuit.getId(), circuit);
        }
        ((ForestryApiImpl) IForestryApi.INSTANCE).setCircuitManager(new CircuitManager(holders.build(), ImmutableMap.copyOf(layouts), circuits.build()));
    }
    private static void add(ImmutableMultimap.Builder<ICircuitLayout, CircuitHolder> holders, ImmutableMap.Builder<String, ICircuit> circuits,
            ICircuitLayout layout, String tube, ICircuit circuit) {
        var id = ForestryConstants.forestry(circuit.getId().substring("forestry.".length()));
        Registry.register(ForestryRegistries.CIRCUIT, id, circuit);
        var item = BuiltInRegistries.ITEM.getOrThrow(ResourceKey.create(Registries.ITEM, ForestryConstants.forestry(
            forestry.core.content.resources.EnumElectronTube.valueOf(tube.toUpperCase(java.util.Locale.ROOT)).registryPath())));
        holders.put(layout, new CircuitHolder(layout.getId(), new ItemStackTemplate(item.value(), 1), circuit));
        circuits.put(circuit.getId(), circuit);
    }
}
