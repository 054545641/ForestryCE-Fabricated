package forestry.core.platform.owner;

import com.mojang.authlib.GameProfile;
import forestry.api.core.INbtReadable;
import forestry.api.core.INbtWritable;
import forestry.core.platform.network.IStreamable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.UUID;

public class OwnerHandler implements IOwnerHandler, IStreamable, INbtWritable, INbtReadable {
	@Nullable
	private GameProfile owner = null;

	@Override
	@Nullable
	public GameProfile getOwner() {
		return this.owner;
	}

	@Override
	public void setOwner(GameProfile owner) {
		this.owner = owner;
	}

	@Override
	public void writeData(RegistryFriendlyByteBuf data) {
		if (this.owner == null) {
			data.writeBoolean(false);
		} else {
			data.writeBoolean(true);
			data.writeLong(this.owner.id().getMostSignificantBits());
			data.writeLong(this.owner.id().getLeastSignificantBits());
			data.writeUtf(this.owner.name());
		}
	}

	@Override
	public void readData(RegistryFriendlyByteBuf data) {
		if (data.readBoolean()) {
			GameProfile owner = new GameProfile(new UUID(data.readLong(), data.readLong()), data.readUtf());
			setOwner(owner);
		} else {
			this.owner = null;
		}
	}

	@Override
	public void read(CompoundTag data, HolderLookup.Provider registries) {
		if (data.contains("owner_name")) {
			UUID id = data.getIntArray("owner_uuid").filter(arr -> arr.length == 4).map(UUIDUtil::uuidFromIntArray).orElse(net.minecraft.util.Util.NIL_UUID);
			setOwner(new GameProfile(id, data.getStringOr("owner_name", "")));
		} else {
			this.owner = null;
		}
	}

	@Override
	public CompoundTag write(CompoundTag data, HolderLookup.Provider registries) {
		if (this.owner != null) {
			if (this.owner.id() != null) {
				data.putIntArray("owner_uuid", UUIDUtil.uuidToIntArray(this.owner.id()));
			}
			data.putString("owner_name", this.owner.name());
		}
		return data;
	}
}
