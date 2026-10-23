package forestry.core.platform.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Bounded native Fabric extended-menu data. */
public record MenuOpeningData(byte[] bytes) {
    public static final int MAX_BYTES = 4096;
    public MenuOpeningData { if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Menu data too large"); bytes = bytes.clone(); }
    @Override public byte[] bytes() { return bytes.clone(); }
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuOpeningData> CODEC = StreamCodec.of(
        (buffer, value) -> buffer.writeByteArray(value.bytes), buffer -> new MenuOpeningData(buffer.readByteArray(MAX_BYTES)));
}
