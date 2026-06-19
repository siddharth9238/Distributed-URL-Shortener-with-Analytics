package com.siddharth.urlshortener.service;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * LRUCache: Fixed-capacity cache with Least Recently Used eviction policy.
 * Uses HashMap for O(1) lookup + doubly-linked list for O(1) eviction.
 * This is the interview-defensible algorithm piece (LeetCode 146).
 */
@Service
public class LRUCache {

    private static class Node {
        String key;
        String value;
        Node prev;
        Node next;

        Node(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    @Setter
    @Value("${app.cache.max-size:10000}")
    private int maxCapacity;

    private final Map<String, Node> cache = new HashMap<>();
    private Node head;
    private Node tail;

    public LRUCache() {
        this.maxCapacity = 10000;
        this.head = new Node(null, null);
        this.tail = new Node(null, null);
        head.next = tail;
        tail.prev = head;
    }

    public synchronized String get(String key) {
        if (!cache.containsKey(key)) {
            return null;
        }

        Node node = cache.get(key);
        moveToHead(node);
        return node.value;
    }

    public synchronized void put(String key, String value) {
        if (cache.containsKey(key)) {
            Node node = cache.get(key);
            node.value = value;
            moveToHead(node);
            return;
        }

        Node newNode = new Node(key, value);
        cache.put(key, newNode);
        addToHead(newNode);

        if (cache.size() > maxCapacity) {
            Node removed = removeTail();
            cache.remove(removed.key);
        }
    }

    public synchronized boolean containsKey(String key) {
        return cache.containsKey(key);
    }

    public synchronized void clear() {
        cache.clear();
        head.next = tail;
        tail.prev = head;
    }

    public synchronized int size() {
        return cache.size();
    }

    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }

    private void addToHead(Node node) {
        node.next = head.next;
        node.prev = head;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private Node removeTail() {
        Node node = tail.prev;
        removeNode(node);
        return node;
    }
}
