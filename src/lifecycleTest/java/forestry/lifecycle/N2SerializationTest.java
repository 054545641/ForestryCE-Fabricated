package forestry.lifecycle;

import forestry.arboriculture.leaves.LeavesBlockEntity;
import forestry.arboriculture.trees.TileTreeContainer;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.lepidopterology.genetics.IButterfly;
import forestry.core.platform.multiblock.MultiblockTileEntityForestry;
import forestry.core.platform.util.ValueIOUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;

/** Reusing a loaded object must replace absent persistent fields, without confusing visual packets with full NBT. */
final class N2SerializationTest {
	static void replacement(GameTestHelper h) {
		var level = h.getLevel();
		var specimens = N2WorldTest.specimens();
		for (var stack : N2WorldTest.specimenStacks()) {
			var encoded = forestry.core.platform.util.InventoryData.writeStack(stack, level.registryAccess());
			var decoded = forestry.core.platform.util.InventoryData.readStack(encoded, level.registryAccess());
			h.assertTrue(net.minecraft.world.item.ItemStack.matches(stack, decoded), "Individual components NBT round trip");
			var stream = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
			try {
				net.minecraft.world.item.ItemStack.STREAM_CODEC.encode(stream, stack);
				h.assertTrue(net.minecraft.world.item.ItemStack.matches(stack, net.minecraft.world.item.ItemStack.STREAM_CODEC.decode(stream)), "Individual components stream round trip");
			} finally { stream.release(); }
		}
		var sapling = (TileTreeContainer) N2WorldTest.place(level, h.absolutePos(new BlockPos(1, 2, 1)), "sapling_ge");
		sapling.setTree((ITree) specimens.get(1));
		ValueIOUtil.readTag(new CompoundTag(), level.registryAccess(), sapling::loadCustomOnly);
		h.assertTrue(sapling.getTree() == null, "Absent tree clears the previous genome on full load");
		sapling.setTree((ITree) specimens.get(1));
		var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
		try {
			buffer.writeBoolean(false);
			sapling.readData(buffer);
			h.assertTrue(sapling.getTree() == null, "Empty tree packet clears the previous species");
		} finally { buffer.release(); }
		var leaves = (LeavesBlockEntity) N2WorldTest.place(level, h.absolutePos(new BlockPos(2, 2, 1)), "leaves");
		leaves.setTree((ITree) specimens.get(1));
		leaves.setCaterpillar((IButterfly) specimens.get(2));
		CompoundTag withoutNursery = leaves.saveCustomOnly(level.registryAccess());
		withoutNursery.remove("CATER");
		withoutNursery.remove("CATMAT");
		ValueIOUtil.readTag(withoutNursery, level.registryAccess(), leaves::loadCustomOnly);
		h.assertTrue(leaves.getCaterpillar() == null && !leaves.saveCustomOnly(level.registryAccess()).contains("CATER"), "Removed nursery does not survive a replacement load");
		var member = (MultiblockTileEntityForestry<?>) N2WorldTest.place(level, h.absolutePos(new BlockPos(3, 2, 1)), "alveary_block");
		CompoundTag payload = new CompoundTag();
		payload.putString("sentinel", "old controller");
		CompoundTag stored = new CompoundTag();
		stored.put(MultiblockTileEntityForestry.PAYLOAD_KEY, payload);
		ValueIOUtil.readTag(stored, level.registryAccess(), member::loadCustomOnly);
		member.setAnchorPos(member.getBlockPos());
		ValueIOUtil.readTag(new CompoundTag(), level.registryAccess(), member::loadCustomOnly);
		h.assertTrue(member.getAnchorPos() == null && !member.hasStash(), "Absent anchor/payload clears previous multiblock state");
		var owner = new forestry.core.platform.owner.OwnerHandler();
		owner.setOwner(PostalTests.profile("N2Owner"));
		owner.read(new CompoundTag(), level.registryAccess());
		h.assertTrue(owner.getOwner() == null, "Absent owner clears ownership on full load");
		CompoundTag legacyOwner = new CompoundTag();
		legacyOwner.putString("owner_name", "LegacyOwner");
		owner.read(legacyOwner, level.registryAccess());
		h.assertTrue(owner.getOwner().name().equals("LegacyOwner") && owner.getOwner().id().equals(net.minecraft.util.Util.NIL_UUID), "Name-only legacy ownership remains readable");
		owner.setOwner(PostalTests.profile("N2Owner"));
		var ownerPacket = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
		try {
			ownerPacket.writeBoolean(false);
			owner.readData(ownerPacket);
			h.assertTrue(owner.getOwner() == null, "Empty owner packet clears client ownership");
		} finally { ownerPacket.release(); }
		ValueIOUtil.readTag(new CompoundTag(), level.registryAccess(), leaves::loadCustomOnly);
		var leafPacket = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
		try {
			leaves.writeData(leafPacket);
			leaves.setTree((ITree) specimens.get(1));
			leaves.readData(leafPacket);
			h.assertTrue(leaves.getTree() == null && leafPacket.readableBytes() == 0, "Empty leaves stream clears old species without throwing");
		} finally { leafPacket.release(); }
		h.succeed();
	}
}
