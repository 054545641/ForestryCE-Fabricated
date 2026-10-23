package forestry.api.mail;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ITradeStationInfo {
	IMailAddress address();

	GameProfile owner();

	ItemStack tradegood();

	List<ItemStack> required();

	EnumTradeStationState state();
}
