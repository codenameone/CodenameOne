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
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.NativeDragAndDrop;
import com.codename1.ui.NativeDropEvent;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.GridColumn;
import com.codename1.util.Base64;
import com.codenameone.examples.bazaar.api.OptionDto;
import com.codenameone.examples.bazaar.api.PhotoDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.ProductPhotoDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ops.Customs;
import com.codenameone.examples.bazaar.ops.Margins;
import com.codenameone.examples.bazaar.ops.UnitPricing;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Photos;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/// Editing one product: its words, its options and the variants they make,
/// and its photos.
///
/// The variant matrix is regenerated from the options whenever they change
/// (`VariantMatrix` keeps what was typed into existing variants). On a wide
/// screen the variants are a grid edited in place -- a USB barcode scanner
/// types straight into the barcode cell -- and on a phone a list whose rows
/// open a small form with a camera scan button. Photos come from the camera,
/// the gallery or, on a desktop, files dragged onto the gallery.
final class ProductEditor extends Container {
    private static final int OPTION_ROWS = VariantMatrix.MAX_OPTIONS;

    private final MerchantPane pane;
    private final Runnable saved;
    private ProductDto product;
    private final List<VariantDto> variants = new ArrayList<VariantDto>();
    private final Rows<VariantDto> variantRows = new Rows<VariantDto>();
    private final TextField[] optionNames = new TextField[OPTION_ROWS];
    private final TextField[] optionValues = new TextField[OPTION_ROWS];
    private TextField title;
    private TextArea description;
    private TextField type;
    private TextField vendor;
    private TextField tags;
    private TextField seoTitle;
    /// The product's taxonomy category id, 0 for none.
    private int categoryId;
    private Button categoryButton;
    private TextArea seoDescription;
    private Picker status;
    private Container variantHost;
    private Container gallery;
    private DataGrid grid;

    private ProductEditor(MerchantPane pane, ProductDto product, Runnable saved) {
        super(new BorderLayout());
        this.pane = pane;
        this.saved = saved;
        setName("product-editor");
        variantRows.col(Lang.tr("Variant"), v -> v.title())
                .col(Lang.tr("SKU"), v -> v.sku(), (v, x) -> change(v, "sku", x))
                .col(Lang.tr("Barcode"), v -> v.barcode(), (v, x) -> change(v, "barcode", x))
                .col(Lang.tr("Price"), v -> Long.valueOf(v.priceCents()), (v, x) -> change(v, "price", x))
                .col(Lang.tr("Compare at"), v -> Long.valueOf(v.compareAtCents()),
                        (v, x) -> change(v, "compare", x))
                .col(Lang.tr("Cost"), v -> Long.valueOf(v.costCents()), (v, x) -> change(v, "cost", x))
                .col(Lang.tr("Margin"), v -> Margins.percent(Margins.marginBps(v.priceCents(),
                        v.costCents())))
                .col(Lang.tr("Weight (g)"), v -> Integer.valueOf(v.weightGrams()),
                        (v, x) -> change(v, "weight", x))
                .col(Lang.tr("Stock"), v -> Integer.valueOf(v.available()));
        show(product);
    }

    /// Opens the editor over `pane`: a new product when `productId` is null.
    static void open(final MerchantPane pane, String productId, final Runnable saved) {
        if (productId == null) {
            pane.push(Lang.tr("New product"), new ProductEditor(pane, null, saved));
            return;
        }
        Api.catalog().merchantProduct(MerchantState.storeId(), productId, Net.to(p ->
                pane.push(p.title(), new ProductEditor(pane, p, saved)), Ui::fail));
    }

    private void show(ProductDto p) {
        product = p;
        removeAll();
        variants.clear();
        if (p != null && p.variants() != null) {
            variants.addAll(p.variants());
        }
        if (variants.isEmpty()) {
            variants.addAll(VariantMatrix.generate(null, new ArrayList<OptionDto>(), null));
        }
        Container page = MUi.page("product-page");
        boolean twoColumns = Layouts.wide();
        Container columns = new Container(twoColumns ? new BorderLayout() : BoxLayout.y());
        Container main = new Container(BoxLayout.y());
        Container side = new Container(BoxLayout.y());
        side.setUIID("MeSide");
        if (twoColumns) {
            columns.add(BorderLayout.CENTER, main);
            columns.add(BorderLayout.EAST, side);
        } else {
            columns.add(main);
            columns.add(side);
        }
        page.add(columns);

        Container words = card(main, "Details");
        title = MUi.text(words, "Title", "product-title", p == null ? "" : p.title());
        words.add(Ui.label("Description", "SvLabel"));
        description = new TextArea(p == null || p.description() == null ? "" : p.description(), 4, 40);
        description.setUIID("SvField");
        description.setName("product-description");
        words.add(description);
        words.add(Ui.link(Lang.tr("Draft description"), "product-draft-description",
                e -> draftDescription()));

        Container opts = card(main, "Options");
        opts.add(Ui.text(Lang.tr("Up to three options, values separated by commas: "
                + "Size: S, M, L. Every combination becomes a variant."), "SvMuted"));
        for (int iter = 0; iter < OPTION_ROWS; iter++) {
            OptionDto o = p != null && p.options() != null && iter < p.options().size()
                    ? p.options().get(iter) : null;
            Container row = new Container(new GridLayout(1, 2));
            optionNames[iter] = new TextField(o == null ? "" : o.name(), Lang.tr("Option name"), 10,
                    TextArea.ANY);
            optionNames[iter].setUIID("SvField");
            optionNames[iter].setName("option-name-" + iter);
            optionValues[iter] = new TextField(o == null ? "" : VariantMatrix.join(o.values()),
                    Lang.tr("Values"), 20, TextArea.ANY);
            optionValues[iter].setUIID("SvField");
            optionValues[iter].setName("option-values-" + iter);
            optionValues[iter].setDoneListener(e -> regenerate());
            row.add(optionNames[iter]).add(optionValues[iter]);
            opts.add(row);
        }
        opts.add(MUi.buttons(Ui.secondary(Lang.tr("Update variants"), "variants-update",
                e -> regenerate())));

        Container vars = card(main, "Variants");
        variantHost = new Container(new BorderLayout());
        vars.add(variantHost);
        paintVariants();

        Container photos = card(main, "Photos");
        gallery = new Container(new FlowLayout());
        gallery.setUIID("MeGallery");
        gallery.setName("product-gallery");
        photos.add(gallery);
        Container photoButtons = new Container(new FlowLayout());
        Button camera = Ui.secondary(Lang.tr("Take photo"), "photo-camera",
                e -> Photos.camera((jpeg, image) -> upload(jpeg)));
        FontImage.setMaterialIcon(camera, FontImage.MATERIAL_ADD_A_PHOTO, 3.2f);
        Button pick = Ui.secondary(Lang.tr("From gallery"), "photo-gallery",
                e -> Photos.gallery((jpeg, image) -> upload(jpeg)));
        FontImage.setMaterialIcon(pick, FontImage.MATERIAL_PHOTO_LIBRARY, 3.2f);
        camera.setEnabled(com.codename1.capture.Capture.hasCamera());
        photoButtons.add(camera).add(pick);
        photos.add(photoButtons);
        if (p != null && p.id() != null) {
            photos.add(ProductMedia.buttons(p.id(), this::mediaChanged));
        }
        if (NativeDragAndDrop.isSupported()) {
            photos.add(Ui.plain(Lang.tr("Or drag image files onto the photos"), "SvMuted"));
            gallery.setNativeDropTarget(true);
            gallery.addNativeDropListener(e -> {
                if (e instanceof NativeDropEvent) {
                    dropped(((NativeDropEvent) e).getFiles());
                }
            });
        }
        paintGallery();

        Container state = card(side, "Status");
        status = MUi.picker(state, null, "product-status", Words.PRODUCT_STATUSES,
                p == null ? "DRAFT" : p.status());
        Container org = card(side, "Organization");
        type = MUi.text(org, "Type", "product-type", p == null ? "" : p.productType());
        vendor = MUi.text(org, "Vendor", "product-vendor", p == null ? "" : p.vendor());
        tags = MUi.text(org, "Tags", "product-tags", p == null ? "" : VariantMatrix.join(p.tags()));
        org.add(Ui.label("Category", "SvLabel"));
        categoryId = p == null ? 0 : Math.max(0, p.categoryId());
        categoryButton = new Button(Lang.tr("None"));
        categoryButton.setUIID("SvField");
        categoryButton.setName("product-category");
        categoryButton.addActionListener(e -> CategoryPicker.show(categoryId, (id, path) -> {
            categoryId = id;
            categoryButton.setText(id == 0 ? Lang.tr("None") : path);
            categoryButton.getParent().revalidate();
        }));
        org.add(categoryButton);
        if (categoryId > 0) {
            CategoryPicker.label(categoryId, path -> {
                categoryButton.setText(path);
                categoryButton.getParent().revalidate();
            });
        }
        Container seo = card(side, "Search engine listing");
        seoTitle = MUi.text(seo, "Page title", "product-seo-title", p == null ? "" : p.seoTitle());
        seo.add(Ui.label("Meta description", "SvLabel"));
        seoDescription = new TextArea(p == null || p.seoDescription() == null ? ""
                : p.seoDescription(), 3, 30);
        seoDescription.setUIID("SvField");
        seoDescription.setName("product-seo-description");
        seo.add(seoDescription);
        if (p != null && p.id() != null) {
            ProductChannels.add(card(main, "Bundle"), card(side, "Publishing schedule"), p.id());
            ProductTranslations.add(card(side, "Translations"), p.id());
        }

        Container bottom = MUi.buttons();
        if (p != null) {
            bottom.add(Ui.secondary(Lang.tr("Duplicate"), "product-duplicate", e -> duplicate()));
            if (!"ARCHIVED".equals(p.status())) {
                bottom.add(Ui.danger(Lang.tr("Archive"), "product-archive", e -> archive()));
            }
        }
        bottom.add(Ui.primary(Lang.tr("Save"), "product-save", e -> save()));
        add(BorderLayout.CENTER, page);
        add(BorderLayout.SOUTH, bottom);
        Ui.relayout(this);
    }

    private static Container card(Container to, String heading) {
        Ui.section(to, heading);
        Container c = new Container(BoxLayout.y());
        c.setUIID("MeCard");
        to.add(c);
        return c;
    }

    private List<OptionDto> options() {
        List<OptionDto> out = new ArrayList<OptionDto>();
        for (int iter = 0; iter < OPTION_ROWS; iter++) {
            List<String> values = VariantMatrix.split(optionValues[iter].getText());
            String name = optionNames[iter].getText().trim();
            if (values.isEmpty()) {
                continue;
            }
            out.add(new OptionDto(null, name.length() == 0 ? Lang.tr("Option") : name, values,
                    out.size()));
        }
        return out;
    }

    private void regenerate() {
        try {
            List<VariantDto> made = VariantMatrix.generate(product == null ? null : product.id(),
                    options(), variants);
            variants.clear();
            variants.addAll(made);
        } catch (IllegalArgumentException tooMany) {
            Ui.say(Lang.tr("A product can have at most {0} variants",
                    String.valueOf(VariantMatrix.MAX_VARIANTS)));
            return;
        }
        paintVariants();
    }

    private void paintVariants() {
        variantHost.removeAll();
        variantRows.set(variants);
        if (Layouts.wide()) {
            grid = MUi.grid(variantRows, "variants-grid");
            grid.setSelectionMode(DataGrid.SelectionMode.SINGLE_CELL);
            grid.getColumn(0).setFlex(1);
            for (int iter = 3; iter <= 5; iter++) {
                grid.getColumn(iter).setKind(GridColumn.Kind.NUMBER).setEditable(true)
                        .setFormatter(new OrdersPane.MoneyFormat());
            }
            grid.getColumn(6).setKind(GridColumn.Kind.NUMBER).setEditable(true);
            grid.getColumn(7).setKind(GridColumn.Kind.NUMBER);
            grid.getColumn(1).setEditable(true);
            grid.getColumn(2).setEditable(true);
            grid.setPreferredH(Math.min(CN.convertToPixels(80f),
                    CN.convertToPixels(8f) * (variants.size() + 1) + CN.convertToPixels(2f)));
            grid.addActionListener(e -> {
                VariantDto v = variantRows.at(grid.getModelRow(grid.getSelectedRow()));
                if (v != null) {
                    editVariant(v);
                }
            });
            variantHost.add(BorderLayout.CENTER, grid);
        } else {
            Container list = new Container(BoxLayout.y());
            for (final VariantDto v : variants) {
                Container row = Ui.row((char) 0, v.title(), (v.sku() == null || v.sku().length() == 0
                        ? Lang.tr("No SKU") : v.sku()) + Ui.DOT + Lang.tr("{0} in stock",
                        String.valueOf(v.available())), Ui.plain(MerchantState.money(v.priceCents()),
                        "SvRowValue"));
                list.add(Ui.tap(row, "variant-" + v.position(), e -> editVariant(v)));
            }
            variantHost.add(BorderLayout.CENTER, list);
        }
        Ui.relayout(variantHost);
    }

    /// A small form for one variant, with a barcode scan button.
    private void editVariant(final VariantDto v) {
        Container body = new Container(BoxLayout.y());
        body.add(Ui.plain(v.title(), "SvHeading"));
        TextField sku = MUi.text(body, "SKU", "variant-sku", v.sku());
        body.add(Ui.label("Barcode", "SvLabel"));
        TextField barcode = Scan.field("variant-barcode", null);
        barcode.setText(v.barcode() == null ? "" : v.barcode());
        Container scanRow = new Container(new BorderLayout());
        scanRow.add(BorderLayout.CENTER, barcode);
        scanRow.add(BorderLayout.EAST, Scan.button("variant-scan", barcode, null));
        body.add(scanRow);
        TextField price = MUi.money(body, "Price", "variant-price", v.priceCents());
        TextField compare = MUi.money(body, "Compare at price", "variant-compare", v.compareAtCents());
        TextField cost = MUi.money(body, "Cost per item", "variant-cost", v.costCents());
        final Label margin = Ui.label(marginText(v.priceCents(), v.costCents()), "SvLabel");
        margin.setName("variant-margin");
        body.add(margin);
        final TextField priceField = price;
        final TextField costField = cost;
        com.codename1.ui.events.DataChangedListener recalc = (type, index) -> {
            margin.setText(marginText(MUi.cents(priceField), MUi.cents(costField)));
            margin.getParent().revalidate();
        };
        price.addDataChangedListener(recalc);
        cost.addDataChangedListener(recalc);
        TextField weight = MUi.number(body, "Weight in grams", "variant-weight", v.weightGrams());
        CheckBox track = new CheckBox(Lang.tr("Track inventory"));
        track.setSelected(v.trackInventory());
        CheckBox backorder = new CheckBox(Lang.tr("Sell when out of stock"));
        backorder.setSelected(v.allowBackorder());
        body.add(track).add(backorder);
        CheckBox digital = new CheckBox(Lang.tr("Digital download, no shipping"));
        digital.setName("variant-digital");
        digital.setSelected(v.digital());
        body.add(digital);
        if (v.id() != null && v.digital()) {
            body.add(DigitalFiles.button(v));
        }
        body.add(Ui.plain(Lang.tr("Customs"), "SvHeading"));
        TextField hs = MUi.text(body, "HS code", "variant-hs", v.hsCode());
        TextField origin = MUi.text(body, "Country of origin", "variant-origin",
                v.originCountry());
        body.add(Ui.plain(Lang.tr("Unit price"), "SvHeading"));
        TextField measure = MUi.text(body, "Measure", "variant-measure",
                v.unitQuantity() > 0 ? UnitPricing.quantity(v.unitQuantity()) : "");
        TextField unit = MUi.text(body, "Unit (g, kg, ml, l, m...)", "variant-unit",
                v.unitName());
        TextField per = MUi.number(body, "Price per", "variant-per",
                v.unitReference() > 0 ? v.unitReference() : 1);
        TextField perUnit = MUi.text(body, "Unit of that price", "variant-per-unit",
                v.unitReferenceName());
        Command ok = Command.create(Lang.tr("Done"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
        if (Dialog.show(Lang.tr("Variant"), body, cancel, ok) != ok) {
            return;
        }
        String hsCode = Customs.hsCode(hs.getText());
        String country = Customs.country(origin.getText());
        String unitName = unit.getText().trim();
        long quantity = UnitPricing.parseQuantity(measure.getText());
        String refName = perUnit.getText().trim().length() == 0 ? unitName
                : perUnit.getText().trim();
        int reference = MUi.integer(per);
        String problem = VariantChecks.unitProblem(hsCode, country, quantity, unitName,
                reference, refName);
        if (problem != null) {
            Ui.say(Lang.tr(problem));
            return;
        }
        boolean byMeasure = quantity > 0 && unitName.length() > 0;
        replace(v, new VariantDto(v.id(), v.productId(), v.title(), v.option1(), v.option2(),
                v.option3(), sku.getText().trim(), barcode.getText().trim(), MUi.cents(price),
                MUi.cents(compare), MUi.cents(cost), MUi.integer(weight), v.taxable(),
                track.isSelected(), backorder.isSelected(), v.position(), v.available(), v.stock(),
                digital.isSelected(), hsCode, country, byMeasure ? quantity : 0,
                byMeasure ? unitName : "", byMeasure ? reference : 0,
                byMeasure ? refName : ""));
        paintVariants();
    }

    /// "Margin 40.0% - markup 66.7%", or a hint when no cost is entered.
    static String marginText(long priceCents, long costCents) {
        int m = Margins.marginBps(priceCents, costCents);
        if (m == Margins.UNKNOWN) {
            return Lang.tr("Enter a cost to see the margin");
        }
        return Lang.tr("Margin") + " " + Margins.percent(m) + " - " + Lang.tr("markup") + " "
                + Margins.percent(Margins.markupBps(priceCents, costCents));
    }

    /// An edit made in a grid cell.
    private void change(VariantDto v, String field, Object value) {
        String text = value == null ? "" : String.valueOf(value).trim();
        long number = value instanceof Long ? ((Long) value).longValue()
                : value instanceof Number ? ((Number) value).longValue() : ProductCsv.cents(text);
        VariantDto out = new VariantDto(v.id(), v.productId(), v.title(), v.option1(), v.option2(),
                v.option3(),
                "sku".equals(field) ? text : v.sku(),
                "barcode".equals(field) ? text : v.barcode(),
                "price".equals(field) ? number : v.priceCents(),
                "compare".equals(field) ? number : v.compareAtCents(),
                "cost".equals(field) ? number : v.costCents(),
                "weight".equals(field) ? (int) Math.max(0, Math.min(Integer.MAX_VALUE,
                        value instanceof Number ? ((Number) value).longValue() : number / 100))
                        : v.weightGrams(),
                v.taxable(), v.trackInventory(), v.allowBackorder(), v.position(), v.available(),
                v.stock(), v.digital(), v.hsCode(), v.originCountry(), v.unitQuantity(),
                v.unitName(), v.unitReference(), v.unitReferenceName());
        replace(v, out);
        variantRows.replace(variantRows.indexOf(v), out);
    }

    private void replace(VariantDto was, VariantDto now) {
        int at = variants.indexOf(was);
        if (at >= 0) {
            variants.set(at, now);
        }
    }

    private void paintGallery() {
        gallery.removeAll();
        List<ProductPhotoDto> photos = product == null || product.photos() == null
                ? new ArrayList<ProductPhotoDto>() : product.photos();
        int size = CN.convertToPixels(22f);
        for (final ProductPhotoDto photo : photos) {
            Label l = ProductMedia.isMedia(photo) ? ProductMedia.tile(photo, size)
                    : ProductArt.label(product.id(), product.title(), product.productType(),
                        photo.photoId(), size, size, "MeThumb");
            l.setName("photo-" + photo.position());
            Container holder = new Container(new BorderLayout());
            holder.add(BorderLayout.CENTER, l);
            Button remove = Ui.flat(FontImage.MATERIAL_DELETE, "photo-remove-" + photo.position(),
                    e -> removePhoto(photo));
            holder.add(BorderLayout.SOUTH, remove);
            RowMenus.attach(holder, Command.create(Lang.tr("Remove photo"), null,
                    e -> removePhoto(photo)), Command.create(Lang.tr("Move earlier"), null,
                    e -> ProductMedia.move(product.id(), photo, true, this::mediaChanged)),
                    Command.create(Lang.tr("Move later"), null,
                    e -> ProductMedia.move(product.id(), photo, false, this::mediaChanged)));
            gallery.add(holder);
        }
        if (photos.isEmpty()) {
            gallery.add(Ui.plain(product == null ? Lang.tr("Save the product to add photos")
                    : Lang.tr("No photos yet"), "SvMuted"));
        }
        Ui.relayout(gallery);
    }

    private void mediaChanged(ProductDto p) {
        product = p;
        paintGallery();
    }

    private void dropped(String[] files) {
        for (int iter = 0; files != null && iter < files.length; iter++) {
            String f = files[iter];
            if (product != null && product.id() != null && ProductMedia.kindOf(f) != null) {
                ProductMedia.dropped(product.id(), f, this::mediaChanged);
                continue;
            }
            if (!(endsWith(f, ".jpg") || endsWith(f, ".jpeg") || endsWith(f, ".png"))) {
                continue;
            }
            InputStream in = null;
            try {
                in = FileSystemStorage.getInstance().openInputStream(f);
                upload(Util.readInputStream(in), endsWith(f, ".png") ? "image/png" : "image/jpeg");
            } catch (java.io.IOException failed) {
                Log.e(failed);
                Ui.say("That picture could not be read");
            } finally {
                Util.cleanup(in);
            }
        }
    }

    private static boolean endsWith(String s, String suffix) {
        return s != null && s.length() >= suffix.length()
                && s.regionMatches(true, s.length() - suffix.length(), suffix, 0, suffix.length());
    }

    private void upload(byte[] jpeg) {
        upload(jpeg, "image/jpeg");
    }

    private void upload(byte[] bytes, String type) {
        if (product == null || product.id() == null) {
            Ui.say("Save the product to add photos");
            return;
        }
        if (bytes == null) {
            return;
        }
        Api.catalog().addPhoto(MerchantState.storeId(), product.id(),
                new PhotoDto(null, type, Base64.encodeNoNewline(bytes)), Net.to(p -> {
                    product = p;
                    paintGallery();
                }, Ui::fail));
    }

    private void removePhoto(ProductPhotoDto photo) {
        Api.catalog().removePhoto(MerchantState.storeId(), product.id(), photo.id(), Net.to(p -> {
            product = p;
            paintGallery();
        }, Ui::fail));
    }

    private ProductDto collect() {
        String t = title.getText().trim();
        List<OptionDto> opts = options();
        List<VariantDto> vs = new ArrayList<VariantDto>();
        for (int iter = 0; iter < variants.size(); iter++) {
            VariantDto v = variants.get(iter);
            vs.add(new VariantDto(v.id(), v.productId(), v.title(), v.option1(), v.option2(),
                    v.option3(), v.sku(), v.barcode(), v.priceCents(), v.compareAtCents(),
                    v.costCents(), v.weightGrams(), v.taxable(), v.trackInventory(),
                    v.allowBackorder(), iter, v.available(), v.stock(), v.digital(),
                    v.hsCode(), v.originCountry(), v.unitQuantity(), v.unitName(),
                    v.unitReference(), v.unitReferenceName()));
        }
        return new ProductDto(product == null ? null : product.id(), MerchantState.storeId(),
                product == null || product.handle() == null ? Handles.of(t) : product.handle(), t,
                description.getText(), vendor.getText().trim(), type.getText().trim(),
                VariantMatrix.split(tags.getText()), MUi.code(status, Words.PRODUCT_STATUSES),
                seoTitle.getText().trim(), seoDescription.getText().trim(),
                product == null ? null : product.photoId(), 0, 0, opts, vs,
                product == null ? new ArrayList<ProductPhotoDto>() : product.photos(),
                product == null ? new ArrayList<String>() : product.collectionIds(), categoryId);
    }

    private void save() {
        if (title.getText().trim().length() == 0) {
            Ui.say("A product needs a title");
            return;
        }
        ProductDto wanted = collect();
        if (product == null || product.id() == null) {
            Api.catalog().createProduct(MerchantState.storeId(), wanted, Net.to(made -> {
                Ui.say("Product created");
                show(made);
                saved.run();
            }, Ui::fail));
        } else {
            Api.catalog().updateProduct(MerchantState.storeId(), product.id(), wanted, Net.to(p -> {
                Ui.say("Saved");
                show(p);
                saved.run();
            }, Ui::fail));
        }
    }

    private void duplicate() {
        Api.catalog().duplicateProduct(MerchantState.storeId(), product.id(), Net.to(copy -> {
            Ui.say("Duplicated");
            saved.run();
            pane.pop();
            pane.push(copy.title(), new ProductEditor(pane, copy, saved));
        }, Ui::fail));
    }

    private void archive() {
        if (!MUi.confirm("Archive product", "It leaves the store and every sales channel. "
                + "You can make it active again later.", "Archive")) {
            return;
        }
        Api.catalog().archiveProduct(MerchantState.storeId(), product.id(), Net.to(p -> {
            saved.run();
            pane.popAll();
        }, Ui::fail));
    }

    /// Asks the store's AI for a description from the title, vendor, type,
    /// tags and options as they are in the form, and puts the draft in the
    /// description field for the merchant to edit. Saving stays theirs.
    private void draftDescription() {
        StringBuilder options = new StringBuilder();
        for (int iter = 0; iter < optionNames.length; iter++) {
            String name = optionNames[iter].getText().trim();
            String values = optionValues[iter].getText().trim();
            if (name.length() > 0 && values.length() > 0) {
                options.append(options.length() == 0 ? "" : "; ").append(name).append(": ")
                        .append(values);
            }
        }
        String fields = DescriptionDraft.fields(title.getText(), vendor == null ? ""
                : vendor.getText(), type == null ? "" : type.getText(), tags == null ? ""
                : tags.getText(), options.toString());
        Ui.say("Drafting a description...");
        DescriptionDraft.draft(com.codename1.ai.LlmClient.localOpenAiCompatible(
                com.codenameone.examples.bazaar.net.Session.serverUrl() + "/api/merchant/stores/"
                + MerchantState.storeId() + "/ai/v1", "", "bazaar"), fields,
                new AssistantRun.Done() {
                    @Override
                    public void answered(String text) {
                        if (text.length() > 0) {
                            description.setText(text);
                            description.getParent().revalidate();
                        }
                    }

                    @Override
                    public void failed(Throwable error) {
                        Ui.say("The description could not be drafted");
                    }
                });
    }
}
