package forestry.core.platform.util;

import com.mojang.authlib.GameProfile;
import forestry.core.platform.network.IStreamable;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.network.connection.ConnectionType;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public abstract class NBTUtilForestry {
	private static final String GAME_PROFILE_ID = "Id";
	private static final String GAME_PROFILE_NAME = "Name";

	public static CompoundTag writeStreamableToNbt(IStreamable streamable, CompoundTag nbt, HolderLookup.Provider registries) {
		return writeStreamableToNbt(streamable, nbt, (RegistryAccess) registries);
	}

	public static CompoundTag writeStreamableToNbt(IStreamable streamable, CompoundTag nbt, RegistryAccess registries) {
		RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
		try {
			streamable.writeData(data);
			byte[] bytes = new byte[data.readableBytes()];
			data.getBytes(data.readerIndex(), bytes);
			nbt.putByteArray("dataBytes", bytes);
			return nbt;
		} finally {
			data.release();
		}
	}

	public static void readStreamableFromNbt(IStreamable streamable, CompoundTag nbt, HolderLookup.Provider registries) {
		readStreamableFromNbt(streamable, nbt, (RegistryAccess) registries);
	}

	public static void readStreamableFromNbt(IStreamable streamable, CompoundTag nbt, RegistryAccess registries) {
		nbt.getByteArray("dataBytes").ifPresent(bytes -> {
			RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes), registries, ConnectionType.NEOFORGE);
			try {
				streamable.readData(data);
			} finally {
				data.release();
			}
		});
	}

	public static void putBlockPos(CompoundTag nbt, String key, BlockPos pos) {
		nbt.putIntArray(key, new int[]{pos.getX(), pos.getY(), pos.getZ()});
	}

	public static Optional<BlockPos> getBlockPos(CompoundTag nbt, String key) {
		return nbt.getIntArray(key).flatMap(arr -> arr.length == 3 ? Optional.of(new BlockPos(arr[0], arr[1], arr[2])) : Optional.empty());
	}

	@Nullable
	public static GameProfile readGameProfile(CompoundTag nbt) {
		if (nbt.isEmpty()) {
			return null;
		}

		UUID id = nbt.getIntArray(GAME_PROFILE_ID).filter(arr -> arr.length == 4).map(UUIDUtil::uuidFromIntArray).orElse(null);
		String name = nbt.getString(GAME_PROFILE_NAME).orElse(null);
		if (id == null && (name == null || name.isEmpty())) {
			return null;
		}
		// authlib rejects a null id or name. A half-written profile is a name-only trade station address
		// or an id-only owner, so fill the missing half with the value the rest of the mod reads as absent
		return new GameProfile(id == null ? Util.NIL_UUID : id, name == null ? "" : name);
	}

	public static CompoundTag writeGameProfile(CompoundTag nbt, GameProfile profile) {
		if (profile.id() != null) {
			nbt.putIntArray(GAME_PROFILE_ID, UUIDUtil.uuidToIntArray(profile.id()));
		}
		if (profile.name() != null) {
			nbt.putString(GAME_PROFILE_NAME, profile.name());
		}
		return nbt;
	}

	@Nullable
	public static CompoundTag getItemStackTag(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		return customData == null || customData.isEmpty() ? null : customData.copyTag();
	}

	public static void setItemStackTag(ItemStack stack, CompoundTag tag) {
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
	}
}
