package forestry.core.content.backpacks;
import forestry.api.ForestryTags;
import forestry.api.core.backpacks.IBackpackInterface;
import forestry.api.core.genetics.ForestrySpeciesTypes;
public final class BackpackDefinitions {
	public static final IBackpackInterface BACKPACK_INTERFACE = new BackpackInterface();

	public static final BackpackDefinition APIARIST = new BackpackDefinition(0xc4923d, 0xffffff, BACKPACK_INTERFACE.createNaturalistBackpackFilter(ForestrySpeciesTypes.BEE));
	public static final BackpackDefinition ARBORIST = new BackpackDefinition(0x657e3a, 0xffffff, BACKPACK_INTERFACE.createNaturalistBackpackFilter(ForestrySpeciesTypes.TREE));
	public static final BackpackDefinition LEPIDOPTERIST = new BackpackDefinition(0x995b31, 0xffffff, BACKPACK_INTERFACE.createNaturalistBackpackFilter(ForestrySpeciesTypes.BUTTERFLY));
	public static final BackpackDefinition MINER = new BackpackDefinition(0x36187d, 0xffffff, new BackpackFilter(ForestryTags.Items.MINER_ALLOW, ForestryTags.Items.MINER_REJECT));
	public static final BackpackDefinition DIGGER = new BackpackDefinition(0x363cc5, 0xffffff, new BackpackFilter(ForestryTags.Items.DIGGER_ALLOW, ForestryTags.Items.DIGGER_REJECT));
	public static final BackpackDefinition FORESTER = new BackpackDefinition(0x347427, 0xffffff, new BackpackFilter(ForestryTags.Items.FORESTER_ALLOW, ForestryTags.Items.FORESTER_REJECT));
	public static final BackpackDefinition HUNTER = new BackpackDefinition(0x412215, 0xffffff, new BackpackFilter(ForestryTags.Items.HUNTER_ALLOW, ForestryTags.Items.HUNTER_REJECT));
	public static final BackpackDefinition ADVENTURER = new BackpackDefinition(0x7fb8c2, 0xffffff, new BackpackFilter(ForestryTags.Items.ADVENTURER_ALLOW, ForestryTags.Items.ADVENTURER_REJECT));
	public static final BackpackDefinition BUILDER = new BackpackDefinition(0xdd3a3a, 0xffffff, new BackpackFilter(ForestryTags.Items.BUILDER_ALLOW, ForestryTags.Items.BUILDER_REJECT));
	public static final BackpackDefinition BREWER = new BackpackDefinition(0xBD7CBD, 0xffffff, new BackpackFilter(ForestryTags.Items.BREWER_ALLOW, ForestryTags.Items.BREWER_REJECT));

}
