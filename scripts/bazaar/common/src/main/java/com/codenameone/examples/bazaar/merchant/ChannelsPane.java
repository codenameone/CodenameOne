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
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.bazaar.api.ChannelDto;
import com.codenameone.examples.bazaar.api.ChannelListingDto;
import com.codenameone.examples.bazaar.api.ChannelSetupDto;
import com.codenameone.examples.bazaar.api.SyncLogDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Connected marketplaces: each channel's state and failing listings, and
/// per channel its listings and sync log with a "sync now". Tokens are
/// entered when connecting and never shown again.
final class ChannelsPane extends MerchantPane {
    ChannelsPane() {
        super("Channels", "merchant-channels");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "Connect", "channels-connect", this::connect);
        Api.channels().channels(MerchantState.storeId(), Net.to(this::showChannels, Ui::fail));
    }

    private void showChannels(List<ChannelDto> found) {
        Container list = new Container(BoxLayout.y());
        list.setName("channels-list");
        if (found.isEmpty()) {
            list.add(Ui.plain(Lang.tr("No channels connected"), "SvMuted"));
        }
        for (int iter = 0; iter < found.size(); iter++) {
            final ChannelDto c = found.get(iter);
            String detail = c.kind() + " - " + c.listings() + " " + Lang.tr("listings")
                    + (c.errors() > 0 ? ", " + c.errors() + " " + Lang.tr("failing") : "")
                    + (c.lastError().length() > 0 ? " - " + c.lastError() : "");
            list.add(Ui.primary(c.name() + " (" + c.status() + ")", "channel-" + c.id(),
                    e -> open(c)));
            list.add(Ui.plain(detail, "SvMuted"));
        }
        root(list);
    }

    private void open(final ChannelDto c) {
        final Container page = new Container(BoxLayout.y());
        page.setName("channel-page");
        final Container log = new Container(BoxLayout.y());
        page.add(Ui.primary(Lang.tr("Sync now"), "channel-sync", e -> Api.channels().syncChannel(
                MerchantState.storeId(), c.id(), Net.to(r -> showLog(log, r), Ui::fail))));
        final Container listings = new Container(BoxLayout.y());
        page.add(listings);
        page.add(log);
        Api.channels().channelListings(MerchantState.storeId(), c.id(), Net.to(ls -> {
            for (int iter = 0; iter < ls.size(); iter++) {
                ChannelListingDto l = ls.get(iter);
                listings.add(Ui.row((char) 0, l.title(), l.externalSku() + " - " + l.status()
                        + (l.lastError().length() > 0 ? " - " + l.lastError() : ""),
                        Ui.plain(String.valueOf(l.pushedQty()), "SvRowValue")));
            }
            Ui.relayout(listings);
        }, Ui::fail));
        Api.channels().channelLog(MerchantState.storeId(), c.id(),
                Net.to(r -> showLog(log, r), Ui::fail));
        push(c.name(), page);
    }

    private void showLog(Container log, List<SyncLogDto> entries) {
        log.removeAll();
        for (int iter = 0; iter < entries.size(); iter++) {
            SyncLogDto e = entries.get(iter);
            log.add(Ui.row((char) 0, e.action() + " " + e.ref(), e.message(),
                    Ui.plain(e.outcome(), "SvRowValue")));
        }
        Ui.relayout(log);
    }

    private void connect() {
        final Container page = new Container(BoxLayout.y());
        page.setName("channels-connect-page");
        final TextField kind = MUi.text(page, "Marketplace (SIMULATED or EBAY)", "channel-kind",
                "SIMULATED");
        final TextField name = MUi.text(page, "Name", "channel-name", "");
        final TextField token = MUi.text(page, "Refresh token", "channel-token", "");
        token.setConstraint(TextField.PASSWORD);
        final TextField secret = MUi.text(page, "Notification secret", "channel-secret", "");
        secret.setConstraint(TextField.PASSWORD);
        page.add(Ui.primary(Lang.tr("Connect"), "channel-connect-save", e -> Api.channels()
                .connectChannel(MerchantState.storeId(), new ChannelSetupDto(kind.getText(),
                name.getText(), token.getText(), secret.getText()), Net.to(r -> {
                    pop();
                    showChannels(r);
                }, Ui::fail))));
        push(Lang.tr("Connect a marketplace"), page);
    }
}
