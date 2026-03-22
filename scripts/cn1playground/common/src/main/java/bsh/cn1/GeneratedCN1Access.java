package bsh.cn1;

import com.codename1.ui.Form;
import com.codenameone.playground.PlaygroundContext;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ads;
import bsh.cn1.gen.GeneratedAccess_com_codename1_analytics;
import bsh.cn1.gen.GeneratedAccess_com_codename1_annotations;
import bsh.cn1.gen.GeneratedAccess_com_codename1_background;
import bsh.cn1.gen.GeneratedAccess_com_codename1_capture;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts_compat;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts_models;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts_renderers;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts_transitions;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts_util;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts_views;
import bsh.cn1.gen.GeneratedAccess_com_codename1_cloud;
import bsh.cn1.gen.GeneratedAccess_com_codename1_codescan;
import bsh.cn1.gen.GeneratedAccess_com_codename1_compat_java_util;
import bsh.cn1.gen.GeneratedAccess_com_codename1_components;
import bsh.cn1.gen.GeneratedAccess_com_codename1_contacts;
import bsh.cn1.gen.GeneratedAccess_com_codename1_db;
import bsh.cn1.gen.GeneratedAccess_com_codename1_facebook;
import bsh.cn1.gen.GeneratedAccess_com_codename1_facebook_ui;
import bsh.cn1.gen.GeneratedAccess_com_codename1_impl;
import bsh.cn1.gen.GeneratedAccess_com_codename1_io;
import bsh.cn1.gen.GeneratedAccess_com_codename1_io_gzip;
import bsh.cn1.gen.GeneratedAccess_com_codename1_io_rest;
import bsh.cn1.gen.GeneratedAccess_com_codename1_io_services;
import bsh.cn1.gen.GeneratedAccess_com_codename1_io_tar;
import bsh.cn1.gen.GeneratedAccess_com_codename1_javascript;
import bsh.cn1.gen.GeneratedAccess_com_codename1_l10n;
import bsh.cn1.gen.GeneratedAccess_com_codename1_location;
import bsh.cn1.gen.GeneratedAccess_com_codename1_maps;
import bsh.cn1.gen.GeneratedAccess_com_codename1_maps_layers;
import bsh.cn1.gen.GeneratedAccess_com_codename1_maps_providers;
import bsh.cn1.gen.GeneratedAccess_com_codename1_media;
import bsh.cn1.gen.GeneratedAccess_com_codename1_messaging;
import bsh.cn1.gen.GeneratedAccess_com_codename1_notifications;
import bsh.cn1.gen.GeneratedAccess_com_codename1_payment;
import bsh.cn1.gen.GeneratedAccess_com_codename1_plugin;
import bsh.cn1.gen.GeneratedAccess_com_codename1_plugin_event;
import bsh.cn1.gen.GeneratedAccess_com_codename1_processing;
import bsh.cn1.gen.GeneratedAccess_com_codename1_properties;
import bsh.cn1.gen.GeneratedAccess_com_codename1_push;
import bsh.cn1.gen.GeneratedAccess_com_codename1_share;
import bsh.cn1.gen.GeneratedAccess_com_codename1_social;
import bsh.cn1.gen.GeneratedAccess_com_codename1_system;
import bsh.cn1.gen.GeneratedAccess_com_codename1_testing;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_animations;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_css;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_events;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_geom;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_html;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_layouts;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_layouts_mig;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_list;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_painter;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_plaf;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_scene;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_spinner;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_table;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_tree;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_util;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_validation;
import bsh.cn1.gen.GeneratedAccess_com_codename1_util;
import bsh.cn1.gen.GeneratedAccess_com_codename1_util_promise;
import bsh.cn1.gen.GeneratedAccess_com_codename1_util_regex;
import bsh.cn1.gen.GeneratedAccess_com_codename1_xml;
import bsh.cn1.gen.GeneratedAccess_com_codenameone_playground;
import bsh.cn1.gen.GeneratedAccess_java_io;
import bsh.cn1.gen.GeneratedAccess_java_lang;
import bsh.cn1.gen.GeneratedAccess_java_lang_ref;
import bsh.cn1.gen.GeneratedAccess_java_lang_reflect;
import bsh.cn1.gen.GeneratedAccess_java_net;
import bsh.cn1.gen.GeneratedAccess_java_nio_charset;
import bsh.cn1.gen.GeneratedAccess_java_text;
import bsh.cn1.gen.GeneratedAccess_java_util;
import bsh.cn1.gen.GeneratedAccess_java_util_concurrent;

/**
 * Generated registry. Re-run tools/generate-cn1-access-registry.sh after updating the CN1 sources.
 */
public final class GeneratedCN1Access implements CN1Access {
    public static final GeneratedCN1Access INSTANCE = new GeneratedCN1Access();

    private GeneratedCN1Access() {
    }

    @Override
    public Class<?> findClass(String name) {
        if (shouldDebugFindClass(name)) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass(" + name + ")");
        }
        Class<?> found;
        found = GeneratedAccess_com_codename1_ads.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ads -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_analytics.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.analytics -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_annotations.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.annotations -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_background.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.background -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_capture.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.capture -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_charts.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.charts -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_charts_compat.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.charts.compat -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_charts_models.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.charts.models -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_charts_renderers.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.charts.renderers -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_charts_transitions.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.charts.transitions -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_charts_util.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.charts.util -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_charts_views.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.charts.views -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_cloud.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.cloud -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_codescan.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.codescan -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_compat_java_util.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.compat.java.util -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_components.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.components -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_contacts.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.contacts -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_db.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.db -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_facebook.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.facebook -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_facebook_ui.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.facebook.ui -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_impl.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.impl -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_io.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.io -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_io_gzip.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.io.gzip -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_io_rest.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.io.rest -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_io_services.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.io.services -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_io_tar.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.io.tar -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_javascript.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.javascript -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_l10n.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.l10n -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_location.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.location -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_maps.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.maps -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_maps_layers.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.maps.layers -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_maps_providers.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.maps.providers -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_media.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.media -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_messaging.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.messaging -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_notifications.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.notifications -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_payment.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.payment -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_plugin.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.plugin -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_plugin_event.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.plugin.event -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_processing.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.processing -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_properties.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.properties -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_push.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.push -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_share.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.share -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_social.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.social -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_system.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.system -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_testing.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.testing -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_animations.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.animations -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_css.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.css -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_events.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.events -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_geom.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.geom -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_html.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.html -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_layouts.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.layouts -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_layouts_mig.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.layouts.mig -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_list.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.list -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_painter.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.painter -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_plaf.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.plaf -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_scene.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.scene -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_spinner.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.spinner -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_table.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.table -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_tree.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.tree -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_util.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.util -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_ui_validation.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.ui.validation -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_util.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.util -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_util_promise.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.util.promise -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_util_regex.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.util.regex -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codename1_xml.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codename1.xml -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_com_codenameone_playground.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit com.codenameone.playground -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_java_io.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit java.io -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_java_lang.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit java.lang -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_java_lang_ref.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit java.lang.ref -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_java_lang_reflect.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit java.lang.reflect -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_java_net.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit java.net -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_java_nio_charset.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit java.nio.charset -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_java_text.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit java.text -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_java_util.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit java.util -> " + found);
        }
        if (found != null) {
            return found;
        }
        found = GeneratedAccess_java_util_concurrent.findClass(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit java.util.concurrent -> " + found);
        }
        if (found != null) {
            return found;
        }
        if (shouldDebugFindClass(name)) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass miss " + name);
        }
        return null;
    }

    @Override
    public Object construct(Class<?> type, Object[] args) throws Exception {
        String packageName = packageName(type);
        if ("com.codename1.ads".equals(packageName)) {
            return GeneratedAccess_com_codename1_ads.construct(type, args);
        }
        if ("com.codename1.analytics".equals(packageName)) {
            return GeneratedAccess_com_codename1_analytics.construct(type, args);
        }
        if ("com.codename1.annotations".equals(packageName)) {
            return GeneratedAccess_com_codename1_annotations.construct(type, args);
        }
        if ("com.codename1.background".equals(packageName)) {
            return GeneratedAccess_com_codename1_background.construct(type, args);
        }
        if ("com.codename1.capture".equals(packageName)) {
            return GeneratedAccess_com_codename1_capture.construct(type, args);
        }
        if ("com.codename1.charts".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts.construct(type, args);
        }
        if ("com.codename1.charts.compat".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_compat.construct(type, args);
        }
        if ("com.codename1.charts.models".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_models.construct(type, args);
        }
        if ("com.codename1.charts.renderers".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_renderers.construct(type, args);
        }
        if ("com.codename1.charts.transitions".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_transitions.construct(type, args);
        }
        if ("com.codename1.charts.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_util.construct(type, args);
        }
        if ("com.codename1.charts.views".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_views.construct(type, args);
        }
        if ("com.codename1.cloud".equals(packageName)) {
            return GeneratedAccess_com_codename1_cloud.construct(type, args);
        }
        if ("com.codename1.codescan".equals(packageName)) {
            return GeneratedAccess_com_codename1_codescan.construct(type, args);
        }
        if ("com.codename1.compat.java.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_compat_java_util.construct(type, args);
        }
        if ("com.codename1.components".equals(packageName)) {
            return GeneratedAccess_com_codename1_components.construct(type, args);
        }
        if ("com.codename1.contacts".equals(packageName)) {
            return GeneratedAccess_com_codename1_contacts.construct(type, args);
        }
        if ("com.codename1.db".equals(packageName)) {
            return GeneratedAccess_com_codename1_db.construct(type, args);
        }
        if ("com.codename1.facebook".equals(packageName)) {
            return GeneratedAccess_com_codename1_facebook.construct(type, args);
        }
        if ("com.codename1.facebook.ui".equals(packageName)) {
            return GeneratedAccess_com_codename1_facebook_ui.construct(type, args);
        }
        if ("com.codename1.impl".equals(packageName)) {
            return GeneratedAccess_com_codename1_impl.construct(type, args);
        }
        if ("com.codename1.io".equals(packageName)) {
            return GeneratedAccess_com_codename1_io.construct(type, args);
        }
        if ("com.codename1.io.gzip".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_gzip.construct(type, args);
        }
        if ("com.codename1.io.rest".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_rest.construct(type, args);
        }
        if ("com.codename1.io.services".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_services.construct(type, args);
        }
        if ("com.codename1.io.tar".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_tar.construct(type, args);
        }
        if ("com.codename1.javascript".equals(packageName)) {
            return GeneratedAccess_com_codename1_javascript.construct(type, args);
        }
        if ("com.codename1.l10n".equals(packageName)) {
            return GeneratedAccess_com_codename1_l10n.construct(type, args);
        }
        if ("com.codename1.location".equals(packageName)) {
            return GeneratedAccess_com_codename1_location.construct(type, args);
        }
        if ("com.codename1.maps".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps.construct(type, args);
        }
        if ("com.codename1.maps.layers".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps_layers.construct(type, args);
        }
        if ("com.codename1.maps.providers".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps_providers.construct(type, args);
        }
        if ("com.codename1.media".equals(packageName)) {
            return GeneratedAccess_com_codename1_media.construct(type, args);
        }
        if ("com.codename1.messaging".equals(packageName)) {
            return GeneratedAccess_com_codename1_messaging.construct(type, args);
        }
        if ("com.codename1.notifications".equals(packageName)) {
            return GeneratedAccess_com_codename1_notifications.construct(type, args);
        }
        if ("com.codename1.payment".equals(packageName)) {
            return GeneratedAccess_com_codename1_payment.construct(type, args);
        }
        if ("com.codename1.plugin".equals(packageName)) {
            return GeneratedAccess_com_codename1_plugin.construct(type, args);
        }
        if ("com.codename1.plugin.event".equals(packageName)) {
            return GeneratedAccess_com_codename1_plugin_event.construct(type, args);
        }
        if ("com.codename1.processing".equals(packageName)) {
            return GeneratedAccess_com_codename1_processing.construct(type, args);
        }
        if ("com.codename1.properties".equals(packageName)) {
            return GeneratedAccess_com_codename1_properties.construct(type, args);
        }
        if ("com.codename1.push".equals(packageName)) {
            return GeneratedAccess_com_codename1_push.construct(type, args);
        }
        if ("com.codename1.share".equals(packageName)) {
            return GeneratedAccess_com_codename1_share.construct(type, args);
        }
        if ("com.codename1.social".equals(packageName)) {
            return GeneratedAccess_com_codename1_social.construct(type, args);
        }
        if ("com.codename1.system".equals(packageName)) {
            return GeneratedAccess_com_codename1_system.construct(type, args);
        }
        if ("com.codename1.testing".equals(packageName)) {
            return GeneratedAccess_com_codename1_testing.construct(type, args);
        }
        if ("com.codename1.ui".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui.construct(type, args);
        }
        if ("com.codename1.ui.animations".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_animations.construct(type, args);
        }
        if ("com.codename1.ui.css".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_css.construct(type, args);
        }
        if ("com.codename1.ui.events".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_events.construct(type, args);
        }
        if ("com.codename1.ui.geom".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_geom.construct(type, args);
        }
        if ("com.codename1.ui.html".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_html.construct(type, args);
        }
        if ("com.codename1.ui.layouts".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_layouts.construct(type, args);
        }
        if ("com.codename1.ui.layouts.mig".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_layouts_mig.construct(type, args);
        }
        if ("com.codename1.ui.list".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_list.construct(type, args);
        }
        if ("com.codename1.ui.painter".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_painter.construct(type, args);
        }
        if ("com.codename1.ui.plaf".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_plaf.construct(type, args);
        }
        if ("com.codename1.ui.scene".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_scene.construct(type, args);
        }
        if ("com.codename1.ui.spinner".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_spinner.construct(type, args);
        }
        if ("com.codename1.ui.table".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_table.construct(type, args);
        }
        if ("com.codename1.ui.tree".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_tree.construct(type, args);
        }
        if ("com.codename1.ui.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_util.construct(type, args);
        }
        if ("com.codename1.ui.validation".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_validation.construct(type, args);
        }
        if ("com.codename1.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_util.construct(type, args);
        }
        if ("com.codename1.util.promise".equals(packageName)) {
            return GeneratedAccess_com_codename1_util_promise.construct(type, args);
        }
        if ("com.codename1.util.regex".equals(packageName)) {
            return GeneratedAccess_com_codename1_util_regex.construct(type, args);
        }
        if ("com.codename1.xml".equals(packageName)) {
            return GeneratedAccess_com_codename1_xml.construct(type, args);
        }
        if ("com.codenameone.playground".equals(packageName)) {
            return GeneratedAccess_com_codenameone_playground.construct(type, args);
        }
        if ("java.io".equals(packageName)) {
            return GeneratedAccess_java_io.construct(type, args);
        }
        if ("java.lang".equals(packageName)) {
            return GeneratedAccess_java_lang.construct(type, args);
        }
        if ("java.lang.ref".equals(packageName)) {
            return GeneratedAccess_java_lang_ref.construct(type, args);
        }
        if ("java.lang.reflect".equals(packageName)) {
            return GeneratedAccess_java_lang_reflect.construct(type, args);
        }
        if ("java.net".equals(packageName)) {
            return GeneratedAccess_java_net.construct(type, args);
        }
        if ("java.nio.charset".equals(packageName)) {
            return GeneratedAccess_java_nio_charset.construct(type, args);
        }
        if ("java.text".equals(packageName)) {
            return GeneratedAccess_java_text.construct(type, args);
        }
        if ("java.util".equals(packageName)) {
            return GeneratedAccess_java_util.construct(type, args);
        }
        if ("java.util.concurrent".equals(packageName)) {
            return GeneratedAccess_java_util_concurrent.construct(type, args);
        }
        throw unsupportedConstruct(type, args);
    }

    @Override
    public Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        String packageName = packageName(type);
        if ("com.codename1.ads".equals(packageName)) {
            return GeneratedAccess_com_codename1_ads.invokeStatic(type, name, args);
        }
        if ("com.codename1.analytics".equals(packageName)) {
            return GeneratedAccess_com_codename1_analytics.invokeStatic(type, name, args);
        }
        if ("com.codename1.annotations".equals(packageName)) {
            return GeneratedAccess_com_codename1_annotations.invokeStatic(type, name, args);
        }
        if ("com.codename1.background".equals(packageName)) {
            return GeneratedAccess_com_codename1_background.invokeStatic(type, name, args);
        }
        if ("com.codename1.capture".equals(packageName)) {
            return GeneratedAccess_com_codename1_capture.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts.compat".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_compat.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts.models".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_models.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts.renderers".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_renderers.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts.transitions".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_transitions.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_util.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts.views".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_views.invokeStatic(type, name, args);
        }
        if ("com.codename1.cloud".equals(packageName)) {
            return GeneratedAccess_com_codename1_cloud.invokeStatic(type, name, args);
        }
        if ("com.codename1.codescan".equals(packageName)) {
            return GeneratedAccess_com_codename1_codescan.invokeStatic(type, name, args);
        }
        if ("com.codename1.compat.java.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_compat_java_util.invokeStatic(type, name, args);
        }
        if ("com.codename1.components".equals(packageName)) {
            return GeneratedAccess_com_codename1_components.invokeStatic(type, name, args);
        }
        if ("com.codename1.contacts".equals(packageName)) {
            return GeneratedAccess_com_codename1_contacts.invokeStatic(type, name, args);
        }
        if ("com.codename1.db".equals(packageName)) {
            return GeneratedAccess_com_codename1_db.invokeStatic(type, name, args);
        }
        if ("com.codename1.facebook".equals(packageName)) {
            return GeneratedAccess_com_codename1_facebook.invokeStatic(type, name, args);
        }
        if ("com.codename1.facebook.ui".equals(packageName)) {
            return GeneratedAccess_com_codename1_facebook_ui.invokeStatic(type, name, args);
        }
        if ("com.codename1.impl".equals(packageName)) {
            return GeneratedAccess_com_codename1_impl.invokeStatic(type, name, args);
        }
        if ("com.codename1.io".equals(packageName)) {
            return GeneratedAccess_com_codename1_io.invokeStatic(type, name, args);
        }
        if ("com.codename1.io.gzip".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_gzip.invokeStatic(type, name, args);
        }
        if ("com.codename1.io.rest".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_rest.invokeStatic(type, name, args);
        }
        if ("com.codename1.io.services".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_services.invokeStatic(type, name, args);
        }
        if ("com.codename1.io.tar".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_tar.invokeStatic(type, name, args);
        }
        if ("com.codename1.javascript".equals(packageName)) {
            return GeneratedAccess_com_codename1_javascript.invokeStatic(type, name, args);
        }
        if ("com.codename1.l10n".equals(packageName)) {
            return GeneratedAccess_com_codename1_l10n.invokeStatic(type, name, args);
        }
        if ("com.codename1.location".equals(packageName)) {
            return GeneratedAccess_com_codename1_location.invokeStatic(type, name, args);
        }
        if ("com.codename1.maps".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps.invokeStatic(type, name, args);
        }
        if ("com.codename1.maps.layers".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps_layers.invokeStatic(type, name, args);
        }
        if ("com.codename1.maps.providers".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps_providers.invokeStatic(type, name, args);
        }
        if ("com.codename1.media".equals(packageName)) {
            return GeneratedAccess_com_codename1_media.invokeStatic(type, name, args);
        }
        if ("com.codename1.messaging".equals(packageName)) {
            return GeneratedAccess_com_codename1_messaging.invokeStatic(type, name, args);
        }
        if ("com.codename1.notifications".equals(packageName)) {
            return GeneratedAccess_com_codename1_notifications.invokeStatic(type, name, args);
        }
        if ("com.codename1.payment".equals(packageName)) {
            return GeneratedAccess_com_codename1_payment.invokeStatic(type, name, args);
        }
        if ("com.codename1.plugin".equals(packageName)) {
            return GeneratedAccess_com_codename1_plugin.invokeStatic(type, name, args);
        }
        if ("com.codename1.plugin.event".equals(packageName)) {
            return GeneratedAccess_com_codename1_plugin_event.invokeStatic(type, name, args);
        }
        if ("com.codename1.processing".equals(packageName)) {
            return GeneratedAccess_com_codename1_processing.invokeStatic(type, name, args);
        }
        if ("com.codename1.properties".equals(packageName)) {
            return GeneratedAccess_com_codename1_properties.invokeStatic(type, name, args);
        }
        if ("com.codename1.push".equals(packageName)) {
            return GeneratedAccess_com_codename1_push.invokeStatic(type, name, args);
        }
        if ("com.codename1.share".equals(packageName)) {
            return GeneratedAccess_com_codename1_share.invokeStatic(type, name, args);
        }
        if ("com.codename1.social".equals(packageName)) {
            return GeneratedAccess_com_codename1_social.invokeStatic(type, name, args);
        }
        if ("com.codename1.system".equals(packageName)) {
            return GeneratedAccess_com_codename1_system.invokeStatic(type, name, args);
        }
        if ("com.codename1.testing".equals(packageName)) {
            return GeneratedAccess_com_codename1_testing.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.animations".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_animations.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.css".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_css.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.events".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_events.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.geom".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_geom.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.html".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_html.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.layouts".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_layouts.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.layouts.mig".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_layouts_mig.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.list".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_list.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.painter".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_painter.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.plaf".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_plaf.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.scene".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_scene.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.spinner".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_spinner.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.table".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_table.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.tree".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_tree.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_util.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.validation".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_validation.invokeStatic(type, name, args);
        }
        if ("com.codename1.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_util.invokeStatic(type, name, args);
        }
        if ("com.codename1.util.promise".equals(packageName)) {
            return GeneratedAccess_com_codename1_util_promise.invokeStatic(type, name, args);
        }
        if ("com.codename1.util.regex".equals(packageName)) {
            return GeneratedAccess_com_codename1_util_regex.invokeStatic(type, name, args);
        }
        if ("com.codename1.xml".equals(packageName)) {
            return GeneratedAccess_com_codename1_xml.invokeStatic(type, name, args);
        }
        if ("com.codenameone.playground".equals(packageName)) {
            return GeneratedAccess_com_codenameone_playground.invokeStatic(type, name, args);
        }
        if ("java.io".equals(packageName)) {
            return GeneratedAccess_java_io.invokeStatic(type, name, args);
        }
        if ("java.lang".equals(packageName)) {
            return GeneratedAccess_java_lang.invokeStatic(type, name, args);
        }
        if ("java.lang.ref".equals(packageName)) {
            return GeneratedAccess_java_lang_ref.invokeStatic(type, name, args);
        }
        if ("java.lang.reflect".equals(packageName)) {
            return GeneratedAccess_java_lang_reflect.invokeStatic(type, name, args);
        }
        if ("java.net".equals(packageName)) {
            return GeneratedAccess_java_net.invokeStatic(type, name, args);
        }
        if ("java.nio.charset".equals(packageName)) {
            return GeneratedAccess_java_nio_charset.invokeStatic(type, name, args);
        }
        if ("java.text".equals(packageName)) {
            return GeneratedAccess_java_text.invokeStatic(type, name, args);
        }
        if ("java.util".equals(packageName)) {
            return GeneratedAccess_java_util.invokeStatic(type, name, args);
        }
        if ("java.util.concurrent".equals(packageName)) {
            return GeneratedAccess_java_util_concurrent.invokeStatic(type, name, args);
        }
        throw unsupportedStatic(type, name, args);
    }

    @Override
    public Object invoke(Object target, String name, Object[] args) throws Exception {
        if (interceptShownForm(target, name, args)) {
            return null;
        }
        CN1AccessException unsupported = null;
        try {
            return GeneratedAccess_com_codename1_ads.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_analytics.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_annotations.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_background.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_capture.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_compat.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_models.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_renderers.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_transitions.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_util.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_views.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_cloud.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_codescan.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_compat_java_util.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_components.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_contacts.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_db.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_facebook.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_facebook_ui.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_impl.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_gzip.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_rest.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_services.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_tar.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_javascript.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_l10n.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_location.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_maps.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_maps_layers.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_maps_providers.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_media.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_messaging.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_notifications.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_payment.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_plugin.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_plugin_event.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_processing.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_properties.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_push.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_share.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_social.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_system.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_testing.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_animations.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_css.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_events.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_geom.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_html.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_layouts.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_layouts_mig.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_list.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_painter.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_plaf.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_scene.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_spinner.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_table.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_tree.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_util.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_validation.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_util.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_util_promise.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_util_regex.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_xml.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codenameone_playground.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_io.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_lang.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_lang_ref.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_lang_reflect.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_net.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_nio_charset.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_text.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_util.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_util_concurrent.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, args);
    }

    private static boolean interceptShownForm(Object target, String name, Object[] args) {
        PlaygroundContext context = PlaygroundContext.getCurrent();
        if (context == null || !(target instanceof Form) || !"show".equals(name)) {
            return false;
        }
        if (args != null && args.length != 0) {
            return false;
        }
        context.captureShownForm((Form) target);
        return true;
    }

    @Override
    public Object getStaticField(Class<?> type, String name) throws Exception {
        String packageName = packageName(type);
        if ("com.codename1.ads".equals(packageName)) {
            return GeneratedAccess_com_codename1_ads.getStaticField(type, name);
        }
        if ("com.codename1.analytics".equals(packageName)) {
            return GeneratedAccess_com_codename1_analytics.getStaticField(type, name);
        }
        if ("com.codename1.annotations".equals(packageName)) {
            return GeneratedAccess_com_codename1_annotations.getStaticField(type, name);
        }
        if ("com.codename1.background".equals(packageName)) {
            return GeneratedAccess_com_codename1_background.getStaticField(type, name);
        }
        if ("com.codename1.capture".equals(packageName)) {
            return GeneratedAccess_com_codename1_capture.getStaticField(type, name);
        }
        if ("com.codename1.charts".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts.getStaticField(type, name);
        }
        if ("com.codename1.charts.compat".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_compat.getStaticField(type, name);
        }
        if ("com.codename1.charts.models".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_models.getStaticField(type, name);
        }
        if ("com.codename1.charts.renderers".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_renderers.getStaticField(type, name);
        }
        if ("com.codename1.charts.transitions".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_transitions.getStaticField(type, name);
        }
        if ("com.codename1.charts.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_util.getStaticField(type, name);
        }
        if ("com.codename1.charts.views".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_views.getStaticField(type, name);
        }
        if ("com.codename1.cloud".equals(packageName)) {
            return GeneratedAccess_com_codename1_cloud.getStaticField(type, name);
        }
        if ("com.codename1.codescan".equals(packageName)) {
            return GeneratedAccess_com_codename1_codescan.getStaticField(type, name);
        }
        if ("com.codename1.compat.java.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_compat_java_util.getStaticField(type, name);
        }
        if ("com.codename1.components".equals(packageName)) {
            return GeneratedAccess_com_codename1_components.getStaticField(type, name);
        }
        if ("com.codename1.contacts".equals(packageName)) {
            return GeneratedAccess_com_codename1_contacts.getStaticField(type, name);
        }
        if ("com.codename1.db".equals(packageName)) {
            return GeneratedAccess_com_codename1_db.getStaticField(type, name);
        }
        if ("com.codename1.facebook".equals(packageName)) {
            return GeneratedAccess_com_codename1_facebook.getStaticField(type, name);
        }
        if ("com.codename1.facebook.ui".equals(packageName)) {
            return GeneratedAccess_com_codename1_facebook_ui.getStaticField(type, name);
        }
        if ("com.codename1.impl".equals(packageName)) {
            return GeneratedAccess_com_codename1_impl.getStaticField(type, name);
        }
        if ("com.codename1.io".equals(packageName)) {
            return GeneratedAccess_com_codename1_io.getStaticField(type, name);
        }
        if ("com.codename1.io.gzip".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_gzip.getStaticField(type, name);
        }
        if ("com.codename1.io.rest".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_rest.getStaticField(type, name);
        }
        if ("com.codename1.io.services".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_services.getStaticField(type, name);
        }
        if ("com.codename1.io.tar".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_tar.getStaticField(type, name);
        }
        if ("com.codename1.javascript".equals(packageName)) {
            return GeneratedAccess_com_codename1_javascript.getStaticField(type, name);
        }
        if ("com.codename1.l10n".equals(packageName)) {
            return GeneratedAccess_com_codename1_l10n.getStaticField(type, name);
        }
        if ("com.codename1.location".equals(packageName)) {
            return GeneratedAccess_com_codename1_location.getStaticField(type, name);
        }
        if ("com.codename1.maps".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps.getStaticField(type, name);
        }
        if ("com.codename1.maps.layers".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps_layers.getStaticField(type, name);
        }
        if ("com.codename1.maps.providers".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps_providers.getStaticField(type, name);
        }
        if ("com.codename1.media".equals(packageName)) {
            return GeneratedAccess_com_codename1_media.getStaticField(type, name);
        }
        if ("com.codename1.messaging".equals(packageName)) {
            return GeneratedAccess_com_codename1_messaging.getStaticField(type, name);
        }
        if ("com.codename1.notifications".equals(packageName)) {
            return GeneratedAccess_com_codename1_notifications.getStaticField(type, name);
        }
        if ("com.codename1.payment".equals(packageName)) {
            return GeneratedAccess_com_codename1_payment.getStaticField(type, name);
        }
        if ("com.codename1.plugin".equals(packageName)) {
            return GeneratedAccess_com_codename1_plugin.getStaticField(type, name);
        }
        if ("com.codename1.plugin.event".equals(packageName)) {
            return GeneratedAccess_com_codename1_plugin_event.getStaticField(type, name);
        }
        if ("com.codename1.processing".equals(packageName)) {
            return GeneratedAccess_com_codename1_processing.getStaticField(type, name);
        }
        if ("com.codename1.properties".equals(packageName)) {
            return GeneratedAccess_com_codename1_properties.getStaticField(type, name);
        }
        if ("com.codename1.push".equals(packageName)) {
            return GeneratedAccess_com_codename1_push.getStaticField(type, name);
        }
        if ("com.codename1.share".equals(packageName)) {
            return GeneratedAccess_com_codename1_share.getStaticField(type, name);
        }
        if ("com.codename1.social".equals(packageName)) {
            return GeneratedAccess_com_codename1_social.getStaticField(type, name);
        }
        if ("com.codename1.system".equals(packageName)) {
            return GeneratedAccess_com_codename1_system.getStaticField(type, name);
        }
        if ("com.codename1.testing".equals(packageName)) {
            return GeneratedAccess_com_codename1_testing.getStaticField(type, name);
        }
        if ("com.codename1.ui".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui.getStaticField(type, name);
        }
        if ("com.codename1.ui.animations".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_animations.getStaticField(type, name);
        }
        if ("com.codename1.ui.css".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_css.getStaticField(type, name);
        }
        if ("com.codename1.ui.events".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_events.getStaticField(type, name);
        }
        if ("com.codename1.ui.geom".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_geom.getStaticField(type, name);
        }
        if ("com.codename1.ui.html".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_html.getStaticField(type, name);
        }
        if ("com.codename1.ui.layouts".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_layouts.getStaticField(type, name);
        }
        if ("com.codename1.ui.layouts.mig".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_layouts_mig.getStaticField(type, name);
        }
        if ("com.codename1.ui.list".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_list.getStaticField(type, name);
        }
        if ("com.codename1.ui.painter".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_painter.getStaticField(type, name);
        }
        if ("com.codename1.ui.plaf".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_plaf.getStaticField(type, name);
        }
        if ("com.codename1.ui.scene".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_scene.getStaticField(type, name);
        }
        if ("com.codename1.ui.spinner".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_spinner.getStaticField(type, name);
        }
        if ("com.codename1.ui.table".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_table.getStaticField(type, name);
        }
        if ("com.codename1.ui.tree".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_tree.getStaticField(type, name);
        }
        if ("com.codename1.ui.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_util.getStaticField(type, name);
        }
        if ("com.codename1.ui.validation".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_validation.getStaticField(type, name);
        }
        if ("com.codename1.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_util.getStaticField(type, name);
        }
        if ("com.codename1.util.promise".equals(packageName)) {
            return GeneratedAccess_com_codename1_util_promise.getStaticField(type, name);
        }
        if ("com.codename1.util.regex".equals(packageName)) {
            return GeneratedAccess_com_codename1_util_regex.getStaticField(type, name);
        }
        if ("com.codename1.xml".equals(packageName)) {
            return GeneratedAccess_com_codename1_xml.getStaticField(type, name);
        }
        if ("com.codenameone.playground".equals(packageName)) {
            return GeneratedAccess_com_codenameone_playground.getStaticField(type, name);
        }
        if ("java.io".equals(packageName)) {
            return GeneratedAccess_java_io.getStaticField(type, name);
        }
        if ("java.lang".equals(packageName)) {
            return GeneratedAccess_java_lang.getStaticField(type, name);
        }
        if ("java.lang.ref".equals(packageName)) {
            return GeneratedAccess_java_lang_ref.getStaticField(type, name);
        }
        if ("java.lang.reflect".equals(packageName)) {
            return GeneratedAccess_java_lang_reflect.getStaticField(type, name);
        }
        if ("java.net".equals(packageName)) {
            return GeneratedAccess_java_net.getStaticField(type, name);
        }
        if ("java.nio.charset".equals(packageName)) {
            return GeneratedAccess_java_nio_charset.getStaticField(type, name);
        }
        if ("java.text".equals(packageName)) {
            return GeneratedAccess_java_text.getStaticField(type, name);
        }
        if ("java.util".equals(packageName)) {
            return GeneratedAccess_java_util.getStaticField(type, name);
        }
        if ("java.util.concurrent".equals(packageName)) {
            return GeneratedAccess_java_util_concurrent.getStaticField(type, name);
        }
        throw unsupportedStaticField(type, name);
    }

    @Override
    public Object getField(Object target, String name) throws Exception {
        CN1AccessException unsupported = null;
        try {
            return GeneratedAccess_com_codename1_ads.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_analytics.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_annotations.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_background.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_capture.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_compat.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_models.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_renderers.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_transitions.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_util.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_views.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_cloud.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_codescan.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_compat_java_util.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_components.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_contacts.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_db.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_facebook.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_facebook_ui.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_impl.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_gzip.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_rest.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_services.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_tar.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_javascript.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_l10n.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_location.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_maps.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_maps_layers.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_maps_providers.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_media.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_messaging.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_notifications.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_payment.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_plugin.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_plugin_event.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_processing.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_properties.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_push.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_share.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_social.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_system.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_testing.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_animations.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_css.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_events.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_geom.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_html.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_layouts.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_layouts_mig.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_list.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_painter.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_plaf.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_scene.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_spinner.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_table.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_tree.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_util.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_validation.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_util.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_util_promise.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_util_regex.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_xml.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codenameone_playground.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_io.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_lang.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_lang_ref.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_lang_reflect.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_net.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_nio_charset.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_text.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_util.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_util_concurrent.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedField(target, name);
    }

    @Override
    public void setStaticField(Class<?> type, String name, Object value) throws Exception {
        String packageName = packageName(type);
        if ("com.codename1.ads".equals(packageName)) {
            GeneratedAccess_com_codename1_ads.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.analytics".equals(packageName)) {
            GeneratedAccess_com_codename1_analytics.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.annotations".equals(packageName)) {
            GeneratedAccess_com_codename1_annotations.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.background".equals(packageName)) {
            GeneratedAccess_com_codename1_background.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.capture".equals(packageName)) {
            GeneratedAccess_com_codename1_capture.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts".equals(packageName)) {
            GeneratedAccess_com_codename1_charts.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts.compat".equals(packageName)) {
            GeneratedAccess_com_codename1_charts_compat.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts.models".equals(packageName)) {
            GeneratedAccess_com_codename1_charts_models.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts.renderers".equals(packageName)) {
            GeneratedAccess_com_codename1_charts_renderers.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts.transitions".equals(packageName)) {
            GeneratedAccess_com_codename1_charts_transitions.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts.util".equals(packageName)) {
            GeneratedAccess_com_codename1_charts_util.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts.views".equals(packageName)) {
            GeneratedAccess_com_codename1_charts_views.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.cloud".equals(packageName)) {
            GeneratedAccess_com_codename1_cloud.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.codescan".equals(packageName)) {
            GeneratedAccess_com_codename1_codescan.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.compat.java.util".equals(packageName)) {
            GeneratedAccess_com_codename1_compat_java_util.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.components".equals(packageName)) {
            GeneratedAccess_com_codename1_components.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.contacts".equals(packageName)) {
            GeneratedAccess_com_codename1_contacts.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.db".equals(packageName)) {
            GeneratedAccess_com_codename1_db.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.facebook".equals(packageName)) {
            GeneratedAccess_com_codename1_facebook.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.facebook.ui".equals(packageName)) {
            GeneratedAccess_com_codename1_facebook_ui.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.impl".equals(packageName)) {
            GeneratedAccess_com_codename1_impl.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.io".equals(packageName)) {
            GeneratedAccess_com_codename1_io.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.io.gzip".equals(packageName)) {
            GeneratedAccess_com_codename1_io_gzip.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.io.rest".equals(packageName)) {
            GeneratedAccess_com_codename1_io_rest.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.io.services".equals(packageName)) {
            GeneratedAccess_com_codename1_io_services.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.io.tar".equals(packageName)) {
            GeneratedAccess_com_codename1_io_tar.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.javascript".equals(packageName)) {
            GeneratedAccess_com_codename1_javascript.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.l10n".equals(packageName)) {
            GeneratedAccess_com_codename1_l10n.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.location".equals(packageName)) {
            GeneratedAccess_com_codename1_location.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.maps".equals(packageName)) {
            GeneratedAccess_com_codename1_maps.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.maps.layers".equals(packageName)) {
            GeneratedAccess_com_codename1_maps_layers.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.maps.providers".equals(packageName)) {
            GeneratedAccess_com_codename1_maps_providers.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.media".equals(packageName)) {
            GeneratedAccess_com_codename1_media.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.messaging".equals(packageName)) {
            GeneratedAccess_com_codename1_messaging.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.notifications".equals(packageName)) {
            GeneratedAccess_com_codename1_notifications.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.payment".equals(packageName)) {
            GeneratedAccess_com_codename1_payment.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.plugin".equals(packageName)) {
            GeneratedAccess_com_codename1_plugin.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.plugin.event".equals(packageName)) {
            GeneratedAccess_com_codename1_plugin_event.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.processing".equals(packageName)) {
            GeneratedAccess_com_codename1_processing.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.properties".equals(packageName)) {
            GeneratedAccess_com_codename1_properties.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.push".equals(packageName)) {
            GeneratedAccess_com_codename1_push.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.share".equals(packageName)) {
            GeneratedAccess_com_codename1_share.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.social".equals(packageName)) {
            GeneratedAccess_com_codename1_social.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.system".equals(packageName)) {
            GeneratedAccess_com_codename1_system.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.testing".equals(packageName)) {
            GeneratedAccess_com_codename1_testing.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui".equals(packageName)) {
            GeneratedAccess_com_codename1_ui.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.animations".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_animations.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.css".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_css.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.events".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_events.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.geom".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_geom.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.html".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_html.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.layouts".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_layouts.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.layouts.mig".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_layouts_mig.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.list".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_list.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.painter".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_painter.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.plaf".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_plaf.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.scene".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_scene.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.spinner".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_spinner.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.table".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_table.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.tree".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_tree.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.util".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_util.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.validation".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_validation.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.util".equals(packageName)) {
            GeneratedAccess_com_codename1_util.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.util.promise".equals(packageName)) {
            GeneratedAccess_com_codename1_util_promise.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.util.regex".equals(packageName)) {
            GeneratedAccess_com_codename1_util_regex.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.xml".equals(packageName)) {
            GeneratedAccess_com_codename1_xml.setStaticField(type, name, value);
            return;
        }
        if ("com.codenameone.playground".equals(packageName)) {
            GeneratedAccess_com_codenameone_playground.setStaticField(type, name, value);
            return;
        }
        if ("java.io".equals(packageName)) {
            GeneratedAccess_java_io.setStaticField(type, name, value);
            return;
        }
        if ("java.lang".equals(packageName)) {
            GeneratedAccess_java_lang.setStaticField(type, name, value);
            return;
        }
        if ("java.lang.ref".equals(packageName)) {
            GeneratedAccess_java_lang_ref.setStaticField(type, name, value);
            return;
        }
        if ("java.lang.reflect".equals(packageName)) {
            GeneratedAccess_java_lang_reflect.setStaticField(type, name, value);
            return;
        }
        if ("java.net".equals(packageName)) {
            GeneratedAccess_java_net.setStaticField(type, name, value);
            return;
        }
        if ("java.nio.charset".equals(packageName)) {
            GeneratedAccess_java_nio_charset.setStaticField(type, name, value);
            return;
        }
        if ("java.text".equals(packageName)) {
            GeneratedAccess_java_text.setStaticField(type, name, value);
            return;
        }
        if ("java.util".equals(packageName)) {
            GeneratedAccess_java_util.setStaticField(type, name, value);
            return;
        }
        if ("java.util.concurrent".equals(packageName)) {
            GeneratedAccess_java_util_concurrent.setStaticField(type, name, value);
            return;
        }
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    @Override
    public void setField(Object target, String name, Object value) throws Exception {
        CN1AccessException unsupported = null;
        try {
            GeneratedAccess_com_codename1_ads.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_analytics.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_annotations.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_background.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_capture.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts_compat.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts_models.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts_renderers.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts_transitions.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts_util.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts_views.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_cloud.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_codescan.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_compat_java_util.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_components.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_contacts.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_db.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_facebook.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_facebook_ui.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_impl.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_io.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_io_gzip.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_io_rest.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_io_services.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_io_tar.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_javascript.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_l10n.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_location.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_maps.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_maps_layers.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_maps_providers.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_media.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_messaging.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_notifications.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_payment.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_plugin.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_plugin_event.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_processing.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_properties.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_push.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_share.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_social.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_system.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_testing.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_animations.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_css.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_events.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_geom.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_html.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_layouts.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_layouts_mig.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_list.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_painter.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_plaf.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_scene.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_spinner.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_table.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_tree.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_util.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_validation.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_util.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_util_promise.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_util_regex.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_xml.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codenameone_playground.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_io.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_lang.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_lang_ref.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_lang_reflect.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_net.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_nio_charset.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_text.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_util.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_util_concurrent.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedFieldWrite(target, name, value);
    }

    private static String packageName(Class<?> type) {
        String name = type.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? "" : name.substring(0, lastDot);
    }

    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {
        return new CN1AccessException("Generated constructor dispatch not implemented for " + type.getName() + describeArgs(args));
    }

    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {
        return new CN1AccessException("Generated static dispatch not implemented for " + type.getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {
        return new CN1AccessException("Generated instance dispatch not implemented for " + target.getClass().getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {
        return new CN1AccessException("Generated static field access not implemented for " + type.getName() + "." + name);
    }

    private static CN1AccessException unsupportedField(Object target, String name) {
        return new CN1AccessException("Generated field access not implemented for " + target.getClass().getName() + "." + name);
    }

    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {
        return new CN1AccessException("Generated static field write not implemented for " + type.getName() + "." + name + " value=" + describeValue(value));
    }

    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {
        return new CN1AccessException("Generated field write not implemented for " + target.getClass().getName() + "." + name + " value=" + describeValue(value));
    }

    private static boolean shouldDebugFindClass(String name) {
        return name != null && (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components."));
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeValue(args[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String describeValue(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
