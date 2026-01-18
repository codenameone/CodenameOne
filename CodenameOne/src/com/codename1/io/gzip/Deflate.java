/* -*-mode:java; c-basic-offset:2; -*- */
/*
Copyright (c) 2000-2011 ymnk, JCraft,Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.

  2. Redistributions in binary form must reproduce the above copyright 
     notice, this list of conditions and the following disclaimer in 
     the documentation and/or other materials provided with the distribution.

  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.codename1.io.gzip;

public
final class Deflate {

    static final private int MAX_MEM_LEVEL = 9;

    static final private int Z_DEFAULT_COMPRESSION = -1;

    static final private int MAX_WBITS = 15;            // 32K LZ77 window
    static final private int DEF_MEM_LEVEL = 8;
    static final private int STORED = 0;
    static final private int FAST = 1;
    static final private int SLOW = 2;
    static final private Config[] CONFIG_TABLE;
    static final private String[] Z_ERRMSG = {
            "need dictionary",     // Z_NEED_DICT       2
            "stream end",          // Z_STREAM_END      1
            "",                    // Z_OK              0
            "file error",          // Z_ERRNO         (-1)
            "stream error",        // Z_STREAM_ERROR  (-2)
            "data error",          // Z_DATA_ERROR    (-3)
            "insufficient memory", // Z_MEM_ERROR     (-4)
            "buffer error",        // Z_BUF_ERROR     (-5)
            "incompatible version",// Z_VERSION_ERROR (-6)
            ""
    };
    // block not completed, need more input or more output
    static final private int NEED_MORE = 0;
    // block flush performed
    static final private int BLOCK_DONE = 1;
    // finish started, need only more output at next deflate
    static final private int FINISH_STARTED = 2;
    // finish done, accept no more input or output
    static final private int FINISH_DONE = 3;
    // preset dictionary flag in zlib header
    static final private int PRESET_DICT = 0x20;
    static final private int Z_FILTERED = 1;
    static final private int Z_HUFFMAN_ONLY = 2;
    static final private int Z_DEFAULT_STRATEGY = 0;
    static final private int Z_NO_FLUSH = 0;
    static final private int Z_PARTIAL_FLUSH = 1;
    static final private int Z_SYNC_FLUSH = 2;
    static final private int Z_FULL_FLUSH = 3;
    static final private int Z_FINISH = 4;
    static final private int Z_OK = 0;
    static final private int Z_STREAM_END = 1;
    static final private int Z_NEED_DICT = 2;
    static final private int Z_ERRNO = -1;
    static final private int Z_STREAM_ERROR = -2;
    static final private int Z_DATA_ERROR = -3;
    static final private int Z_MEM_ERROR = -4;
    static final private int Z_BUF_ERROR = -5;
    static final private int Z_VERSION_ERROR = -6;
    static final private int INIT_STATE = 42;
    static final private int BUSY_STATE = 113;
    static final private int FINISH_STATE = 666;
    // The deflate compression method
    static final private int Z_DEFLATED = 8;
    static final private int STORED_BLOCK = 0;
    static final private int STATIC_TREES = 1;
    static final private int DYN_TREES = 2;
    // The three kinds of block type
    static final private int Z_BINARY = 0;
    static final private int Z_ASCII = 1;
    static final private int Z_UNKNOWN = 2;
    static final private int BUF_SIZE = 8 * 2;
    // repeat previous bit length 3-6 times (2 bits of repeat count)
    static final private int REP_3_6 = 16;
    // repeat a zero length 3-10 times  (3 bits of repeat count)
    static final private int REPZ_3_10 = 17;
    // repeat a zero length 11-138 times  (7 bits of repeat count)
    static final private int REPZ_11_138 = 18;
    static final private int MIN_MATCH = 3;
    static final private int MAX_MATCH = 258;
    static final private int MIN_LOOKAHEAD = (MAX_MATCH + MIN_MATCH + 1);
    static final private int MAX_BITS = 15;
    static final private int D_CODES = 30;
    static final private int BL_CODES = 19;
    static final private int LENGTH_CODES = 29;
    static final private int LITERALS = 256;
    static final private int L_CODES = (LITERALS + 1 + LENGTH_CODES);
    static final private int HEAP_SIZE = (2 * L_CODES + 1);
    static final private int END_BLOCK = 256;

    static {
        CONFIG_TABLE = new Config[10];
        //                         good  lazy  nice  chain
        CONFIG_TABLE[0] = new Config(0, 0, 0, 0, STORED);
        CONFIG_TABLE[1] = new Config(4, 4, 8, 4, FAST);
        CONFIG_TABLE[2] = new Config(4, 5, 16, 8, FAST);
        CONFIG_TABLE[3] = new Config(4, 6, 32, 32, FAST);

        CONFIG_TABLE[4] = new Config(4, 4, 16, 16, SLOW);
        CONFIG_TABLE[5] = new Config(8, 16, 32, 32, SLOW);
        CONFIG_TABLE[6] = new Config(8, 16, 128, 128, SLOW);
        CONFIG_TABLE[7] = new Config(8, 32, 128, 256, SLOW);
        CONFIG_TABLE[8] = new Config(32, 128, 258, 1024, SLOW);
        CONFIG_TABLE[9] = new Config(32, 258, 258, 4096, SLOW);
    }

    ZStream strm;        // pointer back to this zlib stream
    int status;           // as the name implies
    byte[] pendingBuf;   // output still pending
    int pendingBufSize; // size of pending_buf
    int pendingOut;      // next pending byte to output to the stream
    int pending;          // nb of bytes in the pending buffer
    int wrap = 1;
    byte dataType;       // UNKNOWN, BINARY or ASCII
    int lastFlush;       // value of flush param for previous deflate call
    int wSize;           // LZ77 window size (32K by default)
    int wBits;           // log2(w_size)  (8..16)
    int wMask;           // w_size - 1
    byte[] window;
    int windowSize;
    // Sliding window. Input bytes are read into the second half of the window,
    // and move to the first half later to keep a dictionary of at least wSize
    // bytes. With this organization, matches are limited to a distance of
    // wSize-MAX_MATCH bytes, but this ensures that IO is always
    // performed with a length multiple of the block size. Also, it limits
    // the window size to 64K, which is quite useful on MSDOS.
    // To do: use the user input buffer as sliding window.
    short[] prev;
    // Actual size of window: 2*wSize, except when the user input buffer
    // is directly used as sliding window.
    short[] head; // Heads of the hash chains or NIL.
    // Link to older string with same hash index. To limit the size of this
    // array to 64K, this link is maintained only for the last 32K strings.
    // An index in this array is thus a window index modulo 32K.
    int insH;          // hash index of string to be inserted
    int hashSize;      // number of elements in hash table
    int hashBits;      // log2(hash_size)
    int hashMask;      // hash_size-1
    // Number of bits by which ins_h must be shifted at each input
    // step. It must be such that after MIN_MATCH steps, the oldest
    // byte no longer takes part in the hash key, that is:
    // hash_shift * MIN_MATCH >= hash_bits
    int hashShift;
    int blockStart;

    // Window position at the beginning of the current output block. Gets
    // negative when the window is moved backwards.
    int matchLength;           // length of best match
    int prevMatch;             // previous match
    int matchAvailable;        // set if previous match exists
    int strStart;               // start of string to insert
    int matchStart;            // start of matching string
    int lookahead;              // number of valid bytes ahead in window
    // Length of the best match at previous step. Matches not greater than this
    // are discarded. This is used in the lazy match evaluation.
    int prevLength;
    // To speed up deflation, hash chains are never searched beyond this
    // length.  A higher limit improves compression ratio but degrades the speed.
    int maxChainLength;
    // Attempt to find a better match only when the current match is strictly
    // smaller than this value. This mechanism is used only for compression
    // levels >= 4.
    int maxLazyMatch;
    int level;    // compression level (1..9)

    // Insert new strings in the hash table only if the match length is not
    // greater than this length. This saves time but degrades compression.
    // max_insert_length is used only for compression levels <= 3.
    int strategy; // favor or force Huffman coding
    // Use a faster search when the previous match is longer than this
    int goodMatch;
    // Stop searching when current match exceeds this
    int niceMatch;
    short[] dynLtree;       // literal and length tree
    short[] dynDtree;       // distance tree
    short[] blTree;         // Huffman tree for bit lengths
    Tree lDesc = new Tree();  // desc for literal tree
    Tree dDesc = new Tree();  // desc for distance tree
    Tree blDesc = new Tree(); // desc for bit length tree
    // number of codes at each bit length for an optimal tree
    short[] blCount = new short[MAX_BITS + 1];
    // working area to be used in Tree#gen_codes()
    short[] nextCode = new short[MAX_BITS + 1];
    // heap used to build the Huffman trees
    int[] heap = new int[2 * L_CODES + 1];
    int heapLen;               // number of elements in the heap
    int heapMax;               // element of largest frequency
    // Depth of each subtree used as tie breaker for trees of equal frequency
    byte[] depth = new byte[2 * L_CODES + 1];
    // The sons of heap[n] are heap[2*n] and heap[2*n+1]. heap[0] is not used.
    // The same heap array is used to build all trees.
    int lBuf;               // index for literals or lengths */
    // Size of match buffer for literals/lengths.  There are 4 reasons for
    // limiting lit_bufsize to 64K:
    //   - frequencies can be kept in 16 bit counters
    //   - if compression is not successful for the first block, all input
    //     data is still in the window so we can still emit a stored block even
    //     when input comes from standard input.  (This can also be done for
    //     all blocks if lit_bufsize is not greater than 32K.)
    //   - if compression is not successful for a file smaller than 64K, we can
    //     even emit a stored file instead of a stored block (saving 5 bytes).
    //     This is applicable only for zip (not gzip or zlib).
    //   - creating new Huffman trees less frequently may not provide fast
    //     adaptation to changes in the input data statistics. (Take for
    //     example a binary file with poorly compressible code followed by
    //     a highly compressible string table.) Smaller buffer sizes give
    //     fast adaptation but have of course the overhead of transmitting
    //     trees more frequently.
    //   - I can't count above 4
    int litBufSize;
    int lastLit;      // running index in l_buf
    int dBuf;         // index of pendig_buf

    // Buffer for distances. To simplify the code, d_buf and l_buf have
    // the same number of elements. To use different lengths, an extra flag
    // array would be necessary.
    int optLen;        // bit length of current block with optimal trees
    int staticLen;     // bit length of current block with static trees
    int matches;        // number of string matches in current block
    int lastEobLen;   // bit length of EOB code for last block
    // Output buffer. bits are inserted starting at the bottom (least
    // significant bits).
    short biBuf;
    // Number of valid bits in bi_buf.  All bits above the last valid bit
    // are always zero.
    int biValid;
    GZIPHeader gheader = null;

    Deflate(ZStream strm) {
        this.strm = strm;
        dynLtree = new short[HEAP_SIZE * 2];
        dynDtree = new short[(2 * D_CODES + 1) * 2]; // distance tree
        blTree = new short[(2 * BL_CODES + 1) * 2];  // Huffman tree for bit lengths
    }

    static boolean smaller(short[] tree, int n, int m, byte[] depth) {
        short tn2 = tree[n * 2];
        short tm2 = tree[m * 2];
        return (tn2 < tm2 ||
                (tn2 == tm2 && depth[n] <= depth[m]));
    }

    static int deflateCopy(ZStream dest, ZStream src) {

        if (src.dstate == null) {
            return Z_STREAM_ERROR;
        }

        if (src.nextIn != null) {
            dest.nextIn = new byte[src.nextIn.length];
            System.arraycopy(src.nextIn, 0, dest.nextIn, 0, src.nextIn.length);
        }
        dest.nextInIndex = src.nextInIndex;
        dest.availIn = src.availIn;
        dest.totalIn = src.totalIn;

        if (src.nextOut != null) {
            dest.nextOut = new byte[src.nextOut.length];
            System.arraycopy(src.nextOut, 0, dest.nextOut, 0, src.nextOut.length);
        }

        dest.nextOutIndex = src.nextOutIndex;
        dest.availOut = src.availOut;
        dest.totalOut = src.totalOut;

        dest.msg = src.msg;
        dest.dataType = src.dataType;
        dest.adler = src.adler.copy();

        dest.dstate = (Deflate) src.dstate.clone();
        dest.dstate.strm = dest;
        return Z_OK;
    }

    void lmInit() {
        windowSize = 2 * wSize;

        head[hashSize - 1] = 0;
        for (int i = 0; i < hashSize - 1; i++) {
            head[i] = 0;
        }

        // Set the default configuration parameters:
        maxLazyMatch = Deflate.CONFIG_TABLE[level].maxLazy;
        goodMatch = Deflate.CONFIG_TABLE[level].goodLength;
        niceMatch = Deflate.CONFIG_TABLE[level].niceLength;
        maxChainLength = Deflate.CONFIG_TABLE[level].maxChain;

        strStart = 0;
        blockStart = 0;
        lookahead = 0;
        matchLength = prevLength = MIN_MATCH - 1;
        matchAvailable = 0;
        insH = 0;
    }

    // Initialize the tree data structures for a new zlib stream.
    void trInit() {

        lDesc.dynTree = dynLtree;
        lDesc.statDesc = StaticTree.staticLDesc;

        dDesc.dynTree = dynDtree;
        dDesc.statDesc = StaticTree.staticDDesc;

        blDesc.dynTree = blTree;
        blDesc.statDesc = StaticTree.staticBlDesc;

        biBuf = 0;
        biValid = 0;
        lastEobLen = 8; // enough lookahead for inflate

        // Initialize the first block of the first file:
        initBlock();
    }

    void initBlock() {
        // Initialize the trees.
        for (int i = 0; i < L_CODES; i++) dynLtree[i * 2] = 0;
        for (int i = 0; i < D_CODES; i++) dynDtree[i * 2] = 0;
        for (int i = 0; i < BL_CODES; i++) blTree[i * 2] = 0;

        dynLtree[END_BLOCK * 2] = 1;
        optLen = staticLen = 0;
        lastLit = matches = 0;
    }

    // Restore the heap property by moving down the tree starting at node k,
    // exchanging a node with the smallest of its two sons if necessary, stopping
    // when the heap property is re-established (each father smaller than its
    // two sons).
    void pqdownheap(short[] tree,  // the tree to restore
                    int k          // node to move down
    ) {
        int v = heap[k];
        int j = k << 1;  // left son of k
        while (j <= heapLen) {
            // Set j to the smallest of the two sons:
            if (j < heapLen &&
                    smaller(tree, heap[j + 1], heap[j], depth)) {
                j++;
            }
            // Exit if v is smaller than both sons
            if (smaller(tree, v, heap[j], depth)) break;

            // Exchange v with the smallest son
            heap[k] = heap[j];
            k = j;
            // And continue down the tree, setting j to the left son of k
            j <<= 1;
        }
        heap[k] = v;
    }

    // Scan a literal or distance tree to determine the frequencies of the codes
    // in the bit length tree.
    void scanTree(short[] tree,// the tree to be scanned
                  int maxCode // and its largest code of non-zero frequency
    ) {
        int n;                     // iterates over all tree elements
        int prevlen = -1;          // last emitted length
        int curlen;                // length of current code
        int nextlen = tree[1]; // length of next code
        int count = 0;             // repeat count of the current code
        int maxCount = 7;         // max repeat count
        int minCount = 4;         // min repeat count

        if (nextlen == 0) {
            maxCount = 138;
            minCount = 3;
        }
        tree[(maxCode + 1) * 2 + 1] = (short) 0xffff; // guard

        for (n = 0; n <= maxCode; n++) {
            curlen = nextlen;
            nextlen = tree[(n + 1) * 2 + 1];
            if (++count < maxCount && curlen == nextlen) {
                continue;
            } else if (count < minCount) {
                blTree[curlen * 2] += count;
            } else if (curlen != 0) {
                if (curlen != prevlen) blTree[curlen * 2]++;
                blTree[REP_3_6 * 2]++;
            } else if (count <= 10) {
                blTree[REPZ_3_10 * 2]++;
            } else {
                blTree[REPZ_11_138 * 2]++;
            }
            count = 0;
            prevlen = curlen;
            if (nextlen == 0) {
                maxCount = 138;
                minCount = 3;
            } else if (curlen == nextlen) {
                maxCount = 6;
                minCount = 3;
            } else {
                maxCount = 7;
                minCount = 4;
            }
        }
    }

    // Construct the Huffman tree for the bit lengths and return the index in
    // BL_ORDER of the last bit length code to send.
    int buildBlTree() {
        int maxBlindex;  // index of last bit length code of non zero freq

        // Determine the bit length frequencies for literal and distance trees
        scanTree(dynLtree, lDesc.maxCode);
        scanTree(dynDtree, dDesc.maxCode);

        // Build the bit length tree:
        blDesc.buildTree(this);
        // opt_len now includes the length of the tree representations, except
        // the lengths of the bit lengths codes and the 5+5+4 bits for the counts.

        // Determine the number of bit length codes to send. The pkzip format
        // requires that at least 4 bit length codes be sent. (appnote.txt says
        // 3 but the actual value used is 4.)
        for (maxBlindex = BL_CODES - 1; maxBlindex >= 3; maxBlindex--) {
            if (blTree[Tree.BL_ORDER[maxBlindex] * 2 + 1] != 0) break;
        }
        // Update opt_len to include the bit length tree and counts
        optLen += 3 * (maxBlindex + 1) + 5 + 5 + 4;

        return maxBlindex;
    }


    // Send the header for a block using dynamic Huffman trees: the counts, the
    // lengths of the bit length codes, the literal tree and the distance tree.
    // IN assertion: lcodes >= 257, dcodes >= 1, blcodes >= 4.
    void sendAllTrees(int lcodes, int dcodes, int blcodes) {
        int rank;                    // index in bl_order

        sendBits(lcodes - 257, 5); // not +255 as stated in appnote.txt
        sendBits(dcodes - 1, 5);
        sendBits(blcodes - 4, 4); // not -3 as stated in appnote.txt
        for (rank = 0; rank < blcodes; rank++) {
            sendBits(blTree[Tree.BL_ORDER[rank] * 2 + 1], 3);
        }
        sendTree(dynLtree, lcodes - 1); // literal tree
        sendTree(dynDtree, dcodes - 1); // distance tree
    }

    // Send a literal or distance tree in compressed form, using the codes in
    // bl_tree.
    void sendTree(short[] tree,// the tree to be sent
                  int maxCode // and its largest code of non zero frequency
    ) {
        int n;                     // iterates over all tree elements
        int prevlen = -1;          // last emitted length
        int curlen;                // length of current code
        int nextlen = tree[1]; // length of next code
        int count = 0;             // repeat count of the current code
        int maxCount = 7;         // max repeat count
        int minCount = 4;         // min repeat count

        if (nextlen == 0) {
            maxCount = 138;
            minCount = 3;
        }

        for (n = 0; n <= maxCode; n++) {
            curlen = nextlen;
            nextlen = tree[(n + 1) * 2 + 1];
            if (++count < maxCount && curlen == nextlen) {
                continue;
            } else if (count < minCount) {
                do {
                    sendCode(curlen, blTree);
                } while (--count != 0);
            } else if (curlen != 0) {
                if (curlen != prevlen) {
                    sendCode(curlen, blTree);
                    count--;
                }
                sendCode(REP_3_6, blTree);
                sendBits(count - 3, 2);
            } else if (count <= 10) {
                sendCode(REPZ_3_10, blTree);
                sendBits(count - 3, 3);
            } else {
                sendCode(REPZ_11_138, blTree);
                sendBits(count - 11, 7);
            }
            count = 0;
            prevlen = curlen;
            if (nextlen == 0) {
                maxCount = 138;
                minCount = 3;
            } else if (curlen == nextlen) {
                maxCount = 6;
                minCount = 3;
            } else {
                maxCount = 7;
                minCount = 4;
            }
        }
    }

    // Output a byte on the stream.
    // IN assertion: there is enough room in pending_buf.
    void putByte(byte[] p, int start, int len) {
        System.arraycopy(p, start, pendingBuf, pending, len);
        pending += len;
    }

    void putByte(byte c) {
        pendingBuf[pending++] = c;
    }

    void putShort(int w) {
        putByte((byte) (w/*&0xff*/));
        putByte((byte) (w >>> 8));
    }

    void putShortMSB(int b) {
        putByte((byte) (b >> 8));
        putByte((byte) (b/*&0xff*/));
    }

    void sendCode(int c, short[] tree) {
        int c2 = c * 2;
        sendBits((tree[c2] & 0xffff), (tree[c2 + 1] & 0xffff));
    }

    void sendBits(int value, int length) {
        int len = length;
        if (biValid > BUF_SIZE - len) {
            int val = value;
//      bi_buf |= (val << bi_valid);
            biBuf |= ((val << biValid) & 0xffff);
            putShort(biBuf);
            biBuf = (short) (val >>> (BUF_SIZE - biValid));
            biValid += len - BUF_SIZE;
        } else {
//      bi_buf |= (value) << bi_valid;
            biBuf |= (((value) << biValid) & 0xffff);
            biValid += len;
        }
    }

    // Send one empty static block to give enough lookahead for inflate.
    // This takes 10 bits, of which 7 may remain in the bit buffer.
    // The current inflate code requires 9 bits of lookahead. If the
    // last two codes for the previous block (real code plus EOB) were coded
    // on 5 bits or less, inflate may have only 5+3 bits of lookahead to decode
    // the last real code. In this case we send two empty static blocks instead
    // of one. (There are no problems if the previous block is stored or fixed.)
    // To simplify the code, we assume the worst case of last real code encoded
    // on one bit only.
    void trAlign() {
        sendBits(STATIC_TREES << 1, 3);
        sendCode(END_BLOCK, StaticTree.STATIC_LTREE);

        biFlush();

        // Of the 10 bits for the empty block, we have already sent
        // (10 - bi_valid) bits. The lookahead for the last real code (before
        // the EOB of the previous block) was thus at least one plus the length
        // of the EOB plus what we have just sent of the empty static block.
        if (1 + lastEobLen + 10 - biValid < 9) {
            sendBits(STATIC_TREES << 1, 3);
            sendCode(END_BLOCK, StaticTree.STATIC_LTREE);
            biFlush();
        }
        lastEobLen = 7;
    }


    // Save the match info and tally the frequency counts. Return true if
    // the current block must be flushed.
    boolean trTally(int dist, // distance of matched string
                    int lc // match length-MIN_MATCH or unmatched char (if dist==0)
    ) {

        pendingBuf[dBuf + lastLit * 2] = (byte) (dist >>> 8);
        pendingBuf[dBuf + lastLit * 2 + 1] = (byte) dist;

        pendingBuf[lBuf + lastLit] = (byte) lc;
        lastLit++;

        if (dist == 0) {
            // lc is the unmatched char
            dynLtree[lc * 2]++;
        } else {
            matches++;
            // Here, lc is the match length - MIN_MATCH
            dist--;             // dist = match distance - 1
            dynLtree[(Tree.LENGTH_CODE[lc] + LITERALS + 1) * 2]++;
            dynDtree[Tree.dCode(dist) * 2]++;
        }

        if ((lastLit & 0x1fff) == 0 && level > 2) {
            // Compute an upper bound for the compressed length
            int outLength = lastLit * 8;
            int inLength = strStart - blockStart;
            int dcode;
            for (dcode = 0; dcode < D_CODES; dcode++) {
                outLength += (int) dynDtree[dcode * 2] *
                        (5L + Tree.EXTRA_DBITS[dcode]);
            }
            outLength >>>= 3;
            if ((matches < (lastLit / 2)) && outLength < inLength / 2) return true;
        }

        return (lastLit == litBufSize - 1);
        // We avoid equality with lit_bufsize because of wraparound at 64K
        // on 16 bit machines and because stored blocks are restricted to
        // 64K-1 bytes.
    }

    // Send the block data compressed using the given Huffman trees
    void compressBlock(short[] ltree, short[] dtree) {
        int dist;      // distance of matched string
        int lc;         // match length or unmatched char (if dist == 0)
        int lx = 0;     // running index in l_buf
        int code;       // the code to send
        int extra;      // number of extra bits to send

        if (lastLit != 0) {
            do {
                dist = ((pendingBuf[dBuf + lx * 2] << 8) & 0xff00) |
                        (pendingBuf[dBuf + lx * 2 + 1] & 0xff);
                lc = (pendingBuf[lBuf + lx]) & 0xff;
                lx++;

                if (dist == 0) {
                    sendCode(lc, ltree); // send a literal byte
                } else {
                    // Here, lc is the match length - MIN_MATCH
                    code = Tree.LENGTH_CODE[lc];

                    sendCode(code + LITERALS + 1, ltree); // send the length code
                    extra = Tree.EXTRA_LBITS[code];
                    if (extra != 0) {
                        lc -= Tree.BASE_LENGTH[code];
                        sendBits(lc, extra);       // send the extra length bits
                    }
                    dist--; // dist is now the match distance - 1
                    code = Tree.dCode(dist);

                    sendCode(code, dtree);       // send the distance code
                    extra = Tree.EXTRA_DBITS[code];
                    if (extra != 0) {
                        dist -= Tree.BASE_DIST[code];
                        sendBits(dist, extra);   // send the extra distance bits
                    }
                } // literal or match pair ?

                // Check that the overlay between pending_buf and d_buf+l_buf is ok:
            }
            while (lx < lastLit);
        }

        sendCode(END_BLOCK, ltree);
        lastEobLen = ltree[END_BLOCK * 2 + 1];
    }

    // Set the data type to ASCII or BINARY, using a crude approximation:
    // binary if more than 20% of the bytes are <= 6 or >= 128, ascii otherwise.
    // IN assertion: the fields freq of dyn_ltree are set and the total of all
    // frequencies does not exceed 64K (to fit in an int on 16 bit machines).
    void setDataType() {
        int n = 0;
        int asciiFreq = 0;
        int binFreq = 0;
        while (n < 7) {
            binFreq += dynLtree[n * 2];
            n++;
        }
        while (n < 128) {
            asciiFreq += dynLtree[n * 2];
            n++;
        }
        while (n < LITERALS) {
            binFreq += dynLtree[n * 2];
            n++;
        }
        dataType = (byte) (binFreq > (asciiFreq >>> 2) ? Z_BINARY : Z_ASCII);
    }

    // Flush the bit buffer, keeping at most 7 bits in it.
    void biFlush() {
        if (biValid == 16) {
            putShort(biBuf);
            biBuf = 0;
            biValid = 0;
        } else if (biValid >= 8) {
            putByte((byte) biBuf);
            biBuf >>>= 8;
            biValid -= 8;
        }
    }

    // Flush the bit buffer and align the output on a byte boundary
    void biWindup() {
        if (biValid > 8) {
            putShort(biBuf);
        } else if (biValid > 0) {
            putByte((byte) biBuf);
        }
        biBuf = 0;
        biValid = 0;
    }

    // Copy a stored block, storing first the length and its
    // one's complement if requested.
    void copyBlock(int buf,         // the input data
                   int len,         // its length
                   boolean header   // true if block header must be written
    ) {
        int index = 0;
        biWindup();      // align on byte boundary
        lastEobLen = 8; // enough lookahead for inflate

        if (header) {
            putShort((short) len);
            putShort((short) ~len);
        }

        //  while(len--!=0) {
        //    put_byte(window[buf+index]);
        //    index++;
        //  }
        putByte(window, buf, len);
    }

    void flushBlockOnly(boolean eof) {
        trFlushBlock(blockStart >= 0 ? blockStart : -1,
                strStart - blockStart,
                eof);
        blockStart = strStart;
        strm.flushPending();
    }

    // Copy without compression as much as possible from the input stream, return
    // the current block state.
    // This function does not insert new strings in the dictionary since
    // uncompressible data is probably not useful. This function is used
    // only for the level=0 compression option.
    // NOTE: this function should be optimized to avoid extra copying from
    // window to pending_buf.
    int deflateStored(int flush) {
        // Stored blocks are limited to 0xffff bytes, pending_buf is limited
        // to pending_buf_size, and each stored block has a 5 byte header:

        int maxBlockSize = 0xffff;
        int maxStart;

        if (maxBlockSize > pendingBufSize - 5) {
            maxBlockSize = pendingBufSize - 5;
        }

        // Copy as much as possible from input to output:
        while (true) {
            // Fill the window as much as possible:
            if (lookahead <= 1) {
                fillWindow();
                if (lookahead == 0 && flush == Z_NO_FLUSH) return NEED_MORE;
                if (lookahead == 0) break; // flush the current block
            }

            strStart += lookahead;
            lookahead = 0;

            // Emit a stored block if pending_buf will be full:
            maxStart = blockStart + maxBlockSize;
            if (strStart == 0 || strStart >= maxStart) {
                // strstart == 0 is possible when wraparound on 16-bit machine
                lookahead = strStart - maxStart;
                strStart = maxStart;

                flushBlockOnly(false);
                if (strm.availOut == 0) return NEED_MORE;

            }

            // Flush if we may have to slide, otherwise block_start may become
            // negative and the data will be gone:
            if (strStart - blockStart >= wSize - MIN_LOOKAHEAD) {
                flushBlockOnly(false);
                if (strm.availOut == 0) return NEED_MORE;
            }
        }

        flushBlockOnly(flush == Z_FINISH);
        if (strm.availOut == 0)
            return (flush == Z_FINISH) ? FINISH_STARTED : NEED_MORE;

        return flush == Z_FINISH ? FINISH_DONE : BLOCK_DONE;
    }

    // Send a stored block
    void trStoredBlock(int buf,        // input block
                       int storedLen, // length of input block
                       boolean eof     // true if this is the last block for a file
    ) {
        sendBits((eof ? 1 : 0), 3);  // send block type
        copyBlock(buf, storedLen, true);          // with header
    }

    // Determine the best encoding for the current block: dynamic trees, static
    // trees or store, and output the encoded block to the zip file.
    void trFlushBlock(int buf,        // input block, or NULL if too old
                      int storedLen, // length of input block
                      boolean eof     // true if this is the last block for a file
    ) {
        int optLenb, staticLenb;// opt_len and static_len in bytes
        int maxBlIndex = 0;      // index of last bit length code of non zero freq

        // Build the Huffman trees unless a stored block is forced
        if (level > 0) {
            // Check if the file is ascii or binary
            if (dataType == Z_UNKNOWN) setDataType();

            // Construct the literal and distance trees
            lDesc.buildTree(this);

            dDesc.buildTree(this);

            // At this point, opt_len and static_len are the total bit lengths of
            // the compressed block data, excluding the tree representations.

            // Build the bit length tree for the above two trees, and get the index
            // in BL_ORDER of the last bit length code to send.
            maxBlIndex = buildBlTree();

            // Determine the best encoding. Compute first the block length in bytes
            optLenb = (optLen + 3 + 7) >>> 3;
            staticLenb = (staticLen + 3 + 7) >>> 3;

            if (staticLenb <= optLenb) optLenb = staticLenb;
        } else {
            optLenb = staticLenb = storedLen + 5; // force a stored block
        }

        if (storedLen + 4 <= optLenb && buf != -1) {
            // 4: two words for the lengths
            // The test buf != NULL is only necessary if LIT_BUFSIZE > WSIZE.
            // Otherwise we can't have processed more than WSIZE input bytes since
            // the last block flush, because compression would have been
            // successful. If LIT_BUFSIZE <= WSIZE, it is never too late to
            // transform a block into a stored block.
            trStoredBlock(buf, storedLen, eof);
        } else if (staticLenb == optLenb) {
            sendBits((STATIC_TREES << 1) + (eof ? 1 : 0), 3);
            compressBlock(StaticTree.STATIC_LTREE, StaticTree.STATIC_DTREE);
        } else {
            sendBits((DYN_TREES << 1) + (eof ? 1 : 0), 3);
            sendAllTrees(lDesc.maxCode + 1, dDesc.maxCode + 1, maxBlIndex + 1);
            compressBlock(dynLtree, dynDtree);
        }

        // The above check is made mod 2^32, for files larger than 512 MB
        // and uLong implemented on 32 bits.

        initBlock();

        if (eof) {
            biWindup();
        }
    }

    // Fill the window when the lookahead becomes insufficient.
    // Updates strstart and lookahead.
    //
    // IN assertion: lookahead < MIN_LOOKAHEAD
    // OUT assertions: strstart <= window_size-MIN_LOOKAHEAD
    //    At least one byte has been read, or avail_in == 0; reads are
    //    performed for at least two bytes (required for the zip translate_eol
    //    option -- not supported here).
    void fillWindow() {
        int n, m;
        int p;
        int more;    // Amount of free space at the end of the window.

        do {
            more = (windowSize - lookahead - strStart);

            // Deal with !@#$% 64K limit:
            if (more == 0 && strStart == 0 && lookahead == 0) {
                more = wSize;
            } else if (more == -1) {
                // Very unlikely, but possible on 16 bit machine if strstart == 0
                // and lookahead == 1 (input done one byte at time)
                more--;

                // If the window is almost full and there is insufficient lookahead,
                // move the upper half to the lower one to make room in the upper half.
            } else if (strStart >= wSize + wSize - MIN_LOOKAHEAD) {
                System.arraycopy(window, wSize, window, 0, wSize);
                matchStart -= wSize;
                strStart -= wSize; // we now have strstart >= MAX_DIST
                blockStart -= wSize;

                // Slide the hash table (could be avoided with 32 bit values
                // at the expense of memory usage). We slide even when level == 0
                // to keep the hash table consistent if we switch back to level > 0
                // later. (Using level 0 permanently is not an optimal usage of
                // zlib, so we don't care about this pathological case.)

                n = hashSize;
                p = n;
                do {
                    m = (head[--p] & 0xffff);
                    head[p] = (m >= wSize ? (short) (m - wSize) : 0);
                }
                while (--n != 0);

                n = wSize;
                p = n;
                do {
                    m = (prev[--p] & 0xffff);
                    prev[p] = (m >= wSize ? (short) (m - wSize) : 0);
                    // If n is not on any hash chain, prev[n] is garbage but
                    // its value will never be used.
                }
                while (--n != 0);
                more += wSize;
            }

            if (strm.availIn == 0) return;

            // If there was no sliding:
            //    strstart <= WSIZE+MAX_DIST-1 && lookahead <= MIN_LOOKAHEAD - 1 &&
            //    more == window_size - lookahead - strstart
            // => more >= window_size - (MIN_LOOKAHEAD-1 + WSIZE + MAX_DIST-1)
            // => more >= window_size - 2*WSIZE + 2
            // In the BIG_MEM or MMAP case (not yet supported),
            //   window_size == input_size + MIN_LOOKAHEAD  &&
            //   strstart + s->lookahead <= input_size => more >= MIN_LOOKAHEAD.
            // Otherwise, window_size == 2*WSIZE so more >= 2.
            // If there was sliding, more >= WSIZE. So in all cases, more >= 2.

            n = strm.readBuf(window, strStart + lookahead, more);
            lookahead += n;

            // Initialize the hash value now that we have some input:
            if (lookahead >= MIN_MATCH) {
                insH = window[strStart] & 0xff;
                insH = (((insH) << hashShift) ^ (window[strStart + 1] & 0xff)) & hashMask;
            }
            // If the whole input has less than MIN_MATCH bytes, ins_h is garbage,
            // but this is not important since only literal bytes will be emitted.
        }
        while (lookahead < MIN_LOOKAHEAD && strm.availIn != 0);
    }

    // Compress as much as possible from the input stream, return the current
    // block state.
    // This function does not perform lazy evaluation of matches and inserts
    // new strings in the dictionary only for unmatched strings or for short
    // matches. It is used only for the fast compression options.
    int deflateFast(int flush) {
//    short hashHead = 0; // head of the hash chain
        int hashHead = 0; // head of the hash chain
        boolean bflush;      // set if current block must be flushed

        while (true) {
            // Make sure that we always have enough lookahead, except
            // at the end of the input file. We need MAX_MATCH bytes
            // for the next match, plus MIN_MATCH bytes to insert the
            // string following the next match.
            if (lookahead < MIN_LOOKAHEAD) {
                fillWindow();
                if (lookahead < MIN_LOOKAHEAD && flush == Z_NO_FLUSH) {
                    return NEED_MORE;
                }
                if (lookahead == 0) break; // flush the current block
            }

            // Insert the string window[strstart .. strstart+2] in the
            // dictionary, and set hashHead to the head of the hash chain:
            if (lookahead >= MIN_MATCH) {
                insH = (((insH) << hashShift) ^ (window[(strStart) + (MIN_MATCH - 1)] & 0xff)) & hashMask;

//	prev[strstart&w_mask]=hashHead=head[ins_h];
                hashHead = (head[insH] & 0xffff);
                prev[strStart & wMask] = head[insH];
                head[insH] = (short) strStart;
            }

            // Find the longest match, discarding those <= prev_length.
            // At this point we have always match_length < MIN_MATCH

            if (hashHead != 0L &&
                    ((strStart - hashHead) & 0xffff) <= wSize - MIN_LOOKAHEAD
            ) {
                // To simplify the code, we prevent matches with the string
                // of window index 0 (in particular we have to avoid a match
                // of the string with itself at the start of the input file).
                if (strategy != Z_HUFFMAN_ONLY) {
                    matchLength = longestMatch(hashHead);
                }
                // longest_match() sets match_start
            }
            if (matchLength >= MIN_MATCH) {
                //        check_match(strstart, match_start, match_length);

                bflush = trTally(strStart - matchStart, matchLength - MIN_MATCH);

                lookahead -= matchLength;

                // Insert new strings in the hash table only if the match length
                // is not too large. This saves time but degrades compression.
                if (matchLength <= maxLazyMatch &&
                        lookahead >= MIN_MATCH) {
                    matchLength--; // string at strstart already in hash table
                    do {
                        strStart++;

                        insH = ((insH << hashShift) ^ (window[(strStart) + (MIN_MATCH - 1)] & 0xff)) & hashMask;
//	    prev[strstart&w_mask]=hashHead=head[ins_h];
                        hashHead = (head[insH] & 0xffff);
                        prev[strStart & wMask] = head[insH];
                        head[insH] = (short) strStart;

                        // strstart never exceeds WSIZE-MAX_MATCH, so there are
                        // always MIN_MATCH bytes ahead.
                    }
                    while (--matchLength != 0);
                    strStart++;
                } else {
                    strStart += matchLength;
                    matchLength = 0;
                    insH = window[strStart] & 0xff;

                    insH = (((insH) << hashShift) ^ (window[strStart + 1] & 0xff)) & hashMask;
                    // If lookahead < MIN_MATCH, ins_h is garbage, but it does not
                    // matter since it will be recomputed at next deflate call.
                }
            } else {
                // No match, output a literal byte

                bflush = trTally(0, window[strStart] & 0xff);
                lookahead--;
                strStart++;
            }
            if (bflush) {

                flushBlockOnly(false);
                if (strm.availOut == 0) return NEED_MORE;
            }
        }

        flushBlockOnly(flush == Z_FINISH);
        if (strm.availOut == 0) {
            if (flush == Z_FINISH) return FINISH_STARTED;
            else return NEED_MORE;
        }
        return flush == Z_FINISH ? FINISH_DONE : BLOCK_DONE;
    }

    // Same as above, but achieves better compression. We use a lazy
    // evaluation for matches: a match is finally adopted only if there is
    // no better match at the next window position.
    int deflateSlow(int flush) {
//    short hashHead = 0;    // head of hash chain
        int hashHead = 0;    // head of hash chain
        boolean bflush;         // set if current block must be flushed

        // Process the input block.
        while (true) {
            // Make sure that we always have enough lookahead, except
            // at the end of the input file. We need MAX_MATCH bytes
            // for the next match, plus MIN_MATCH bytes to insert the
            // string following the next match.

            if (lookahead < MIN_LOOKAHEAD) {
                fillWindow();
                if (lookahead < MIN_LOOKAHEAD && flush == Z_NO_FLUSH) {
                    return NEED_MORE;
                }
                if (lookahead == 0) break; // flush the current block
            }

            // Insert the string window[strstart .. strstart+2] in the
            // dictionary, and set hashHead to the head of the hash chain:

            if (lookahead >= MIN_MATCH) {
                insH = (((insH) << hashShift) ^ (window[(strStart) + (MIN_MATCH - 1)] & 0xff)) & hashMask;
//	prev[strstart&w_mask]=hashHead=head[ins_h];
                hashHead = (head[insH] & 0xffff);
                prev[strStart & wMask] = head[insH];
                head[insH] = (short) strStart;
            }

            // Find the longest match, discarding those <= prev_length.
            prevLength = matchLength;
            prevMatch = matchStart;
            matchLength = MIN_MATCH - 1;

            if (hashHead != 0 && prevLength < maxLazyMatch &&
                    ((strStart - hashHead) & 0xffff) <= wSize - MIN_LOOKAHEAD
            ) {
                // To simplify the code, we prevent matches with the string
                // of window index 0 (in particular we have to avoid a match
                // of the string with itself at the start of the input file).

                if (strategy != Z_HUFFMAN_ONLY) {
                    matchLength = longestMatch(hashHead);
                }
                // longest_match() sets match_start

                if (matchLength <= 5 && (strategy == Z_FILTERED ||
                        (matchLength == MIN_MATCH &&
                                strStart - matchStart > 4096))) {

                    // If prev_match is also MIN_MATCH, match_start is garbage
                    // but we will ignore the current match anyway.
                    matchLength = MIN_MATCH - 1;
                }
            }

            // If there was a match at the previous step and the current
            // match is not better, output the previous match:
            if (prevLength >= MIN_MATCH && matchLength <= prevLength) {
                int maxInsert = strStart + lookahead - MIN_MATCH;
                // Do not insert strings in hash table beyond this.

                //          check_match(strstart-1, prev_match, prev_length);

                bflush = trTally(strStart - 1 - prevMatch, prevLength - MIN_MATCH);

                // Insert in hash table all strings up to the end of the match.
                // strstart-1 and strstart are already inserted. If there is not
                // enough lookahead, the last two strings are not inserted in
                // the hash table.
                lookahead -= prevLength - 1;
                prevLength -= 2;
                do {
                    if (++strStart <= maxInsert) {
                        insH = (((insH) << hashShift) ^ (window[(strStart) + (MIN_MATCH - 1)] & 0xff)) & hashMask;
                        //prev[strstart&w_mask]=hashHead=head[ins_h];
                        hashHead = (head[insH] & 0xffff);
                        prev[strStart & wMask] = head[insH];
                        head[insH] = (short) strStart;
                    }
                }
                while (--prevLength != 0);
                matchAvailable = 0;
                matchLength = MIN_MATCH - 1;
                strStart++;

                if (bflush) {
                    flushBlockOnly(false);
                    if (strm.availOut == 0) return NEED_MORE;
                }
            } else if (matchAvailable != 0) {

                // If there was no match at the previous position, output a
                // single literal. If there was a match but the current match
                // is longer, truncate the previous match to a single literal.

                bflush = trTally(0, window[strStart - 1] & 0xff);

                if (bflush) {
                    flushBlockOnly(false);
                }
                strStart++;
                lookahead--;
                if (strm.availOut == 0) return NEED_MORE;
            } else {
                // There is no previous match to compare with, wait for
                // the next step to decide.

                matchAvailable = 1;
                strStart++;
                lookahead--;
            }
        }

        if (matchAvailable != 0) {
            trTally(0, window[strStart - 1] & 0xff);
            matchAvailable = 0;
        }
        flushBlockOnly(flush == Z_FINISH);

        if (strm.availOut == 0) {
            if (flush == Z_FINISH) return FINISH_STARTED;
            else return NEED_MORE;
        }

        return flush == Z_FINISH ? FINISH_DONE : BLOCK_DONE;
    }

    int longestMatch(int curMatch) {
        int chainLength = maxChainLength; // max hash chain length
        int scan = strStart;                 // current string
        int match;                           // matched string
        int len;                             // length of current match
        int bestLen = prevLength;          // best match length so far
        int limit = strStart > (wSize - MIN_LOOKAHEAD) ?
                strStart - (wSize - MIN_LOOKAHEAD) : 0;
        int niceMatch = this.niceMatch;

        // Stop when curMatch becomes <= limit. To simplify the code,
        // we prevent matches with the string of window index 0.

        int wmask = wMask;

        int strend = strStart + MAX_MATCH;
        byte scanEnd1 = window[scan + bestLen - 1];
        byte scanEnd = window[scan + bestLen];

        // The code is optimized for HASH_BITS >= 8 and MAX_MATCH-2 multiple of 16.
        // It is easy to get rid of this optimization if necessary.

        // Do not waste too much time if we already have a good match:
        if (prevLength >= goodMatch) {
            chainLength >>= 2;
        }

        // Do not look for matches beyond the end of the input. This is necessary
        // to make deflate deterministic.
        if (niceMatch > lookahead) niceMatch = lookahead;

        do {
            match = curMatch;

            // Skip to next match if the match length cannot increase
            // or if the match length is less than 2:
            if (window[match + bestLen] != scanEnd ||
                    window[match + bestLen - 1] != scanEnd1 ||
                    window[match] != window[scan] ||
                    window[++match] != window[scan + 1]) continue;

            // The check at bestLen-1 can be removed because it will be made
            // again later. (This heuristic is not always a win.)
            // It is not necessary to compare scan[2] and match[2] since they
            // are always equal when the other bytes match, given that
            // the hash keys are equal and that HASH_BITS >= 8.
            scan += 2;
            match++;

            // We check for insufficient lookahead only every 8th comparison;
            // the 256th check will be made at strstart+258.
            do {
            } while (window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    scan < strend);

            len = MAX_MATCH - (strend - scan);
            scan = strend - MAX_MATCH;

            if (len > bestLen) {
                matchStart = curMatch;
                bestLen = len;
                if (len >= niceMatch) break;
                scanEnd1 = window[scan + bestLen - 1];
                scanEnd = window[scan + bestLen];
            }

        } while ((curMatch = (prev[curMatch & wmask] & 0xffff)) > limit
                && --chainLength != 0);

        if (bestLen <= lookahead) return bestLen;
        return lookahead;
    }

    int deflateInit(int level, int bits, int memlevel) {
        return deflateInit(level, Z_DEFLATED, bits, memlevel,
                Z_DEFAULT_STRATEGY);
    }

    int deflateInit(int level, int bits) {
        return deflateInit(level, Z_DEFLATED, bits, DEF_MEM_LEVEL,
                Z_DEFAULT_STRATEGY);
    }

    int deflateInit(int level) {
        return deflateInit(level, MAX_WBITS);
    }

    private int deflateInit(int level, int method, int windowBits,
                            int memLevel, int strategy) {
        int wrap = 1;
        //    byte[] my_version=ZLIB_VERSION;

        //
        //  if (version == null || version[0] != my_version[0]
        //  || stream_size != sizeof(z_stream)) {
        //  return Z_VERSION_ERROR;
        //  }

        strm.msg = null;

        if (level == Z_DEFAULT_COMPRESSION) level = 6;

        if (windowBits < 0) { // undocumented feature: suppress zlib header
            wrap = 0;
            windowBits = -windowBits;
        } else if (windowBits > 15) {
            wrap = 2;
            windowBits -= 16;
            strm.adler = new CRC32();
        }

        if (memLevel < 1 || memLevel > MAX_MEM_LEVEL ||
                method != Z_DEFLATED ||
                windowBits < 9 || windowBits > 15 || level < 0 || level > 9 ||
                strategy < 0 || strategy > Z_HUFFMAN_ONLY) {
            return Z_STREAM_ERROR;
        }

        strm.dstate = this;

        this.wrap = wrap;
        wBits = windowBits;
        wSize = 1 << wBits;
        wMask = wSize - 1;

        hashBits = memLevel + 7;
        hashSize = 1 << hashBits;
        hashMask = hashSize - 1;
        hashShift = ((hashBits + MIN_MATCH - 1) / MIN_MATCH);

        window = new byte[wSize * 2];
        prev = new short[wSize];
        head = new short[hashSize];

        litBufSize = 1 << (memLevel + 6); // 16K elements by default

        // We overlay pending_buf and d_buf+l_buf. This works since the average
        // output size for (length,distance) codes is <= 24 bits.
        pendingBuf = new byte[litBufSize * 4];
        pendingBufSize = litBufSize * 4;

        dBuf = litBufSize / 2;
        lBuf = (1 + 2) * litBufSize;

        this.level = level;

        this.strategy = strategy;
        return deflateReset();
    }

    int deflateReset() {
        strm.totalIn = strm.totalOut = 0;
        strm.msg = null; //
        strm.dataType = Z_UNKNOWN;

        pending = 0;
        pendingOut = 0;

        if (wrap < 0) {
            wrap = -wrap;
        }
        status = (wrap == 0) ? BUSY_STATE : INIT_STATE;
        strm.adler.reset();

        lastFlush = Z_NO_FLUSH;

        trInit();
        lmInit();
        return Z_OK;
    }

    int deflateEnd() {
        if (status != INIT_STATE && status != BUSY_STATE && status != FINISH_STATE) {
            return Z_STREAM_ERROR;
        }
        // Deallocate in reverse order of allocations:
        pendingBuf = null;
        head = null;
        prev = null;
        window = null;
        // free
        // dstate=null;
        return status == BUSY_STATE ? Z_DATA_ERROR : Z_OK;
    }

    int deflateParams(int levelParam, int strategyParam) {
        int err = Z_OK;

        if (levelParam == Z_DEFAULT_COMPRESSION) {
            levelParam = 6;
        }
        if (levelParam < 0 || levelParam > 9 ||
                strategyParam < 0 || strategyParam > Z_HUFFMAN_ONLY) {
            return Z_STREAM_ERROR;
        }

        if (CONFIG_TABLE[level].func != CONFIG_TABLE[levelParam].func &&
                strm.totalIn != 0) {
            // Flush the last buffer:
            err = strm.deflate(Z_PARTIAL_FLUSH);
        }

        if (level != levelParam) {
            level = levelParam;
            maxLazyMatch = CONFIG_TABLE[level].maxLazy;
            goodMatch = CONFIG_TABLE[level].goodLength;
            niceMatch = CONFIG_TABLE[level].niceLength;
            maxChainLength = CONFIG_TABLE[level].maxChain;
        }
        strategy = strategyParam;
        return err;
    }

    int deflateSetDictionary(byte[] dictionary, int dictLength) {
        int length = dictLength;
        int index = 0;

        if (dictionary == null || status != INIT_STATE)
            return Z_STREAM_ERROR;

        strm.adler.update(dictionary, 0, dictLength);

        if (length < MIN_MATCH) return Z_OK;
        if (length > wSize - MIN_LOOKAHEAD) {
            length = wSize - MIN_LOOKAHEAD;
            index = dictLength - length; // use the tail of the dictionary
        }
        System.arraycopy(dictionary, index, window, 0, length);
        strStart = length;
        blockStart = length;

        // Insert all strings in the hash table (except for the last two bytes).
        // s->lookahead stays null, so s->ins_h will be recomputed at the next
        // call of fill_window.

        insH = window[0] & 0xff;
        insH = (((insH) << hashShift) ^ (window[1] & 0xff)) & hashMask;

        for (int n = 0; n <= length - MIN_MATCH; n++) {
            insH = (((insH) << hashShift) ^ (window[(n) + (MIN_MATCH - 1)] & 0xff)) & hashMask;
            prev[n & wMask] = head[insH];
            head[insH] = (short) n;
        }
        return Z_OK;
    }

    int deflate(int flush) {
        int oldFlush;

        if (flush > Z_FINISH || flush < 0) {
            return Z_STREAM_ERROR;
        }

        if (strm.nextOut == null ||
                (strm.nextIn == null && strm.availIn != 0) ||
                (status == FINISH_STATE && flush != Z_FINISH)) {
            strm.msg = Z_ERRMSG[Z_NEED_DICT - (Z_STREAM_ERROR)];
            return Z_STREAM_ERROR;
        }
        if (strm.availOut == 0) {
            strm.msg = Z_ERRMSG[Z_NEED_DICT - (Z_BUF_ERROR)];
            return Z_BUF_ERROR;
        }

        oldFlush = lastFlush;
        lastFlush = flush;

        // Write the zlib header
        if (status == INIT_STATE) {
            if (wrap == 2) {
                getGZIPHeader().put(this);
                status = BUSY_STATE;
                strm.adler.reset();
            } else {
                int header = (Z_DEFLATED + ((wBits - 8) << 4)) << 8;
                int levelFlags = ((level - 1) & 0xff) >> 1;

                if (levelFlags > 3) levelFlags = 3;
                header |= (levelFlags << 6);
                if (strStart != 0) header |= PRESET_DICT;
                header += 31 - (header % 31);

                status = BUSY_STATE;
                putShortMSB(header);


                // Save the adler32 of the preset dictionary:
                if (strStart != 0) {
                    long adler = strm.adler.getValue();
                    putShortMSB((int) (adler >>> 16));
                    putShortMSB((int) (adler & 0xffff));
                }
                strm.adler.reset();
            }
        }

        // Flush as much pending output as possible
        if (pending != 0) {
            strm.flushPending();
            if (strm.availOut == 0) {
                // Since avail_out is 0, deflate will be called again with
                // more output space, but possibly with both pending and
                // avail_in equal to zero. There won't be anything to do,
                // but this is not an error situation so make sure we
                // return OK instead of BUF_ERROR at next call of deflate:
                lastFlush = -1;
                return Z_OK;
            }

            // Make sure there is something to do and avoid duplicate consecutive
            // flushes. For repeated and useless calls with Z_FINISH, we keep
            // returning Z_STREAM_END instead of Z_BUFF_ERROR.
        } else if (strm.availIn == 0 && flush <= oldFlush &&
                flush != Z_FINISH) {
            strm.msg = Z_ERRMSG[Z_NEED_DICT - (Z_BUF_ERROR)];
            return Z_BUF_ERROR;
        }

        // User must not provide more input after the first FINISH:
        if (status == FINISH_STATE && strm.availIn != 0) {
            strm.msg = Z_ERRMSG[Z_NEED_DICT - (Z_BUF_ERROR)];
            return Z_BUF_ERROR;
        }

        // Start a new block or continue the current one.
        if (strm.availIn != 0 || lookahead != 0 ||
                (flush != Z_NO_FLUSH && status != FINISH_STATE)) {
            int bstate = -1;
            switch (CONFIG_TABLE[level].func) {
                case STORED:
                    bstate = deflateStored(flush);
                    break;
                case FAST:
                    bstate = deflateFast(flush);
                    break;
                case SLOW:
                    bstate = deflateSlow(flush);
                    break;
                default:
            }

            if (bstate == FINISH_STARTED || bstate == FINISH_DONE) {
                status = FINISH_STATE;
            }
            if (bstate == NEED_MORE || bstate == FINISH_STARTED) {
                if (strm.availOut == 0) {
                    lastFlush = -1; // avoid BUF_ERROR next call, see above
                }
                return Z_OK;
                // If flush != Z_NO_FLUSH && avail_out == 0, the next call
                // of deflate should use the same flush parameter to make sure
                // that the flush is complete. So we don't have to output an
                // empty block here, this will be done at next call. This also
                // ensures that for a very small output buffer, we emit at most
                // one empty block.
            }

            if (bstate == BLOCK_DONE) {
                if (flush == Z_PARTIAL_FLUSH) {
                    trAlign();
                } else { // FULL_FLUSH or SYNC_FLUSH
                    trStoredBlock(0, 0, false);
                    // For a full flush, this empty block will be recognized
                    // as a special marker by inflate_sync().
                    if (flush == Z_FULL_FLUSH) {
                        //state.head[s.hash_size-1]=0;
                        for (int i = 0; i < hashSize/*-1*/; i++)  // forget history
                            head[i] = 0;
                    }
                }
                strm.flushPending();
                if (strm.availOut == 0) {
                    lastFlush = -1; // avoid BUF_ERROR at next call, see above
                    return Z_OK;
                }
            }
        }

        if (flush != Z_FINISH) return Z_OK;
        if (wrap <= 0) return Z_STREAM_END;

        if (wrap == 2) {
            long adler = strm.adler.getValue();
            putByte((byte) (adler & 0xff));
            putByte((byte) ((adler >> 8) & 0xff));
            putByte((byte) ((adler >> 16) & 0xff));
            putByte((byte) ((adler >> 24) & 0xff));
            putByte((byte) (strm.totalIn & 0xff));
            putByte((byte) ((strm.totalIn >> 8) & 0xff));
            putByte((byte) ((strm.totalIn >> 16) & 0xff));
            putByte((byte) ((strm.totalIn >> 24) & 0xff));

            getGZIPHeader().setCRC(adler);
        } else {
            // Write the zlib trailer (adler32)
            long adler = strm.adler.getValue();
            putShortMSB((int) (adler >>> 16));
            putShortMSB((int) (adler & 0xffff));
        }

        strm.flushPending();

        // If avail_out is zero, the application will call deflate again
        // to flush the rest.

        if (wrap > 0) wrap = -wrap; // write the trailer only once!
        return pending != 0 ? Z_OK : Z_STREAM_END;
    }

    @Override
    public Object clone() {
        Deflate dest = new Deflate(strm);

        dest.pendingBuf = dup(dest.pendingBuf);
        dest.window = dup(dest.window);

        dest.prev = dup(dest.prev);
        dest.head = dup(dest.head);
        dest.dynLtree = dup(dest.dynLtree);
        dest.dynDtree = dup(dest.dynDtree);
        dest.blTree = dup(dest.blTree);

        dest.blCount = dup(dest.blCount);
        dest.nextCode = dup(dest.nextCode);
        dest.heap = dup(dest.heap);
        dest.depth = dup(dest.depth);

        dest.lDesc.dynTree = dest.dynLtree;
        dest.dDesc.dynTree = dest.dynDtree;
        dest.blDesc.dynTree = dest.blTree;

        if (dest.gheader != null) {
            dest.gheader = (GZIPHeader) dest.gheader.clone();
        }

        return dest;
    }

    private byte[] dup(byte[] buf) {
        byte[] foo = new byte[buf.length];
        System.arraycopy(buf, 0, foo, 0, foo.length);
        return foo;
    }

    private short[] dup(short[] buf) {
        short[] foo = new short[buf.length];
        System.arraycopy(buf, 0, foo, 0, foo.length);
        return foo;
    }

    private int[] dup(int[] buf) {
        int[] foo = new int[buf.length];
        System.arraycopy(buf, 0, foo, 0, foo.length);
        return foo;
    }

    synchronized GZIPHeader getGZIPHeader() {
        if (gheader == null) {
            gheader = new GZIPHeader();
        }
        return gheader;
    }

    static class Config {
        int goodLength; // reduce lazy search above this match length
        int maxLazy;    // do not perform lazy search above this match length
        int niceLength; // quit search above this match length
        int maxChain;
        int func;

        Config(int goodLength, int maxLazy,
               int niceLength, int maxChain, int func) {
            this.goodLength = goodLength;
            this.maxLazy = maxLazy;
            this.niceLength = niceLength;
            this.maxChain = maxChain;
            this.func = func;
        }
    }
}
