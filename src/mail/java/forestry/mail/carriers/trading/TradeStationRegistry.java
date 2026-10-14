package forestry.mail.carriers.trading;

import com.mojang.authlib.GameProfile;
import forestry.api.mail.IMailAddress;
import forestry.api.mail.ITradeStation;
import forestry.api.mail.IWatchable;
import forestry.mail.letters.MailAddress;
import forestry.mail.carriers.PostalCarriers;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.resources.Identifier;
import forestry.core.platform.util.SavedDataMigration;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TradeStationRegistry extends SavedData implements IWatchable.Watcher {
	private static final String SAVE_NAME = "forestry_trade_stations";

	public static final Pattern TRADE_STATION_NAME_REGEX = Pattern.compile("^[a-zA-Z0-9]+$");

	private final Map<IMailAddress, ITradeStation> cachedTradeStations = new HashMap<>();

	/**
	 * @param address the potential address of the Trader
	 * @return true if the passed address can be an address for a trade station
	 */
	public boolean isValidTradeAddress(IMailAddress address) {
		return address.getCarrier().equals(PostalCarriers.TRADER.value()) && TRADE_STATION_NAME_REGEX.matcher(address.getName()).matches();
	}

	/**
	 * @param address the potential address of the Trader
	 * @return true if the trade address has not yet been used before.
	 */
	public boolean isAvailableTradeAddress(IMailAddress address) {
		return getTradeStation(address) == null;
	}

	public void registerTradeStation(IMailAddress address, ITradeStation station) {
        this.cachedTradeStations.put(address, station);
		station.registerUpdateWatcher(this);
		setDirty();
	}

	@Nullable
	public ITradeStation getTradeStation(IMailAddress address) {
		if (this.cachedTradeStations.containsKey(address)) {
			return this.cachedTradeStations.get(address);
		}

		return null;
	}

	public ITradeStation getOrCreateTradeStation(GameProfile owner, IMailAddress address) {
		ITradeStation trade = getTradeStation(address);

		if (trade == null) {
			trade = new TradeStation(owner, address);
			registerTradeStation(address, trade);
			trade.setDirty();
		}

		return trade;
	}

	public void deleteTradeStation(IMailAddress address) {
		ITradeStation trade = getTradeStation(address);
		if (trade == null) {
			return;
		}
		trade.invalidate();
		trade.unregisterUpdateWatcher(this);
        this.cachedTradeStations.remove(address);
		setDirty();
	}

	public Map<IMailAddress, ITradeStation> getActiveTradeStations() {
		return this.cachedTradeStations;
	}

	@Override
	public void onWatchableUpdate() {
		setDirty();
	}

	private static TradeStationRegistry create() {
		return new TradeStationRegistry();
	}

	private static TradeStationRegistry load(CompoundTag compoundTag, HolderLookup.Provider registries) {
		TradeStationRegistry registry = new TradeStationRegistry();
		ListTag tradeStations = compoundTag.getList("tradeStations").orElseThrow(() -> new IllegalArgumentException("Missing tradeStations list"));
		for (int i = 0; i < tradeStations.size(); ++i) {
			CompoundTag stationTag = tradeStations.getCompound(i).orElseThrow(() -> new IllegalArgumentException("Invalid mail registry entry"));

			IMailAddress address = new MailAddress(stationTag.getCompound("address").orElseThrow(() -> new IllegalArgumentException("Missing mail address")));
			ITradeStation station = new TradeStation(stationTag.getCompound("station").orElseThrow(() -> new IllegalArgumentException("Missing station data")), registries);
			if (!address.isValid() || !address.getCarrier().equals(PostalCarriers.TRADER.value()) || registry.getTradeStation(address) != null) {
				throw new IllegalArgumentException("Invalid or duplicate mail address " + address);
			}
			registry.cachedTradeStations.put(address, station);
			station.registerUpdateWatcher(registry);
		}
		return registry;
	}

	public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider registries) {
		ListTag tradeStations = new ListTag();
		for (Map.Entry<IMailAddress, ITradeStation> entry : this.cachedTradeStations.entrySet()) {
			CompoundTag entryTag = new CompoundTag();
			entryTag.put("address", entry.getKey().write(new CompoundTag(), registries));
			entryTag.put("station", entry.getValue().write(new CompoundTag(), registries));
			tradeStations.add(entryTag);
		}
		compoundTag.put("tradeStations", tradeStations);
		return compoundTag;
	}

	public static TradeStationRegistry getOrCreate(ServerLevel level) {
		var registries = level.registryAccess();
		SavedDataType<TradeStationRegistry> type = new SavedDataType<>(Identifier.parse("forestry:mail/trade_stations"),
			TradeStationRegistry::create, CompoundTag.CODEC.xmap(tag -> load(tag, registries), data -> data.save(new CompoundTag(), registries)));
		return SavedDataMigration.getOrCreate(level, type, SAVE_NAME, tag -> load(tag, registries));
	}
}
