/*
 * TODO:
 * target.getDataTracker().getChangedEntries().size() <- can cause errors if
 * getChangedEntries is empty
 * DONT ALLOW the bot taking control is baritone is
 * making the bot jump, or take baritone's control
 * over bot jumping away
 *
 * #set primaryTimeoutMS 1
#set primaryTimeoutMS 500
#settings planAheadPrimaryTimeoutMS 1
#settings planAheadPrimaryTimeoutMS 4000
 */

package name.dunderbotdlc;

//import name.dunderbotdlc.commands.;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import name.dunderbotdlc.commands.*;
import name.dunderbotdlc.inventorymanagement.InventoryScorer;
import name.dunderbotdlc.physics.SimInstance;
import name.dunderbotdlc.physics.SimpleSim;
import name.dunderbotdlc.physics.SmartWalk;
import name.dunderbotdlc.structs.AABB;
import name.dunderbotdlc.structs.jumpSprintState;
import net.fabricmc.api.ClientModInitializer;
//import name.dunderbotdlc.structs.AABB;
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
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.BubbleColumnBlock;
import net.minecraft.block.Waterloggable;
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
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.ParticleTypes;
//import net.minecraft.util.math.BlockPos;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
/*import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;*/
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.shape.VoxelShape;
//import name.dunderbotdlc.commands.Prediction;
//import net.minecraft.client.util.InputUtil;
//import name.dunderbotdlc.mixin.client.IBaritoneAPI;



public class DunderBotdlcClient implements ClientModInitializer {
    public static int bestPathNum = 0;

    public boolean isBotting = false;
    public IBaritone bbaritone;
    //private PathingCommand pauseCommand;
    public int attackCooldown = 0;
    public static ClientWorld world;
    public Entity target;
    public int targetID;
    public double threatLevel;
    public Vec3d playerPos;
    public PlayerEntity player;
    public static MinecraftClient client;

    public boolean c_spr = false;
    public boolean c_W = false;
    public boolean c_S = false;
    public boolean c_A = false;
    public boolean c_D = false;
    public boolean c_Z = false;
    public boolean c_j = false;
    public boolean c_lc = false;
    public boolean c_rc = false;
    public int shieldTimer = 0;
    public int botMode = 1;
    public Map<String, Integer> mobMap = new HashMap<String, Integer>();
    public Map<String, Vec3d> projectileMap = new HashMap<String, Vec3d>();
    public ArrayList<Entity> threatList = new ArrayList<Entity>();
    public ArrayList<Double> threatTable = new ArrayList<Double>();
    public ArrayList<Double> threatDist = new ArrayList<Double>();

    public ArrayList<jumpSprintState> jumpSprintStates = new ArrayList<jumpSprintState>();
    public jumpSprintState leBest;
    //private ArrayList<Vec3d> jumpTargets = new ArrayList<Vec3d>();
    //private Vec3d jumpTarget;
    //private float jumpYaw;
    //private int bestJumpSprintState;
    public int noJumpAttempts = 0;
	//private Rotation rotation;
    public PrintStream myStream;


    //
    // inside DunderBotdlcClient
    public final BlockCache blockCache = new BlockCache();
    public static int currentMove = 0;
    public static List<BetterBlockPos> bPos = new ArrayList<BetterBlockPos>();

    public static final class BlockCache {
        // key = ((x & 0xFFF)<<20) | ((z & 0xFFF)<<8) | (y & 0xFF)
        private final Long2ObjectOpenHashMap<CachedBlock> map = new Long2ObjectOpenHashMap<>();

        void clear()        { map.clear(); }
        CachedBlock get(int x, int y, int z) {
            long key = ((long)(x & 0xFFF) << 20) | ((long)(z & 0xFFF) << 8) | (y & 0xFF);
            return map.computeIfAbsent(key, k -> new CachedBlock(x, y, z));
        }
    }
    public static final class CachedBlock {
        final AABB[] colliders;   // null if empty
        AABB waterBB;       // null if not water
        final boolean isWeb, isBubbleDrag;
        CachedBlock(int bx, int by, int bz) {
            BlockState bs = world.getBlockState(new BlockPos(bx, by, bz));
            // ---- collision ----
            VoxelShape vs = bs.getCollisionShape(world, new BlockPos(bx, by, bz));
            List<Box> boxes = vs.getBoundingBoxes();
            if (boxes.isEmpty()) {
                colliders = null;
            } else {
                colliders = new AABB[boxes.size()];
                for (int i = 0; i < boxes.size(); i++) {
                    Box b = boxes.get(i);
                    colliders[i] = new AABB(bx + b.minX, by + b.minY, bz + b.minZ,
                            bx + b.maxX, by + b.maxY, bz + b.maxZ);
                }
            }
            // ---- water ----
            FluidState fs = bs.getFluidState();
            boolean water = fs.isStill() || fs.isOf(Fluids.WATER) ||
                    (bs.getBlock() instanceof Waterloggable && bs.get(Properties.WATERLOGGED));
            if (water) {
                double h = fs.getHeight(world, new BlockPos(bx, by, bz));
                double top = by + 1 - h;
                waterBB = new AABB(bx, by, bz, bx + 1, top, bz + 1);
            } else waterBB = null;
            isWeb = bs.isOf(Blocks.COBWEB);
            isBubbleDrag = bs.isOf(Blocks.BUBBLE_COLUMN) && bs.get(BubbleColumnBlock.DRAG);
        }
    }
    //

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

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(ClientCommandManager.literal("moveitem")
                    .then(ClientCommandManager.argument("slotFrom", IntegerArgumentType.integer(0, 45))
                            .then(ClientCommandManager.argument("slotTo", IntegerArgumentType.integer(0, 45))
                                    .executes(context -> {
                                        int slotFrom = IntegerArgumentType.getInteger(context, "slotFrom");
                                        int slotTo = IntegerArgumentType.getInteger(context, "slotTo");
                                        moveItem(slotFrom, slotTo);//36 = hotbar, 1-4 = crafting, 9 = inventory begin
                                        context.getSource().sendFeedback(Text.of("Moved item from slot " + slotFrom + " to slot " + slotTo));
                                        return 1;
                                    }))));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(ClientCommandManager.literal("throwitem")
                    .then(ClientCommandManager.argument("slot", IntegerArgumentType.integer(0, 45))
                                    .executes(context -> {
                                        int slot = IntegerArgumentType.getInteger(context, "slot");
                                        throwItem(slot);
                                        context.getSource().sendFeedback(Text.of("Threw item from slot " + slot));
                                        return 1;
                                    })));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(ClientCommandManager.literal("scoreinventory")
                    .then(ClientCommandManager.argument("table", IntegerArgumentType.integer(0, 10))
                            .executes(context -> {
                                int table = IntegerArgumentType.getInteger(context, "table");
                                MinecraftClient client = MinecraftClient.getInstance();
                                if (client.player != null) {
                                    double score = InventoryScorer.scoreInventory(client.player.getInventory().main).totalScore;
                                    context.getSource().sendFeedback(Text.of("Inventory score: " + (Math.round(score * 20.0f) / 20.0f)));
                                    System.out.println("Score: " + score);
                                }

                                return 1;
                            })));
        });
	}

    public void throwItem(int slot) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.interactionManager != null) {
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slot, 1, SlotActionType.THROW, client.player);
        }
    }
    public void moveItem(int slotFrom, int slotTo) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.interactionManager != null) {
            // Select the item from the source slot
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slotFrom, 0, SlotActionType.PICKUP, client.player);
            // Place it in the target slot
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slotTo, 0, SlotActionType.PICKUP, client.player);
            // Then place the target slot's item back into the source slot
            client.interactionManager.clickSlot(client.player.currentScreenHandler.syncId, slotFrom, 0, SlotActionType.PICKUP, client.player);
        }
    }

    private void logInventory(PlayerEntity player) {
        if (player == null) return;

        System.out.println("=== Player Inventory ===");
        player.getInventory().main.forEach(stack -> {
            if (!stack.isEmpty()) {
                System.out.println(stack.getCount() + "x " + stack.getName().getString());
            }
        });
        System.out.println("========================");
        System.out.println("Hello World!");
    }

    //@SuppressWarnings("rawtypes")
	private void onClientTick() {
        blockCache.clear();
        
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
                botMode = 0;
                if (client.options.hotbarKeys[0].isPressed()) {
                    botMode = 1;
                } else if (client.options.hotbarKeys[1].isPressed()) {
                    botMode = 2;
                } else if (client.options.hotbarKeys[2].isPressed()) {
                    botMode = 3;
                }  else if (client.options.hotbarKeys[8].isPressed()) {
                    logInventory(client.player);
                }

                BaritoneAPI.getProvider().getPrimaryBaritone().getInputOverrideHandler().clearAllKeys();
                ((IBaritoneAPIMixin) BaritoneAPI.getProvider()).setSoftPause(isBotting);
                //if (isBotting) {
                   //Check IBaritoneAPIMixin.java to see this madness
                //}
                /*if (isBotting) {
                    if (botMode == 0) {
                        botMode = 1;
                    } else {
                        botMode = 0;
                    }
                }*/
                System.out.println("DunderLC - isBotting is now set to " + isBotting + ", mode: " + botMode);
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
                    DunderPvE.doPvE(this);
                } else if (botMode == 1) {
                    DunderJumpSprint.doJumpsprint(this, 3);
                } else if (botMode == 2) {
                    doAdjustTests();
                } else if (botMode == 3) {
                    doAdjustTests2();
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
                    client.player.setSprinting(false);
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

	public void lookAtEntity(PlayerEntity player, Entity entity) {
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
    
    public void releaseKey(ClientPlayerEntity player, KeyBinding keyBinding) {
        if (keyBinding.isPressed()) {
            keyBinding.setPressed(false);
        }
    }

    public void pressKey(ClientPlayerEntity player, KeyBinding keyBinding, boolean spam) {
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

    if (index >= 0) {
        currentMove = index;
    } else if (index == -1) {
        DunderBotdlcClient.currentMove = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getCurrent().getPosition();
    }
    //System.out.println(DunderBotdlcClient.currentMove + ", " + DunderBotdlcClient.myBlock.x + ", " + DunderBotdlcClient.myBlock.y + ", " + DunderBotdlcClient.myBlock.z);

    //Profiling
    /*if (depth >= 0) {
        return new jumpSprintState(stateBase, true, true, 0.0);
    }*/
    double walker = 0.5 - (0.25 * action);

    //int minimumMove = Math.min(bPos.size(), 20);


    double myScore = 25;
    //boolean[] barry = new boolean[stateBase.myControls.size()];
    //OptimizedSimInstance myState = new OptimizedSimInstance(new Vec3d(stateBase.x, stateBase.y, stateBase.z), new Vec3d(stateBase.velX, stateBase.velY, stateBase.velZ), (float)stateBase.yaw, stateBase.onGround, barry, world);

    SimInstance myState = new SimInstance(stateBase.onGround, stateBase.myControls, new Vec3d(stateBase.x, stateBase.y, stateBase.z), new Vec3d(stateBase.velX, stateBase.velY, stateBase.velZ), (float)stateBase.yaw);
    for (int i = 0; i < 30; i++) {
        myState.simulatePlayer();
        myScore += 0.05;
        if (myState.controlforward && myState.controljump && myState.controlsprint) {
            //System.out.println("(!!!)Doing thingies: " + i);
        }
        if (i < 31 && ((myState.onGround && (i > 3 || action == 1)) && (action == 0 || action == 1 && i >= 2) || myState.isInWater || myState.isInLava || (i >= 2 && myState.isInWeb))) {
            //console.log(i);
            i = 30;
            //System.out.println("(!!!)Doing thingies2: " + dist3d(myState.x, 0, myState.z, stateBase.x, 0, stateBase.z));
        }
            /*if (depth == 1 && i % 3 == 0) {
                world.addParticle(ParticleTypes.FLAME,
                                      myState.x,
                                      myState.y,
                                      myState.z, 0.0, 0.0, 0.0);
            } else if (depth == 0 && i % 10 == 0) {
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
            if (bestPos2 < 0) {bestPos2 = 0;}
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

    //System.out.println("score " + myScore);
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

    int slowDownTicks = 0;
    int slowDownType = 1;
    public void doAdjustTests2() {
        slowDownTicks = SimpleSim.simpleProblem(player, 0, 1);
    }
    public void doAdjustTests() {
        slowDownTicks--;
        if (player.isOnGround() && slowDownTicks <= 0) {
            slowDownTicks = SimpleSim.simpleProblem(player, 0, 0);
            System.out.println("Slow down ticks: " + slowDownTicks);
            slowDownType = 0;
            if (slowDownTicks < 0) {
                System.out.println("slowDownTicks: " + slowDownTicks);
                slowDownTicks = -slowDownTicks;
                //slowDownTicks -= 7;
                slowDownType = 1;
                System.out.println("slowing down...");
            }
        }
        c_W = true;
        c_spr = (slowDownTicks <= 0 || slowDownType == 0);
        c_j = (player.getVelocity().y <= 0 && (slowDownTicks <= 0 || slowDownType == 1));
    }
}