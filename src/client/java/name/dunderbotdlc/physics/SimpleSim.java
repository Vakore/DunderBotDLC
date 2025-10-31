package name.dunderbotdlc.physics;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;

import static name.dunderbotdlc.DunderBotdlcClient.client;

/*
8 - 11 falling = too far, try delaying jump slightly

0 - 1 ascending = too close, try slowing down for a tick or two on current jump,
                  or do so on the previous jump
 */

/*
function tryJump() {
  bool delayJump = 0;//Positive if delay needs to occur
  //Simulate the jump1
  bestPos = jump1Pos
  if (collide && falling) {
    delayJump = find ticks;
  }

  if (delayJump) {
    //Simulate jump1 with delay ticks. If it's better, use it.
    bestPos = jump1Pos
  }

  //Simulate the next jump
  if (!delayJump && nextJumpBad) {
    delayJump = find ticks;
    if (delayJump) {
      //simulate jump1 with delay ticks
    }
    //simulate jump2
  }
}
 */

public class SimpleSim {
    //physics
    public static int simpleProblem(PlayerEntity player, int slowDown, int useDebug) {
        boolean[] playerControlList = new boolean[8];
        for (int i = 0; i < 8; i++) {playerControlList[i] = (i == 1 || i == 2 || i == 3);}
        SimInstance myStateBase = new SimInstance(player.isOnGround(), playerControlList, client.player.getPos(), client.player.getVelocity(), (float) (Math.PI + (-client.player.getYaw() * Math.PI / 180.0)));
        SimInstance myState = myStateBase.clone();
        if (useDebug > 0) {
            myState.x = 465.5f;
            myState.y = 96;
            myState.z = 2.5f;
            myState.yaw = 3.14f;//(180.0f / (float)Math.PI);
            myState.onGround = true;
            myState.velX = 0.0;
            myState.velY = 0.0;
            myState.velZ = 0.0;
        }

        int firstTry = -99;
        for (int i = 0; i < 30; i++) {

            if (i < slowDown) {
                myState.controlsprint = false;
            } else {
                myState.controlsprint = true;
            }

            if (i % 3 == 0) {
                client.world.addParticle(ParticleTypes.FLAME,
                        myState.x,
                        myState.y,
                        myState.z, 0.0, 0.0, 0.0);
            }
            myState.simulatePlayer();
            if (myState.isCollidedHorizontally || myState.isCollidedVertically) {
                if (myState.isCollidedHorizontally) {
                    System.out.println("Time #1: Tick collided: " + i + ", velY: " + myState.velY);
                    System.out.println((myState.velY > 0) ? "Scenario #1: Too close to block, slow down" : "Scenario #2: Too far from block, delay jump");
                    if (myState.velY < 0) {
                        firstTry = i - 4;
                        if (firstTry < 0) {firstTry = 0;}
                    } else {
                        firstTry = -2;
                    }
                }
                client.world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            myState.x,
                            myState.y,
                            myState.z, 0.0, 0.0, 0.0);
                i = 30;
            }
        }
        if (firstTry != -99) {
            return firstTry;
        }
        //return 0;

        //SimInstance myState2 = myState.clone();
        int redo = 0;
        for (int i = 0; i < 30; i++) {
            if (i % 3 == 0) {
                client.world.addParticle(ParticleTypes.FLAME,
                        myState.x,
                        myState.y,
                        myState.z, 0.0, 0.0, 0.0);
            }
            myState.simulatePlayer();
            if (myState.isCollidedHorizontally || myState.isCollidedVertically) {
                client.world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        myState.x,
                        myState.y,
                        myState.z, 0.0, 0.0, 0.0);
                if (myState.isCollidedHorizontally) {
                    System.out.println("Time #2: Tick collided: " + i + ", velY: " + myState.velY);
                    redo = i * ((int)Math.signum(myState.velY));
                    redo += 5 * (int)Math.signum(myState.velY);
                    System.out.println(" " + i + ", " + ((int)Math.signum(myState.velY)));
                    System.out.println((myState.velY > 0) ? "Scenario #3: 2nd jump too close to block, delay jump" : "Scenario #4: 2nd jump too far from block... later problem lol");
                }
                i = 30;////tp @s 465 96 2.5 0 0
            }
        }

        if (redo > 0 && (Math.abs(myState.y - client.player.getPos().y) < 0.9f)) {
            System.out.println("Second jump: too far away. Delaying jump...");
            return redo;
        } else if (redo < 0) {
            //System.out.println("Second jump: too close, attempting to slow down.");
            //simpleProblem(player, redo);
            //return redo;
        }
        return 0;
    }
}
