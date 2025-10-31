package name.dunderbotdlc;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;
import name.dunderbotdlc.physics.SimInstance;
import name.dunderbotdlc.structs.jumpSprintState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class DunderJumpSprint {


    public static void doJumpsprint(DunderBotdlcClient dis, int depth) {
        DunderBotdlcClient.bPos.clear();
        DunderBotdlcClient.currentMove = 0;

        try {
            if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior() != null &&
                    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath() != null &&
                    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get() != null &&
                    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions() != null &&
                    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions().get(0) != null) {//was .getLast()
                //bPos = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions();
                DunderBotdlcClient.bPos.clear();
                DunderBotdlcClient.bPos.addAll(BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions());
                IPathExecutor extraSegment = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getNext();
                if (extraSegment != null &&
                        extraSegment.getPath() != null &&
                        extraSegment.getPath().positions() != null &&
                        extraSegment.getPath().positions().get(0) != null) {
                    DunderBotdlcClient.bPos.addAll(extraSegment.getPath().positions());
                }
            }
        } catch (Exception e) {
            //System.out.println("Ya done goofed\n" + e);
        }


        if (dis.noJumpAttempts <= 0 && dis.player.isOnGround()) {
            //System.out.println("oh yeah");
            int bestPos = -1;
            try {
                //
                List<BetterBlockPos> bPos = new ArrayList<BetterBlockPos>();
                if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior() != null &&
                        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath() != null &&
                        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get() != null &&
                        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions() != null &&
                        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions().get(0) != null) {//was .getLast()
                    //bPos = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions();
                    bPos.clear();
                    bPos.addAll(BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions());
                    IPathExecutor extraSegment = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getNext();
                    if (    extraSegment != null &&
                            extraSegment.getPath() != null &&
                            extraSegment.getPath().positions() != null &&
                            extraSegment.getPath().positions().get(0) != null) {
                        bPos.addAll(extraSegment.getPath().positions());
                    }
                }

                bestPos = Math.min(BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getCurrent().getPosition() - 1, 0);
                if (bestPos < 0) {bestPos = 0;}
                for (int i = bestPos; i < bPos.size(); i++) {
                    if (dis.player.getPos().distanceTo( new Vec3d(bPos.get(i).x, bPos.get(i).y, bPos.get(i).z)) <
                            dis.player.getPos().distanceTo( new Vec3d(bPos.get(bestPos).x, bPos.get(bestPos).y, bPos.get(bestPos).z))) {
                        bestPos = i;
                    }
                    //particle
                    dis.world.addParticle(ParticleTypes.FLAME,
                            bPos.get(i).x+0.5,
                            bPos.get(i).y,
                            bPos.get(i).z+0.5, 0.0, 0.0, 0.0);
                }
                dis.world.addParticle(ParticleTypes.HEART,
                        bPos.get(bestPos).x+0.5,
                        bPos.get(bestPos).y+0.25,
                        bPos.get(bestPos).z+0.5, 0.0, 0.0, 0.0);
                dis.bestPathNum = bestPos - BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getCurrent().getPosition() + 1;
            } catch (Exception e) {
                System.out.println("Baked:\n" + e);
                return;
            }

            boolean[] playerControlList = new boolean[8];
            for (int i = 0; i < 8; i++) {playerControlList[i] = (i == 1 || i == 2 || i == 3);}
            SimInstance myStateBase = new SimInstance(dis.player.isOnGround(), playerControlList, dis.client.player.getPos(), dis.client.player.getVelocity(), (float)(Math.PI + (-dis.client.player.getYaw() * Math.PI / 180.0)));

            dis.jumpSprintStates.clear();
            dis.leBest = null;

            //Simulate jump sprints
            for (int j = 0; j < 7; j++) {
                SimInstance myState = myStateBase.clone();
                myState.yaw = (float)(myStateBase.yaw - (Math.PI / 2) + (Math.PI / 8) + ((Math.PI / 8) * new int[]{3,4,2,5,1,7,0}[j]));
                jumpSprintState pushDis = dis.simulateAction(depth, bestPos, 0, new Vec3d(0, 0, 0), myState);
                if (pushDis != null) {
                    dis.jumpSprintStates.add(pushDis);
                }
            }

            //simulate walks
            /*for (int j = 0; j < 5; j++) {
                SimInstance myState = myStateBase.clone();
                myState.controljump = false;
                myState.myControls[1] = false;
                myState.yaw = (float)(myStateBase.yaw - (Math.PI / 2) + (Math.PI / 8) + ((Math.PI / 8) * new int[]{3,4,2,7,0}[j]));
                jumpSprintState pushDis = dis.simulateAction(depth, bestPos, 1, new Vec3d(0, 0, 0), myState);
                if (pushDis != null) {
                    dis.jumpSprintStates.add(pushDis);
                }
            }*/
            if (dis.jumpSprintStates.size() > 0) {
                int myBestState = 0;
                for (int i = 0; i < dis.jumpSprintStates.size(); i++) {
                    if (dis.jumpSprintStates.get(i).open == true && dis.jumpSprintStates.get(i).score < dis.jumpSprintStates.get(myBestState).score) {
                        myBestState = i;
                    }
                }

                if (myBestState >= 0) {
                    dis.leBest = dis.jumpSprintStates.get(myBestState);
                    if (!dis.leBest.shouldJump) {
                        dis.noJumpAttempts = 2;
                    }
                }
            }
        }

        if (dis.leBest != null) {
            dis.player.setYaw((float)-((dis.leBest.state.yaw - Math.PI) * 180.0f / Math.PI));
            dis.c_W = dis.leBest.state.controlforward;
            dis.c_A = dis.leBest.state.controlleft;
            dis.c_D = dis.leBest.state.controlright;
            dis.c_spr = dis.leBest.state.controlsprint;
            dis.c_j = (dis.player.getVelocity().y <= 0) && dis.leBest.state.controljump;
        }
    }
}
