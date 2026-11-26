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
package com.codenameone.examples.bazaar.inventory;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.AdjustDto;
import com.codenameone.examples.bazaar.api.InventoryApiServer;
import com.codenameone.examples.bazaar.api.LevelDto;
import com.codenameone.examples.bazaar.api.MoveDto;
import com.codenameone.examples.bazaar.api.PurchaseOrderDto;
import com.codenameone.examples.bazaar.api.ReceiveDto;
import com.codenameone.examples.bazaar.api.SupplierDto;
import com.codenameone.examples.bazaar.api.TransferDto;
import com.codenameone.examples.bazaar.api.VariantHitDto;

import java.util.List;

/// The server's half of `InventoryApi`; [Stock] checks the caller's role at
/// the store named in the path.
@Component
public class InventoryEndpoint implements InventoryApiServer {
    private final Stock stock;

    public InventoryEndpoint(Stock stock) {
        this.stock = stock;
    }

    @Override
    public List<LevelDto> levels(String storeId, String location, String product, String low)
            throws Exception {
        return stock.levels(Caller.name(), storeId, location, product, "true".equals(low));
    }

    @Override
    public List<LevelDto> lowStock(String storeId) throws Exception {
        return stock.levels(Caller.name(), storeId, null, null, true);
    }

    @Override
    public LevelDto adjust(String storeId, AdjustDto body) throws Exception {
        return stock.adjust(Caller.name(), storeId, body);
    }

    @Override
    public LevelDto count(String storeId, AdjustDto body) throws Exception {
        return stock.count(Caller.name(), storeId, body);
    }

    @Override
    public LevelDto reorderPoint(String storeId, AdjustDto body) throws Exception {
        return stock.reorderPoint(Caller.name(), storeId, body);
    }

    @Override
    public List<MoveDto> moves(String storeId, String variant, String location, String limit)
            throws Exception {
        return stock.moves(Caller.name(), storeId, variant, location, limit);
    }

    @Override
    public VariantHitDto lookup(String storeId, String code) throws Exception {
        return stock.lookup(Caller.name(), storeId, code);
    }

    @Override
    public List<TransferDto> transfers(String storeId) throws Exception {
        return stock.transfers(Caller.name(), storeId);
    }

    @Override
    public TransferDto createTransfer(String storeId, TransferDto body) throws Exception {
        return stock.createTransfer(Caller.name(), storeId, body);
    }

    @Override
    public TransferDto sendTransfer(String storeId, String transferId) throws Exception {
        return stock.sendTransfer(Caller.name(), storeId, transferId);
    }

    @Override
    public TransferDto receiveTransfer(String storeId, String transferId, ReceiveDto body)
            throws Exception {
        return stock.receiveTransfer(Caller.name(), storeId, transferId, body);
    }

    @Override
    public TransferDto cancelTransfer(String storeId, String transferId) throws Exception {
        return stock.cancelTransfer(Caller.name(), storeId, transferId);
    }

    @Override
    public List<SupplierDto> suppliers(String storeId) throws Exception {
        return stock.suppliers(Caller.name(), storeId);
    }

    @Override
    public SupplierDto saveSupplier(String storeId, SupplierDto body) throws Exception {
        return stock.saveSupplier(Caller.name(), storeId, body);
    }

    @Override
    public SupplierDto deleteSupplier(String storeId, String supplierId) throws Exception {
        return stock.deleteSupplier(Caller.name(), storeId, supplierId);
    }

    @Override
    public List<PurchaseOrderDto> purchaseOrders(String storeId) throws Exception {
        return stock.purchaseOrders(Caller.name(), storeId);
    }

    @Override
    public PurchaseOrderDto purchaseOrder(String storeId, String orderId) throws Exception {
        return stock.purchaseOrder(Caller.name(), storeId, orderId);
    }

    @Override
    public PurchaseOrderDto createPurchaseOrder(String storeId, PurchaseOrderDto body)
            throws Exception {
        return stock.createPurchaseOrder(Caller.name(), storeId, body);
    }

    @Override
    public PurchaseOrderDto placePurchaseOrder(String storeId, String orderId) throws Exception {
        return stock.placePurchaseOrder(Caller.name(), storeId, orderId);
    }

    @Override
    public PurchaseOrderDto receivePurchaseOrder(String storeId, String orderId, ReceiveDto body)
            throws Exception {
        return stock.receivePurchaseOrder(Caller.name(), storeId, orderId, body);
    }

    @Override
    public PurchaseOrderDto cancelPurchaseOrder(String storeId, String orderId) throws Exception {
        return stock.cancelPurchaseOrder(Caller.name(), storeId, orderId);
    }
}
