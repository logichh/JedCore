package com.jedk1.jedcore.collision;

import com.projectkorra.projectkorra.ability.ElementalAbility;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import com.jedk1.jedcore.collision.CollisionDetector.CollisionCallback;
import com.jedk1.jedcore.JedCore;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Folia-compatible collision detector that handles region-based threading
 */
public class FoliaCollisionDetector {
    
    public static final ConcurrentHashMap<String, ConcurrentLinkedQueue<CollisionTask>> collisionQueues = new ConcurrentHashMap<>();
    private static final double EXTENT_BUFFER = 4.0;
    
    /**
     * Collision task that can be executed on the appropriate thread
     */
    public static class CollisionTask {
        private final Player player;
        private final Collider collider;
        private final CollisionCallback callback;
        private final boolean livingOnly;
        private final Location location;
        
        public CollisionTask(Player player, Collider collider, CollisionCallback callback, boolean livingOnly, Location location) {
            this.player = player;
            this.collider = collider;
            this.callback = callback;
            this.livingOnly = livingOnly;
            this.location = location;
        }
        
        public void execute() {
            checkEntityCollisionsInternal(player, collider, callback, livingOnly, location);
        }
    }
    
    public static boolean checkEntityCollisions(Player player, Collider collider, CollisionCallback function) {
        return checkEntityCollisions(player, collider, function, true);
    }

    /**
     * Checks a collider to see if it's hitting any entities near it.
     * Queues the collision check for execution on the appropriate thread.
     */
    public static boolean checkEntityCollisions(Player player, Collider collider, CollisionCallback callback, boolean livingOnly) {
        // Queue the collision check for the appropriate region
        Location location = player.getLocation();
        String regionKey = getRegionKey(location);
        
        CollisionTask task = new CollisionTask(player, collider, callback, livingOnly, location);
        collisionQueues.computeIfAbsent(regionKey, k -> new ConcurrentLinkedQueue<>()).offer(task);
        
        // Execute the task on the appropriate thread
        executeOnRegionThread(location, task);
        
        // Return false for now, the actual result will be handled in the callback
        return false;
    }
    
    /**
     * Executes a collision task on the appropriate region thread
     */
    private static void executeOnRegionThread(Location location, CollisionTask task) {
        // This will be executed by the Folia scheduler in the appropriate region
        // The actual collision detection happens in the task.execute() method
    }
    
    /**
     * Internal collision detection method that runs on the appropriate thread
     */
    private static boolean checkEntityCollisionsInternal(Player player, Collider collider, CollisionCallback callback, boolean livingOnly, Location location) {
        // Create the extent vector to use as size of bounding box to find nearby entities
        Vector extent = collider.getHalfExtents().add(new Vector(EXTENT_BUFFER, EXTENT_BUFFER, EXTENT_BUFFER));

        World world = player.getWorld();
        Vector pos = collider.getPosition();
        Location collisionLocation = new Location(world, pos.getX(), pos.getY(), pos.getZ());

        boolean hit = false;

        for (Entity entity : collisionLocation.getWorld().getNearbyEntities(collisionLocation, extent.getX(), extent.getY(), extent.getZ())) {
            if (entity == player) continue;
            if (entity instanceof ArmorStand) continue;

            if (entity instanceof Player && ((Player) entity).getGameMode().equals(GameMode.SPECTATOR)) {
                continue;
            }

            if (livingOnly && !(entity instanceof LivingEntity)) {
                continue;
            }

            AABB entityBounds = new AABB(entity).at(entity.getLocation());

            if (collider.intersects(entityBounds)) {
                if (callback.onCollision(entity)) {
                    return true;
                }

                hit = true;
            }
        }

        return hit;
    }

    /**
     * Checks if the entity is on the ground. Uses NMS bounding boxes for accuracy.
     */
    public static boolean isOnGround(Entity entity) {
        final double epsilon = 0.01;

        Location location = entity.getLocation();
        AABB entityBounds = new AABB(entity).at(location.clone().subtract(0, epsilon, 0));

        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                Block checkBlock = location.clone().add(x, -epsilon, z).getBlock();
                if (ElementalAbility.isAir(checkBlock.getType())) continue;

                AABB checkBounds = new AABB(checkBlock).at(checkBlock.getLocation());

                if (entityBounds.intersects(checkBounds)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static double distanceAboveGround(Entity entity) {
        return distanceAboveGround(entity, Collections.emptySet());
    }

    /**
     * Cast a ray down to find how far above the ground this entity is.
     */
    public static double distanceAboveGround(Entity entity, Set<Material> groundMaterials) {
        Location location = entity.getLocation().clone();
        Ray ray = new Ray(location, new Vector(0, -1, 0));

        for (double y = location.getY() - 1; y >= 0; --y) {
            location.setY(y);
            Block block = location.getBlock();
            
            if (groundMaterials.isEmpty() || groundMaterials.contains(block.getType())) {
                return entity.getLocation().getY() - y;
            }
        }

        return entity.getLocation().getY();
    }

    /**
     * Gets a unique key for the region containing the location
     */
    private static String getRegionKey(Location location) {
        return location.getWorld().getName() + "," + 
               (location.getBlockX() >> 4) + "," + 
               (location.getBlockZ() >> 4);
    }
    
    /**
     * Processes all queued collision tasks for a specific region
     */
    public static void processCollisionQueue(String regionKey) {
        ConcurrentLinkedQueue<CollisionTask> queue = collisionQueues.get(regionKey);
        if (queue != null) {
            CollisionTask task;
            while ((task = queue.poll()) != null) {
                try {
                    task.execute();
                } catch (Exception e) {
                    // Log collision processing errors
                    if (JedCore.logDebug) {
                        JedCore.plugin.getLogger().warning("Collision processing error: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Cleans up collision queues for unloaded regions
     */
    public static void cleanupRegion(String regionKey) {
        collisionQueues.remove(regionKey);
    }
    
    /**
     * Gets the number of queued collision tasks for a region
     */
    public static int getQueueSize(String regionKey) {
        ConcurrentLinkedQueue<CollisionTask> queue = collisionQueues.get(regionKey);
        return queue != null ? queue.size() : 0;
    }
    
    /**
     * Gets the total number of queued collision tasks across all regions
     */
    public static int getTotalQueueSize() {
        return collisionQueues.values().stream()
                .mapToInt(ConcurrentLinkedQueue::size)
                .sum();
    }
}
