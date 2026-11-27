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
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codename1.kanban.KanbanLane;
import com.codename1.ui.FontImage;
import com.codename1.ui.kanban.KanbanAdapter;
import com.codename1.ui.kanban.KanbanBoard;
import com.codenameone.examples.bazaar.api.QuoteMoveDto;
import com.codenameone.examples.bazaar.api.TradeQuoteDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// B2B quotes as a Kanban board, a column per status. Dragging a card asks
/// the server to move the quote; the server owns the rules, so a move it
/// refuses comes back as its 409 message, shown as it is, and the board is
/// reloaded to put the card back.
final class QuotesPane extends MerchantPane {
    private boolean busy;

    QuotesPane() {
        super("Quotes", "merchant-quotes");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "quotes-refresh", this::load);
        Api.trade().quotes(MerchantState.storeId(), Net.to(quotes -> {
            String[] titles = new String[QuoteMoves.COLUMNS.length];
            for (int iter = 0; iter < titles.length; iter++) {
                titles[iter] = Lang.tr(Words.of(QuoteMoves.COLUMNS[iter]));
            }
            KanbanBoard board = new KanbanBoard(QuoteMoves.model(quotes, titles));
            board.setName("quotes-board");
            board.setColumnReorderEnabled(false);
            board.addKanbanListener(new Moves());
            root(board);
        }, Ui::fail));
    }

    private final class Moves extends KanbanAdapter {
        private String from;

        @Override
        public boolean cardMoving(KanbanBoard b, KanbanCard card, KanbanColumn column,
                KanbanLane lane, int index) {
            String was = card.getColumnId();
            from = was != null && was.equals(column.getId()) ? null : was;
            // Never vetoed here: the server is the judge and its message is
            // the explanation the merchant sees.
            return !busy;
        }

        @Override
        public void cardMoved(KanbanBoard b, KanbanCard card) {
            String was = from;
            from = null;
            if (was == null || !(card.getUserData() instanceof TradeQuoteDto)) {
                return;
            }
            TradeQuoteDto q = (TradeQuoteDto) card.getUserData();
            busy = true;
            Api.trade().moveQuote(MerchantState.storeId(), q.id(),
                    new QuoteMoveDto(card.getColumnId()), Net.to(moved -> {
                        busy = false;
                        Ui.say("Quote moved");
                        load();
                    }, (status, message) -> {
                        busy = false;
                        Ui.fail(status, message);
                        load();
                    }));
        }
    }
}
