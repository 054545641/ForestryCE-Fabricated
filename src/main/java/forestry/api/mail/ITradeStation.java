package forestry.api.mail;

import forestry.api.core.IInventoryAdapter;
import net.minecraft.world.Container;

import javax.annotation.Nullable;

public interface ITradeStation extends ILetterHandler, Container, IWatchable, IInventoryAdapter {

	@Nullable
	IMailAddress getAddress();

	boolean isValid();

	void invalidate();

	void setVirtual(boolean isVirtual);

	boolean isVirtual();

	ITradeStationInfo getTradeInfo();

}
