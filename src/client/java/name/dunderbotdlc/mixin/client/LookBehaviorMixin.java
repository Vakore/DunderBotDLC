package name.dunderbotdlc.mixin.client;

import name.dunderbotdlc.commands.IBaritoneAPIMixin;
import baritone.api.BaritoneAPI;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(baritone.f.class)
public abstract class LookBehaviorMixin {
    @Inject(method = "onPlayerUpdate", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideOnPlayerUpdate(CallbackInfo info) {
        //System.out.println(((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause());
        if (((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause()) {
            info.cancel();
        }
    }

    @Inject(method = "onPlayerRotationMove", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideOnPlayerRotationMove(CallbackInfo info) {
        //System.out.println(((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause());
        if (((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause()) {
            info.cancel();
        }
    }
}