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
package com.codenameone.examples.bastion.domain;

import com.codename1.annotations.Column;
import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

/// Coins crossing between the bank and a blockchain, in either direction.
///
/// A withdrawal (`OUT`) moves through QUEUED (the ledger has taken the coins
/// from the customer into the in-flight account), SIGNED (a transaction with
/// a fixed nonce exists and `raw` holds it, so a crash re-broadcasts the same
/// bytes rather than signing a second spend), BROADCAST, and then CONFIRMED
/// or FAILED, each final state posting once to the ledger. A deposit (`IN`)
/// is written only when it is already deep enough to credit, CONFIRMED from
/// birth; `(txHash, logIndex)` is unique, so a rescan cannot credit it twice.
@Entity
public class ChainTransfer {
    @Id(autoIncrement = false)
    @Column(nullable = false, type = "VARCHAR(32)")
    private String id = "";

    /// `IN` or `OUT`.
    @Column(nullable = false, type = "VARCHAR(3)")
    private String direction = "";

    @Column(nullable = false, type = "VARCHAR(190)")
    private String owner = "";

    @Column(nullable = false, type = "VARCHAR(12)")
    private String asset = "";

    /// The outside address: where an OUT pays, or the deposit address an IN arrived at.
    @Column(nullable = false, type = "VARCHAR(42)")
    private String address = "";

    /// Ledger units (gwei for ETH).
    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, type = "VARCHAR(12)")
    private String status = "";

    /// Empty until signed.
    @Column(nullable = false, type = "VARCHAR(66)")
    private String txHash = "";

    /// 0 for a plain value transfer; reserved for token transfers, which arrive as logs.
    @Column(nullable = false)
    private int logIndex;

    /// The hot wallet nonce an OUT was signed with, or -1.
    @Column(nullable = false)
    private long nonce;

    /// The signed transaction, hex, so a re-broadcast is the same bytes.
    @Column(nullable = false, type = "VARCHAR(4000)")
    private String raw = "";

    @Column(nullable = false)
    private long blockNumber;

    @Column(nullable = false, type = "VARCHAR(66)")
    private String blockHash = "";

    /// Network fee paid, ledger units, rounded up.
    @Column(nullable = false)
    private long fee;

    /// The CryptoAddress an OUT pays, or the DepositAddress an IN reached.
    @Column(nullable = false, type = "VARCHAR(32)")
    private String reference = "";

    /// Why it failed, for the customer and the back office.
    @Column(nullable = false, type = "VARCHAR(200)")
    private String detail = "";

    @Column(nullable = false)
    private long broadcastAt;

    @Column(nullable = false)
    private long createdAt;

    @Column(nullable = false)
    private long updatedAt;

    public ChainTransfer() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public int getLogIndex() {
        return logIndex;
    }

    public void setLogIndex(int logIndex) {
        this.logIndex = logIndex;
    }

    public long getNonce() {
        return nonce;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public long getFee() {
        return fee;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public long getBroadcastAt() {
        return broadcastAt;
    }

    public void setBroadcastAt(long broadcastAt) {
        this.broadcastAt = broadcastAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
