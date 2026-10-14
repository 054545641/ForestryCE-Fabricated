package forestry.mail.carriers.players;

import forestry.api.mail.IMailAddress;
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
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class POBoxRegistry extends SavedData implements IWatchable.Watcher {
	private static final String SAVE_NAME = "forestry_poboxes";

	public final Map<IMailAddress, POBox> cachedPOBoxes = new HashMap<>();
	private final forestry.mail.postoffice.PostalRegistryJournal<IMailAddress, POBox> journal = new forestry.mail.postoffice.PostalRegistryJournal<>(
		this.cachedPOBoxes, box -> box.registerUpdateWatcher(this), box -> box.unregisterUpdateWatcher(this), this::setDirty);

	/**
	 * @param address the potential address of the PO box
	 * @return true if the passed address is valid for PO Boxes.
	 */
	public boolean isValidPOBox(IMailAddress address) {
		return address.getCarrier().equals(PostalCarriers.PLAYER.value()) && address.getName().matches("^[a-zA-Z0-9_]+$");
	}

	private void registerPOBOx(IMailAddress address, POBox box) {
        this.cachedPOBoxes.put(address, box);
		box.registerUpdateWatcher(this);
	}

	@Nullable
	public POBox getPOBox(IMailAddress address) {
		return this.cachedPOBoxes.get(address);
	}

	public POBox getOrCreatePOBox(IMailAddress address) {
		try (var tx = net.neoforged.neoforge.transfer.transaction.Transaction.open(net.neoforged.neoforge.transfer.transaction.Transaction.getCurrentOpenedTransaction())) {
			POBox box = getOrCreatePOBox(address, tx);
			tx.commit();
			return box;
		}
	}

	public POBox getOrCreatePOBox(IMailAddress address, net.neoforged.neoforge.transfer.transaction.TransactionContext transaction) {
		if (!address.isValid() || !isValidPOBox(address)) throw new IllegalArgumentException("Invalid PO box address");
		POBox box = getPOBox(address);
		if (box == null) {
			this.journal.updateSnapshots(transaction);
			box = new POBox(address);
			this.cachedPOBoxes.put(address, box);
			box.registerUpdateWatcher(this);
		}
		return box;
	}

	@Override
	public void onWatchableUpdate() {
		setDirty();
	}

	private static POBoxRegistry create() {
		return new POBoxRegistry();
	}

	private static POBoxRegistry load(CompoundTag compoundTag, HolderLookup.Provider registries) {
		POBoxRegistry registry = new POBoxRegistry();
		ListTag tradeStations = compoundTag.getList("poboxes").orElseThrow(() -> new IllegalArgumentException("Missing poboxes list"));
		for (int i = 0; i < tradeStations.size(); ++i) {
			CompoundTag stationTag = tradeStations.getCompound(i).orElseThrow(() -> new IllegalArgumentException("Invalid mail registry entry"));

			IMailAddress address = new MailAddress(stationTag.getCompound("address").orElseThrow(() -> new IllegalArgumentException("Missing mail address")));
			POBox pobox = new POBox(stationTag.getCompound("pobox").orElseThrow(() -> new IllegalArgumentException("Missing pobox data")), registries);
			if (!address.isValid() || !address.getCarrier().equals(PostalCarriers.PLAYER.value()) || registry.getPOBox(address) != null) {
				throw new IllegalArgumentException("Invalid or duplicate mail address " + address);
			}
			registry.registerPOBOx(address, pobox);
		}
		return registry;
	}

	public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider registries) {
		ListTag poboxes = new ListTag();
		for (Map.Entry<IMailAddress, POBox> entry : this.cachedPOBoxes.entrySet()) {
			CompoundTag entryTag = new CompoundTag();
			entryTag.put("address", entry.getKey().write(new CompoundTag(), registries));
			entryTag.put("pobox", entry.getValue().write(new CompoundTag(), registries));
			poboxes.add(entryTag);
		}
		compoundTag.put("poboxes", poboxes);
		return compoundTag;
	}

	public static POBoxRegistry getOrCreate(ServerLevel level) {
		var registries = level.registryAccess();
		SavedDataType<POBoxRegistry> type = new SavedDataType<>(Identifier.parse("forestry:mail/poboxes"),
			POBoxRegistry::create, CompoundTag.CODEC.xmap(tag -> load(tag, registries), data -> data.save(new CompoundTag(), registries)));
		return SavedDataMigration.getOrCreate(level, type, SAVE_NAME, tag -> load(tag, registries));
	}
}
