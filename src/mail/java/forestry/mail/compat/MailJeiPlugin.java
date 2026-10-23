package forestry.mail.compat;

import forestry.api.modules.ForestryModuleIds;
import forestry.core.platform.util.JeiUtil;
import forestry.mail.blocks.MailBlockType;
import forestry.mail.features.MailBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;

@JeiPlugin
public class MailJeiPlugin implements IModPlugin {
	@Override
	public Identifier getPluginUid() {
		return ForestryModuleIds.MAIL;
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		JeiUtil.addDescription(registration, MailBlocks.BASE.get(MailBlockType.MAILBOX).block(), MailBlocks.BASE.get(MailBlockType.STAMP_COLLETOR).block(), MailBlocks.BASE.get(MailBlockType.TRADE_STATION).block());
	}
}
