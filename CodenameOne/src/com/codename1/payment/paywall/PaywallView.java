/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.payment.paywall;

import com.codename1.components.SpanLabel;
import com.codename1.io.Log;
import com.codename1.payment.CommerceManager;
import com.codename1.payment.CommercePackage;
import com.codename1.payment.CustomerInfo;
import com.codename1.payment.EntitlementInfo;
import com.codename1.payment.Offering;
import com.codename1.payment.Product;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.URLImage;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A paywall built from a paywall document and the offering it sells.
///
/// The document is a tree of nodes, each a JSON object with a `type`:
/// `stack`, `text`, `image`, `icon`, `features`, `packages`,
/// `purchaseButton`, `restoreButton`, `closeButton`, `carousel`, `footer`
/// and `spacer`. A node of any other type is skipped, so a document made for
/// a newer client still shows. Sizes in the document are millimetres.
///
/// A text is first looked up in the document's `strings`, by the device's
/// locale and then the document's default locale, and used as written when
/// it is not a key there. Then `{{price}}`, `{{period}}`, `{{trialPeriod}}`,
/// `{{introPrice}}`, `{{productName}}` and `{{packageId}}` are replaced from
/// the selected package, again whenever the selection changes. The price is
/// the store's localized one when the store listed the product.
///
/// #### Styling
///
/// Every element has a UIID, so a theme can restyle the paywall. The colors
/// the document names -- its `dark` set when the device is in dark mode, its
/// `light` set otherwise -- are applied on top of the theme.
///
/// - `Paywall`: the view itself
/// - `PaywallStack`: a `stack` node
/// - `PaywallTitle`, `PaywallHeadline`, `PaywallBody`, `PaywallCaption`: the
///   text of a `text` node, by its style
/// - `PaywallImage`, `PaywallIcon`: an `image` and an `icon` node
/// - `PaywallFeatures`, `PaywallFeature`, `PaywallFeatureIcon`,
///   `PaywallFeatureText`: a `features` node, one of its rows and the row's
///   parts
/// - `PaywallPackages`: a `packages` node
/// - `PaywallPackage`, `PaywallPackageTitle`, `PaywallPackagePrice`: a
///   package and its two lines
/// - `PaywallPackageSelected`, `PaywallPackageTitleSelected`,
///   `PaywallPackagePriceSelected`: the same for the selected package
/// - `PaywallPurchaseButton`, `PaywallRestoreButton`, `PaywallCloseButton`
/// - `PaywallCloseBar`: the row holding the close button
/// - `PaywallCarousel`: a `carousel` node
/// - `PaywallFooter`, `PaywallFooterLink`: a `footer` node and its links
/// - `PaywallSpacer`: a `spacer` node, and the gap between a stack's children
///
/// #### Events
///
/// The view reports `IMPRESSION` when it is first shown, `PURCHASE_START`,
/// `PURCHASE` and `CLOSE` through
/// [CommerceManager#logEvent(String, String, String, String)].
public class PaywallView extends Container {
    private static final String[] ICON_NAMES = {
        "check", "check_circle", "done", "star", "lock", "favorite", "cloud", "bolt", "sync",
        "security", "block", "speed", "support", "person", "group", "notifications", "download",
        "info", "verified", "all_inclusive", "close"
    };
    private static final char[] ICON_CHARS = {
        FontImage.MATERIAL_CHECK, FontImage.MATERIAL_CHECK_CIRCLE, FontImage.MATERIAL_DONE,
        FontImage.MATERIAL_STAR, FontImage.MATERIAL_LOCK, FontImage.MATERIAL_FAVORITE,
        FontImage.MATERIAL_CLOUD, FontImage.MATERIAL_BOLT, FontImage.MATERIAL_SYNC,
        FontImage.MATERIAL_SECURITY, FontImage.MATERIAL_BLOCK, FontImage.MATERIAL_SPEED,
        FontImage.MATERIAL_SUPPORT, FontImage.MATERIAL_PERSON, FontImage.MATERIAL_GROUP,
        FontImage.MATERIAL_NOTIFICATIONS, FontImage.MATERIAL_DOWNLOAD, FontImage.MATERIAL_INFO,
        FontImage.MATERIAL_VERIFIED, FontImage.MATERIAL_ALL_INCLUSIVE, FontImage.MATERIAL_CLOSE
    };

    private final Offering offering;
    private final Map document;
    private final String paywallId;
    private final List<Map> stringTables = new ArrayList<Map>();
    private final Map colors;
    private final List<Container> packageRows = new ArrayList<Container>();
    private final List<CommercePackage> rowPackages = new ArrayList<CommercePackage>();
    private final List<Component> textComponents = new ArrayList<Component>();
    private final List<String> textSources = new ArrayList<String>();
    private CommercePackage selected;
    private CommercePackage pending;
    private boolean restoring;
    private boolean impressionLogged;
    private boolean closed;
    private PaywallListener listener;
    private Runnable closeHandler;
    private ActionListener customerListener;
    private boolean webPurchaseStarted;
    private Form shownForm;
    private final ActionListener showListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            formShown();
        }
    };

    /// Builds the paywall of an offering from the offering's own document,
    /// or from [the built-in one][Paywall#createDefault(Offering)] when it
    /// has none.
    ///
    /// #### Parameters
    ///
    /// - `offering`: the offering to sell
    public PaywallView(Offering offering) {
        this(offering, offering == null ? null : offering.getPaywall());
    }

    /// Builds a paywall from a document.
    ///
    /// #### Parameters
    ///
    /// - `offering`: the offering to sell
    ///
    /// - `paywallDocument`: the parsed paywall document; null uses
    ///   [the built-in one][Paywall#createDefault(Offering)]
    public PaywallView(Offering offering, Map paywallDocument) {
        super(BoxLayout.y());
        this.offering = offering;
        document = paywallDocument == null ? Paywall.createDefault(offering) : paywallDocument;
        Object id = document.get("id");
        paywallId = id == null ? null : id.toString();
        setUIID("Paywall");
        setScrollableY(true);
        loadStrings();
        colors = chooseColors();
        int bg = color("background");
        if (bg >= 0) {
            getAllStyles().setBgColor(bg);
            getAllStyles().setBgTransparency(255);
        }
        Object root = document.get("root");
        if (root instanceof Map) {
            Component c = build((Map) root);
            if (c != null) {
                add(c);
            }
        }
        if (selected == null && !rowPackages.isEmpty()) {
            selected = rowPackages.get(0);
        }
        if (selected == null && offering != null && !offering.getPackages().isEmpty()) {
            selected = offering.getPackages().get(0);
        }
        applySelection();
    }

    // ---- public surface ------------------------------------------------------

    /// Sets the listener that is told what happens on this paywall.
    ///
    /// #### Parameters
    ///
    /// - `listener`: the listener, or null for none
    public void setPaywallListener(PaywallListener listener) {
        this.listener = listener;
    }

    /// The offering this paywall sells.
    public Offering getOffering() {
        return offering;
    }

    /// The id of the paywall document, or null when it has none.
    public String getPaywallId() {
        return paywallId;
    }

    /// The package that is selected, which the purchase button buys.
    ///
    /// #### Returns
    ///
    /// the package, or null when the offering has no package
    public CommercePackage getSelectedPackage() {
        return selected;
    }

    /// Selects a package, as tapping it does.
    ///
    /// #### Parameters
    ///
    /// - `packageId`: the id of a package of the offering; any other value
    ///   is ignored
    public void selectPackage(String packageId) {
        CommercePackage pkg = offering == null ? null : offering.getPackage(packageId);
        if (pkg == null) {
            return;
        }
        selected = pkg;
        applySelection();
        if (getComponentForm() != null) {
            revalidate();
        }
    }

    /// Buys the selected package, as the purchase button does. The listener
    /// hears `purchaseStarted` now and `purchaseCompleted` once the cloud
    /// reports the customer as owning the product.
    public void purchase() {
        CommercePackage pkg = selected;
        if (pkg == null) {
            return;
        }
        pending = pkg;
        log("PURCHASE_START", pkg.getId());
        if (listener != null) {
            listener.purchaseStarted(pkg);
        }
        watchCustomer();
        CommerceManager cm = CommerceManager.getInstance();
        if (cm.isWebPurchase(pkg)) {
            // The customer pays in the browser and nothing reports back:
            // the cloud is asked again when this form is next shown.
            if (cm.purchaseOnWeb(pkg)) {
                webPurchaseStarted = true;
            }
            return;
        }
        cm.purchase(pkg);
    }

    /// Restores the purchases of the store account, as the restore button
    /// does. The listener hears `restoreCompleted` when the customer
    /// information changes afterwards.
    public void restore() {
        restoring = true;
        watchCustomer();
        CommerceManager.getInstance().restore();
    }

    /// Closes the paywall, as the close button does. A purchase that was
    /// started and not confirmed yet is reported as cancelled.
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        boolean cancelled = pending != null;
        pending = null;
        restoring = false;
        stopWatchingCustomer();
        log("CLOSE", null);
        if (listener != null) {
            if (cancelled) {
                listener.purchaseCancelled();
            }
            listener.closed();
        }
        if (closeHandler != null) {
            closeHandler.run();
        }
    }

    /// Sets what closing the paywall does to the screen, after the listener
    /// was told. [Paywall#show(Offering, PaywallListener)] sets it to return
    /// to the previous form; an application that embeds the view in a form
    /// of its own sets it to whatever removes the view.
    ///
    /// #### Parameters
    ///
    /// - `closeHandler`: run on the EDT when the paywall closes, or null
    public void setCloseHandler(Runnable closeHandler) {
        this.closeHandler = closeHandler;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        if (!impressionLogged) {
            impressionLogged = true;
            log("IMPRESSION", null);
        }
        Form form = getComponentForm();
        if (form != null && shownForm != form) {
            if (shownForm != null) {
                shownForm.removeShowListener(showListener);
            }
            shownForm = form;
            form.addShowListener(showListener);
        }
    }

    /// Asks the cloud again once the form is back after a web checkout was
    /// opened, which is how a purchase made in the browser is learned of.
    /// The refresh blocks on the network, so it runs in the background; the
    /// customer information listener hears the answer.
    void formShown() {
        if (!webPurchaseStarted || closed) {
            return;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override
            public void run() {
                CommerceManager.getInstance().refresh();
            }
        });
    }

    // ---- purchase tracking ---------------------------------------------------

    private void watchCustomer() {
        if (customerListener != null) {
            return;
        }
        customerListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Object source = evt.getSource();
                if (source instanceof CustomerInfo) {
                    customerChanged((CustomerInfo) source);
                }
            }
        };
        CommerceManager.getInstance().addCustomerInfoListener(customerListener);
    }

    private void stopWatchingCustomer() {
        if (customerListener != null) {
            CommerceManager.getInstance().removeCustomerInfoListener(customerListener);
            customerListener = null;
        }
    }

    private void customerChanged(CustomerInfo info) {
        if (pending != null && owns(info, pending.getProductId())) {
            CommercePackage bought = pending;
            pending = null;
            log("PURCHASE", bought.getId());
            if (listener != null) {
                listener.purchaseCompleted(info);
            }
        }
        if (restoring) {
            restoring = false;
            if (listener != null) {
                listener.restoreCompleted(info);
            }
        }
        if (pending == null) {
            stopWatchingCustomer();
        }
    }

    private static boolean owns(CustomerInfo info, String productId) {
        if (productId == null) {
            return false;
        }
        if (info.getActiveSubscriptions().contains(productId)) {
            return true;
        }
        for (EntitlementInfo e : info.getActiveEntitlements()) {
            if (productId.equals(e.getProductId())) {
                return true;
            }
        }
        return false;
    }

    private void log(String type, String packageId) {
        CommerceManager.getInstance().logEvent(type,
                offering == null ? null : offering.getId(), paywallId, packageId);
    }

    // ---- building ------------------------------------------------------------

    /// Builds the component of one node, or null for a node to skip.
    private Component build(Map node) {
        String type = str(node.get("type"));
        if (type == null) {
            return null;
        }
        if ("stack".equalsIgnoreCase(type)) {
            return buildStack(node);
        }
        if ("text".equalsIgnoreCase(type)) {
            return buildText(node);
        }
        if ("image".equalsIgnoreCase(type)) {
            return buildImage(node);
        }
        if ("icon".equalsIgnoreCase(type)) {
            Label icon = new Label();
            icon.setUIID("PaywallIcon");
            setIcon(icon, str(node.get("name")));
            tint(icon.getAllStyles(), "accent");
            return icon;
        }
        if ("features".equalsIgnoreCase(type)) {
            return buildFeatures(node);
        }
        if ("packages".equalsIgnoreCase(type)) {
            return buildPackages(node);
        }
        if ("purchaseButton".equalsIgnoreCase(type)) {
            Button b = new Button();
            b.setUIID("PaywallPurchaseButton");
            int accent = color("accent");
            if (accent >= 0) {
                b.getAllStyles().setBgColor(accent);
                b.getAllStyles().setBgTransparency(255);
            }
            tint(b.getAllStyles(), "accentText");
            registerText(b, textOr(node, "Continue"));
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    purchase();
                }
            });
            return b;
        }
        if ("restoreButton".equalsIgnoreCase(type)) {
            Button b = new Button();
            b.setUIID("PaywallRestoreButton");
            tint(b.getAllStyles(), "muted");
            registerText(b, textOr(node, "Restore purchases"));
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    restore();
                }
            });
            return b;
        }
        if ("closeButton".equalsIgnoreCase(type)) {
            Button b = new Button();
            b.setUIID("PaywallCloseButton");
            setIcon(b, "close");
            tint(b.getAllStyles(), "muted");
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    close();
                }
            });
            Container bar = new Container(new FlowLayout(Component.RIGHT));
            bar.setUIID("PaywallCloseBar");
            bar.add(b);
            return bar;
        }
        if ("carousel".equalsIgnoreCase(type)) {
            Tabs tabs = new Tabs();
            tabs.setUIID("PaywallCarousel");
            tabs.hideTabs();
            tabs.setSwipeActivated(true);
            for (Component c : buildChildren(node)) {
                tabs.addTab("", (Image) null, c);
            }
            return tabs;
        }
        if ("footer".equalsIgnoreCase(type)) {
            Container footer = new Container(new FlowLayout(Component.CENTER));
            footer.setUIID("PaywallFooter");
            addLink(footer, str(node.get("termsUrl")), "footer.terms", "Terms of Service");
            addLink(footer, str(node.get("privacyUrl")), "footer.privacy", "Privacy Policy");
            return footer;
        }
        if ("spacer".equalsIgnoreCase(type)) {
            return gap(0, mm(node.get("size"), 2));
        }
        return null;
    }

    private List<Component> buildChildren(Map node) {
        List<Component> out = new ArrayList<Component>();
        Object children = node.get("children");
        if (children instanceof List) {
            for (Object o : (List) children) {
                if (o instanceof Map) {
                    Component c = build((Map) o);
                    if (c != null) {
                        out.add(c);
                    }
                }
            }
        }
        return out;
    }

    private Component buildStack(Map node) {
        boolean horizontal = "x".equalsIgnoreCase(str(node.get("axis")));
        Container stack = new Container(horizontal ? BoxLayout.x() : BoxLayout.y());
        stack.setUIID("PaywallStack");
        int gap = mm(node.get("gap"), 0);
        boolean first = true;
        for (Component c : buildChildren(node)) {
            if (!first && gap > 0) {
                stack.add(horizontal ? gap(gap, 0) : gap(0, gap));
            }
            first = false;
            stack.add(c);
        }
        if (node.get("padding") != null) {
            int p = mm(node.get("padding"), 0);
            Style s = stack.getAllStyles();
            s.setPaddingUnit(Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS,
                    Style.UNIT_TYPE_PIXELS, Style.UNIT_TYPE_PIXELS);
            s.setPadding(p, p, p, p);
        }
        int align = alignment(str(node.get("align")));
        if (horizontal && align != Component.LEFT) {
            // A box layout fills its axis; a flow layout places the row.
            Container holder = new Container(new FlowLayout(align));
            holder.setUIID("PaywallStack");
            holder.add(stack);
            return holder;
        }
        return stack;
    }

    private Component buildText(Map node) {
        String style = str(node.get("style"));
        String uiid = "PaywallBody";
        String colorKey = "text";
        if ("title".equalsIgnoreCase(style)) {
            uiid = "PaywallTitle";
        } else if ("headline".equalsIgnoreCase(style)) {
            uiid = "PaywallHeadline";
        } else if ("caption".equalsIgnoreCase(style)) {
            uiid = "PaywallCaption";
            colorKey = "muted";
        }
        SpanLabel label = new SpanLabel();
        label.setTextUIID(uiid);
        tint(label.getTextAllStyles(), colorKey);
        if (node.get("align") != null) {
            int align = alignment(str(node.get("align")));
            label.getTextAllStyles().setAlignment(align);
            label.setTextBlockAlign(align);
        }
        registerText(label, textOr(node, ""));
        return label;
    }

    private Component buildImage(Map node) {
        Label label = new Label();
        label.setUIID("PaywallImage");
        String url = str(node.get("url"));
        if (url == null || url.length() == 0) {
            return label;
        }
        int height = mm(node.get("height"), 40);
        int width = Math.max(1, Display.getInstance().getDisplayWidth());
        try {
            EncodedImage placeholder = EncodedImage.createFromImage(
                    Image.createImage(width, Math.max(1, height), 0), false);
            label.setIcon(URLImage.createToStorage(placeholder,
                    "CN1Paywall_" + Integer.toHexString(url.hashCode()), url,
                    URLImage.RESIZE_SCALE_TO_FILL));
        } catch (Throwable t) {
            // A paywall without its picture still sells.
            Log.p("PaywallView: could not load " + url + ": " + t);
        }
        return label;
    }

    private Component buildFeatures(Map node) {
        Container features = new Container(BoxLayout.y());
        features.setUIID("PaywallFeatures");
        Object items = node.get("items");
        if (items instanceof List) {
            for (Object o : (List) items) {
                if (!(o instanceof Map)) {
                    continue;
                }
                Map item = (Map) o;
                Container row = new Container(BoxLayout.x());
                row.setUIID("PaywallFeature");
                Label icon = new Label();
                icon.setUIID("PaywallFeatureIcon");
                setIcon(icon, str(item.get("icon")));
                tint(icon.getAllStyles(), "accent");
                SpanLabel text = new SpanLabel();
                text.setTextUIID("PaywallFeatureText");
                tint(text.getTextAllStyles(), "text");
                registerText(text, textOr(item, ""));
                row.add(icon);
                row.add(text);
                features.add(row);
            }
        }
        return features;
    }

    private Component buildPackages(Map node) {
        List<CommercePackage> all = offering == null
                ? new ArrayList<CommercePackage>() : offering.getPackages();
        boolean tiles = "tiles".equalsIgnoreCase(str(node.get("style")));
        Container holder = new Container(tiles && !all.isEmpty()
                ? new GridLayout(1, all.size()) : BoxLayout.y());
        holder.setUIID("PaywallPackages");
        String preselected = str(node.get("selected"));
        for (final CommercePackage pkg : all) {
            Container row = new Container(BoxLayout.y());
            row.setName("PaywallPackage_" + pkg.getId());
            Button title = new Button(packageTitle(pkg));
            Label price = new Label(packagePrice(pkg));
            title.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    selectPackage(pkg.getId());
                }
            });
            row.add(title);
            row.add(price);
            // The whole row acts as the one button inside it.
            row.setLeadComponent(title);
            holder.add(row);
            packageRows.add(row);
            rowPackages.add(pkg);
            if (selected == null && preselected != null && preselected.equals(pkg.getId())) {
                selected = pkg;
            }
        }
        return holder;
    }

    /// Gives every package row the UIIDs of its state, which also drops the
    /// colors a previous state painted on it, then fills in the texts that
    /// name the selected package.
    private void applySelection() {
        for (int i = 0; i < packageRows.size(); i++) {
            Container row = packageRows.get(i);
            boolean on = rowPackages.get(i) == selected; //NOPMD CompareObjectsWithEquals - the selection is one of these instances
            String suffix = on ? "Selected" : "";
            row.setUIID("PaywallPackage" + suffix);
            row.getComponentAt(0).setUIID("PaywallPackageTitle" + suffix);
            row.getComponentAt(1).setUIID("PaywallPackagePrice" + suffix);
            if (on) {
                int accent = color("accent");
                if (accent >= 0) {
                    row.getAllStyles().setBgColor(accent);
                    row.getAllStyles().setBgTransparency(255);
                }
                tint(row.getComponentAt(0).getAllStyles(), "accentText");
                tint(row.getComponentAt(1).getAllStyles(), "accentText");
            } else {
                tint(row.getComponentAt(0).getAllStyles(), "text");
                tint(row.getComponentAt(1).getAllStyles(), "muted");
            }
        }
        for (int i = 0; i < textComponents.size(); i++) {
            Component c = textComponents.get(i);
            String text = substitute(lookup(textSources.get(i)));
            if (c instanceof SpanLabel) {
                ((SpanLabel) c).setText(text);
            } else if (c instanceof Label) {
                ((Label) c).setText(text);
            }
        }
    }

    private void addLink(Container footer, String url, String key, String fallback) {
        if (url == null || url.length() == 0) {
            return;
        }
        String text = lookup(key);
        Button link = new Button(key.equals(text) ? fallback : text);
        link.setUIID("PaywallFooterLink");
        tint(link.getAllStyles(), "muted");
        link.addActionListener(new OpenUrl(url));
        footer.add(link);
    }

    /// Opens a page of the footer in the platform's browser.
    private static final class OpenUrl implements ActionListener {
        private final String url;

        OpenUrl(String url) {
            this.url = url;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            Display.getInstance().execute(url);
        }
    }

    private void registerText(Component c, String source) {
        textComponents.add(c);
        textSources.add(source);
    }

    private static String textOr(Map node, String fallback) {
        String t = str(node.get("text"));
        return t == null ? fallback : t;
    }

    private static Component gap(int width, int height) {
        Component c = new Gap(width, height);
        c.setUIID("PaywallSpacer");
        return c;
    }

    /// An empty component of a fixed size.
    private static final class Gap extends Component {
        private final int width;
        private final int height;

        Gap(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(width, height);
        }
    }

    private static void setIcon(Label label, String name) {
        char icon = FontImage.MATERIAL_CHECK;
        if (name != null) {
            for (int i = 0; i < ICON_NAMES.length; i++) {
                if (ICON_NAMES[i].equalsIgnoreCase(name)) {
                    icon = ICON_CHARS[i];
                    break;
                }
            }
        }
        try {
            FontImage.setMaterialIcon(label, icon);
        } catch (Throwable t) {
            // a port with no icon font shows the element without its icon
            Log.p("PaywallView: no material icon font: " + t);
        }
    }

    // ---- package texts -------------------------------------------------------

    private String packageTitle(CommercePackage pkg) {
        String key = "package." + pkg.getId();
        String text = lookup(key);
        if (!key.equals(text)) {
            return text;
        }
        Product product = pkg.getProduct();
        if (product != null && product.getDisplayName() != null && product.getDisplayName().length() > 0) {
            return product.getDisplayName();
        }
        if (pkg.isType(CommercePackage.TYPE_LIFETIME)) {
            return "Lifetime";
        }
        if (pkg.isType(CommercePackage.TYPE_ANNUAL)) {
            return "Annual";
        }
        if (pkg.isType(CommercePackage.TYPE_SIX_MONTH)) {
            return "6 months";
        }
        if (pkg.isType(CommercePackage.TYPE_THREE_MONTH)) {
            return "3 months";
        }
        if (pkg.isType(CommercePackage.TYPE_TWO_MONTH)) {
            return "2 months";
        }
        if (pkg.isType(CommercePackage.TYPE_MONTHLY)) {
            return "Monthly";
        }
        if (pkg.isType(CommercePackage.TYPE_WEEKLY)) {
            return "Weekly";
        }
        return pkg.getId() == null ? "" : pkg.getId();
    }

    /// The second line of a package: the store's localized price and the
    /// billing period, or whichever of them is known.
    private static String packagePrice(CommercePackage pkg) {
        String price = price(pkg);
        String period = periodText(pkg.getPeriod());
        if (price.length() > 0 && period.length() > 0) {
            return price + " / " + period;
        }
        return price.length() > 0 ? price : period;
    }

    private static String price(CommercePackage pkg) {
        Product product = pkg.getProduct();
        if (product != null && product.getLocalizedPrice() != null) {
            return product.getLocalizedPrice();
        }
        return "";
    }

    /// An ISO-8601 duration of one unit, such as `P1M` or `P7D`, in words.
    /// Anything else is shown as it is written.
    static String periodText(String iso) {
        if (iso == null || iso.length() < 3 || (iso.charAt(0) != 'P' && iso.charAt(0) != 'p')) {
            return iso == null ? "" : iso;
        }
        int count = 0;
        for (int i = 1; i < iso.length() - 1; i++) {
            char c = iso.charAt(i);
            if (c < '0' || c > '9' || count > 100000) {
                return iso;
            }
            count = count * 10 + (c - '0');
        }
        String unit;
        switch (iso.charAt(iso.length() - 1)) {
            case 'Y': case 'y': unit = "year"; break;
            case 'M': case 'm': unit = "month"; break;
            case 'W': case 'w': unit = "week"; break;
            case 'D': case 'd': unit = "day"; break;
            default: return iso;
        }
        return count == 1 ? unit : count + " " + unit + "s";
    }

    // ---- strings, variables, colors, sizes -----------------------------------

    /// Collects the string tables to search, most specific first: the
    /// device's language and country, its language, the document's default.
    private void loadStrings() {
        Object strings = document.get("strings");
        if (!(strings instanceof Map)) {
            return;
        }
        Map all = (Map) strings;
        String language = null;
        String country = null;
        try {
            language = Display.getInstance().getLocalizationManager().getLanguage();
            country = Display.getInstance().getLocalizationManager().getLocale();
        } catch (Throwable t) {
            // no localization on this port: the default locale's strings
            Log.p("PaywallView: no locale: " + t);
        }
        if (language != null && country != null) {
            addTable(all.get(language + "_" + country));
            addTable(all.get(language + "-" + country));
        }
        if (language != null) {
            addTable(all.get(language));
        }
        Object def = document.get("defaultLocale");
        if (def != null) {
            addTable(all.get(def.toString()));
        }
    }

    private void addTable(Object table) {
        if (table instanceof Map) {
            stringTables.add((Map) table);
        }
    }

    /// The string a key names, or the key itself when no table has it.
    private String lookup(String key) {
        if (key == null) {
            return "";
        }
        for (Map table : stringTables) {
            Object v = table.get(key);
            if (v != null) {
                return v.toString();
            }
        }
        return key;
    }

    /// Replaces the `{{variables}}` of a text from the selected package. A
    /// variable with no value becomes empty, and an unknown one stays as it
    /// is written.
    private String substitute(String text) {
        if (text.indexOf("{{") < 0) {
            return text;
        }
        Map<String, String> vars = new HashMap<String, String>();
        CommercePackage p = selected;
        vars.put("price", p == null ? "" : price(p));
        vars.put("period", p == null ? "" : periodText(p.getPeriod()));
        vars.put("trialPeriod", p == null ? "" : periodText(p.getTrialPeriod()));
        vars.put("introPrice", p == null || p.getIntroPrice() == null ? "" : p.getIntroPrice());
        vars.put("productName", p == null ? "" : packageTitle(p));
        vars.put("packageId", p == null || p.getId() == null ? "" : p.getId());
        StringBuilder out = new StringBuilder();
        int pos = 0;
        while (pos < text.length()) {
            int open = text.indexOf("{{", pos);
            int end = open < 0 ? -1 : text.indexOf("}}", open + 2);
            if (end < 0) {
                out.append(text.substring(pos));
                break;
            }
            out.append(text.substring(pos, open));
            String value = vars.get(text.substring(open + 2, end).trim());
            if (value == null) {
                out.append(text.substring(open, end + 2));
            } else {
                out.append(value);
            }
            pos = end + 2;
        }
        return out.toString();
    }

    private Map chooseColors() {
        Object all = document.get("colors");
        if (!(all instanceof Map)) {
            return null;
        }
        Boolean dark = Display.getInstance().isDarkMode();
        Object set = null;
        if (dark != null && dark.booleanValue()) {
            set = ((Map) all).get("dark");
        }
        if (!(set instanceof Map)) {
            set = ((Map) all).get("light");
        }
        return set instanceof Map ? (Map) set : null;
    }

    /// A color of the chosen set as RGB, or -1 when the document does not
    /// name it or it is not `#rrggbb`.
    private int color(String key) {
        if (colors == null) {
            return -1;
        }
        Object v = colors.get(key);
        if (v == null) {
            return -1;
        }
        String s = v.toString();
        if (s.length() != 7 || s.charAt(0) != '#') {
            return -1;
        }
        int rgb = 0;
        for (int i = 1; i < 7; i++) {
            char c = s.charAt(i);
            int digit;
            if (c >= '0' && c <= '9') {
                digit = c - '0';
            } else if (c >= 'a' && c <= 'f') {
                digit = c - 'a' + 10;
            } else if (c >= 'A' && c <= 'F') {
                digit = c - 'A' + 10;
            } else {
                return -1;
            }
            rgb = (rgb << 4) | digit;
        }
        return rgb;
    }

    private void tint(Style style, String key) {
        int c = color(key);
        if (c >= 0) {
            style.setFgColor(c);
        }
    }

    private static int alignment(String align) {
        if ("center".equalsIgnoreCase(align)) {
            return Component.CENTER;
        }
        if ("end".equalsIgnoreCase(align) || "right".equalsIgnoreCase(align)) {
            return Component.RIGHT;
        }
        return Component.LEFT;
    }

    /// A size of the document, in millimetres, as pixels.
    private static int mm(Object value, float fallback) {
        float v = fallback;
        if (value instanceof Number) {
            v = ((Number) value).floatValue();
        }
        if (v <= 0) {
            return 0;
        }
        return Display.getInstance().convertToPixels(v);
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
