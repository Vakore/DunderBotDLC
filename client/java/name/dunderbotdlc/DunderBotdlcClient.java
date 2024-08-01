/*
 * TODO:
 * target.getDataTracker().getChangedEntries().size() <- can cause errors if
 * getChangedEntries is empty
 * DONT ALLOW the bot taking control is baritone is
 * making the bot jump, or take baritone's control
 * over bot jumping away
 */

package name.dunderbotdlc;

//import name.dunderbotdlc.commands.;

import net.fabricmc.api.ClientModInitializer;
import name.dunderbotdlc.commands.IBaritoneAPIMixin;
//import name.dunderbotdlc.commands.AABB;
import name.dunderbotdlc.commands.NewCommand;
import name.dunderbotdlc.commands.SimInstance;
import name.dunderbotdlc.commands.SmartWalk;
import name.dunderbotdlc.commands.jumpSprintState;
//import name.dunderbotdlc.mixin.client.BaritoneAPIMixin;
//import name.dunderbotdlc.mixin.client.PathingBehaviorMixin;

import java.io.PrintStream;
import java.lang.reflect.Field;
//import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
//import java.util.UUID;
import java.util.Map;

//import com.mojang.brigadier.context.CommandContext;

//import org.spongepowered.include.com.google.common.collect.Lists;

//import com.mojang.authlib.GameProfile;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.behavior.IPathingBehavior;
//import baritone.api.event.events.TickEvent;
//import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.input.Input;
//import baritone.api.utils.input.Input;
//import baritone.api.process.PathingCommand;
//import baritone.api.process.PathingCommandType;
//import baritone.api.utils.BetterBlockPos;
//import baritone.api.utils.Rotation;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
//import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
//import net.minecraft.entity.mob.CreeperEntity;
//import net.minecraft.entity.mob.SkeletonEntity;
//import net.minecraft.entity.EntityType;
//import net.minecraft.entity.EntityType;
//import net.minecraft.entity.LightningEntity;
//import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireballEntity;
//import net.minecraft.util.math.BlockPos;
import net.minecraft.particle.ParticleTypes;
//import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
/*import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;*/
import net.minecraft.client.option.KeyBinding;
//import name.dunderbotdlc.commands.Prediction;
//import net.minecraft.client.util.InputUtil;
//import name.dunderbotdlc.mixin.client.IBaritoneAPI;



public class DunderBotdlcClient implements ClientModInitializer {
    public static int bestPathNum = 0;

    private boolean isBotting = false;
    private IBaritone bbaritone;
    //private PathingCommand pauseCommand;
    private int attackCooldown = 0;
    private ClientWorld world;
    private Entity target;
    private int targetID;
    private double threatLevel;
    private Vec3d playerPos;
    private PlayerEntity player;
    public static MinecraftClient client;

    private boolean c_spr = false;
    private boolean c_W = false;
    private boolean c_S = false;
    private boolean c_A = false;
    private boolean c_D = false;
    private boolean c_Z = false;
    private boolean c_j = false;
    private boolean c_lc = false;
    private boolean c_rc = false;
    private int shieldTimer = 0;
    private int botMode = 1;
    private Map<String, Integer> mobMap = new HashMap<String, Integer>();
    private Map<String, Vec3d> projectileMap = new HashMap<String, Vec3d>();
    private ArrayList<Entity> threatList = new ArrayList<Entity>();
    private ArrayList<Double> threatTable = new ArrayList<Double>();
    private ArrayList<Double> threatDist = new ArrayList<Double>();

    private ArrayList<jumpSprintState> jumpSprintStates = new ArrayList<jumpSprintState>();
    private jumpSprintState leBest;
    //private ArrayList<Vec3d> jumpTargets = new ArrayList<Vec3d>();
    //private Vec3d jumpTarget;
    //private float jumpYaw;
    //private int bestJumpSprintState;
    private int noJumpAttempts = 0;
	//private Rotation rotation;
    private PrintStream myStream;

	@Override
	public void onInitializeClient() {
        //This is for testing, so that when
        //I want to print a bijillion things
        //in the console every tick while debugging
        //it won't save a 200kb file each time I need
        //to update my code
        myStream = new PrintStream(System.out) {
            @Override
            public void println(String x) {
                super.println(x);
            }
        };
        System.setOut(myStream);

		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		System.out.println("DunderLC - Hello Fabric world!");
		NewCommand newCommand = new NewCommand (BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().getBaritone()); BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().getRegistry().register(newCommand);
		// Initialize Baritone
		bbaritone = BaritoneAPI.getProvider().getPrimaryBaritone();

        //pauseCommand = new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
        System.out.println(bbaritone.toString());

		// Register the client tick event
        //ClientTickEvents.START_CLIENT_TICK.register(client -> onClientTick2());
        ClientTickEvents.END_CLIENT_TICK.register(client -> onClientTick());
	}

    //@SuppressWarnings("rawtypes")
	private void onClientTick() {
        
        if (isBotting && client.options.togglePerspectiveKey.isPressed()) {
            //((IBaritoneAPIMixin) BaritoneAPI.getProvider()).setSoftPause(false);
            //BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
        } else if (isBotting) {
            ((IBaritoneAPIMixin) BaritoneAPI.getProvider()).setSoftPause(true);
        }
        
        if (attackCooldown < 200) {
            attackCooldown++;
        }
        if (shieldTimer > 0) {
            shieldTimer--;
        }
        if (noJumpAttempts > 0) {
            noJumpAttempts--;
        }
		// Get the current client instance
        client = MinecraftClient.getInstance();
        c_S = false;
        c_spr = false;
        c_W = false;
        c_A = false;
        c_D = false;
        c_Z = false;
        c_j = false;
        c_rc = false;
        c_lc = false;
        
        // Check if the client is in a world
        if (client.world != null && client.player != null) {
            // Get the client world
            world = client.world;

			// Get the player
            player = client.player;
            playerPos = player.getPos();
            target = null;
            targetID = -1;
            threatLevel = 0.0;

            if (client.options.swapHandsKey.isPressed()) {
                SmartWalk.thinkJump(client.world, playerPos, (double)player.getYaw());
                client.options.swapHandsKey.setPressed(false);
            }

            if (client.options.dropKey.isPressed() && attackCooldown > 10) {
                attackCooldown = 0;
                isBotting = !isBotting;//
                botMode = client.options.hotbarKeys[0].isPressed() ? 1 : 0;
                BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().clearAllKeys();
                ((IBaritoneAPIMixin) BaritoneAPI.getProvider()).setSoftPause(isBotting);
                if (isBotting) {
                    
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
                }
                /*if (isBotting) {
                    if (botMode == 0) {
                        botMode = 1;
                    } else {
                        botMode = 0;
                    }
                }*/
                System.out.println("DunderLC - isBotting is now set to " + isBotting);
                System.out.println(world.getBlockState(player.getBlockPos()));
                c_W = false;
                c_spr = false;
                c_S = false;
                c_A = false;
                c_D = false;
                c_Z = false;
                c_j = false;
                c_lc = false;
                c_rc = false;
                releaseKey(client.player, client.options.useKey);//
                releaseKey(client.player, client.options.attackKey);//
                releaseKey(client.player, client.options.forwardKey);//
                releaseKey(client.player, client.options.backKey);//
                releaseKey(client.player, client.options.leftKey);//
                releaseKey(client.player, client.options.rightKey);//
                releaseKey(client.player, client.options.sneakKey);//
                releaseKey(client.player, client.options.jumpKey);//
                releaseKey(client.player, client.options.sprintKey);//

                System.out.println(client.player.getYaw() + ", " + client.player.getPitch());
            }

            


            
            if (isBotting) {
                //PvE code.
                if (botMode == 0) {
                    doPvE();
                } else if (botMode == 1) {
                    doJumpsprint(0);
                }


                if (c_A) {
                    client.player.input.movementSideways = 1.0f;
                    System.out.println("A");
                    pressKey(client.player, client.options.leftKey, false);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_LEFT, true);
                } else {
                    releaseKey(client.player, client.options.leftKey);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_LEFT, false);
                }
                if (c_D) {
                    client.player.input.movementSideways = -1.0f;
                    System.out.println("D");
                    pressKey(client.player, client.options.rightKey, false);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_RIGHT, true);
                } else {
                    releaseKey(client.player, client.options.rightKey);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_RIGHT, false);
                }

                if (c_spr) {
                    client.player.setSprinting(true);
                } else {
                    client.player.setSprinting(true);
                }

                if (c_W) {
                    client.player.input.movementForward = 1.0f;
                    pressKey(client.player, client.options.forwardKey, false);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, true);
                } else {
                    releaseKey(client.player, client.options.forwardKey);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_FORWARD, false);
                }
            
                if (c_S) {
                    client.player.input.movementForward = -1.0f;
                    pressKey(client.player, client.options.backKey, false);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_BACK, true);
                } else {
                    releaseKey(client.player, client.options.backKey);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().setInputForceState(Input.MOVE_BACK, false);
                }
            
                if (c_Z) {
                    pressKey(client.player, client.options.sneakKey, false);
                } else {
                    releaseKey(client.player, client.options.sneakKey);
                }
                
                if (c_j) {
                    pressKey(client.player, client.options.jumpKey, false);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().setInputForceState(Input.JUMP, true);
                    client.player.input.jumping = true;
                    //client.player.sprint
                    /*if (client.player.isOnGround()) {
                        client.player.setJumping(true);
                        client.player.jump();
                    }*/
                    //client.player.setJumping(true);
                } else {
                    releaseKey(client.player, client.options.jumpKey);
                    BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().setInputForceState(Input.JUMP, false);
                    client.player.input.jumping = false;
                }
                
                if (c_rc) {
                    pressKey(client.player, client.options.useKey, false);
                } else {
                    releaseKey(client.player, client.options.useKey);
                }

                
                if (c_lc) {
                    pressKey(client.player, client.options.attackKey, false);
                } else {
                    releaseKey(client.player, client.options.attackKey);
                }
            }
            //wantedYaw = player.getYaw();
        }
    }

	private void lookAtEntity(PlayerEntity player, Entity entity) {
        Vec3d playerPos = player.getPos().add(0, 1.6, 0);
        float yAt = -1.0f;//(float)entity.getEyeHeight(null);
        if (entity.getY() > player.getPos().y + 1.6) {
            yAt = 0.1f;
        } else if (entity.getY() + entity.getHeight() < player.getPos().y + 1.6) {
            yAt = (float)entity.getHeight() - 0.1f;
        }
        Vec3d entityPos = entity.getPos().add(0, yAt, 0);
        
        // Calculate direction vector
        Vec3d direction = entityPos.subtract(playerPos).normalize();

        // Calculate pitch and yaw to look at the entity
        double pitch = Math.asin(-direction.y);
        double yaw = Math.atan2(direction.z, direction.x) - Math.PI / 2;

        // Convert to degrees
        pitch = Math.toDegrees(pitch);
        yaw = Math.toDegrees(yaw);
        if (yAt == -1.0f) {
            pitch = 0.0f;
        }

        player.setYaw((float)yaw);
        player.setPitch((float)pitch);

		//rotation.subtract(getYaw())
        // Apply rotation
        //baritone.getLookBehavior().updateTarget(rotation, true);
    }
    
    private void releaseKey(ClientPlayerEntity player, KeyBinding keyBinding) {
        if (keyBinding.isPressed()) {
            keyBinding.setPressed(false);
        }
    }

    private void pressKey(ClientPlayerEntity player, KeyBinding keyBinding, boolean spam) {
        if (spam) {
            //System.out.println(keyBinding.isPressed());
        }

        if (!keyBinding.isPressed()) {
            keyBinding.setPressed(true);
        } else if (spam) {
            keyBinding.setPressed(false);
        }
    }

    /*private boolean isColliding(Entity entity) {
        AABB queryBB = new AABB(entity.getBoundingBox().minX, 
                entity.getBoundingBox().minY, 
                entity.getBoundingBox().minZ, 
                entity.getBoundingBox().maxX, 
                entity.getBoundingBox().maxY, 
                entity.getBoundingBox().maxZ);

        System.out.println("queeryBB minX: ");
        System.out.println(queryBB.minX);
        vec3e cursor = new vec3e(0.0, 0.0, 0.0);
        for (cursor.y = Math.floor(queryBB.minY) - 1; cursor.y <= Math.floor(queryBB.maxY); cursor.y++) {
          for (cursor.z = Math.floor(queryBB.minZ); cursor.z <= Math.floor(queryBB.maxZ); cursor.z++) {
            for (cursor.x = Math.floor(queryBB.minX); cursor.x <= Math.floor(queryBB.maxX); cursor.x++) {
                BlockPos blockPos = new BlockPos((int)Math.floor(cursor.x), (int)Math.floor(cursor.y), (int)Math.floor(cursor.z));
                VoxelShape bst = world.getBlockState(blockPos).getCollisionShape(world, blockPos);
                //System.out.println(bst);
                if (bst != VoxelShapes.empty()) {
                    if (bst.getBoundingBoxes().size() > 0) {
                        for (int i = 0; i < bst.getBoundingBoxes().size(); i++) {
                            Box bstBox = bst.getBoundingBoxes().get(i);
                            if (queryBB.maxX + 0.05 >= cursor.x + bstBox.minX &&
                                queryBB.minX - 0.05 <= cursor.x + bstBox.maxX &&
                                queryBB.maxY + 0.05 >= cursor.y + bstBox.minY &&
                                queryBB.minY - 0.05 <= cursor.y + bstBox.maxY &&
                                queryBB.maxZ + 0.05 >= cursor.z + bstBox.minZ &&
                                queryBB.minZ - 0.05 <= cursor.z + bstBox.maxZ) {
                                System.out.println("colliding!");
                                return true;
                            }
                            //surroundingBBs.add(new AABB((int)Math.floor(cursor.x) + bstBox.minX, (int)Math.floor(cursor.y) + bstBox.minY, (int)Math.floor(cursor.z) + bstBox.minZ, blockPos.getX() + bstBox.maxX, blockPos.getY() + bstBox.maxY, blockPos.getZ() + bstBox.maxZ));
                        }
                    }
                }
            }
          }
        }
        return false;
    }*/

    private void doJumpsprint(int depth) {
      if (noJumpAttempts <= 0 && player.isOnGround()) {
        System.out.println("oh yeah");
        int bestPos = -1;
        try {
            List<BetterBlockPos> bPos = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions();
            bestPos = Math.min(BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getCurrent().getPosition() - 1, 0);
            for (int i = bestPos; i < bPos.size(); i++) {
                if (player.getPos().distanceTo( new Vec3d(bPos.get(i).x, bPos.get(i).y, bPos.get(i).z)) <
                    player.getPos().distanceTo( new Vec3d(bPos.get(bestPos).x, bPos.get(bestPos).y, bPos.get(bestPos).z))) {
                    bestPos = i;
                }
            }
            bestPathNum = bestPos - BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getCurrent().getPosition() + 1;

            //BaritoneAPI.getProvider().getPrimaryBaritone
            //BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getNext().getPath().positions();
            
            //setCurrentPathPosition(bestPos);
        } catch (Exception e) {
            System.out.println("Baked:\n" + e);
        }

        ArrayList<Boolean> playerControlList = new ArrayList<Boolean>();
        for (int i = 0; i < 8; i++) {playerControlList.add(i == 1 || i == 2 || i == 3);}
        SimInstance myStateBase = new SimInstance(player.isOnGround(), playerControlList, client.player.getPos(), client.player.getVelocity(), (float)(Math.PI + (-client.player.getYaw() * Math.PI / 180.0)));

        jumpSprintStates.clear();
        leBest = null;
        //Simulate jump sprints
        /*myStateBase.yaw = (float)(myStateBase.yaw * (float)(180/(float)Math.PI));
        myStateBase.yaw = (float)Math.round(myStateBase.yaw / 10)*10;
        myStateBase.yaw = myStateBase.yaw * (float)Math.PI/180;*/
        for (int j = 0; j < 7; j++) {
            SimInstance myState = myStateBase.clone();
            myState.yaw = (float)(myStateBase.yaw - (Math.PI / 2) + (Math.PI / 8) + ((Math.PI / 8) * new int[]{3,4,2,5,1,7,0}[j]));
            jumpSprintState pushDis = simulateAction(depth, bestPos, 0, new Vec3d(0, 0, 0), myState);
            if (pushDis != null) {
                jumpSprintStates.add(pushDis);
            }
        }
        for (int j = 0; j < 5; j++) {
            SimInstance myState = myStateBase.clone();
            myState.controljump = false;
            myState.myControls.set(1, false);
            myState.yaw = (float)(myStateBase.yaw - (Math.PI / 2) + (Math.PI / 8) + ((Math.PI / 8) * new int[]{3,4,2/* ,5,1*/,7,0}[j]));
            jumpSprintState pushDis = simulateAction(depth, bestPos, 1, new Vec3d(0, 0, 0), myState);
            if (pushDis != null) {
                jumpSprintStates.add(pushDis);
            }
        }
        /*for (int j = 0; j < 1; j++) {
          SimInstance myState = myStateBase.clone();
          myState.controlleft = true;
          myState.myControls.set(5, true);
          myState.yaw = (float)(myStateBase.yaw - (Math.PI / 2) + (Math.PI / 8) + ((Math.PI / 8) * new int[]{3,4,2,5,1,7,0}[j]));
          jumpSprintState pushDis = simulateAction(1, bestPos, 0, new Vec3d(0, 0, 0), myState);
          if (pushDis != null) {
              jumpSprintStates.add(pushDis);
          }
        }
        for (int j = 0; j < 1; j++) {
          SimInstance myState = myStateBase.clone();
          myState.controlright = true;
          myState.myControls.set(6, true);
          myState.yaw = (float)(myStateBase.yaw - (Math.PI / 2) + (Math.PI / 8) + ((Math.PI / 8) * new int[]{3,4,2,5,1,7,0}[j]));
          jumpSprintState pushDis = simulateAction(1, bestPos, 0, new Vec3d(0, 0, 0), myState);
          if (pushDis != null) {
              jumpSprintStates.add(pushDis);
          }
        }*/
        
        /*for (int j = 0; j < 7; j++) {
          SimInstance myState = myStateBase.clone();
          myState.controlsprint = false;
          myState.myControls.set(2, false);
          myState.yaw = (float)(myStateBase.yaw - (Math.PI / 2) + (Math.PI / 8) + ((Math.PI / 8) * new int[]{3,4,2,5,1,7,0}[j]));
          jumpSprintState pushDis = simulateAction(bestPos, 0, new Vec3d(0, 0, 0), myState);
          if (pushDis != null) {
              jumpSprintStates.add(pushDis);
          }
        }*/
        if (jumpSprintStates.size() > 0) {
            int myBestState = 0;
            for (int i = 0; i < jumpSprintStates.size(); i++) {
                if (jumpSprintStates.get(i).open == true && jumpSprintStates.get(i).score < jumpSprintStates.get(myBestState).score) {
                    myBestState = i;
                }
            }

            if (myBestState >= 0) {
                leBest = jumpSprintStates.get(myBestState);
                if (!leBest.shouldJump) {
                    noJumpAttempts = 2;
                }
                //player.setYaw((float)-((leBest.state.yaw - Math.PI) * 180.0f / Math.PI));
            }
            /*if (searchCount <= 0) {
                //console.log("decent jumps found");
                jumpSprintState mySearcher = jumpSprintStates.get(myBestState);
                *while (mySearcher.parent) {
                    bot.dunder.jumpTargets.push(mySearcher.state.pos);
                    mySearcher = mySearcher.parent;
                }*
                jumpTargets.add(mySearcher.state.pos);
                jumpTarget = mySearcher.state.pos;
                jumpYaw = mySearcher.state.yaw;
                bestJumpSprintState = myBestState;
                if (mySearcher.state.isInLava) {System.out.println("fire");}
                if (mySearcher.score > -131) {
                    jumpTargetDelay = 15;
                }
            }*/
          }
      }

      //c_W = true;
      if (leBest != null) {
        player.setYaw((float)-((leBest.state.yaw - Math.PI) * 180.0f / Math.PI));
        c_W = leBest.state.controlforward;
        c_A = leBest.state.controlleft;
        c_D = leBest.state.controlright;
        c_spr = leBest.state.controlsprint;
        c_j = (player.getVelocity().y <= 0) && leBest.state.controljump;
        //System.out.println(leBest.state.controlleft + ", " + leBest.state.controlright);
      }

        //simulateAction(0, new Vec3d(0, player.getY(), 0), myStateBase);
    }


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
    
    private void doPvE() {
        threatList.clear();
        threatTable.clear();
        threatDist.clear();
        for (Entity entity : world.getEntities()) {
            double distance = playerPos.distanceTo(entity.getPos());
            double threatLvl = 1/Math.max(0.5, distance);
            if (entity instanceof ArrowEntity) {
                //System.out.println(entity.getVelocity().length());
                if (projectileMap.get(entity.getUuidAsString()) != null &&
                    projectileMap.get(entity.getUuidAsString()).subtract(entity.getPos()).equals(Vec3d.ZERO) ||
                    entity.getPos().distanceTo(playerPos) < entity.getPos().add(entity.getVelocity()).distanceTo(playerPos) ||
                    entity.getVelocity().length() < 0.7) {
                    mobMap.put(entity.getUuidAsString(), 1);
                } else if (projectileMap.get(entity.getUuidAsString()) != null) {
                    mobMap.remove(entity.getUuidAsString());
                    threatLvl *= 5;
                    threatLvl += 10;
                } else {
                    threatLvl *= 5;
                    threatLvl += 10;
                }
                projectileMap.put(entity.getUuidAsString(), new Vec3d(entity.getX(), entity.getY(), entity.getZ()));
            } else if (entity.getClass().getSimpleName().matches("SkeletonEntity|IllusionerEntity|StrayEntity")) {
                threatLvl /= 3.0;
                boolean foundId = false;
                for (int i = 0; i < entity.getDataTracker().getChangedEntries().size(); i++) {
                    if (entity.getDataTracker().getChangedEntries().get(i).id() == 8) {
                        mobMap.put(entity.getUuidAsString(),
                        (mobMap.get(entity.getUuidAsString()) != null) ? (mobMap.get(entity.getUuidAsString()) + 1) : 1);
                        foundId = true;
                    }
                }
                if (!foundId) {
                    mobMap.put(entity.getUuidAsString(), 1);
                }
                if (mobMap.get(entity.getUuidAsString()) != null && mobMap.get(entity.getUuidAsString()) >= 3) {
                    threatLvl += mobMap.get(entity.getUuidAsString()) / 15;
                }
            }
            if (distance <= 16 && distance > 0) {
                if (!(entity instanceof ArrowEntity)) {
                    System.out.println(entity.getClass().getSimpleName() + ", " + threatLvl);
                }
                //System.out.println(entity instanceof net.minecraft.entity.projectile.ArrowEntity);
                if ((entity.isAttackable() && entity.isAlive() && !(entity instanceof FireballEntity) ||
                    entity instanceof ArrowEntity &&
                    mobMap.get(entity.getUuidAsString()) == null)) {
                    threatList.add(entity);
                    threatDist.add(distance);
                    threatTable.add(threatLvl);
                    if ((target == null || threatLvl > threatLevel/*distance < playerPos.distanceTo(target.getPos()))*/)) {
                        target = entity;
                        targetID = threatList.size() - 1;
                        threatLevel = threatLvl;
                    }
                }
            }
        }


        // && BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getNext().getPath().movements().getLast().safeToCancel()
        if (target != null && isBotting) {//(!!!)
            //BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("pause");
            //simulateAction();
            //BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            //baritone.getPathingBehavior().execute(pauseCommand);
            //System.out.println("Entity: " + entity.getType().getName().getString() + " at " + entity.getPos() + " is within 3 blocks of the player");
            lookAtEntity(player, target);
            double distance = playerPos.distanceTo(target.getPos());
            //System.out.println(target.getVelocity());
            //System.out.println(target.groundCollision);
            //System.out.println(target.get);
            double threatDistance = 3.0;
            if (target.getClass().getSimpleName().matches("SkeletonEntity|IllusionerEntity|StrayEntity") &&
                true/*target.getHandItems().forEach()*/) {
                threatDistance = 0.2;
            }


            int bestTarget = -1;
            for (int i = 0; i < threatDist.size(); i++) {
                if (threatList.get(i) instanceof ArrowEntity) {
                    continue;
                }

                if (bestTarget == -1 ||
                    playerPos.distanceTo(threatList.get(i).getPos()) <
                    playerPos.distanceTo(threatList.get(bestTarget).getPos())) {
                    bestTarget = i;
                }
            }
            
            if (bestTarget - targetID == 0) {
                if (distance > threatDistance) {
                    ArrayList<Boolean> playerControlList = new ArrayList<Boolean>();
                    for (int i = 0; i < 16; i++) {playerControlList.add(i == 3);}
                    playerControlList = SmartWalk.moveInDir(new SimInstance(client.player.isOnGround(), playerControlList, client.player.getPos(), client.player.getVelocity(), (float)(Math.PI + (-client.player.getYaw() * Math.PI / 180.0))));
                    c_W = playerControlList.get(3);
                    c_S = false;
                    c_Z = playerControlList.get(0);
                    c_j = playerControlList.get(1);
                    c_spr = c_W;
                } else {
                    ArrayList<Boolean> playerControlList = new ArrayList<Boolean>();
                    for (int i = 0; i < 8; i++) {playerControlList.add(i == 4);}
                    playerControlList = SmartWalk.moveInDir(new SimInstance(client.player.isOnGround(), playerControlList, client.player.getPos(), client.player.getVelocity(), (float)(Math.PI + (-client.player.getYaw() * Math.PI / 180.0))));
                    c_W = false;
                    c_spr = false;
                    c_S = playerControlList.get(4);
                    c_Z = playerControlList.get(0);
                    c_j = playerControlList.get(1);
                }
            } else {
                boolean isSkele = threatList.get(bestTarget).getClass().getSimpleName().matches("SkeletonEntity|IllusionerEntity|StrayEntity");
                ArrayList<Boolean> playerControlList = new ArrayList<Boolean>();
                for (int i = 0; i < 8; i++) {playerControlList.add(false);}
                
                if (playerPos.distanceTo(threatList.get(bestTarget).getPos()) >
                    playerPos.add(Math.cos(Math.toRadians(player.getYaw() + 90)) * 0.2, 0, Math.sin(Math.toRadians(player.getYaw() + 90)) * 0.2).distanceTo(threatList.get(bestTarget).getPos())) {
                        playerControlList.set(3, isSkele);
                        playerControlList.set(4, !isSkele);
                } else {
                    playerControlList.set(4, isSkele);
                    playerControlList.set(3, !isSkele);
                }
                
                if (playerPos.distanceTo(threatList.get(bestTarget).getPos()) >
                    playerPos.add(Math.cos(Math.toRadians(player.getYaw())) * 0.2, 0, Math.sin(Math.toRadians(player.getYaw())) * 0.2).distanceTo(threatList.get(bestTarget).getPos())) {
                    playerControlList.set(5, isSkele);
                    playerControlList.set(6, !isSkele);
                } else {
                    playerControlList.set(6, isSkele);
                    playerControlList.set(5, !isSkele);
                }

                playerControlList = SmartWalk.moveInDir(new SimInstance(client.player.isOnGround(), playerControlList, client.player.getPos(), client.player.getVelocity(), (float)(Math.PI + (-client.player.getYaw() * Math.PI / 180.0))));
                c_W = playerControlList.get(3);
                c_S = playerControlList.get(4);
                c_A = playerControlList.get(5);
                c_D = playerControlList.get(6);
                c_Z = playerControlList.get(0);
                c_j = playerControlList.get(1);
                c_spr = playerControlList.get(2);
            }

            
                if (target.getClass().getSimpleName().matches("SkeletonEntity|IllusionerEntity|StrayEntity") &&
                    mobMap.get(target.getUuidAsString()) != null &&
                    mobMap.get(target.getUuidAsString()) >= 11) {
                    shieldTimer = 2;
                } else if (target.getClass().getSimpleName().matches("BlazeEntity")) {
                    for (int i = 0; i < target.getDataTracker().getChangedEntries().size(); i++) {
                        if (target.getDataTracker().getChangedEntries().get(i).id() == 16) {
                            shieldTimer = 2;
                        }
                    }
                } else if (target.getClass().getSimpleName().matches("CreeperEntity")) {
                    for (int i = 0; i < target.getDataTracker().getChangedEntries().size(); i++) {
                        if (target.getDataTracker().getChangedEntries().get(i).id() == 16) {
                            shieldTimer = 2;
                        }
                    }
                } else if (target instanceof ArrowEntity) {
                    shieldTimer = 2;
                }
                //System.out.println(client.player.getOffHandStack().getRegistryEntry());
            if (target.isAttackable() && shieldTimer <= 0 && !client.options.attackKey.isPressed() && attackCooldown >= 12 && distance <= 3.0 && target.isAttackable() && target.isAlive()) {
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(client.player.preferredHand);
                attackCooldown = 0;
                c_lc = true;
            } else {
                c_lc = false;
                releaseKey(client.player, client.options.attackKey);
            }
            if (shieldTimer > 0) {
                c_rc = true;
            }
            pressKey(client.player, client.options.sprintKey, true);
        } else if (isBotting) {
            //BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("resume");
            //simulateAction();
        }
    }
































public double dist3d(double x1, double y1, double z1, double x2, double y2, double z2) {
    return Math.sqrt((x2 - x1)*(x2 - x1) + (y2 - y1)*(y2 - y1) + (z2 - z1)*(z2 - z1));
};

public void setCurrentPathPosition(int newIndex) {
    if (client != null && client.player != null) {
        IPathingBehavior pathingProcess = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior();
        //if (pathingProcess.isPathing()) {
            // Get the current path executor
            IPathExecutor pathExecutor = (IPathExecutor) pathingProcess.getCurrent();

            if (pathExecutor != null) {
                try {
                    // Use reflection to access the private field
                    Field[] fields = IPathExecutor.class.getDeclaredFields();
                        System.out.println("Fields in PathExecutor:");
                        for (Field field : fields) {
                            System.out.println(field.getName());
                        }

                    Field pathPositionField = IPathExecutor.class.getDeclaredField("pathPosition");
                    pathPositionField.setAccessible(true);

                    // Set the current position in the path to the new index
                    pathPositionField.setInt(pathExecutor, newIndex);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    System.out.println("Cooked: \n" + e);
                    e.printStackTrace(myStream);
                }
            }
        //}
    }
}

    /*private int extendPath(int x, int z) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(new GoalXZ(x, z));
        return 1;
    }*/

public jumpSprintState simulateAction(int depth, int index, int action, Vec3d target, SimInstance stateBase) {
    double walker = 0.5 - (0.25 * action);
    BetterBlockPos myBlock;
    List<BetterBlockPos> bPos = new ArrayList<BetterBlockPos>();
    int currentMove = 0;
    if (index >= 0) {
        currentMove = index;
    }

    try {
      if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior() != null &&
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath() != null &&
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get() != null &&
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions() != null &&
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions().getLast() != null) {
            bPos = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions();
            /*BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getCurrent().getClass().getDeclaredField("pathPosition").setAccessible(true);
            BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getCurrent().getClass().getDeclaredField("pathPosition").set(
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getCurrent(),
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getCurrent().getPosition() + 3
            );*/
            if (index == -1) {
                currentMove = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getCurrent().getPosition();
            }
            myBlock = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions().get(currentMove);
            /*System.out.println(
                "sppoky: " + BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions().size() + ", " +
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getCurrent().getPosition()
                );*/
            System.out.println(currentMove + ", " + myBlock.x + ", " + myBlock.y + ", " + myBlock.z);
        }
    } catch (Exception e) {
        //System.out.println("Ya done goofed\n" + e);
    }

    //int minimumMove = Math.min(bPos.size(), 20);


    double myScore = 25;
    SimInstance myState = new SimInstance(stateBase.onGround, stateBase.myControls, new Vec3d(stateBase.x, stateBase.y, stateBase.z), new Vec3d(stateBase.velX, stateBase.velY, stateBase.velZ), (float)stateBase.yaw);
    for (int i = 0; i < 30; i++) {
        myState.simulatePlayer();
        myScore += 0.05;
        if (myState.controlforward && myState.controljump && myState.controlsprint) {
            System.out.println("(!!!)Doing thingies: " + i);
        }
        if (i < 31 && ((myState.onGround && (i > 3 || action == 1)) && (action == 0 || action == 1 && i >= 2) || myState.isInWater || myState.isInLava || (i >= 2 && myState.isInWeb))) {
            //console.log(i);
            i = 30;
            System.out.println("(!!!)Doing thingies2: " + dist3d(myState.x, 0, myState.z, stateBase.x, 0, stateBase.z));
        }
            if (depth == 1 && i % 3 == 0) {
                world.addParticle(ParticleTypes.FLAME,
                                      myState.x,
                                      myState.y,
                                      myState.z, 0.0, 0.0, 0.0);
            } /*else if (depth == 0 && i % 10 == 0) {
                world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                                      myState.x,
                                      myState.y,
                                      myState.z, 0.0, 0.0, 0.0);
            }*/
        //console.log(JSON.stringify(myState));
        //if (myState.isCollidedHorizontally) {myScore += 0.25;}
    }

    
    //double oldYaw = myState.yaw;
    //boolean oldSprint = myState.controlsprint;
    double tooLow = 0;
    int oldCurrentMove = 0 + currentMove;
    double oldX = myState.x;
    double oldY = myState.y;
    double oldZ = myState.z;
    boolean inBounds = false;
    double bestDistVal = 100;
    int bestDistIndex = 0;
    for (int i = currentMove; i < oldCurrentMove + 20 && i < bPos.size(); i++) {
        double leDist = dist3d(myState.x, myState.y, myState.z,
        bPos.get(i).x + 0.5, bPos.get(i).y, bPos.get(i).z + 0.5);
        if (leDist <= 5 && myState.y >= bPos.get(i).y - 2.25) {
            //myScore += dist3d(myState.pos.x, myState.pos.y, myState.pos.z,
            //                  bot.dunder.movesToGo[i].x + 0.5, bot.dunder.movesToGo[i].y, bot.dunder.movesToGo[i].z + 0.5);
            myScore -= (27 - (leDist / 2)) * (i - currentMove);
            inBounds = true;
        } else {
            myScore += (leDist) * (i - currentMove);
        }

        if (bestDistVal > leDist) {
            bestDistVal = leDist;
            bestDistIndex = i;
        }

        if (i < oldCurrentMove + 5) {
            tooLow += bPos.get(i).y - myState.y;
        }

        /*if (dist3d(lastGroundPos.x, lastGroundPos.y, lastGroundPos.z, myState.pos.x, myState.pos.y, myState.pos.z) < 1.0 && Math.abs(lastGroundPos.y - myState.pos.y) < 0.5 && dist3d(myState.pos.x, myState.pos.y, myState.pos.z, bPos.get(currentMove).x + 0.5, bPos.get(currentMove).y, bPos.get(currentMove).z + 0.5) < dist3d(lastGroundPos.x, lastGroundPos.y, lastGroundPos.z, bPos.get(currentMove).x + 0.5, bPos.get(currentMove).y, bPos.get(currentMove).z + 0.5)) {
            myScore += 1500;
        }*/
        //myScore -= Math.sqrt(myState.velX * myState.velX + myState.velZ * myState.velZ) * 3;
    }

    if (!inBounds) {
        myScore += 1000;
        bestDistIndex = -1000;
        bestDistVal = 0;
    }

    if (Math.abs(tooLow) > 7) {
        myScore += 1500;
    }

    
    //myScore -= Math.sqrt(myState.velX * myState.velX + myState.velZ * myState.velZ) * 3;
    if (Math.abs(oldY - myState.y) <= 0.25 && Math.abs(oldX - myState.x) <= (walker) && Math.abs(oldZ -  myState.z) <= (walker)) {
        myScore += (1/Math.max(0.05, (Math.abs(oldX - myState.x) + Math.abs(oldZ - myState.z)/2))) * 50;
    }
    myScore = -bestDistIndex + bestDistVal;
    if (depth > 0) {
        depth--;
        ArrayList<jumpSprintState> localJumpSprintStates = new ArrayList<jumpSprintState>();
        localJumpSprintStates.clear();
        int bestPos2 = -1;
        try {
            bestPos2 = Math.min(BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getCurrent().getPosition() - 1, 0);
            for (int i = bestPos2; i < bPos.size(); i++) {
                if (new Vec3d(myState.x, myState.y, myState.z).distanceTo( new Vec3d(bPos.get(i).x, bPos.get(i).y, bPos.get(i).z)) <
                new Vec3d(myState.x, myState.y, myState.z).distanceTo( new Vec3d(bPos.get(bestPos2).x, bPos.get(bestPos2).y, bPos.get(bestPos2).z))) {
                    bestPos2 = i;
                }
            }
        } catch (Exception e) {
            System.out.println("Baked:\n" + e);
        }
        //Simulate jump sprints
        /*myState.yaw = (float)(myState.yaw * (float)(180/(float)Math.PI));
        myState.yaw = (float)Math.round(myState.yaw / 10)*10;
        myState.yaw = myState.yaw * (float)Math.PI/180;*/
        for (int j = 0; j < 7; j++) {
            SimInstance localMyState = myState.clone();
            localMyState.yaw = (float)(myState.yaw - (Math.PI / 2) + (Math.PI / 8) + ((Math.PI / 8) * new int[]{3,4,2,5,1,7,0}[j]));
            jumpSprintState pushDis = simulateAction(depth, bestPos2, 0, new Vec3d(0, 0, 0), localMyState);
            if (pushDis != null) {
                localJumpSprintStates.add(pushDis);
            }
        }
        /*for (int j = 0; j < 5; j++) {
            SimInstance localMyState = myState.clone();
            localMyState.controljump = false;
            localMyState.myControls.set(1, false);
            localMyState.yaw = (float)(myState.yaw - (Math.PI / 2) + (Math.PI / 8) + ((Math.PI / 8) * new int[]{3,4,2,7,0}[j]));
            jumpSprintState pushDis = simulateAction(depth, bestPos2, 1, new Vec3d(0, 0, 0), localMyState);
            if (pushDis != null) {
                localJumpSprintStates.add(pushDis);
            }
        }*/
        /*for (int j = 0; j < 1; j++) {
          SimInstance localMyState = myState.clone();
          localMyState.controlleft = true;
          localMyState.myControls.set(5, true);
          localMyState.yaw = (float)(myState.yaw - (Math.PI / 2) + (Math.PI / 8) + ((Math.PI / 8) * new int[]{3,4,2,5,1,7,0}[j]));
          jumpSprintState pushDis = simulateAction(depth, bestPos2, 0, new Vec3d(0, 0, 0), localMyState);
          if (pushDis != null) {
              localJumpSprintStates.add(pushDis);
          }
        }
        for (int j = 0; j < 1; j++) {
          SimInstance localMyState = myState.clone();
          localMyState.controlright = true;
          localMyState.myControls.set(6, true);
          localMyState.yaw = (float)(myState.yaw - (Math.PI / 2) + (Math.PI / 8) + ((Math.PI / 8) * new int[]{3,4,2,5,1,7,0}[j]));
          jumpSprintState pushDis = simulateAction(depth, bestPos2, 0, new Vec3d(0, 0, 0), localMyState);
          if (pushDis != null) {
              localJumpSprintStates.add(pushDis);
          }
        }*/

        int newScoreIndex = -1;
        for (int i = 0; i < localJumpSprintStates.size(); i++) {
            if (newScoreIndex == -1 || localJumpSprintStates.get(i).score < localJumpSprintStates.get(newScoreIndex).score) {
                newScoreIndex = i;
            }
        }
        if (newScoreIndex > -1) {
            myScore = localJumpSprintStates.get(newScoreIndex).score;
        }
    }

    System.out.println("score " + myScore);
    if (myState.onGround && !myState.isInLava) {
        return new jumpSprintState(myState, true, myState.controljump, myScore);//{state:myState,parent:theParent,open:true, shouldJump:true, score:myScore};
    } else {
        return new jumpSprintState(myState, true, myState.controljump, myScore + 5000);//{state:myState,parent:theParent,open:true, shouldJump:true, score:myScore + 5000};
    }
}

/*private void jumpSprintOnPath(Vec3d target, SimInstance myStateBase, int searchCount, int theParent) {
    if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior() != null &&
    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath() != null &&
    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get() != null &&
    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions() != null &&
    BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions().getLast() != null) {

    } else {
        return;
    }
    List<BetterBlockPos> bPos = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath().get().positions();
        int currentMove = bPos.size() - 1;
        int minimumMove = currentMove - 20;
        if (minimumMove < 0) {
            minimumMove = 0;
        }
        //console.log("minimumMove: " + minimumMove);
        if (bPos.get(minimumMove) == null) {
            return;
        }

        var myDelta = new Vec3d(bPos.get(minimumMove).x + 0.5 - myStateBase.pos.x, bPos.get(minimumMove).y - myStateBase.pos.y, bPos.get(minimumMove).z + 0.5 - myStateBase.pos.z);
        myStateBase.yaw = (float)Math.atan2(-myDelta.x, -myDelta.z);
        //console.log(myDelta.x + ", " + -Math.cos(-Math.PI/2 + myStateBase.yaw));
        //console.log(myDelta.z + ", " + -Math.sin(-Math.PI/2 + myStateBase.yaw));
        //console.log(-(Math.PI/2 + Math.atan2(-Math.cos(myStateBase.yaw), -Math.sin(myStateBase.yaw))) + " : " + myStateBase.yaw + " : " + (Math.atan2(Math.cos(-Math.PI/2 + myStateBase.yaw), -Math.sin(-Math.PI/2 + myStateBase.yaw))));

        //Simulate jump sprints
        for (int j = 0; j < 7; j++) {
          SimInstance myState = myStateBase.clone();
          myState.yaw = (float)(myStateBase.yaw - (Math.PI / 2) + (Math.PI / 8) + ((Math.PI / 8) * new int[]{3,4,2,5,1,7,0}[j]));
          jumpSprintState pushDis = simulateAction(0, target, myState);
          if (pushDis != null) {
              jumpSprintStates.add(pushDis);
          }
        }


      if (jumpSprintStates.size() > 0) {
        int myBestState = 0;
        for (int i = 0; i < jumpSprintStates.size(); i++) {
            if (jumpSprintStates.get(i).open == true && jumpSprintStates.get(i).score < jumpSprintStates.get(myBestState).score) {
                myBestState = i;
            }
        }
        if (searchCount <= 0) {
            //console.log("decent jumps found");
            jumpSprintState mySearcher = jumpSprintStates.get(myBestState);
            *while (mySearcher.parent) {
                bot.dunder.jumpTargets.push(mySearcher.state.pos);
                mySearcher = mySearcher.parent;
            }*
            jumpTargets.add(mySearcher.state.pos);
            jumpTarget = mySearcher.state.pos;
            jumpYaw = mySearcher.state.yaw;
            bestJumpSprintState = myBestState;
            if (mySearcher.state.isInLava) {System.out.println("fire");}
            if (mySearcher.score > -131) {
                jumpTargetDelay = 15;
            }
        }
      }
    }*/
}