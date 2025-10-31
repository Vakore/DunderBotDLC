package name.dunderbotdlc.physics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import name.dunderbotdlc.structs.AABB;
import net.minecraft.block.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public class OptimizedSimInstance {
    // Physics constants
    private static final double GRAVITY = 0.08;
    private static final double AIR_DRAG = 0.98;
    private static final double STEP_HEIGHT = 0.6;
    private static final double PLAYER_SPEED = 0.1;
    private static final double SPRINT_SPEED = 0.3;
    private static final double SNEAK_SPEED = 0.3;
    private static final double NEGLIGEABLE_VELOCITY = 0.003;
    private static final double PLAYER_HALF_WIDTH = 0.3;
    private static final double PLAYER_HEIGHT = 1.8;
    private static final double WATER_INERTIA = 0.8;
    private static final double LAVA_INERTIA = 0.5;
    private static final double LIQUID_ACCELERATION = 0.02;
    private static final double AIRBORNE_INERTIA = 0.91;
    private static final double AIRBORNE_ACCELERATION = 0.02;
    private static final double DEFAULT_SLIPPERINESS = 0.6;
    private static final double OUT_OF_LIQUID_IMPULSE = 0.3;
    private static final double WATER_GRAVITY = 0.02;
    private static final double LAVA_GRAVITY = 0.02;

    // State variables
    public double x, y, z;
    public double velX, velY, velZ;
    public float yaw;
    public boolean onGround;

    private boolean isCollidedHorizontally;
    private boolean isCollidedVertically;
    private boolean isInWeb;
    private boolean isInWater;
    private boolean isInLava;

    // Controls
    private boolean controlSneak, controlSprint, controlJump;
    private boolean controlForward, controlBack, controlLeft, controlRight;

    private int jumpTicks;
    private boolean jumpQueued;
    private double jumpBoost;

    // World reference
    private ClientWorld world;

    // Unified cache for all block data
    private BlockDataCache blockCache;

    // Pre-allocated objects to avoid GC pressure
    private final AABB tempPlayerBB = new AABB(0, 0, 0, 0, 0, 0);
    private final AABB tempQueryBB = new AABB(0, 0, 0, 0, 0, 0);
    private final AABB tempBB1 = new AABB(0, 0, 0, 0, 0, 0);
    private final AABB tempBB2 = new AABB(0, 0, 0, 0, 0, 0);
    private final ArrayList<AABB> tempCollisionList = new ArrayList<>(64);
    private final ArrayList<AABB> tempWaterList = new ArrayList<>(16);

    public OptimizedSimInstance(Vec3d pos, Vec3d vel, float yaw, boolean onGround,
                                boolean[] controls, ClientWorld world) {
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.velX = vel.x;
        this.velY = vel.y;
        this.velZ = vel.z;
        this.yaw = yaw;
        this.onGround = onGround;
        this.world = world;

        // Parse controls
        if (controls.length >= 7) {
            this.controlSneak = controls[0];
            this.controlJump = controls[1];
            this.controlSprint = controls[2];
            this.controlForward = controls[3];
            this.controlBack = controls[4];
            this.controlLeft = controls[5];
            this.controlRight = controls[6];
        }

        // Initialize cache
        this.blockCache = new BlockDataCache(world);

        this.jumpTicks = 0;
        this.jumpQueued = false;
        this.jumpBoost = 0.0;
    }

    /**
     * Simulates player physics until touching ground or max ticks reached
     * @param maxTicks Maximum number of ticks to simulate
     * @return SimulationResult containing final state and tick count
     */
    public SimulationResult simulateUntilGrounded(int maxTicks) {
        int ticksSimulated = 0;
        boolean touchedGround = this.onGround;

        // Fast path: already on ground
        if (touchedGround) {
            return new SimulationResult(this, 0, true);
        }

        // Estimate fall distance and pre-cache blocks
        double estimatedFallDistance = Math.abs(velY) * maxTicks + 0.5 * GRAVITY * maxTicks * maxTicks;
        int minBlockY = (int) Math.floor(y - estimatedFallDistance - 2);
        int maxBlockY = (int) Math.ceil(y + PLAYER_HEIGHT + 1);
        int minBlockX = (int) Math.floor(x - PLAYER_HALF_WIDTH - 2);
        int maxBlockX = (int) Math.ceil(x + PLAYER_HALF_WIDTH + 2);
        int minBlockZ = (int) Math.floor(z - PLAYER_HALF_WIDTH - 2);
        int maxBlockZ = (int) Math.ceil(z + PLAYER_HALF_WIDTH + 2);

        // Pre-warm cache with likely blocks
        blockCache.preloadRegion(minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ);

        while (ticksSimulated < maxTicks && !touchedGround) {
            simulatePlayerTick();
            ticksSimulated++;

            // Check if we've touched ground
            if (this.onGround) {
                touchedGround = true;
                break;
            }

            // Early exit if velocity is negligible and we're falling
            if (Math.abs(velX) < NEGLIGEABLE_VELOCITY &&
                    Math.abs(velZ) < NEGLIGEABLE_VELOCITY &&
                    velY < -0.5 && ticksSimulated > 5) {

                // Try to fast-forward using physics equations
                int ticksToSkip = fastForwardFreeFall(maxTicks - ticksSimulated);
                if (ticksToSkip > 0) {
                    ticksSimulated += ticksToSkip;
                    if (this.onGround) {
                        touchedGround = true;
                        break;
                    }
                }
            }
        }

        return new SimulationResult(this, ticksSimulated, touchedGround);
    }

    /**
     * Fast-forward free fall when no horizontal movement
     * @param maxTicks Maximum ticks to skip
     * @return Number of ticks skipped
     */
    private int fastForwardFreeFall(int maxTicks) {
        double currentY = this.y;
        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int startBlockY = (int) Math.floor(currentY) - 1;

        // Scan downward for collision
        for (int by = startBlockY; by >= startBlockY - maxTicks && by > -64; by--) {
            BlockPos pos = new BlockPos(blockX, by, blockZ);
            CachedBlockData blockData = blockCache.getBlockData(pos);

            if (blockData.hasCollision) {
                // Found a block, calculate time to collision
                double targetY = by + blockData.maxCollisionY;
                double distance = currentY - targetY;

                if (distance > 0) {
                    // Use physics to calculate ticks needed
                    double v0 = velY;
                    double a = -GRAVITY;

                    // Quadratic formula: d = v0*t + 0.5*a*t^2
                    double discriminant = v0 * v0 - 2 * a * distance;
                    if (discriminant >= 0) {
                        double t = (-v0 - Math.sqrt(discriminant)) / a;
                        int ticks = (int) Math.floor(t);

                        if (ticks > 0 && ticks <= maxTicks) {
                            // Apply the motion
                            for (int i = 0; i < ticks; i++) {
                                velY -= GRAVITY;
                                velY *= AIR_DRAG;
                                y += velY;
                            }

                            // Final collision check
                            simulatePlayerTick();
                            return ticks + 1;
                        }
                    }
                }
                break;
            }
        }

        return 0;
    }

    private void simulatePlayerTick() {
        updateWaterState();

        // Reset negligeable velocities
        if (Math.abs(velX) < NEGLIGEABLE_VELOCITY) velX = 0.0;
        if (Math.abs(velY) < NEGLIGEABLE_VELOCITY) velY = 0.0;
        if (Math.abs(velZ) < NEGLIGEABLE_VELOCITY) velZ = 0.0;

        handleJumping();

        double strafe = 0.0;
        double forward = 0.0;
        if (controlRight) strafe++;
        if (controlLeft) strafe--;
        if (controlForward) forward++;
        if (controlBack) forward--;
        strafe *= 0.98;
        forward *= 0.98;

        if (controlSneak) {
            strafe *= SNEAK_SPEED;
            forward *= SNEAK_SPEED;
        }

        moveEntityWithHeading(strafe, forward);
    }

    private void updateWaterState() {
        getPlayerBB(tempPlayerBB);
        tempPlayerBB.contract(0.001, 0.401, 0.001);

        this.isInWater = isInWaterApplyCurrent(tempPlayerBB);
        this.isInLava = false;
    }

    private void handleJumping() {
        if (controlJump || jumpQueued) {
            if (jumpTicks > 0) jumpTicks--;
            if (isInWater || isInLava) {
                velY += 0.04;
            } else if (onGround && jumpTicks == 0) {
                velY = 0.42;
                if (jumpBoost > 0.0) {
                    velY += 0.1 * jumpBoost;
                }
                if (controlSprint) {
                    double yawRad = Math.PI - yaw;
                    velX -= Math.sin(yawRad) * 0.2;
                    velZ += Math.cos(yawRad) * 0.2;
                }
                jumpTicks = 0;
            }
        } else {
            jumpTicks = 0;
        }
        jumpQueued = false;
    }

    private void moveEntityWithHeading(double strafe, double forward) {
        if (isInWater || isInLava) {
            moveInLiquid(strafe, forward);
        } else {
            moveOnLand(strafe, forward);
        }
    }

    private void moveInLiquid(double strafe, double forward) {
        double acceleration = LIQUID_ACCELERATION;
        double inertia = isInWater ? WATER_INERTIA : LAVA_INERTIA;
        double horizontalInertia = inertia;

        applyHeading(strafe, forward, acceleration);
        moveEntity(velX, velY, velZ);

        velY *= inertia;
        velY -= (isInWater ? WATER_GRAVITY : LAVA_GRAVITY);
        velX *= horizontalInertia;
        velZ *= horizontalInertia;

        if (isCollidedHorizontally && velY < 0.0) {
            getPlayerBB(tempQueryBB);
            tempQueryBB.offset(velX, velY + 0.6, velZ);
            if (getSurroundingBBs(tempQueryBB, tempCollisionList).isEmpty()) {
                velY = OUT_OF_LIQUID_IMPULSE;
            }
        }
    }

    private void moveOnLand(double strafe, double forward) {
        double acceleration;
        double inertia;

        if (onGround) {
            double playerSpeedAttribute = PLAYER_SPEED;
            if (controlSprint) {
                playerSpeedAttribute += playerSpeedAttribute * SPRINT_SPEED;
            }

            inertia = DEFAULT_SLIPPERINESS * 0.91;
            acceleration = playerSpeedAttribute * (0.1627714 / (inertia * inertia * inertia));
            if (acceleration < 0.0) acceleration = 0.0;
        } else {
            acceleration = AIRBORNE_ACCELERATION;
            inertia = AIRBORNE_INERTIA;

            if (controlSprint) {
                acceleration += AIRBORNE_ACCELERATION * 0.3;
            }
        }

        applyHeading(strafe, forward, acceleration);
        moveEntity(velX, velY, velZ);

        velY -= GRAVITY;
        velY *= AIR_DRAG;
        velX *= inertia;
        velZ *= inertia;
    }

    private void applyHeading(double strafe, double forward, double multiplier) {
        double speed = Math.sqrt(strafe * strafe + forward * forward);
        if (speed < 0.01) return;

        speed = multiplier / Math.max(speed, 1);
        strafe *= speed;
        forward *= speed;

        double yawRad = Math.PI - this.yaw;
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        velX += strafe * cos - forward * sin;
        velZ += forward * cos + strafe * sin;
    }

    private void moveEntity(double dx, double dy, double dz) {
        if (isInWeb) {
            dx *= 0.25;
            dy *= 0.05;
            dz *= 0.25;
            velX = 0.0;
            velY = 0.0;
            velZ = 0.0;
            isInWeb = false;
        }

        double oldVelX = dx;
        double oldVelY = dy;
        double oldVelZ = dz;

        // Sneak edge detection
        if (controlSneak && onGround) {
            dx = applySneakClamp(dx, dz, true);
            dz = applySneakClamp(dz, dx, false);
        }

        getPlayerBB(tempPlayerBB);
        AABB playerBB = tempPlayerBB.clone();
        tempQueryBB.set(playerBB);
        tempQueryBB.extend(dx, dy, dz);

        ArrayList<AABB> surroundingBBs = getSurroundingBBs(tempQueryBB, tempCollisionList);
        AABB oldBB = playerBB.clone();

        // Y collision
        for (AABB blockBB : surroundingBBs) {
            dy = blockBB.computeOffsetY(playerBB, dy);
        }
        playerBB.offset(0.0, dy, 0.0);

        // X collision
        for (AABB blockBB : surroundingBBs) {
            dx = blockBB.computeOffsetX(playerBB, dx);
        }
        playerBB.offset(dx, 0.0, 0.0);

        // Z collision
        for (AABB blockBB : surroundingBBs) {
            dz = blockBB.computeOffsetZ(playerBB, dz);
        }
        playerBB.offset(0.0, 0.0, dz);

        // Step assist
        if (STEP_HEIGHT > 0.0 && (onGround || (dy != oldVelY && oldVelY < 0.0)) &&
                (dx != oldVelX || dz != oldVelZ)) {

            double[] result = handleStepUp(oldBB, surroundingBBs, oldVelX, oldVelY, oldVelZ, dx, dy, dz);
            dx = result[0];
            dy = result[1];
            dz = result[2];
            playerBB.set(tempPlayerBB);
        }

        updatePositionAndFlags(playerBB, dx, dy, dz, oldVelX, oldVelY, oldVelZ);
        handleBlockCollisions(playerBB);
    }

    private double applySneakClamp(double primary, double secondary, boolean isX) {
        double step = 0.05;
        double oldPrimary = primary;

        getPlayerBB(tempQueryBB);

        while (primary != 0.0) {
            tempBB1.set(tempQueryBB);
            if (isX) {
                tempBB1.offset(primary, 0.0, 0.0);
            } else {
                tempBB1.offset(0.0, 0.0, primary);
            }

            if (getSurroundingBBs(tempBB1, tempCollisionList).isEmpty()) {
                oldPrimary = primary;
                if (Math.abs(primary) < step) {
                    primary = 0.0;
                } else if (primary > 0.0) {
                    primary -= step;
                } else {
                    primary += step;
                }
            } else {
                break;
            }
        }

        return oldPrimary;
    }

    private double[] handleStepUp(AABB oldBB, ArrayList<AABB> surroundingBBs,
                                  double oldVelX, double oldVelY, double oldVelZ,
                                  double dx, double dy, double dz) {
        double oldVelXCol = dx;
        double oldVelYCol = dy;
        double oldVelZCol = dz;
        AABB oldBBCol = tempPlayerBB.clone();

        dy = STEP_HEIGHT;
        tempQueryBB.set(oldBB);
        tempQueryBB.extend(oldVelX, dy, oldVelZ);
        ArrayList<AABB> surroundingBBs2 = getSurroundingBBs(tempQueryBB, new ArrayList<>(64));

        tempBB1.set(oldBB);
        tempBB2.set(oldBB);
        AABB BB_XZ = tempBB1.clone();
        BB_XZ.extend(dx, 0.0, dz);

        double dy1 = dy;
        double dy2 = dy;
        for (AABB blockBB : surroundingBBs2) {
            dy1 = blockBB.computeOffsetY(BB_XZ, dy1);
            dy2 = blockBB.computeOffsetY(tempBB2, dy2);
        }
        tempBB1.offset(0.0, dy1, 0.0);
        tempBB2.offset(0.0, dy2, 0.0);

        double dx1 = oldVelX;
        double dx2 = oldVelX;
        for (AABB blockBB : surroundingBBs2) {
            dx1 = blockBB.computeOffsetX(tempBB1, dx1);
            dx2 = blockBB.computeOffsetX(tempBB2, dx2);
        }
        tempBB1.offset(dx1, 0.0, 0.0);
        tempBB2.offset(dx2, 0.0, 0.0);

        double dz1 = oldVelZ;
        double dz2 = oldVelZ;
        for (AABB blockBB : surroundingBBs2) {
            dz1 = blockBB.computeOffsetZ(tempBB1, dz1);
            dz2 = blockBB.computeOffsetZ(tempBB2, dz2);
        }
        tempBB1.offset(0.0, 0.0, dz1);
        tempBB2.offset(0.0, 0.0, dz2);

        double norm1 = dx1 * dx1 + dz1 * dz1;
        double norm2 = dx2 * dx2 + dz2 * dz2;

        AABB selectedBB;
        if (norm1 > norm2) {
            dx = dx1;
            dy = -dy1;
            dz = dz1;
            selectedBB = tempBB1;
        } else {
            dx = dx2;
            dy = -dy2;
            dz = dz2;
            selectedBB = tempBB2;
        }

        for (AABB blockBB : surroundingBBs2) {
            dy = blockBB.computeOffsetY(selectedBB, dy);
        }
        selectedBB.offset(0.0, dy, 0.0);

        if (oldVelXCol * oldVelXCol + oldVelZCol * oldVelZCol >= dx * dx + dz * dz) {
            dx = oldVelXCol;
            dy = oldVelYCol;
            dz = oldVelZCol;
            tempPlayerBB.set(oldBBCol);
        } else {
            tempPlayerBB.set(selectedBB);
        }

        return new double[]{dx, dy, dz};
    }

    private void updatePositionAndFlags(AABB playerBB, double dx, double dy, double dz,
                                        double oldVelX, double oldVelY, double oldVelZ) {
        x = playerBB.minX + PLAYER_HALF_WIDTH;
        y = playerBB.minY;
        z = playerBB.minZ + PLAYER_HALF_WIDTH;

        isCollidedHorizontally = (dx != oldVelX || dz != oldVelZ);
        isCollidedVertically = (dy != oldVelY);
        onGround = (isCollidedVertically && oldVelY < 0);

        if (dx != oldVelX) velX = 0.0;
        if (dz != oldVelZ) velZ = 0.0;
        if (dy != oldVelY) velY = 0.0;
    }

    private void handleBlockCollisions(AABB playerBB) {
        playerBB.contract(0.001, 0.001, 0.001);

        int minX = (int) Math.floor(playerBB.minX);
        int minY = (int) Math.floor(playerBB.minY);
        int minZ = (int) Math.floor(playerBB.minZ);
        int maxX = (int) Math.floor(playerBB.maxX);
        int maxY = (int) Math.floor(playerBB.maxY);
        int maxZ = (int) Math.floor(playerBB.maxZ);

        for (int by = minY; by <= maxY; by++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                for (int bx = minX; bx <= maxX; bx++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    CachedBlockData blockData = blockCache.getBlockData(pos);

                    if (blockData.block == Blocks.COBWEB) {
                        isInWeb = true;
                    } else if (blockData.block == Blocks.BUBBLE_COLUMN) {
                        handleBubbleColumn(blockData, bx, by, bz);
                    }
                }
            }
        }
    }

    private void handleBubbleColumn(CachedBlockData blockData, int bx, int by, int bz) {
        boolean down = blockData.isDragBubble;
        BlockPos abovePos = new BlockPos(bx, by + 1, bz);
        CachedBlockData aboveData = blockCache.getBlockData(abovePos);
        boolean isSurface = (aboveData.block == Blocks.AIR);

        if (down) {
            velY = Math.max(isSurface ? -0.9 : -0.3, velY - 0.03);
        } else {
            velY = Math.min(isSurface ? 1.8 : 0.7, velY + (isSurface ? 0.1 : 0.06));
        }
    }

    private void getPlayerBB(AABB out) {
        out.set(-PLAYER_HALF_WIDTH + x, y, -PLAYER_HALF_WIDTH + z,
                PLAYER_HALF_WIDTH + x, PLAYER_HEIGHT + y, PLAYER_HALF_WIDTH + z);
    }

    private ArrayList<AABB> getSurroundingBBs(AABB queryBB, ArrayList<AABB> out) {
        out.clear();

        int minX = (int) Math.floor(queryBB.minX) - 1;
        int minY = (int) Math.floor(queryBB.minY) - 2;
        int minZ = (int) Math.floor(queryBB.minZ) - 1;
        int maxX = (int) Math.floor(queryBB.maxX) + 1;
        int maxY = (int) Math.floor(queryBB.maxY) + 1;
        int maxZ = (int) Math.floor(queryBB.maxZ) + 1;

        for (int by = minY; by <= maxY; by++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                for (int bx = minX; bx <= maxX; bx++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    CachedBlockData blockData = blockCache.getBlockData(pos);

                    // Get cached collision boxes
                    if (blockData.collisionBoxes != null) {
                        out.addAll(blockData.collisionBoxes);
                    }
                }
            }
        }

        return out;
    }

    private boolean isInWaterApplyCurrent(AABB bb) {
        tempWaterList.clear();
        getWaterInBB(bb, tempWaterList);

        if (tempWaterList.isEmpty()) {
            return false;
        }

        Vec3d acceleration = Vec3d.ZERO;
        for (AABB waterPos : tempWaterList) {
            BlockPos pos = new BlockPos((int) waterPos.minX, (int) waterPos.minY, (int) waterPos.minZ);
            CachedBlockData blockData = blockCache.getBlockData(pos);

            // Use cached water flow if available
            if (blockData.waterFlow != null) {
                acceleration = acceleration.add(blockData.waterFlow);
            }
        }

        double len = acceleration.length();
        if (len > 0) {
            double factor = 0.014 / len;
            velX += acceleration.x * factor;
            velY += acceleration.y * factor;
            velZ += acceleration.z * factor;
        }

        return true;
    }

    private void getWaterInBB(AABB queryBB, ArrayList<AABB> out) {
        out.clear();

        int minX = (int) Math.floor(queryBB.minX);
        int minY = (int) Math.floor(queryBB.minY) - 1;
        int minZ = (int) Math.floor(queryBB.minZ);
        int maxX = (int) Math.floor(queryBB.maxX);
        int maxY = (int) Math.floor(queryBB.maxY);
        int maxZ = (int) Math.floor(queryBB.maxZ);

        for (int by = minY; by <= maxY; by++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                for (int bx = minX; bx <= maxX; bx++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    CachedBlockData blockData = blockCache.getBlockData(pos);

                    if (blockData.isWater || blockData.isWaterlogged) {
                        double waterLevel = by + 1 - blockData.waterHeight;
                        if (queryBB.maxY >= waterLevel) {
                            // Use cached water AABB if available
                            if (blockData.waterAABB != null) {
                                out.add(blockData.waterAABB);
                            } else {
                                out.add(new AABB(bx, by, bz, bx + 1, waterLevel, bz + 1));
                            }
                        }
                    }
                }
            }
        }
    }

    public Vec3d getPosition() {
        return new Vec3d(x, y, z);
    }

    public Vec3d getVelocity() {
        return new Vec3d(velX, velY, velZ);
    }

    public void clearCache() {
        blockCache.clear();
    }

    // Cached block data structure
    private static class CachedBlockData {
        public final Block block;
        public final boolean hasCollision;
        public final boolean isWater;
        public final boolean isWaterlogged;
        public final boolean isDragBubble;
        public final double waterHeight;
        public final double maxCollisionY;
        public final List<AABB> collisionBoxes;
        public final AABB waterAABB;
        public final Vec3d waterFlow;

        public CachedBlockData(ClientWorld world, BlockPos pos) {
            BlockState state = world.getBlockState(pos);
            this.block = state.getBlock();

            // Cache collision data
            VoxelShape collisionShape = state.getCollisionShape(world, pos);
            this.hasCollision = collisionShape != VoxelShapes.empty();

            if (hasCollision) {
                List<Box> boxes = collisionShape.getBoundingBoxes();
                this.collisionBoxes = new ArrayList<>(boxes.size());
                double maxY = 0.0;

                for (Box box : boxes) {
                    AABB aabb = new AABB(
                            pos.getX() + box.minX, pos.getY() + box.minY, pos.getZ() + box.minZ,
                            pos.getX() + box.maxX, pos.getY() + box.maxY, pos.getZ() + box.maxZ
                    );
                    this.collisionBoxes.add(aabb);
                    maxY = Math.max(maxY, box.maxY);
                }
                this.maxCollisionY = maxY;
            } else {
                this.collisionBoxes = null;
                this.maxCollisionY = 0.0;
            }

            // Cache water data
            boolean isWaterFluid = state.getFluidState().isStill() ||
                    state.getFluidState().isOf(net.minecraft.fluid.Fluids.WATER);
            boolean isWaterlogged = (block instanceof Waterloggable) &&
                    state.contains(Properties.WATERLOGGED) &&
                    state.get(Properties.WATERLOGGED);

            this.isWater = isWaterFluid;
            this.isWaterlogged = isWaterlogged;

            if (isWater || isWaterlogged) {
                this.waterHeight = state.getFluidState().getHeight(world, pos);
                double waterLevel = pos.getY() + 1 - this.waterHeight;
                this.waterAABB = new AABB(pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, waterLevel, pos.getZ() + 1);
                this.waterFlow = world.getFluidState(pos).getVelocity(world, pos);
            } else {
                this.waterHeight = 0.0;
                this.waterAABB = null;
                this.waterFlow = null;
            }

            // Cache bubble column data
            if (block == Blocks.BUBBLE_COLUMN) {
                this.isDragBubble = state.get(BubbleColumnBlock.DRAG);
            } else {
                this.isDragBubble = false;
            }
        }
    }

    // Unified block data cache
    private static class BlockDataCache {
        private final Map<Long, CachedBlockData> cache = new HashMap<>();
        private final ClientWorld world;

        public BlockDataCache(ClientWorld world) {
            this.world = world;
        }

        public void preloadRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        getBlockData(pos);
                    }
                }
            }
        }

        public CachedBlockData getBlockData(BlockPos pos) {
            long key = pos.asLong();
            CachedBlockData cached = cache.get(key);
            if (cached == null) {
                cached = new CachedBlockData(world, pos);
                cache.put(key, cached);
            }
            return cached;
        }

        public void clear() {
            cache.clear();
        }
    }

    // Simulation result wrapper
    public static class SimulationResult {
        public final double x, y, z;
        public final double velX, velY, velZ;
        public final float yaw;
        public final boolean onGround;
        public final int ticksSimulated;
        public final boolean touchedGround;

        public SimulationResult(OptimizedSimInstance sim, int ticks, boolean touched) {
            this.x = sim.x;
            this.y = sim.y;
            this.z = sim.z;
            this.velX = sim.velX;
            this.velY = sim.velY;
            this.velZ = sim.velZ;
            this.yaw = sim.yaw;
            this.onGround = sim.onGround;
            this.ticksSimulated = ticks;
            this.touchedGround = touched;
        }

        public Vec3d getPosition() {
            return new Vec3d(x, y, z);
        }

        public Vec3d getVelocity() {
            return new Vec3d(velX, velY, velZ);
        }

        @Override
        public String toString() {
            return String.format("SimResult[pos=(%.2f, %.2f, %.2f), vel=(%.3f, %.3f, %.3f), ticks=%d, grounded=%b]",
                    x, y, z, velX, velY, velZ, ticksSimulated, touchedGround);
        }
    }
}