package forestry.mail.data;

import net.minecraft.data.PackOutput;

import forestry.core.data.models.BlockAssetsProvider;

import forestry.api.ForestryConstants;
import forestry.core.data.models.ForestryBlockStateProvider;
import forestry.mail.blocks.MailBlockType;
import forestry.mail.features.MailBlocks;

/**
 * Generates the blockstates and block models for the mail jar. All three machines are horizontal-facing
 * cubes, and their item models stay hand-authored for the custom display transforms.
 */
public class MailBlockStateProvider extends BlockAssetsProvider {
	public MailBlockStateProvider(PackOutput output) {
		super(output);
	}

	@Override
	protected void registerStatesAndModels() {
		ForestryBlockStateProvider.horizontalMachine(this, MailBlocks.BASE.get(MailBlockType.MAILBOX).block(), "mailbox", 0, 1, 2, 2, 2, 2, 2);
		ForestryBlockStateProvider.horizontalMachine(this, MailBlocks.BASE.get(MailBlockType.STAMP_COLLETOR).block(), "philatelist", 0, 1, 3, 2, 2, 2, 2);
		ForestryBlockStateProvider.horizontalMachine(this, MailBlocks.BASE.get(MailBlockType.TRADE_STATION).block(), "tradestation", 0, 1, 3, 2, 4, 4, 4);
	}
}
