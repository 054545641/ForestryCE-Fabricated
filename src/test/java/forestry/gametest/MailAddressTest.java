package forestry.gametest;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.mail.IMailAddress;
import forestry.api.mail.IPostalCarrier;
import forestry.mail.carriers.PostalCarriers;
import forestry.mail.letters.MailAddress;

/**
 * Guards the trade station side of {@link MailAddress}. Trader addresses carry no player UUID, so the
 * name is the only identity they have. Every path that drops it silently unnames every trade station in
 * the world and collapses them onto one map key.
 */
@GameTestHolder(ForestryConstants.MOD_ID)
@PrefixGameTestTemplate(false)
public class MailAddressTest {
	@GameTest(template = "empty")
	public static void traderNameSurvivesConstruction(GameTestHelper helper) {
		MailAddress address = new MailAddress("Emporium");

		if (!address.getName().equals("Emporium")) {
			helper.fail("Trader address dropped its name: got '" + address.getName() + "'");
			return;
		}
		if (!address.isValid()) {
			helper.fail("Trader address with a name reports itself invalid");
			return;
		}
		if (!address.getCarrier().equals(PostalCarriers.TRADER.value())) {
			helper.fail("Trader address lost its carrier");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void tradersWithDifferentNamesAreDistinct(GameTestHelper helper) {
		MailAddress first = new MailAddress("Emporium");
		MailAddress second = new MailAddress("Bazaar");

		if (first.equals(second)) {
			helper.fail("Two differently named trade stations compare equal");
			return;
		}

		// TradeStationRegistry keys its stations by address, so a shared hash bucket plus equals() is a merge
		Map<IMailAddress, String> stations = new HashMap<>();
		stations.put(first, "first");
		stations.put(second, "second");
		if (stations.size() != 2) {
			helper.fail("Two trade stations collapsed onto one map key");
			return;
		}
		if (!"first".equals(stations.get(new MailAddress("Emporium")))) {
			helper.fail("Trade station lookup by an equal address failed");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void traderNbtRoundTrip(GameTestHelper helper) {
		HolderLookup.Provider registries = helper.getLevel().registryAccess();
		MailAddress original = new MailAddress("Emporium");

		MailAddress restored = new MailAddress(original.write(new CompoundTag(), registries));

		if (!restored.getName().equals("Emporium")) {
			helper.fail("NBT round-trip dropped the trader name: got '" + restored.getName() + "'");
			return;
		}
		if (!restored.getCarrier().equals(PostalCarriers.TRADER.value())) {
			helper.fail("NBT round-trip dropped the trader carrier");
			return;
		}
		if (!restored.equals(original)) {
			helper.fail("NBT round-trip produced an unequal address");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void traderCodecRoundTrip(GameTestHelper helper) {
		MailAddress original = new MailAddress("Emporium");

		Tag encoded = MailAddress.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
		MailAddress decoded = MailAddress.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();

		if (!decoded.getName().equals("Emporium")) {
			helper.fail("Codec round-trip dropped the trader name: " + encoded);
			return;
		}
		if (!decoded.getCarrier().equals(PostalCarriers.TRADER.value())) {
			helper.fail("Codec round-trip dropped the trader carrier: " + encoded);
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void traderProfileIsNotAPlayerProfile(GameTestHelper helper) {
		MailAddress trader = new MailAddress("Emporium");
		MailAddress player = new MailAddress(new GameProfile(UUID.nameUUIDFromBytes("Steve".getBytes(StandardCharsets.UTF_8)), "Steve"));

		if (!trader.getPlayerProfile().getName().isEmpty()) {
			helper.fail("Trader address handed out a player profile");
			return;
		}
		if (!player.getName().equals("Steve")) {
			helper.fail("Player address dropped its name");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void legacyTraderNbtWithoutIdLoads(GameTestHelper helper) {
		// Addresses written before the profile gained a UUID have a Name and nothing else
		CompoundTag profile = new CompoundTag();
		profile.putString("Name", "Emporium");
		CompoundTag nbt = new CompoundTag();
		nbt.putString("carrier", "forestry:trader");
		nbt.put("profile", profile);

		MailAddress restored = new MailAddress(nbt);

		if (!restored.getName().equals("Emporium")) {
			helper.fail("Legacy trader NBT lost its name: got '" + restored.getName() + "'");
			return;
		}
		helper.succeed();
	}

	@GameTest(template = "empty")
	public static void copyOfForeignTraderKeepsName(GameTestHelper helper) {
		MailAddress copy = MailAddress.copyOf(new ForeignTraderAddress("Emporium"));

		if (!copy.getName().equals("Emporium")) {
			helper.fail("copyOf dropped the trader name: got '" + copy.getName() + "'");
			return;
		}
		helper.succeed();
	}

	// Not a MailAddress, so copyOf has to rebuild it rather than hand the instance back
	private record ForeignTraderAddress(String name) implements IMailAddress {
		@Override
		public IPostalCarrier getCarrier() {
			return PostalCarriers.TRADER.value();
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public boolean isValid() {
			return true;
		}

		@Override
		public GameProfile getPlayerProfile() {
			return MailAddress.INVALID.getPlayerProfile();
		}

		@Override
		public CompoundTag write(CompoundTag nbt, HolderLookup.Provider registries) {
			return nbt;
		}
	}
}
