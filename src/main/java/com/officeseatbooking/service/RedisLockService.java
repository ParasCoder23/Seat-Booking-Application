package com.officeseatbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
public class RedisLockService {

    private static final Logger logger = LoggerFactory.getLogger(RedisLockService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String LOCK_PREFIX = "seat_lock:";
    private static final long LOCK_TIMEOUT = 2; // 2 minutes

    // Lua script for atomic lock acquisition with TTL
    private static final String ACQUIRE_LOCK_SCRIPT =
        "if redis.call('EXISTS', KEYS[1]) == 0 then " +
        "  redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[2]) " +
        "  return 1 " +
        "else " +
        "  return 0 " +
        "end";

    // Lua script for safe lock release (only if owner)
    private static final String RELEASE_LOCK_SCRIPT =
        "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
        "  return redis.call('DEL', KEYS[1]) " +
        "else " +
        "  return 0 " +
        "end";

    public boolean acquireLock(Long seatId, String userId) {
        String lockKey = LOCK_PREFIX + seatId;
        String lockValue = userId + ":" + System.currentTimeMillis();

        try {
            logger.debug("Attempting to acquire lock for seat {} with key {} and value {}", seatId, lockKey, lockValue);

            // Try Lua script approach first (atomic)
            try {
                DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                script.setScriptText(ACQUIRE_LOCK_SCRIPT);
                script.setResultType(Long.class);

                // Convert TTL to seconds as integer, not string
                Long ttlSeconds = LOCK_TIMEOUT * 60; // Convert minutes to seconds

                Long result = redisTemplate.execute(script,
                    Collections.singletonList(lockKey),
                    lockValue,
                    ttlSeconds); // Pass as Long, not String

                boolean success = result != null && result.equals(1L);
                logger.debug("Lock acquisition result for seat {} (Lua script): {}", seatId, success);

                if (success) {
                    logger.info("Successfully acquired lock for seat {} by user {} using Lua script", seatId, userId);
                } else {
                    String currentOwner = getLockOwner(seatId);
                    logger.warn("Failed to acquire lock for seat {} by user {} using Lua script. Current owner: {}", seatId, userId, currentOwner);
                }

                return success;

            } catch (Exception luaException) {
                logger.warn("Lua script failed for seat {}, falling back to simple Redis operations: {}", seatId, luaException.getMessage());

                // Fallback to simple Redis operations
                Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, Duration.ofMinutes(LOCK_TIMEOUT));
                boolean result = Boolean.TRUE.equals(success);

                logger.debug("Lock acquisition result for seat {} (fallback): {}", seatId, result);

                if (result) {
                    logger.info("Successfully acquired lock for seat {} by user {} using fallback method", seatId, userId);
                } else {
                    String currentOwner = getLockOwner(seatId);
                    logger.warn("Failed to acquire lock for seat {} by user {} using fallback method. Current owner: {}", seatId, userId, currentOwner);
                }

                return result;
            }

        } catch (Exception e) {
            logger.error("Error acquiring lock for seat {}: ", seatId, e);
            return false;
        }
    }

    public boolean releaseLock(Long seatId, String userId) {
        String lockKey = LOCK_PREFIX + seatId;
        String expectedValue = userId + ":";

        try {
            logger.debug("Attempting to release lock for seat {} with key {}", seatId, lockKey);

            // Get the full lock value to match exactly
            String lockValue = (String) redisTemplate.opsForValue().get(lockKey);
            logger.debug("Current lock value for seat {}: {}", seatId, lockValue);

            if (lockValue != null && lockValue.startsWith(expectedValue)) {
                try {
                    // Try Lua script approach first (atomic)
                    DefaultRedisScript<Long> script = new DefaultRedisScript<>();
                    script.setScriptText(RELEASE_LOCK_SCRIPT);
                    script.setResultType(Long.class);

                    Long result = redisTemplate.execute(script,
                        Collections.singletonList(lockKey),
                        lockValue);

                    boolean released = result != null && result.equals(1L);
                    logger.debug("Lock release result for seat {} (Lua script): {}", seatId, released);

                    if (released) {
                        logger.info("Successfully released lock for seat {} by user {} using Lua script", seatId, userId);
                    } else {
                        logger.warn("Failed to release lock for seat {} by user {} using Lua script - lock may have expired", seatId, userId);
                    }
                    return released;

                } catch (Exception luaException) {
                    logger.warn("Lua script failed for releasing lock on seat {}, falling back to simple delete: {}", seatId, luaException.getMessage());

                    // Fallback to simple delete (less safe but works)
                    redisTemplate.delete(lockKey);
                    logger.info("Released lock for seat {} by user {} using fallback method", seatId, userId);
                    return true;
                }
            } else {
                logger.warn("Cannot release lock for seat {} - lock value mismatch. Expected prefix: {}, Actual: {}",
                           seatId, expectedValue, lockValue);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error releasing lock for seat {}: ", seatId, e);
            return false;
        }
    }

    public boolean isLocked(Long seatId) {
        String lockKey = LOCK_PREFIX + seatId;
        try {
            Boolean result = redisTemplate.hasKey(lockKey);
            boolean isLocked = Boolean.TRUE.equals(result);
            logger.debug("Lock status check for seat {}: {}", seatId, isLocked);
            return isLocked;
        } catch (Exception e) {
            logger.error("Error checking lock status for seat {}: ", seatId, e);
            return false;
        }
    }

    public String getLockOwner(Long seatId) {
        String lockKey = LOCK_PREFIX + seatId;
        try {
            String lockValue = (String) redisTemplate.opsForValue().get(lockKey);
            logger.debug("Getting lock owner for seat {}: {}", seatId, lockValue);

            if (lockValue != null && lockValue.contains(":")) {
                String owner = lockValue.split(":")[0];
                logger.debug("Lock owner for seat {}: {}", seatId, owner);
                return owner;
            }
            logger.debug("No lock owner found for seat {}", seatId);
            return null;
        } catch (Exception e) {
            logger.error("Error getting lock owner for seat {}: ", seatId, e);
            return null;
        }
    }

    public boolean extendLock(Long seatId, String userId) {
        String lockKey = LOCK_PREFIX + seatId;
        String expectedPrefix = userId + ":";

        try {
            String lockValue = (String) redisTemplate.opsForValue().get(lockKey);
            if (lockValue != null && lockValue.startsWith(expectedPrefix)) {
                String newLockValue = userId + ":" + System.currentTimeMillis();
                redisTemplate.opsForValue().set(lockKey, newLockValue, Duration.ofMinutes(LOCK_TIMEOUT));
                logger.debug("Lock extended for seat {} with new value {}", seatId, newLockValue);
                return true;
            } else {
                logger.warn("Cannot extend lock for seat {} - lock value mismatch", seatId);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error extending lock for seat {}: ", seatId, e);
            return false;
        }
    }

    public long getLockRemainingTime(Long seatId) {
        String lockKey = LOCK_PREFIX + seatId;
        try {
            Long ttl = redisTemplate.getExpire(lockKey);
            return ttl != null ? ttl : -1;
        } catch (Exception e) {
            logger.error("Error getting lock remaining time for seat {}: ", seatId, e);
            return -1;
        }
    }
}
