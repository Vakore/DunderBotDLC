package name.dunderbotdlc.commands;

import java.util.ArrayList;
import java.util.List;

import baritone.bb;
import baritone.api.utils.input.Input;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
//import net.minecraft.world.BlockView;
import net.minecraft.util.shape.VoxelShapes;
import name.dunderbotdlc.commands.AABB;

public class SimInstance {
  //physics
  public double physicsStepHeight = 0.6;
  
  public double physicsgravity = 0.08; // blocks/tick^2 https://minecraft.gamepedia.com/Entity#Motion_of_entities
  public double physicsairdrag=  0.98; // actually (1 - drag)
  public double physicsyawSpeed = 3.0;
  public double physicspitchSpeed= 3.0;
  public double physicsplayerSpeed= 0.1;
  public double physicssprintSpeed= 0.3;
  public double physicssneakSpeed= 0.3;
  public double physicsstepHeight= 0.6; // how much height can the bot step on without jump
  public double physicsnegligeableVelocity= 0.003; // actually 0.005 for 1.8, but seems fine
  public double physicssoulsandSpeed= 0.4;
  public double physicshoneyblockSpeed= 0.4;
  public double physicshoneyblockJumpSpeed= 0.4;
  public double physicsladderMaxSpeed= 0.15;
  public double physicsladderClimbSpeed= 0.2;
  public double physicsplayerHalfWidth= 0.3;
  public double physicsplayerHeight= 1.8;
  public double physicswaterInertia= 0.8;
  public double physicslavaInertia= 0.5;
  public double physicsliquidAcceleration= 0.02;
  public double physicsairborneInertia= 0.91;
  public double physicsairborneAcceleration= 0.02;
  public double physicsdefaultSlipperiness= 0.6;
  public double physicsoutOfLiquidImpulse= 0.3;
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
    public ArrayList<Boolean> myControls;
    public SimInstance(boolean grounded, ArrayList<Boolean> leControls, Vec3d ps, Vec3d pv, float ya) {
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
        if (leControls.get(0) == true) {controlsneak = true;}
        if (leControls.get(1) == true) {controljump = true;}
        if (leControls.get(2) == true) {controlsprint = true;}
        if (leControls.get(3) == true) {controlforward = true;}
        if (leControls.get(4) == true) {controlback = true;}
        if (leControls.get(5) == true) {controlleft = true;}
        if (leControls.get(6) == true) {controlright = true;}
        myControls = leControls;
    }

    public ArrayList<AABB> getSurroundingBBs (AABB queryBB) {
        ArrayList<AABB> surroundingBBs = new ArrayList<AABB>();
        vec3e cursor = new vec3e(0.0, 0.0, 0.0);
        for (cursor.y = Math.floor(queryBB.minY) - 2; cursor.y <= Math.floor(queryBB.maxY) + 1; cursor.y++) {
          for (cursor.z = Math.floor(queryBB.minZ) - 1; cursor.z <= Math.floor(queryBB.maxZ) + 1; cursor.z++) {
            for (cursor.x = Math.floor(queryBB.minX) - 1; cursor.x <= Math.floor(queryBB.maxX) + 1; cursor.x++) {
                BlockPos blockPos = new BlockPos((int)Math.floor(cursor.x), (int)Math.floor(cursor.y), (int)Math.floor(cursor.z));
                VoxelShape bst = world.getBlockState(blockPos).getCollisionShape(this.world, blockPos);
                //System.out.println(bst);
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

    public ArrayList<AABB> getWaterBBs (AABB queryBB) {
      ArrayList<AABB> surroundingBBs = new ArrayList<AABB>();
      vec3e cursor = new vec3e(0.0, 0.0, 0.0);
      for (cursor.y = Math.floor(queryBB.minY) - 1; cursor.y <= Math.floor(queryBB.maxY); cursor.y++) {
        for (cursor.z = Math.floor(queryBB.minZ); cursor.z <= Math.floor(queryBB.maxZ); cursor.z++) {
          for (cursor.x = Math.floor(queryBB.minX); cursor.x <= Math.floor(queryBB.maxX); cursor.x++) {
              BlockPos blockPos = new BlockPos((int)Math.floor(cursor.x), (int)Math.floor(cursor.y), (int)Math.floor(cursor.z));
              if (world.getBlockState(blockPos).getBlock().getName().getString().toLowerCase().contains("water")) {
                surroundingBBs.add(new AABB((int)Math.floor(cursor.x), (int)Math.floor(cursor.y), (int)Math.floor(cursor.z), blockPos.getX() + 1, blockPos.getY() + 0.778, blockPos.getZ() + 1));
              }
              //VoxelShape bst = world.getBlockState(blockPos).getCollisionShape(this.world, blockPos);
              //System.out.println(bst);
              /*if (bst != VoxelShapes.empty()) {
                  if (bst.getBoundingBoxes().size() > 0) {
                      for (int i = 0; i < bst.getBoundingBoxes().size(); i++) {
                          Box bstBox = bst.getBoundingBoxes().get(i);
                          surroundingBBs.add(new AABB((int)Math.floor(cursor.x) + bstBox.minX, (int)Math.floor(cursor.y) + bstBox.minY, (int)Math.floor(cursor.z) + bstBox.minZ, blockPos.getX() + bstBox.maxX, blockPos.getY() + bstBox.maxY, blockPos.getZ() + bstBox.maxZ));
                      }
                  }
              }*/
          }
        }
      }
      return surroundingBBs;
  }

    public AABB getPlayerBB(Vec3d pos) {
      double w = 0.3;
      return new AABB(-w, 0, -w, w, 1.8, w).offset(pos.x, pos.y, pos.z);
    }

    public vec3e setPositionToBB (AABB bb) {
      return new vec3e(bb.minX + this.physicsplayerHalfWidth, bb.minY, bb.minZ + this.physicsplayerHalfWidth);
     //pos.x = bb.minX + physics.playerHalfWidth;
     //pos.y = bb.minY;
     //pos.z = bb.minZ + physics.playerHalfWidth;
    }

    public void moveEntity (double dx, double dy, double dz) {
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
    
          // In the 3 loops bellow, y offset should be -1, but that doesnt reproduce vanilla behavior.
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
        ArrayList<AABB> surroundingBBs = getSurroundingBBs(queryBB);
        AABB oldBB = playerBB.clone();
    
        //double oldDy = dy;
        for (AABB blockBB : surroundingBBs) {
          dy = blockBB.computeOffsetY(playerBB, dy);
        }
        playerBB.offset(0.0, dy, 0.0);
    
        for (AABB blockBB : surroundingBBs) {
          dx = blockBB.computeOffsetX(playerBB, dx);
        }
        playerBB.offset(dx, 0.0, 0.0);
    
        for (AABB blockBB : surroundingBBs) {
          dz = blockBB.computeOffsetZ(playerBB, dz);
        }
        playerBB.offset(0.0, 0.0, dz);
    
        // Step on block if height < stepHeight
        if (this.physicsStepHeight > 0.0 &&
          (this.onGround || (dy != oldVelY && oldVelY < 0.0)) &&
          (dx != oldVelX || dz != oldVelZ)) {
            double oldVelXCol = dx;
          double oldVelYCol = dy;
          double oldVelZCol = dz;
          AABB oldBBCol = playerBB.clone();
    
          dy = this.physicsStepHeight;
          AABB queryBB2 = oldBB.clone().extend(oldVelX, dy, oldVelZ);
          ArrayList<AABB> surroundingBBs2 = getSurroundingBBs(queryBB);
    
          AABB BB1 = oldBB.clone();
          AABB BB2 = oldBB.clone();
          AABB BB_XZ = BB1.clone().extend(dx, 0.0, dz);
    
          double dy1 = dy;
          double dy2 = dy;
          for (AABB blockBB : surroundingBBs) {
            dy1 = blockBB.computeOffsetY(BB_XZ, dy1);
            dy2 = blockBB.computeOffsetY(BB2, dy2);
          }
          BB1.offset(0.0, dy1, 0.0);
          BB2.offset(0.0, dy2, 0.0);
    
          double dx1 = oldVelX;
          double dx2 = oldVelX;
          for (AABB blockBB : surroundingBBs) {
            dx1 = blockBB.computeOffsetX(BB1, dx1);
            dx2 = blockBB.computeOffsetX(BB2, dx2);
          }
          BB1.offset(dx1, 0.0, 0.0);
          BB2.offset(dx2, 0.0, 0.0);
    
          double dz1 = oldVelZ;
          double dz2 = oldVelZ;
          for (AABB blockBB : surroundingBBs) {
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
    
          for (AABB blockBB : surroundingBBs) {
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
        vec3e oldPos = new vec3e(this.x, this.y, this.z);
        vec3e newPos1 = setPositionToBB(playerBB);
        this.x = newPos1.x;
        this.y = newPos1.y;
        this.z = newPos1.z;
        this.isCollidedHorizontally = (dx != oldVelX || dz != oldVelZ);
        this.isCollidedVertically = (dy != oldVelY);
        this.onGround = (this.isCollidedVertically && oldVelY < 0);
    
        //const blockAtFeet = world.getBlock(new Vec3d(this.x, this.y, this.z).offset(0, -0.2, 0));
    
        if (dx != oldVelX) this.velX = 0.0;
        if (dz != oldVelZ) this.velZ = 0.0;
        if (dy != oldVelY) {
          if (false/*blockAtFeet && blockAtFeet.type === slimeBlockId && !this.controlsneak*/) {
            this.velY = -this.velY;
          } else {
            this.velY = 0.0;
          }
        }
    
        // Finally, apply block collisions (web, soulsand...)
        playerBB.contract(0.001, 0.001, 0.001);
        vec3e cursor = new vec3e(0.0, 0.0, 0.0);
        for (cursor.y = Math.floor(playerBB.minY); cursor.y <= Math.floor(playerBB.maxY); cursor.y++) {
          for (cursor.z = Math.floor(playerBB.minZ); cursor.z <= Math.floor(playerBB.maxZ); cursor.z++) {
            for (cursor.x = Math.floor(playerBB.minX); cursor.x <= Math.floor(playerBB.maxX); cursor.x++) {
              //const block = world.getBlock(cursor);
              BlockPos blockPos = new BlockPos((int)Math.floor(cursor.x), (int)Math.floor(cursor.y), (int)Math.floor(cursor.z));
              BlockState blockState = world.getBlockState(blockPos);
              //if (blockState != null) {
                //if (supportFeature('velocityBlocksOnCollision')) {
                  /*if (block.type === soulsandId) {
                    vel.x *= physics.soulsandSpeed
                    vel.z *= physics.soulsandSpeed
                  } else if (block.type === honeyblockId) {
                    vel.x *= physics.honeyblockSpeed
                    vel.z *= physics.honeyblockSpeed
                  }*/
                //}
                /*if (block.type === webId) {
                  entity.isInWeb = true
                } else if (block.type === bubblecolumnId) {
                  const down = !block.metadata
                  const aboveBlock = world.getBlock(cursor.offset(0, 1, 0))
                  const bubbleDrag = (aboveBlock && aboveBlock.type === 0) ? physics.bubbleColumnSurfaceDrag : physics.bubbleColumnDrag
                  if (down) {
                    vel.y = Math.max(bubbleDrag.maxDown, vel.y - bubbleDrag.down)
                  } else {
                    vel.y = Math.min(bubbleDrag.maxUp, vel.y + bubbleDrag.up)
                  }
                }
              }*/
            }
          }
        }
        /*if (supportFeature('velocityBlocksOnTop')) {
          const blockBelow = world.getBlock(entity.pos.floored().offset(0, -0.5, 0))
          if (blockBelow) {
            if (blockBelow.type === soulsandId) {
              vel.x *= physics.soulsandSpeed
              vel.z *= physics.soulsandSpeed
            } else if (blockBelow.type === honeyblockId) {
              vel.x *= physics.honeyblockSpeed
              vel.z *= physics.honeyblockSpeed
            }
          }
        }*/
      }

    public void applyHeading (double strafe, double forward, double multiplier) {
      double speed = Math.sqrt(strafe * strafe + forward * forward);
      if (speed < 0.01) return;
  
      speed = multiplier / Math.max(speed, 1);
  
      strafe *= speed;
      forward *= speed;
  
      double yaw = Math.PI - this.yaw;
      double sin = Math.sin(yaw);
      double cos = Math.cos(yaw);
  
      this.velX -= strafe * cos + forward * sin;
      this.velZ += forward * cos - strafe * sin;
    }






    public void moveEntityWithHeading (double strafe, double forward) {
    
  
      //double gravityMultiplier = (this.velY <= 0 && entity.slowFalling > 0) ? physics.slowFalling : 1;
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
  
        /*if (this.isCollidedHorizontally && doesNotCollide(world, new Vec3d(this.x + this.velX, this.y + this.velY + 0.6 - this.y + lastY, this.z + this.z))) {
          this.velY = physicsoutOfLiquidImpulse; // jump out of liquid
        }*/
      } else if (false/*entity.elytraFlying*/) {
        /*const {
          pitch,
          sinPitch,
          cosPitch,
          lookDir
        } = getLookingVector(entity)
        const horizontalSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z)
        const cosPitchSquared = cosPitch * cosPitch
        vel.y += physics.gravity * gravityMultiplier * (-1.0 + cosPitchSquared * 0.75)
        // cosPitch is in [0, 1], so cosPitch > 0.0 is just to protect against
        // divide by zero errors
        if (vel.y < 0.0 && cosPitch > 0.0) {
          const movingDownSpeedModifier = vel.y * (-0.1) * cosPitchSquared
          vel.x += lookDir.x * movingDownSpeedModifier / cosPitch
          vel.y += movingDownSpeedModifier
          vel.z += lookDir.z * movingDownSpeedModifier / cosPitch
        }
  
        if (pitch < 0.0 && cosPitch > 0.0) {
          const lookDownSpeedModifier = horizontalSpeed * (-sinPitch) * 0.04
          vel.x += -lookDir.x * lookDownSpeedModifier / cosPitch
          vel.y += lookDownSpeedModifier * 3.2
          vel.z += -lookDir.z * lookDownSpeedModifier / cosPitch
        }
  
        if (cosPitch > 0.0) {
          vel.x += (lookDir.x / cosPitch * horizontalSpeed - vel.x) * 0.1
          vel.z += (lookDir.z / cosPitch * horizontalSpeed - vel.z) * 0.1
        }
  
        vel.x *= 0.99
        vel.y *= 0.98
        vel.z *= 0.99
        moveEntity(entity, world, vel.x, vel.y, vel.z)
  
        if (entity.onGround) {
          entity.elytraFlying = false
        }*/
      } else {
        // Normal movement
        double acceleration = 0.0;
        double inertia = 0.0;
        //const blockUnder = world.getBlock(pos.offset(0, -1, 0));
        if (this.onGround /*&& blockUnder*/) {
          double playerSpeedAttribute;
          if (false) {
            // Use server-side player attributes
            playerSpeedAttribute = 0.7;//!!!
          } else {
            // Create an attribute if the player does not have it
            playerSpeedAttribute = physicsplayerSpeed;//!!!
          }
          // Client-side sprinting (don't rely on server-side sprinting)
          // setSprinting in LivingEntity.java
          
          if (this.controlsprint) {
              playerSpeedAttribute += playerSpeedAttribute * physicssprintSpeed;//!!!
          }
          // Calculate what the speed is (0.1 if no modification)
          double attributeSpeed = playerSpeedAttribute;
          inertia = (/*blockSlipperiness[blockUnder.type] ||*/ physicsdefaultSlipperiness) * 0.91;
          acceleration = attributeSpeed * (0.1627714 / (inertia * inertia * inertia));
          if (acceleration < 0.0) acceleration = 0.0; // acceleration should not be negative
        } else {
          acceleration = physicsairborneAcceleration;
          inertia = physicsairborneInertia;
  
          if (this.controlsprint) {
            double airSprintFactor = physicsairborneAcceleration * 0.3;
            acceleration += airSprintFactor;
          }
        }
  
        applyHeading(strafe, forward, acceleration);
  
        if (false/*isOnLadder(world, pos)*/) {
          this.velX = Math.clamp(-physicsladderMaxSpeed, this.velX, physicsladderMaxSpeed);
          this.velZ = Math.clamp(-physicsladderMaxSpeed, this.velZ, physicsladderMaxSpeed);
          this.velY = Math.max(this.velY, this.controlsneak ? 0 : -physicsladderMaxSpeed);
        }
  
        moveEntity(this.velX, this.velY, this.velZ);
  
        if (/*isOnLadder(world, pos)*/false && (this.isCollidedHorizontally ||
          (/*supportFeature('climbUsingJump') &&*/ this.controljump))) {
            this.velY = physicsladderClimbSpeed; // climb ladder
        }
  
        // Apply friction and gravity
        if (this.levitation > 0.0) {
          this.velY += (0.05 * this.levitation - velY) * 0.2;
        } else {
          this.velY -= physicsgravity * gravityMultiplier;
        }
        this.velY *= physicsairdrag;
        this.velX *= inertia;
        this.velZ *= inertia;
      }
    }








    public void advance() {
        this.x += Math.cos(Math.toRadians(this.yaw + 90)) * 0.2;
        //this.y -= Math.sin(Math.toRadians(this.pitch)) * 2;
        this.y += this.velY;
        this.z += Math.sin(Math.toRadians(this.yaw + 90)) * 0.2;

        
        BlockPos blockPos = new BlockPos((int)Math.floor(this.x), (int)Math.floor(this.y), (int)Math.floor(this.z));
        BlockState blockState = world.getBlockState(blockPos);
        //BlockView leView = this.world;
        
        this.velY -= 0.08;
        VoxelShape bst = blockState.getCollisionShape(this.world, blockPos);
        //System.out.println(bst);
        if (bst != VoxelShapes.empty()) {
            if (bst.getBoundingBoxes().size() > 0) {
                List<AABB> surroundingBBs = new ArrayList<AABB>();
                for (int i = 0; i < bst.getBoundingBoxes().size(); i++) {
                    Box bstBox = bst.getBoundingBoxes().get(i);
                    surroundingBBs.add(new AABB(blockPos.getX() + bstBox.minX, blockPos.getY() + bstBox.minY, blockPos.getZ() + bstBox.minZ, blockPos.getX() + bstBox.maxX, blockPos.getY() + bstBox.maxY, blockPos.getZ() + bstBox.maxZ));
                    if (surroundingBBs.get(i).intersects(new AABB(this.x - 0.3, this.y, this.z - 0.3, this.x + 0.3, this.y + 1.8, this.z + 0.3))) {
                        this.y = blockPos.getY() + bstBox.maxY;
                        this.velY = 0.42;
                    }
                }
            }
        }

        this.velY *= (1.0 - 0.02);
    }










boolean isInWaterApplyCurrent (ClientWorld world, AABB bb, Vec3d vel) {
    Vec3d acceleration = new Vec3d(0, 0, 0);
    //boolean waterBlocks = getWaterInBB(world, bb);
    boolean isInWater = false;//waterBlocks.length > 0;
    /*for (const block of waterBlocks) {
      const flow = getFlow(world, block)
      acceleration.add(flow)
    }

    const len = acceleration.norm()
    if (len > 0) {
      vel.x += acceleration.x / len * 0.014
      vel.y += acceleration.y / len * 0.014
      vel.z += acceleration.z / len * 0.014
    }*/
      return isInWater;
    }



    public void simulatePlayer() {
  
      AABB waterBB = getPlayerBB(new Vec3d(this.x, this.y, this.z)).contract(0.001, 0.401, 0.001);
      AABB lavaBB = getPlayerBB(new Vec3d(this.x, this.y, this.z)).contract(0.1, 0.4, 0.1);
  
      this.isInWater = isInWaterApplyCurrent(world, waterBB, new Vec3d(this.velX, this.velY, this.velZ));
      this.isInLava = false;//isMaterialInBB(world, lavaBB, lavaIds);
  
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
          //const blockBelow = world.getBlock(entity.pos.floored().offset(0, -0.5, 0))
          this.velY = 0.42 * 1.0;//((blockBelow && blockBelow.type === honeyblockId) ? physics.honeyblockJumpSpeed : 1)
          if (this.jumpBoost > 0.0) {
            this.velY += 0.1 * this.jumpBoost;
          }
          if (this.controlsprint) {
            double yaw = Math.PI - this.yaw;
            this.velX -= Math.sin(yaw) * 0.2;
            this.velZ += Math.cos(yaw) * 0.2;
          }
          this.jumpTicks = 0;//physicsautojumpCooldown;
        }
      } else {
        this.jumpTicks = 0; // reset autojump cooldown
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
  
      this.elytraFlying = false;//entity.elytraFlying && entity.elytraEquipped && !entity.onGround && !entity.levitation
  
      /*if (entity.fireworkRocketDuration > 0) {
        if (!entity.elytraFlying) {
          entity.fireworkRocketDuration = 0
        } else {
          const { lookDir } = getLookingVector(entity)
          vel.x += lookDir.x * 0.1 + (lookDir.x * 1.5 - vel.x) * 0.5
          vel.y += lookDir.y * 0.1 + (lookDir.y * 1.5 - vel.y) * 0.5
          vel.z += lookDir.z * 0.1 + (lookDir.z * 1.5 - vel.z) * 0.5
          --entity.fireworkRocketDuration
        }
      }*/
  
      this.moveEntityWithHeading(strafe, forward);
    }

    public SimInstance clone() {
      SimInstance newInstance = new SimInstance(
        this.onGround,
        this.myControls,
        new Vec3d(this.x, this.y, this.z),
        new Vec3d(this.velX, this.velY, this.velZ),
        this.yaw 
      );
      return newInstance;
    }
}
