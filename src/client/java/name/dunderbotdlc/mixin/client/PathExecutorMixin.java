package name.dunderbotdlc.mixin.client;

import name.dunderbotdlc.DunderBotdlcClient;
import name.dunderbotdlc.commands.IBaritoneAPIMixin;
import baritone.api.BaritoneAPI;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;

@Mixin(baritone.dg.class)
public abstract class PathExecutorMixin {
    /*@Inject(method = "a", at = @At("HEAD"), cancellable = true, remap = false)
    private void overrideOnTick(CallbackInfoReturnable<Boolean> info) {
        /*Probably executes the path
          Turning off helps maintain control, but it disables path functions.
          Presumably:
          1. disable "ctx.player.setSprinting" on line 239ish
          2. 
        *
        if (((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause()) {
            //System.out.println( ((baritone.dg)(Object)this). );
            info.setReturnValue(false);
        }
    }*/

    /*@ModifyVariable(method = "a", at = @At(value = "STORE", target = "Lbaritone/dg;var42"), remap = false)
    public boolean makeVar42True(boolean original) {
        return true;
    }*/

    //Caused by: org.spongepowered.asm.mixin.injection.throwables.InjectionError: Critical injection failure: Callback method makeVar42True(Z)Z in dunderbotdlc.client.mixins.json:PathExecutorMixin from mod dunderbotdlc failed injection check, (0/1) succeeded. Scanned 1 target(s). No refMap loaded.
    /*@WrapOperation(method = "a", at = @At(value = "INVOKE", target = "Lbaritone/dg;method_5728"), remap = false)
    public boolean makeVar42True(boolean original) {
        return true;
    }*/

    //@Overwrite
    
    
    //Fails. this.b is not this.b()
    /*@ModifyExpressionValue(method = "a", at = @At(value = "INVOKE", target = "Lbaritone/dg;b"), remap = false)
    public boolean overrideOnTick2(boolean original) {
        return true;
    }*/

    @WrapWithCondition(
        method = "a",
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/network/ClientPlayerEntity.setSprinting(Z)V"
        ),
        remap = false
    )
    private boolean wrapWithCondition$method_5728(net.minecraft.client.network.ClientPlayerEntity TargetClass, boolean value) {
        //System.out.println("big false");
        return !((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause();
    }

    
    @WrapWithCondition(method = "a", at = @At(value = "INVOKE",
            target = "baritone/eq.setInputForceState(Lbaritone/api/utils/input/Input;Z)V"
        ), remap = false)
    private boolean wrapWithCondition$setInputForceState(baritone.eq TargetThing, baritone.api.utils.input.Input value, boolean original) {
        //System.out.println("bigger false");
        return !((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause();
    }

    
    @WrapOperation(method = "a", at = @At(value = "INVOKE",
            target = "baritone/bw.update()Lbaritone/api/pathing/movement/MovementStatus;"
        ), remap = false)
    private baritone.api.pathing.movement.MovementStatus wrapWithCondition$updateStatus(baritone.bw TargetThing, Operation<baritone.api.pathing.movement.MovementStatus> original) {
        //System.out.println("smaller false");
        if (((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause()) {
            /*MinecraftClient myClient = MinecraftClient.getInstance();
            if (myClient != null &&
                myClient.options.swapHandsKey.isPressed()) {
                myClient.options.swapHandsKey.setPressed(false);
                return baritone.api.pathing.movement.MovementStatus.SUCCESS;
            }*/
            if (DunderBotdlcClient.bestPathNum > 0) {
                DunderBotdlcClient.bestPathNum--;
                return baritone.api.pathing.movement.MovementStatus.SUCCESS;
            }
            return baritone.api.pathing.movement.MovementStatus.PREPPING;
        } else {
            return original.call(TargetThing);
        }
    }

    @ModifyExpressionValue(method = "a", at = @At(value = "INVOKE",
            target = "java/util/Set.contains(Ljava/lang/Object;)Z"
        ), remap = false)
    private boolean modifyExpression$contains(boolean original) {
        /*if (((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause()) {
            System.out.println("This is doing something, but not enough.\n" +
                original + " | " + !((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause() + " | " +
                (original || !((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause()));
        }*/
        return original || ((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause();
    }


}