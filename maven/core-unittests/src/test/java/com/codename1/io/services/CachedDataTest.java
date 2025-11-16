package com.codename1.io.services;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CachedDataTest extends UITestBase {

    @FormTest
    void testCachedDataSerialization() throws IOException {
        CachedData cached = new CachedData();
        byte[] data = new byte[]{1, 2, 3, 4};
        cached.setData(data);
        cached.setEtag("tag");
        cached.setModified("now");
        cached.setUrl("http://example.com");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(bout);
        cached.externalize(dout);

        CachedData restored = new CachedData();
        restored.internalize(1, new DataInputStream(new ByteArrayInputStream(bout.toByteArray())));
        assertEquals("http://example.com", restored.getUrl());
        assertEquals("tag", restored.getEtag());
        assertEquals("now", restored.getModified());
        assertArrayEquals(data, restored.getData());
    }

    @FormTest
    void testFetchingFlagCanBeToggled() {
        CachedData cached = new CachedData();
        assertFalse(cached.isFetching());
        cached.setFetching(true);
        assertTrue(cached.isFetching());
        cached.setFetching(false);
        assertFalse(cached.isFetching());
    }
}
