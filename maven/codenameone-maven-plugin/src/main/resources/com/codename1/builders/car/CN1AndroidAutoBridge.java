/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package com.codename1.impl.android;

import android.graphics.Bitmap;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.ScreenManager;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.GridItem;
import androidx.car.app.model.GridTemplate;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.SectionedItemList;
import androidx.car.app.model.Template;
import androidx.car.app.model.ListTemplate;
import androidx.core.graphics.drawable.IconCompat;
import com.codename1.car.CarAction;
import com.codename1.car.CarActionListener;
import com.codename1.car.CarActionStrip;
import com.codename1.car.CarGridItem;
import com.codename1.car.CarGridTemplate;
import com.codename1.car.CarListTemplate;
import com.codename1.car.CarMessageTemplate;
import com.codename1.car.CarPaneTemplate;
import com.codename1.car.CarRow;
import com.codename1.car.CarSection;
import com.codename1.car.CarTemplate;
import com.codename1.car.spi.CarBridge;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Android Auto backing for the portable com.codename1.car API. Translates the CarTemplate tree
 * into androidx.car.app templates and drives the host ScreenManager. Injected by the Codename One
 * build (typed against androidx.car.app, which is added as a dependency only when the app references
 * com.codename1.car), so the runtime port carries no compile-time dependency on the car library.
 *
 * <p>Each portable CarScreen maps to a {@link CN1CarScreen} proxy. The root proxy is returned from
 * {@link CN1CarSession} before the com.codename1.car session has produced its root screen; once the
 * session calls {@link #pushScreen}, the proxy adopts that screen and invalidates. Selections are
 * dispatched to the supplied {@link CarActionListener} on the Codename One EDT.</p>
 */
final class CN1AndroidAutoBridge implements CarBridge {
    private final CarContext carContext;
    private final Map<com.codename1.car.CarScreen, CN1CarScreen> proxies =
            new HashMap<com.codename1.car.CarScreen, CN1CarScreen>();
    private CN1CarScreen rootProxy;
    private volatile boolean connected = true;

    private CN1AndroidAutoBridge(CarContext carContext) {
        this.carContext = carContext;
    }

    /** Entry point from CN1CarSession: builds the bridge, starts the portable session, returns root. */
    static Screen start(CarContext carContext) {
        CN1AndroidAutoBridge b = new CN1AndroidAutoBridge(carContext);
        AndroidCarSupport.setBridge(b);
        b.rootProxy = new CN1CarScreen(carContext, b, null, true);
        com.codename1.car.Car.startSession(b);
        return b.rootProxy;
    }

    private ScreenManager screenManager() {
        return carContext.getCarService(ScreenManager.class);
    }

    // --- CarBridge -----------------------------------------------------------

    @Override
    public void pushScreen(final com.codename1.car.CarScreen screen) {
        if (rootProxy != null && rootProxy.cn1Screen == null) {
            rootProxy.cn1Screen = screen;
            proxies.put(screen, rootProxy);
            carContext.getMainExecutor().execute(() -> rootProxy.invalidate());
        } else {
            final CN1CarScreen proxy = new CN1CarScreen(carContext, this, screen, false);
            proxies.put(screen, proxy);
            carContext.getMainExecutor().execute(() -> screenManager().push(proxy));
        }
    }

    @Override
    public void popScreen() {
        carContext.getMainExecutor().execute(() -> {
            if (screenManager().getStackSize() > 1) {
                screenManager().pop();
            }
        });
    }

    @Override
    public void invalidate(final com.codename1.car.CarScreen screen) {
        final CN1CarScreen proxy = proxies.get(screen);
        if (proxy != null) {
            carContext.getMainExecutor().execute(() -> proxy.invalidate());
        }
    }

    @Override
    public void finish() {
        connected = false;
        carContext.getMainExecutor().execute(() -> carContext.finishCarApp());
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void showToast(final String message, final int durationSeconds) {
        carContext.getMainExecutor().execute(() -> CarToast.makeText(carContext, message == null ? "" : message,
                durationSeconds > 1 ? CarToast.LENGTH_LONG : CarToast.LENGTH_SHORT).show());
    }

    @Override
    public int getListRowLimit() {
        try {
            return carContext.getCarService(ConstraintManager.class)
                    .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST);
        } catch (Throwable t) {
            return 0;
        }
    }

    @Override
    public int getGridItemLimit() {
        try {
            return carContext.getCarService(ConstraintManager.class)
                    .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID);
        } catch (Throwable t) {
            return 0;
        }
    }

    // --- CarTemplate -> androidx Template ------------------------------------

    Template buildTemplate(com.codename1.car.CarScreen cn1Screen, boolean root) {
        Action headerAction = root ? Action.APP_ICON : Action.BACK;
        if (cn1Screen == null) {
            return new MessageTemplate.Builder("…")
                    .setHeaderAction(Action.APP_ICON)
                    .build();
        }
        CarTemplate t = cn1Screen.dispatchCreateTemplate();
        if (t instanceof CarListTemplate) {
            return buildList((CarListTemplate) t, headerAction);
        }
        if (t instanceof CarGridTemplate) {
            return buildGrid((CarGridTemplate) t, headerAction);
        }
        if (t instanceof CarPaneTemplate) {
            return buildPane((CarPaneTemplate) t, headerAction);
        }
        if (t instanceof CarMessageTemplate) {
            return buildMessage((CarMessageTemplate) t, headerAction);
        }
        // NowPlaying (media-session driven) and Navigation (needs the navigation surface) are not
        // expressible as a plain template here; render an explanatory message so the screen is valid.
        return new MessageTemplate.Builder("This screen type is not available on Android Auto.")
                .setHeaderAction(headerAction)
                .build();
    }

    private Template buildList(CarListTemplate t, Action headerAction) {
        ListTemplate.Builder b = new ListTemplate.Builder();
        if (t.isLoading()) {
            b.setLoading(true);
        } else {
            List<CarSection> sections = t.getSections();
            boolean single = sections.size() == 1 && sections.get(0).getHeader() == null;
            if (single) {
                b.setSingleList(buildItemList(sections.get(0).getRows()));
            } else {
                for (CarSection s : sections) {
                    String header = s.getHeader() == null ? " " : s.getHeader();
                    b.addSectionedList(SectionedItemList.create(buildItemList(s.getRows()), header));
                }
            }
        }
        b.setHeaderAction(headerAction);
        if (t.getTitle() != null && t.getTitle().length() > 0) {
            b.setTitle(t.getTitle());
        }
        if (t.getHeaderActions() != null) {
            b.setActionStrip(buildActionStrip(t.getHeaderActions()));
        }
        return b.build();
    }

    private ItemList buildItemList(List<CarRow> rows) {
        ItemList.Builder il = new ItemList.Builder();
        for (CarRow r : rows) {
            il.addItem(buildRow(r));
        }
        return il.build();
    }

    private Row buildRow(CarRow r) {
        Row.Builder rb = new Row.Builder();
        rb.setTitle(safe(r.getTitle()));
        if (r.getText() != null) {
            rb.addText(r.getText());
        }
        CarIcon ic = icon(r.getImage());
        if (ic != null) {
            rb.setImage(ic);
        }
        rb.setBrowsable(r.isBrowsable());
        final CarActionListener l = r.getOnAction();
        if (r.isBrowsable() || l != null) {
            rb.setOnClickListener(() -> dispatch(l));
        }
        return rb.build();
    }

    private Template buildGrid(CarGridTemplate t, Action headerAction) {
        GridTemplate.Builder b = new GridTemplate.Builder();
        if (t.isLoading()) {
            b.setLoading(true);
        } else {
            ItemList.Builder il = new ItemList.Builder();
            for (CarGridItem it : t.getItems()) {
                GridItem.Builder gb = new GridItem.Builder();
                gb.setTitle(safe(it.getTitle()));
                if (it.getText() != null) {
                    gb.setText(it.getText());
                }
                CarIcon ic = icon(it.getImage());
                if (ic != null) {
                    gb.setImage(ic, GridItem.IMAGE_TYPE_LARGE);
                }
                final CarActionListener l = it.getOnAction();
                gb.setOnClickListener(() -> dispatch(l));
                il.addItem(gb.build());
            }
            b.setSingleList(il.build());
        }
        b.setHeaderAction(headerAction);
        if (t.getTitle() != null && t.getTitle().length() > 0) {
            b.setTitle(t.getTitle());
        }
        if (t.getHeaderActions() != null) {
            b.setActionStrip(buildActionStrip(t.getHeaderActions()));
        }
        return b.build();
    }

    private Template buildPane(CarPaneTemplate t, Action headerAction) {
        Pane.Builder pane = new Pane.Builder();
        if (t.isLoading()) {
            pane.setLoading(true);
        } else {
            for (CarRow r : t.getRows()) {
                pane.addRow(buildRow(r));
            }
            for (CarAction a : t.getActions()) {
                pane.addAction(buildAction(a));
            }
        }
        PaneTemplate.Builder b = new PaneTemplate.Builder(pane.build());
        b.setHeaderAction(headerAction);
        if (t.getTitle() != null && t.getTitle().length() > 0) {
            b.setTitle(t.getTitle());
        }
        if (t.getHeaderActions() != null) {
            b.setActionStrip(buildActionStrip(t.getHeaderActions()));
        }
        return b.build();
    }

    private Template buildMessage(CarMessageTemplate t, Action headerAction) {
        MessageTemplate.Builder b = new MessageTemplate.Builder(safe(t.getMessage()));
        b.setHeaderAction(headerAction);
        if (t.getTitle() != null && t.getTitle().length() > 0) {
            b.setTitle(t.getTitle());
        }
        CarIcon ic = icon(t.getIcon());
        if (ic != null) {
            b.setIcon(ic);
        }
        for (CarAction a : t.getActions()) {
            b.addAction(buildAction(a));
        }
        if (t.getHeaderActions() != null) {
            b.setActionStrip(buildActionStrip(t.getHeaderActions()));
        }
        return b.build();
    }

    private ActionStrip buildActionStrip(CarActionStrip strip) {
        ActionStrip.Builder b = new ActionStrip.Builder();
        for (CarAction a : strip.getActions()) {
            b.addAction(buildAction(a));
        }
        return b.build();
    }

    private Action buildAction(CarAction a) {
        Action.Builder b = new Action.Builder();
        if (a.getTitle() != null) {
            b.setTitle(a.getTitle());
        }
        CarIcon ic = icon(a.getIcon());
        if (ic != null) {
            b.setIcon(ic);
        }
        b.setBackgroundColor(color(a.getBackgroundColor()));
        final CarActionListener l = a.getOnAction();
        b.setOnClickListener(() -> dispatch(l));
        return b.build();
    }

    // --- helpers -------------------------------------------------------------

    private void dispatch(final CarActionListener l) {
        if (l == null) {
            return;
        }
        Display.getInstance().callSerially(() -> {
            com.codename1.car.CarContext ctx = com.codename1.car.Car.getCurrentContext();
            if (ctx != null) {
                l.actionPerformed(ctx);
            }
        });
    }

    private static String safe(String s) {
        return (s == null || s.length() == 0) ? " " : s;
    }

    private static CarIcon icon(Image img) {
        if (img == null) {
            return null;
        }
        Object n = img.getImage();
        if (n instanceof Bitmap) {
            return new CarIcon.Builder(IconCompat.createWithBitmap((Bitmap) n)).build();
        }
        return null;
    }

    private static CarColor color(com.codename1.car.CarColor c) {
        if (c == null) {
            return CarColor.DEFAULT;
        }
        switch (c) {
            case PRIMARY:
                return CarColor.PRIMARY;
            case RED:
                return CarColor.RED;
            case GREEN:
                return CarColor.GREEN;
            case BLUE:
                return CarColor.BLUE;
            case YELLOW:
                return CarColor.YELLOW;
            default:
                return CarColor.DEFAULT;
        }
    }
}
