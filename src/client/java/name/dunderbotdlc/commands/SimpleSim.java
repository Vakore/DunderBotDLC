package name.dunderbotdlc.commands;

import java.util.ArrayList;

import baritone.api.utils.input.Input;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

public class SimpleSim {
    //physics
  /*public double physicsStepHeight = 0.6;
  
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
    public SimpleSim(boolean grounded, ArrayList<Boolean> leControls, Vec3d ps, Vec3d pv, float ya) {
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
    }*/
}
