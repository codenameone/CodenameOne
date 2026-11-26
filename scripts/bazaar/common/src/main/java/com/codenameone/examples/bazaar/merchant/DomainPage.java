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
import com.codenameone.examples.bazaar.api.CustomDomainDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

/// The store's custom domain, from the settings. Claiming a domain hands
/// out a token; the page shows the two ways to publish it (a file on the
/// domain or a DNS TXT record) and a button that asks the server to look for
/// it now. The domain serves the store only once that check found it.
final class DomainPage {
    private final MerchantPane host;
    private final Container page = MUi.page("domain-page");

    DomainPage(MerchantPane host) {
        this.host = host;
    }

    void show() {
        host.push("Custom domain", page);
        Api.domains().domain(MerchantState.storeId(), Net.to(this::paint, Ui::fail));
    }

    private void paint(final CustomDomainDto d) {
        page.removeAll();
        String state = StoreZones.state(d);
        Container card = new Container(BoxLayout.y());
        card.setUIID("MeCard");
        final TextField domain = MUi.text(card, "Domain", "domain-name",
                d == null || d.domain() == null ? "" : d.domain());
        domain.setHint("shop.example.com");
        card.add(Ui.row(FontImage.MATERIAL_LANGUAGE, Lang.tr("Status"),
                d != null && d.message() != null ? d.message() : null,
                MUi.badge(state)));
        page.add(card);
        page.add(MUi.buttons(Ui.primary(Lang.tr("Claim domain"), "domain-claim",
                e -> {
                    String typed = StoreZones.host(domain.getText());
                    // Empty would remove the domain; only Remove domain does that.
                    if (typed.length() == 0) {
                        Ui.say("Type a domain such as shop.example.com");
                        return;
                    }
                    claim(typed);
                })));
        if (!StoreZones.NONE.equals(state)) {
            Ui.section(page, "Prove the store owns it");
            Container proof = new Container(BoxLayout.y());
            proof.setUIID("MeCard");
            proof.add(Ui.label("Token", "SvLabel"));
            proof.add(Ui.text(d.token(), "SvBody"));
            proof.add(Ui.text(Lang.tr("Either publish a file whose whole body is the token at:"),
                    "SvMuted"));
            proof.add(Ui.text(d.fileUrl(), "SvBody"));
            proof.add(Ui.text(Lang.tr("Or add this DNS TXT record:"), "SvMuted"));
            proof.add(Ui.text(StoreZones.txtLine(d), "SvBody"));
            page.add(proof);
            page.add(MUi.buttons(Ui.primary(Lang.tr("Verify now"), "domain-verify",
                    e -> Api.domains().verify(MerchantState.storeId(), Net.to(found -> {
                        Ui.say(found != null && found.verified() ? "Domain verified"
                                : "The token was not found yet");
                        paint(found);
                    }, Ui::fail))), Ui.secondary(Lang.tr("Remove domain"), "domain-remove",
                    e -> {
                        if (MUi.confirm("Remove domain",
                                "The store stops answering on this domain.", "Remove")) {
                            claim("");
                        }
                    })));
        }
        Ui.relayout(page);
    }

    private void claim(String host) {
        Api.domains().claim(MerchantState.storeId(), new CustomDomainDto(host, null, false, 0,
                null, null, null, null), Net.to(this::paint, Ui::fail));
    }
}
