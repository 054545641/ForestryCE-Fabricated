package forestry.n6;

import com.mojang.authlib.GameProfile;
import forestry.core.features.CoreItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.common.util.FakePlayer;

import static forestry.n6.N6ReleaseTest.check;

final class N6ServerChecks {
    static void manual(ServerLevel level) {
        int[] counts = new int[2];
        var player = new FakePlayer(level, new GameProfile(java.util.UUID.randomUUID(), "N6Manual")) {
            @Override public void sendOverlayMessage(Component message) {
                check(message.getContents() instanceof TranslatableContents text && text.getKey().equals("item.forestry.foresters_manual.missing_patchouli"), "Manual hint has translatable key");
                counts[0]++;
            }
            @Override public void playSound(SoundEvent sound, float volume, float pitch) { counts[1]++; }
        };
        player.setItemInHand(InteractionHand.MAIN_HAND, CoreItems.FORESTERS_MANUAL.stack());
        check(CoreItems.FORESTERS_MANUAL.item().use(level, player, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "Missing-provider manual use succeeds");
        check(counts[0] == 1 && counts[1] == 0 && player.getMainHandItem().getCount() == 1, "One manual hint, no success sound or consumption");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), level.registryAccess());
        try {
            buffer.writeBlockPos(net.minecraft.core.BlockPos.ZERO);
            buffer.writeVarInt(10_000);
            try {
                forestry.core.content.machines.network.packets.PacketRecipeTransferRequest.decode(buffer);
                throw new AssertionError("Oversized ghost transfer must be rejected before decoding items");
            } catch (io.netty.handler.codec.DecoderException expected) { }
        } finally { buffer.release(); }
        com.mojang.logging.LogUtils.getLogger().info("FORESTRY_N6_MANUAL_READY hint=1 sound=0 stack=1 bounded_transfer=true");
    }
}
