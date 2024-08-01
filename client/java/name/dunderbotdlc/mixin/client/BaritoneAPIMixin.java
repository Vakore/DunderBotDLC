package name.dunderbotdlc.mixin.client;

import name.dunderbotdlc.commands.IBaritoneAPIMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(baritone.BaritoneProvider.class)
//@Mixin(baritone.eq.class)
public class BaritoneAPIMixin implements IBaritoneAPIMixin {
    @Unique
    private Boolean softPause = false;

    @Override
    public Boolean getSoftPause() {
        return softPause;
    }

    @Override
    public void setSoftPause(Boolean value) {
        this.softPause = value;
    }
}