package com.siddharth.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    private Base62Encoder base62Encoder;

    @BeforeEach
    void setUp() {
        base62Encoder = new Base62Encoder();
    }

    @Test
    void testEncode() {
        // Test encoding
        String encoded = base62Encoder.encode(1L);
        assertEquals("1", encoded);

        encoded = base62Encoder.encode(10L);
        assertEquals("a", encoded);

        encoded = base62Encoder.encode(100L);
        assertEquals("1C", encoded);
    }

    @Test
    void testDecode() {
        // Test decoding
        long decoded = base62Encoder.decode("1");
        assertEquals(1L, decoded);

        decoded = base62Encoder.decode("a");
        assertEquals(10L, decoded);

        decoded = base62Encoder.decode("1C");
        assertEquals(100L, decoded);
    }

    @Test
    void testEncodeDecodeRoundTrip() {
        // Test round trip
        long[] testIds = {1L, 10L, 100L, 1000L, 123456L, 999999L};

        for (long id : testIds) {
            String encoded = base62Encoder.encode(id);
            long decoded = base62Encoder.decode(encoded);
            assertEquals(id, decoded, "Round trip failed for ID: " + id);
        }
    }

    @Test
    void testInvalidEncodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> base62Encoder.encode(0L));
        assertThrows(IllegalArgumentException.class, () -> base62Encoder.encode(-1L));
    }

    @Test
    void testInvalidDecodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> base62Encoder.decode(null));
        assertThrows(IllegalArgumentException.class, () -> base62Encoder.decode(""));
        assertThrows(IllegalArgumentException.class, () -> base62Encoder.decode("!@#"));
    }
}
