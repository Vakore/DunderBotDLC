package name.dunderbotdlc.mixin.client;

import name.dunderbotdlc.commands.IBaritoneAPIMixin;
import baritone.api.BaritoneAPI;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(baritone.h.class)
public abstract class PathingBehaviorMixin {
    @Inject(method = "b", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideOnTick(CallbackInfo info) {
        //I forget what this does. Does not appear to be necessary to maintain control.
        if (((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause()) {
            //info.cancel();
        }
    }

    @Inject(method = "onPlayerSprintState", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideOnPlayerSprintState(CallbackInfo info) {
        //Believed to handle overriding player sprinting
        if (((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause()) {
            info.cancel();
        }
    }


}