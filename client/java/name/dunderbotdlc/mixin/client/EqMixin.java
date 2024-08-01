package name.dunderbotdlc.mixin.client;

import name.dunderbotdlc.commands.IBaritoneAPIMixin;
import baritone.api.BaritoneAPI;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(baritone.eq.class)
public abstract class EqMixin {
    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideOnTick(CallbackInfo info) {
        //System.out.println(((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause());
        if (((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause()) {
            info.cancel();
        }
    }

    @Inject(method = "setInputForceState", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideInControl(CallbackInfo info) {
        //System.out.println("yahoo: " + ((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause());
        if (((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause()) {
            info.cancel();
        }
    }


}