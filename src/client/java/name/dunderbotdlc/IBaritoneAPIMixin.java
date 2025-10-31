package name.dunderbotdlc;


                    /*Class thisClass = net.minecraft.client.gui.screen.Screen.class;//BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().getClass();
                    System.out.println(thisClass.getSimpleName());
                    Method[] methods = thisClass.getDeclaredMethods();

                    for (int i = 0; i < methods.length; i++) {
                        System.out.println(methods[i].toString());
                    }
/*
 *
 */

/* IPathingControlManager - actuall the IpathingControlmanager
 * et
final boolean baritone.et.a(baritone.api.pathing.goals.Goal)
public final baritone.api.process.PathingCommand baritone.et.a()
public final void baritone.et.a()
public final java.util.Optional baritone.et.mostRecentInControl()
public final void baritone.et.registerProcess(baritone.api.process.IBaritoneProcess)
public final java.util.Optional baritone.et.mostRecentCommand()
 */

/*
IPathExecutor
dg
public final int baritone.dg.getPosition()
public final boolean baritone.dg.b()
private void baritone.dg.b()
private void baritone.dg.c()
private baritone.dg baritone.dg.a()
private baritone.dg baritone.dg.a(baritone.dg,baritone.dh)
public final baritone.dg baritone.dg.a(baritone.dg)
public final boolean baritone.dg.a()
private boolean baritone.dg.a(net.minecraft.util.Pair,double)
private net.minecraft.util.Pair baritone.dg.a(baritone.da)
public final void baritone.dg.a()
private static boolean baritone.dg.a(baritone.api.utils.IPlayerContext,baritone.api.pathing.movement.IMovement,baritone.api.pathing.movement.IMovement)
private static boolean baritone.dg.a(baritone.api.utils.IPlayerContext,baritone.dd,baritone.cw,baritone.api.pathing.movement.IMovement)
public final baritone.api.pathing.calc.IPath baritone.dg.getPath()
*/


/*  IPathingBehavior
 * h
private boolean baritone.h.b()
private void baritone.h.b()
private void baritone.h.c()
public final baritone.api.utils.BetterBlockPos baritone.h.a()
private void baritone.h.a(baritone.api.utils.BetterBlockPos)
private void baritone.h.a(net.minecraft.util.math.BlockPos,boolean,baritone.bv)
public final void baritone.h.a()
private baritone.dg baritone.h.a(baritone.api.pathing.calc.IPath)
private void baritone.h.a(boolean,net.minecraft.util.math.BlockPos,baritone.api.pathing.goals.Goal,baritone.bs,long,long)
private static baritone.bs baritone.h.a(net.minecraft.util.math.BlockPos,baritone.api.pathing.goals.Goal,baritone.api.pathing.calc.IPath,baritone.bv)
private static double baritone.h.a(double,double,baritone.api.utils.BetterBlockPos)
private void baritone.h.a(baritone.api.event.events.PathEvent)
public final boolean baritone.h.a()
public final boolean baritone.h.a(baritone.api.process.PathingCommand)
public final void baritone.h.onPlayerSprintState(baritone.api.event.events.SprintStateEvent)
public final baritone.api.pathing.path.IPathExecutor baritone.h.getNext()
public final void baritone.h.onRenderPass(baritone.api.event.events.RenderEvent)
public final void baritone.h.forceCancel()
public final java.util.Optional baritone.h.getInProgress()
public final boolean baritone.h.cancelEverything()
public final void baritone.h.onPlayerUpdate(baritone.api.event.events.PlayerUpdateEvent)
public final java.util.Optional baritone.h.estimatedTicksToGoal()
public final boolean baritone.h.isPathing()
public final void baritone.h.onTick(baritone.api.event.events.TickEvent)
public final baritone.api.pathing.path.IPathExecutor baritone.h.getCurrent()
public final baritone.api.pathing.goals.Goal baritone.h.getGoal()
 */
/* IMovement
 * dd
public final void baritone.dd.reset()
public final boolean baritone.dd.b(baritone.by)
public final baritone.by baritone.dd.a(baritone.by)
public final boolean baritone.dd.a(baritone.by)
public static double baritone.dd.a(baritone.bv,int,int,int,int,int)
public final java.util.Set baritone.dd.a()
public final double baritone.dd.a(baritone.bv)
 */
/*
f
public final void baritone.f.updateTarget(baritone.api.utils.Rotation,boolean)
public final void baritone.f.onPlayerUpdate(baritone.api.event.events.PlayerUpdateEvent)
public final void baritone.f.onPlayerRotationMove(baritone.api.event.events.RotationMoveEvent)
 */


                    /*Class thisClass = BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().getClass();
                    Method[] methods = thisClass.getDeclaredMethods();

                    for (int i = 0; i < methods.length; i++) {
                        System.out.println(methods[i].toString());
                    }*/
                    /*
public final void baritone.eq.clearAllKeys()
public final void baritone.eq.onTick(baritone.api.event.events.TickEvent)
public final boolean baritone.eq.isInputForcedDown(baritone.api.utils.input.Input)
public final void baritone.eq.setInputForceState(baritone.api.utils.input.Input,boolean)
                     */
//System.out.println(BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().getClass().getDeclaredMethods().);
//System.out.println(BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().getClass().getSimpleName());
                    /*System.out.println("Soft Pause");
                    System.out.println(((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause());
                    ((IBaritoneAPIMixin) BaritoneAPI.getProvider()).setSoftPause(!((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause());
                    System.out.println(((IBaritoneAPIMixin) BaritoneAPI.getProvider()).getSoftPause());*/

public interface IBaritoneAPIMixin {
    Boolean getSoftPause();
    void setSoftPause(Boolean value);
}