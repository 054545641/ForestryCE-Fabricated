package forestry.core.content.escritoire;

import forestry.api.IForestryApi;
import forestry.api.client.ForestrySprites;
import forestry.api.core.INbtWritable;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.core.platform.network.IStreamable;
import forestry.core.platform.util.ColourUtil;
import forestry.core.platform.util.NetworkUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class EscritoireGameToken implements INbtWritable, IStreamable {
	public static final String NBT_TOKEN_SPECIES = "tokenSpecies";
	public static final String NBT_TOKEN_TYPE = "tokenSpeciesType";

	private enum State {
		UNREVEALED,// face down
		PROBED,    // shown by escritoire probe action
		SELECTED,  // selected by the user as the first half of a match
		MATCHED,   // successfully matched
		FAILED;    // failed to match
		public static final State[] VALUES = values();
	}

	@Nullable
	private ISpeciesType<? extends ISpecies<?>, ?> tokenType;
	@Nullable
	private IIndividual tokenIndividual;
	private ItemStack tokenStack = ItemStack.EMPTY;

	private State state = State.UNREVEALED;

	public EscritoireGameToken(RegistryFriendlyByteBuf data) {
		readData(data);
	}

	public EscritoireGameToken(ISpecies<?> species) {
		setTokenSpecies(species);
	}

	public EscritoireGameToken(CompoundTag nbt) {
		readFromTag(nbt);
	}

	private void setTokenSpecies(Identifier typeId, Identifier speciesId) {
		if (this.tokenType != null && typeId == this.tokenType.id()) {
			setTokenSpecies(this.tokenType.getSpecies(speciesId));
		} else {
			ISpeciesType<?, ?> type = IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(typeId);
			setTokenSpecies(type.getSpecies(speciesId));
		}
	}

	private void setTokenSpecies(ISpecies<?> species) {
		this.tokenIndividual = species.createIndividual();
		this.tokenType = species.getType();
		this.tokenStack = species.createStack(species.getType().getDefaultStage());
	}

	public ItemStack getTokenStack() {
		return this.tokenStack;
	}

	public boolean isVisible() {
		return this.state != State.UNREVEALED;
	}

	public boolean isProbed() {
		return this.state == State.PROBED;
	}

	public boolean isMatched() {
		return this.state == State.MATCHED;
	}

	public boolean isSelected() {
		return this.state == State.SELECTED;
	}

	public void setFailed() {
        this.state = State.FAILED;
	}

	public void setProbed(boolean probed) {
		if (probed) {
            this.state = State.PROBED;
		} else {
            this.state = State.UNREVEALED;
		}
	}

	public void setSelected() {
        this.state = State.SELECTED;
	}

	public void setMatched() {
        this.state = State.MATCHED;
	}

	public int getTokenColour() {
		if (this.tokenIndividual == null || !isVisible()) {
			return 0xffffff;
		}

		int iconColor = this.tokenIndividual.getSpecies().getEscritoireColor();

		if (this.state == State.MATCHED) {
			return ColourUtil.multiplyRGBComponents(iconColor, 0.7f);
		} else {
			return iconColor;
		}
	}


	public Component getTooltip() {
		return !this.tokenStack.isEmpty() ? this.tokenStack.getHoverName() : Component.translatable("for.gui.unknown");
	}

	@Nullable
	public Identifier getOverlayToken() {
		return switch (this.state) {
			case FAILED -> ForestrySprites.ERROR_ERRORED;
			case SELECTED -> ForestrySprites.ERROR_UNKNOWN;
			default -> null;
		};
	}

	public boolean matches(EscritoireGameToken other) {
		return ItemStack.matches(this.tokenStack, other.getTokenStack());
	}

	@Override
	public CompoundTag write(CompoundTag nbt, HolderLookup.Provider registries) {
		nbt.putInt("state", this.state.ordinal());

		if (this.tokenIndividual != null && this.tokenType != null) {
			nbt.putString(NBT_TOKEN_SPECIES, this.tokenIndividual.getSpecies().id().toString());
			nbt.putString(NBT_TOKEN_TYPE, this.tokenType.id().toString());
		}
		return nbt;
	}

	private void readFromTag(CompoundTag nbt) {
		if (nbt.contains("state")) {
			int stateOrdinal = nbt.getIntOr("state", 0);
			this.state = State.VALUES[stateOrdinal];
		}

		String tokenSpecies = nbt.getStringOr(NBT_TOKEN_SPECIES, "");
		String tokenType = nbt.getStringOr(NBT_TOKEN_TYPE, "");

		if (!tokenSpecies.isEmpty() && !tokenType.isEmpty()) {
			setTokenSpecies(Identifier.parse(tokenType), Identifier.parse(tokenSpecies));
		}
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		NetworkUtil.writeEnum(data, this.state);
		if (this.tokenIndividual != null && this.tokenType != null) {
			data.writeBoolean(true);
			data.writeIdentifier(this.tokenIndividual.getSpecies().id());
			data.writeIdentifier(this.tokenType.id());
		} else {
			data.writeBoolean(false);
		}
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		this.state = NetworkUtil.readEnum(data, State.VALUES);
		if (data.readBoolean()) {
			Identifier speciesId = data.readIdentifier();
			Identifier typeId = data.readIdentifier();
			setTokenSpecies(typeId, speciesId);
		}
	}
}
