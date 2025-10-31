package name.dunderbotdlc;

import name.dunderbotdlc.physics.SimInstance;
import name.dunderbotdlc.physics.SmartWalk;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.util.math.Vec3d;

public class DunderPvE {
    /*
    5. Something better
    4. Fixed
    3. Acceptable
    2. Tolerable
    1. Hacky
    0. TODO

    Problems:
    FIXED - Always looking at top of entity when it should look straight at times
    Potentially distance to attack calculations

    Swimming/dealing with objects that need to be pathfinded

    Seeing non-threats as threats(zombified pigmen, piglins when wearing
    gold armor, wolves, passive mobs, etc.)

    Trying to kill threats that are too far away, not visible(i.e. cave),
    or not worth worrying about(zombie 8 blocks off the path when you're
    moving away from it)

    Jockey Entities

    Projectiles
     Arrows: 1
     Blaze fireballs: 0

    Fighting projectile mobs without a shield

    Specific scenarios:
    1. Skeleton on both sides of the player - Tolerable
    2. Singular blaze - 0
    3. Skeleton Jockey - 0 - Player needs to be able to jump, shield, turn around, unshield
    4. 2 skeleton + 2 zombie combo - 0
    */

    public static void doPvE(DunderBotdlcClient dis) {
        dis.threatList.clear();
        dis.threatTable.clear();
        dis.threatDist.clear();
        for (Entity entity : dis.world.getEntities()) {
            double distance = dis.playerPos.distanceTo(entity.getPos());
            double threatLvl = 1/Math.max(0.5, distance);
            if (entity instanceof ArrowEntity) {
                //System.out.println(entity.getVelocity().length());
                if (dis.projectileMap.get(entity.getUuidAsString()) != null &&
                        dis.projectileMap.get(entity.getUuidAsString()).subtract(entity.getPos()).equals(Vec3d.ZERO) ||
                        entity.getPos().distanceTo(dis.playerPos) < entity.getPos().add(entity.getVelocity()).distanceTo(dis.playerPos) ||
                        entity.getVelocity().length() < 0.7) {
                    dis.mobMap.put(entity.getUuidAsString(), 1);
                } else if (dis.projectileMap.get(entity.getUuidAsString()) != null) {
                    dis.mobMap.remove(entity.getUuidAsString());
                    threatLvl *= 5;
                    threatLvl += 10;
                } else {
                    threatLvl *= 5;
                    threatLvl += 10;
                }
                dis.projectileMap.put(entity.getUuidAsString(), new Vec3d(entity.getX(), entity.getY(), entity.getZ()));
            } else if (entity.getClass().getSimpleName().matches("SkeletonEntity|IllusionerEntity|StrayEntity")) {
                threatLvl /= 3.0;
                boolean foundId = false;
                for (int i = 0; i < entity.getDataTracker().getChangedEntries().size(); i++) {
                    if (entity.getDataTracker().getChangedEntries().get(i).id() == 8) {
                        dis.mobMap.put(entity.getUuidAsString(),
                                (dis.mobMap.get(entity.getUuidAsString()) != null) ? (dis.mobMap.get(entity.getUuidAsString()) + 1) : 1);
                        foundId = true;
                    }
                }
                if (!foundId) {
                    dis.mobMap.put(entity.getUuidAsString(), 1);
                }
                if (dis.mobMap.get(entity.getUuidAsString()) != null && dis.mobMap.get(entity.getUuidAsString()) >= 3) {
                    threatLvl += dis.mobMap.get(entity.getUuidAsString()) / 15;
                }
            }
            if (distance <= 16 && distance > 0) {
                if (!(entity instanceof ArrowEntity)) {
                    System.out.println(entity.getClass().getSimpleName() + ", " + threatLvl);
                }
                //System.out.println(entity instanceof net.minecraft.entity.projectile.ArrowEntity);
                if ((entity.isAttackable() && entity.isAlive() && !(entity instanceof FireballEntity) ||
                        entity instanceof ArrowEntity &&
                                dis.mobMap.get(entity.getUuidAsString()) == null)) {
                    dis.threatList.add(entity);
                    dis.threatDist.add(distance);
                    dis.threatTable.add(threatLvl);
                    if ((dis.target == null || threatLvl > dis.threatLevel/*distance < playerPos.distanceTo(target.getPos()))*/)) {
                        dis.target = entity;
                        dis.targetID = dis.threatList.size() - 1;
                        dis.threatLevel = threatLvl;
                    }
                }
            }
        }


        // && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getNext().getPath().movements().getLast().safeToCancel()
        if (dis.target != null && dis.isBotting) {//(!!!)
            //BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
            //simulateAction();
            //BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            //baritone.getPathingBehavior().execute(pauseCommand);
            //System.out.println("Entity: " + entity.getType().getName().getString() + " at " + entity.getPos() + " is within 3 blocks of the player");
            dis.lookAtEntity(dis.player, dis.target);
            double distance = dis.playerPos.distanceTo(dis.target.getPos());
            //System.out.println(target.getVelocity());
            //System.out.println(target.groundCollision);
            //System.out.println(target.get);
            double threatDistance = 3.0;
            if (dis.target.getClass().getSimpleName().matches("SkeletonEntity|IllusionerEntity|StrayEntity") &&
                    true/*target.getHandItems().forEach()*/) {
                threatDistance = 0.2;
            }


            int bestTarget = -1;
            for (int i = 0; i < dis.threatDist.size(); i++) {
                if (dis.threatList.get(i) instanceof ArrowEntity) {
                    continue;
                }

                if (bestTarget == -1 ||
                        dis.playerPos.distanceTo(dis.threatList.get(i).getPos()) <
                                dis.playerPos.distanceTo(dis.threatList.get(bestTarget).getPos())) {
                    bestTarget = i;
                }
            }

            if (bestTarget - dis.targetID == 0) {
                if (distance > threatDistance) {
                    boolean[] playerControlList = new boolean[8];
                    for (int i = 0; i < 8; i++) {playerControlList[i] = (i == 3);}
                    playerControlList = SmartWalk.moveInDir(new SimInstance(dis.client.player.isOnGround(), playerControlList, dis.client.player.getPos(), dis.client.player.getVelocity(), (float)(Math.PI + (-dis.client.player.getYaw() * Math.PI / 180.0))));
                    dis.c_W = playerControlList[3];
                    dis.c_S = false;
                    dis.c_Z = playerControlList[0];
                    dis.c_j = playerControlList[1];
                    dis.c_spr = dis.c_W;
                } else {
                    boolean[] playerControlList = new boolean[8];
                    for (int i = 0; i < 8; i++) {playerControlList[i] = (i == 4);}
                    playerControlList = SmartWalk.moveInDir(new SimInstance(dis.client.player.isOnGround(), playerControlList, dis.client.player.getPos(), dis.client.player.getVelocity(), (float)(Math.PI + (-dis.client.player.getYaw() * Math.PI / 180.0))));
                    dis.c_W = false;
                    dis.c_spr = false;
                    dis.c_S = playerControlList[4];
                    dis.c_Z = playerControlList[0];
                    dis.c_j = playerControlList[1];
                }
            } else {
                boolean isSkele = dis.threatList.get(bestTarget).getClass().getSimpleName().matches("SkeletonEntity|IllusionerEntity|StrayEntity");
                boolean[] playerControlList = new boolean[8];
                for (int i = 0; i < 8; i++) {playerControlList[i] = false;}

                if (dis.playerPos.distanceTo(dis.threatList.get(bestTarget).getPos()) >
                        dis.playerPos.add(Math.cos(Math.toRadians(dis.player.getYaw() + 90)) * 0.2, 0, Math.sin(Math.toRadians(dis.player.getYaw() + 90)) * 0.2).distanceTo(dis.threatList.get(bestTarget).getPos())) {
                    playerControlList[3] = (isSkele);
                    playerControlList[4] = (!isSkele);
                } else {
                    playerControlList[4] = (isSkele);
                    playerControlList[3] = (!isSkele);
                }

                if (dis.playerPos.distanceTo(dis.threatList.get(bestTarget).getPos()) >
                        dis.playerPos.add(Math.cos(Math.toRadians(dis.player.getYaw())) * 0.2, 0, Math.sin(Math.toRadians(dis.player.getYaw())) * 0.2).distanceTo(dis.threatList.get(bestTarget).getPos())) {
                    playerControlList[5] = (isSkele);
                    playerControlList[6] = (!isSkele);
                } else {
                    playerControlList[6] = (isSkele);
                    playerControlList[5] = (!isSkele);
                }

                playerControlList = SmartWalk.moveInDir(new SimInstance(dis.client.player.isOnGround(), playerControlList, dis.client.player.getPos(), dis.client.player.getVelocity(), (float)(Math.PI + (-dis.client.player.getYaw() * Math.PI / 180.0))));
                dis.c_W = playerControlList[3];
                dis.c_S = playerControlList[4];
                dis.c_A = playerControlList[5];
                dis.c_D = playerControlList[6];
                dis.c_Z = playerControlList[0];
                dis.c_j = playerControlList[1];
                dis.c_spr = playerControlList[2];
            }


            if (dis.target.getClass().getSimpleName().matches("SkeletonEntity|IllusionerEntity|StrayEntity") &&
                    dis.mobMap.get(dis.target.getUuidAsString()) != null &&
                    dis.mobMap.get(dis.target.getUuidAsString()) >= 11) {
                dis.shieldTimer = 2;
            } else if (dis.target.getClass().getSimpleName().matches("BlazeEntity")) {
                for (int i = 0; i < dis.target.getDataTracker().getChangedEntries().size(); i++) {
                    if (dis.target.getDataTracker().getChangedEntries().get(i).id() == 16) {
                        dis.shieldTimer = 2;
                    }
                }
            } else if (dis.target.getClass().getSimpleName().matches("CreeperEntity")) {
                for (int i = 0; i < dis.target.getDataTracker().getChangedEntries().size(); i++) {
                    if (dis.target.getDataTracker().getChangedEntries().get(i).id() == 16) {
                        dis.shieldTimer = 2;
                    }
                }
            } else if (dis.target instanceof ArrowEntity) {
                dis.shieldTimer = 2;
            }
            //System.out.println(client.player.getOffHandStack().getRegistryEntry());
            if (dis.target.isAttackable() && dis.shieldTimer <= 0 && !dis.client.options.attackKey.isPressed() && dis.attackCooldown >= 12 && distance <= 3.0 && dis.target.isAttackable() && dis.target.isAlive()) {
                dis.client.interactionManager.attackEntity(dis.client.player, dis.target);
                dis.client.player.swingHand(dis.client.player.preferredHand);
                dis.attackCooldown = 0;
                dis.c_lc = true;
            } else {
                dis.c_lc = false;
                dis.releaseKey(dis.client.player, dis.client.options.attackKey);
            }
            if (dis.shieldTimer > 0) {
                dis.c_rc = true;
            }
            dis.pressKey(dis.client.player, dis.client.options.sprintKey, true);
        } else if (dis.isBotting) {
            //BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
            //simulateAction();
        }
    }
}
