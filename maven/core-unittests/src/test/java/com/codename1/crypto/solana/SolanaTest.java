/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.solana;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Solana vectors from the `@solana/web3.js` test suite.
class SolanaTest extends UITestBase {
    private static final SolanaAddress BPF = SolanaAddress.parse("BPFLoader1111111111111111111111111111111111");

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void programAddresses() {
        assertEquals("3gF2KMe9KiC6FNVBmfg9i267aMPvK37FewCip4eGBFcT",
                SolanaAddress.createProgramAddress(new byte[][] {new byte[0], {1}}, BPF).toString());
        assertEquals("7ytmC1nT1xY4RfxCV2ZgyA7UakC93do5ZdyhdF3EtPj7",
                SolanaAddress.createProgramAddress(new byte[][] {{(byte) 0xe2, (byte) 0x98, (byte) 0x89}}, BPF)
                        .toString());
        assertEquals("HwRVBufQ4haG5XSgpspwKtNd3PC9GM9m1196uJW36vds",
                SolanaAddress.createProgramAddress(new byte[][] {utf8("Talking"), utf8("Squirrels")}, BPF)
                        .toString());
        assertEquals("GUs5qLUfsEHkcMB9T38vjr18ypEhRuNWiePW2LoK4E3K", SolanaAddress.createProgramAddress(
                new byte[][] {SolanaAddress.parse("SeedPubey1111111111111111111111111111111111").toBytes()}, BPF)
                .toString());
        byte[] two = new byte[8];
        two[0] = 2;
        assertEquals("12rqwuEgBYiGhBrDJStCiqEtzQpTTiZbh7teNVLuYcFA", SolanaAddress.createProgramAddress(
                new byte[][] {SolanaAddress.parse("H4snTKK9adiU15gP22ErfZYtro3aqR9BTMXiH3AwiUTQ").toBytes(), two},
                SolanaAddress.parse("4ckmDgGdxQoPDLUkDT3vHgSAkzA3QRdNq5ywwY4sUSJn")).toString());
        assertEquals("9h1HyLCW5dZnBVap8C5egQ9Z6pHyjsh5MNy83iPqqRuq", SolanaAddress.createWithSeed(
                SolanaAddress.SYSTEM_PROGRAM, "limber chicken: 4/45", SolanaAddress.SYSTEM_PROGRAM).toString());

        SolanaAddress.ProgramAddress found = SolanaAddress.findProgramAddress(new byte[][] {new byte[0]}, BPF);
        assertFalse(found.getAddress().isOnCurve());
        assertEquals(found.getAddress(), SolanaAddress.createProgramAddress(
                new byte[][] {new byte[0], {(byte) found.getBump()}}, BPF));
        assertTrue(SolanaAddress.fromPrivateKey(new byte[32]).isOnCurve());
        assertFalse(SolanaAddress.isValid("0OIl"));
    }

    @Test
    void signedTransfer() {
        byte[] seed = new byte[32];
        Arrays.fill(seed, (byte) 8);
        SolanaAddress sender = SolanaAddress.fromPrivateKey(seed);
        SolanaAddress to = SolanaAddress.parse("J3dxNj7nDRRqRRXuEMynDG57DkZK4jYRuv3Garmb1i99");
        List<Instruction> ixs = new ArrayList<Instruction>();
        ixs.add(SystemProgram.transfer(sender, to, 49));
        SolanaTransaction tx = new SolanaTransaction(
                SolanaMessage.compile(sender, ixs, "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k"));
        assertFalse(tx.isFullySigned());
        tx.sign(seed);
        assertTrue(tx.isFullySigned());
        String expected = "AVuErQHaXv0SG0/PchunfxHKt8wMRfMZzqV0tkC5qO6owYxWU2v871AoWywGoFQr4z+q/7mE8lIufNl/kxj+nQ0BAAEDE5j2"
                + "LG0aRXxRumpLXz29L2n8qTIWIY3ImX5Ba9F9k8r9Q5/Mtmcn8onFxt47xKj+XdXXd3C8j/FcPu7csUrz/AAAAAAAAAAAAAAA"
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAxJrndgN4IFTxep3s6kO0ROug7bEsbx0xxuDkqEvwUusBAgIAAQwCAAAAMQAAAAAAAAA=";
        assertEquals(expected, tx.toBase64());

        SolanaTransaction back = SolanaTransaction.fromBase64(expected);
        assertTrue(back.isFullySigned());
        assertEquals(tx.getId(), back.getId());
        List<Instruction> parsed = back.getMessage().getInstructions();
        assertEquals(1, parsed.size());
        assertEquals(SolanaAddress.SYSTEM_PROGRAM, parsed.get(0).getProgramId());
        assertTrue(Arrays.equals(ixs.get(0).getData(), parsed.get(0).getData()));
        assertTrue(parsed.get(0).getAccounts().get(0).isSigner());
        assertTrue(parsed.get(0).getAccounts().get(1).isWritable());
        assertFalse(parsed.get(0).getAccounts().get(1).isSigner());
    }

    @Test
    void v0TokenTransferRoundTrips() {
        byte[] payerSeed = new byte[32];
        payerSeed[0] = 1;
        byte[] ownerSeed = new byte[32];
        ownerSeed[0] = 2;
        SolanaAddress payer = SolanaAddress.fromPrivateKey(payerSeed);
        SolanaAddress owner = SolanaAddress.fromPrivateKey(ownerSeed);
        SolanaAddress mint = SolanaAddress.parse("EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v");
        SolanaAddress recipient = SolanaAddress.parse("J3dxNj7nDRRqRRXuEMynDG57DkZK4jYRuv3Garmb1i99");
        SolanaAddress src = SolanaAddress.associatedTokenAddress(owner, mint);
        SolanaAddress dst = SolanaAddress.associatedTokenAddress(recipient, mint);
        assertFalse(src.isOnCurve());
        List<Instruction> ixs = new ArrayList<Instruction>();
        ixs.add(ComputeBudget.setComputeUnitPrice(1000));
        ixs.add(TokenProgram.createAssociatedTokenAccount(payer, recipient, mint, true));
        ixs.add(TokenProgram.transferChecked(src, mint, dst, owner, 2500000L, 6));
        ixs.add(ComputeBudget.memo("invoice 7", owner));
        SolanaMessage m = SolanaMessage.compileV0(payer, ixs, "EETubP5AKHgjPAhzPAFcb8BAY1hMH639CWCFTqi3hq1k");
        assertEquals(0, m.getVersion());
        assertEquals(2, m.getNumRequiredSignatures());
        assertEquals(payer, m.getFeePayer());
        assertEquals(owner, m.getAccountKeys().get(1));
        assertFalse(m.isWritable(1));
        SolanaTransaction tx = new SolanaTransaction(m);
        tx.sign(ownerSeed);
        assertFalse(tx.isFullySigned());
        SolanaTransaction passed = SolanaTransaction.fromBase64(tx.toBase64());
        passed.sign(payerSeed);
        assertTrue(passed.isFullySigned());
        assertEquals(0x80, passed.getMessageBytes()[0] & 0xff);
        assertEquals(Base64.getEncoder().encodeToString(passed.serialize()), SolanaTransaction.parse(passed.serialize()).toBase64());
        List<Instruction> back = passed.getMessage().getInstructions();
        assertEquals(4, back.size());
        assertTrue(Arrays.equals(ixs.get(2).getData(), back.get(2).getData()));
        try {
            tx.sign(new byte[32]);
            fail("not a signer");
        } catch (IllegalArgumentException expected) {
            // a stranger's key has no slot
        }
    }
}
