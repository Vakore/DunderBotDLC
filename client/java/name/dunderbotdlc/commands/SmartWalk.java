package name.dunderbotdlc.commands;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;


/*
Next steps:
0. End raycast on demand - DONE

1. Determine if a jump will ascend a player

2. Determine if a jump will descend a player, descend during raycast too
    -Basics - done
    -colliding on descent - not done(headhitters)

3. Handle headhitters(jump sprinting in 2-block tall spaces should
  be considered "less complicated" than flat terrian)


 * 16 ticks - ~5.46, down 3(fall damage, since we jumped 1 first)
 * 14 ticks - ~5.1, down 2
 * 13 ticks - ~4.1, down 1
 * 11 ticks - ~4, flat
 * 8 ticks - ~3, up 1
 * 
 We'll assume 4 for flat or down, and 5 for down 2
 */
public class SmartWalk {

    public static int mapSize = 33;//17;
    public static int halfMap = 16;//8;

    private static final int[] rowDirs = {-1, 1, 0, 0};
    private static final int[] colDirs = {0, 0, -1, 1};
    public static ArrayList<Boolean> moveInDir(SimInstance myState) {
        int toValid = 1;
        boolean toJump = false;
    
        for (int i = 0; i < 10; i++) {
            myState.simulatePlayer();
            if ((i < 5) && (!myState.onGround && !myState.isInWater) || myState.isInLava) {
                toValid = (myState.isInLava) ? -1 : 0;
            } else if (i >= 5 && toValid == 0 && myState.onGround || myState.isInWater) {
                toValid = 1;
            }
            //if (maxVelX > Math.abs(myState.vel.x) * 1.1 || maxVelZ > Math.abs(myState.vel.z) * 1.1) {
            if (i < 5 && myState.isCollidedHorizontally) {
                //i = 10;
                toJump = true;
            }
        }
        if (toValid >= 1) {
            if (toJump) {myState.controljump = true;}
        } else if (myState.onGround) {
            myState.controlsneak = true;
        }
        
        ArrayList<Boolean> playerControlList = new ArrayList<Boolean>();
        playerControlList.add(myState.controlsneak);
        playerControlList.add(myState.controljump);
        playerControlList.add(myState.controlsprint);
        playerControlList.add(myState.controlforward);
        playerControlList.add(myState.controlback);
        playerControlList.add(myState.controlleft);
        playerControlList.add(myState.controlright);

        return playerControlList;
    };

    public static int distToInt(double posX, double posY, double trueMapX, double trueMapY) {
        double squareDist = (trueMapX - posX)*(trueMapX - posX) + (trueMapY - posY)*(trueMapY - posY);

        if (squareDist >= 30.25) {
            return 3;
        } else if (squareDist >= 25.0) {
            return 2;
        } else if (squareDist >= 16.0) {
            return 1;
        }
        return 0;
    }

    public static int planRaycast(int iter, int mx, int my, ClientWorld world, int[][] heightmap, int[][] headmap, int[][] liquidMap, double iPosX, double iPosH, double iPosY, double angle) {
        double posX = ((iPosX) - Math.floor(iPosX)) + mx;
        double posY = ((iPosY) - Math.floor(iPosY)) + my;
        double mapX = Math.floor(posX);
        double mapY = Math.floor(posY);
        if ((int)mapX >= heightmap[0].length || (int)mapY >= heightmap[0].length ||
            (int)mapX < 0 || (int)mapY < 0) {
            return 0;
        }
        int myHeight = heightmap[(int)mapX][(int)mapY];
        //System.out.println("itr " + iter + " myh " + myHeight);
        int myHeightOG = myHeight + 0;
        double cosAngle = Math.cos(angle * Math.PI / 180.0);
        double sinAngle = Math.sin(angle * Math.PI / 180.0);
        double dx = (cosAngle == 0.0) ? 0.0 : Math.abs(1 / cosAngle);
        double dy = (sinAngle == 0.0) ? 0.0 : Math.abs(1 / sinAngle);
        double sideDx = 0.0;
        double sideDy = 0.0;
        int stepX = 0;
        int stepY = 0;
        int hit = 0;
        int edge = 0;
        int side = 0;
        if (cosAngle < 0.0) {
            stepX = -1;
            sideDx = (posX - mapX) * dx;
        } else {
            stepX = 1;
            sideDx = (mapX + 1.0 - posX) * dx;
        }
    
        if (sinAngle < 0) {
            stepY = -1;
            sideDy = (posY - mapY) * dy;
        } else {
            stepY = 1;
            sideDy = (mapY + 1.0 - posY) * dy;
        }

        //System.out.println(sideDx + ", " + sideDy);
    
        while (hit == 0) {
            //jump to next map square, either in x-direction, or in y-direction
            if (sideDx < sideDy) {
                sideDx += dx;
                mapX += stepX;
                side = 0;
            } else {
                sideDy += dy;
                mapY += stepY;
                side = 1;
            }

            double wallX; //where exactly the wall was hit
            if (side == 0) {
                wallX = posY + (sideDx - dx) * sinAngle;
            } else {
                wallX = posX + (sideDy - dy) * cosAngle;
            }
            wallX -= Math.floor((wallX));
        
            double trueMapX = mapX + 0.0;
            double trueMapY = mapY + 0.0;
            if (side == 0) {
                trueMapY += wallX;
                if (cosAngle < 0) {
                    trueMapX++;
                }
            } else {
                trueMapX += wallX;
                if (sinAngle < 0) {
                    trueMapY++;   
                }
            }

            //Check if ray has hit a wall
            if (mapX < 0.0 || mapX >= heightmap.length ||
                mapY < 0.0 || mapY >= heightmap.length) {
                //System.out.println("Complication: 0");
                edge = 7;
                hit = 1;
            } else if (heightmap[(int)mapX][(int)mapY] > myHeight) {
                //edge = 1;//doing this for now. change later to allow ascending/descending based on distance
                if ((trueMapX - posX)*(trueMapX - posX) + (trueMapY - posY)*(trueMapY - posY) <= 8) {
                    myHeight++;
                    edge = 2;
                } else {
                    edge = 1;
                }

                hit = 1;
            } else if ((trueMapX - posX)*(trueMapX - posX) + (trueMapY - posY)*(trueMapY - posY) >= 16 &&
                        heightmap[(int)mapX][(int)mapY] <= myHeight) {
                if (heightmap[(int)mapX][(int)mapY] < myHeight) {
                    myHeight = myHeightOG - distToInt(posX, posY, trueMapX, trueMapY);
                    if (heightmap[(int)mapX][(int)mapY] > myHeight) {
                        myHeight = heightmap[(int)mapX][(int)mapY];
                        hit = 1;
                    }
                } else {
                    hit = 1;
                }
            }
            world.addParticle(ParticleTypes.FLAME,
                                      mapX - mx + iPosX,
                                      myHeight - myHeightOG + iPosH,
                                      mapY - my + iPosY, 0.0, 0.0, 0.0);
        }

        //return if out of bounds
        if (edge == 7) {
            return 0;
        }

        //calculate value of wallX
        double wallX; //where exactly the wall was hit
        if (side == 0) {
            wallX = posY + (sideDx - dx) * sinAngle;
        } else {
            wallX = posX + (sideDy - dy) * cosAngle;
        }
        wallX -= Math.floor((wallX));

        if (edge == 0) {
            liquidMap[(int)mapX][(int)mapY] = 1;
        } else if (edge == 2) {
            liquidMap[(int)mapX][(int)mapY] = 2;
        } 
        
        if (side == 0) {
            mapY += wallX;
            if (cosAngle < 0) {
                mapX++;
            }
        } else {
            mapX += wallX;
            if (sinAngle < 0) {
                mapY++;   
            }
        }

        if (edge == 2) {
            mapX += cosAngle * 0.2;
            mapY += sinAngle * 0.2;
        } else if (edge == 0 && (posX-(mapX))*(posX-(mapX)) + (posY-(mapY))*(posY-(mapY)) > 4.2*4.2) {
            mapX = posX + (cosAngle * 4.2);
            mapY = posY + (sinAngle * 4.2);
        }

        world.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                                  mapX - mx + Math.floor(iPosX),
                                  myHeight - myHeightOG + iPosH + 0.5,
                                  mapY - my + Math.floor(iPosY), 0.0, 0.0, 0.0);
        

        //System.out.println("iter " + iter + " angle " + angle);
        int bestScore = 0;
        if (iter > 0) {
            bestScore = Math.max(bestScore, planRaycast(iter-1, (int)mapX, (int)mapY, world, heightmap, headmap, liquidMap, mapX - mx + Math.floor(iPosX), myHeight - myHeightOG + iPosH, mapY - my + Math.floor(iPosY), angle));
            for (int i = 1; i < 5; i++) {
                //planRaycast(iter-1, (int)mapX, (int)mapY, world, heightmap, headmap, liquidMap, mapX - mx + Math.floor(iPosX), myHeight - myHeightOG + iPosH, mapY - my + Math.floor(iPosY), angle + 22.5*i);
                //planRaycast(iter-1, (int)mapX, (int)mapY, world, heightmap, headmap, liquidMap, mapX - mx + Math.floor(iPosX), myHeight - myHeightOG + iPosH, mapY - my + Math.floor(iPosY), angle - 22.5*i);
            }
            return bestScore;
        } else {
            return iter;
        }
        //from edge to edge going up one block - 3 blocks straight, a little over 2 diag
        //return 0; //[mapX * 16, mapY * 16];
    };

    public static void thinkJump(ClientWorld world, Vec3d pos, double yaw) {
        long startTime = System.nanoTime();
        Vec3i p = new Vec3i((int)Math.floor(pos.x), (int)Math.floor(pos.y), (int)Math.floor(pos.z));
        int[][] array = new int[mapSize][mapSize];
        int[][] headbutter = new int[mapSize][mapSize];
        int[][] liquidType = new int[mapSize][mapSize];
        boolean[][] visited = new boolean[array.length][array[0].length];
        Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{halfMap, halfMap, 4});
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int row = current[0];
            int col = current[1];
            int x = current[0]-halfMap;
            int z = current[1]-halfMap;
            int y = current[2]-4;
            int oldC = current[2] + 0;

            //
            headbutter[row][col] = -15;
            if (world.getBlockState(new BlockPos(p.add(x, y, z))).getCollisionShape(world, new BlockPos(p.add(x, y, z))).isEmpty() &&
                world.getBlockState(new BlockPos(p.add(x, y+1, z))).getCollisionShape(world, new BlockPos(p.add(x, y+1, z))).isEmpty()) {
                if (!world.getBlockState(new BlockPos(p.add(x, y+2, z))).getCollisionShape(world, new BlockPos(p.add(x, y+2, z))).isEmpty()) {
                    headbutter[row][col] = current[2] + 3;
                } else if (!world.getBlockState(new BlockPos(p.add(x, y+3, z))).getCollisionShape(world, new BlockPos(p.add(x, y+3, z))).isEmpty()) {
                    headbutter[row][col] = current[2] + 4;
                }

                
                int maxFall = 10;
                String blockName = world.getBlockState(new BlockPos(p.add(x, y, z))).getBlock().getName().getString().toLowerCase();
                if (blockName.contains("water")) {
                    maxFall = 0;
                    liquidType[row][col] = 1;
                } else if (blockName.contains("lava")) {
                    maxFall = 0;
                    liquidType[row][col] = 2;
                } else {
                    while (maxFall > 0 && world.getBlockState(new BlockPos(p.add(x, y-1, z))).getCollisionShape(world, new BlockPos(p.add(x, y-1, z))).isEmpty()) {
                        current[2]--;
                        y--;
                        maxFall--;
                        blockName = world.getBlockState(new BlockPos(p.add(x, y, z))).getBlock().getName().getString().toLowerCase();
                        if (blockName.contains("water")) {
                            maxFall = 0;
                            liquidType[row][col] = 1;
                        } else if (blockName.contains("lava")) {
                            maxFall = 0;
                            liquidType[row][col] = 2;
                        }
                    }
                }
            } else {
                int finished = 10;
                while (finished > 0) {
                    current[2]++;
                    y++;
                    finished--;
                    if (world.getBlockState(new BlockPos(p.add(x, y, z))).getCollisionShape(world, new BlockPos(p.add(x, y, z))).isEmpty() &&
                        world.getBlockState(new BlockPos(p.add(x, y+1, z))).getCollisionShape(world, new BlockPos(p.add(x, y+1, z))).isEmpty()) {
                        finished = 0;
                    }
                }
                String blockName = world.getBlockState(new BlockPos(p.add(x, y, z))).getBlock().getName().getString().toLowerCase();
                if (blockName.contains("water")) {
                    liquidType[row][col] = 1;
                } else if (blockName.contains("lava")) {
                    liquidType[row][col] = 2;
                }
            }

            /*if (world.getBlockState(new BlockPos(p.add(x, y+1, z))).getCollisionShape(world, new BlockPos(p.add(x, y+1, z))).isEmpty() ) {
                if (world.getBlockState(new BlockPos(p.add(x, y, z))).getCollisionShape(world, new BlockPos(p.add(x, y, z))).isEmpty()) {
                    int maxFall = 10;
                    while (maxFall > 0 && world.getBlockState(new BlockPos(p.add(x, y-1, z))).getCollisionShape(world, new BlockPos(p.add(x, y-1, z))).isEmpty()) {
                        current[2]--;
                        y--;
                        maxFall--;
                    }
                    /*if (maxFall == 0) {
                        current[2] -= 200;
                    }*
                    //height = height;
                } else {
                    current[2] += 1;
                }
            }*/
            array[row][col] = current[2];
            //
            if (Math.abs(current[2] - oldC) <= 2) {
                for (int i = 0; i < 4; i++) {
                    int newRow = row + rowDirs[i];
                    int newCol = col + colDirs[i];
                    if (newRow >= 0 && newRow < array.length && newCol >= 0 && newCol < array[0].length && (!visited[newRow][newCol])) {
                        queue.offer(new int[]{newRow, newCol, current[2] + 0});
                        visited[newRow][newCol] = true;
                    }
                }
            }
        }

        //System.out.println(array);
        System.out.println(((double)(System.nanoTime() - startTime)/1000000) + "\n \u001B[32m");
        yaw += 90;
        int depth = 2;

        int bestScore = 0;
        bestScore = Math.max(bestScore, planRaycast(depth, halfMap, halfMap, world, array, headbutter, liquidType, pos.x, pos.y, pos.z, yaw));
        for (int i = 1; i < 5; i++) {
            Math.max(bestScore, planRaycast(depth, halfMap, halfMap, world, array, headbutter, liquidType, pos.x, pos.y, pos.z, yaw + 22.5*i));
            Math.max(bestScore, planRaycast(depth, halfMap, halfMap, world, array, headbutter, liquidType, pos.x, pos.y, pos.z, yaw - 22.5*i));
        }
        //planRaycast(depth, halfMap, halfMap, world, array, headbutter, liquidType, pos.x, pos.y, pos.z, yaw + 90);
        //planRaycast(depth, halfMap, halfMap, world, array, headbutter, liquidType, pos.x, pos.y, pos.z, yaw - 90);

        
        System.out.println(((double)(System.nanoTime() - startTime)/1000000) + "\n \u001B[32m");

        //System.out.println("Skipping heightmap printing");
        for (int j = 0; j < array.length; j++) {
            String thisRow = "";
            for (int i = 0; i < array[0].length; i++) {
                if (liquidType[i][j] == 1) {
                    thisRow += "\033[44m";
                } else if (liquidType[i][j] == 2) {
                    thisRow += "\033[41m";
                }
                if (array[i][j] > 0 && array[i][j] < 10) {
                    if (headbutter[i][j] == array[i][j] + 3) {
                        thisRow += "\u001B[36m";
                    } else if (headbutter[i][j] == array[i][j] + 4) {
                        thisRow += "\u001B[37m";
                    } else {
                        thisRow += (array[i][j] % 2 == 0) ? "\u001B[32m" : "\u001B[33m";
                    }
                    thisRow += array[i][j];
                } else {
                    thisRow += "\u001B[31m" + "X";// + "\u001B[32m";
                }
                thisRow += "\u001B[0m";
            }
            System.out.println(thisRow);
        }
        System.out.println("\u001B[0m" + ((System.nanoTime() - startTime)/1000000));


        /*for (int j = -5; j < 5; j++) {
            String thisRow = "";
            for (int i = -5; i < 5; i++) {
                if (world.getBlockState(new BlockPos(p.add(i, 1, j))).getCollisionShape(world, new BlockPos(p.add(i, 1, j))).isEmpty() ) {
                    if (world.getBlockState(new BlockPos(p.add(i, 0, j))).getCollisionShape(world, new BlockPos(p.add(i, 0, j))).isEmpty()) {
                        thisRow += "0";
                    } else {
                        thisRow += "1";
                    }
                } else {
                    thisRow += "2";
                }
            }
            System.out.println(thisRow);
        }*/
    }
}
