package forestry.arboriculture.fruit;

import forestry.api.core.IProduct;
import forestry.api.core.genetics.IGenome;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.BlockGetter;

import java.util.List;
import forestry.arboriculture.fruit.Fruit;

// Fruits that grow unripe in tree leaves, then ripen over time.
public class RipeningFruit extends Fruit {
	private final int colourCallow;
	private final int diffR;
	private final int diffG;
	private final int diffB;
	private final Identifier sprite;

	public RipeningFruit(boolean dominant, int ripeningPeriod, Identifier sprite, int ripe, int callow, List<IProduct> products) {
		super(dominant, ripeningPeriod, products);
		this.sprite = sprite;

		this.colourCallow = callow;
		this.diffR = (ripe >> 16 & 255) - (callow >> 16 & 255);
		this.diffG = (ripe >> 8 & 255) - (callow >> 8 & 255);
		this.diffB = (ripe & 255) - (callow & 255);
	}

	private float getRipeningStage(int ripeningTime) {
		if (ripeningTime >= this.ripeningPeriod) {
			return 1.0f;
		}

		return (float) ripeningTime / this.ripeningPeriod;
	}

	@Override
	public boolean isFruitLeaf() {
		return true;
	}

	@Override
	public int getColour(IGenome genome, BlockGetter world, BlockPos pos, int ripeningTime) {
		float stage = getRipeningStage(ripeningTime);
		return getColour(stage);
	}

	private int getColour(float stage) {
		int r = (this.colourCallow >> 16 & 255) + (int) (this.diffR * stage);
		int g = (this.colourCallow >> 8 & 255) + (int) (this.diffG * stage);
		int b = (this.colourCallow & 255) + (int) (this.diffB * stage);

		return (r & 255) << 16 | (g & 255) << 8 | b & 255;
	}

	@Override
	public int getDecorativeColor() {
		return getColour(1.0f);
	}

	@Override
	public Identifier getSprite(IGenome genome, BlockGetter world, BlockPos pos, int ripeningTime) {
		return this.sprite;
	}

	@Override
	public Identifier getDecorativeSprite() {
		return this.sprite;
	}

	@Override
	public int getRipeningPeriod() {
		return this.ripeningPeriod;
	}
}
