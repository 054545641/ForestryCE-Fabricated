package forestry.lepidopterology.butterflies.genetics;

import forestry.api.core.IProduct;
import forestry.api.lepidopterology.IButterflyCocoon;

import java.util.List;

public class DefaultCocoon implements IButterflyCocoon {
	private final String texture;
	private final List<IProduct> products;

	public DefaultCocoon(String texture, List<IProduct> products) {
		this.texture = texture;
		this.products = products;
	}

	@Override
	public List<IProduct> getProducts() {
		return this.products;
	}

	@Override
	public boolean isDominant() {
		return false;
	}
}
