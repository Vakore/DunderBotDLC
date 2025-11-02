package name.dunderbotdlc.physics;

import java.util.ArrayList;
import java.util.Collections;

import baritone.api.utils.input.Input;
import name.dunderbotdlc.DunderBotdlcClient;
import name.dunderbotdlc.structs.vec3e;
import net.minecraft.block.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import name.dunderbotdlc.structs.AABB;

import static name.dunderbotdlc.DunderBotdlcClient.blockCache;

public class SimInstance {
    //physics
    public double physicsStepHeight = 0.6;

    public double physicsgravity = 0.08; // blocks/tick^2 https://minecraft.gamepedia.com/Entity#Motion_of_entities
    public double physicsairdrag = 0.98; // actually (1 - drag)
    public double physicsyawSpeed = 3.0;
    public double physicspitchSpeed = 3.0;
    public double physicsplayerSpeed = 0.1;
    public double physicssprintSpeed = 0.3;
    public double physicssneakSpeed = 0.3;
    public double physicsstepHeight = 0.6; // how much height can the bot step on without jump
    public double physicsnegligeableVelocity = 0.003; // actually 0.005 for 1.8, but seems fine
    public double physicssoulsandSpeed = 0.4;
    public double physicshoneyblockSpeed = 0.4;
    public double physicshoneyblockJumpSpeed = 0.4;
    public double physicsladderMaxSpeed = 0.15;
    public double physicsladderClimbSpeed = 0.2;
    public double physicsplayerHalfWidth = 0.3;
    public double physicsplayerHeight = 1.8;
    public double physicswaterInertia = 0.8;
    public double physicslavaInertia = 0.5;
    public double physicsliquidAcceleration = 0.02;
    public double physicsairborneInertia = 0.91;
    public double physicsairborneAcceleration = 0.02;
    public double physicsdefaultSlipperiness = 0.6;
    public double physicsoutOfLiquidImpulse = 0.3;
    public double physicswaterGravity = 0.02;
    public double physicslavaGravity = 0.02;

    public Input leInput;
    public float yaw;
    public Vec3d pos;
    public double x;
    public double y;
    public double z;
    public double velX = 0.0;
    public double velY = 0.0;
    public double velZ = 0.0;

    public double startY;

    //todo
    public boolean isCollidedHorizontally = false;
    public boolean isCollidedVertically = false;
    public boolean isInWeb = false;
    public double dolphinsGrace = 0.0;
    public double depthStrider = 0.0;
    public double levitation = 0.0;

    public boolean onGround = false;
    public boolean isInWater = false;
    public boolean isInLava = false;
    public boolean controlsneak = false;
    public boolean controlsprint = false;
    public boolean controljump = false;
    public boolean controlforward = false;
    public boolean controlright = false;
    public boolean controlleft = false;
    public boolean controlback = false;
    public int jumpTicks = 0;
    public boolean jumpQueued = false;
    public double jumpBoost = 0.0;
    public boolean elytraFlying = false;


    private ClientWorld world;
    public boolean[] myControls;
    ArrayList<AABB> surroundingCachedBBs;
    ArrayList<AABB> surroundingCachedBBs2;

    public SimInstance(boolean grounded, boolean[] leControls, Vec3d ps, Vec3d pv, float ya) {
        this.yaw = ya;
        pos = ps;
        x = ps.x;
        y = ps.y;
        z = ps.z;
        this.velX = pv.x;
        this.velY = pv.y;
        this.velZ = pv.z;
        startY = y;
        world = name.dunderbotdlc.DunderBotdlcClient.client.world;
        this.onGround = grounded;
        controlsneak = leControls[0];
        controljump = leControls[1];
        controlsprint = leControls[2];
        controlforward = leControls[3];
        controlback = leControls[4];
        controlleft = leControls[5];
        controlright = leControls[6];
        myControls = leControls;

        surroundingCachedBBs = new ArrayList<>();
        surroundingCachedBBs2 = new ArrayList<>();
    }


    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }


    public void getCachedBBs(AABB queryBB, ArrayList<AABB> surroundingBBs) {
        //return surroundingBBs; //profiling
        for (int j = (int)(Math.floor(queryBB.minY) - 1.1); j <= (int)(Math.floor(queryBB.maxY) + 0.1); j++) {
            for (int k = (int)(Math.floor(queryBB.minZ) - 0.1); k <= (int)(Math.floor(queryBB.maxZ) + 0.1); k++) {
                for (int i = (int)(Math.floor(queryBB.minX) - 0.1); i <= (int)(Math.floor(queryBB.maxX) + 0.1); i++) {

                    //BlockPos blockPos = new BlockPos((int)Math.floor(i), (int)Math.floor(j), (int)Math.floor(k));
                    DunderBotdlcClient.CachedBlock blockData = blockCache.get(i, j, k);

                    // Get cached collision boxes
                    if (blockData.colliders != null) {
                        Collections.addAll(surroundingBBs, blockData.colliders);
                    }
                    /*VoxelShape bst = world.getBlockState(blockPos).getCollisionShape(this.world, blockPos);
                    if (bst != VoxelShapes.empty()) {
                        if (bst.getBoundingBoxes().size() > 0) {
                            for (int l = 0; l < bst.getBoundingBoxes().size(); l++) {
                                Box bstBox = bst.getBoundingBoxes().get(l);
                                surroundingBBs.add(new AABB((int)Math.floor(i) + bstBox.minX, (int)Math.floor(j) + bstBox.minY, (int)Math.floor(k) + bstBox.minZ, blockPos.getX() + bstBox.maxX, blockPos.getY() + bstBox.maxY, blockPos.getZ() + bstBox.maxZ));
                            }
                        }
                    }*/
                }
            }
        }
    }

    public ArrayList<AABB> getSurroundingBBs(AABB queryBB) {
        ArrayList<AABB> surroundingBBs = new ArrayList<AABB>();
        //return surroundingBBs; //profiling
        vec3e cursor = new vec3e(0.0, 0.0, 0.0);
        for (cursor.y = Math.floor(queryBB.minY) - 2; cursor.y <= Math.floor(queryBB.maxY) + 1; cursor.y++) {
            for (cursor.z = Math.floor(queryBB.minZ) - 1; cursor.z <= Math.floor(queryBB.maxZ) + 1; cursor.z++) {
                for (cursor.x = Math.floor(queryBB.minX) - 1; cursor.x <= Math.floor(queryBB.maxX) + 1; cursor.x++) {
                    BlockPos blockPos = new BlockPos((int)Math.floor(cursor.x), (int)Math.floor(cursor.y), (int)Math.floor(cursor.z));
                    VoxelShape bst = world.getBlockState(blockPos).getCollisionShape(this.world, blockPos);
                    if (bst != VoxelShapes.empty()) {
                        if (bst.getBoundingBoxes().size() > 0) {
                            for (int i = 0; i < bst.getBoundingBoxes().size(); i++) {
                                Box bstBox = bst.getBoundingBoxes().get(i);
                                surroundingBBs.add(new AABB((int)Math.floor(cursor.x) + bstBox.minX, (int)Math.floor(cursor.y) + bstBox.minY, (int)Math.floor(cursor.z) + bstBox.minZ, blockPos.getX() + bstBox.maxX, blockPos.getY() + bstBox.maxY, blockPos.getZ() + bstBox.maxZ));
                            }
                        }
                    }
                }
            }
        }
        return surroundingBBs;
    }

    public ArrayList<AABB> getWaterInBB(AABB queryBB) {
        ArrayList<AABB> waterBBs = new ArrayList<AABB>();
        //return waterBBs; //profiling
        vec3e cursor = new vec3e(0.0, 0.0, 0.0);
        for (cursor.y = Math.floor(queryBB.minY) - 1; cursor.y <= Math.floor(queryBB.maxY); cursor.y++) {
            for (cursor.z = Math.floor(queryBB.minZ); cursor.z <= Math.floor(queryBB.maxZ); cursor.z++) {
                for (cursor.x = Math.floor(queryBB.minX); cursor.x <= Math.floor(queryBB.maxX); cursor.x++) {
                    BlockPos blockPos = new BlockPos((int)Math.floor(cursor.x), (int)Math.floor(cursor.y), (int)Math.floor(cursor.z));
                    BlockState blockState = world.getBlockState(blockPos);
                    if (blockState != null) {
                        Block block = blockState.getBlock();
                        boolean isWater = blockState.getFluidState().isStill() || blockState.getFluidState().isOf(net.minecraft.fluid.Fluids.WATER);
                        boolean isWaterlogged = blockState.getBlock() instanceof Waterloggable && blockState.get(Properties.WATERLOGGED);

                        if (isWater || isWaterlogged) {
                            double waterLevel = cursor.y + 1 - blockState.getFluidState().getHeight(world, blockPos);
                            if (queryBB.maxY >= waterLevel) {
                                waterBBs.add(new AABB((int)Math.floor(cursor.x), (int)Math.floor(cursor.y), (int)Math.floor(cursor.z), blockPos.getX() + 1, waterLevel, blockPos.getZ() + 1));
                            }
                        }
                    }
                }
            }
        }
        return waterBBs;
    }

    public AABB getPlayerBB(Vec3d pos) {
        double w = 0.3;
        return new AABB(-w, 0, -w, w, 1.8, w).offset(pos.x, pos.y, pos.z);
    }

    public vec3e setPositionToBB(AABB bb) {
        return new vec3e(bb.minX + this.physicsplayerHalfWidth, bb.minY, bb.minZ + this.physicsplayerHalfWidth);
    }

    public void moveEntity(double dx, double dy, double dz) {
        if (this.isInWeb) {
            dx *= 0.25;
            dy *= 0.05;
            dz *= 0.25;
            this.velX = 0.0;
            this.velY = 0.0;
            this.velZ = 0.0;
            this.isInWeb = false;
        }

        double oldVelX = dx;
        double oldVelY = dy;
        double oldVelZ = dz;

        if (this.controlsneak && this.onGround) {
            double step = 0.05;

            // In the 3 loops below, y offset should be -1, but that doesn't reproduce vanilla behavior.
            for (; dx != 0.0 && getSurroundingBBs(getPlayerBB(new Vec3d(this.x, this.y, this.z)).offset(dx, 0.0, 0.0)).size() == 0; oldVelX = dx) {
                if (dx < step && dx >= -step) dx = 0.0;
                else if (dx > 0.0) dx -= step;
                else dx += step;
            }

            for (; dz != 0.0 && getSurroundingBBs(getPlayerBB(new Vec3d(this.x, this.y, this.z)).offset(0.0, 0.0, dz)).size() == 0; oldVelZ = dz) {
                if (dz < step && dz >= -step) dz = 0.0;
                else if (dz > 0.0) dz -= step;
                else dz += step;
            }

            while (dx != 0.0 && dz != 0.0 && getSurroundingBBs(getPlayerBB(new Vec3d(this.x, this.y, this.z)).offset(dx, 0.0, dz)).size() == 0) {
                if (dx < step && dx >= -step) dx = 0.0;
                else if (dx > 0.0) dx -= step;
                else dx += step;

                if (dz < step && dz >= -step) dz = 0.0;
                else if (dz > 0.0) dz -= step;
                else dz += step;

                oldVelX = dx;
                oldVelZ = dz;
            }
        }

        AABB playerBB = getPlayerBB(new Vec3d(this.x, this.y, this.z));
        AABB queryBB = playerBB.clone().extend(dx, dy, dz);
        this.surroundingCachedBBs.clear();
        getCachedBBs(queryBB, surroundingCachedBBs);
        AABB oldBB = playerBB.clone();

        for (AABB blockBB : surroundingCachedBBs) {
            dy = blockBB.computeOffsetY(playerBB, dy);
        }
        playerBB.offset(0.0, dy, 0.0);

        for (AABB blockBB : surroundingCachedBBs) {
            dx = blockBB.computeOffsetX(playerBB, dx);
        }
        playerBB.offset(dx, 0.0, 0.0);

        for (AABB blockBB : surroundingCachedBBs) {
            dz = blockBB.computeOffsetZ(playerBB, dz);
        }
        playerBB.offset(0.0, 0.0, dz);

        // Step on block if height < stepHeight
        if (this.physicsStepHeight > 0.0 && (dx != oldVelX || dz != oldVelZ) &&
                (this.onGround || (dy != oldVelY && oldVelY < 0.0)) &&
                (dx != oldVelX || dz != oldVelZ)) {
            double oldVelXCol = dx;
            double oldVelYCol = dy;
            double oldVelZCol = dz;
            AABB oldBBCol = playerBB.clone();

            dy = this.physicsStepHeight;
            AABB queryBB2 = oldBB.clone().extend(oldVelX, dy, oldVelZ);
            surroundingCachedBBs2.clear();
            getCachedBBs(queryBB2, surroundingCachedBBs2);

            AABB BB1 = oldBB.clone();
            AABB BB2 = oldBB.clone();
            AABB BB_XZ = BB1.clone().extend(dx, 0.0, dz);

            double dy1 = dy;
            double dy2 = dy;
            for (AABB blockBB : surroundingCachedBBs2) {
                dy1 = blockBB.computeOffsetY(BB_XZ, dy1);
                dy2 = blockBB.computeOffsetY(BB2, dy2);
            }
            BB1.offset(0.0, dy1, 0.0);
            BB2.offset(0.0, dy2, 0.0);

            double dx1 = oldVelX;
            double dx2 = oldVelX;
            for (AABB blockBB : surroundingCachedBBs2) {
                dx1 = blockBB.computeOffsetX(BB1, dx1);
                dx2 = blockBB.computeOffsetX(BB2, dx2);
            }
            BB1.offset(dx1, 0.0, 0.0);
            BB2.offset(dx2, 0.0, 0.0);

            double dz1 = oldVelZ;
            double dz2 = oldVelZ;
            for (AABB blockBB : surroundingCachedBBs2) {
                dz1 = blockBB.computeOffsetZ(BB1, dz1);
                dz2 = blockBB.computeOffsetZ(BB2, dz2);
            }
            BB1.offset(0.0, 0.0, dz1);
            BB2.offset(0.0, 0.0, dz2);

            double norm1 = dx1 * dx1 + dz1 * dz1;
            double norm2 = dx2 * dx2 + dz2 * dz2;

            if (norm1 > norm2) {
                dx = dx1;
                dy = -dy1;
                dz = dz1;
                playerBB = BB1;
            } else {
                dx = dx2;
                dy = -dy2;
                dz = dz2;
                playerBB = BB2;
            }

            for (AABB blockBB : surroundingCachedBBs2) {
                dy = blockBB.computeOffsetY(playerBB, dy);
            }
            playerBB.offset(0.0, dy, 0.0);

            if (oldVelXCol * oldVelXCol + oldVelZCol * oldVelZCol >= dx * dx + dz * dz) {
                dx = oldVelXCol;
                dy = oldVelYCol;
                dz = oldVelZCol;
                playerBB = oldBBCol;
            }
        }

        // Update flags
        vec3e newPos1 = setPositionToBB(playerBB);
        this.x = newPos1.x;
        this.y = newPos1.y;
        this.z = newPos1.z;
        this.isCollidedHorizontally = (dx != oldVelX || dz != oldVelZ);
        this.isCollidedVertically = (dy != oldVelY);
        this.onGround = (this.isCollidedVertically && oldVelY < 0);

        if (dx != oldVelX) this.velX = 0.0;
        if (dz != oldVelZ) this.velZ = 0.0;
        if (dy != oldVelY) {
            this.velY = 0.0;
        }

        // Finally, apply block collisions (web, bubble columns, etc.)
        playerBB.contract(0.001, 0.001, 0.001);
        for (int j = (int)Math.floor(playerBB.minY); j <= (int)Math.floor(playerBB.maxY); j++) {
            for (int k = (int)Math.floor(playerBB.minZ); k <= (int)Math.floor(playerBB.maxZ); k++) {
                for (int i = (int)Math.floor(playerBB.minX); i <= (int)Math.floor(playerBB.maxX); i++) {
                    DunderBotdlcClient.CachedBlock blockState = blockCache.get(i, j, k);//world.getBlockState(blockPos);
                    if (blockState != null) {
                        if (blockState.isWeb) {
                            this.isInWeb = true;
                        } else if (blockState.isBubbleDrag) {
                            boolean down = blockState.bubbleDrag;
                            BlockState aboveBlock = world.getBlockState(
                                    new BlockPos(i, j+1, k)
                            );
                            boolean bubbleDragIsSurface = (aboveBlock != null && aboveBlock.getBlock() == Blocks.AIR);
                            if (down) {
                                this.velY = Math.max(bubbleDragIsSurface ? -0.9 : -0.3, this.velY - 0.03);
                            } else {
                                this.velY = Math.min(bubbleDragIsSurface ? 1.8 : 0.7, this.velY + (bubbleDragIsSurface ? 0.1 : 0.06));
                            }
                        }
                    }
                }
            }
        }
    }

    public void applyHeading(double strafe, double forward, double multiplier) {
        double speed = Math.sqrt(strafe * strafe + forward * forward);
        if (speed < 0.01) return;

        speed = multiplier / Math.max(speed, 1);

        strafe *= speed;
        forward *= speed;

        double yaw = Math.PI - this.yaw;
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);

        this.velX += strafe * cos - forward * sin;
        this.velZ += forward * cos + strafe * sin;
    }

    public void moveEntityWithHeading(double strafe, double forward) {
        double gravityMultiplier = 1.0;

        if (this.isInWater || this.isInLava) {
            // Water / Lava movement
            double lastY = this.y;
            double acceleration = physicsliquidAcceleration;
            double inertia = this.isInWater ? physicswaterInertia : physicslavaInertia;
            double horizontalInertia = inertia;

            if (this.isInWater) {
                double strider = Math.min(this.depthStrider, 3);
                if (!this.onGround) {
                    strider *= 0.5;
                }
                if (strider > 0) {
                    horizontalInertia += (0.546 - horizontalInertia) * strider / 3;
                    acceleration += (0.7 - acceleration) * strider / 3;
                }

                if (this.dolphinsGrace > 0) horizontalInertia = 0.96;
            }

            this.applyHeading(strafe, forward, acceleration);
            this.moveEntity(this.velX, this.velY, this.velZ);
            this.velY *= inertia;
            this.velY -= (this.isInWater ? physicswaterGravity : physicslavaGravity) * gravityMultiplier;
            this.velX *= horizontalInertia;
            this.velZ *= horizontalInertia;

            if (this.isCollidedHorizontally && this.velY < 0.0) {
                AABB queryBB = getPlayerBB(new Vec3d(this.x + this.velX, this.y + this.velY + 0.6, this.z + this.velZ));
                if (getSurroundingBBs(queryBB).isEmpty()) {
                    this.velY = physicsoutOfLiquidImpulse; // jump out of liquid
                }
            }
        } else {
            // Normal movement
            double acceleration = 0.0;
            double inertia = 0.0;

            if (this.onGround) {
                double playerSpeedAttribute = physicsplayerSpeed;

                if (this.controlsprint) {
                    playerSpeedAttribute += playerSpeedAttribute * physicssprintSpeed;
                }

                double attributeSpeed = playerSpeedAttribute;
                inertia = physicsdefaultSlipperiness * 0.91;
                acceleration = attributeSpeed * (0.1627714 / (inertia * inertia * inertia));
                if (acceleration < 0.0) acceleration = 0.0;
            } else {
                acceleration = physicsairborneAcceleration;
                inertia = physicsairborneInertia;

                if (this.controlsprint) {
                    double airSprintFactor = physicsairborneAcceleration * 0.3;
                    acceleration += airSprintFactor;
                }
            }

            applyHeading(strafe, forward, acceleration);

            //if (this.controljump) return; //profiling
            moveEntity(this.velX, this.velY, this.velZ);

            // Apply friction and gravity
            if (this.levitation > 0.0) {
                this.velY += (0.05 * this.levitation - this.velY) * 0.2;
            } else {
                this.velY -= physicsgravity * gravityMultiplier;
            }
            this.velY *= physicsairdrag;
            this.velX *= inertia;
            this.velZ *= inertia;
        }
    }

    boolean isInWaterApplyCurrent(ClientWorld world, AABB bb, Vec3d vel) {
        Vec3d acceleration = new Vec3d(0, 0, 0);
        ArrayList<AABB> waterBlocks = getWaterInBB(bb);
        boolean isInWater = !waterBlocks.isEmpty();
        for (AABB pos : waterBlocks) {
            BlockPos block = new BlockPos((int)pos.minX, (int)pos.minY, (int)pos.minZ);
            Vec3d flow = world.getFluidState(block).getVelocity(world, block);
            acceleration = acceleration.add(flow);
        }

        double len = acceleration.length();
        if (len > 0) {
            double factor = 0.014 / len;
            velX += acceleration.x * factor;
            velY += acceleration.y * factor;
            velZ += acceleration.z * factor;
        }
        return isInWater;
    }

    public void simulatePlayer() {
        //if (this.controljump) return; //profiling
        AABB waterBB = getPlayerBB(new Vec3d(this.x, this.y, this.z)).contract(0.001, 0.401, 0.001);
        AABB lavaBB = getPlayerBB(new Vec3d(this.x, this.y, this.z)).contract(0.1, 0.4, 0.1);

        this.isInWater = isInWaterApplyCurrent(world, waterBB, new Vec3d(this.velX, this.velY, this.velZ));
        this.isInLava = false;

        // Reset velocity component if it falls under the threshold
        if (Math.abs(this.velX) < physicsnegligeableVelocity) this.velX = 0.0;
        if (Math.abs(this.velY) < physicsnegligeableVelocity) this.velY = 0.0;
        if (Math.abs(this.velZ) < physicsnegligeableVelocity) this.velZ = 0.0;

        // Handle inputs
        if (this.controljump || this.jumpQueued) {
            if (this.jumpTicks > 0) this.jumpTicks--;
            if (this.isInWater || this.isInLava) {
                this.velY += 0.04;
            } else if (this.onGround && this.jumpTicks == 0) {
                this.velY = 0.42;
                if (this.jumpBoost > 0.0) {
                    this.velY += 0.1 * this.jumpBoost;
                }
                if (this.controlsprint) {
                    double yaw = Math.PI - this.yaw;
                    this.velX -= Math.sin(yaw) * 0.2;
                    this.velZ += Math.cos(yaw) * 0.2;
                }
                this.jumpTicks = 0;
            }
        } else {
            this.jumpTicks = 0;
        }
        this.jumpQueued = false;

        double strafe = 0.0;
        double forward = 0.0;
        if (this.controlright) {strafe++;}
        if (this.controlleft) {strafe--;}
        if (this.controlforward) {forward++;}
        if (this.controlback) {forward--;}
        strafe *= 0.98;
        forward *= 0.98;

        if (this.controlsneak) {
            strafe *= physicssneakSpeed;
            forward *= physicssneakSpeed;
        }

        this.elytraFlying = false;
        //if (this.controljump) return; //profiling
        this.moveEntityWithHeading(strafe, forward);
    }

    public SimInstance clone() {
        SimInstance newInstance = new SimInstance(
                this.onGround,
                this.myControls.clone(),
                new Vec3d(this.x, this.y, this.z),
                new Vec3d(this.velX, this.velY, this.velZ),
                this.yaw
        );

        // Copy additional state
        newInstance.isCollidedHorizontally = this.isCollidedHorizontally;
        newInstance.isCollidedVertically = this.isCollidedVertically;
        newInstance.isInWeb = this.isInWeb;
        newInstance.isInWater = this.isInWater;
        newInstance.isInLava = this.isInLava;
        newInstance.dolphinsGrace = this.dolphinsGrace;
        newInstance.depthStrider = this.depthStrider;
        newInstance.levitation = this.levitation;
        newInstance.jumpTicks = this.jumpTicks;
        newInstance.jumpQueued = this.jumpQueued;
        newInstance.jumpBoost = this.jumpBoost;
        newInstance.elytraFlying = this.elytraFlying;
        newInstance.startY = this.startY;

        return newInstance;
    }

    // Helper method to get current position as Vec3d
    public Vec3d getPosition() {
        return new Vec3d(this.x, this.y, this.z);
    }

    // Helper method to get current velocity as Vec3d
    public Vec3d getVelocity() {
        return new Vec3d(this.velX, this.velY, this.velZ);
    }
}