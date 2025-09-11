package name.dunderbotdlc.commands;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static name.dunderbotdlc.DunderBotdlcClient.client;

/*
8 - 11 falling = too far, try delaying jump slightly
0 - 1 ascending = too close, try slowing down for a tick or two on current jump,
                  or do so on the previous jump
*/

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.FoodComponent;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.EmptyBlockView;

import java.util.*;

public class InventoryScorer {

    private static final List<String> WEAPON_LEVELS = List.of("wooden", "golden", "stone", "iron", "diamond", "netherite");
    private static final List<String> TOOL_LEVELS = List.of("wooden", "stone", "golden", "iron", "diamond", "netherite");
    private static final List<String> ARMOR_LEVELS = List.of("leather", "golden", "chainmail", "iron", "diamond", "netherite");

    private static final Map<String, Integer> WEAPON_BONUSES = Map.of("sword", 50);
    private static final Map<String, Integer> TOOL_BONUSES = Map.of(
            "pickaxe", 45,
            "axe", 40,
            "shovel", 20,
            "hoe", 10
    );

    // Static map to track the slot indices of the best items (e.g., best sword, axe, etc.)
    private static final Map<String, Integer> BEST_ITEM_SLOTS = new HashMap<>();

    public static class InventoryScore {
        public double totalScore = 0;
        public double[] slotScores = new double[36]; // 36 slot main inventory
        public Map<String, Integer> itemCounts = new HashMap<>();
        public int waterBuckets = 0;
        public int pufferfishBuckets = 0;
        public int lavaBuckets = 0;
        public int flintAndSteelCount = 0;
        public int boatCount = 0;
        public int blockStacks = 0;
        public int bestSword = 0;
        public int bestAxe = 0;
        public int bestPickaxe = 0;
        public Map<String, Integer> bestItemSlots = new HashMap<>(); // Tracks slot indices of best items
        public Map<String, FoodInfo> foodInfo = new HashMap<>(); // Stores food properties
    }

    public static class FoodInfo {
        public double effectiveQuality;
        public int hungerBars;
        public float saturationPoints;

        public FoodInfo(double effectiveQuality, int hungerBars, float saturationPoints) {
            this.effectiveQuality = effectiveQuality;
            this.hungerBars = hungerBars;
            this.saturationPoints = saturationPoints;
        }
    }

    public static InventoryScore scoreInventory(DefaultedList<ItemStack> inventory) {
        InventoryScore result = new InventoryScore();

        Map<String, Integer> bestToolLevel = new HashMap<>();
        Map<String, Integer> bestWeaponLevel = new HashMap<>();
        Map<String, Integer> bestArmorLevel = new HashMap<>();
        int[] bestSlotIndices = new int[36];

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if (stack.isEmpty()) continue;

            Item item = stack.getItem();
            String name = Registries.ITEM.getId(item).getPath(); // e.g., diamond_sword
            int count = stack.getCount();
            double slotScore = 0;
            boolean shouldReduce = true;

            // Update item count
            result.itemCounts.put(name, result.itemCounts.getOrDefault(name, 0) + count);

            // Durability multiplier
            double durabilityMultiplier = 1.0;
            if (stack.isDamageable()) {
                int max = stack.getMaxDamage();
                int current = max - stack.getDamage();
                durabilityMultiplier = current / (double) max;
            }

            // Check if item is a block and has a full 1x1x1 collision shape
            boolean isFullBlock = false;
            Block block = Block.getBlockFromItem(item);
            if (block != null) {
                try {
                    isFullBlock = block.getDefaultState().getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).getBoundingBox().equals(new net.minecraft.util.math.Box(0, 0, 0, 1, 1, 1));
                } catch (Exception ignored) {
                }
            }

            // Check if item is food and get properties
            FoodComponent food = item.getFoodComponent();
            if (food != null) {
                int hunger = food.getHunger();
                float saturationModifier = food.getSaturationModifier();
                float saturationPoints = hunger * saturationModifier * 2.0f;
                double effectiveQuality = hunger + saturationPoints;//(saturationPoints > 0) ? hunger / saturationPoints : hunger;
                result.foodInfo.put(name, new FoodInfo(effectiveQuality, hunger, saturationPoints));
                System.out.println(name + ", " + hunger + ", " + saturationPoints);
            }

            // Handle specific items
            switch (name) {
                case "bow":
                    slotScore = 40 * durabilityMultiplier;
                    result.bestItemSlots.putIfAbsent("bow", i);
                    BEST_ITEM_SLOTS.put("bow", i);
                    break;
                case "arrow":
                case "spectral_arrow":
                    slotScore = 5 * count;
                    break;
                case "string":
                    slotScore = 3;
                    break;
                case "crafting_table":
                    slotScore = 80 + Math.sqrt(count) * 0.5;
                    result.bestItemSlots.putIfAbsent("crafting_table", i);
                    BEST_ITEM_SLOTS.put("crafting_table", i);
                    break;
                case "obsidian":
                case "leather":
                case "paper":
                case "book":
                case "lapis":
                    slotScore = 0; // Less valuable if enchantment table not needed
                    break;
                case "golden_apple":
                    slotScore = (20 + 50 * count);
                    result.bestItemSlots.putIfAbsent("golden_apple", i);
                    BEST_ITEM_SLOTS.put("golden_apple", i);
                    break;
                case "cobweb":
                    slotScore = 20 + 20 * Math.sqrt(count);
                    break;
                case "totem_of_undying":
                    slotScore = (110 * Math.pow(0.9, result.itemCounts.getOrDefault(name, 1)) + 5.5);
                    result.bestItemSlots.putIfAbsent("totem_of_undying", i);
                    BEST_ITEM_SLOTS.put("totem_of_undying", i);
                    break;
                case "pufferfish_bucket":
                    slotScore = 200 / (result.pufferfishBuckets * result.pufferfishBuckets);
                    result.pufferfishBuckets++;
                    result.waterBuckets++;
                    shouldReduce = false;
                    result.bestItemSlots.putIfAbsent("pufferfish_bucket", i);
                    BEST_ITEM_SLOTS.put("pufferfish_bucket", i);
                    break;
                case "lava_bucket":
                    slotScore = 1 + (200 / (result.lavaBuckets * result.lavaBuckets));
                    result.lavaBuckets++;
                    shouldReduce = false;
                    result.bestItemSlots.putIfAbsent("lava_bucket", i);
                    BEST_ITEM_SLOTS.put("lava_bucket", i);
                    break;
                case "flint_and_steel":
                    if (result.flintAndSteelCount == 0) {
                        slotScore += 30;
                        result.flintAndSteelCount++;
                    }
                    slotScore += 2;
                    slotScore *= durabilityMultiplier;
                    result.bestItemSlots.putIfAbsent("flint_and_steel", i);
                    BEST_ITEM_SLOTS.put("flint_and_steel", i);
                    break;
                case "shield":
                    slotScore = (500 * Math.pow(0.2, result.itemCounts.getOrDefault(name, 1)) + 5);
                    slotScore *= durabilityMultiplier;
                    result.bestItemSlots.putIfAbsent("shield", i);
                    BEST_ITEM_SLOTS.put("shield", i);
                    break;
                case "wooden_sword":
                    if (result.bestSword <= 1) {
                        slotScore += 100;
                        result.bestSword = 1;
                        result.bestItemSlots.put("sword", i);
                        BEST_ITEM_SLOTS.put("sword", i);
                    }
                    slotScore += 1;
                    slotScore *= durabilityMultiplier;
                    bestWeaponLevel.put("sword", Math.max(bestWeaponLevel.getOrDefault("sword", -1), 0));
                    bestSlotIndices[i] = 0;
                    break;
                case "golden_sword":
                    if (result.bestSword <= 2) {
                        slotScore += 100;
                        result.bestSword = 2;
                        result.bestItemSlots.put("sword", i);
                        BEST_ITEM_SLOTS.put("sword", i);
                    }
                    slotScore += 9;
                    slotScore *= durabilityMultiplier;
                    bestWeaponLevel.put("sword", Math.max(bestWeaponLevel.getOrDefault("sword", -1), 1));
                    bestSlotIndices[i] = 1;
                    break;
                case "stone_sword":
                    if (result.bestSword <= 3) {
                        slotScore += 100;
                        result.bestSword = 3;
                        result.bestItemSlots.put("sword", i);
                        BEST_ITEM_SLOTS.put("sword", i);
                    }
                    slotScore += 2;
                    slotScore *= durabilityMultiplier;
                    bestWeaponLevel.put("sword", Math.max(bestWeaponLevel.getOrDefault("sword", -1), 2));
                    bestSlotIndices[i] = 2;
                    break;
                case "iron_sword":
                    if (result.bestSword <= 4) {
                        slotScore += 100;
                        result.bestSword = 4;
                        result.bestItemSlots.put("sword", i);
                        BEST_ITEM_SLOTS.put("sword", i);
                    }
                    slotScore += 10;
                    slotScore *= durabilityMultiplier;
                    bestWeaponLevel.put("sword", Math.max(bestWeaponLevel.getOrDefault("sword", -1), 3));
                    bestSlotIndices[i] = 3;
                    break;
                case "diamond_sword":
                    if (result.bestSword <= 5) {
                        slotScore += 100;
                        result.bestSword = 5;
                        result.bestItemSlots.put("sword", i);
                        BEST_ITEM_SLOTS.put("sword", i);
                    }
                    slotScore += 11;
                    slotScore *= durabilityMultiplier;
                    bestWeaponLevel.put("sword", Math.max(bestWeaponLevel.getOrDefault("sword", -1), 4));
                    bestSlotIndices[i] = 4;
                    break;
                case "netherite_sword":
                    if (result.bestSword <= 6) {
                        slotScore += 100;
                        result.bestSword = 6;
                        result.bestItemSlots.put("sword", i);
                        BEST_ITEM_SLOTS.put("sword", i);
                    }
                    slotScore += 12;
                    slotScore *= durabilityMultiplier;
                    bestWeaponLevel.put("sword", Math.max(bestWeaponLevel.getOrDefault("sword", -1), 5));
                    bestSlotIndices[i] = 5;
                    break;
                case "wooden_axe":
                    if (result.bestAxe <= 1) {
                        slotScore += 105;
                        result.bestAxe = 1;
                        result.bestItemSlots.put("axe", i);
                        BEST_ITEM_SLOTS.put("axe", i);
                    }
                    slotScore += 4;
                    slotScore *= durabilityMultiplier;
                    bestToolLevel.put("axe", Math.max(bestToolLevel.getOrDefault("axe", -1), 0));
                    bestSlotIndices[i] = 0;
                    break;
                case "golden_axe":
                    if (result.bestAxe <= 2) {
                        slotScore += 105;
                        result.bestAxe = 2;
                        result.bestItemSlots.put("axe", i);
                        BEST_ITEM_SLOTS.put("axe", i);
                    }
                    slotScore += 5;
                    slotScore *= durabilityMultiplier;
                    bestToolLevel.put("axe", Math.max(bestToolLevel.getOrDefault("axe", -1), 1));
                    bestSlotIndices[i] = 1;
                    break;
                case "stone_axe":
                    if (result.bestAxe <= 3) {
                        slotScore += 105;
                        result.bestAxe = 3;
                        result.bestItemSlots.put("axe", i);
                        BEST_ITEM_SLOTS.put("axe", i);
                    }
                    slotScore += 5;
                    slotScore *= durabilityMultiplier;
                    bestToolLevel.put("axe", Math.max(bestToolLevel.getOrDefault("axe", -1), 2));
                    bestSlotIndices[i] = 2;
                    break;
                case "iron_axe":
                    if (result.bestAxe <= 4) {
                        slotScore += 105;
                        result.bestAxe = 4;
                        result.bestItemSlots.put("axe", i);
                        BEST_ITEM_SLOTS.put("axe", i);
                    }
                    slotScore += 6;
                    slotScore *= durabilityMultiplier;
                    bestToolLevel.put("axe", Math.max(bestToolLevel.getOrDefault("axe", -1), 3));
                    bestSlotIndices[i] = 3;
                    break;
                case "diamond_axe":
                    if (result.bestAxe <= 5) {
                        slotScore += 105;
                        result.bestAxe = 5;
                        result.bestItemSlots.put("axe", i);
                        BEST_ITEM_SLOTS.put("axe", i);
                    }
                    slotScore += 7;
                    slotScore *= durabilityMultiplier;
                    bestToolLevel.put("axe", Math.max(bestToolLevel.getOrDefault("axe", -1), 4));
                    bestSlotIndices[i] = 4;
                    break;
                case "netherite_axe":
                    if (result.bestAxe <= 6) {
                        slotScore += 105;
                        result.bestAxe = 6;
                        result.bestItemSlots.put("axe", i);
                        BEST_ITEM_SLOTS.put("axe", i);
                    }
                    slotScore += 8;
                    slotScore *= durabilityMultiplier;
                    bestToolLevel.put("axe", Math.max(bestToolLevel.getOrDefault("axe", -1), 5));
                    bestSlotIndices[i] = 5;
                    break;
                case "wooden_pickaxe":
                    if (result.bestPickaxe <= 1) {
                        slotScore += 105;
                        result.bestPickaxe = 1;
                        result.bestItemSlots.put("pickaxe", i);
                        BEST_ITEM_SLOTS.put("pickaxe", i);
                    }
                    slotScore += 4;
                    slotScore *= durabilityMultiplier;
                    bestToolLevel.put("pickaxe", Math.max(bestToolLevel.getOrDefault("pickaxe", -1), 0));
                    bestSlotIndices[i] = 0;
                    break;
                case "golden_pickaxe":
                    if (result.bestPickaxe <= 2) {
                        slotScore += 105;
                        result.bestPickaxe = 2;
                        result.bestItemSlots.put("pickaxe", i);
                        BEST_ITEM_SLOTS.put("pickaxe", i);
                    }
                    slotScore += 7;
                    slotScore *= durabilityMultiplier;
                    bestToolLevel.put("pickaxe", Math.max(bestToolLevel.getOrDefault("pickaxe", -1), 1));
                    bestSlotIndices[i] = 1;
                    break;
                case "stone_pickaxe":
                    if (result.bestPickaxe <= 3) {
                        slotScore += 105;
                        result.bestPickaxe = 3;
                        result.bestItemSlots.put("pickaxe", i);
                        BEST_ITEM_SLOTS.put("pickaxe", i);
                    }
                    slotScore += 5;
                    slotScore *= durabilityMultiplier;
                    bestToolLevel.put("pickaxe", Math.max(bestToolLevel.getOrDefault("pickaxe", -1), 2));
                    bestSlotIndices[i] = 2;
                    break;
                case "iron_pickaxe":
                    if (result.bestPickaxe <= 4) {
                        slotScore += 105;
                        result.bestPickaxe = 4;
                        result.bestItemSlots.put("pickaxe", i);
                        BEST_ITEM_SLOTS.put("pickaxe", i);
                    }
                    slotScore += 8;
                    slotScore *= durabilityMultiplier;
                    bestToolLevel.put("pickaxe", Math.max(bestToolLevel.getOrDefault("pickaxe", -1), 3));
                    bestSlotIndices[i] = 3;
                    break;
                case "diamond_pickaxe":
                    if (result.bestPickaxe <= 5) {
                        slotScore += 105;
                        result.bestPickaxe = 5;
                        result.bestItemSlots.put("pickaxe", i);
                        BEST_ITEM_SLOTS.put("pickaxe", i);
                    }
                    slotScore += 10;
                    slotScore *= durabilityMultiplier;
                    bestToolLevel.put("pickaxe", Math.max(bestToolLevel.getOrDefault("pickaxe", -1), 4));
                    bestSlotIndices[i] = 4;
                    break;
                case "netherite_pickaxe":
                    if (result.bestPickaxe <= 6) {
                        slotScore += 105;
                        result.bestPickaxe = 6;
                        result.bestItemSlots.put("pickaxe", i);
                        BEST_ITEM_SLOTS.put("pickaxe", i);
                    }
                    slotScore += 12;
                    slotScore *= durabilityMultiplier;
                    bestToolLevel.put("pickaxe", Math.max(bestToolLevel.getOrDefault("pickaxe", -1), 5));
                    bestSlotIndices[i] = 5;
                    break;
                default:
                    // Handle other tools (shovel, hoe) from original Java code
                    for (String tool : TOOL_BONUSES.keySet()) {
                        if (name.endsWith("_" + tool)) {
                            String level = name.replace("_" + tool, "");
                            int tier = TOOL_LEVELS.indexOf(level);
                            if (tier != -1) {
                                int baseScore = 3 * (tier + 1);
                                slotScore = baseScore * durabilityMultiplier;
                                bestToolLevel.put(tool, Math.max(bestToolLevel.getOrDefault(tool, -1), tier));
                                bestSlotIndices[i] = tier;
                                result.bestItemSlots.put(tool, i);
                                BEST_ITEM_SLOTS.put(tool, i);
                            }
                        }
                    }
                    // Handle armor from original Java code
                    if (name.contains("_helmet") || name.contains("_chestplate") || name.contains("_leggings") || name.contains("_boots")) {
                        for (String armorType : ARMOR_LEVELS) {
                            if (name.startsWith(armorType)) {
                                int tier = ARMOR_LEVELS.indexOf(armorType);
                                int baseScore = (tier + 1) * 10;
                                slotScore = baseScore * durabilityMultiplier;
                                bestArmorLevel.put(name, Math.max(bestArmorLevel.getOrDefault(name, -1), tier));
                                bestSlotIndices[i] = tier;
                                result.bestItemSlots.put(name, i);
                                BEST_ITEM_SLOTS.put(name, i);
                            }
                        }
                    }
                    // Handle buckets, boats, food, and blocks
                    if (slotScore == 0) {
                        if (name.contains("_bucket") && !name.equals("pufferfish_bucket") && !name.equals("lava_bucket")) {
                            slotScore = (result.pufferfishBuckets != result.waterBuckets) ?
                                    200 / (result.waterBuckets * result.waterBuckets) : 150;
                            result.waterBuckets++;
                            shouldReduce = false;
                            result.bestItemSlots.putIfAbsent("bucket", i);
                            BEST_ITEM_SLOTS.put("bucket", i);
                        } else if (name.contains("_boat") || name.contains("_raft")) {
                            result.boatCount++;
                            slotScore = (result.boatCount < 3) ?
                                    (50 - result.boatCount * result.boatCount * 8) :
                                    (Math.sqrt(result.boatCount) - Math.sqrt(result.boatCount - 1)) * 10;
                            shouldReduce = false;
                            result.bestItemSlots.putIfAbsent("boat", i);
                            BEST_ITEM_SLOTS.put("boat", i);
                        } else if (result.foodInfo.containsKey(name)) {
                            slotScore = Math.sqrt(count) * result.foodInfo.get(name).effectiveQuality;
                            System.out.println(name + " x" + count + " score: " + slotScore + " eQ: " + result.foodInfo.get(name).effectiveQuality);
                            if (name.equals("rotten_flesh")) {
                                slotScore /= 3;
                            } else if (name.equals("spider_eye")) {
                                slotScore /= 10;
                            }
                        } else if (isFullBlock) {
                            result.blockStacks++;
                            slotScore = count / (double) result.blockStacks;
                        }
                        // Quantity-based value for unknown items
                        slotScore += (count / 10.0);
                        if (name.contains("_door")) {
                            slotScore *= 10;
                            slotScore += 10;
                        }
                    }
                    break;
            }

            // Apply reduction if necessary
            if (shouldReduce && result.itemCounts.getOrDefault(name, 1) > 0) {
                slotScore /= result.itemCounts.get(name);
            }

            result.slotScores[i] = slotScore;
            result.totalScore += slotScore;
        }

        // Add bonuses for best items
        for (Map.Entry<String, Integer> entry : bestWeaponLevel.entrySet()) {
            if (entry.getValue() == WEAPON_LEVELS.size() - 1) {
                result.totalScore += WEAPON_BONUSES.getOrDefault(entry.getKey(), 0);
            }
        }

        for (Map.Entry<String, Integer> entry : bestToolLevel.entrySet()) {
            if (entry.getValue() == TOOL_LEVELS.size() - 1) {
                result.totalScore += TOOL_BONUSES.getOrDefault(entry.getKey(), 0);
            }
        }

        return result;
    }

    // Getter for accessing the last known best item slots
    public static Map<String, Integer> getBestItemSlots() {
        return new HashMap<>(BEST_ITEM_SLOTS);
    }
}

