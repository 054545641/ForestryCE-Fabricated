package forestry.mail.letters;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import forestry.api.ForestryRegistries;
import forestry.core.platform.util.NBTUtilForestry;
import forestry.api.mail.IMailAddress;
import forestry.api.mail.IPostalCarrier;
import forestry.core.platform.util.PlayerUtil;
import forestry.mail.carriers.PostalCarriers;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class MailAddress implements IMailAddress {
	public static final Codec<MailAddress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Identifier.CODEC.fieldOf("carrier").forGetter(address -> ForestryRegistries.POSTAL_CARRIER.getKey(address.getCarrier())),
		Codec.STRING.optionalFieldOf("name", "").forGetter(MailAddress::getName),
		UUIDUtil.CODEC.optionalFieldOf("id").forGetter(MailAddress::getPlayerId)
	).apply(instance, MailAddress::fromCodec));

	// Traders have no player UUID. authlib rejects a null id, and PlayerUtil treats this one as absent,
	// so it is how an address says "identified by name alone"
	private static final UUID NO_ID = new UUID(0, 0);

	private static final GameProfile INVALID_GAME_PROFILE = new GameProfile(NO_ID, "");

	public static final MailAddress INVALID = new MailAddress(null, INVALID_GAME_PROFILE);

	@Nullable
	private final IPostalCarrier carrier;
	private final GameProfile gameProfile; // gameProfile is a fake GameProfile for traders, and real for players

	public MailAddress(GameProfile gameProfile) {

		this.carrier = PostalCarriers.PLAYER.value();
		this.gameProfile = gameProfile;
	}

	// used by trading stations
	public MailAddress(String name) {
		Preconditions.checkNotNull(name, "name must not be null");
		Preconditions.checkArgument(StringUtils.isNotBlank(name), "name must not be blank");

		this.carrier = PostalCarriers.TRADER.value();
		this.gameProfile = new GameProfile(NO_ID, name);
	}

	public MailAddress(CompoundTag nbt) {
		IPostalCarrier carrier = null;
		GameProfile gameProfile = INVALID_GAME_PROFILE;
		if (nbt.contains("carrier")) {
			Identifier carrierId = Identifier.tryParse(nbt.getStringOr("carrier", ""));
			carrier = carrierId == null ? null : ForestryRegistries.POSTAL_CARRIER.getValue(carrierId);
		}

		if (carrier == null) {
			carrier = PostalCarriers.PLAYER.value();
		} else if (nbt.contains("profile")) {
			CompoundTag profileTag = nbt.getCompoundOrEmpty("profile");
			gameProfile = NBTUtilForestry.readGameProfile(profileTag);
			if (gameProfile == null) {
				gameProfile = INVALID_GAME_PROFILE;
			}
		}

		this.carrier = carrier;
		this.gameProfile = gameProfile;
	}

	private MailAddress(@Nullable IPostalCarrier carrier, GameProfile gameProfile) {
		this.carrier = carrier;
		this.gameProfile = gameProfile;
	}

	private static MailAddress fromCodec(Identifier carrierId, String name, Optional<UUID> id) {
		IPostalCarrier carrier = ForestryRegistries.POSTAL_CARRIER.getValue(carrierId);
		if (carrier == null) {
			carrier = PostalCarriers.PLAYER.value();
		}

		GameProfile profile;
		if (StringUtils.isBlank(name)) {
			profile = INVALID_GAME_PROFILE;
		} else {
			profile = new GameProfile(id.orElse(NO_ID), name);
		}
		return new MailAddress(carrier, profile);
	}

	public static MailAddress copyOf(IMailAddress address) {
		if (address instanceof MailAddress mailAddress) {
			return mailAddress;
		}

		GameProfile profile = address.getCarrier().equals(PostalCarriers.PLAYER.value())
			? address.getPlayerProfile()
			: new GameProfile(NO_ID, address.getName());
		return new MailAddress(address.getCarrier(), profile);
	}

	// Empty for traders, which are identified by name alone
	private Optional<UUID> getPlayerId() {
		UUID id = this.gameProfile.id();
		return id == null || id.equals(NO_ID) ? Optional.empty() : Optional.of(id);
	}

	@Override
	public IPostalCarrier getCarrier() {
		return this.carrier == null ? PostalCarriers.PLAYER.value() : this.carrier;
	}

	@Override
	public String getName() {
		return this.gameProfile.name();
	}

	@Override
	public boolean isValid() {
		return this.gameProfile.name() != null && !PlayerUtil.isSameGameProfile(this.gameProfile, INVALID_GAME_PROFILE);
	}

	@Override
	public GameProfile getPlayerProfile() {
		if (!getCarrier().equals(PostalCarriers.PLAYER.value())) {
			return INVALID_GAME_PROFILE;
		}
		return this.gameProfile;
	}

	@Override
	public int hashCode() {
		return this.gameProfile.name().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MailAddress address)) {
			return false;
		}

		return PlayerUtil.isSameGameProfile(address.gameProfile, this.gameProfile);
	}

	@Override
	public String toString() {
		String name = getName().toLowerCase(Locale.ENGLISH);
		if (getCarrier().equals(PostalCarriers.PLAYER.value())) {
			return getCarrier() + "-" + name + '-' + this.gameProfile.id();
		} else {
			return getCarrier() + "-" + name;
		}
	}

	@Override
	public CompoundTag write(CompoundTag compoundNBT, HolderLookup.Provider registries) {
		compoundNBT.putString("carrier", ForestryRegistries.POSTAL_CARRIER.getKey(getCarrier()).toString());

		if (this.gameProfile != INVALID_GAME_PROFILE) {
			CompoundTag profileNbt = new CompoundTag();
			NBTUtilForestry.writeGameProfile(profileNbt, this.gameProfile);
			compoundNBT.put("profile", profileNbt);
		}
		return compoundNBT;
	}


}
