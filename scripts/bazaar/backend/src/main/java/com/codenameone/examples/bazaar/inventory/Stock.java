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

import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.AdjustDto;
import com.codenameone.examples.bazaar.api.LevelDto;
import com.codenameone.examples.bazaar.api.MoveDto;
import com.codenameone.examples.bazaar.api.PurchaseLineDto;
import com.codenameone.examples.bazaar.api.PurchaseOrderDto;
import com.codenameone.examples.bazaar.api.ReceiveDto;
import com.codenameone.examples.bazaar.api.ReceiveLineDto;
import com.codenameone.examples.bazaar.api.StockDto;
import com.codenameone.examples.bazaar.api.SupplierDto;
import com.codenameone.examples.bazaar.api.TransferDto;
import com.codenameone.examples.bazaar.api.TransferLineDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import com.codenameone.examples.bazaar.api.VariantHitDto;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.InventoryMove;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.PurchaseLine;
import com.codenameone.examples.bazaar.domain.PurchaseOrder;
import com.codenameone.examples.bazaar.domain.StockTransfer;
import com.codenameone.examples.bazaar.domain.Supplier;
import com.codenameone.examples.bazaar.domain.TransferLine;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.repository.InventoryLevelRepository;
import com.codenameone.examples.bazaar.repository.InventoryMoveRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.PurchaseLineRepository;
import com.codenameone.examples.bazaar.repository.PurchaseOrderRepository;
import com.codenameone.examples.bazaar.repository.StockTransferRepository;
import com.codenameone.examples.bazaar.repository.SupplierRepository;
import com.codenameone.examples.bazaar.repository.TransferLineRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.ops.Perms;
import com.codenameone.examples.bazaar.store.StoreAccess;

import com.codenameone.examples.bazaar.ops.Timelines;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The stock screens: levels, adjustments and counts, the ledger, transfers
/// between locations, suppliers and purchase orders. Every change goes
/// through [Inventory], so the ledger stays whole.
///
/// Transfers go DRAFT, IN_TRANSIT (sent: off hand at the source, incoming at
/// the destination), PARTIAL, RECEIVED; or CANCELLED. Purchase orders go
/// DRAFT, ORDERED (incoming at their location), PARTIAL, RECEIVED; or
/// CANCELLED. A receipt may be part of what is outstanding and never more.
@Component
public class Stock {
    public static final String DRAFT = "DRAFT";
    public static final String IN_TRANSIT = "IN_TRANSIT";
    public static final String ORDERED = "ORDERED";
    public static final String PARTIAL = "PARTIAL";
    public static final String RECEIVED = "RECEIVED";
    public static final String CANCELLED = "CANCELLED";
    private static final String[] ADJUST_REASONS = {Inventory.DAMAGE, Inventory.CORRECTION,
        Inventory.RESTOCK, Inventory.RETURN};

    private final Inventory inventory;
    private final StoreAccess access;
    private final InventoryLevelRepository levels;
    private final InventoryMoveRepository moves;
    private final VariantRepository variants;
    private final ProductRepository products;
    private final LocationRepository locations;
    private final StockTransferRepository transfers;
    private final TransferLineRepository transferLines;
    private final SupplierRepository suppliers;
    private final PurchaseOrderRepository orders;
    private final PurchaseLineRepository orderLines;
    private final Timelines timelines;

    public Stock(Inventory inventory, StoreAccess access, InventoryLevelRepository levels,
            InventoryMoveRepository moves, VariantRepository variants,
            ProductRepository products, LocationRepository locations,
            StockTransferRepository transfers, TransferLineRepository transferLines,
            SupplierRepository suppliers, PurchaseOrderRepository orders,
            PurchaseLineRepository orderLines, Timelines timelines) {
        this.timelines = timelines;
        this.inventory = inventory;
        this.access = access;
        this.levels = levels;
        this.moves = moves;
        this.variants = variants;
        this.products = products;
        this.locations = locations;
        this.transfers = transfers;
        this.transferLines = transferLines;
        this.suppliers = suppliers;
        this.orders = orders;
        this.orderLines = orderLines;
    }

    // ---------------------------------------------------------------- levels

    @Transactional(readOnly = true)
    public List<LevelDto> levels(String username, String storeId, String locationId,
            String productId, boolean lowOnly) throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, StoreAccess.STOCK);
        Names names = new Names(storeId);
        List<InventoryLevel> all = levels.findByStoreId(storeId);
        List<LevelDto> out = new ArrayList<LevelDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            InventoryLevel level = all.get(iter);
            Variant variant = names.variants.get(level.getVariantId());
            if (variant == null || !variant.isTrackInventory()) {
                continue;
            }
            if (locationId != null && locationId.length() > 0
                    && !locationId.equals(level.getLocationId())) {
                continue;
            }
            if (productId != null && productId.length() > 0
                    && !productId.equals(variant.getProductId())) {
                continue;
            }
            if (lowOnly && level.getOnHand() - level.getCommitted() > level.getReorderPoint()) {
                continue;
            }
            out.add(names.level(level));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public LevelDto adjust(String username, String storeId, AdjustDto adjust) throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.STOCK);
        if (adjust == null || adjust.delta() == 0) {
            throw new ResponseStatusException(400, "Say how many units to add or take away");
        }
        String reason = Text.oneOf(adjust.reason(), ADJUST_REASONS, Inventory.CORRECTION,
                "the reason");
        variant(storeId, adjust.variantId());
        inventory.change(storeId, adjust.variantId(), location(storeId, adjust.locationId()),
                adjust.delta(), 0, 0, reason, Text.optional(adjust.note(), 64, "the note"),
                username);
        return level(storeId, adjust.variantId(), adjust.locationId());
    }

    @Transactional(rollbackFor = IOException.class)
    public LevelDto count(String username, String storeId, AdjustDto count) throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.STOCK);
        if (count == null) {
            throw new ResponseStatusException(400, "Nothing counted");
        }
        variant(storeId, count.variantId());
        inventory.count(storeId, count.variantId(), location(storeId, count.locationId()),
                count.quantity(), Text.optional(count.note(), 64, "the note"), username);
        return level(storeId, count.variantId(), count.locationId());
    }

    @Transactional(rollbackFor = IOException.class)
    public LevelDto reorderPoint(String username, String storeId, AdjustDto point)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.STOCK);
        if (point == null || point.quantity() < 0) {
            throw new ResponseStatusException(400, "A reorder point is zero or more");
        }
        variant(storeId, point.variantId());
        InventoryLevel level = inventory.level(storeId, point.variantId(),
                location(storeId, point.locationId()));
        levels.setReorderPoint(level.getId(), point.quantity());
        return level(storeId, point.variantId(), point.locationId());
    }

    @Transactional(readOnly = true)
    public List<MoveDto> moves(String username, String storeId, String variantId,
            String locationId, String limit) throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, StoreAccess.STOCK);
        int max = 200;
        if (limit != null && limit.length() > 0 && limit.length() < 5) {
            try {
                max = Math.max(1, Math.min(1000, Integer.parseInt(limit)));
            } catch (NumberFormatException bad) {
                throw new ResponseStatusException(400, "The limit is a number");
            }
        }
        List<InventoryMove> rows;
        if (variantId != null && variantId.length() > 0) {
            variant(storeId, variantId);
            rows = moves.findByVariantIdOrderByCreatedAtDesc(variantId);
        } else {
            rows = moves.findByStoreIdOrderByCreatedAtDesc(storeId, new PageRequest(0, 5000));
        }
        List<MoveDto> out = new ArrayList<MoveDto>();
        for (int iter = 0; iter < rows.size() && out.size() < max; iter++) {
            InventoryMove m = rows.get(iter);
            if (locationId != null && locationId.length() > 0
                    && !locationId.equals(m.getLocationId())) {
                continue;
            }
            out.add(new MoveDto(m.getId(), m.getVariantId(), m.getLocationId(),
                    m.getOnHandDelta(), m.getCommittedDelta(), m.getReason(), m.getReference(),
                    m.getUsername(), m.getCreatedAt().toEpochMilli()));
        }
        return out;
    }

    /// A variant by barcode, or failing that by SKU ignoring case.
    @Transactional(readOnly = true)
    public VariantHitDto lookup(String username, String storeId, String code)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        String wanted = code == null ? "" : code.trim();
        if (wanted.length() == 0) {
            throw new ResponseStatusException(400, "Scan or type a code");
        }
        List<Variant> hits = variants.findByStoreIdAndBarcode(storeId, wanted);
        Variant found = hits.isEmpty() ? null : hits.get(0);
        if (found == null) {
            List<Variant> all = variants.findByStoreId(storeId);
            for (int iter = 0; iter < all.size() && found == null; iter++) {
                if (all.get(iter).getSku().equalsIgnoreCase(wanted)) {
                    found = all.get(iter);
                }
            }
        }
        if (found == null) {
            throw new ResponseStatusException(404, "Nothing has that code");
        }
        Product product = products.findById(found.getProductId());
        Names names = new Names(storeId);
        List<InventoryLevel> held = levels.findByVariantId(found.getId());
        List<StockDto> per = new ArrayList<StockDto>();
        int total = 0;
        for (int iter = 0; iter < held.size(); iter++) {
            InventoryLevel level = held.get(iter);
            int available = level.getOnHand() - level.getCommitted();
            total += Math.max(0, available);
            per.add(new StockDto(level.getLocationId(), names.location(level.getLocationId()),
                    available));
        }
        Variant v = found;
        VariantDto dto = new VariantDto(v.getId(), v.getProductId(), v.getTitle(), v.getOption1(),
                v.getOption2(), v.getOption3(), v.getSku(), v.getBarcode(), v.getPriceCents(),
                v.getCompareAtCents(), v.getCostCents(), v.getWeightGrams(), v.isTaxable(),
                v.isTrackInventory(), v.isAllowBackorder(), v.getPosition(),
                v.isTrackInventory() ? total : -1, per);
        return new VariantHitDto(v.getProductId(), product == null ? "" : product.getTitle(),
                product == null ? "" : product.getStatus(),
                product == null ? "" : product.getPhotoId(), dto);
    }

    // ------------------------------------------------------------- transfers

    @Transactional(readOnly = true)
    public List<TransferDto> transfers(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, StoreAccess.STOCK);
        Names names = new Names(storeId);
        List<StockTransfer> all = transfers.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<TransferDto> out = new ArrayList<TransferDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(toDto(all.get(iter), names));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public TransferDto createTransfer(String username, String storeId, TransferDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.STOCK);
        if (wanted == null || wanted.lines() == null || wanted.lines().isEmpty()) {
            throw new ResponseStatusException(400, "A transfer moves at least one variant");
        }
        String from = location(storeId, wanted.fromLocationId());
        String to = location(storeId, wanted.toLocationId());
        if (from.equals(to)) {
            throw new ResponseStatusException(400, "A transfer goes somewhere else");
        }
        StockTransfer transfer = new StockTransfer();
        transfer.setId(Ids.next());
        transfer.setStoreId(storeId);
        transfer.setFromLocationId(from);
        transfer.setToLocationId(to);
        transfer.setStatus(DRAFT);
        transfer.setNote(Text.optional(wanted.note(), 500, "the note"));
        transfer.setCreatedAt(Instant.now());
        transfer.setUpdatedAt(Instant.now());
        transfers.insert(transfer);
        timelines.record(storeId, Timelines.TRANSFER, transfer.getId(), Timelines.CREATED,
                "Transfer created", username);
        for (int iter = 0; iter < wanted.lines().size(); iter++) {
            TransferLineDto w = wanted.lines().get(iter);
            variant(storeId, w.variantId());
            if (w.quantity() <= 0) {
                throw new ResponseStatusException(400, "A quantity is at least 1");
            }
            TransferLine line = new TransferLine();
            line.setId(Ids.next());
            line.setTransferId(transfer.getId());
            line.setVariantId(w.variantId());
            line.setQuantity(w.quantity());
            transferLines.insert(line);
        }
        return toDto(transfers.findById(transfer.getId()), new Names(storeId));
    }

    /// Sends a draft transfer. Refused when the source has fewer available
    /// than a line moves.
    @Transactional(rollbackFor = IOException.class)
    public TransferDto sendTransfer(String username, String storeId, String transferId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.STOCK);
        StockTransfer transfer = transfer(storeId, transferId, DRAFT);
        List<TransferLine> lines = transferLines.findByTransferId(transferId);
        for (int iter = 0; iter < lines.size(); iter++) {
            TransferLine line = lines.get(iter);
            Variant variant = variant(storeId, line.getVariantId());
            if (variant.isTrackInventory()
                    && inventory.available(line.getVariantId(), transfer.getFromLocationId())
                    < line.getQuantity()) {
                throw new ResponseStatusException(409, "Not enough " + variant.getTitle()
                        + " " + variant.getSku() + " to send");
            }
        }
        for (int iter = 0; iter < lines.size(); iter++) {
            TransferLine line = lines.get(iter);
            inventory.change(storeId, line.getVariantId(), transfer.getFromLocationId(),
                    -line.getQuantity(), 0, 0, Inventory.TRANSFER_OUT, transferId, username);
            inventory.change(storeId, line.getVariantId(), transfer.getToLocationId(),
                    0, 0, line.getQuantity(), Inventory.TRANSFER_IN, transferId, username);
        }
        return status(transferId, IN_TRANSIT, storeId, username);
    }

    @Transactional(rollbackFor = IOException.class)
    public TransferDto receiveTransfer(String username, String storeId, String transferId,
            ReceiveDto receive) throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.STOCK);
        StockTransfer transfer = transfer(storeId, transferId, IN_TRANSIT, PARTIAL);
        String to = transfer.getToLocationId();
        List<TransferLine> lines = transferLines.findByTransferId(transferId);
        int[] counts = received(receive, lines.size(), new LineIds() {
            @Override
            public String id(int index) {
                return lines.get(index).getId();
            }

            @Override
            public int outstanding(int index) {
                return lines.get(index).getQuantity() - lines.get(index).getReceived();
            }
        });
        boolean done = true;
        for (int iter = 0; iter < lines.size(); iter++) {
            TransferLine line = lines.get(iter);
            int got = counts[iter];
            // Read before the stock change below empties the session.
            int before = line.getReceived();
            done = done && before + got >= line.getQuantity();
            if (got > 0) {
                inventory.change(storeId, line.getVariantId(), to, got, 0, -got,
                        Inventory.TRANSFER_IN, transferId, username);
                TransferLine fresh = transferLines.findById(line.getId());
                fresh.setReceived(fresh.getReceived() + got);
                transferLines.save(fresh);
            }
        }
        return status(transferId, done ? RECEIVED : PARTIAL, storeId, username);
    }

    /// Cancels a transfer: a sent one returns what had not arrived to the
    /// source, as a CORRECTION, and stops counting it as incoming.
    @Transactional(rollbackFor = IOException.class)
    public TransferDto cancelTransfer(String username, String storeId, String transferId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.STOCK);
        StockTransfer transfer = transfer(storeId, transferId, DRAFT, IN_TRANSIT, PARTIAL);
        if (!DRAFT.equals(transfer.getStatus())) {
            String from = transfer.getFromLocationId();
            String to = transfer.getToLocationId();
            List<TransferLine> lines = transferLines.findByTransferId(transferId);
            for (int iter = 0; iter < lines.size(); iter++) {
                TransferLine line = lines.get(iter);
                int left = line.getQuantity() - line.getReceived();
                if (left > 0) {
                    inventory.change(storeId, line.getVariantId(), from, left, 0, 0,
                            Inventory.CORRECTION, transferId, username);
                    inventory.change(storeId, line.getVariantId(), to, 0, 0, -left,
                            Inventory.CORRECTION, transferId, username);
                }
            }
        }
        return status(transferId, CANCELLED, storeId, username);
    }

    private TransferDto status(String transferId, String status, String storeId,
            String username) throws IOException {
        timelines.status(storeId, Timelines.TRANSFER, transferId, status, username);
        StockTransfer transfer = transfers.findById(transferId);
        transfer.setStatus(status);
        transfer.setUpdatedAt(Instant.now());
        transfers.save(transfer);
        return toDto(transfers.findById(transferId), new Names(storeId));
    }

    private StockTransfer transfer(String storeId, String transferId, String... states) {
        StockTransfer transfer = transferId == null ? null : transfers.findById(transferId);
        StoreAccess.requireIn(transfer == null ? null : transfer.getStoreId(), storeId,
                "transfer");
        if (!StoreAccess.isRole(transfer.getStatus(), states)) {
            throw new ResponseStatusException(409, "The transfer is " + transfer.getStatus());
        }
        return transfer;
    }

    private TransferDto toDto(StockTransfer t, Names names) {
        List<TransferLine> lines = transferLines.findByTransferId(t.getId());
        List<TransferLineDto> out = new ArrayList<TransferLineDto>();
        for (int iter = 0; iter < lines.size(); iter++) {
            TransferLine line = lines.get(iter);
            Variant v = names.variants.get(line.getVariantId());
            out.add(new TransferLineDto(line.getId(), line.getVariantId(),
                    v == null ? "" : v.getSku(), names.title(v), line.getQuantity(),
                    line.getReceived()));
        }
        return new TransferDto(t.getId(), t.getFromLocationId(), t.getToLocationId(),
                t.getStatus(), t.getNote(), out, t.getCreatedAt().toEpochMilli());
    }

    // ------------------------------------------------------------- suppliers

    @Transactional(readOnly = true)
    public List<SupplierDto> suppliers(String username, String storeId) throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, StoreAccess.STOCK);
        List<Supplier> all = suppliers.findByStoreIdOrderByNameAsc(storeId);
        List<SupplierDto> out = new ArrayList<SupplierDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(toDto(all.get(iter)));
        }
        return out;
    }

    /// Adds a supplier, or saves one when `id` names it.
    @Transactional(rollbackFor = IOException.class)
    public SupplierDto saveSupplier(String username, String storeId, SupplierDto wanted)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.MANAGERS);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        Supplier supplier;
        boolean fresh = wanted.id() == null || wanted.id().length() == 0;
        if (fresh) {
            supplier = new Supplier();
            supplier.setId(Ids.next());
            supplier.setStoreId(storeId);
        } else {
            supplier = supplier(storeId, wanted.id());
        }
        supplier.setName(Text.required(wanted.name(), 120, "the supplier's name"));
        supplier.setContact(Text.optional(wanted.contact(), 120, "the contact"));
        supplier.setEmail(Text.optional(wanted.email(), 190, "the e-mail"));
        supplier.setPhone(Text.optional(wanted.phone(), 32, "the phone"));
        supplier.setAddress(Text.optional(wanted.address(), 255, "the address"));
        supplier.setNote(Text.optional(wanted.note(), 500, "the note"));
        if (fresh) {
            suppliers.insert(supplier);
        } else {
            suppliers.save(supplier);
        }
        return toDto(supplier);
    }

    @Transactional(rollbackFor = IOException.class)
    public SupplierDto deleteSupplier(String username, String storeId, String supplierId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.MANAGERS);
        Supplier supplier = supplier(storeId, supplierId);
        List<PurchaseOrder> placed = orders.findBySupplierId(supplierId);
        for (int iter = 0; iter < placed.size(); iter++) {
            String status = placed.get(iter).getStatus();
            if (!RECEIVED.equals(status) && !CANCELLED.equals(status)) {
                throw new ResponseStatusException(409,
                        "The supplier has an open purchase order");
            }
        }
        SupplierDto answer = toDto(supplier);
        suppliers.delete(supplier);
        return answer;
    }

    private Supplier supplier(String storeId, String supplierId) {
        Supplier supplier = supplierId == null ? null : suppliers.findById(supplierId);
        StoreAccess.requireIn(supplier == null ? null : supplier.getStoreId(), storeId,
                "supplier");
        return supplier;
    }

    private static SupplierDto toDto(Supplier s) {
        return new SupplierDto(s.getId(), s.getName(), s.getContact(), s.getEmail(), s.getPhone(),
                s.getAddress(), s.getNote());
    }

    // ------------------------------------------------------- purchase orders

    @Transactional(readOnly = true)
    public List<PurchaseOrderDto> purchaseOrders(String username, String storeId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, StoreAccess.STOCK);
        Names names = new Names(storeId);
        List<PurchaseOrder> all = orders.findByStoreIdOrderByCreatedAtDesc(storeId);
        List<PurchaseOrderDto> out = new ArrayList<PurchaseOrderDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(toDto(all.get(iter), names));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto purchaseOrder(String username, String storeId, String orderId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, false, StoreAccess.STOCK);
        return toDto(order(storeId, orderId), new Names(storeId));
    }

    @Transactional(rollbackFor = IOException.class)
    public PurchaseOrderDto createPurchaseOrder(String username, String storeId,
            PurchaseOrderDto wanted) throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.MANAGERS);
        if (wanted == null || wanted.lines() == null || wanted.lines().isEmpty()) {
            throw new ResponseStatusException(400, "A purchase order has at least one line");
        }
        supplier(storeId, wanted.supplierId());
        PurchaseOrder order = new PurchaseOrder();
        order.setId(Ids.next());
        order.setStoreId(storeId);
        order.setNumber((int) orders.countByStoreId(storeId) + 1001);
        order.setSupplierId(wanted.supplierId());
        order.setLocationId(location(storeId, wanted.locationId()));
        order.setStatus(DRAFT);
        order.setExpectedAt(Instant.ofEpochMilli(Math.max(0, wanted.expectedAt())));
        order.setNote(Text.optional(wanted.note(), 500, "the note"));
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(Instant.now());
        long total = 0;
        List<PurchaseLine> made = new ArrayList<PurchaseLine>();
        for (int iter = 0; iter < wanted.lines().size(); iter++) {
            PurchaseLineDto w = wanted.lines().get(iter);
            Variant variant = variant(storeId, w.variantId());
            if (w.quantity() <= 0 || w.costCents() < 0) {
                throw new ResponseStatusException(400, "A line orders at least 1 unit");
            }
            PurchaseLine line = new PurchaseLine();
            line.setId(Ids.next());
            line.setPurchaseOrderId(order.getId());
            line.setVariantId(w.variantId());
            line.setQuantity(w.quantity());
            line.setCostCents(w.costCents() > 0 ? w.costCents() : variant.getCostCents());
            total += line.getCostCents() * line.getQuantity();
            made.add(line);
        }
        order.setTotalCents(total);
        orders.insert(order);
        timelines.record(storeId, Timelines.PURCHASE, order.getId(), Timelines.CREATED,
                "Purchase order created", username);
        for (int iter = 0; iter < made.size(); iter++) {
            orderLines.insert(made.get(iter));
        }
        return toDto(orders.findById(order.getId()), new Names(storeId));
    }

    /// Places a draft order: what it orders counts as incoming.
    @Transactional(rollbackFor = IOException.class)
    public PurchaseOrderDto placePurchaseOrder(String username, String storeId, String orderId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.MANAGERS);
        PurchaseOrder order = order(storeId, orderId, DRAFT);
        List<PurchaseLine> lines = orderLines.findByPurchaseOrderId(orderId);
        for (int iter = 0; iter < lines.size(); iter++) {
            inventory.change(storeId, lines.get(iter).getVariantId(), order.getLocationId(), 0, 0,
                    lines.get(iter).getQuantity(), Inventory.RECEIVE, orderId, username);
        }
        return orderStatus(orderId, ORDERED, storeId, username);
    }

    /// Books in what arrived: RECEIVE moves add it on hand, and it stops
    /// counting as incoming.
    @Transactional(rollbackFor = IOException.class)
    public PurchaseOrderDto receivePurchaseOrder(String username, String storeId, String orderId,
            ReceiveDto receive) throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.STOCK);
        PurchaseOrder order = order(storeId, orderId, ORDERED, PARTIAL);
        String at = order.getLocationId();
        List<PurchaseLine> lines = orderLines.findByPurchaseOrderId(orderId);
        int[] counts = received(receive, lines.size(), new LineIds() {
            @Override
            public String id(int index) {
                return lines.get(index).getId();
            }

            @Override
            public int outstanding(int index) {
                return lines.get(index).getQuantity() - lines.get(index).getReceived();
            }
        });
        boolean done = true;
        for (int iter = 0; iter < lines.size(); iter++) {
            PurchaseLine line = lines.get(iter);
            int got = counts[iter];
            done = done && line.getReceived() + got >= line.getQuantity();
            if (got > 0) {
                inventory.change(storeId, line.getVariantId(), at, got, 0, -got,
                        Inventory.RECEIVE, orderId, username);
                PurchaseLine fresh = orderLines.findById(line.getId());
                fresh.setReceived(fresh.getReceived() + got);
                orderLines.save(fresh);
            }
        }
        return orderStatus(orderId, done ? RECEIVED : PARTIAL, storeId, username);
    }

    /// Cancels an order; what was ordered and never arrived stops counting
    /// as incoming.
    @Transactional(rollbackFor = IOException.class)
    public PurchaseOrderDto cancelPurchaseOrder(String username, String storeId, String orderId)
            throws IOException {
        access.requireArea(username, storeId, Perms.INVENTORY, true, StoreAccess.MANAGERS);
        PurchaseOrder order = order(storeId, orderId, DRAFT, ORDERED, PARTIAL);
        if (!DRAFT.equals(order.getStatus())) {
            List<PurchaseLine> lines = orderLines.findByPurchaseOrderId(orderId);
            for (int iter = 0; iter < lines.size(); iter++) {
                int left = lines.get(iter).getQuantity() - lines.get(iter).getReceived();
                if (left > 0) {
                    inventory.change(storeId, lines.get(iter).getVariantId(),
                            order.getLocationId(), 0, 0, -left, Inventory.RECEIVE, orderId,
                            username);
                }
            }
        }
        return orderStatus(orderId, CANCELLED, storeId, username);
    }

    private PurchaseOrderDto orderStatus(String orderId, String status, String storeId,
            String username) throws IOException {
        timelines.status(storeId, Timelines.PURCHASE, orderId, status, username);
        PurchaseOrder order = orders.findById(orderId);
        order.setStatus(status);
        order.setUpdatedAt(Instant.now());
        orders.save(order);
        return toDto(orders.findById(orderId), new Names(storeId));
    }

    private PurchaseOrder order(String storeId, String orderId, String... states) {
        PurchaseOrder order = orderId == null ? null : orders.findById(orderId);
        StoreAccess.requireIn(order == null ? null : order.getStoreId(), storeId,
                "purchase order");
        if (states.length > 0 && !StoreAccess.isRole(order.getStatus(), states)) {
            throw new ResponseStatusException(409, "The purchase order is " + order.getStatus());
        }
        return order;
    }

    private PurchaseOrderDto toDto(PurchaseOrder o, Names names) {
        List<PurchaseLine> lines = orderLines.findByPurchaseOrderId(o.getId());
        List<PurchaseLineDto> out = new ArrayList<PurchaseLineDto>();
        for (int iter = 0; iter < lines.size(); iter++) {
            PurchaseLine line = lines.get(iter);
            Variant v = names.variants.get(line.getVariantId());
            out.add(new PurchaseLineDto(line.getId(), line.getVariantId(),
                    v == null ? "" : v.getSku(), names.title(v), line.getQuantity(),
                    line.getReceived(), line.getCostCents()));
        }
        Supplier supplier = suppliers.findById(o.getSupplierId());
        return new PurchaseOrderDto(o.getId(), o.getNumber(), o.getSupplierId(),
                supplier == null ? "" : supplier.getName(), o.getLocationId(), o.getStatus(),
                o.getExpectedAt().toEpochMilli(), o.getNote(), o.getTotalCents(), out,
                o.getCreatedAt().toEpochMilli());
    }

    // --------------------------------------------------------------- helpers

    /// The lines of a transfer or purchase order, for [#received].
    private interface LineIds {
        String id(int index);

        int outstanding(int index);
    }

    /// How many units of each line arrived, in line order. Answers 400 for a
    /// line that is not this order's, or for more than is outstanding.
    private static int[] received(ReceiveDto receive, int size, LineIds lines) {
        if (receive == null || receive.lines() == null || receive.lines().isEmpty()) {
            throw new ResponseStatusException(400, "Say what arrived");
        }
        int[] counts = new int[size];
        for (int r = 0; r < receive.lines().size(); r++) {
            ReceiveLineDto got = receive.lines().get(r);
            int index = -1;
            for (int iter = 0; iter < size; iter++) {
                if (lines.id(iter).equals(got.lineId())) {
                    index = iter;
                }
            }
            if (index < 0) {
                throw new ResponseStatusException(400, "That line is not on this order");
            }
            if (got.quantity() < 0) {
                throw new ResponseStatusException(400, "A quantity is not negative");
            }
            counts[index] += got.quantity();
            if (counts[index] > lines.outstanding(index)) {
                throw new ResponseStatusException(400, "More arrived than was outstanding");
            }
        }
        return counts;
    }

    private LevelDto level(String storeId, String variantId, String locationId)
            throws IOException {
        InventoryLevel level = levels.findByVariantIdAndLocationId(variantId, locationId);
        return new Names(storeId).level(level);
    }

    private Variant variant(String storeId, String variantId) {
        Variant variant = variantId == null ? null : variants.findById(variantId);
        StoreAccess.requireIn(variant == null ? null : variant.getStoreId(), storeId, "variant");
        return variant;
    }

    private String location(String storeId, String locationId) {
        Location location = locationId == null ? null : locations.findById(locationId);
        StoreAccess.requireIn(location == null ? null : location.getStoreId(), storeId,
                "location");
        return locationId;
    }

    /// The names a list of levels, transfers or orders shows, read once.
    private final class Names {
        final Map<String, Variant> variants = new HashMap<String, Variant>();
        final Map<String, Product> products = new HashMap<String, Product>();
        final Map<String, String> locations = new HashMap<String, String>();

        Names(String storeId) {
            List<Variant> all = Stock.this.variants.findByStoreId(storeId);
            for (int iter = 0; iter < all.size(); iter++) {
                variants.put(all.get(iter).getId(), all.get(iter));
            }
            List<Product> catalog = Stock.this.products.findByStoreIdOrderByTitleAsc(storeId);
            for (int iter = 0; iter < catalog.size(); iter++) {
                products.put(catalog.get(iter).getId(), catalog.get(iter));
            }
            List<Location> places = Stock.this.locations.findByStoreIdOrderByPositionAsc(storeId);
            for (int iter = 0; iter < places.size(); iter++) {
                locations.put(places.get(iter).getId(), places.get(iter).getName());
            }
        }

        String location(String id) {
            String name = locations.get(id);
            return name == null ? "" : name;
        }

        String title(Variant v) {
            if (v == null) {
                return "";
            }
            Product p = products.get(v.getProductId());
            String product = p == null ? "" : p.getTitle();
            return "Default".equals(v.getTitle()) ? product : product + " - " + v.getTitle();
        }

        LevelDto level(InventoryLevel level) {
            Variant v = variants.get(level.getVariantId());
            Product p = v == null ? null : products.get(v.getProductId());
            return new LevelDto(level.getId(), level.getVariantId(), level.getLocationId(),
                    location(level.getLocationId()), p == null ? "" : p.getId(),
                    p == null ? "" : p.getTitle(), v == null ? "" : v.getTitle(),
                    v == null ? "" : v.getSku(), v == null ? "" : v.getBarcode(),
                    level.getOnHand(), level.getCommitted(),
                    level.getOnHand() - level.getCommitted(), level.getIncoming(),
                    level.getReorderPoint());
        }
    }
}
