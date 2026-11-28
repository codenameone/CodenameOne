/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

// The StoreKit 2 path end to end, on the simulator, against StoreKitTest's
// local App Store (Products.storekit): no account, no network, no dialogs.
//
// The test cannot call Java, so it drives the app's real purchase API through
// a command queue (PurchaseTestSink.takeCommand, polled by PurchaseTestApp)
// and reads back every callback, receipt and product listing the app records
// (PurchaseTestSink.recordEvent). Both live in NSUserDefaults, which the hosted
// test shares with the app.
//
// One method, run in order, because each stage builds on the last: what is
// bought is later refunded and restored.

import XCTest
import StoreKit
import StoreKitTest

private let eventsKey = "CN1IAPTestEvents"
private let commandsKey = "CN1IAPTestCommands"
private let lifetime = "com.codenameone.hello.lifetime"
private let monthly = "com.codenameone.hello.monthly"
private let consumable = "com.codenameone.hello.pro"

@available(iOS 17.0, *)
final class PurchaseStoreKit2Tests: XCTestCase {
    private var session: SKTestSession!

    override func setUpWithError() throws {
        continueAfterFailure = false
        session = try SKTestSession(configurationFileNamed: "Products")
        session.disableDialogs = true
        session.askToBuyEnabled = false
        session.failTransactionsEnabled = false
        session.clearTransactions()
        UserDefaults.standard.removeObject(forKey: eventsKey)
        UserDefaults.standard.removeObject(forKey: commandsKey)
        // The app boots its VM asynchronously; resetState answers once the
        // command channel is being polled.
        command("resetState")
        waitFor("stateReset", timeout: 90)
        UserDefaults.standard.removeObject(forKey: eventsKey)
    }

    override func tearDown() {
        command("resetState")
        _ = wait({ self.events().contains("stateReset") }, timeout: 20)
        session?.clearTransactions()
        session = nil
        super.tearDown()
    }

    func testStoreKit2Lifecycle() throws {
        // A build that lost the bridge falls back to StoreKit 1 and would
        // still pass most of what follows.
        XCTAssertNotNil(NSClassFromString("CN1StoreKit2"), "the app was built without the StoreKit 2 bridge")

        // Payments allowed.
        command("canMakePayments")
        waitFor("canMakePayments true")

        // Products come back in request order, matched by sku, with an
        // unknown sku in the middle left empty rather than shifting the rest.
        command("products:\(monthly),does.not.exist,\(lifetime)")
        let listing = waitForPrefix("products;")
        let parts = listing.split(separator: ";").map(String.init)
        XCTAssertEqual(parts.count, 4, listing)
        XCTAssertTrue(parts[1].hasPrefix("\(monthly)|CN1 Test Monthly|"), listing)
        XCTAssertTrue(parts[1].contains("1.99"), listing)
        XCTAssertEqual(parts[2], "does.not.exist|null|null", listing)
        XCTAssertTrue(parts[3].hasPrefix("\(lifetime)|CN1 Test Lifetime|"), listing)
        XCTAssertTrue(parts[3].contains("4.99"), listing)

        // A purchase the app starts: the callback, a receipt whose order data
        // is the signed transaction the commerce service decodes, and the
        // transaction finished once the receipt is stored.
        command("purchase:\(lifetime)")
        waitFor("itemPurchased \(lifetime)")
        let lifetimeReceipt = waitForPrefix("receipt \(lifetime) ")
        let lifetimeId = try XCTUnwrap(receiptTransactionId(lifetimeReceipt))
        XCTAssertTrue(lifetimeReceipt.hasSuffix(" jws:\(lifetime):\(lifetimeId)"),
                      "order data is not the StoreKit 2 JWS: \(lifetimeReceipt)")
        assertFinished(lifetime)

        // A subscription, bought in the app.
        command("subscribe:\(monthly)")
        waitFor("itemPurchased \(monthly)")
        let monthlyReceipt = waitForPrefix("receipt \(monthly) ")
        XCTAssertTrue(monthlyReceipt.contains(" jws:\(monthly):"), monthlyReceipt)
        assertFinished(monthly)

        // Its renewal happens outside the app and reaches it only through
        // Transaction.updates, with no command: a second receipt, for a new
        // transaction. (SKTestSession.buyProduct cannot stand in for this: it
        // runs in the app's own process, StoreKit counts it as the app's
        // purchase, and updates never emits it.)
        try session.forceRenewalOfSubscription(productIdentifier: monthly)
        XCTAssertTrue(wait({ self.events().filter { $0.hasPrefix("receipt \(monthly) ") }.count >= 2 },
                           timeout: 30), "the renewal never reached the app: \(events())")
        let renewal = events().filter { $0.hasPrefix("receipt \(monthly) ") }[1]
        XCTAssertNotEqual(receiptTransactionId(renewal), receiptTransactionId(monthlyReceipt))
        assertFinished(monthly)

        // A purchase the store fails reports an error, grants nothing.
        // failTransactionsEnabled is StoreKit 1's switch; StoreKit 2's
        // failures are simulated per API (iOS 17).
        try simulate(.generic(.unknown), StoreKitPurchaseAPI())
        command("purchase:\(consumable)")
        waitForPrefix("itemPurchaseError \(consumable)")
        try simulate(nil, StoreKitPurchaseAPI())
        XCTAssertFalse(events().contains("itemPurchased \(consumable)"), "\(events())")

        // Ask to Buy: nothing is granted while it waits, and the approved
        // transaction arrives later through Transaction.updates.
        session.askToBuyEnabled = true
        command("purchase:\(consumable)")
        let pending = wait({
            self.session.allTransactions().contains { $0.productIdentifier == consumable && $0.pendingAskToBuyConfirmation }
        }, timeout: 30)
        XCTAssertTrue(pending, "no Ask to Buy transaction was created: \(session.allTransactions())")
        XCTAssertFalse(events().contains("itemPurchased \(consumable)"), "granted before approval: \(events())")
        let ask = try XCTUnwrap(session.allTransactions().first {
            $0.productIdentifier == consumable && $0.pendingAskToBuyConfirmation
        })
        try session.approveAskToBuyTransaction(identifier: ask.identifier)
        session.askToBuyEnabled = false
        waitFor("itemPurchased \(consumable)")
        waitForPrefix("receipt \(consumable) ")
        assertFinished(consumable)

        // A refund reaches the app as itemRefunded.
        try session.refundTransaction(identifier: UInt(lifetimeId)!)
        waitFor("itemRefunded \(lifetime)")

        // Restore reports what the account still owns, and not the refunded
        // non-consumable, then completes.
        command("restore")
        waitFor("restoreRequestComplete")
        XCTAssertTrue(events().contains("itemRestored \(monthly)"), "\(events())")
        XCTAssertFalse(events().contains("itemRestored \(lifetime)"), "refunded item restored: \(events())")

        // Each transaction was delivered once, however many StoreKit sources
        // (purchase result, updates, unfinished) reported it.
        let all = events()
        for (sku, times) in [(lifetime, 1), (monthly, 2), (consumable, 1)] {
            XCTAssertEqual(all.filter { $0 == "itemPurchased \(sku)" }.count, times, "\(sku): \(all)")
        }
        let receiptIds = all.filter { $0.hasPrefix("receipt ") }.compactMap(receiptTransactionId)
        XCTAssertEqual(receiptIds.count, Set(receiptIds).count, "a receipt was submitted twice: \(all)")
        XCTAssertFalse(all.contains { $0.hasPrefix("receipt ") && !$0.contains(" jws:") },
                       "a receipt without the StoreKit 2 JWS: \(all)")

        // Last, because it leaves a transaction StoreKit will offer again: a
        // purchase whose signature does not verify grants nothing and reports
        // an error.
        try simulate(.verification(.invalidSignature), StoreKitVerificationAPI())
        command("purchase:\(lifetime)")
        waitForPrefix("itemPurchaseError \(lifetime)")
        try simulate(nil, StoreKitVerificationAPI())
        XCTAssertEqual(events().filter { $0 == "itemPurchased \(lifetime)" }.count, 1,
                       "an unverified purchase was granted: \(events())")
    }

    // MARK: helpers

    private func simulate<API: FailableStoreKitAPI>(_ failure: API.Failure?, _ api: API) throws {
        let session = self.session!
        var error: Error?
        var done = false
        Task {
            do {
                try await session.setSimulatedError(failure, forAPI: api)
            } catch let e {
                error = e
            }
            done = true
        }
        XCTAssertTrue(wait({ done }, timeout: 10), "setSimulatedError never returned")
        if let error = error {
            throw error
        }
    }

    /// Queues a command and waits until the app has taken it. The queue is an
    /// NSUserDefaults array that the app's takeCommand (on the EDT) and this
    /// method (on the main thread) each read, change and write back; with a
    /// second command appended while the app was taking the first, one write
    /// overwrote the other and a command vanished. Waiting for the app's echo
    /// means this only ever writes to an empty queue, and the app only writes
    /// to a non-empty one.
    private func command(_ c: String, file: StaticString = #filePath, line: UInt = #line) {
        let echo = "command \(c)"
        let before = events().filter { $0 == echo }.count
        var q = UserDefaults.standard.stringArray(forKey: commandsKey) ?? []
        q.append(c)
        UserDefaults.standard.set(q, forKey: commandsKey)
        XCTAssertTrue(wait({ self.events().filter { $0 == echo }.count > before }, timeout: 15),
                      "the app never took \(c): \(events())", file: file, line: line)
    }

    private func events() -> [String] {
        return UserDefaults.standard.stringArray(forKey: eventsKey) ?? []
    }

    /// Spins the main run loop (StoreKit and the app need it) until the
    /// condition holds or the time is up.
    private func wait(_ condition: () -> Bool, timeout: TimeInterval) -> Bool {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            if condition() {
                return true
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.2))
        }
        return condition()
    }

    private func waitFor(_ event: String, timeout: TimeInterval = 30,
                         file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertTrue(wait({ self.events().contains(event) }, timeout: timeout),
                      "never saw '\(event)'; events: \(events())", file: file, line: line)
    }

    @discardableResult
    private func waitForPrefix(_ prefix: String, timeout: TimeInterval = 30,
                               file: StaticString = #filePath, line: UInt = #line) -> String {
        var found: String?
        _ = wait({
            found = self.events().first { $0.hasPrefix(prefix) }
            return found != nil
        }, timeout: timeout)
        XCTAssertNotNil(found, "never saw '\(prefix)...'; events: \(events())", file: file, line: line)
        return found ?? ""
    }

    /// "receipt <sku> <transactionId> <kind>" -> transactionId.
    private func receiptTransactionId(_ event: String) -> String? {
        let p = event.split(separator: " ")
        return p.count >= 4 ? String(p[2]) : nil
    }

    /// The app finishes a transaction once its receipt is stored; until then
    /// it is in Transaction.unfinished.
    private func assertFinished(_ sku: String, file: StaticString = #filePath, line: UInt = #line) {
        let done = wait({ !self.unfinishedProducts().contains(sku) }, timeout: 20)
        XCTAssertTrue(done, "\(sku) was never finished", file: file, line: line)
    }

    private func unfinishedProducts() -> [String] {
        var out: [String] = []
        var complete = false
        Task.detached {
            var ids: [String] = []
            for await r in Transaction.unfinished {
                if case .verified(let t) = r {
                    ids.append(t.productID)
                }
            }
            DispatchQueue.main.async {
                out = ids
                complete = true
            }
        }
        _ = wait({ complete }, timeout: 10)
        return out
    }
}
