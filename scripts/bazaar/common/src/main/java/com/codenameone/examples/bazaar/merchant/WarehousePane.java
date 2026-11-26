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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codename1.kanban.KanbanLane;
import com.codename1.ui.FontImage;
import com.codename1.ui.kanban.KanbanAdapter;
import com.codename1.ui.kanban.KanbanBoard;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.PickLineDto;
import com.codenameone.examples.bazaar.api.WarehouseMoveDto;
import com.codenameone.examples.bazaar.api.WarehouseTaskDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ops.WarehouseFlow;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The warehouse board of one location: receiving and put-away for
/// inbound purchase orders, pick, pack and dispatch for orders. A card is
/// dragged one step at a time; a drop the flow does not allow is refused
/// before it is sent ([WarehouseFlow#legal]), and the server judges again.
final class WarehousePane extends MerchantPane {
    private List<LocationDto> locations;
    private int location;
    private boolean busy;

    WarehousePane() {
        super("Warehouse", "merchant-warehouse");
    }

    @Override
    void load() {
        if (locations == null) {
            Api.stores().locations(MerchantState.storeId(), Net.to(found -> {
                locations = found;
                load();
            }, Ui::fail));
            return;
        }
        if (locations.isEmpty()) {
            root(Ui.plain(Lang.tr("Add a location first"), "SvMuted"));
            return;
        }
        final String loc = locations.get(location % locations.size()).id();
        clearTools();
        tool(FontImage.MATERIAL_REFRESH, "Sync", "warehouse-sync", () -> Api.ops().syncWarehouse(
                MerchantState.storeId(), loc, Net.to(x -> load(), Ui::fail)));
        tool(FontImage.MATERIAL_LIST, "Pick list", "warehouse-picks", () -> picks(loc));
        if (locations.size() > 1) {
            tool(FontImage.MATERIAL_PLACE, locations.get((location + 1) % locations.size()).name(),
                    "warehouse-location", () -> {
                        location++;
                        load();
                    });
        }
        Api.ops().warehouseBoard(MerchantState.storeId(), loc, Net.to(tasks -> {
            KanbanBoard board = new KanbanBoard(model(tasks));
            board.setName("warehouse-board");
            board.setColumnReorderEnabled(false);
            board.addKanbanListener(new Moves());
            root(board);
        }, Ui::fail));
    }

    static DefaultKanbanModel model(List<WarehouseTaskDto> tasks) {
        DefaultKanbanModel model = new DefaultKanbanModel();
        for (int iter = 0; iter < WarehouseFlow.COLUMNS.length; iter++) {
            model.addColumn(new KanbanColumn(WarehouseFlow.COLUMNS[iter],
                    Lang.tr(Words.of(WarehouseFlow.COLUMNS[iter]))));
        }
        for (int iter = 0; tasks != null && iter < tasks.size(); iter++) {
            WarehouseTaskDto t = tasks.get(iter);
            KanbanCard card = new KanbanCard(t.id(), t.label());
            card.setDescription(Lang.tr(WarehouseFlow.INBOUND.equals(t.kind()) ? "Inbound" : "Outbound"));
            card.setUserData(t);
            model.addCard(t.stage(), card);
        }
        return model;
    }

    private void picks(String loc) {
        Api.ops().pickList(MerchantState.storeId(), loc, Net.to(lines -> {
            Rows<PickLineDto> rows = new Rows<PickLineDto>()
                    .col(Lang.tr("Bin"), PickLineDto::binCode)
                    .col(Lang.tr("Item"), PickLineDto::title)
                    .col(Lang.tr("SKU"), PickLineDto::sku)
                    .col(Lang.tr("Quantity"), l -> Integer.valueOf(l.quantity()));
            rows.set(lines);
            push(Lang.tr("Pick list"), MUi.grid(rows, "warehouse-pick-grid"));
        }, Ui::fail));
    }

    private final class Moves extends KanbanAdapter {
        private String from;

        @Override
        public boolean cardMoving(KanbanBoard b, KanbanCard card, KanbanColumn column,
                KanbanLane lane, int index) {
            String was = card.getColumnId();
            if (was != null && was.equals(column.getId())) {
                from = null;
                return !busy;
            }
            WarehouseTaskDto t = card.getUserData() instanceof WarehouseTaskDto
                    ? (WarehouseTaskDto) card.getUserData() : null;
            if (busy || t == null || !WarehouseFlow.legal(t.kind(), was, column.getId())) {
                from = null;
                return false;
            }
            from = was;
            return true;
        }

        @Override
        public void cardMoved(KanbanBoard b, KanbanCard card) {
            String was = from;
            from = null;
            if (was == null || !(card.getUserData() instanceof WarehouseTaskDto)) {
                return;
            }
            WarehouseTaskDto t = (WarehouseTaskDto) card.getUserData();
            busy = true;
            Api.ops().moveWarehouseTask(MerchantState.storeId(),
                    new WarehouseMoveDto(t.id(), card.getColumnId()), Net.to(moved -> {
                        busy = false;
                        load();
                    }, (status, message) -> {
                        busy = false;
                        Ui.fail(status, message);
                        load();
                    }));
        }
    }
}
