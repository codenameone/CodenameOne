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

import com.codename1.backend.DataSource;
import java.time.Instant;
import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codenameone.examples.bastion.account.DemoAccounts;
import com.codenameone.examples.bastion.api.CustodyDto;
import com.codenameone.examples.bastion.domain.ChainTransfer;
import com.codenameone.examples.bastion.domain.CryptoAddress;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codenameone.examples.bastion.ledger.Leg;
import com.codenameone.examples.bastion.money.Money;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Real Ether on a real node: a development chain (geth --dev), which mines a
/// block a second and starts with one funded, unlocked account. Runs only
/// when `BASTION_CHAIN_RPC_URL` names one; `scripts/bastion/run-chain-test.sh`
/// starts the node and sets everything this needs, and CI runs it in the
/// `chain` job.
///
/// Everything the bank does here is what it would do on mainnet; only the
/// test's own funding uses the node's unlocked account.
@BackendTest
class ChainIntegrationTest {
    private static final String RPC = System.getenv("BASTION_CHAIN_RPC_URL");
    private static final long ONE_ETH = 1000000000L;

    @Autowired
    private Chain chain;
    @Autowired
    private Money money;
    @Autowired
    private Ledger ledger;
    @Autowired
    private DataSource db;

    @BeforeAll
    static void needsANode() {
        Assumptions.assumeTrue(RPC != null && RPC.length() > 0,
                "set BASTION_CHAIN_RPC_URL (scripts/bastion/run-chain-test.sh) to run");
    }

    @Test
    void etherCrossesTheChainBothWaysAndTheBooksAgree() throws Exception {
        assertTrue(chain.enabled());
        String owner = DemoAccounts.CUSTOMER;
        String deposit = chain.depositAddress(owner).getAddress();
        assertEquals(deposit, chain.depositAddress(owner).getAddress(), "one address per customer");

        // A deposit of 1.5 ETH, from outside the bank.
        String funding = fund(deposit, "0x14d1120d7b160000");
        until("the deposit is credited", () -> credited(funding));
        LedgerAccount wallet = wallet(owner);
        assertEquals(15L * ONE_ETH / 10, wallet.getBalance());

        // Swept into the hot wallet, minus its own fee.
        until("the sweep is final", () -> swept());
        assertTrue(balance(chain.hotWallet()) > ONE_ETH, "the hot wallet holds the deposit");

        // The sweep's fee left the chain while the customer is still owed the
        // whole deposit, so custody is short by that fee: the bank pays network
        // fees from a float of its own. Until it funds one nothing goes out.
        assertTrue(chain.custody() < chain.custodyReport().owed(), "a sweep fee is the bank's to cover");
        final long before = balance(chain.hotWallet());
        fund(chain.hotWallet(), "0x2386f26fc10000"); // 0.01 ETH
        until("the float arrives", () -> balanceQuietly(chain.hotWallet()) > before);

        // A withdrawal of 0.4 ETH to an outside address, past its cooling-off.
        String outside = "0x" + com.codename1.crypto.encoding.Hex.encode(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                13, 14, 15, 16, 17, 18, 19, 20});
        CryptoAddress to = new CryptoAddress();
        to.setId("chaintest-address");
        to.setOwner(owner);
        to.setAsset("ETH");
        to.setAddress(outside);
        to.setLabel("test");
        to.setUsableAt(Instant.EPOCH);
        to.setCreatedAt(Instant.now());
        // In memory only: the endpoint's allow-list and cooling-off checks
        // are ExchangeTest's subject, not this one's.
        String entry = money.withdrawCrypto(owner, "chain-test-withdrawal-1", wallet,
                to, 4L * ONE_ETH / 10).entryId();
        assertEquals(11L * ONE_ETH / 10, wallet(owner).getBalance());
        until("the withdrawal is final", () -> status(entry).equals(Chain.CONFIRMED));
        // Exactly the amount arrived, on the chain, at the address.
        assertEquals(4L * ONE_ETH / 10, balance(outside));
        assertEquals(0L, ledger.system("ETH", AccountKinds.SYS_CHAIN_TRANSIT).getBalance());
        ChainTransfer out = chain.transfer(entry);
        assertTrue(out.getTxHash().startsWith("0x") && out.getFee() > 0);

        // Reserves: the chain holds at least what the ledger owes.
        CustodyDto custody = chain.custodyReport();
        assertTrue(custody.onChain() >= custody.owed(), custody.toString());
        assertTrue(ledger.verify(new ArrayList<String>(Arrays.asList(owner))).isEmpty());

        // Coins minted in the ledger without a deposit: no withdrawal is
        // signed until a person looks.
        LedgerAccount cash = ledger.system("ETH", AccountKinds.SYS_CASH);
        ledger.post("chain-test-unbacked-mint", "DEPOSIT", owner, "not backed",
                Arrays.asList(new Leg(cash.getId(), -100L * ONE_ETH), new Leg(wallet.getId(), 100L * ONE_ETH)));
        String held = money.withdrawCrypto(owner, "chain-test-withdrawal-2", wallet(owner), to,
                ONE_ETH / 10).entryId();
        chain.tick();
        chain.tick();
        ChainTransfer stuck = chain.transfer(held);
        assertEquals(Chain.QUEUED, stuck.getStatus());
        assertTrue(stuck.getDetail().contains("owes"), stuck.getDetail());
    }

    private boolean credited(String txHash) {
        chain.tick();
        try {
            for (ChainTransfer t : chain.transfers()) {
                if (Chain.IN.equals(t.getDirection()) && txHash.equals(t.getTxHash())) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean swept() {
        chain.tick();
        try {
            for (ChainTransfer t : chain.transfers()) {
                if (Chain.SWEEP.equals(t.getDirection()) && Chain.CONFIRMED.equals(t.getStatus())) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    private String status(String id) {
        chain.tick();
        try {
            return chain.transfer(id).getStatus();
        } catch (Exception e) {
            return "";
        }
    }

    private LedgerAccount wallet(String owner) throws Exception {
        for (LedgerAccount a : ledger.accountsOf(owner)) {
            if ("ETH".equals(a.getAsset()) && AccountKinds.WALLET.equals(a.getKind())) {
                return a;
            }
        }
        throw new AssertionError("no ETH wallet");
    }

    private long balanceQuietly(String address) {
        try {
            return balance(address);
        } catch (Exception e) {
            return -1;
        }
    }

    private void until(String what, BooleanSupplier done) throws Exception {
        long end = System.currentTimeMillis() + 120000L;
        while (!done.getAsBoolean()) {
            if (System.currentTimeMillis() > end) {
                // A tick records why it failed in the audit; without it a
                // timeout says nothing about the cause.
                throw new AssertionError("timed out waiting for " + what + "; chain failures: "
                        + db.query("SELECT detail FROM audit_event WHERE action LIKE 'CHAIN%'"
                        + " ORDER BY seq DESC", new Object[0]));
            }
            Thread.sleep(1000);
        }
    }

    /// Sends `wei` from the development chain's unlocked account.
    @SuppressWarnings("unchecked")
    private static String fund(String to, String wei) throws Exception {
        List<Object> accounts = (List<Object>) rpc("eth_accounts");
        Map<String, Object> tx = new HashMap<String, Object>();
        tx.put("from", accounts.get(0));
        tx.put("to", to);
        tx.put("value", wei);
        return (String) rpc("eth_sendTransaction", tx);
    }

    private static long balance(String address) throws Exception {
        return Chain.gwei(com.codename1.crypto.UInt256.parse((String) rpc("eth_getBalance", address, "latest")));
    }

    private static Object rpc(String method, Object... params) throws Exception {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("jsonrpc", "2.0");
        request.put("id", Integer.valueOf(1));
        request.put("method", method);
        request.put("params", new ArrayList<Object>(Arrays.asList(params)));
        Web.Result r = Web.postJson(RPC, Json.write(request), null);
        Map answer = Json.parseObject(r.getBodyAsString());
        if (answer.get("error") != null) {
            throw new AssertionError(method + ": " + answer.get("error"));
        }
        return answer.get("result");
    }
}
