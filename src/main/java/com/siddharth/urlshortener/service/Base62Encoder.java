package com.siddharth.urlshortener.service;

import org.springframework.stereotype.Service;

/**
 * Base62Encoder: Converts between long IDs and short alphanumeric codes.
 * Uses 0-9a-zA-Z character set (62 characters total).
 * This is the interview-defensible algorithm piece for number base conversion.
 */
@Service
public class Base62Encoder {

    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;
    private static final long MIN_ID = 1L;

    /**
     * Encodes a long ID into a base62 string.
     * @param id The numeric ID to encode
     * @return A base62-encoded string
     */
    public String encode(long id) {
        if (id < MIN_ID) {
            throw new IllegalArgumentException("ID must be >= " + MIN_ID);
        }

        StringBuilder result = new StringBuilder();
        while (id > 0) {
            result.append(BASE62_CHARS.charAt((int) (id % BASE)));
            id /= BASE;
        }
        return result.reverse().toString();
    }

    /**
     * Decodes a base62 string back into a long ID.
     * @param code The base62-encoded string to decode
     * @return The numeric ID
     */
    public long decode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("Code cannot be null or empty");
        }

        long result = 0;
        for (char c : code.toCharArray()) {
            int digit = BASE62_CHARS.indexOf(c);
            if (digit == -1) {
                throw new IllegalArgumentException("Invalid character in code: " + c);
            }
            result = result * BASE + digit;
        }
        return result;
    }
}
