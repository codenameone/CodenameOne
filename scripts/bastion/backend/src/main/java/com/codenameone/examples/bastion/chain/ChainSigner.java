/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.chain;

/// Everything that touches a chain private key, behind one interface.
///
/// [SeedSigner] keeps the seed in this process, which is right for a test
/// network and wrong for real money. A product implements this over an HSM or
/// a cloud key service that holds secp256k1 keys and signs a 32-byte digest
/// (AWS KMS and Google Cloud KMS both offer `ECC_SECG_P256K1`): the key then
/// never exists in the server's memory, and every signature is logged by a
/// system the application cannot rewrite. The service answers DER; recover
/// the id by trying both values against the known public key.
public interface ChainSigner {
    /// The address withdrawals are paid from.
    String hotAddress();

    /// The customer deposit address at `index`.
    String depositAddress(long index);

    /// r || s || recovery id over `hash32`, with the hot wallet key.
    byte[] signWithHot(byte[] hash32);

    /// The same with the deposit key at `index`, to sweep it.
    byte[] signWithDeposit(long index, byte[] hash32);
}
