package forestry.core.platform.advancements;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The way an advancement with no criterion of its own is granted. Every advancement handed out here
 * is built on {@code minecraft:impossible}, so the code that knows the player earned it says so.
 */
public class AdvancementHelper {
	/**
	 * Awards every criterion the player has not met yet on one advancement.
	 *
	 * @param player The player to award, ignored on the client and for any non-server player
	 * @param id     The advancement to award
	 */
	public static void tryUnlock(Player player, Identifier id) {
		if (player instanceof ServerPlayer serverPlayer) {
			// Deviation from 1.20.1: getAdvancement(id) became get(id) and returns an AdvancementHolder
			AdvancementHolder advancement = serverPlayer.level().getServer().getAdvancements().get(id);

			if (advancement != null) {
				AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(advancement);

				for (String criterion : progress.getRemainingCriteria()) {
					serverPlayer.getAdvancements().award(advancement, criterion);
				}
			}
		}
	}
}
