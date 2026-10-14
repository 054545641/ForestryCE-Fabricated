package forestry.core.engine.circuits;

import forestry.api.IForestryApi;
import forestry.api.core.circuits.ICircuit;
import forestry.api.core.circuits.ICircuitBoard;
import forestry.api.core.circuits.ICircuitLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CircuitBoard implements ICircuitBoard {
	private final EnumCircuitBoardType type;
	@Nullable
	private final ICircuitLayout layout;
	private final ICircuit[] circuits;

	public CircuitBoard(EnumCircuitBoardType type, @Nullable ICircuitLayout layout, ICircuit[] circuits) {
		this.type = type;
		this.layout = layout;
		this.circuits = circuits;
	}

	public CircuitBoard(CompoundTag compound) {
        this.type = EnumCircuitBoardType.values()[compound.getShortOr("T", (short) 0)];

		// Layout
		ICircuitLayout layout = null;
		if (compound.contains("LY")) {
			layout = IForestryApi.INSTANCE.getCircuitManager().getLayout(compound.getStringOr("LY", ""));
		}
		this.layout = layout;

        this.circuits = new ICircuit[4];

		for (int i = 0; i < 4; i++) {
			if (!compound.contains("CA.I" + i)) {
				continue;
			}
			ICircuit circuit = IForestryApi.INSTANCE.getCircuitManager().getCircuit(compound.getStringOr("CA.I" + i, ""));
			if (circuit != null) {
                this.circuits[i] = circuit;
			}
		}
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void addTooltip(List<Component> list) {
		if (this.layout != null) {
			list.add(Component.literal(this.layout.getUsage().getString() + ":").withStyle(ChatFormatting.GOLD));
			List<Component> extendedTooltip = new ArrayList<>();
			for (ICircuit circuit : this.circuits) {
				if (circuit != null) {
					circuit.addTooltip(extendedTooltip);
				}
			}

			if (net.minecraft.client.Minecraft.getInstance().hasShiftDown() || extendedTooltip.size() <= 4) {
				list.addAll(extendedTooltip);
			} else {
				list.add(Component.literal("<").withStyle(ChatFormatting.UNDERLINE, ChatFormatting.GRAY)
					.append(Component.translatable("for.gui.tooltip.tmi"))
					.append(Component.literal(">")));
			}
		} else {
			int socketCount = this.type.getSockets();
			String localizationKey = "item.forestry.circuit_board.tooltip." + (socketCount == 1 ? "singular" : "plural");
			list.add(Component.translatable(localizationKey, this.type.getSockets()).withStyle(ChatFormatting.GRAY));
		}
	}

	@Override
	public CompoundTag write(CompoundTag compound, HolderLookup.Provider registries) {

		compound.putShort("T", (short) this.type.ordinal());

		// Layout
		if (this.layout != null) {
			compound.putString("LY", this.layout.getId());
		}

		// Circuits
		for (int i = 0; i < this.circuits.length; i++) {
			ICircuit circuit = this.circuits[i];
			if (circuit == null) {
				continue;
			}

			compound.putString("CA.I" + i, circuit.getId());
		}
		return compound;
	}

	@Override
	public void onInsertion(Object tile) {
		for (int i = 0; i < this.circuits.length; i++) {
			ICircuit circuit = this.circuits[i];
			if (circuit == null) {
				continue;
			}
			circuit.onInsertion(i, tile);
		}
	}

	@Override
	public void onLoad(Object tile) {
		for (int i = 0; i < this.circuits.length; i++) {
			ICircuit circuit = this.circuits[i];
			if (circuit == null) {
				continue;
			}
			circuit.onLoad(i, tile);
		}
	}

	@Override
	public void onRemoval(Object tile) {
		for (int i = 0; i < this.circuits.length; i++) {
			ICircuit circuit = this.circuits[i];
			if (circuit == null) {
				continue;
			}
			circuit.onRemoval(i, tile);
		}
	}

	@Override
	public void onTick(Object tile) {
		for (int i = 0; i < this.circuits.length; i++) {
			ICircuit circuit = this.circuits[i];
			if (circuit == null) {
				continue;
			}
			circuit.onTick(i, tile);
		}
	}

	@Override
	public ICircuit[] getCircuits() {
		return this.circuits;
	}

	@Nullable
	@Override
	public Identifier getSocketType() {
		if (this.layout == null) {
			return null;
		}
		return this.layout.getSocketType();
	}
}
