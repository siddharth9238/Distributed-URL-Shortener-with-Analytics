package com.siddharth.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LRUCacheTest {

    private LRUCache lruCache;

    @BeforeEach
    void setUp() {
        lruCache = new LRUCache();
        lruCache.setMaxCapacity(3);
    }

    @Test
    void testPutAndGet() {
        lruCache.put("key1", "value1");
        assertEquals("value1", lruCache.get("key1"));
    }

    @Test
    void testGetNonExistentKey() {
        assertNull(lruCache.get("nonexistent"));
    }

    @Test
    void testLRUEviction() {
        // Fill cache to capacity
        lruCache.put("key1", "value1");
        lruCache.put("key2", "value2");
        lruCache.put("key3", "value3");

        assertEquals(3, lruCache.size());

        // Access key1 to make it most recently used
        lruCache.get("key1");

        // Add new key, should evict key2 (least recently used)
        lruCache.put("key4", "value4");

        assertEquals(3, lruCache.size());
        assertNull(lruCache.get("key2"));
        assertNotNull(lruCache.get("key1"));
        assertNotNull(lruCache.get("key3"));
        assertNotNull(lruCache.get("key4"));
    }

    @Test
    void testUpdateExistingKey() {
        lruCache.put("key1", "value1");
        assertEquals("value1", lruCache.get("key1"));

        lruCache.put("key1", "updated_value");
        assertEquals("updated_value", lruCache.get("key1"));
        assertEquals(1, lruCache.size());
    }

    @Test
    void testClear() {
        lruCache.put("key1", "value1");
        lruCache.put("key2", "value2");
        lruCache.clear();

        assertEquals(0, lruCache.size());
        assertNull(lruCache.get("key1"));
        assertNull(lruCache.get("key2"));
    }

    @Test
    void testContainsKey() {
        lruCache.put("key1", "value1");

        assertTrue(lruCache.containsKey("key1"));
        assertFalse(lruCache.containsKey("key2"));
    }

    @Test
    void testCacheHitIncreaseRecency() {
        lruCache.put("key1", "value1");
        lruCache.put("key2", "value2");
        lruCache.put("key3", "value3");

        // Access key1 to make it recently used
        lruCache.get("key1");

        // Add key4, should evict key2 (not key1)
        lruCache.put("key4", "value4");

        assertNotNull(lruCache.get("key1"));
        assertNull(lruCache.get("key2"));
    }
}
