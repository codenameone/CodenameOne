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
package com.codenameone.examples.bazaar.pos;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.CashMoveDto;
import com.codenameone.examples.bazaar.api.CloseShiftDto;
import com.codenameone.examples.bazaar.api.OpenShiftDto;
import com.codenameone.examples.bazaar.api.PinSignInDto;
import com.codenameone.examples.bazaar.api.PosApiServer;
import com.codenameone.examples.bazaar.api.PosCatalogDto;
import com.codenameone.examples.bazaar.api.PosCustomerDto;
import com.codenameone.examples.bazaar.api.PosReceiptDto;
import com.codenameone.examples.bazaar.api.PosRegisterDto;
import com.codenameone.examples.bazaar.api.PosReturnDto;
import com.codenameone.examples.bazaar.api.PosReturnResultDto;
import com.codenameone.examples.bazaar.api.PosSessionDto;
import com.codenameone.examples.bazaar.api.PosSyncDto;
import com.codenameone.examples.bazaar.api.PosSyncResultDto;
import com.codenameone.examples.bazaar.api.ReceiptSendDto;
import com.codenameone.examples.bazaar.api.ShiftDto;

import java.util.List;

/// The server's half of `PosApi`.
@Component
public class PosEndpoint implements PosApiServer {
    private final Pos pos;
    private final PosSales sales;

    public PosEndpoint(Pos pos, PosSales sales) {
        this.pos = pos;
        this.sales = sales;
    }

    @Override
    public List<PosRegisterDto> registers(String storeId) throws Exception {
        return pos.registers(Caller.name(), storeId);
    }

    @Override
    public PosRegisterDto addRegister(String storeId, PosRegisterDto body) throws Exception {
        return pos.addRegister(Caller.name(), storeId, body);
    }

    @Override
    public PosRegisterDto saveRegister(String storeId, String registerId, PosRegisterDto body)
            throws Exception {
        return pos.saveRegister(Caller.name(), storeId, registerId, body);
    }

    @Override
    public PosRegisterDto removeRegister(String storeId, String registerId) throws Exception {
        return pos.removeRegister(Caller.name(), storeId, registerId);
    }

    @Override
    public PosSessionDto signIn(String storeId, String registerId, PinSignInDto body)
            throws Exception {
        return pos.signIn(Caller.name(), storeId, registerId, body);
    }

    @Override
    public ShiftDto openShift(String storeId, String registerId, OpenShiftDto body)
            throws Exception {
        return pos.openShift(Caller.name(), storeId, registerId, body);
    }

    @Override
    public ShiftDto shift(String storeId, String shiftId) throws Exception {
        return pos.shift(Caller.name(), storeId, shiftId);
    }

    @Override
    public List<CashMoveDto> cashMoves(String storeId, String shiftId) throws Exception {
        return pos.cashMoves(Caller.name(), storeId, shiftId);
    }

    @Override
    public ShiftDto moveCash(String storeId, String shiftId, CashMoveDto body) throws Exception {
        return pos.moveCash(Caller.name(), storeId, shiftId, body);
    }

    @Override
    public ShiftDto closeShift(String storeId, String shiftId, CloseShiftDto body)
            throws Exception {
        return pos.closeShift(Caller.name(), storeId, shiftId, body);
    }

    @Override
    public PosCatalogDto catalog(String storeId, String registerId, String version)
            throws Exception {
        return pos.catalog(Caller.name(), storeId, registerId, version);
    }

    @Override
    public List<PosCustomerDto> customers(String storeId, String q) throws Exception {
        return pos.customers(Caller.name(), storeId, q);
    }

    @Override
    public PosSyncResultDto sync(String storeId, String registerId, PosSyncDto body)
            throws Exception {
        return sales.sync(Caller.name(), storeId, registerId, body);
    }

    @Override
    public PosReturnResultDto counterReturn(String storeId, String registerId, String orderId,
            PosReturnDto body) throws Exception {
        return pos.counterReturn(Caller.name(), storeId, registerId, orderId, body);
    }

    @Override
    public PosReceiptDto receipt(String storeId, String orderId) throws Exception {
        return pos.receipt(Caller.name(), storeId, orderId);
    }

    @Override
    public PosReceiptDto sendReceipt(String storeId, String orderId, ReceiptSendDto body)
            throws Exception {
        return pos.sendReceipt(Caller.name(), storeId, orderId, body);
    }
}
