/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend.messaging.kafka;

import com.github.luben.zstd.ZstdOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FrameOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xerial.snappy.SnappyOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/// The transport's own snappy, lz4 and zstd decoders against the reference
/// compressors, over inputs big and varied enough to reach every part of each
/// format: many blocks, back-references across blocks, incompressible and
/// run-length data, Huffman and FSE tables built and repeated.
class KafkaCompressionTest {
    private static final int[] SIZES = {0, 1, 5, 17, 100, 1000, 4096, 65535, 65536, 65537,
        131072, 131072 + 17, 300000, 1 << 20, 3 * (1 << 20) + 11};

    /// Text-like, with words repeating at many distances; random bytes;
    /// a run of one byte; and a mix that switches between them.
    static byte[] sample(int kind, int size, long seed) {
        Random r = new Random(seed);
        byte[] b = new byte[size];
        String[] words = {"order", "id", "customer", "\"status\":", "PAID", "SHIPPED", "{", "}",
            "2026-11-25T10:00:00Z", "amount", "EUR", "Lorem", "ipsum", "kafka", "\n", " ", ",", "0"};
        int p = 0;
        while (p < size) {
            int mode = kind == 3 ? (p / 7919) % 3 : kind;
            if (mode == 0) {
                byte[] w = words[r.nextInt(words.length)].getBytes();
                for (int i = 0; i < w.length && p < size; i++) {
                    b[p++] = w[i];
                }
            } else if (mode == 1) {
                b[p++] = (byte) r.nextInt();
            } else {
                b[p++] = 'x';
            }
        }
        return b;
    }

    interface Codec {
        byte[] compress(byte[] in) throws IOException;
    }

    private static byte[] through(byte[] in, StreamMaker maker) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputStream out = maker.make(bytes);
        out.write(in);
        out.close();
        return bytes.toByteArray();
    }

    interface StreamMaker {
        OutputStream make(OutputStream out) throws IOException;
    }

    private static void roundTrip(String name, Codec codec, Decoder decoder) throws IOException {
        for (int kind = 0; kind < 4; kind++) {
            for (int size : SIZES) {
                byte[] in = sample(kind, size, size * 31L + kind);
                byte[] packed = codec.compress(in);
                byte[] back;
                try {
                    back = decoder.decode(packed);
                } catch (IOException err) {
                    throw new AssertionError(name + " kind " + kind + " size " + size + ": "
                            + err.getMessage(), err);
                }
                assertArrayEquals(in, back, name + " kind " + kind + " size " + size);
            }
        }
    }

    interface Decoder {
        byte[] decode(byte[] in) throws IOException;
    }

    @Test
    @DisplayName("zstd: every level class, with and without a content size and checksum")
    void zstd() throws IOException {
        int[] levels = {-5, 1, 3, 9, 19};
        for (final int level : levels) {
            roundTrip("zstd " + level, new Codec() {
                @Override
                public byte[] compress(byte[] in) {
                    return com.github.luben.zstd.Zstd.compress(in, level);
                }
            }, Zstd::decompress);
            roundTrip("zstd stream " + level, new Codec() {
                @Override
                public byte[] compress(byte[] in) throws IOException {
                    return through(in, out -> {
                        ZstdOutputStream z = new ZstdOutputStream(out, level);
                        z.setChecksum(true);
                        return z;
                    });
                }
            }, Zstd::decompress);
        }
        roundTrip("zstd long distance", in -> through(in, out -> {
            ZstdOutputStream z = new ZstdOutputStream(out, 19);
            z.setLong(27);
            return z;
        }), Zstd::decompress);
    }

    @Test
    @DisplayName("zstd: frames and skippable frames one after another")
    void zstdConcatenated() throws IOException {
        byte[] a = sample(0, 70000, 1);
        byte[] b = sample(3, 200000, 2);
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        all.write(com.github.luben.zstd.Zstd.compress(a, 3));
        all.write(new byte[] {0x50, 0x2A, 0x4D, 0x18, 3, 0, 0, 0, 9, 9, 9});
        all.write(com.github.luben.zstd.Zstd.compress(b, 7));
        ByteArrayOutputStream expected = new ByteArrayOutputStream();
        expected.write(a);
        expected.write(b);
        assertArrayEquals(expected.toByteArray(), Zstd.decompress(all.toByteArray()));
    }

    @Test
    @DisplayName("lz4: frames with every option, and blocks from the high compressor")
    void lz4() throws IOException {
        LZ4FrameOutputStream.BLOCKSIZE[] sizes = {LZ4FrameOutputStream.BLOCKSIZE.SIZE_64KB,
            LZ4FrameOutputStream.BLOCKSIZE.SIZE_4MB};
        for (final LZ4FrameOutputStream.BLOCKSIZE size : sizes) {
            roundTrip("lz4 " + size, in -> through(in, out -> new LZ4FrameOutputStream(out, size)),
                    Lz4::decompress);
            roundTrip("lz4 checksums " + size, in -> through(in,
                    out -> new LZ4FrameOutputStream(out, size, in.length,
                            LZ4FrameOutputStream.FLG.Bits.BLOCK_INDEPENDENCE,
                            LZ4FrameOutputStream.FLG.Bits.BLOCK_CHECKSUM,
                            LZ4FrameOutputStream.FLG.Bits.CONTENT_SIZE,
                            LZ4FrameOutputStream.FLG.Bits.CONTENT_CHECKSUM)), Lz4::decompress);
        }
        final LZ4Compressor hc = LZ4Factory.fastestJavaInstance().highCompressor(17);
        roundTrip("lz4 hc block", in -> hc.compress(in), in -> {
            ByteSink out = new ByteSink(16);
            Lz4.block(in, 0, in.length, out, 0);
            return out.toArray();
        });
    }

    @Test
    @DisplayName("snappy: bare blocks and snappy-java's framing, which the Java client writes")
    void snappy() throws IOException {
        roundTrip("snappy raw", org.xerial.snappy.Snappy::compress, Snappy::decompress);
        roundTrip("snappy xerial", in -> through(in, SnappyOutputStream::new), Snappy::decompress);
        roundTrip("snappy xerial small blocks",
                in -> through(in, out -> new SnappyOutputStream(out, 1024)), Snappy::decompress);
    }

    @Test
    @DisplayName("Corrupt input is refused with an IOException and never anything else")
    void corruptInput() throws IOException {
        byte[] in = sample(3, 200000, 9);
        byte[][] packed = {com.github.luben.zstd.Zstd.compress(in, 5),
            through(in, out -> new LZ4FrameOutputStream(out)),
            through(in, SnappyOutputStream::new)};
        Decoder[] decoders = {Zstd::decompress, Lz4::decompress, Snappy::decompress};
        Random r = new Random(5);
        for (int c = 0; c < 3; c++) {
            for (int round = 0; round < 400; round++) {
                byte[] bad = packed[c].clone();
                int flips = 1 + r.nextInt(4);
                for (int f = 0; f < flips; f++) {
                    bad[r.nextInt(bad.length)] ^= (byte) (1 + r.nextInt(255));
                }
                if (round % 5 == 0) {
                    byte[] cut = new byte[r.nextInt(bad.length)];
                    System.arraycopy(bad, 0, cut, 0, cut.length);
                    bad = cut;
                }
                try {
                    decoders[c].decode(bad);
                } catch (IOException expected) {
                    // the only acceptable failure
                } catch (RuntimeException err) {
                    fail("codec " + c + " round " + round + " threw " + err);
                }
            }
        }
    }

    // ------------------------------------------------- whole record batches

    /// A record batch of `values`, compressed with `codec`, as a producer
    /// writes it.
    static byte[] batch(long base, int codec, long producer, boolean transactional,
                        boolean control, List<byte[]> values) throws IOException {
        KafkaConnection.Out records = new KafkaConnection.Out();
        for (int i = 0; i < values.size(); i++) {
            KafkaConnection.Out r = new KafkaConnection.Out();
            r.int8(0).varlong(0).varint(i).varbytes(control ? new byte[] {0, 0, 0, 0} : null)
                    .varbytes(values.get(i)).varint(0);
            records.varint(r.length).raw(r.data, 0, r.length);
        }
        byte[] plain = new byte[records.length];
        System.arraycopy(records.data, 0, plain, 0, plain.length);
        byte[] body = compress(codec, plain);
        int attributes = codec | (transactional ? 0x10 : 0) | (control ? 0x20 : 0);
        KafkaConnection.Out tail = new KafkaConnection.Out();
        tail.int16(attributes).int32(values.size() - 1).int64(0).int64(0).int64(producer)
                .int16(0).int32(0).int32(values.size()).raw(body, 0, body.length);
        KafkaConnection.Out b = new KafkaConnection.Out();
        b.int64(base).int32(4 + 1 + 4 + tail.length).int32(0).int8(2)
                .int32(KafkaConnection.Crc32c.of(tail.data, 0, tail.length))
                .raw(tail.data, 0, tail.length);
        byte[] out = new byte[b.length];
        System.arraycopy(b.data, 0, out, 0, out.length);
        return out;
    }

    private static byte[] compress(int codec, byte[] plain) throws IOException {
        switch (codec) {
            case 0:
                return plain;
            case 1:
                return through(plain, GZIPOutputStream::new);
            case 2:
                return through(plain, SnappyOutputStream::new);
            case 3:
                return through(plain, LZ4FrameOutputStream::new);
            default:
                return com.github.luben.zstd.Zstd.compress(plain, 3);
        }
    }

    @Test
    @DisplayName("A fetch of batches in every codec reads every record")
    void everyCodecInOneFetch() throws IOException {
        ByteArrayOutputStream set = new ByteArrayOutputStream();
        List<byte[]> expected = new ArrayList<byte[]>();
        long base = 0;
        for (int codec = 0; codec <= 4; codec++) {
            List<byte[]> values = new ArrayList<byte[]>();
            for (int i = 0; i < 500; i++) {
                values.add(sample(0, 200 + i, codec * 1000L + i));
            }
            set.write(batch(base, codec, -1, false, false, values));
            expected.addAll(values);
            base += values.size();
        }
        List<KafkaConnection.Record> out = new ArrayList<KafkaConnection.Record>();
        long next = KafkaConnection.records(set.toByteArray(), 0, out, null);
        assertEquals(base, next);
        assertEquals(expected.size(), out.size());
        for (int i = 0; i < out.size(); i++) {
            assertEquals(i, out.get(i).offset);
            assertArrayEquals(expected.get(i), out.get(i).value);
        }
    }

    @Test
    @DisplayName("read_committed drops an aborted transaction's records and keeps a committed one's")
    void readCommitted() throws IOException {
        List<byte[]> aborted = new ArrayList<byte[]>();
        aborted.add("lost-1".getBytes());
        aborted.add("lost-2".getBytes());
        List<byte[]> committed = new ArrayList<byte[]>();
        committed.add("kept".getBytes());
        List<byte[]> marker = new ArrayList<byte[]>();
        marker.add(new byte[] {0, 0, 0, 0, 0, 0});
        List<byte[]> later = new ArrayList<byte[]>();
        later.add("later".getBytes());
        ByteArrayOutputStream set = new ByteArrayOutputStream();
        set.write(batch(0, 4, 7, true, false, aborted));
        set.write(batch(2, 0, 8, true, false, committed));
        set.write(batch(3, 0, 7, true, true, marker));
        set.write(batch(4, 3, 7, true, false, later));
        List<KafkaConnection.Record> out = new ArrayList<KafkaConnection.Record>();
        KafkaConnection.records(set.toByteArray(), 0, out, new long[][] {{7, 0}});
        assertEquals(2, out.size());
        assertEquals("kept", new String(out.get(0).value));
        assertEquals("later", new String(out.get(1).value));
        out.clear();
        KafkaConnection.records(set.toByteArray(), 0, out, null);
        assertEquals(4, out.size());
    }

    @Test
    @DisplayName("A fetch that ends on a skipped batch still moves the offset past it")
    void skippedLastBatchAdvances() throws IOException {
        List<byte[]> aborted = new ArrayList<byte[]>();
        aborted.add("lost".getBytes());
        List<byte[]> marker = new ArrayList<byte[]>();
        marker.add(new byte[] {0, 0, 0, 0, 0, 0});
        ByteArrayOutputStream set = new ByteArrayOutputStream();
        set.write(batch(10, 0, 7, true, false, aborted));
        set.write(batch(11, 0, 7, true, true, marker));
        List<KafkaConnection.Record> out = new ArrayList<KafkaConnection.Record>();
        assertEquals(12, KafkaConnection.records(set.toByteArray(), 10, out,
                new long[][] {{7, 10}}));
        assertEquals(0, out.size());
        assertEquals(12, KafkaConnection.records(set.toByteArray(), 10, out, null));
        assertEquals(1, out.size());
        ByteArrayOutputStream onlyMarker = new ByteArrayOutputStream();
        onlyMarker.write(batch(11, 0, 7, true, true, marker));
        out.clear();
        assertEquals(12, KafkaConnection.records(onlyMarker.toByteArray(), 11, out, null));
        assertEquals(0, out.size());
    }
}
