package com.jedk1.jedcore.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Thread-safe utilities for managing concurrent operations in Folia environments
 */
public class ThreadSafeUtils {
    
    /**
     * Thread-safe boolean flag
     */
    public static class SafeBoolean {
        private final AtomicBoolean value = new AtomicBoolean(false);
        
        public boolean get() {
            return value.get();
        }
        
        public void set(boolean newValue) {
            value.set(newValue);
        }
        
        public boolean compareAndSet(boolean expect, boolean update) {
            return value.compareAndSet(expect, update);
        }
        
        public boolean getAndSet(boolean newValue) {
            return value.getAndSet(newValue);
        }
    }
    
    /**
     * Thread-safe integer counter
     */
    public static class SafeInteger {
        private final AtomicInteger value = new AtomicInteger(0);
        
        public int get() {
            return value.get();
        }
        
        public void set(int newValue) {
            value.set(newValue);
        }
        
        public int incrementAndGet() {
            return value.incrementAndGet();
        }
        
        public int decrementAndGet() {
            return value.decrementAndGet();
        }
        
        public int addAndGet(int delta) {
            return value.addAndGet(delta);
        }
        
        public boolean compareAndSet(int expect, int update) {
            return value.compareAndSet(expect, update);
        }
    }
    
    /**
     * Thread-safe long counter
     */
    public static class SafeLong {
        private final AtomicLong value = new AtomicLong(0L);
        
        public long get() {
            return value.get();
        }
        
        public void set(long newValue) {
            value.set(newValue);
        }
        
        public long incrementAndGet() {
            return value.incrementAndGet();
        }
        
        public long decrementAndGet() {
            return value.decrementAndGet();
        }
        
        public long addAndGet(long delta) {
            return value.addAndGet(delta);
        }
        
        public boolean compareAndSet(long expect, long update) {
            return value.compareAndSet(expect, update);
        }
    }
    
    /**
     * Thread-safe reference holder
     */
    public static class SafeReference<T> {
        private final AtomicReference<T> value = new AtomicReference<>();
        
        public T get() {
            return value.get();
        }
        
        public void set(T newValue) {
            value.set(newValue);
        }
        
        public T getAndSet(T newValue) {
            return value.getAndSet(newValue);
        }
        
        public boolean compareAndSet(T expect, T update) {
            return value.compareAndSet(expect, update);
        }
    }
    
    /**
     * Thread-safe location-based cache
     */
    public static class LocationCache<T> {
        private final ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>();
        
        public T get(Location location, Supplier<T> supplier) {
            String key = locationToString(location);
            return cache.computeIfAbsent(key, k -> supplier.get());
        }
        
        public T get(Location location) {
            String key = locationToString(location);
            return cache.get(key);
        }
        
        public void put(Location location, T value) {
            String key = locationToString(location);
            cache.put(key, value);
        }
        
        public void remove(Location location) {
            String key = locationToString(location);
            cache.remove(key);
        }
        
        public void clear() {
            cache.clear();
        }
        
        private String locationToString(Location location) {
            return location.getWorld().getName() + "," + 
                   location.getBlockX() + "," + 
                   location.getBlockY() + "," + 
                   location.getBlockZ();
        }
    }
    
    /**
     * Thread-safe world-based cache
     */
    public static class WorldCache<T> {
        private final ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>();
        
        public T get(World world, Supplier<T> supplier) {
            return cache.computeIfAbsent(world.getName(), k -> supplier.get());
        }
        
        public T get(World world) {
            return cache.get(world.getName());
        }
        
        public void put(World world, T value) {
            cache.put(world.getName(), value);
        }
        
        public void remove(World world) {
            cache.remove(world.getName());
        }
        
        public void clear() {
            cache.clear();
        }
    }
    
    /**
     * Thread-safe player-based cache
     */
    public static class PlayerCache<T> {
        private final ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>();
        
        public T get(Player player, Supplier<T> supplier) {
            return cache.computeIfAbsent(player.getUniqueId().toString(), k -> supplier.get());
        }
        
        public T get(Player player) {
            return cache.get(player.getUniqueId().toString());
        }
        
        public void put(Player player, T value) {
            cache.put(player.getUniqueId().toString(), value);
        }
        
        public void remove(Player player) {
            cache.remove(player.getUniqueId().toString());
        }
        
        public void clear() {
            cache.clear();
        }
    }
    
    /**
     * Thread-safe task queue for processing tasks on the main thread
     */
    public static class MainThreadQueue {
        private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
        private final AtomicBoolean isProcessing = new AtomicBoolean(false);
        
        public void addTask(Runnable task) {
            taskQueue.offer(task);
            processTasks();
        }
        
        private void processTasks() {
            if (isProcessing.compareAndSet(false, true)) {
                try {
                    Runnable task;
                    while ((task = taskQueue.poll()) != null) {
                        try {
                            task.run();
                        } catch (Exception e) {
                            // Log task processing errors silently
                        }
                    }
                } finally {
                    isProcessing.set(false);
                    // Check if more tasks were added while processing
                    if (!taskQueue.isEmpty()) {
                        processTasks();
                    }
                }
            }
        }
        
        public int getQueueSize() {
            return taskQueue.size();
        }
        
        public boolean isEmpty() {
            return taskQueue.isEmpty();
        }
    }
    
    /**
     * Thread-safe rate limiter
     */
    public static class RateLimiter {
        private final AtomicLong lastExecution = new AtomicLong(0);
        private final long minInterval;
        
        public RateLimiter(long minIntervalMs) {
            this.minInterval = minIntervalMs;
        }
        
        public boolean canExecute() {
            long now = System.currentTimeMillis();
            long last = lastExecution.get();
            
            if (now - last >= minInterval) {
                return lastExecution.compareAndSet(last, now);
            }
            return false;
        }
        
        public void execute(Runnable task) {
            if (canExecute()) {
                task.run();
            }
        }
        
        public long getTimeUntilNextExecution() {
            long now = System.currentTimeMillis();
            long last = lastExecution.get();
            long timeSinceLast = now - last;
            
            if (timeSinceLast >= minInterval) {
                return 0;
            }
            return minInterval - timeSinceLast;
        }
    }
    
    /**
     * Thread-safe circular buffer
     */
    public static class CircularBuffer<T> {
        private final T[] buffer;
        private final AtomicInteger head = new AtomicInteger(0);
        private final AtomicInteger tail = new AtomicInteger(0);
        private final AtomicInteger size = new AtomicInteger(0);
        private final int capacity;
        
        @SuppressWarnings("unchecked")
        public CircularBuffer(int capacity) {
            this.capacity = capacity;
            this.buffer = (T[]) new Object[capacity];
        }
        
        public boolean offer(T element) {
            if (size.get() >= capacity) {
                return false;
            }
            
            int currentTail = tail.get();
            int nextTail = (currentTail + 1) % capacity;
            
            if (tail.compareAndSet(currentTail, nextTail)) {
                buffer[currentTail] = element;
                size.incrementAndGet();
                return true;
            }
            return false;
        }
        
        public T poll() {
            if (size.get() <= 0) {
                return null;
            }
            
            int currentHead = head.get();
            int nextHead = (currentHead + 1) % capacity;
            
            if (head.compareAndSet(currentHead, nextHead)) {
                T element = buffer[currentHead];
                buffer[currentHead] = null;
                size.decrementAndGet();
                return element;
            }
            return null;
        }
        
        public int size() {
            return size.get();
        }
        
        public boolean isEmpty() {
            return size.get() == 0;
        }
        
        public boolean isFull() {
            return size.get() >= capacity;
        }
        
        public void clear() {
            head.set(0);
            tail.set(0);
            size.set(0);
            for (int i = 0; i < capacity; i++) {
                buffer[i] = null;
            }
        }
    }
}
