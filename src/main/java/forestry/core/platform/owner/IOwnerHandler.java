package forestry.core.platform.owner;

import com.mojang.authlib.GameProfile;

import javax.annotation.Nullable;

public interface IOwnerHandler {
	@Nullable
	GameProfile getOwner();

	void setOwner(GameProfile owner);
}
