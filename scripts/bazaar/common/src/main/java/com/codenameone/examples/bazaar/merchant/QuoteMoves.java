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

import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codenameone.examples.bazaar.api.TradeQuoteDto;
import java.util.ArrayList;
import java.util.List;

/// The quotes board: a column per quote status and the moves a merchant
/// may make between them, mirroring the server's `Trade.legal` for the
/// merchant's side.
///
/// The merchant drafts and sends; the buyer accepts or declines; the
/// merchant then orders an accepted quote. So DRAFT and SENT swap, either
/// may be declined, and ACCEPTED goes to ORDERED -- nothing else. The map
/// only labels the board and fills a card's "Move to" menu: a drag is always
/// sent, and a move the server refuses comes back as its 409 message, which
/// is the authority (the quote may have changed meanwhile).
final class QuoteMoves {
    static final String DRAFT = "DRAFT";
    static final String SENT = "SENT";
    static final String ACCEPTED = "ACCEPTED";
    static final String ORDERED = "ORDERED";
    static final String DECLINED = "DECLINED";
    /// The board's columns, left to right.
    static final String[] COLUMNS = {DRAFT, SENT, ACCEPTED, ORDERED, DECLINED};

    private QuoteMoves() {
    }

    /// Whether a merchant may move a quote from `from` to `to`.
    static boolean legal(String from, String to) {
        if (from == null || to == null) {
            return false;
        }
        return DRAFT.equals(from) && SENT.equals(to)
                || SENT.equals(from) && DRAFT.equals(to)
                || (DRAFT.equals(from) || SENT.equals(from)) && DECLINED.equals(to)
                || ACCEPTED.equals(from) && ORDERED.equals(to);
    }

    /// Where a quote in `from` may go, in column order.
    static List<String> targets(String from) {
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            if (legal(from, COLUMNS[iter])) {
                out.add(COLUMNS[iter]);
            }
        }
        return out;
    }

    /// The column a status lands in; anything unknown is a draft.
    static String column(String status) {
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            if (COLUMNS[iter].equals(status)) {
                return status;
            }
        }
        return DRAFT;
    }

    /// The board for `quotes`: every column, each quote in its status's.
    static DefaultKanbanModel model(List<TradeQuoteDto> quotes, String[] titles) {
        DefaultKanbanModel model = new DefaultKanbanModel();
        for (int iter = 0; iter < COLUMNS.length; iter++) {
            model.addColumn(new KanbanColumn(COLUMNS[iter], titles[iter]));
        }
        for (int iter = 0; quotes != null && iter < quotes.size(); iter++) {
            TradeQuoteDto q = quotes.get(iter);
            KanbanCard card = new KanbanCard(q.id(), q.companyName() == null ? q.id()
                    : q.companyName());
            card.setDescription(MerchantState.money(q.totalCents())
                    + (q.buyerUsername() == null || q.buyerUsername().length() == 0 ? ""
                    : "  " + q.buyerUsername()));
            card.setUserData(q);
            model.addCard(column(q.status()), card);
        }
        return model;
    }
}
