package forestry.mail.carriers.trading;

import com.google.common.base.Preconditions;
import com.mojang.authlib.GameProfile;
import forestry.api.mail.EnumTradeStationState;
import forestry.api.mail.IMailAddress;
import forestry.api.mail.ITradeStationInfo;
import forestry.mail.carriers.PostalCarriers;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record TradeStationInfo(
	IMailAddress address,
	GameProfile owner,
	ItemStack tradegood,
	List<ItemStack> required,
	EnumTradeStationState state
) implements ITradeStationInfo {
	public TradeStationInfo {
		Preconditions.checkArgument(address.getCarrier().equals(PostalCarriers.TRADER.get()), "TradeStation address must be a trader");
	}
}
