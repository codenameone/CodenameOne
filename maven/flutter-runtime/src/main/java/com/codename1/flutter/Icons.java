/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

import com.codename1.ui.FontImage;

/**
 * Material icons, named as in Flutter's {@code Icons} class and backed by
 * the CN1 material icon font codepoints. Every
 * {@code FontImage.MATERIAL_*} glyph is here under Flutter's lower-case name.
 */
///
/// Each icon is a method, not a static final field: one class initializer that built
/// all 2,176 IconData objects was kept whole by ParparVM's dead-code removal and grew
/// every native executable ~20%. A method the app never calls is dropped instead.
/// IconData compares by code point, so a fresh instance per read is still == in Dart.
public final class Icons {

    private Icons() {
    }

    public static IconData access_alarm() {
        return new IconData(FontImage.MATERIAL_ACCESS_ALARM);
    }
    public static IconData access_time() {
        return new IconData(FontImage.MATERIAL_ACCESS_TIME);
    }
    public static IconData account_circle() {
        return new IconData(FontImage.MATERIAL_ACCOUNT_CIRCLE);
    }
    public static IconData add() {
        return new IconData(FontImage.MATERIAL_ADD);
    }
    public static IconData add_circle() {
        return new IconData(FontImage.MATERIAL_ADD_CIRCLE);
    }
    public static IconData add_circle_outline() {
        return new IconData(FontImage.MATERIAL_ADD_CIRCLE_OUTLINE);
    }
    public static IconData add_comment() {
        return new IconData(FontImage.MATERIAL_ADD_COMMENT);
    }
    public static IconData add_shopping_cart() {
        return new IconData(FontImage.MATERIAL_ADD_SHOPPING_CART);
    }
    public static IconData airplanemode_active() {
        return new IconData(FontImage.MATERIAL_AIRPLANEMODE_ACTIVE);
    }
    public static IconData alarm_on() {
        return new IconData(FontImage.MATERIAL_ALARM_ON);
    }
    public static IconData arrow_back() {
        return new IconData(FontImage.MATERIAL_ARROW_BACK);
    }
    public static IconData arrow_back_ios() {
        return new IconData(FontImage.MATERIAL_ARROW_BACK_IOS);
    }
    public static IconData arrow_drop_down() {
        return new IconData(FontImage.MATERIAL_ARROW_DROP_DOWN);
    }
    public static IconData arrow_drop_up() {
        return new IconData(FontImage.MATERIAL_ARROW_DROP_UP);
    }
    public static IconData arrow_forward() {
        return new IconData(FontImage.MATERIAL_ARROW_FORWARD);
    }
    public static IconData arrow_forward_ios() {
        return new IconData(FontImage.MATERIAL_ARROW_FORWARD_IOS);
    }
    public static IconData arrow_left() {
        return new IconData(FontImage.MATERIAL_ARROW_LEFT);
    }
    public static IconData attach_money() {
        return new IconData(FontImage.MATERIAL_ATTACH_MONEY);
    }
    public static IconData book() {
        return new IconData(FontImage.MATERIAL_BOOK);
    }
    public static IconData bookmark_border() {
        return new IconData(FontImage.MATERIAL_BOOKMARK_BORDER);
    }
    public static IconData brightness_5() {
        return new IconData(FontImage.MATERIAL_BRIGHTNESS_5);
    }
    public static IconData calendar_today() {
        return new IconData(FontImage.MATERIAL_CALENDAR_TODAY);
    }
    public static IconData camera_enhance() {
        return new IconData(FontImage.MATERIAL_CAMERA_ENHANCE);
    }
    public static IconData check() {
        return new IconData(FontImage.MATERIAL_CHECK);
    }
    public static IconData check_circle() {
        return new IconData(FontImage.MATERIAL_CHECK_CIRCLE);
    }
    public static IconData check_circle_outline() {
        return new IconData(FontImage.MATERIAL_CHECK_CIRCLE_OUTLINE);
    }
    public static IconData chevron_left() {
        return new IconData(FontImage.MATERIAL_CHEVRON_LEFT);
    }
    public static IconData chevron_right() {
        return new IconData(FontImage.MATERIAL_CHEVRON_RIGHT);
    }
    public static IconData close() {
        return new IconData(FontImage.MATERIAL_CLOSE);
    }
    public static IconData code() {
        return new IconData(FontImage.MATERIAL_CODE);
    }
    public static IconData comment() {
        return new IconData(FontImage.MATERIAL_COMMENT);
    }
    public static IconData create() {
        return new IconData(FontImage.MATERIAL_CREATE);
    }
    public static IconData credit_card() {
        return new IconData(FontImage.MATERIAL_CREDIT_CARD);
    }
    public static IconData date_range() {
        return new IconData(FontImage.MATERIAL_DATE_RANGE);
    }
    public static IconData delete() {
        return new IconData(FontImage.MATERIAL_DELETE);
    }
    public static IconData directions_bike() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_BIKE);
    }
    public static IconData edit() {
        return new IconData(FontImage.MATERIAL_EDIT);
    }
    public static IconData email() {
        return new IconData(FontImage.MATERIAL_EMAIL);
    }
    public static IconData favorite() {
        return new IconData(FontImage.MATERIAL_FAVORITE);
    }
    public static IconData favorite_border() {
        return new IconData(FontImage.MATERIAL_FAVORITE_BORDER);
    }
    public static IconData feedback() {
        return new IconData(FontImage.MATERIAL_FEEDBACK);
    }
    public static IconData format_bold() {
        return new IconData(FontImage.MATERIAL_FORMAT_BOLD);
    }
    public static IconData format_italic() {
        return new IconData(FontImage.MATERIAL_FORMAT_ITALIC);
    }
    public static IconData format_underline() {
        return new IconData(FontImage.MATERIAL_FORMAT_UNDERLINE);
    }
    public static IconData fullscreen() {
        return new IconData(FontImage.MATERIAL_FULLSCREEN);
    }
    public static IconData help() {
        return new IconData(FontImage.MATERIAL_HELP);
    }
    public static IconData home() {
        return new IconData(FontImage.MATERIAL_HOME);
    }
    public static IconData hotel() {
        return new IconData(FontImage.MATERIAL_HOTEL);
    }
    public static IconData info() {
        return new IconData(FontImage.MATERIAL_INFO);
    }
    public static IconData info_outline() {
        return new IconData(FontImage.MATERIAL_INFO_OUTLINE);
    }
    public static IconData keyboard_arrow_down() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_ARROW_DOWN);
    }
    public static IconData keyboard_arrow_up() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_ARROW_UP);
    }
    public static IconData library_books() {
        return new IconData(FontImage.MATERIAL_LIBRARY_BOOKS);
    }
    public static IconData link() {
        return new IconData(FontImage.MATERIAL_LINK);
    }
    public static IconData lock() {
        return new IconData(FontImage.MATERIAL_LOCK);
    }
    public static IconData menu() {
        return new IconData(FontImage.MATERIAL_MENU);
    }
    public static IconData mic() {
        return new IconData(FontImage.MATERIAL_MIC);
    }
    public static IconData money_off() {
        return new IconData(FontImage.MATERIAL_MONEY_OFF);
    }
    public static IconData more_vert() {
        return new IconData(FontImage.MATERIAL_MORE_VERT);
    }
    public static IconData more_horiz() {
        return new IconData(FontImage.MATERIAL_MORE_HORIZ);
    }
    public static IconData not_interested() {
        return new IconData(FontImage.MATERIAL_NOT_INTERESTED);
    }
    public static IconData notifications() {
        return new IconData(FontImage.MATERIAL_NOTIFICATIONS);
    }
    public static IconData person() {
        return new IconData(FontImage.MATERIAL_PERSON);
    }
    public static IconData person_add() {
        return new IconData(FontImage.MATERIAL_PERSON_ADD);
    }
    public static IconData phone() {
        return new IconData(FontImage.MATERIAL_PHONE);
    }
    public static IconData photo() {
        return new IconData(FontImage.MATERIAL_PHOTO);
    }
    public static IconData photo_library() {
        return new IconData(FontImage.MATERIAL_PHOTO_LIBRARY);
    }
    public static IconData pie_chart() {
        return new IconData(FontImage.MATERIAL_PIE_CHART);
    }
    public static IconData place() {
        return new IconData(FontImage.MATERIAL_PLACE);
    }
    public static IconData remove() {
        return new IconData(FontImage.MATERIAL_REMOVE);
    }
    public static IconData remove_circle_outline() {
        return new IconData(FontImage.MATERIAL_REMOVE_CIRCLE_OUTLINE);
    }
    public static IconData refresh() {
        return new IconData(FontImage.MATERIAL_REFRESH);
    }
    public static IconData replay() {
        return new IconData(FontImage.MATERIAL_REPLAY);
    }
    public static IconData reply_all() {
        return new IconData(FontImage.MATERIAL_REPLY_ALL);
    }
    public static IconData restaurant_menu() {
        return new IconData(FontImage.MATERIAL_RESTAURANT_MENU);
    }
    public static IconData search() {
        return new IconData(FontImage.MATERIAL_SEARCH);
    }
    public static IconData security() {
        return new IconData(FontImage.MATERIAL_SECURITY);
    }
    public static IconData settings() {
        return new IconData(FontImage.MATERIAL_SETTINGS);
    }
    public static IconData share() {
        return new IconData(FontImage.MATERIAL_SHARE);
    }
    public static IconData shopping_cart() {
        return new IconData(FontImage.MATERIAL_SHOPPING_CART);
    }
    public static IconData sort() {
        return new IconData(FontImage.MATERIAL_SORT);
    }
    public static IconData star() {
        return new IconData(FontImage.MATERIAL_STAR);
    }
    public static IconData star_border() {
        return new IconData(FontImage.MATERIAL_STAR_BORDER);
    }
    public static IconData table_chart() {
        return new IconData(FontImage.MATERIAL_TABLE_CHART);
    }
    public static IconData tune() {
        return new IconData(FontImage.MATERIAL_TUNE);
    }
    public static IconData vertical_split() {
        return new IconData(FontImage.MATERIAL_VERTICAL_SPLIT);
    }
    public static IconData visibility() {
        return new IconData(FontImage.MATERIAL_VISIBILITY);
    }
    public static IconData visibility_off() {
        return new IconData(FontImage.MATERIAL_VISIBILITY_OFF);
    }
    public static IconData web_asset() {
        return new IconData(FontImage.MATERIAL_WEB_ASSET);
    }

    // The rest of the Material set, one constant per FontImage.MATERIAL_* glyph under
    // Flutter's lower-case name.
    public static IconData abc() {
        return new IconData(FontImage.MATERIAL_ABC);
    }
    public static IconData ac_unit() {
        return new IconData(FontImage.MATERIAL_AC_UNIT);
    }
    public static IconData access_alarms() {
        return new IconData(FontImage.MATERIAL_ACCESS_ALARMS);
    }
    public static IconData access_time_filled() {
        return new IconData(FontImage.MATERIAL_ACCESS_TIME_FILLED);
    }
    public static IconData accessibility() {
        return new IconData(FontImage.MATERIAL_ACCESSIBILITY);
    }
    public static IconData accessibility_new() {
        return new IconData(FontImage.MATERIAL_ACCESSIBILITY_NEW);
    }
    public static IconData accessible() {
        return new IconData(FontImage.MATERIAL_ACCESSIBLE);
    }
    public static IconData accessible_forward() {
        return new IconData(FontImage.MATERIAL_ACCESSIBLE_FORWARD);
    }
    public static IconData account_balance() {
        return new IconData(FontImage.MATERIAL_ACCOUNT_BALANCE);
    }
    public static IconData account_balance_wallet() {
        return new IconData(FontImage.MATERIAL_ACCOUNT_BALANCE_WALLET);
    }
    public static IconData account_box() {
        return new IconData(FontImage.MATERIAL_ACCOUNT_BOX);
    }
    public static IconData account_tree() {
        return new IconData(FontImage.MATERIAL_ACCOUNT_TREE);
    }
    public static IconData ad_units() {
        return new IconData(FontImage.MATERIAL_AD_UNITS);
    }
    public static IconData adb() {
        return new IconData(FontImage.MATERIAL_ADB);
    }
    public static IconData add_a_photo() {
        return new IconData(FontImage.MATERIAL_ADD_A_PHOTO);
    }
    public static IconData add_alarm() {
        return new IconData(FontImage.MATERIAL_ADD_ALARM);
    }
    public static IconData add_alert() {
        return new IconData(FontImage.MATERIAL_ADD_ALERT);
    }
    public static IconData add_box() {
        return new IconData(FontImage.MATERIAL_ADD_BOX);
    }
    public static IconData add_business() {
        return new IconData(FontImage.MATERIAL_ADD_BUSINESS);
    }
    public static IconData add_call() {
        return new IconData(FontImage.MATERIAL_ADD_CALL);
    }
    public static IconData add_card() {
        return new IconData(FontImage.MATERIAL_ADD_CARD);
    }
    public static IconData add_chart() {
        return new IconData(FontImage.MATERIAL_ADD_CHART);
    }
    public static IconData add_home() {
        return new IconData(FontImage.MATERIAL_ADD_HOME);
    }
    public static IconData add_home_work() {
        return new IconData(FontImage.MATERIAL_ADD_HOME_WORK);
    }
    public static IconData add_ic_call() {
        return new IconData(FontImage.MATERIAL_ADD_IC_CALL);
    }
    public static IconData add_link() {
        return new IconData(FontImage.MATERIAL_ADD_LINK);
    }
    public static IconData add_location() {
        return new IconData(FontImage.MATERIAL_ADD_LOCATION);
    }
    public static IconData add_location_alt() {
        return new IconData(FontImage.MATERIAL_ADD_LOCATION_ALT);
    }
    public static IconData add_moderator() {
        return new IconData(FontImage.MATERIAL_ADD_MODERATOR);
    }
    public static IconData add_photo_alternate() {
        return new IconData(FontImage.MATERIAL_ADD_PHOTO_ALTERNATE);
    }
    public static IconData add_reaction() {
        return new IconData(FontImage.MATERIAL_ADD_REACTION);
    }
    public static IconData add_road() {
        return new IconData(FontImage.MATERIAL_ADD_ROAD);
    }
    public static IconData add_task() {
        return new IconData(FontImage.MATERIAL_ADD_TASK);
    }
    public static IconData add_to_drive() {
        return new IconData(FontImage.MATERIAL_ADD_TO_DRIVE);
    }
    public static IconData add_to_home_screen() {
        return new IconData(FontImage.MATERIAL_ADD_TO_HOME_SCREEN);
    }
    public static IconData add_to_photos() {
        return new IconData(FontImage.MATERIAL_ADD_TO_PHOTOS);
    }
    public static IconData add_to_queue() {
        return new IconData(FontImage.MATERIAL_ADD_TO_QUEUE);
    }
    public static IconData addchart() {
        return new IconData(FontImage.MATERIAL_ADDCHART);
    }
    public static IconData adf_scanner() {
        return new IconData(FontImage.MATERIAL_ADF_SCANNER);
    }
    public static IconData adjust() {
        return new IconData(FontImage.MATERIAL_ADJUST);
    }
    public static IconData admin_panel_settings() {
        return new IconData(FontImage.MATERIAL_ADMIN_PANEL_SETTINGS);
    }
    public static IconData adobe() {
        return new IconData(FontImage.MATERIAL_ADOBE);
    }
    public static IconData ads_click() {
        return new IconData(FontImage.MATERIAL_ADS_CLICK);
    }
    public static IconData agriculture() {
        return new IconData(FontImage.MATERIAL_AGRICULTURE);
    }
    public static IconData air() {
        return new IconData(FontImage.MATERIAL_AIR);
    }
    public static IconData airline_seat_flat() {
        return new IconData(FontImage.MATERIAL_AIRLINE_SEAT_FLAT);
    }
    public static IconData airline_seat_flat_angled() {
        return new IconData(FontImage.MATERIAL_AIRLINE_SEAT_FLAT_ANGLED);
    }
    public static IconData airline_seat_individual_suite() {
        return new IconData(FontImage.MATERIAL_AIRLINE_SEAT_INDIVIDUAL_SUITE);
    }
    public static IconData airline_seat_legroom_extra() {
        return new IconData(FontImage.MATERIAL_AIRLINE_SEAT_LEGROOM_EXTRA);
    }
    public static IconData airline_seat_legroom_normal() {
        return new IconData(FontImage.MATERIAL_AIRLINE_SEAT_LEGROOM_NORMAL);
    }
    public static IconData airline_seat_legroom_reduced() {
        return new IconData(FontImage.MATERIAL_AIRLINE_SEAT_LEGROOM_REDUCED);
    }
    public static IconData airline_seat_recline_extra() {
        return new IconData(FontImage.MATERIAL_AIRLINE_SEAT_RECLINE_EXTRA);
    }
    public static IconData airline_seat_recline_normal() {
        return new IconData(FontImage.MATERIAL_AIRLINE_SEAT_RECLINE_NORMAL);
    }
    public static IconData airline_stops() {
        return new IconData(FontImage.MATERIAL_AIRLINE_STOPS);
    }
    public static IconData airlines() {
        return new IconData(FontImage.MATERIAL_AIRLINES);
    }
    public static IconData airplane_ticket() {
        return new IconData(FontImage.MATERIAL_AIRPLANE_TICKET);
    }
    public static IconData airplanemode_inactive() {
        return new IconData(FontImage.MATERIAL_AIRPLANEMODE_INACTIVE);
    }
    public static IconData airplanemode_off() {
        return new IconData(FontImage.MATERIAL_AIRPLANEMODE_OFF);
    }
    public static IconData airplanemode_on() {
        return new IconData(FontImage.MATERIAL_AIRPLANEMODE_ON);
    }
    public static IconData airplay() {
        return new IconData(FontImage.MATERIAL_AIRPLAY);
    }
    public static IconData airport_shuttle() {
        return new IconData(FontImage.MATERIAL_AIRPORT_SHUTTLE);
    }
    public static IconData alarm() {
        return new IconData(FontImage.MATERIAL_ALARM);
    }
    public static IconData alarm_add() {
        return new IconData(FontImage.MATERIAL_ALARM_ADD);
    }
    public static IconData alarm_off() {
        return new IconData(FontImage.MATERIAL_ALARM_OFF);
    }
    public static IconData album() {
        return new IconData(FontImage.MATERIAL_ALBUM);
    }
    public static IconData align_horizontal_center() {
        return new IconData(FontImage.MATERIAL_ALIGN_HORIZONTAL_CENTER);
    }
    public static IconData align_horizontal_left() {
        return new IconData(FontImage.MATERIAL_ALIGN_HORIZONTAL_LEFT);
    }
    public static IconData align_horizontal_right() {
        return new IconData(FontImage.MATERIAL_ALIGN_HORIZONTAL_RIGHT);
    }
    public static IconData align_vertical_bottom() {
        return new IconData(FontImage.MATERIAL_ALIGN_VERTICAL_BOTTOM);
    }
    public static IconData align_vertical_center() {
        return new IconData(FontImage.MATERIAL_ALIGN_VERTICAL_CENTER);
    }
    public static IconData align_vertical_top() {
        return new IconData(FontImage.MATERIAL_ALIGN_VERTICAL_TOP);
    }
    public static IconData all_inbox() {
        return new IconData(FontImage.MATERIAL_ALL_INBOX);
    }
    public static IconData all_inclusive() {
        return new IconData(FontImage.MATERIAL_ALL_INCLUSIVE);
    }
    public static IconData all_out() {
        return new IconData(FontImage.MATERIAL_ALL_OUT);
    }
    public static IconData alt_route() {
        return new IconData(FontImage.MATERIAL_ALT_ROUTE);
    }
    public static IconData alternate_email() {
        return new IconData(FontImage.MATERIAL_ALTERNATE_EMAIL);
    }
    public static IconData amp_stories() {
        return new IconData(FontImage.MATERIAL_AMP_STORIES);
    }
    public static IconData analytics() {
        return new IconData(FontImage.MATERIAL_ANALYTICS);
    }
    public static IconData anchor() {
        return new IconData(FontImage.MATERIAL_ANCHOR);
    }
    public static IconData android() {
        return new IconData(FontImage.MATERIAL_ANDROID);
    }
    public static IconData animation() {
        return new IconData(FontImage.MATERIAL_ANIMATION);
    }
    public static IconData announcement() {
        return new IconData(FontImage.MATERIAL_ANNOUNCEMENT);
    }
    public static IconData aod() {
        return new IconData(FontImage.MATERIAL_AOD);
    }
    public static IconData apartment() {
        return new IconData(FontImage.MATERIAL_APARTMENT);
    }
    public static IconData api() {
        return new IconData(FontImage.MATERIAL_API);
    }
    public static IconData app_blocking() {
        return new IconData(FontImage.MATERIAL_APP_BLOCKING);
    }
    public static IconData app_registration() {
        return new IconData(FontImage.MATERIAL_APP_REGISTRATION);
    }
    public static IconData app_settings_alt() {
        return new IconData(FontImage.MATERIAL_APP_SETTINGS_ALT);
    }
    public static IconData app_shortcut() {
        return new IconData(FontImage.MATERIAL_APP_SHORTCUT);
    }
    public static IconData apple() {
        return new IconData(FontImage.MATERIAL_APPLE);
    }
    public static IconData approval() {
        return new IconData(FontImage.MATERIAL_APPROVAL);
    }
    public static IconData apps() {
        return new IconData(FontImage.MATERIAL_APPS);
    }
    public static IconData apps_outage() {
        return new IconData(FontImage.MATERIAL_APPS_OUTAGE);
    }
    public static IconData architecture() {
        return new IconData(FontImage.MATERIAL_ARCHITECTURE);
    }
    public static IconData archive() {
        return new IconData(FontImage.MATERIAL_ARCHIVE);
    }
    public static IconData area_chart() {
        return new IconData(FontImage.MATERIAL_AREA_CHART);
    }
    public static IconData arrow_back_ios_new() {
        return new IconData(FontImage.MATERIAL_ARROW_BACK_IOS_NEW);
    }
    public static IconData arrow_circle_down() {
        return new IconData(FontImage.MATERIAL_ARROW_CIRCLE_DOWN);
    }
    public static IconData arrow_circle_left() {
        return new IconData(FontImage.MATERIAL_ARROW_CIRCLE_LEFT);
    }
    public static IconData arrow_circle_right() {
        return new IconData(FontImage.MATERIAL_ARROW_CIRCLE_RIGHT);
    }
    public static IconData arrow_circle_up() {
        return new IconData(FontImage.MATERIAL_ARROW_CIRCLE_UP);
    }
    public static IconData arrow_downward() {
        return new IconData(FontImage.MATERIAL_ARROW_DOWNWARD);
    }
    public static IconData arrow_drop_down_circle() {
        return new IconData(FontImage.MATERIAL_ARROW_DROP_DOWN_CIRCLE);
    }
    public static IconData arrow_outward() {
        return new IconData(FontImage.MATERIAL_ARROW_OUTWARD);
    }
    public static IconData arrow_right() {
        return new IconData(FontImage.MATERIAL_ARROW_RIGHT);
    }
    public static IconData arrow_right_alt() {
        return new IconData(FontImage.MATERIAL_ARROW_RIGHT_ALT);
    }
    public static IconData arrow_upward() {
        return new IconData(FontImage.MATERIAL_ARROW_UPWARD);
    }
    public static IconData art_track() {
        return new IconData(FontImage.MATERIAL_ART_TRACK);
    }
    public static IconData article() {
        return new IconData(FontImage.MATERIAL_ARTICLE);
    }
    public static IconData aspect_ratio() {
        return new IconData(FontImage.MATERIAL_ASPECT_RATIO);
    }
    public static IconData assessment() {
        return new IconData(FontImage.MATERIAL_ASSESSMENT);
    }
    public static IconData assignment() {
        return new IconData(FontImage.MATERIAL_ASSIGNMENT);
    }
    public static IconData assignment_add() {
        return new IconData(FontImage.MATERIAL_ASSIGNMENT_ADD);
    }
    public static IconData assignment_ind() {
        return new IconData(FontImage.MATERIAL_ASSIGNMENT_IND);
    }
    public static IconData assignment_late() {
        return new IconData(FontImage.MATERIAL_ASSIGNMENT_LATE);
    }
    public static IconData assignment_return() {
        return new IconData(FontImage.MATERIAL_ASSIGNMENT_RETURN);
    }
    public static IconData assignment_returned() {
        return new IconData(FontImage.MATERIAL_ASSIGNMENT_RETURNED);
    }
    public static IconData assignment_turned_in() {
        return new IconData(FontImage.MATERIAL_ASSIGNMENT_TURNED_IN);
    }
    public static IconData assist_walker() {
        return new IconData(FontImage.MATERIAL_ASSIST_WALKER);
    }
    public static IconData assistant() {
        return new IconData(FontImage.MATERIAL_ASSISTANT);
    }
    public static IconData assistant_direction() {
        return new IconData(FontImage.MATERIAL_ASSISTANT_DIRECTION);
    }
    public static IconData assistant_navigation() {
        return new IconData(FontImage.MATERIAL_ASSISTANT_NAVIGATION);
    }
    public static IconData assistant_photo() {
        return new IconData(FontImage.MATERIAL_ASSISTANT_PHOTO);
    }
    public static IconData assured_workload() {
        return new IconData(FontImage.MATERIAL_ASSURED_WORKLOAD);
    }
    public static IconData atm() {
        return new IconData(FontImage.MATERIAL_ATM);
    }
    public static IconData attach_email() {
        return new IconData(FontImage.MATERIAL_ATTACH_EMAIL);
    }
    public static IconData attach_file() {
        return new IconData(FontImage.MATERIAL_ATTACH_FILE);
    }
    public static IconData attachment() {
        return new IconData(FontImage.MATERIAL_ATTACHMENT);
    }
    public static IconData attractions() {
        return new IconData(FontImage.MATERIAL_ATTRACTIONS);
    }
    public static IconData attribution() {
        return new IconData(FontImage.MATERIAL_ATTRIBUTION);
    }
    public static IconData audio_file() {
        return new IconData(FontImage.MATERIAL_AUDIO_FILE);
    }
    public static IconData audiotrack() {
        return new IconData(FontImage.MATERIAL_AUDIOTRACK);
    }
    public static IconData auto_awesome() {
        return new IconData(FontImage.MATERIAL_AUTO_AWESOME);
    }
    public static IconData auto_awesome_mosaic() {
        return new IconData(FontImage.MATERIAL_AUTO_AWESOME_MOSAIC);
    }
    public static IconData auto_awesome_motion() {
        return new IconData(FontImage.MATERIAL_AUTO_AWESOME_MOTION);
    }
    public static IconData auto_delete() {
        return new IconData(FontImage.MATERIAL_AUTO_DELETE);
    }
    public static IconData auto_fix_high() {
        return new IconData(FontImage.MATERIAL_AUTO_FIX_HIGH);
    }
    public static IconData auto_fix_normal() {
        return new IconData(FontImage.MATERIAL_AUTO_FIX_NORMAL);
    }
    public static IconData auto_fix_off() {
        return new IconData(FontImage.MATERIAL_AUTO_FIX_OFF);
    }
    public static IconData auto_graph() {
        return new IconData(FontImage.MATERIAL_AUTO_GRAPH);
    }
    public static IconData auto_mode() {
        return new IconData(FontImage.MATERIAL_AUTO_MODE);
    }
    public static IconData auto_stories() {
        return new IconData(FontImage.MATERIAL_AUTO_STORIES);
    }
    public static IconData autofps_select() {
        return new IconData(FontImage.MATERIAL_AUTOFPS_SELECT);
    }
    public static IconData autorenew() {
        return new IconData(FontImage.MATERIAL_AUTORENEW);
    }
    public static IconData av_timer() {
        return new IconData(FontImage.MATERIAL_AV_TIMER);
    }
    public static IconData baby_changing_station() {
        return new IconData(FontImage.MATERIAL_BABY_CHANGING_STATION);
    }
    public static IconData back_hand() {
        return new IconData(FontImage.MATERIAL_BACK_HAND);
    }
    public static IconData backpack() {
        return new IconData(FontImage.MATERIAL_BACKPACK);
    }
    public static IconData backspace() {
        return new IconData(FontImage.MATERIAL_BACKSPACE);
    }
    public static IconData backup() {
        return new IconData(FontImage.MATERIAL_BACKUP);
    }
    public static IconData backup_table() {
        return new IconData(FontImage.MATERIAL_BACKUP_TABLE);
    }
    public static IconData badge() {
        return new IconData(FontImage.MATERIAL_BADGE);
    }
    public static IconData bakery_dining() {
        return new IconData(FontImage.MATERIAL_BAKERY_DINING);
    }
    public static IconData balance() {
        return new IconData(FontImage.MATERIAL_BALANCE);
    }
    public static IconData balcony() {
        return new IconData(FontImage.MATERIAL_BALCONY);
    }
    public static IconData ballot() {
        return new IconData(FontImage.MATERIAL_BALLOT);
    }
    public static IconData bar_chart() {
        return new IconData(FontImage.MATERIAL_BAR_CHART);
    }
    public static IconData barcode_reader() {
        return new IconData(FontImage.MATERIAL_BARCODE_READER);
    }
    public static IconData batch_prediction() {
        return new IconData(FontImage.MATERIAL_BATCH_PREDICTION);
    }
    public static IconData bathroom() {
        return new IconData(FontImage.MATERIAL_BATHROOM);
    }
    public static IconData bathtub() {
        return new IconData(FontImage.MATERIAL_BATHTUB);
    }
    public static IconData battery_0_bar() {
        return new IconData(FontImage.MATERIAL_BATTERY_0_BAR);
    }
    public static IconData battery_1_bar() {
        return new IconData(FontImage.MATERIAL_BATTERY_1_BAR);
    }
    public static IconData battery_2_bar() {
        return new IconData(FontImage.MATERIAL_BATTERY_2_BAR);
    }
    public static IconData battery_3_bar() {
        return new IconData(FontImage.MATERIAL_BATTERY_3_BAR);
    }
    public static IconData battery_4_bar() {
        return new IconData(FontImage.MATERIAL_BATTERY_4_BAR);
    }
    public static IconData battery_5_bar() {
        return new IconData(FontImage.MATERIAL_BATTERY_5_BAR);
    }
    public static IconData battery_6_bar() {
        return new IconData(FontImage.MATERIAL_BATTERY_6_BAR);
    }
    public static IconData battery_alert() {
        return new IconData(FontImage.MATERIAL_BATTERY_ALERT);
    }
    public static IconData battery_charging_full() {
        return new IconData(FontImage.MATERIAL_BATTERY_CHARGING_FULL);
    }
    public static IconData battery_full() {
        return new IconData(FontImage.MATERIAL_BATTERY_FULL);
    }
    public static IconData battery_saver() {
        return new IconData(FontImage.MATERIAL_BATTERY_SAVER);
    }
    public static IconData battery_std() {
        return new IconData(FontImage.MATERIAL_BATTERY_STD);
    }
    public static IconData battery_unknown() {
        return new IconData(FontImage.MATERIAL_BATTERY_UNKNOWN);
    }
    public static IconData beach_access() {
        return new IconData(FontImage.MATERIAL_BEACH_ACCESS);
    }
    public static IconData bed() {
        return new IconData(FontImage.MATERIAL_BED);
    }
    public static IconData bedroom_baby() {
        return new IconData(FontImage.MATERIAL_BEDROOM_BABY);
    }
    public static IconData bedroom_child() {
        return new IconData(FontImage.MATERIAL_BEDROOM_CHILD);
    }
    public static IconData bedroom_parent() {
        return new IconData(FontImage.MATERIAL_BEDROOM_PARENT);
    }
    public static IconData bedtime() {
        return new IconData(FontImage.MATERIAL_BEDTIME);
    }
    public static IconData bedtime_off() {
        return new IconData(FontImage.MATERIAL_BEDTIME_OFF);
    }
    public static IconData beenhere() {
        return new IconData(FontImage.MATERIAL_BEENHERE);
    }
    public static IconData bento() {
        return new IconData(FontImage.MATERIAL_BENTO);
    }
    public static IconData bike_scooter() {
        return new IconData(FontImage.MATERIAL_BIKE_SCOOTER);
    }
    public static IconData biotech() {
        return new IconData(FontImage.MATERIAL_BIOTECH);
    }
    public static IconData blender() {
        return new IconData(FontImage.MATERIAL_BLENDER);
    }
    public static IconData blind() {
        return new IconData(FontImage.MATERIAL_BLIND);
    }
    public static IconData blinds() {
        return new IconData(FontImage.MATERIAL_BLINDS);
    }
    public static IconData blinds_closed() {
        return new IconData(FontImage.MATERIAL_BLINDS_CLOSED);
    }
    public static IconData block() {
        return new IconData(FontImage.MATERIAL_BLOCK);
    }
    public static IconData block_flipped() {
        return new IconData(FontImage.MATERIAL_BLOCK_FLIPPED);
    }
    public static IconData bloodtype() {
        return new IconData(FontImage.MATERIAL_BLOODTYPE);
    }
    public static IconData bluetooth() {
        return new IconData(FontImage.MATERIAL_BLUETOOTH);
    }
    public static IconData bluetooth_audio() {
        return new IconData(FontImage.MATERIAL_BLUETOOTH_AUDIO);
    }
    public static IconData bluetooth_connected() {
        return new IconData(FontImage.MATERIAL_BLUETOOTH_CONNECTED);
    }
    public static IconData bluetooth_disabled() {
        return new IconData(FontImage.MATERIAL_BLUETOOTH_DISABLED);
    }
    public static IconData bluetooth_drive() {
        return new IconData(FontImage.MATERIAL_BLUETOOTH_DRIVE);
    }
    public static IconData bluetooth_searching() {
        return new IconData(FontImage.MATERIAL_BLUETOOTH_SEARCHING);
    }
    public static IconData blur_circular() {
        return new IconData(FontImage.MATERIAL_BLUR_CIRCULAR);
    }
    public static IconData blur_linear() {
        return new IconData(FontImage.MATERIAL_BLUR_LINEAR);
    }
    public static IconData blur_off() {
        return new IconData(FontImage.MATERIAL_BLUR_OFF);
    }
    public static IconData blur_on() {
        return new IconData(FontImage.MATERIAL_BLUR_ON);
    }
    public static IconData bolt() {
        return new IconData(FontImage.MATERIAL_BOLT);
    }
    public static IconData book_online() {
        return new IconData(FontImage.MATERIAL_BOOK_ONLINE);
    }
    public static IconData bookmark() {
        return new IconData(FontImage.MATERIAL_BOOKMARK);
    }
    public static IconData bookmark_add() {
        return new IconData(FontImage.MATERIAL_BOOKMARK_ADD);
    }
    public static IconData bookmark_added() {
        return new IconData(FontImage.MATERIAL_BOOKMARK_ADDED);
    }
    public static IconData bookmark_outline() {
        return new IconData(FontImage.MATERIAL_BOOKMARK_OUTLINE);
    }
    public static IconData bookmark_remove() {
        return new IconData(FontImage.MATERIAL_BOOKMARK_REMOVE);
    }
    public static IconData bookmarks() {
        return new IconData(FontImage.MATERIAL_BOOKMARKS);
    }
    public static IconData border_all() {
        return new IconData(FontImage.MATERIAL_BORDER_ALL);
    }
    public static IconData border_bottom() {
        return new IconData(FontImage.MATERIAL_BORDER_BOTTOM);
    }
    public static IconData border_clear() {
        return new IconData(FontImage.MATERIAL_BORDER_CLEAR);
    }
    public static IconData border_color() {
        return new IconData(FontImage.MATERIAL_BORDER_COLOR);
    }
    public static IconData border_horizontal() {
        return new IconData(FontImage.MATERIAL_BORDER_HORIZONTAL);
    }
    public static IconData border_inner() {
        return new IconData(FontImage.MATERIAL_BORDER_INNER);
    }
    public static IconData border_left() {
        return new IconData(FontImage.MATERIAL_BORDER_LEFT);
    }
    public static IconData border_outer() {
        return new IconData(FontImage.MATERIAL_BORDER_OUTER);
    }
    public static IconData border_right() {
        return new IconData(FontImage.MATERIAL_BORDER_RIGHT);
    }
    public static IconData border_style() {
        return new IconData(FontImage.MATERIAL_BORDER_STYLE);
    }
    public static IconData border_top() {
        return new IconData(FontImage.MATERIAL_BORDER_TOP);
    }
    public static IconData border_vertical() {
        return new IconData(FontImage.MATERIAL_BORDER_VERTICAL);
    }
    public static IconData boy() {
        return new IconData(FontImage.MATERIAL_BOY);
    }
    public static IconData branding_watermark() {
        return new IconData(FontImage.MATERIAL_BRANDING_WATERMARK);
    }
    public static IconData breakfast_dining() {
        return new IconData(FontImage.MATERIAL_BREAKFAST_DINING);
    }
    public static IconData brightness_1() {
        return new IconData(FontImage.MATERIAL_BRIGHTNESS_1);
    }
    public static IconData brightness_2() {
        return new IconData(FontImage.MATERIAL_BRIGHTNESS_2);
    }
    public static IconData brightness_3() {
        return new IconData(FontImage.MATERIAL_BRIGHTNESS_3);
    }
    public static IconData brightness_4() {
        return new IconData(FontImage.MATERIAL_BRIGHTNESS_4);
    }
    public static IconData brightness_6() {
        return new IconData(FontImage.MATERIAL_BRIGHTNESS_6);
    }
    public static IconData brightness_7() {
        return new IconData(FontImage.MATERIAL_BRIGHTNESS_7);
    }
    public static IconData brightness_auto() {
        return new IconData(FontImage.MATERIAL_BRIGHTNESS_AUTO);
    }
    public static IconData brightness_high() {
        return new IconData(FontImage.MATERIAL_BRIGHTNESS_HIGH);
    }
    public static IconData brightness_low() {
        return new IconData(FontImage.MATERIAL_BRIGHTNESS_LOW);
    }
    public static IconData brightness_medium() {
        return new IconData(FontImage.MATERIAL_BRIGHTNESS_MEDIUM);
    }
    public static IconData broadcast_on_home() {
        return new IconData(FontImage.MATERIAL_BROADCAST_ON_HOME);
    }
    public static IconData broadcast_on_personal() {
        return new IconData(FontImage.MATERIAL_BROADCAST_ON_PERSONAL);
    }
    public static IconData broken_image() {
        return new IconData(FontImage.MATERIAL_BROKEN_IMAGE);
    }
    public static IconData browse_gallery() {
        return new IconData(FontImage.MATERIAL_BROWSE_GALLERY);
    }
    public static IconData browser_not_supported() {
        return new IconData(FontImage.MATERIAL_BROWSER_NOT_SUPPORTED);
    }
    public static IconData browser_updated() {
        return new IconData(FontImage.MATERIAL_BROWSER_UPDATED);
    }
    public static IconData brunch_dining() {
        return new IconData(FontImage.MATERIAL_BRUNCH_DINING);
    }
    public static IconData brush() {
        return new IconData(FontImage.MATERIAL_BRUSH);
    }
    public static IconData bubble_chart() {
        return new IconData(FontImage.MATERIAL_BUBBLE_CHART);
    }
    public static IconData bug_report() {
        return new IconData(FontImage.MATERIAL_BUG_REPORT);
    }
    public static IconData build() {
        return new IconData(FontImage.MATERIAL_BUILD);
    }
    public static IconData build_circle() {
        return new IconData(FontImage.MATERIAL_BUILD_CIRCLE);
    }
    public static IconData bungalow() {
        return new IconData(FontImage.MATERIAL_BUNGALOW);
    }
    public static IconData burst_mode() {
        return new IconData(FontImage.MATERIAL_BURST_MODE);
    }
    public static IconData bus_alert() {
        return new IconData(FontImage.MATERIAL_BUS_ALERT);
    }
    public static IconData business() {
        return new IconData(FontImage.MATERIAL_BUSINESS);
    }
    public static IconData business_center() {
        return new IconData(FontImage.MATERIAL_BUSINESS_CENTER);
    }
    public static IconData cabin() {
        return new IconData(FontImage.MATERIAL_CABIN);
    }
    public static IconData cable() {
        return new IconData(FontImage.MATERIAL_CABLE);
    }
    public static IconData cached() {
        return new IconData(FontImage.MATERIAL_CACHED);
    }
    public static IconData cake() {
        return new IconData(FontImage.MATERIAL_CAKE);
    }
    public static IconData calculate() {
        return new IconData(FontImage.MATERIAL_CALCULATE);
    }
    public static IconData calendar_month() {
        return new IconData(FontImage.MATERIAL_CALENDAR_MONTH);
    }
    public static IconData calendar_view_day() {
        return new IconData(FontImage.MATERIAL_CALENDAR_VIEW_DAY);
    }
    public static IconData calendar_view_month() {
        return new IconData(FontImage.MATERIAL_CALENDAR_VIEW_MONTH);
    }
    public static IconData calendar_view_week() {
        return new IconData(FontImage.MATERIAL_CALENDAR_VIEW_WEEK);
    }
    public static IconData call() {
        return new IconData(FontImage.MATERIAL_CALL);
    }
    public static IconData call_end() {
        return new IconData(FontImage.MATERIAL_CALL_END);
    }
    public static IconData call_made() {
        return new IconData(FontImage.MATERIAL_CALL_MADE);
    }
    public static IconData call_merge() {
        return new IconData(FontImage.MATERIAL_CALL_MERGE);
    }
    public static IconData call_missed() {
        return new IconData(FontImage.MATERIAL_CALL_MISSED);
    }
    public static IconData call_missed_outgoing() {
        return new IconData(FontImage.MATERIAL_CALL_MISSED_OUTGOING);
    }
    public static IconData call_received() {
        return new IconData(FontImage.MATERIAL_CALL_RECEIVED);
    }
    public static IconData call_split() {
        return new IconData(FontImage.MATERIAL_CALL_SPLIT);
    }
    public static IconData call_to_action() {
        return new IconData(FontImage.MATERIAL_CALL_TO_ACTION);
    }
    public static IconData camera() {
        return new IconData(FontImage.MATERIAL_CAMERA);
    }
    public static IconData camera_alt() {
        return new IconData(FontImage.MATERIAL_CAMERA_ALT);
    }
    public static IconData camera_front() {
        return new IconData(FontImage.MATERIAL_CAMERA_FRONT);
    }
    public static IconData camera_indoor() {
        return new IconData(FontImage.MATERIAL_CAMERA_INDOOR);
    }
    public static IconData camera_outdoor() {
        return new IconData(FontImage.MATERIAL_CAMERA_OUTDOOR);
    }
    public static IconData camera_rear() {
        return new IconData(FontImage.MATERIAL_CAMERA_REAR);
    }
    public static IconData camera_roll() {
        return new IconData(FontImage.MATERIAL_CAMERA_ROLL);
    }
    public static IconData cameraswitch() {
        return new IconData(FontImage.MATERIAL_CAMERASWITCH);
    }
    public static IconData campaign() {
        return new IconData(FontImage.MATERIAL_CAMPAIGN);
    }
    public static IconData cancel() {
        return new IconData(FontImage.MATERIAL_CANCEL);
    }
    public static IconData cancel_presentation() {
        return new IconData(FontImage.MATERIAL_CANCEL_PRESENTATION);
    }
    public static IconData cancel_schedule_send() {
        return new IconData(FontImage.MATERIAL_CANCEL_SCHEDULE_SEND);
    }
    public static IconData candlestick_chart() {
        return new IconData(FontImage.MATERIAL_CANDLESTICK_CHART);
    }
    public static IconData car_crash() {
        return new IconData(FontImage.MATERIAL_CAR_CRASH);
    }
    public static IconData car_rental() {
        return new IconData(FontImage.MATERIAL_CAR_RENTAL);
    }
    public static IconData car_repair() {
        return new IconData(FontImage.MATERIAL_CAR_REPAIR);
    }
    public static IconData card_giftcard() {
        return new IconData(FontImage.MATERIAL_CARD_GIFTCARD);
    }
    public static IconData card_membership() {
        return new IconData(FontImage.MATERIAL_CARD_MEMBERSHIP);
    }
    public static IconData card_travel() {
        return new IconData(FontImage.MATERIAL_CARD_TRAVEL);
    }
    public static IconData carpenter() {
        return new IconData(FontImage.MATERIAL_CARPENTER);
    }
    public static IconData cases() {
        return new IconData(FontImage.MATERIAL_CASES);
    }
    public static IconData casino() {
        return new IconData(FontImage.MATERIAL_CASINO);
    }
    public static IconData cast() {
        return new IconData(FontImage.MATERIAL_CAST);
    }
    public static IconData cast_connected() {
        return new IconData(FontImage.MATERIAL_CAST_CONNECTED);
    }
    public static IconData cast_for_education() {
        return new IconData(FontImage.MATERIAL_CAST_FOR_EDUCATION);
    }
    public static IconData castle() {
        return new IconData(FontImage.MATERIAL_CASTLE);
    }
    public static IconData catching_pokemon() {
        return new IconData(FontImage.MATERIAL_CATCHING_POKEMON);
    }
    public static IconData category() {
        return new IconData(FontImage.MATERIAL_CATEGORY);
    }
    public static IconData celebration() {
        return new IconData(FontImage.MATERIAL_CELEBRATION);
    }
    public static IconData cell_tower() {
        return new IconData(FontImage.MATERIAL_CELL_TOWER);
    }
    public static IconData cell_wifi() {
        return new IconData(FontImage.MATERIAL_CELL_WIFI);
    }
    public static IconData center_focus_strong() {
        return new IconData(FontImage.MATERIAL_CENTER_FOCUS_STRONG);
    }
    public static IconData center_focus_weak() {
        return new IconData(FontImage.MATERIAL_CENTER_FOCUS_WEAK);
    }
    public static IconData chair() {
        return new IconData(FontImage.MATERIAL_CHAIR);
    }
    public static IconData chair_alt() {
        return new IconData(FontImage.MATERIAL_CHAIR_ALT);
    }
    public static IconData chalet() {
        return new IconData(FontImage.MATERIAL_CHALET);
    }
    public static IconData change_circle() {
        return new IconData(FontImage.MATERIAL_CHANGE_CIRCLE);
    }
    public static IconData change_history() {
        return new IconData(FontImage.MATERIAL_CHANGE_HISTORY);
    }
    public static IconData charging_station() {
        return new IconData(FontImage.MATERIAL_CHARGING_STATION);
    }
    public static IconData chat() {
        return new IconData(FontImage.MATERIAL_CHAT);
    }
    public static IconData chat_bubble() {
        return new IconData(FontImage.MATERIAL_CHAT_BUBBLE);
    }
    public static IconData chat_bubble_outline() {
        return new IconData(FontImage.MATERIAL_CHAT_BUBBLE_OUTLINE);
    }
    public static IconData check_box() {
        return new IconData(FontImage.MATERIAL_CHECK_BOX);
    }
    public static IconData check_box_outline_blank() {
        return new IconData(FontImage.MATERIAL_CHECK_BOX_OUTLINE_BLANK);
    }
    public static IconData checklist() {
        return new IconData(FontImage.MATERIAL_CHECKLIST);
    }
    public static IconData checklist_rtl() {
        return new IconData(FontImage.MATERIAL_CHECKLIST_RTL);
    }
    public static IconData checkroom() {
        return new IconData(FontImage.MATERIAL_CHECKROOM);
    }
    public static IconData child_care() {
        return new IconData(FontImage.MATERIAL_CHILD_CARE);
    }
    public static IconData child_friendly() {
        return new IconData(FontImage.MATERIAL_CHILD_FRIENDLY);
    }
    public static IconData chrome_reader_mode() {
        return new IconData(FontImage.MATERIAL_CHROME_READER_MODE);
    }
    public static IconData church() {
        return new IconData(FontImage.MATERIAL_CHURCH);
    }
    public static IconData circle() {
        return new IconData(FontImage.MATERIAL_CIRCLE);
    }
    public static IconData circle_notifications() {
        return new IconData(FontImage.MATERIAL_CIRCLE_NOTIFICATIONS);
    }
    public static IconData class_() {
        return new IconData(FontImage.MATERIAL_CLASS);
    }
    public static IconData clean_hands() {
        return new IconData(FontImage.MATERIAL_CLEAN_HANDS);
    }
    public static IconData cleaning_services() {
        return new IconData(FontImage.MATERIAL_CLEANING_SERVICES);
    }
    public static IconData clear() {
        return new IconData(FontImage.MATERIAL_CLEAR);
    }
    public static IconData clear_all() {
        return new IconData(FontImage.MATERIAL_CLEAR_ALL);
    }
    public static IconData close_fullscreen() {
        return new IconData(FontImage.MATERIAL_CLOSE_FULLSCREEN);
    }
    public static IconData closed_caption() {
        return new IconData(FontImage.MATERIAL_CLOSED_CAPTION);
    }
    public static IconData closed_caption_disabled() {
        return new IconData(FontImage.MATERIAL_CLOSED_CAPTION_DISABLED);
    }
    public static IconData closed_caption_off() {
        return new IconData(FontImage.MATERIAL_CLOSED_CAPTION_OFF);
    }
    public static IconData cloud() {
        return new IconData(FontImage.MATERIAL_CLOUD);
    }
    public static IconData cloud_circle() {
        return new IconData(FontImage.MATERIAL_CLOUD_CIRCLE);
    }
    public static IconData cloud_done() {
        return new IconData(FontImage.MATERIAL_CLOUD_DONE);
    }
    public static IconData cloud_download() {
        return new IconData(FontImage.MATERIAL_CLOUD_DOWNLOAD);
    }
    public static IconData cloud_off() {
        return new IconData(FontImage.MATERIAL_CLOUD_OFF);
    }
    public static IconData cloud_queue() {
        return new IconData(FontImage.MATERIAL_CLOUD_QUEUE);
    }
    public static IconData cloud_sync() {
        return new IconData(FontImage.MATERIAL_CLOUD_SYNC);
    }
    public static IconData cloud_upload() {
        return new IconData(FontImage.MATERIAL_CLOUD_UPLOAD);
    }
    public static IconData cloudy_snowing() {
        return new IconData(FontImage.MATERIAL_CLOUDY_SNOWING);
    }
    public static IconData co2() {
        return new IconData(FontImage.MATERIAL_CO2);
    }
    public static IconData co_present() {
        return new IconData(FontImage.MATERIAL_CO_PRESENT);
    }
    public static IconData code_off() {
        return new IconData(FontImage.MATERIAL_CODE_OFF);
    }
    public static IconData coffee() {
        return new IconData(FontImage.MATERIAL_COFFEE);
    }
    public static IconData coffee_maker() {
        return new IconData(FontImage.MATERIAL_COFFEE_MAKER);
    }
    public static IconData collections() {
        return new IconData(FontImage.MATERIAL_COLLECTIONS);
    }
    public static IconData collections_bookmark() {
        return new IconData(FontImage.MATERIAL_COLLECTIONS_BOOKMARK);
    }
    public static IconData color_lens() {
        return new IconData(FontImage.MATERIAL_COLOR_LENS);
    }
    public static IconData colorize() {
        return new IconData(FontImage.MATERIAL_COLORIZE);
    }
    public static IconData comment_bank() {
        return new IconData(FontImage.MATERIAL_COMMENT_BANK);
    }
    public static IconData comments_disabled() {
        return new IconData(FontImage.MATERIAL_COMMENTS_DISABLED);
    }
    public static IconData commit() {
        return new IconData(FontImage.MATERIAL_COMMIT);
    }
    public static IconData commute() {
        return new IconData(FontImage.MATERIAL_COMMUTE);
    }
    public static IconData compare() {
        return new IconData(FontImage.MATERIAL_COMPARE);
    }
    public static IconData compare_arrows() {
        return new IconData(FontImage.MATERIAL_COMPARE_ARROWS);
    }
    public static IconData compass_calibration() {
        return new IconData(FontImage.MATERIAL_COMPASS_CALIBRATION);
    }
    public static IconData compost() {
        return new IconData(FontImage.MATERIAL_COMPOST);
    }
    public static IconData compress() {
        return new IconData(FontImage.MATERIAL_COMPRESS);
    }
    public static IconData computer() {
        return new IconData(FontImage.MATERIAL_COMPUTER);
    }
    public static IconData confirmation_num() {
        return new IconData(FontImage.MATERIAL_CONFIRMATION_NUM);
    }
    public static IconData confirmation_number() {
        return new IconData(FontImage.MATERIAL_CONFIRMATION_NUMBER);
    }
    public static IconData connect_without_contact() {
        return new IconData(FontImage.MATERIAL_CONNECT_WITHOUT_CONTACT);
    }
    public static IconData connected_tv() {
        return new IconData(FontImage.MATERIAL_CONNECTED_TV);
    }
    public static IconData connecting_airports() {
        return new IconData(FontImage.MATERIAL_CONNECTING_AIRPORTS);
    }
    public static IconData construction() {
        return new IconData(FontImage.MATERIAL_CONSTRUCTION);
    }
    public static IconData contact_emergency() {
        return new IconData(FontImage.MATERIAL_CONTACT_EMERGENCY);
    }
    public static IconData contact_mail() {
        return new IconData(FontImage.MATERIAL_CONTACT_MAIL);
    }
    public static IconData contact_page() {
        return new IconData(FontImage.MATERIAL_CONTACT_PAGE);
    }
    public static IconData contact_phone() {
        return new IconData(FontImage.MATERIAL_CONTACT_PHONE);
    }
    public static IconData contact_support() {
        return new IconData(FontImage.MATERIAL_CONTACT_SUPPORT);
    }
    public static IconData contactless() {
        return new IconData(FontImage.MATERIAL_CONTACTLESS);
    }
    public static IconData contacts() {
        return new IconData(FontImage.MATERIAL_CONTACTS);
    }
    public static IconData content_copy() {
        return new IconData(FontImage.MATERIAL_CONTENT_COPY);
    }
    public static IconData content_cut() {
        return new IconData(FontImage.MATERIAL_CONTENT_CUT);
    }
    public static IconData content_paste() {
        return new IconData(FontImage.MATERIAL_CONTENT_PASTE);
    }
    public static IconData content_paste_go() {
        return new IconData(FontImage.MATERIAL_CONTENT_PASTE_GO);
    }
    public static IconData content_paste_off() {
        return new IconData(FontImage.MATERIAL_CONTENT_PASTE_OFF);
    }
    public static IconData content_paste_search() {
        return new IconData(FontImage.MATERIAL_CONTENT_PASTE_SEARCH);
    }
    public static IconData contrast() {
        return new IconData(FontImage.MATERIAL_CONTRAST);
    }
    public static IconData control_camera() {
        return new IconData(FontImage.MATERIAL_CONTROL_CAMERA);
    }
    public static IconData control_point() {
        return new IconData(FontImage.MATERIAL_CONTROL_POINT);
    }
    public static IconData control_point_duplicate() {
        return new IconData(FontImage.MATERIAL_CONTROL_POINT_DUPLICATE);
    }
    public static IconData conveyor_belt() {
        return new IconData(FontImage.MATERIAL_CONVEYOR_BELT);
    }
    public static IconData cookie() {
        return new IconData(FontImage.MATERIAL_COOKIE);
    }
    public static IconData copy_all() {
        return new IconData(FontImage.MATERIAL_COPY_ALL);
    }
    public static IconData copyright() {
        return new IconData(FontImage.MATERIAL_COPYRIGHT);
    }
    public static IconData coronavirus() {
        return new IconData(FontImage.MATERIAL_CORONAVIRUS);
    }
    public static IconData corporate_fare() {
        return new IconData(FontImage.MATERIAL_CORPORATE_FARE);
    }
    public static IconData cottage() {
        return new IconData(FontImage.MATERIAL_COTTAGE);
    }
    public static IconData countertops() {
        return new IconData(FontImage.MATERIAL_COUNTERTOPS);
    }
    public static IconData create_new_folder() {
        return new IconData(FontImage.MATERIAL_CREATE_NEW_FOLDER);
    }
    public static IconData credit_card_off() {
        return new IconData(FontImage.MATERIAL_CREDIT_CARD_OFF);
    }
    public static IconData credit_score() {
        return new IconData(FontImage.MATERIAL_CREDIT_SCORE);
    }
    public static IconData crib() {
        return new IconData(FontImage.MATERIAL_CRIB);
    }
    public static IconData crisis_alert() {
        return new IconData(FontImage.MATERIAL_CRISIS_ALERT);
    }
    public static IconData crop() {
        return new IconData(FontImage.MATERIAL_CROP);
    }
    public static IconData crop_16_9() {
        return new IconData(FontImage.MATERIAL_CROP_16_9);
    }
    public static IconData crop_3_2() {
        return new IconData(FontImage.MATERIAL_CROP_3_2);
    }
    public static IconData crop_5_4() {
        return new IconData(FontImage.MATERIAL_CROP_5_4);
    }
    public static IconData crop_7_5() {
        return new IconData(FontImage.MATERIAL_CROP_7_5);
    }
    public static IconData crop_din() {
        return new IconData(FontImage.MATERIAL_CROP_DIN);
    }
    public static IconData crop_free() {
        return new IconData(FontImage.MATERIAL_CROP_FREE);
    }
    public static IconData crop_landscape() {
        return new IconData(FontImage.MATERIAL_CROP_LANDSCAPE);
    }
    public static IconData crop_original() {
        return new IconData(FontImage.MATERIAL_CROP_ORIGINAL);
    }
    public static IconData crop_portrait() {
        return new IconData(FontImage.MATERIAL_CROP_PORTRAIT);
    }
    public static IconData crop_rotate() {
        return new IconData(FontImage.MATERIAL_CROP_ROTATE);
    }
    public static IconData crop_square() {
        return new IconData(FontImage.MATERIAL_CROP_SQUARE);
    }
    public static IconData cruelty_free() {
        return new IconData(FontImage.MATERIAL_CRUELTY_FREE);
    }
    public static IconData css() {
        return new IconData(FontImage.MATERIAL_CSS);
    }
    public static IconData currency_bitcoin() {
        return new IconData(FontImage.MATERIAL_CURRENCY_BITCOIN);
    }
    public static IconData currency_exchange() {
        return new IconData(FontImage.MATERIAL_CURRENCY_EXCHANGE);
    }
    public static IconData currency_franc() {
        return new IconData(FontImage.MATERIAL_CURRENCY_FRANC);
    }
    public static IconData currency_lira() {
        return new IconData(FontImage.MATERIAL_CURRENCY_LIRA);
    }
    public static IconData currency_pound() {
        return new IconData(FontImage.MATERIAL_CURRENCY_POUND);
    }
    public static IconData currency_ruble() {
        return new IconData(FontImage.MATERIAL_CURRENCY_RUBLE);
    }
    public static IconData currency_rupee() {
        return new IconData(FontImage.MATERIAL_CURRENCY_RUPEE);
    }
    public static IconData currency_yen() {
        return new IconData(FontImage.MATERIAL_CURRENCY_YEN);
    }
    public static IconData currency_yuan() {
        return new IconData(FontImage.MATERIAL_CURRENCY_YUAN);
    }
    public static IconData curtains() {
        return new IconData(FontImage.MATERIAL_CURTAINS);
    }
    public static IconData curtains_closed() {
        return new IconData(FontImage.MATERIAL_CURTAINS_CLOSED);
    }
    public static IconData cyclone() {
        return new IconData(FontImage.MATERIAL_CYCLONE);
    }
    public static IconData dangerous() {
        return new IconData(FontImage.MATERIAL_DANGEROUS);
    }
    public static IconData dark_mode() {
        return new IconData(FontImage.MATERIAL_DARK_MODE);
    }
    public static IconData dashboard() {
        return new IconData(FontImage.MATERIAL_DASHBOARD);
    }
    public static IconData dashboard_customize() {
        return new IconData(FontImage.MATERIAL_DASHBOARD_CUSTOMIZE);
    }
    public static IconData data_array() {
        return new IconData(FontImage.MATERIAL_DATA_ARRAY);
    }
    public static IconData data_exploration() {
        return new IconData(FontImage.MATERIAL_DATA_EXPLORATION);
    }
    public static IconData data_object() {
        return new IconData(FontImage.MATERIAL_DATA_OBJECT);
    }
    public static IconData data_saver_off() {
        return new IconData(FontImage.MATERIAL_DATA_SAVER_OFF);
    }
    public static IconData data_saver_on() {
        return new IconData(FontImage.MATERIAL_DATA_SAVER_ON);
    }
    public static IconData data_thresholding() {
        return new IconData(FontImage.MATERIAL_DATA_THRESHOLDING);
    }
    public static IconData data_usage() {
        return new IconData(FontImage.MATERIAL_DATA_USAGE);
    }
    public static IconData dataset() {
        return new IconData(FontImage.MATERIAL_DATASET);
    }
    public static IconData dataset_linked() {
        return new IconData(FontImage.MATERIAL_DATASET_LINKED);
    }
    public static IconData deblur() {
        return new IconData(FontImage.MATERIAL_DEBLUR);
    }
    public static IconData deck() {
        return new IconData(FontImage.MATERIAL_DECK);
    }
    public static IconData dehaze() {
        return new IconData(FontImage.MATERIAL_DEHAZE);
    }
    public static IconData delete_forever() {
        return new IconData(FontImage.MATERIAL_DELETE_FOREVER);
    }
    public static IconData delete_outline() {
        return new IconData(FontImage.MATERIAL_DELETE_OUTLINE);
    }
    public static IconData delete_sweep() {
        return new IconData(FontImage.MATERIAL_DELETE_SWEEP);
    }
    public static IconData delivery_dining() {
        return new IconData(FontImage.MATERIAL_DELIVERY_DINING);
    }
    public static IconData density_large() {
        return new IconData(FontImage.MATERIAL_DENSITY_LARGE);
    }
    public static IconData density_medium() {
        return new IconData(FontImage.MATERIAL_DENSITY_MEDIUM);
    }
    public static IconData density_small() {
        return new IconData(FontImage.MATERIAL_DENSITY_SMALL);
    }
    public static IconData departure_board() {
        return new IconData(FontImage.MATERIAL_DEPARTURE_BOARD);
    }
    public static IconData description() {
        return new IconData(FontImage.MATERIAL_DESCRIPTION);
    }
    public static IconData deselect() {
        return new IconData(FontImage.MATERIAL_DESELECT);
    }
    public static IconData design_services() {
        return new IconData(FontImage.MATERIAL_DESIGN_SERVICES);
    }
    public static IconData desk() {
        return new IconData(FontImage.MATERIAL_DESK);
    }
    public static IconData desktop_access_disabled() {
        return new IconData(FontImage.MATERIAL_DESKTOP_ACCESS_DISABLED);
    }
    public static IconData desktop_mac() {
        return new IconData(FontImage.MATERIAL_DESKTOP_MAC);
    }
    public static IconData desktop_windows() {
        return new IconData(FontImage.MATERIAL_DESKTOP_WINDOWS);
    }
    public static IconData details() {
        return new IconData(FontImage.MATERIAL_DETAILS);
    }
    public static IconData developer_board() {
        return new IconData(FontImage.MATERIAL_DEVELOPER_BOARD);
    }
    public static IconData developer_board_off() {
        return new IconData(FontImage.MATERIAL_DEVELOPER_BOARD_OFF);
    }
    public static IconData developer_mode() {
        return new IconData(FontImage.MATERIAL_DEVELOPER_MODE);
    }
    public static IconData device_hub() {
        return new IconData(FontImage.MATERIAL_DEVICE_HUB);
    }
    public static IconData device_thermostat() {
        return new IconData(FontImage.MATERIAL_DEVICE_THERMOSTAT);
    }
    public static IconData device_unknown() {
        return new IconData(FontImage.MATERIAL_DEVICE_UNKNOWN);
    }
    public static IconData devices() {
        return new IconData(FontImage.MATERIAL_DEVICES);
    }
    public static IconData devices_fold() {
        return new IconData(FontImage.MATERIAL_DEVICES_FOLD);
    }
    public static IconData devices_other() {
        return new IconData(FontImage.MATERIAL_DEVICES_OTHER);
    }
    public static IconData dew_point() {
        return new IconData(FontImage.MATERIAL_DEW_POINT);
    }
    public static IconData dialer_sip() {
        return new IconData(FontImage.MATERIAL_DIALER_SIP);
    }
    public static IconData dialpad() {
        return new IconData(FontImage.MATERIAL_DIALPAD);
    }
    public static IconData diamond() {
        return new IconData(FontImage.MATERIAL_DIAMOND);
    }
    public static IconData difference() {
        return new IconData(FontImage.MATERIAL_DIFFERENCE);
    }
    public static IconData dining() {
        return new IconData(FontImage.MATERIAL_DINING);
    }
    public static IconData dinner_dining() {
        return new IconData(FontImage.MATERIAL_DINNER_DINING);
    }
    public static IconData directions() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS);
    }
    public static IconData directions_boat() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_BOAT);
    }
    public static IconData directions_boat_filled() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_BOAT_FILLED);
    }
    public static IconData directions_bus() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_BUS);
    }
    public static IconData directions_bus_filled() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_BUS_FILLED);
    }
    public static IconData directions_car() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_CAR);
    }
    public static IconData directions_car_filled() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_CAR_FILLED);
    }
    public static IconData directions_ferry() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_FERRY);
    }
    public static IconData directions_off() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_OFF);
    }
    public static IconData directions_railway() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_RAILWAY);
    }
    public static IconData directions_railway_filled() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_RAILWAY_FILLED);
    }
    public static IconData directions_run() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_RUN);
    }
    public static IconData directions_subway() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_SUBWAY);
    }
    public static IconData directions_subway_filled() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_SUBWAY_FILLED);
    }
    public static IconData directions_train() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_TRAIN);
    }
    public static IconData directions_transit() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_TRANSIT);
    }
    public static IconData directions_transit_filled() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_TRANSIT_FILLED);
    }
    public static IconData directions_walk() {
        return new IconData(FontImage.MATERIAL_DIRECTIONS_WALK);
    }
    public static IconData dirty_lens() {
        return new IconData(FontImage.MATERIAL_DIRTY_LENS);
    }
    public static IconData disabled_by_default() {
        return new IconData(FontImage.MATERIAL_DISABLED_BY_DEFAULT);
    }
    public static IconData disabled_visible() {
        return new IconData(FontImage.MATERIAL_DISABLED_VISIBLE);
    }
    public static IconData disc_full() {
        return new IconData(FontImage.MATERIAL_DISC_FULL);
    }
    public static IconData discord() {
        return new IconData(FontImage.MATERIAL_DISCORD);
    }
    public static IconData discount() {
        return new IconData(FontImage.MATERIAL_DISCOUNT);
    }
    public static IconData display_settings() {
        return new IconData(FontImage.MATERIAL_DISPLAY_SETTINGS);
    }
    public static IconData diversity_1() {
        return new IconData(FontImage.MATERIAL_DIVERSITY_1);
    }
    public static IconData diversity_2() {
        return new IconData(FontImage.MATERIAL_DIVERSITY_2);
    }
    public static IconData diversity_3() {
        return new IconData(FontImage.MATERIAL_DIVERSITY_3);
    }
    public static IconData dnd_forwardslash() {
        return new IconData(FontImage.MATERIAL_DND_FORWARDSLASH);
    }
    public static IconData dns() {
        return new IconData(FontImage.MATERIAL_DNS);
    }
    public static IconData do_disturb() {
        return new IconData(FontImage.MATERIAL_DO_DISTURB);
    }
    public static IconData do_disturb_alt() {
        return new IconData(FontImage.MATERIAL_DO_DISTURB_ALT);
    }
    public static IconData do_disturb_off() {
        return new IconData(FontImage.MATERIAL_DO_DISTURB_OFF);
    }
    public static IconData do_disturb_on() {
        return new IconData(FontImage.MATERIAL_DO_DISTURB_ON);
    }
    public static IconData do_not_disturb() {
        return new IconData(FontImage.MATERIAL_DO_NOT_DISTURB);
    }
    public static IconData do_not_disturb_alt() {
        return new IconData(FontImage.MATERIAL_DO_NOT_DISTURB_ALT);
    }
    public static IconData do_not_disturb_off() {
        return new IconData(FontImage.MATERIAL_DO_NOT_DISTURB_OFF);
    }
    public static IconData do_not_disturb_on() {
        return new IconData(FontImage.MATERIAL_DO_NOT_DISTURB_ON);
    }
    public static IconData do_not_disturb_on_total_silence() {
        return new IconData(FontImage.MATERIAL_DO_NOT_DISTURB_ON_TOTAL_SILENCE);
    }
    public static IconData do_not_step() {
        return new IconData(FontImage.MATERIAL_DO_NOT_STEP);
    }
    public static IconData do_not_touch() {
        return new IconData(FontImage.MATERIAL_DO_NOT_TOUCH);
    }
    public static IconData dock() {
        return new IconData(FontImage.MATERIAL_DOCK);
    }
    public static IconData document_scanner() {
        return new IconData(FontImage.MATERIAL_DOCUMENT_SCANNER);
    }
    public static IconData domain() {
        return new IconData(FontImage.MATERIAL_DOMAIN);
    }
    public static IconData domain_add() {
        return new IconData(FontImage.MATERIAL_DOMAIN_ADD);
    }
    public static IconData domain_disabled() {
        return new IconData(FontImage.MATERIAL_DOMAIN_DISABLED);
    }
    public static IconData domain_verification() {
        return new IconData(FontImage.MATERIAL_DOMAIN_VERIFICATION);
    }
    public static IconData done() {
        return new IconData(FontImage.MATERIAL_DONE);
    }
    public static IconData done_all() {
        return new IconData(FontImage.MATERIAL_DONE_ALL);
    }
    public static IconData done_outline() {
        return new IconData(FontImage.MATERIAL_DONE_OUTLINE);
    }
    public static IconData donut_large() {
        return new IconData(FontImage.MATERIAL_DONUT_LARGE);
    }
    public static IconData donut_small() {
        return new IconData(FontImage.MATERIAL_DONUT_SMALL);
    }
    public static IconData door_back() {
        return new IconData(FontImage.MATERIAL_DOOR_BACK);
    }
    public static IconData door_front() {
        return new IconData(FontImage.MATERIAL_DOOR_FRONT);
    }
    public static IconData door_sliding() {
        return new IconData(FontImage.MATERIAL_DOOR_SLIDING);
    }
    public static IconData doorbell() {
        return new IconData(FontImage.MATERIAL_DOORBELL);
    }
    public static IconData double_arrow() {
        return new IconData(FontImage.MATERIAL_DOUBLE_ARROW);
    }
    public static IconData downhill_skiing() {
        return new IconData(FontImage.MATERIAL_DOWNHILL_SKIING);
    }
    public static IconData download() {
        return new IconData(FontImage.MATERIAL_DOWNLOAD);
    }
    public static IconData download_done() {
        return new IconData(FontImage.MATERIAL_DOWNLOAD_DONE);
    }
    public static IconData download_for_offline() {
        return new IconData(FontImage.MATERIAL_DOWNLOAD_FOR_OFFLINE);
    }
    public static IconData downloading() {
        return new IconData(FontImage.MATERIAL_DOWNLOADING);
    }
    public static IconData drafts() {
        return new IconData(FontImage.MATERIAL_DRAFTS);
    }
    public static IconData drag_handle() {
        return new IconData(FontImage.MATERIAL_DRAG_HANDLE);
    }
    public static IconData drag_indicator() {
        return new IconData(FontImage.MATERIAL_DRAG_INDICATOR);
    }
    public static IconData draw() {
        return new IconData(FontImage.MATERIAL_DRAW);
    }
    public static IconData drive_eta() {
        return new IconData(FontImage.MATERIAL_DRIVE_ETA);
    }
    public static IconData drive_file_move() {
        return new IconData(FontImage.MATERIAL_DRIVE_FILE_MOVE);
    }
    public static IconData drive_file_move_outline() {
        return new IconData(FontImage.MATERIAL_DRIVE_FILE_MOVE_OUTLINE);
    }
    public static IconData drive_file_move_rtl() {
        return new IconData(FontImage.MATERIAL_DRIVE_FILE_MOVE_RTL);
    }
    public static IconData drive_file_rename_outline() {
        return new IconData(FontImage.MATERIAL_DRIVE_FILE_RENAME_OUTLINE);
    }
    public static IconData drive_folder_upload() {
        return new IconData(FontImage.MATERIAL_DRIVE_FOLDER_UPLOAD);
    }
    public static IconData dry() {
        return new IconData(FontImage.MATERIAL_DRY);
    }
    public static IconData dry_cleaning() {
        return new IconData(FontImage.MATERIAL_DRY_CLEANING);
    }
    public static IconData duo() {
        return new IconData(FontImage.MATERIAL_DUO);
    }
    public static IconData dvr() {
        return new IconData(FontImage.MATERIAL_DVR);
    }
    public static IconData dynamic_feed() {
        return new IconData(FontImage.MATERIAL_DYNAMIC_FEED);
    }
    public static IconData dynamic_form() {
        return new IconData(FontImage.MATERIAL_DYNAMIC_FORM);
    }
    public static IconData e_mobiledata() {
        return new IconData(FontImage.MATERIAL_E_MOBILEDATA);
    }
    public static IconData earbuds() {
        return new IconData(FontImage.MATERIAL_EARBUDS);
    }
    public static IconData earbuds_battery() {
        return new IconData(FontImage.MATERIAL_EARBUDS_BATTERY);
    }
    public static IconData east() {
        return new IconData(FontImage.MATERIAL_EAST);
    }
    public static IconData eco() {
        return new IconData(FontImage.MATERIAL_ECO);
    }
    public static IconData edgesensor_high() {
        return new IconData(FontImage.MATERIAL_EDGESENSOR_HIGH);
    }
    public static IconData edgesensor_low() {
        return new IconData(FontImage.MATERIAL_EDGESENSOR_LOW);
    }
    public static IconData edit_attributes() {
        return new IconData(FontImage.MATERIAL_EDIT_ATTRIBUTES);
    }
    public static IconData edit_calendar() {
        return new IconData(FontImage.MATERIAL_EDIT_CALENDAR);
    }
    public static IconData edit_document() {
        return new IconData(FontImage.MATERIAL_EDIT_DOCUMENT);
    }
    public static IconData edit_location() {
        return new IconData(FontImage.MATERIAL_EDIT_LOCATION);
    }
    public static IconData edit_location_alt() {
        return new IconData(FontImage.MATERIAL_EDIT_LOCATION_ALT);
    }
    public static IconData edit_note() {
        return new IconData(FontImage.MATERIAL_EDIT_NOTE);
    }
    public static IconData edit_notifications() {
        return new IconData(FontImage.MATERIAL_EDIT_NOTIFICATIONS);
    }
    public static IconData edit_off() {
        return new IconData(FontImage.MATERIAL_EDIT_OFF);
    }
    public static IconData edit_road() {
        return new IconData(FontImage.MATERIAL_EDIT_ROAD);
    }
    public static IconData edit_square() {
        return new IconData(FontImage.MATERIAL_EDIT_SQUARE);
    }
    public static IconData egg() {
        return new IconData(FontImage.MATERIAL_EGG);
    }
    public static IconData egg_alt() {
        return new IconData(FontImage.MATERIAL_EGG_ALT);
    }
    public static IconData eject() {
        return new IconData(FontImage.MATERIAL_EJECT);
    }
    public static IconData elderly() {
        return new IconData(FontImage.MATERIAL_ELDERLY);
    }
    public static IconData elderly_woman() {
        return new IconData(FontImage.MATERIAL_ELDERLY_WOMAN);
    }
    public static IconData electric_bike() {
        return new IconData(FontImage.MATERIAL_ELECTRIC_BIKE);
    }
    public static IconData electric_bolt() {
        return new IconData(FontImage.MATERIAL_ELECTRIC_BOLT);
    }
    public static IconData electric_car() {
        return new IconData(FontImage.MATERIAL_ELECTRIC_CAR);
    }
    public static IconData electric_meter() {
        return new IconData(FontImage.MATERIAL_ELECTRIC_METER);
    }
    public static IconData electric_moped() {
        return new IconData(FontImage.MATERIAL_ELECTRIC_MOPED);
    }
    public static IconData electric_rickshaw() {
        return new IconData(FontImage.MATERIAL_ELECTRIC_RICKSHAW);
    }
    public static IconData electric_scooter() {
        return new IconData(FontImage.MATERIAL_ELECTRIC_SCOOTER);
    }
    public static IconData electrical_services() {
        return new IconData(FontImage.MATERIAL_ELECTRICAL_SERVICES);
    }
    public static IconData elevator() {
        return new IconData(FontImage.MATERIAL_ELEVATOR);
    }
    public static IconData emergency() {
        return new IconData(FontImage.MATERIAL_EMERGENCY);
    }
    public static IconData emergency_recording() {
        return new IconData(FontImage.MATERIAL_EMERGENCY_RECORDING);
    }
    public static IconData emergency_share() {
        return new IconData(FontImage.MATERIAL_EMERGENCY_SHARE);
    }
    public static IconData emoji_emotions() {
        return new IconData(FontImage.MATERIAL_EMOJI_EMOTIONS);
    }
    public static IconData emoji_events() {
        return new IconData(FontImage.MATERIAL_EMOJI_EVENTS);
    }
    public static IconData emoji_flags() {
        return new IconData(FontImage.MATERIAL_EMOJI_FLAGS);
    }
    public static IconData emoji_food_beverage() {
        return new IconData(FontImage.MATERIAL_EMOJI_FOOD_BEVERAGE);
    }
    public static IconData emoji_nature() {
        return new IconData(FontImage.MATERIAL_EMOJI_NATURE);
    }
    public static IconData emoji_objects() {
        return new IconData(FontImage.MATERIAL_EMOJI_OBJECTS);
    }
    public static IconData emoji_people() {
        return new IconData(FontImage.MATERIAL_EMOJI_PEOPLE);
    }
    public static IconData emoji_symbols() {
        return new IconData(FontImage.MATERIAL_EMOJI_SYMBOLS);
    }
    public static IconData emoji_transportation() {
        return new IconData(FontImage.MATERIAL_EMOJI_TRANSPORTATION);
    }
    public static IconData energy_savings_leaf() {
        return new IconData(FontImage.MATERIAL_ENERGY_SAVINGS_LEAF);
    }
    public static IconData engineering() {
        return new IconData(FontImage.MATERIAL_ENGINEERING);
    }
    public static IconData enhance_photo_translate() {
        return new IconData(FontImage.MATERIAL_ENHANCE_PHOTO_TRANSLATE);
    }
    public static IconData enhanced_encryption() {
        return new IconData(FontImage.MATERIAL_ENHANCED_ENCRYPTION);
    }
    public static IconData equalizer() {
        return new IconData(FontImage.MATERIAL_EQUALIZER);
    }
    public static IconData error() {
        return new IconData(FontImage.MATERIAL_ERROR);
    }
    public static IconData error_outline() {
        return new IconData(FontImage.MATERIAL_ERROR_OUTLINE);
    }
    public static IconData escalator() {
        return new IconData(FontImage.MATERIAL_ESCALATOR);
    }
    public static IconData escalator_warning() {
        return new IconData(FontImage.MATERIAL_ESCALATOR_WARNING);
    }
    public static IconData euro() {
        return new IconData(FontImage.MATERIAL_EURO);
    }
    public static IconData euro_symbol() {
        return new IconData(FontImage.MATERIAL_EURO_SYMBOL);
    }
    public static IconData ev_station() {
        return new IconData(FontImage.MATERIAL_EV_STATION);
    }
    public static IconData event() {
        return new IconData(FontImage.MATERIAL_EVENT);
    }
    public static IconData event_available() {
        return new IconData(FontImage.MATERIAL_EVENT_AVAILABLE);
    }
    public static IconData event_busy() {
        return new IconData(FontImage.MATERIAL_EVENT_BUSY);
    }
    public static IconData event_note() {
        return new IconData(FontImage.MATERIAL_EVENT_NOTE);
    }
    public static IconData event_repeat() {
        return new IconData(FontImage.MATERIAL_EVENT_REPEAT);
    }
    public static IconData event_seat() {
        return new IconData(FontImage.MATERIAL_EVENT_SEAT);
    }
    public static IconData exit_to_app() {
        return new IconData(FontImage.MATERIAL_EXIT_TO_APP);
    }
    public static IconData expand() {
        return new IconData(FontImage.MATERIAL_EXPAND);
    }
    public static IconData expand_circle_down() {
        return new IconData(FontImage.MATERIAL_EXPAND_CIRCLE_DOWN);
    }
    public static IconData expand_less() {
        return new IconData(FontImage.MATERIAL_EXPAND_LESS);
    }
    public static IconData expand_more() {
        return new IconData(FontImage.MATERIAL_EXPAND_MORE);
    }
    public static IconData explicit() {
        return new IconData(FontImage.MATERIAL_EXPLICIT);
    }
    public static IconData explore() {
        return new IconData(FontImage.MATERIAL_EXPLORE);
    }
    public static IconData explore_off() {
        return new IconData(FontImage.MATERIAL_EXPLORE_OFF);
    }
    public static IconData exposure() {
        return new IconData(FontImage.MATERIAL_EXPOSURE);
    }
    public static IconData exposure_minus_1() {
        return new IconData(FontImage.MATERIAL_EXPOSURE_MINUS_1);
    }
    public static IconData exposure_minus_2() {
        return new IconData(FontImage.MATERIAL_EXPOSURE_MINUS_2);
    }
    public static IconData exposure_neg_1() {
        return new IconData(FontImage.MATERIAL_EXPOSURE_NEG_1);
    }
    public static IconData exposure_neg_2() {
        return new IconData(FontImage.MATERIAL_EXPOSURE_NEG_2);
    }
    public static IconData exposure_plus_1() {
        return new IconData(FontImage.MATERIAL_EXPOSURE_PLUS_1);
    }
    public static IconData exposure_plus_2() {
        return new IconData(FontImage.MATERIAL_EXPOSURE_PLUS_2);
    }
    public static IconData exposure_zero() {
        return new IconData(FontImage.MATERIAL_EXPOSURE_ZERO);
    }
    public static IconData extension() {
        return new IconData(FontImage.MATERIAL_EXTENSION);
    }
    public static IconData extension_off() {
        return new IconData(FontImage.MATERIAL_EXTENSION_OFF);
    }
    public static IconData face() {
        return new IconData(FontImage.MATERIAL_FACE);
    }
    public static IconData face_2() {
        return new IconData(FontImage.MATERIAL_FACE_2);
    }
    public static IconData face_3() {
        return new IconData(FontImage.MATERIAL_FACE_3);
    }
    public static IconData face_4() {
        return new IconData(FontImage.MATERIAL_FACE_4);
    }
    public static IconData face_5() {
        return new IconData(FontImage.MATERIAL_FACE_5);
    }
    public static IconData face_6() {
        return new IconData(FontImage.MATERIAL_FACE_6);
    }
    public static IconData face_retouching_natural() {
        return new IconData(FontImage.MATERIAL_FACE_RETOUCHING_NATURAL);
    }
    public static IconData face_retouching_off() {
        return new IconData(FontImage.MATERIAL_FACE_RETOUCHING_OFF);
    }
    public static IconData facebook() {
        return new IconData(FontImage.MATERIAL_FACEBOOK);
    }
    public static IconData fact_check() {
        return new IconData(FontImage.MATERIAL_FACT_CHECK);
    }
    public static IconData factory() {
        return new IconData(FontImage.MATERIAL_FACTORY);
    }
    public static IconData family_restroom() {
        return new IconData(FontImage.MATERIAL_FAMILY_RESTROOM);
    }
    public static IconData fast_forward() {
        return new IconData(FontImage.MATERIAL_FAST_FORWARD);
    }
    public static IconData fast_rewind() {
        return new IconData(FontImage.MATERIAL_FAST_REWIND);
    }
    public static IconData fastfood() {
        return new IconData(FontImage.MATERIAL_FASTFOOD);
    }
    public static IconData favorite_outline() {
        return new IconData(FontImage.MATERIAL_FAVORITE_OUTLINE);
    }
    public static IconData fax() {
        return new IconData(FontImage.MATERIAL_FAX);
    }
    public static IconData featured_play_list() {
        return new IconData(FontImage.MATERIAL_FEATURED_PLAY_LIST);
    }
    public static IconData featured_video() {
        return new IconData(FontImage.MATERIAL_FEATURED_VIDEO);
    }
    public static IconData feed() {
        return new IconData(FontImage.MATERIAL_FEED);
    }
    public static IconData female() {
        return new IconData(FontImage.MATERIAL_FEMALE);
    }
    public static IconData fence() {
        return new IconData(FontImage.MATERIAL_FENCE);
    }
    public static IconData festival() {
        return new IconData(FontImage.MATERIAL_FESTIVAL);
    }
    public static IconData fiber_dvr() {
        return new IconData(FontImage.MATERIAL_FIBER_DVR);
    }
    public static IconData fiber_manual_record() {
        return new IconData(FontImage.MATERIAL_FIBER_MANUAL_RECORD);
    }
    public static IconData fiber_new() {
        return new IconData(FontImage.MATERIAL_FIBER_NEW);
    }
    public static IconData fiber_pin() {
        return new IconData(FontImage.MATERIAL_FIBER_PIN);
    }
    public static IconData fiber_smart_record() {
        return new IconData(FontImage.MATERIAL_FIBER_SMART_RECORD);
    }
    public static IconData file_copy() {
        return new IconData(FontImage.MATERIAL_FILE_COPY);
    }
    public static IconData file_download() {
        return new IconData(FontImage.MATERIAL_FILE_DOWNLOAD);
    }
    public static IconData file_download_done() {
        return new IconData(FontImage.MATERIAL_FILE_DOWNLOAD_DONE);
    }
    public static IconData file_download_off() {
        return new IconData(FontImage.MATERIAL_FILE_DOWNLOAD_OFF);
    }
    public static IconData file_open() {
        return new IconData(FontImage.MATERIAL_FILE_OPEN);
    }
    public static IconData file_present() {
        return new IconData(FontImage.MATERIAL_FILE_PRESENT);
    }
    public static IconData file_upload() {
        return new IconData(FontImage.MATERIAL_FILE_UPLOAD);
    }
    public static IconData file_upload_off() {
        return new IconData(FontImage.MATERIAL_FILE_UPLOAD_OFF);
    }
    public static IconData filter() {
        return new IconData(FontImage.MATERIAL_FILTER);
    }
    public static IconData filter_1() {
        return new IconData(FontImage.MATERIAL_FILTER_1);
    }
    public static IconData filter_2() {
        return new IconData(FontImage.MATERIAL_FILTER_2);
    }
    public static IconData filter_3() {
        return new IconData(FontImage.MATERIAL_FILTER_3);
    }
    public static IconData filter_4() {
        return new IconData(FontImage.MATERIAL_FILTER_4);
    }
    public static IconData filter_5() {
        return new IconData(FontImage.MATERIAL_FILTER_5);
    }
    public static IconData filter_6() {
        return new IconData(FontImage.MATERIAL_FILTER_6);
    }
    public static IconData filter_7() {
        return new IconData(FontImage.MATERIAL_FILTER_7);
    }
    public static IconData filter_8() {
        return new IconData(FontImage.MATERIAL_FILTER_8);
    }
    public static IconData filter_9() {
        return new IconData(FontImage.MATERIAL_FILTER_9);
    }
    public static IconData filter_9_plus() {
        return new IconData(FontImage.MATERIAL_FILTER_9_PLUS);
    }
    public static IconData filter_alt() {
        return new IconData(FontImage.MATERIAL_FILTER_ALT);
    }
    public static IconData filter_alt_off() {
        return new IconData(FontImage.MATERIAL_FILTER_ALT_OFF);
    }
    public static IconData filter_b_and_w() {
        return new IconData(FontImage.MATERIAL_FILTER_B_AND_W);
    }
    public static IconData filter_center_focus() {
        return new IconData(FontImage.MATERIAL_FILTER_CENTER_FOCUS);
    }
    public static IconData filter_drama() {
        return new IconData(FontImage.MATERIAL_FILTER_DRAMA);
    }
    public static IconData filter_frames() {
        return new IconData(FontImage.MATERIAL_FILTER_FRAMES);
    }
    public static IconData filter_hdr() {
        return new IconData(FontImage.MATERIAL_FILTER_HDR);
    }
    public static IconData filter_list() {
        return new IconData(FontImage.MATERIAL_FILTER_LIST);
    }
    public static IconData filter_list_alt() {
        return new IconData(FontImage.MATERIAL_FILTER_LIST_ALT);
    }
    public static IconData filter_list_off() {
        return new IconData(FontImage.MATERIAL_FILTER_LIST_OFF);
    }
    public static IconData filter_none() {
        return new IconData(FontImage.MATERIAL_FILTER_NONE);
    }
    public static IconData filter_tilt_shift() {
        return new IconData(FontImage.MATERIAL_FILTER_TILT_SHIFT);
    }
    public static IconData filter_vintage() {
        return new IconData(FontImage.MATERIAL_FILTER_VINTAGE);
    }
    public static IconData find_in_page() {
        return new IconData(FontImage.MATERIAL_FIND_IN_PAGE);
    }
    public static IconData find_replace() {
        return new IconData(FontImage.MATERIAL_FIND_REPLACE);
    }
    public static IconData fingerprint() {
        return new IconData(FontImage.MATERIAL_FINGERPRINT);
    }
    public static IconData fire_extinguisher() {
        return new IconData(FontImage.MATERIAL_FIRE_EXTINGUISHER);
    }
    public static IconData fire_hydrant() {
        return new IconData(FontImage.MATERIAL_FIRE_HYDRANT);
    }
    public static IconData fire_hydrant_alt() {
        return new IconData(FontImage.MATERIAL_FIRE_HYDRANT_ALT);
    }
    public static IconData fire_truck() {
        return new IconData(FontImage.MATERIAL_FIRE_TRUCK);
    }
    public static IconData fireplace() {
        return new IconData(FontImage.MATERIAL_FIREPLACE);
    }
    public static IconData first_page() {
        return new IconData(FontImage.MATERIAL_FIRST_PAGE);
    }
    public static IconData fit_screen() {
        return new IconData(FontImage.MATERIAL_FIT_SCREEN);
    }
    public static IconData fitbit() {
        return new IconData(FontImage.MATERIAL_FITBIT);
    }
    public static IconData fitness_center() {
        return new IconData(FontImage.MATERIAL_FITNESS_CENTER);
    }
    public static IconData flag() {
        return new IconData(FontImage.MATERIAL_FLAG);
    }
    public static IconData flag_circle() {
        return new IconData(FontImage.MATERIAL_FLAG_CIRCLE);
    }
    public static IconData flaky() {
        return new IconData(FontImage.MATERIAL_FLAKY);
    }
    public static IconData flare() {
        return new IconData(FontImage.MATERIAL_FLARE);
    }
    public static IconData flash_auto() {
        return new IconData(FontImage.MATERIAL_FLASH_AUTO);
    }
    public static IconData flash_off() {
        return new IconData(FontImage.MATERIAL_FLASH_OFF);
    }
    public static IconData flash_on() {
        return new IconData(FontImage.MATERIAL_FLASH_ON);
    }
    public static IconData flashlight_off() {
        return new IconData(FontImage.MATERIAL_FLASHLIGHT_OFF);
    }
    public static IconData flashlight_on() {
        return new IconData(FontImage.MATERIAL_FLASHLIGHT_ON);
    }
    public static IconData flatware() {
        return new IconData(FontImage.MATERIAL_FLATWARE);
    }
    public static IconData flight() {
        return new IconData(FontImage.MATERIAL_FLIGHT);
    }
    public static IconData flight_class() {
        return new IconData(FontImage.MATERIAL_FLIGHT_CLASS);
    }
    public static IconData flight_land() {
        return new IconData(FontImage.MATERIAL_FLIGHT_LAND);
    }
    public static IconData flight_takeoff() {
        return new IconData(FontImage.MATERIAL_FLIGHT_TAKEOFF);
    }
    public static IconData flip() {
        return new IconData(FontImage.MATERIAL_FLIP);
    }
    public static IconData flip_camera_android() {
        return new IconData(FontImage.MATERIAL_FLIP_CAMERA_ANDROID);
    }
    public static IconData flip_camera_ios() {
        return new IconData(FontImage.MATERIAL_FLIP_CAMERA_IOS);
    }
    public static IconData flip_to_back() {
        return new IconData(FontImage.MATERIAL_FLIP_TO_BACK);
    }
    public static IconData flip_to_front() {
        return new IconData(FontImage.MATERIAL_FLIP_TO_FRONT);
    }
    public static IconData flood() {
        return new IconData(FontImage.MATERIAL_FLOOD);
    }
    public static IconData flourescent() {
        return new IconData(FontImage.MATERIAL_FLOURESCENT);
    }
    public static IconData fluorescent() {
        return new IconData(FontImage.MATERIAL_FLUORESCENT);
    }
    public static IconData flutter_dash() {
        return new IconData(FontImage.MATERIAL_FLUTTER_DASH);
    }
    public static IconData fmd_bad() {
        return new IconData(FontImage.MATERIAL_FMD_BAD);
    }
    public static IconData fmd_good() {
        return new IconData(FontImage.MATERIAL_FMD_GOOD);
    }
    public static IconData foggy() {
        return new IconData(FontImage.MATERIAL_FOGGY);
    }
    public static IconData folder() {
        return new IconData(FontImage.MATERIAL_FOLDER);
    }
    public static IconData folder_copy() {
        return new IconData(FontImage.MATERIAL_FOLDER_COPY);
    }
    public static IconData folder_delete() {
        return new IconData(FontImage.MATERIAL_FOLDER_DELETE);
    }
    public static IconData folder_off() {
        return new IconData(FontImage.MATERIAL_FOLDER_OFF);
    }
    public static IconData folder_open() {
        return new IconData(FontImage.MATERIAL_FOLDER_OPEN);
    }
    public static IconData folder_shared() {
        return new IconData(FontImage.MATERIAL_FOLDER_SHARED);
    }
    public static IconData folder_special() {
        return new IconData(FontImage.MATERIAL_FOLDER_SPECIAL);
    }
    public static IconData folder_zip() {
        return new IconData(FontImage.MATERIAL_FOLDER_ZIP);
    }
    public static IconData follow_the_signs() {
        return new IconData(FontImage.MATERIAL_FOLLOW_THE_SIGNS);
    }
    public static IconData font_download() {
        return new IconData(FontImage.MATERIAL_FONT_DOWNLOAD);
    }
    public static IconData font_download_off() {
        return new IconData(FontImage.MATERIAL_FONT_DOWNLOAD_OFF);
    }
    public static IconData food_bank() {
        return new IconData(FontImage.MATERIAL_FOOD_BANK);
    }
    public static IconData forest() {
        return new IconData(FontImage.MATERIAL_FOREST);
    }
    public static IconData fork_left() {
        return new IconData(FontImage.MATERIAL_FORK_LEFT);
    }
    public static IconData fork_right() {
        return new IconData(FontImage.MATERIAL_FORK_RIGHT);
    }
    public static IconData forklift() {
        return new IconData(FontImage.MATERIAL_FORKLIFT);
    }
    public static IconData format_align_center() {
        return new IconData(FontImage.MATERIAL_FORMAT_ALIGN_CENTER);
    }
    public static IconData format_align_justify() {
        return new IconData(FontImage.MATERIAL_FORMAT_ALIGN_JUSTIFY);
    }
    public static IconData format_align_left() {
        return new IconData(FontImage.MATERIAL_FORMAT_ALIGN_LEFT);
    }
    public static IconData format_align_right() {
        return new IconData(FontImage.MATERIAL_FORMAT_ALIGN_RIGHT);
    }
    public static IconData format_clear() {
        return new IconData(FontImage.MATERIAL_FORMAT_CLEAR);
    }
    public static IconData format_color_fill() {
        return new IconData(FontImage.MATERIAL_FORMAT_COLOR_FILL);
    }
    public static IconData format_color_reset() {
        return new IconData(FontImage.MATERIAL_FORMAT_COLOR_RESET);
    }
    public static IconData format_color_text() {
        return new IconData(FontImage.MATERIAL_FORMAT_COLOR_TEXT);
    }
    public static IconData format_indent_decrease() {
        return new IconData(FontImage.MATERIAL_FORMAT_INDENT_DECREASE);
    }
    public static IconData format_indent_increase() {
        return new IconData(FontImage.MATERIAL_FORMAT_INDENT_INCREASE);
    }
    public static IconData format_line_spacing() {
        return new IconData(FontImage.MATERIAL_FORMAT_LINE_SPACING);
    }
    public static IconData format_list_bulleted() {
        return new IconData(FontImage.MATERIAL_FORMAT_LIST_BULLETED);
    }
    public static IconData format_list_bulleted_add() {
        return new IconData(FontImage.MATERIAL_FORMAT_LIST_BULLETED_ADD);
    }
    public static IconData format_list_numbered() {
        return new IconData(FontImage.MATERIAL_FORMAT_LIST_NUMBERED);
    }
    public static IconData format_list_numbered_rtl() {
        return new IconData(FontImage.MATERIAL_FORMAT_LIST_NUMBERED_RTL);
    }
    public static IconData format_overline() {
        return new IconData(FontImage.MATERIAL_FORMAT_OVERLINE);
    }
    public static IconData format_paint() {
        return new IconData(FontImage.MATERIAL_FORMAT_PAINT);
    }
    public static IconData format_quote() {
        return new IconData(FontImage.MATERIAL_FORMAT_QUOTE);
    }
    public static IconData format_shapes() {
        return new IconData(FontImage.MATERIAL_FORMAT_SHAPES);
    }
    public static IconData format_size() {
        return new IconData(FontImage.MATERIAL_FORMAT_SIZE);
    }
    public static IconData format_strikethrough() {
        return new IconData(FontImage.MATERIAL_FORMAT_STRIKETHROUGH);
    }
    public static IconData format_textdirection_l_to_r() {
        return new IconData(FontImage.MATERIAL_FORMAT_TEXTDIRECTION_L_TO_R);
    }
    public static IconData format_textdirection_r_to_l() {
        return new IconData(FontImage.MATERIAL_FORMAT_TEXTDIRECTION_R_TO_L);
    }
    public static IconData format_underlined() {
        return new IconData(FontImage.MATERIAL_FORMAT_UNDERLINED);
    }
    public static IconData fort() {
        return new IconData(FontImage.MATERIAL_FORT);
    }
    public static IconData forum() {
        return new IconData(FontImage.MATERIAL_FORUM);
    }
    public static IconData forward() {
        return new IconData(FontImage.MATERIAL_FORWARD);
    }
    public static IconData forward_10() {
        return new IconData(FontImage.MATERIAL_FORWARD_10);
    }
    public static IconData forward_30() {
        return new IconData(FontImage.MATERIAL_FORWARD_30);
    }
    public static IconData forward_5() {
        return new IconData(FontImage.MATERIAL_FORWARD_5);
    }
    public static IconData forward_to_inbox() {
        return new IconData(FontImage.MATERIAL_FORWARD_TO_INBOX);
    }
    public static IconData foundation() {
        return new IconData(FontImage.MATERIAL_FOUNDATION);
    }
    public static IconData free_breakfast() {
        return new IconData(FontImage.MATERIAL_FREE_BREAKFAST);
    }
    public static IconData free_cancellation() {
        return new IconData(FontImage.MATERIAL_FREE_CANCELLATION);
    }
    public static IconData front_hand() {
        return new IconData(FontImage.MATERIAL_FRONT_HAND);
    }
    public static IconData front_loader() {
        return new IconData(FontImage.MATERIAL_FRONT_LOADER);
    }
    public static IconData fullscreen_exit() {
        return new IconData(FontImage.MATERIAL_FULLSCREEN_EXIT);
    }
    public static IconData functions() {
        return new IconData(FontImage.MATERIAL_FUNCTIONS);
    }
    public static IconData g_mobiledata() {
        return new IconData(FontImage.MATERIAL_G_MOBILEDATA);
    }
    public static IconData g_translate() {
        return new IconData(FontImage.MATERIAL_G_TRANSLATE);
    }
    public static IconData gamepad() {
        return new IconData(FontImage.MATERIAL_GAMEPAD);
    }
    public static IconData games() {
        return new IconData(FontImage.MATERIAL_GAMES);
    }
    public static IconData garage() {
        return new IconData(FontImage.MATERIAL_GARAGE);
    }
    public static IconData gas_meter() {
        return new IconData(FontImage.MATERIAL_GAS_METER);
    }
    public static IconData gavel() {
        return new IconData(FontImage.MATERIAL_GAVEL);
    }
    public static IconData generating_tokens() {
        return new IconData(FontImage.MATERIAL_GENERATING_TOKENS);
    }
    public static IconData gesture() {
        return new IconData(FontImage.MATERIAL_GESTURE);
    }
    public static IconData get_app() {
        return new IconData(FontImage.MATERIAL_GET_APP);
    }
    public static IconData gif() {
        return new IconData(FontImage.MATERIAL_GIF);
    }
    public static IconData gif_box() {
        return new IconData(FontImage.MATERIAL_GIF_BOX);
    }
    public static IconData girl() {
        return new IconData(FontImage.MATERIAL_GIRL);
    }
    public static IconData gite() {
        return new IconData(FontImage.MATERIAL_GITE);
    }
    public static IconData golf_course() {
        return new IconData(FontImage.MATERIAL_GOLF_COURSE);
    }
    public static IconData gpp_bad() {
        return new IconData(FontImage.MATERIAL_GPP_BAD);
    }
    public static IconData gpp_good() {
        return new IconData(FontImage.MATERIAL_GPP_GOOD);
    }
    public static IconData gpp_maybe() {
        return new IconData(FontImage.MATERIAL_GPP_MAYBE);
    }
    public static IconData gps_fixed() {
        return new IconData(FontImage.MATERIAL_GPS_FIXED);
    }
    public static IconData gps_not_fixed() {
        return new IconData(FontImage.MATERIAL_GPS_NOT_FIXED);
    }
    public static IconData gps_off() {
        return new IconData(FontImage.MATERIAL_GPS_OFF);
    }
    public static IconData grade() {
        return new IconData(FontImage.MATERIAL_GRADE);
    }
    public static IconData gradient() {
        return new IconData(FontImage.MATERIAL_GRADIENT);
    }
    public static IconData grading() {
        return new IconData(FontImage.MATERIAL_GRADING);
    }
    public static IconData grain() {
        return new IconData(FontImage.MATERIAL_GRAIN);
    }
    public static IconData graphic_eq() {
        return new IconData(FontImage.MATERIAL_GRAPHIC_EQ);
    }
    public static IconData grass() {
        return new IconData(FontImage.MATERIAL_GRASS);
    }
    public static IconData grid_3x3() {
        return new IconData(FontImage.MATERIAL_GRID_3X3);
    }
    public static IconData grid_4x4() {
        return new IconData(FontImage.MATERIAL_GRID_4X4);
    }
    public static IconData grid_goldenratio() {
        return new IconData(FontImage.MATERIAL_GRID_GOLDENRATIO);
    }
    public static IconData grid_off() {
        return new IconData(FontImage.MATERIAL_GRID_OFF);
    }
    public static IconData grid_on() {
        return new IconData(FontImage.MATERIAL_GRID_ON);
    }
    public static IconData grid_view() {
        return new IconData(FontImage.MATERIAL_GRID_VIEW);
    }
    public static IconData group() {
        return new IconData(FontImage.MATERIAL_GROUP);
    }
    public static IconData group_add() {
        return new IconData(FontImage.MATERIAL_GROUP_ADD);
    }
    public static IconData group_off() {
        return new IconData(FontImage.MATERIAL_GROUP_OFF);
    }
    public static IconData group_remove() {
        return new IconData(FontImage.MATERIAL_GROUP_REMOVE);
    }
    public static IconData group_work() {
        return new IconData(FontImage.MATERIAL_GROUP_WORK);
    }
    public static IconData groups() {
        return new IconData(FontImage.MATERIAL_GROUPS);
    }
    public static IconData groups_2() {
        return new IconData(FontImage.MATERIAL_GROUPS_2);
    }
    public static IconData groups_3() {
        return new IconData(FontImage.MATERIAL_GROUPS_3);
    }
    public static IconData h_mobiledata() {
        return new IconData(FontImage.MATERIAL_H_MOBILEDATA);
    }
    public static IconData h_plus_mobiledata() {
        return new IconData(FontImage.MATERIAL_H_PLUS_MOBILEDATA);
    }
    public static IconData hail() {
        return new IconData(FontImage.MATERIAL_HAIL);
    }
    public static IconData handshake() {
        return new IconData(FontImage.MATERIAL_HANDSHAKE);
    }
    public static IconData handyman() {
        return new IconData(FontImage.MATERIAL_HANDYMAN);
    }
    public static IconData hardware() {
        return new IconData(FontImage.MATERIAL_HARDWARE);
    }
    public static IconData hd() {
        return new IconData(FontImage.MATERIAL_HD);
    }
    public static IconData hdr_auto() {
        return new IconData(FontImage.MATERIAL_HDR_AUTO);
    }
    public static IconData hdr_auto_select() {
        return new IconData(FontImage.MATERIAL_HDR_AUTO_SELECT);
    }
    public static IconData hdr_enhanced_select() {
        return new IconData(FontImage.MATERIAL_HDR_ENHANCED_SELECT);
    }
    public static IconData hdr_off() {
        return new IconData(FontImage.MATERIAL_HDR_OFF);
    }
    public static IconData hdr_off_select() {
        return new IconData(FontImage.MATERIAL_HDR_OFF_SELECT);
    }
    public static IconData hdr_on() {
        return new IconData(FontImage.MATERIAL_HDR_ON);
    }
    public static IconData hdr_on_select() {
        return new IconData(FontImage.MATERIAL_HDR_ON_SELECT);
    }
    public static IconData hdr_plus() {
        return new IconData(FontImage.MATERIAL_HDR_PLUS);
    }
    public static IconData hdr_strong() {
        return new IconData(FontImage.MATERIAL_HDR_STRONG);
    }
    public static IconData hdr_weak() {
        return new IconData(FontImage.MATERIAL_HDR_WEAK);
    }
    public static IconData headphones() {
        return new IconData(FontImage.MATERIAL_HEADPHONES);
    }
    public static IconData headphones_battery() {
        return new IconData(FontImage.MATERIAL_HEADPHONES_BATTERY);
    }
    public static IconData headset() {
        return new IconData(FontImage.MATERIAL_HEADSET);
    }
    public static IconData headset_mic() {
        return new IconData(FontImage.MATERIAL_HEADSET_MIC);
    }
    public static IconData headset_off() {
        return new IconData(FontImage.MATERIAL_HEADSET_OFF);
    }
    public static IconData healing() {
        return new IconData(FontImage.MATERIAL_HEALING);
    }
    public static IconData health_and_safety() {
        return new IconData(FontImage.MATERIAL_HEALTH_AND_SAFETY);
    }
    public static IconData hearing() {
        return new IconData(FontImage.MATERIAL_HEARING);
    }
    public static IconData hearing_disabled() {
        return new IconData(FontImage.MATERIAL_HEARING_DISABLED);
    }
    public static IconData heart_broken() {
        return new IconData(FontImage.MATERIAL_HEART_BROKEN);
    }
    public static IconData heat_pump() {
        return new IconData(FontImage.MATERIAL_HEAT_PUMP);
    }
    public static IconData height() {
        return new IconData(FontImage.MATERIAL_HEIGHT);
    }
    public static IconData help_center() {
        return new IconData(FontImage.MATERIAL_HELP_CENTER);
    }
    public static IconData help_outline() {
        return new IconData(FontImage.MATERIAL_HELP_OUTLINE);
    }
    public static IconData hevc() {
        return new IconData(FontImage.MATERIAL_HEVC);
    }
    public static IconData hexagon() {
        return new IconData(FontImage.MATERIAL_HEXAGON);
    }
    public static IconData hide_image() {
        return new IconData(FontImage.MATERIAL_HIDE_IMAGE);
    }
    public static IconData hide_source() {
        return new IconData(FontImage.MATERIAL_HIDE_SOURCE);
    }
    public static IconData high_quality() {
        return new IconData(FontImage.MATERIAL_HIGH_QUALITY);
    }
    public static IconData highlight() {
        return new IconData(FontImage.MATERIAL_HIGHLIGHT);
    }
    public static IconData highlight_alt() {
        return new IconData(FontImage.MATERIAL_HIGHLIGHT_ALT);
    }
    public static IconData highlight_off() {
        return new IconData(FontImage.MATERIAL_HIGHLIGHT_OFF);
    }
    public static IconData highlight_remove() {
        return new IconData(FontImage.MATERIAL_HIGHLIGHT_REMOVE);
    }
    public static IconData hiking() {
        return new IconData(FontImage.MATERIAL_HIKING);
    }
    public static IconData history() {
        return new IconData(FontImage.MATERIAL_HISTORY);
    }
    public static IconData history_edu() {
        return new IconData(FontImage.MATERIAL_HISTORY_EDU);
    }
    public static IconData history_toggle_off() {
        return new IconData(FontImage.MATERIAL_HISTORY_TOGGLE_OFF);
    }
    public static IconData hive() {
        return new IconData(FontImage.MATERIAL_HIVE);
    }
    public static IconData hls() {
        return new IconData(FontImage.MATERIAL_HLS);
    }
    public static IconData hls_off() {
        return new IconData(FontImage.MATERIAL_HLS_OFF);
    }
    public static IconData holiday_village() {
        return new IconData(FontImage.MATERIAL_HOLIDAY_VILLAGE);
    }
    public static IconData home_filled() {
        return new IconData(FontImage.MATERIAL_HOME_FILLED);
    }
    public static IconData home_max() {
        return new IconData(FontImage.MATERIAL_HOME_MAX);
    }
    public static IconData home_mini() {
        return new IconData(FontImage.MATERIAL_HOME_MINI);
    }
    public static IconData home_repair_service() {
        return new IconData(FontImage.MATERIAL_HOME_REPAIR_SERVICE);
    }
    public static IconData home_work() {
        return new IconData(FontImage.MATERIAL_HOME_WORK);
    }
    public static IconData horizontal_distribute() {
        return new IconData(FontImage.MATERIAL_HORIZONTAL_DISTRIBUTE);
    }
    public static IconData horizontal_rule() {
        return new IconData(FontImage.MATERIAL_HORIZONTAL_RULE);
    }
    public static IconData horizontal_split() {
        return new IconData(FontImage.MATERIAL_HORIZONTAL_SPLIT);
    }
    public static IconData hot_tub() {
        return new IconData(FontImage.MATERIAL_HOT_TUB);
    }
    public static IconData hotel_class() {
        return new IconData(FontImage.MATERIAL_HOTEL_CLASS);
    }
    public static IconData hourglass_bottom() {
        return new IconData(FontImage.MATERIAL_HOURGLASS_BOTTOM);
    }
    public static IconData hourglass_disabled() {
        return new IconData(FontImage.MATERIAL_HOURGLASS_DISABLED);
    }
    public static IconData hourglass_empty() {
        return new IconData(FontImage.MATERIAL_HOURGLASS_EMPTY);
    }
    public static IconData hourglass_full() {
        return new IconData(FontImage.MATERIAL_HOURGLASS_FULL);
    }
    public static IconData hourglass_top() {
        return new IconData(FontImage.MATERIAL_HOURGLASS_TOP);
    }
    public static IconData house() {
        return new IconData(FontImage.MATERIAL_HOUSE);
    }
    public static IconData house_siding() {
        return new IconData(FontImage.MATERIAL_HOUSE_SIDING);
    }
    public static IconData houseboat() {
        return new IconData(FontImage.MATERIAL_HOUSEBOAT);
    }
    public static IconData how_to_reg() {
        return new IconData(FontImage.MATERIAL_HOW_TO_REG);
    }
    public static IconData how_to_vote() {
        return new IconData(FontImage.MATERIAL_HOW_TO_VOTE);
    }
    public static IconData html() {
        return new IconData(FontImage.MATERIAL_HTML);
    }
    public static IconData http() {
        return new IconData(FontImage.MATERIAL_HTTP);
    }
    public static IconData https() {
        return new IconData(FontImage.MATERIAL_HTTPS);
    }
    public static IconData hub() {
        return new IconData(FontImage.MATERIAL_HUB);
    }
    public static IconData hvac() {
        return new IconData(FontImage.MATERIAL_HVAC);
    }
    public static IconData ice_skating() {
        return new IconData(FontImage.MATERIAL_ICE_SKATING);
    }
    public static IconData icecream() {
        return new IconData(FontImage.MATERIAL_ICECREAM);
    }
    public static IconData image() {
        return new IconData(FontImage.MATERIAL_IMAGE);
    }
    public static IconData image_aspect_ratio() {
        return new IconData(FontImage.MATERIAL_IMAGE_ASPECT_RATIO);
    }
    public static IconData image_not_supported() {
        return new IconData(FontImage.MATERIAL_IMAGE_NOT_SUPPORTED);
    }
    public static IconData image_search() {
        return new IconData(FontImage.MATERIAL_IMAGE_SEARCH);
    }
    public static IconData imagesearch_roller() {
        return new IconData(FontImage.MATERIAL_IMAGESEARCH_ROLLER);
    }
    public static IconData import_contacts() {
        return new IconData(FontImage.MATERIAL_IMPORT_CONTACTS);
    }
    public static IconData import_export() {
        return new IconData(FontImage.MATERIAL_IMPORT_EXPORT);
    }
    public static IconData important_devices() {
        return new IconData(FontImage.MATERIAL_IMPORTANT_DEVICES);
    }
    public static IconData inbox() {
        return new IconData(FontImage.MATERIAL_INBOX);
    }
    public static IconData incomplete_circle() {
        return new IconData(FontImage.MATERIAL_INCOMPLETE_CIRCLE);
    }
    public static IconData indeterminate_check_box() {
        return new IconData(FontImage.MATERIAL_INDETERMINATE_CHECK_BOX);
    }
    public static IconData input() {
        return new IconData(FontImage.MATERIAL_INPUT);
    }
    public static IconData insert_chart() {
        return new IconData(FontImage.MATERIAL_INSERT_CHART);
    }
    public static IconData insert_chart_outlined() {
        return new IconData(FontImage.MATERIAL_INSERT_CHART_OUTLINED);
    }
    public static IconData insert_comment() {
        return new IconData(FontImage.MATERIAL_INSERT_COMMENT);
    }
    public static IconData insert_drive_file() {
        return new IconData(FontImage.MATERIAL_INSERT_DRIVE_FILE);
    }
    public static IconData insert_emoticon() {
        return new IconData(FontImage.MATERIAL_INSERT_EMOTICON);
    }
    public static IconData insert_invitation() {
        return new IconData(FontImage.MATERIAL_INSERT_INVITATION);
    }
    public static IconData insert_link() {
        return new IconData(FontImage.MATERIAL_INSERT_LINK);
    }
    public static IconData insert_page_break() {
        return new IconData(FontImage.MATERIAL_INSERT_PAGE_BREAK);
    }
    public static IconData insert_photo() {
        return new IconData(FontImage.MATERIAL_INSERT_PHOTO);
    }
    public static IconData insights() {
        return new IconData(FontImage.MATERIAL_INSIGHTS);
    }
    public static IconData install_desktop() {
        return new IconData(FontImage.MATERIAL_INSTALL_DESKTOP);
    }
    public static IconData install_mobile() {
        return new IconData(FontImage.MATERIAL_INSTALL_MOBILE);
    }
    public static IconData integration_instructions() {
        return new IconData(FontImage.MATERIAL_INTEGRATION_INSTRUCTIONS);
    }
    public static IconData interests() {
        return new IconData(FontImage.MATERIAL_INTERESTS);
    }
    public static IconData interpreter_mode() {
        return new IconData(FontImage.MATERIAL_INTERPRETER_MODE);
    }
    public static IconData inventory() {
        return new IconData(FontImage.MATERIAL_INVENTORY);
    }
    public static IconData inventory_2() {
        return new IconData(FontImage.MATERIAL_INVENTORY_2);
    }
    public static IconData invert_colors() {
        return new IconData(FontImage.MATERIAL_INVERT_COLORS);
    }
    public static IconData invert_colors_off() {
        return new IconData(FontImage.MATERIAL_INVERT_COLORS_OFF);
    }
    public static IconData invert_colors_on() {
        return new IconData(FontImage.MATERIAL_INVERT_COLORS_ON);
    }
    public static IconData ios_share() {
        return new IconData(FontImage.MATERIAL_IOS_SHARE);
    }
    public static IconData iron() {
        return new IconData(FontImage.MATERIAL_IRON);
    }
    public static IconData iso() {
        return new IconData(FontImage.MATERIAL_ISO);
    }
    public static IconData javascript() {
        return new IconData(FontImage.MATERIAL_JAVASCRIPT);
    }
    public static IconData join_full() {
        return new IconData(FontImage.MATERIAL_JOIN_FULL);
    }
    public static IconData join_inner() {
        return new IconData(FontImage.MATERIAL_JOIN_INNER);
    }
    public static IconData join_left() {
        return new IconData(FontImage.MATERIAL_JOIN_LEFT);
    }
    public static IconData join_right() {
        return new IconData(FontImage.MATERIAL_JOIN_RIGHT);
    }
    public static IconData kayaking() {
        return new IconData(FontImage.MATERIAL_KAYAKING);
    }
    public static IconData kebab_dining() {
        return new IconData(FontImage.MATERIAL_KEBAB_DINING);
    }
    public static IconData key() {
        return new IconData(FontImage.MATERIAL_KEY);
    }
    public static IconData key_off() {
        return new IconData(FontImage.MATERIAL_KEY_OFF);
    }
    public static IconData keyboard() {
        return new IconData(FontImage.MATERIAL_KEYBOARD);
    }
    public static IconData keyboard_alt() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_ALT);
    }
    public static IconData keyboard_arrow_left() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_ARROW_LEFT);
    }
    public static IconData keyboard_arrow_right() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_ARROW_RIGHT);
    }
    public static IconData keyboard_backspace() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_BACKSPACE);
    }
    public static IconData keyboard_capslock() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_CAPSLOCK);
    }
    public static IconData keyboard_command() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_COMMAND);
    }
    public static IconData keyboard_command_key() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_COMMAND_KEY);
    }
    public static IconData keyboard_control() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_CONTROL);
    }
    public static IconData keyboard_control_key() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_CONTROL_KEY);
    }
    public static IconData keyboard_double_arrow_down() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_DOUBLE_ARROW_DOWN);
    }
    public static IconData keyboard_double_arrow_left() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_DOUBLE_ARROW_LEFT);
    }
    public static IconData keyboard_double_arrow_right() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_DOUBLE_ARROW_RIGHT);
    }
    public static IconData keyboard_double_arrow_up() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_DOUBLE_ARROW_UP);
    }
    public static IconData keyboard_hide() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_HIDE);
    }
    public static IconData keyboard_option() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_OPTION);
    }
    public static IconData keyboard_option_key() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_OPTION_KEY);
    }
    public static IconData keyboard_return() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_RETURN);
    }
    public static IconData keyboard_tab() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_TAB);
    }
    public static IconData keyboard_voice() {
        return new IconData(FontImage.MATERIAL_KEYBOARD_VOICE);
    }
    public static IconData king_bed() {
        return new IconData(FontImage.MATERIAL_KING_BED);
    }
    public static IconData kitchen() {
        return new IconData(FontImage.MATERIAL_KITCHEN);
    }
    public static IconData kitesurfing() {
        return new IconData(FontImage.MATERIAL_KITESURFING);
    }
    public static IconData label() {
        return new IconData(FontImage.MATERIAL_LABEL);
    }
    public static IconData label_important() {
        return new IconData(FontImage.MATERIAL_LABEL_IMPORTANT);
    }
    public static IconData label_important_outline() {
        return new IconData(FontImage.MATERIAL_LABEL_IMPORTANT_OUTLINE);
    }
    public static IconData label_off() {
        return new IconData(FontImage.MATERIAL_LABEL_OFF);
    }
    public static IconData label_outline() {
        return new IconData(FontImage.MATERIAL_LABEL_OUTLINE);
    }
    public static IconData lan() {
        return new IconData(FontImage.MATERIAL_LAN);
    }
    public static IconData landscape() {
        return new IconData(FontImage.MATERIAL_LANDSCAPE);
    }
    public static IconData landslide() {
        return new IconData(FontImage.MATERIAL_LANDSLIDE);
    }
    public static IconData language() {
        return new IconData(FontImage.MATERIAL_LANGUAGE);
    }
    public static IconData laptop() {
        return new IconData(FontImage.MATERIAL_LAPTOP);
    }
    public static IconData laptop_chromebook() {
        return new IconData(FontImage.MATERIAL_LAPTOP_CHROMEBOOK);
    }
    public static IconData laptop_mac() {
        return new IconData(FontImage.MATERIAL_LAPTOP_MAC);
    }
    public static IconData laptop_windows() {
        return new IconData(FontImage.MATERIAL_LAPTOP_WINDOWS);
    }
    public static IconData last_page() {
        return new IconData(FontImage.MATERIAL_LAST_PAGE);
    }
    public static IconData launch() {
        return new IconData(FontImage.MATERIAL_LAUNCH);
    }
    public static IconData layers() {
        return new IconData(FontImage.MATERIAL_LAYERS);
    }
    public static IconData layers_clear() {
        return new IconData(FontImage.MATERIAL_LAYERS_CLEAR);
    }
    public static IconData leaderboard() {
        return new IconData(FontImage.MATERIAL_LEADERBOARD);
    }
    public static IconData leak_add() {
        return new IconData(FontImage.MATERIAL_LEAK_ADD);
    }
    public static IconData leak_remove() {
        return new IconData(FontImage.MATERIAL_LEAK_REMOVE);
    }
    public static IconData leave_bags_at_home() {
        return new IconData(FontImage.MATERIAL_LEAVE_BAGS_AT_HOME);
    }
    public static IconData legend_toggle() {
        return new IconData(FontImage.MATERIAL_LEGEND_TOGGLE);
    }
    public static IconData lens() {
        return new IconData(FontImage.MATERIAL_LENS);
    }
    public static IconData lens_blur() {
        return new IconData(FontImage.MATERIAL_LENS_BLUR);
    }
    public static IconData library_add() {
        return new IconData(FontImage.MATERIAL_LIBRARY_ADD);
    }
    public static IconData library_add_check() {
        return new IconData(FontImage.MATERIAL_LIBRARY_ADD_CHECK);
    }
    public static IconData library_music() {
        return new IconData(FontImage.MATERIAL_LIBRARY_MUSIC);
    }
    public static IconData light() {
        return new IconData(FontImage.MATERIAL_LIGHT);
    }
    public static IconData light_mode() {
        return new IconData(FontImage.MATERIAL_LIGHT_MODE);
    }
    public static IconData lightbulb() {
        return new IconData(FontImage.MATERIAL_LIGHTBULB);
    }
    public static IconData lightbulb_circle() {
        return new IconData(FontImage.MATERIAL_LIGHTBULB_CIRCLE);
    }
    public static IconData lightbulb_outline() {
        return new IconData(FontImage.MATERIAL_LIGHTBULB_OUTLINE);
    }
    public static IconData line_axis() {
        return new IconData(FontImage.MATERIAL_LINE_AXIS);
    }
    public static IconData line_style() {
        return new IconData(FontImage.MATERIAL_LINE_STYLE);
    }
    public static IconData line_weight() {
        return new IconData(FontImage.MATERIAL_LINE_WEIGHT);
    }
    public static IconData linear_scale() {
        return new IconData(FontImage.MATERIAL_LINEAR_SCALE);
    }
    public static IconData link_off() {
        return new IconData(FontImage.MATERIAL_LINK_OFF);
    }
    public static IconData linked_camera() {
        return new IconData(FontImage.MATERIAL_LINKED_CAMERA);
    }
    public static IconData liquor() {
        return new IconData(FontImage.MATERIAL_LIQUOR);
    }
    public static IconData list() {
        return new IconData(FontImage.MATERIAL_LIST);
    }
    public static IconData list_alt() {
        return new IconData(FontImage.MATERIAL_LIST_ALT);
    }
    public static IconData live_help() {
        return new IconData(FontImage.MATERIAL_LIVE_HELP);
    }
    public static IconData live_tv() {
        return new IconData(FontImage.MATERIAL_LIVE_TV);
    }
    public static IconData living() {
        return new IconData(FontImage.MATERIAL_LIVING);
    }
    public static IconData local_activity() {
        return new IconData(FontImage.MATERIAL_LOCAL_ACTIVITY);
    }
    public static IconData local_airport() {
        return new IconData(FontImage.MATERIAL_LOCAL_AIRPORT);
    }
    public static IconData local_atm() {
        return new IconData(FontImage.MATERIAL_LOCAL_ATM);
    }
    public static IconData local_attraction() {
        return new IconData(FontImage.MATERIAL_LOCAL_ATTRACTION);
    }
    public static IconData local_bar() {
        return new IconData(FontImage.MATERIAL_LOCAL_BAR);
    }
    public static IconData local_cafe() {
        return new IconData(FontImage.MATERIAL_LOCAL_CAFE);
    }
    public static IconData local_car_wash() {
        return new IconData(FontImage.MATERIAL_LOCAL_CAR_WASH);
    }
    public static IconData local_convenience_store() {
        return new IconData(FontImage.MATERIAL_LOCAL_CONVENIENCE_STORE);
    }
    public static IconData local_dining() {
        return new IconData(FontImage.MATERIAL_LOCAL_DINING);
    }
    public static IconData local_drink() {
        return new IconData(FontImage.MATERIAL_LOCAL_DRINK);
    }
    public static IconData local_fire_department() {
        return new IconData(FontImage.MATERIAL_LOCAL_FIRE_DEPARTMENT);
    }
    public static IconData local_florist() {
        return new IconData(FontImage.MATERIAL_LOCAL_FLORIST);
    }
    public static IconData local_gas_station() {
        return new IconData(FontImage.MATERIAL_LOCAL_GAS_STATION);
    }
    public static IconData local_grocery_store() {
        return new IconData(FontImage.MATERIAL_LOCAL_GROCERY_STORE);
    }
    public static IconData local_hospital() {
        return new IconData(FontImage.MATERIAL_LOCAL_HOSPITAL);
    }
    public static IconData local_hotel() {
        return new IconData(FontImage.MATERIAL_LOCAL_HOTEL);
    }
    public static IconData local_laundry_service() {
        return new IconData(FontImage.MATERIAL_LOCAL_LAUNDRY_SERVICE);
    }
    public static IconData local_library() {
        return new IconData(FontImage.MATERIAL_LOCAL_LIBRARY);
    }
    public static IconData local_mall() {
        return new IconData(FontImage.MATERIAL_LOCAL_MALL);
    }
    public static IconData local_movies() {
        return new IconData(FontImage.MATERIAL_LOCAL_MOVIES);
    }
    public static IconData local_offer() {
        return new IconData(FontImage.MATERIAL_LOCAL_OFFER);
    }
    public static IconData local_parking() {
        return new IconData(FontImage.MATERIAL_LOCAL_PARKING);
    }
    public static IconData local_pharmacy() {
        return new IconData(FontImage.MATERIAL_LOCAL_PHARMACY);
    }
    public static IconData local_phone() {
        return new IconData(FontImage.MATERIAL_LOCAL_PHONE);
    }
    public static IconData local_pizza() {
        return new IconData(FontImage.MATERIAL_LOCAL_PIZZA);
    }
    public static IconData local_play() {
        return new IconData(FontImage.MATERIAL_LOCAL_PLAY);
    }
    public static IconData local_police() {
        return new IconData(FontImage.MATERIAL_LOCAL_POLICE);
    }
    public static IconData local_post_office() {
        return new IconData(FontImage.MATERIAL_LOCAL_POST_OFFICE);
    }
    public static IconData local_print_shop() {
        return new IconData(FontImage.MATERIAL_LOCAL_PRINT_SHOP);
    }
    public static IconData local_printshop() {
        return new IconData(FontImage.MATERIAL_LOCAL_PRINTSHOP);
    }
    public static IconData local_restaurant() {
        return new IconData(FontImage.MATERIAL_LOCAL_RESTAURANT);
    }
    public static IconData local_see() {
        return new IconData(FontImage.MATERIAL_LOCAL_SEE);
    }
    public static IconData local_shipping() {
        return new IconData(FontImage.MATERIAL_LOCAL_SHIPPING);
    }
    public static IconData local_taxi() {
        return new IconData(FontImage.MATERIAL_LOCAL_TAXI);
    }
    public static IconData location_city() {
        return new IconData(FontImage.MATERIAL_LOCATION_CITY);
    }
    public static IconData location_disabled() {
        return new IconData(FontImage.MATERIAL_LOCATION_DISABLED);
    }
    public static IconData location_history() {
        return new IconData(FontImage.MATERIAL_LOCATION_HISTORY);
    }
    public static IconData location_off() {
        return new IconData(FontImage.MATERIAL_LOCATION_OFF);
    }
    public static IconData location_on() {
        return new IconData(FontImage.MATERIAL_LOCATION_ON);
    }
    public static IconData location_pin() {
        return new IconData(FontImage.MATERIAL_LOCATION_PIN);
    }
    public static IconData location_searching() {
        return new IconData(FontImage.MATERIAL_LOCATION_SEARCHING);
    }
    public static IconData lock_clock() {
        return new IconData(FontImage.MATERIAL_LOCK_CLOCK);
    }
    public static IconData lock_open() {
        return new IconData(FontImage.MATERIAL_LOCK_OPEN);
    }
    public static IconData lock_outline() {
        return new IconData(FontImage.MATERIAL_LOCK_OUTLINE);
    }
    public static IconData lock_person() {
        return new IconData(FontImage.MATERIAL_LOCK_PERSON);
    }
    public static IconData lock_reset() {
        return new IconData(FontImage.MATERIAL_LOCK_RESET);
    }
    public static IconData login() {
        return new IconData(FontImage.MATERIAL_LOGIN);
    }
    public static IconData logo_dev() {
        return new IconData(FontImage.MATERIAL_LOGO_DEV);
    }
    public static IconData logout() {
        return new IconData(FontImage.MATERIAL_LOGOUT);
    }
    public static IconData looks() {
        return new IconData(FontImage.MATERIAL_LOOKS);
    }
    public static IconData looks_3() {
        return new IconData(FontImage.MATERIAL_LOOKS_3);
    }
    public static IconData looks_4() {
        return new IconData(FontImage.MATERIAL_LOOKS_4);
    }
    public static IconData looks_5() {
        return new IconData(FontImage.MATERIAL_LOOKS_5);
    }
    public static IconData looks_6() {
        return new IconData(FontImage.MATERIAL_LOOKS_6);
    }
    public static IconData looks_one() {
        return new IconData(FontImage.MATERIAL_LOOKS_ONE);
    }
    public static IconData looks_two() {
        return new IconData(FontImage.MATERIAL_LOOKS_TWO);
    }
    public static IconData loop() {
        return new IconData(FontImage.MATERIAL_LOOP);
    }
    public static IconData loupe() {
        return new IconData(FontImage.MATERIAL_LOUPE);
    }
    public static IconData low_priority() {
        return new IconData(FontImage.MATERIAL_LOW_PRIORITY);
    }
    public static IconData loyalty() {
        return new IconData(FontImage.MATERIAL_LOYALTY);
    }
    public static IconData lte_mobiledata() {
        return new IconData(FontImage.MATERIAL_LTE_MOBILEDATA);
    }
    public static IconData lte_plus_mobiledata() {
        return new IconData(FontImage.MATERIAL_LTE_PLUS_MOBILEDATA);
    }
    public static IconData luggage() {
        return new IconData(FontImage.MATERIAL_LUGGAGE);
    }
    public static IconData lunch_dining() {
        return new IconData(FontImage.MATERIAL_LUNCH_DINING);
    }
    public static IconData lyrics() {
        return new IconData(FontImage.MATERIAL_LYRICS);
    }
    public static IconData macro_off() {
        return new IconData(FontImage.MATERIAL_MACRO_OFF);
    }
    public static IconData mail() {
        return new IconData(FontImage.MATERIAL_MAIL);
    }
    public static IconData mail_lock() {
        return new IconData(FontImage.MATERIAL_MAIL_LOCK);
    }
    public static IconData mail_outline() {
        return new IconData(FontImage.MATERIAL_MAIL_OUTLINE);
    }
    public static IconData male() {
        return new IconData(FontImage.MATERIAL_MALE);
    }
    public static IconData man() {
        return new IconData(FontImage.MATERIAL_MAN);
    }
    public static IconData man_2() {
        return new IconData(FontImage.MATERIAL_MAN_2);
    }
    public static IconData man_3() {
        return new IconData(FontImage.MATERIAL_MAN_3);
    }
    public static IconData man_4() {
        return new IconData(FontImage.MATERIAL_MAN_4);
    }
    public static IconData manage_accounts() {
        return new IconData(FontImage.MATERIAL_MANAGE_ACCOUNTS);
    }
    public static IconData manage_history() {
        return new IconData(FontImage.MATERIAL_MANAGE_HISTORY);
    }
    public static IconData manage_search() {
        return new IconData(FontImage.MATERIAL_MANAGE_SEARCH);
    }
    public static IconData map() {
        return new IconData(FontImage.MATERIAL_MAP);
    }
    public static IconData maps_home_work() {
        return new IconData(FontImage.MATERIAL_MAPS_HOME_WORK);
    }
    public static IconData maps_ugc() {
        return new IconData(FontImage.MATERIAL_MAPS_UGC);
    }
    public static IconData margin() {
        return new IconData(FontImage.MATERIAL_MARGIN);
    }
    public static IconData mark_as_unread() {
        return new IconData(FontImage.MATERIAL_MARK_AS_UNREAD);
    }
    public static IconData mark_chat_read() {
        return new IconData(FontImage.MATERIAL_MARK_CHAT_READ);
    }
    public static IconData mark_chat_unread() {
        return new IconData(FontImage.MATERIAL_MARK_CHAT_UNREAD);
    }
    public static IconData mark_email_read() {
        return new IconData(FontImage.MATERIAL_MARK_EMAIL_READ);
    }
    public static IconData mark_email_unread() {
        return new IconData(FontImage.MATERIAL_MARK_EMAIL_UNREAD);
    }
    public static IconData mark_unread_chat_alt() {
        return new IconData(FontImage.MATERIAL_MARK_UNREAD_CHAT_ALT);
    }
    public static IconData markunread() {
        return new IconData(FontImage.MATERIAL_MARKUNREAD);
    }
    public static IconData markunread_mailbox() {
        return new IconData(FontImage.MATERIAL_MARKUNREAD_MAILBOX);
    }
    public static IconData masks() {
        return new IconData(FontImage.MATERIAL_MASKS);
    }
    public static IconData maximize() {
        return new IconData(FontImage.MATERIAL_MAXIMIZE);
    }
    public static IconData media_bluetooth_off() {
        return new IconData(FontImage.MATERIAL_MEDIA_BLUETOOTH_OFF);
    }
    public static IconData media_bluetooth_on() {
        return new IconData(FontImage.MATERIAL_MEDIA_BLUETOOTH_ON);
    }
    public static IconData mediation() {
        return new IconData(FontImage.MATERIAL_MEDIATION);
    }
    public static IconData medical_information() {
        return new IconData(FontImage.MATERIAL_MEDICAL_INFORMATION);
    }
    public static IconData medical_services() {
        return new IconData(FontImage.MATERIAL_MEDICAL_SERVICES);
    }
    public static IconData medication() {
        return new IconData(FontImage.MATERIAL_MEDICATION);
    }
    public static IconData medication_liquid() {
        return new IconData(FontImage.MATERIAL_MEDICATION_LIQUID);
    }
    public static IconData meeting_room() {
        return new IconData(FontImage.MATERIAL_MEETING_ROOM);
    }
    public static IconData memory() {
        return new IconData(FontImage.MATERIAL_MEMORY);
    }
    public static IconData menu_book() {
        return new IconData(FontImage.MATERIAL_MENU_BOOK);
    }
    public static IconData menu_open() {
        return new IconData(FontImage.MATERIAL_MENU_OPEN);
    }
    public static IconData merge() {
        return new IconData(FontImage.MATERIAL_MERGE);
    }
    public static IconData merge_type() {
        return new IconData(FontImage.MATERIAL_MERGE_TYPE);
    }
    public static IconData message() {
        return new IconData(FontImage.MATERIAL_MESSAGE);
    }
    public static IconData messenger() {
        return new IconData(FontImage.MATERIAL_MESSENGER);
    }
    public static IconData messenger_outline() {
        return new IconData(FontImage.MATERIAL_MESSENGER_OUTLINE);
    }
    public static IconData mic_external_off() {
        return new IconData(FontImage.MATERIAL_MIC_EXTERNAL_OFF);
    }
    public static IconData mic_external_on() {
        return new IconData(FontImage.MATERIAL_MIC_EXTERNAL_ON);
    }
    public static IconData mic_none() {
        return new IconData(FontImage.MATERIAL_MIC_NONE);
    }
    public static IconData mic_off() {
        return new IconData(FontImage.MATERIAL_MIC_OFF);
    }
    public static IconData microwave() {
        return new IconData(FontImage.MATERIAL_MICROWAVE);
    }
    public static IconData military_tech() {
        return new IconData(FontImage.MATERIAL_MILITARY_TECH);
    }
    public static IconData minimize() {
        return new IconData(FontImage.MATERIAL_MINIMIZE);
    }
    public static IconData minor_crash() {
        return new IconData(FontImage.MATERIAL_MINOR_CRASH);
    }
    public static IconData miscellaneous_services() {
        return new IconData(FontImage.MATERIAL_MISCELLANEOUS_SERVICES);
    }
    public static IconData missed_video_call() {
        return new IconData(FontImage.MATERIAL_MISSED_VIDEO_CALL);
    }
    public static IconData mms() {
        return new IconData(FontImage.MATERIAL_MMS);
    }
    public static IconData mobile_friendly() {
        return new IconData(FontImage.MATERIAL_MOBILE_FRIENDLY);
    }
    public static IconData mobile_off() {
        return new IconData(FontImage.MATERIAL_MOBILE_OFF);
    }
    public static IconData mobile_screen_share() {
        return new IconData(FontImage.MATERIAL_MOBILE_SCREEN_SHARE);
    }
    public static IconData mobiledata_off() {
        return new IconData(FontImage.MATERIAL_MOBILEDATA_OFF);
    }
    public static IconData mode() {
        return new IconData(FontImage.MATERIAL_MODE);
    }
    public static IconData mode_comment() {
        return new IconData(FontImage.MATERIAL_MODE_COMMENT);
    }
    public static IconData mode_edit() {
        return new IconData(FontImage.MATERIAL_MODE_EDIT);
    }
    public static IconData mode_edit_outline() {
        return new IconData(FontImage.MATERIAL_MODE_EDIT_OUTLINE);
    }
    public static IconData mode_fan_off() {
        return new IconData(FontImage.MATERIAL_MODE_FAN_OFF);
    }
    public static IconData mode_night() {
        return new IconData(FontImage.MATERIAL_MODE_NIGHT);
    }
    public static IconData mode_of_travel() {
        return new IconData(FontImage.MATERIAL_MODE_OF_TRAVEL);
    }
    public static IconData mode_standby() {
        return new IconData(FontImage.MATERIAL_MODE_STANDBY);
    }
    public static IconData model_training() {
        return new IconData(FontImage.MATERIAL_MODEL_TRAINING);
    }
    public static IconData monetization_on() {
        return new IconData(FontImage.MATERIAL_MONETIZATION_ON);
    }
    public static IconData money() {
        return new IconData(FontImage.MATERIAL_MONEY);
    }
    public static IconData money_off_csred() {
        return new IconData(FontImage.MATERIAL_MONEY_OFF_CSRED);
    }
    public static IconData monitor() {
        return new IconData(FontImage.MATERIAL_MONITOR);
    }
    public static IconData monitor_heart() {
        return new IconData(FontImage.MATERIAL_MONITOR_HEART);
    }
    public static IconData monitor_weight() {
        return new IconData(FontImage.MATERIAL_MONITOR_WEIGHT);
    }
    public static IconData monochrome_photos() {
        return new IconData(FontImage.MATERIAL_MONOCHROME_PHOTOS);
    }
    public static IconData mood() {
        return new IconData(FontImage.MATERIAL_MOOD);
    }
    public static IconData mood_bad() {
        return new IconData(FontImage.MATERIAL_MOOD_BAD);
    }
    public static IconData moped() {
        return new IconData(FontImage.MATERIAL_MOPED);
    }
    public static IconData more() {
        return new IconData(FontImage.MATERIAL_MORE);
    }
    public static IconData more_time() {
        return new IconData(FontImage.MATERIAL_MORE_TIME);
    }
    public static IconData mosque() {
        return new IconData(FontImage.MATERIAL_MOSQUE);
    }
    public static IconData motion_photos_auto() {
        return new IconData(FontImage.MATERIAL_MOTION_PHOTOS_AUTO);
    }
    public static IconData motion_photos_off() {
        return new IconData(FontImage.MATERIAL_MOTION_PHOTOS_OFF);
    }
    public static IconData motion_photos_on() {
        return new IconData(FontImage.MATERIAL_MOTION_PHOTOS_ON);
    }
    public static IconData motion_photos_pause() {
        return new IconData(FontImage.MATERIAL_MOTION_PHOTOS_PAUSE);
    }
    public static IconData motion_photos_paused() {
        return new IconData(FontImage.MATERIAL_MOTION_PHOTOS_PAUSED);
    }
    public static IconData motorcycle() {
        return new IconData(FontImage.MATERIAL_MOTORCYCLE);
    }
    public static IconData mouse() {
        return new IconData(FontImage.MATERIAL_MOUSE);
    }
    public static IconData move_down() {
        return new IconData(FontImage.MATERIAL_MOVE_DOWN);
    }
    public static IconData move_to_inbox() {
        return new IconData(FontImage.MATERIAL_MOVE_TO_INBOX);
    }
    public static IconData move_up() {
        return new IconData(FontImage.MATERIAL_MOVE_UP);
    }
    public static IconData movie() {
        return new IconData(FontImage.MATERIAL_MOVIE);
    }
    public static IconData movie_creation() {
        return new IconData(FontImage.MATERIAL_MOVIE_CREATION);
    }
    public static IconData movie_edit() {
        return new IconData(FontImage.MATERIAL_MOVIE_EDIT);
    }
    public static IconData movie_filter() {
        return new IconData(FontImage.MATERIAL_MOVIE_FILTER);
    }
    public static IconData moving() {
        return new IconData(FontImage.MATERIAL_MOVING);
    }
    public static IconData mp() {
        return new IconData(FontImage.MATERIAL_MP);
    }
    public static IconData multiline_chart() {
        return new IconData(FontImage.MATERIAL_MULTILINE_CHART);
    }
    public static IconData multiple_stop() {
        return new IconData(FontImage.MATERIAL_MULTIPLE_STOP);
    }
    public static IconData multitrack_audio() {
        return new IconData(FontImage.MATERIAL_MULTITRACK_AUDIO);
    }
    public static IconData museum() {
        return new IconData(FontImage.MATERIAL_MUSEUM);
    }
    public static IconData music_note() {
        return new IconData(FontImage.MATERIAL_MUSIC_NOTE);
    }
    public static IconData music_off() {
        return new IconData(FontImage.MATERIAL_MUSIC_OFF);
    }
    public static IconData music_video() {
        return new IconData(FontImage.MATERIAL_MUSIC_VIDEO);
    }
    public static IconData my_library_add() {
        return new IconData(FontImage.MATERIAL_MY_LIBRARY_ADD);
    }
    public static IconData my_library_books() {
        return new IconData(FontImage.MATERIAL_MY_LIBRARY_BOOKS);
    }
    public static IconData my_library_music() {
        return new IconData(FontImage.MATERIAL_MY_LIBRARY_MUSIC);
    }
    public static IconData my_location() {
        return new IconData(FontImage.MATERIAL_MY_LOCATION);
    }
    public static IconData nat() {
        return new IconData(FontImage.MATERIAL_NAT);
    }
    public static IconData nature() {
        return new IconData(FontImage.MATERIAL_NATURE);
    }
    public static IconData nature_people() {
        return new IconData(FontImage.MATERIAL_NATURE_PEOPLE);
    }
    public static IconData navigate_before() {
        return new IconData(FontImage.MATERIAL_NAVIGATE_BEFORE);
    }
    public static IconData navigate_next() {
        return new IconData(FontImage.MATERIAL_NAVIGATE_NEXT);
    }
    public static IconData navigation() {
        return new IconData(FontImage.MATERIAL_NAVIGATION);
    }
    public static IconData near_me() {
        return new IconData(FontImage.MATERIAL_NEAR_ME);
    }
    public static IconData near_me_disabled() {
        return new IconData(FontImage.MATERIAL_NEAR_ME_DISABLED);
    }
    public static IconData nearby_error() {
        return new IconData(FontImage.MATERIAL_NEARBY_ERROR);
    }
    public static IconData nearby_off() {
        return new IconData(FontImage.MATERIAL_NEARBY_OFF);
    }
    public static IconData nest_cam_wired_stand() {
        return new IconData(FontImage.MATERIAL_NEST_CAM_WIRED_STAND);
    }
    public static IconData network_cell() {
        return new IconData(FontImage.MATERIAL_NETWORK_CELL);
    }
    public static IconData network_check() {
        return new IconData(FontImage.MATERIAL_NETWORK_CHECK);
    }
    public static IconData network_locked() {
        return new IconData(FontImage.MATERIAL_NETWORK_LOCKED);
    }
    public static IconData network_ping() {
        return new IconData(FontImage.MATERIAL_NETWORK_PING);
    }
    public static IconData network_wifi() {
        return new IconData(FontImage.MATERIAL_NETWORK_WIFI);
    }
    public static IconData network_wifi_1_bar() {
        return new IconData(FontImage.MATERIAL_NETWORK_WIFI_1_BAR);
    }
    public static IconData network_wifi_2_bar() {
        return new IconData(FontImage.MATERIAL_NETWORK_WIFI_2_BAR);
    }
    public static IconData network_wifi_3_bar() {
        return new IconData(FontImage.MATERIAL_NETWORK_WIFI_3_BAR);
    }
    public static IconData new_label() {
        return new IconData(FontImage.MATERIAL_NEW_LABEL);
    }
    public static IconData new_releases() {
        return new IconData(FontImage.MATERIAL_NEW_RELEASES);
    }
    public static IconData newspaper() {
        return new IconData(FontImage.MATERIAL_NEWSPAPER);
    }
    public static IconData next_plan() {
        return new IconData(FontImage.MATERIAL_NEXT_PLAN);
    }
    public static IconData next_week() {
        return new IconData(FontImage.MATERIAL_NEXT_WEEK);
    }
    public static IconData nfc() {
        return new IconData(FontImage.MATERIAL_NFC);
    }
    public static IconData night_shelter() {
        return new IconData(FontImage.MATERIAL_NIGHT_SHELTER);
    }
    public static IconData nightlife() {
        return new IconData(FontImage.MATERIAL_NIGHTLIFE);
    }
    public static IconData nightlight() {
        return new IconData(FontImage.MATERIAL_NIGHTLIGHT);
    }
    public static IconData nightlight_round() {
        return new IconData(FontImage.MATERIAL_NIGHTLIGHT_ROUND);
    }
    public static IconData nights_stay() {
        return new IconData(FontImage.MATERIAL_NIGHTS_STAY);
    }
    public static IconData no_accounts() {
        return new IconData(FontImage.MATERIAL_NO_ACCOUNTS);
    }
    public static IconData no_adult_content() {
        return new IconData(FontImage.MATERIAL_NO_ADULT_CONTENT);
    }
    public static IconData no_backpack() {
        return new IconData(FontImage.MATERIAL_NO_BACKPACK);
    }
    public static IconData no_cell() {
        return new IconData(FontImage.MATERIAL_NO_CELL);
    }
    public static IconData no_crash() {
        return new IconData(FontImage.MATERIAL_NO_CRASH);
    }
    public static IconData no_drinks() {
        return new IconData(FontImage.MATERIAL_NO_DRINKS);
    }
    public static IconData no_encryption() {
        return new IconData(FontImage.MATERIAL_NO_ENCRYPTION);
    }
    public static IconData no_encryption_gmailerrorred() {
        return new IconData(FontImage.MATERIAL_NO_ENCRYPTION_GMAILERRORRED);
    }
    public static IconData no_flash() {
        return new IconData(FontImage.MATERIAL_NO_FLASH);
    }
    public static IconData no_food() {
        return new IconData(FontImage.MATERIAL_NO_FOOD);
    }
    public static IconData no_luggage() {
        return new IconData(FontImage.MATERIAL_NO_LUGGAGE);
    }
    public static IconData no_meals() {
        return new IconData(FontImage.MATERIAL_NO_MEALS);
    }
    public static IconData no_meals_ouline() {
        return new IconData(FontImage.MATERIAL_NO_MEALS_OULINE);
    }
    public static IconData no_meeting_room() {
        return new IconData(FontImage.MATERIAL_NO_MEETING_ROOM);
    }
    public static IconData no_photography() {
        return new IconData(FontImage.MATERIAL_NO_PHOTOGRAPHY);
    }
    public static IconData no_sim() {
        return new IconData(FontImage.MATERIAL_NO_SIM);
    }
    public static IconData no_stroller() {
        return new IconData(FontImage.MATERIAL_NO_STROLLER);
    }
    public static IconData no_transfer() {
        return new IconData(FontImage.MATERIAL_NO_TRANSFER);
    }
    public static IconData noise_aware() {
        return new IconData(FontImage.MATERIAL_NOISE_AWARE);
    }
    public static IconData noise_control_off() {
        return new IconData(FontImage.MATERIAL_NOISE_CONTROL_OFF);
    }
    public static IconData nordic_walking() {
        return new IconData(FontImage.MATERIAL_NORDIC_WALKING);
    }
    public static IconData north() {
        return new IconData(FontImage.MATERIAL_NORTH);
    }
    public static IconData north_east() {
        return new IconData(FontImage.MATERIAL_NORTH_EAST);
    }
    public static IconData north_west() {
        return new IconData(FontImage.MATERIAL_NORTH_WEST);
    }
    public static IconData not_accessible() {
        return new IconData(FontImage.MATERIAL_NOT_ACCESSIBLE);
    }
    public static IconData not_listed_location() {
        return new IconData(FontImage.MATERIAL_NOT_LISTED_LOCATION);
    }
    public static IconData not_started() {
        return new IconData(FontImage.MATERIAL_NOT_STARTED);
    }
    public static IconData note() {
        return new IconData(FontImage.MATERIAL_NOTE);
    }
    public static IconData note_add() {
        return new IconData(FontImage.MATERIAL_NOTE_ADD);
    }
    public static IconData note_alt() {
        return new IconData(FontImage.MATERIAL_NOTE_ALT);
    }
    public static IconData notes() {
        return new IconData(FontImage.MATERIAL_NOTES);
    }
    public static IconData notification_add() {
        return new IconData(FontImage.MATERIAL_NOTIFICATION_ADD);
    }
    public static IconData notification_important() {
        return new IconData(FontImage.MATERIAL_NOTIFICATION_IMPORTANT);
    }
    public static IconData notifications_active() {
        return new IconData(FontImage.MATERIAL_NOTIFICATIONS_ACTIVE);
    }
    public static IconData notifications_none() {
        return new IconData(FontImage.MATERIAL_NOTIFICATIONS_NONE);
    }
    public static IconData notifications_off() {
        return new IconData(FontImage.MATERIAL_NOTIFICATIONS_OFF);
    }
    public static IconData notifications_on() {
        return new IconData(FontImage.MATERIAL_NOTIFICATIONS_ON);
    }
    public static IconData notifications_paused() {
        return new IconData(FontImage.MATERIAL_NOTIFICATIONS_PAUSED);
    }
    public static IconData now_wallpaper() {
        return new IconData(FontImage.MATERIAL_NOW_WALLPAPER);
    }
    public static IconData now_widgets() {
        return new IconData(FontImage.MATERIAL_NOW_WIDGETS);
    }
    public static IconData numbers() {
        return new IconData(FontImage.MATERIAL_NUMBERS);
    }
    public static IconData offline_bolt() {
        return new IconData(FontImage.MATERIAL_OFFLINE_BOLT);
    }
    public static IconData offline_pin() {
        return new IconData(FontImage.MATERIAL_OFFLINE_PIN);
    }
    public static IconData offline_share() {
        return new IconData(FontImage.MATERIAL_OFFLINE_SHARE);
    }
    public static IconData oil_barrel() {
        return new IconData(FontImage.MATERIAL_OIL_BARREL);
    }
    public static IconData on_device_training() {
        return new IconData(FontImage.MATERIAL_ON_DEVICE_TRAINING);
    }
    public static IconData ondemand_video() {
        return new IconData(FontImage.MATERIAL_ONDEMAND_VIDEO);
    }
    public static IconData online_prediction() {
        return new IconData(FontImage.MATERIAL_ONLINE_PREDICTION);
    }
    public static IconData opacity() {
        return new IconData(FontImage.MATERIAL_OPACITY);
    }
    public static IconData open_in_browser() {
        return new IconData(FontImage.MATERIAL_OPEN_IN_BROWSER);
    }
    public static IconData open_in_full() {
        return new IconData(FontImage.MATERIAL_OPEN_IN_FULL);
    }
    public static IconData open_in_new() {
        return new IconData(FontImage.MATERIAL_OPEN_IN_NEW);
    }
    public static IconData open_in_new_off() {
        return new IconData(FontImage.MATERIAL_OPEN_IN_NEW_OFF);
    }
    public static IconData open_with() {
        return new IconData(FontImage.MATERIAL_OPEN_WITH);
    }
    public static IconData other_houses() {
        return new IconData(FontImage.MATERIAL_OTHER_HOUSES);
    }
    public static IconData outbond() {
        return new IconData(FontImage.MATERIAL_OUTBOND);
    }
    public static IconData outbound() {
        return new IconData(FontImage.MATERIAL_OUTBOUND);
    }
    public static IconData outbox() {
        return new IconData(FontImage.MATERIAL_OUTBOX);
    }
    public static IconData outdoor_grill() {
        return new IconData(FontImage.MATERIAL_OUTDOOR_GRILL);
    }
    public static IconData outgoing_mail() {
        return new IconData(FontImage.MATERIAL_OUTGOING_MAIL);
    }
    public static IconData outlet() {
        return new IconData(FontImage.MATERIAL_OUTLET);
    }
    public static IconData outlined_flag() {
        return new IconData(FontImage.MATERIAL_OUTLINED_FLAG);
    }
    public static IconData output() {
        return new IconData(FontImage.MATERIAL_OUTPUT);
    }
    public static IconData padding() {
        return new IconData(FontImage.MATERIAL_PADDING);
    }
    public static IconData pages() {
        return new IconData(FontImage.MATERIAL_PAGES);
    }
    public static IconData pageview() {
        return new IconData(FontImage.MATERIAL_PAGEVIEW);
    }
    public static IconData paid() {
        return new IconData(FontImage.MATERIAL_PAID);
    }
    public static IconData palette() {
        return new IconData(FontImage.MATERIAL_PALETTE);
    }
    public static IconData pallet() {
        return new IconData(FontImage.MATERIAL_PALLET);
    }
    public static IconData pan_tool() {
        return new IconData(FontImage.MATERIAL_PAN_TOOL);
    }
    public static IconData pan_tool_alt() {
        return new IconData(FontImage.MATERIAL_PAN_TOOL_ALT);
    }
    public static IconData panorama() {
        return new IconData(FontImage.MATERIAL_PANORAMA);
    }
    public static IconData panorama_fish_eye() {
        return new IconData(FontImage.MATERIAL_PANORAMA_FISH_EYE);
    }
    public static IconData panorama_fisheye() {
        return new IconData(FontImage.MATERIAL_PANORAMA_FISHEYE);
    }
    public static IconData panorama_horizontal() {
        return new IconData(FontImage.MATERIAL_PANORAMA_HORIZONTAL);
    }
    public static IconData panorama_horizontal_select() {
        return new IconData(FontImage.MATERIAL_PANORAMA_HORIZONTAL_SELECT);
    }
    public static IconData panorama_photosphere() {
        return new IconData(FontImage.MATERIAL_PANORAMA_PHOTOSPHERE);
    }
    public static IconData panorama_photosphere_select() {
        return new IconData(FontImage.MATERIAL_PANORAMA_PHOTOSPHERE_SELECT);
    }
    public static IconData panorama_vertical() {
        return new IconData(FontImage.MATERIAL_PANORAMA_VERTICAL);
    }
    public static IconData panorama_vertical_select() {
        return new IconData(FontImage.MATERIAL_PANORAMA_VERTICAL_SELECT);
    }
    public static IconData panorama_wide_angle() {
        return new IconData(FontImage.MATERIAL_PANORAMA_WIDE_ANGLE);
    }
    public static IconData panorama_wide_angle_select() {
        return new IconData(FontImage.MATERIAL_PANORAMA_WIDE_ANGLE_SELECT);
    }
    public static IconData paragliding() {
        return new IconData(FontImage.MATERIAL_PARAGLIDING);
    }
    public static IconData park() {
        return new IconData(FontImage.MATERIAL_PARK);
    }
    public static IconData party_mode() {
        return new IconData(FontImage.MATERIAL_PARTY_MODE);
    }
    public static IconData password() {
        return new IconData(FontImage.MATERIAL_PASSWORD);
    }
    public static IconData pattern() {
        return new IconData(FontImage.MATERIAL_PATTERN);
    }
    public static IconData pause() {
        return new IconData(FontImage.MATERIAL_PAUSE);
    }
    public static IconData pause_circle() {
        return new IconData(FontImage.MATERIAL_PAUSE_CIRCLE);
    }
    public static IconData pause_circle_filled() {
        return new IconData(FontImage.MATERIAL_PAUSE_CIRCLE_FILLED);
    }
    public static IconData pause_circle_outline() {
        return new IconData(FontImage.MATERIAL_PAUSE_CIRCLE_OUTLINE);
    }
    public static IconData pause_presentation() {
        return new IconData(FontImage.MATERIAL_PAUSE_PRESENTATION);
    }
    public static IconData payment() {
        return new IconData(FontImage.MATERIAL_PAYMENT);
    }
    public static IconData payments() {
        return new IconData(FontImage.MATERIAL_PAYMENTS);
    }
    public static IconData paypal() {
        return new IconData(FontImage.MATERIAL_PAYPAL);
    }
    public static IconData pedal_bike() {
        return new IconData(FontImage.MATERIAL_PEDAL_BIKE);
    }
    public static IconData pending() {
        return new IconData(FontImage.MATERIAL_PENDING);
    }
    public static IconData pending_actions() {
        return new IconData(FontImage.MATERIAL_PENDING_ACTIONS);
    }
    public static IconData pentagon() {
        return new IconData(FontImage.MATERIAL_PENTAGON);
    }
    public static IconData people() {
        return new IconData(FontImage.MATERIAL_PEOPLE);
    }
    public static IconData people_alt() {
        return new IconData(FontImage.MATERIAL_PEOPLE_ALT);
    }
    public static IconData people_outline() {
        return new IconData(FontImage.MATERIAL_PEOPLE_OUTLINE);
    }
    public static IconData percent() {
        return new IconData(FontImage.MATERIAL_PERCENT);
    }
    public static IconData perm_camera_mic() {
        return new IconData(FontImage.MATERIAL_PERM_CAMERA_MIC);
    }
    public static IconData perm_contact_cal() {
        return new IconData(FontImage.MATERIAL_PERM_CONTACT_CAL);
    }
    public static IconData perm_contact_calendar() {
        return new IconData(FontImage.MATERIAL_PERM_CONTACT_CALENDAR);
    }
    public static IconData perm_data_setting() {
        return new IconData(FontImage.MATERIAL_PERM_DATA_SETTING);
    }
    public static IconData perm_device_info() {
        return new IconData(FontImage.MATERIAL_PERM_DEVICE_INFO);
    }
    public static IconData perm_device_information() {
        return new IconData(FontImage.MATERIAL_PERM_DEVICE_INFORMATION);
    }
    public static IconData perm_identity() {
        return new IconData(FontImage.MATERIAL_PERM_IDENTITY);
    }
    public static IconData perm_media() {
        return new IconData(FontImage.MATERIAL_PERM_MEDIA);
    }
    public static IconData perm_phone_msg() {
        return new IconData(FontImage.MATERIAL_PERM_PHONE_MSG);
    }
    public static IconData perm_scan_wifi() {
        return new IconData(FontImage.MATERIAL_PERM_SCAN_WIFI);
    }
    public static IconData person_2() {
        return new IconData(FontImage.MATERIAL_PERSON_2);
    }
    public static IconData person_3() {
        return new IconData(FontImage.MATERIAL_PERSON_3);
    }
    public static IconData person_4() {
        return new IconData(FontImage.MATERIAL_PERSON_4);
    }
    public static IconData person_add_alt() {
        return new IconData(FontImage.MATERIAL_PERSON_ADD_ALT);
    }
    public static IconData person_add_alt_1() {
        return new IconData(FontImage.MATERIAL_PERSON_ADD_ALT_1);
    }
    public static IconData person_add_disabled() {
        return new IconData(FontImage.MATERIAL_PERSON_ADD_DISABLED);
    }
    public static IconData person_off() {
        return new IconData(FontImage.MATERIAL_PERSON_OFF);
    }
    public static IconData person_outline() {
        return new IconData(FontImage.MATERIAL_PERSON_OUTLINE);
    }
    public static IconData person_pin() {
        return new IconData(FontImage.MATERIAL_PERSON_PIN);
    }
    public static IconData person_pin_circle() {
        return new IconData(FontImage.MATERIAL_PERSON_PIN_CIRCLE);
    }
    public static IconData person_remove() {
        return new IconData(FontImage.MATERIAL_PERSON_REMOVE);
    }
    public static IconData person_remove_alt_1() {
        return new IconData(FontImage.MATERIAL_PERSON_REMOVE_ALT_1);
    }
    public static IconData person_search() {
        return new IconData(FontImage.MATERIAL_PERSON_SEARCH);
    }
    public static IconData personal_injury() {
        return new IconData(FontImage.MATERIAL_PERSONAL_INJURY);
    }
    public static IconData personal_video() {
        return new IconData(FontImage.MATERIAL_PERSONAL_VIDEO);
    }
    public static IconData pest_control() {
        return new IconData(FontImage.MATERIAL_PEST_CONTROL);
    }
    public static IconData pest_control_rodent() {
        return new IconData(FontImage.MATERIAL_PEST_CONTROL_RODENT);
    }
    public static IconData pets() {
        return new IconData(FontImage.MATERIAL_PETS);
    }
    public static IconData phishing() {
        return new IconData(FontImage.MATERIAL_PHISHING);
    }
    public static IconData phone_android() {
        return new IconData(FontImage.MATERIAL_PHONE_ANDROID);
    }
    public static IconData phone_bluetooth_speaker() {
        return new IconData(FontImage.MATERIAL_PHONE_BLUETOOTH_SPEAKER);
    }
    public static IconData phone_callback() {
        return new IconData(FontImage.MATERIAL_PHONE_CALLBACK);
    }
    public static IconData phone_disabled() {
        return new IconData(FontImage.MATERIAL_PHONE_DISABLED);
    }
    public static IconData phone_enabled() {
        return new IconData(FontImage.MATERIAL_PHONE_ENABLED);
    }
    public static IconData phone_forwarded() {
        return new IconData(FontImage.MATERIAL_PHONE_FORWARDED);
    }
    public static IconData phone_in_talk() {
        return new IconData(FontImage.MATERIAL_PHONE_IN_TALK);
    }
    public static IconData phone_iphone() {
        return new IconData(FontImage.MATERIAL_PHONE_IPHONE);
    }
    public static IconData phone_locked() {
        return new IconData(FontImage.MATERIAL_PHONE_LOCKED);
    }
    public static IconData phone_missed() {
        return new IconData(FontImage.MATERIAL_PHONE_MISSED);
    }
    public static IconData phone_paused() {
        return new IconData(FontImage.MATERIAL_PHONE_PAUSED);
    }
    public static IconData phonelink() {
        return new IconData(FontImage.MATERIAL_PHONELINK);
    }
    public static IconData phonelink_erase() {
        return new IconData(FontImage.MATERIAL_PHONELINK_ERASE);
    }
    public static IconData phonelink_lock() {
        return new IconData(FontImage.MATERIAL_PHONELINK_LOCK);
    }
    public static IconData phonelink_off() {
        return new IconData(FontImage.MATERIAL_PHONELINK_OFF);
    }
    public static IconData phonelink_ring() {
        return new IconData(FontImage.MATERIAL_PHONELINK_RING);
    }
    public static IconData phonelink_setup() {
        return new IconData(FontImage.MATERIAL_PHONELINK_SETUP);
    }
    public static IconData photo_album() {
        return new IconData(FontImage.MATERIAL_PHOTO_ALBUM);
    }
    public static IconData photo_camera() {
        return new IconData(FontImage.MATERIAL_PHOTO_CAMERA);
    }
    public static IconData photo_camera_back() {
        return new IconData(FontImage.MATERIAL_PHOTO_CAMERA_BACK);
    }
    public static IconData photo_camera_front() {
        return new IconData(FontImage.MATERIAL_PHOTO_CAMERA_FRONT);
    }
    public static IconData photo_filter() {
        return new IconData(FontImage.MATERIAL_PHOTO_FILTER);
    }
    public static IconData photo_size_select_actual() {
        return new IconData(FontImage.MATERIAL_PHOTO_SIZE_SELECT_ACTUAL);
    }
    public static IconData photo_size_select_large() {
        return new IconData(FontImage.MATERIAL_PHOTO_SIZE_SELECT_LARGE);
    }
    public static IconData photo_size_select_small() {
        return new IconData(FontImage.MATERIAL_PHOTO_SIZE_SELECT_SMALL);
    }
    public static IconData php() {
        return new IconData(FontImage.MATERIAL_PHP);
    }
    public static IconData piano() {
        return new IconData(FontImage.MATERIAL_PIANO);
    }
    public static IconData piano_off() {
        return new IconData(FontImage.MATERIAL_PIANO_OFF);
    }
    public static IconData picture_as_pdf() {
        return new IconData(FontImage.MATERIAL_PICTURE_AS_PDF);
    }
    public static IconData picture_in_picture() {
        return new IconData(FontImage.MATERIAL_PICTURE_IN_PICTURE);
    }
    public static IconData picture_in_picture_alt() {
        return new IconData(FontImage.MATERIAL_PICTURE_IN_PICTURE_ALT);
    }
    public static IconData pie_chart_outline() {
        return new IconData(FontImage.MATERIAL_PIE_CHART_OUTLINE);
    }
    public static IconData pie_chart_outlined() {
        return new IconData(FontImage.MATERIAL_PIE_CHART_OUTLINED);
    }
    public static IconData pin() {
        return new IconData(FontImage.MATERIAL_PIN);
    }
    public static IconData pin_drop() {
        return new IconData(FontImage.MATERIAL_PIN_DROP);
    }
    public static IconData pin_end() {
        return new IconData(FontImage.MATERIAL_PIN_END);
    }
    public static IconData pin_invoke() {
        return new IconData(FontImage.MATERIAL_PIN_INVOKE);
    }
    public static IconData pinch() {
        return new IconData(FontImage.MATERIAL_PINCH);
    }
    public static IconData pivot_table_chart() {
        return new IconData(FontImage.MATERIAL_PIVOT_TABLE_CHART);
    }
    public static IconData pix() {
        return new IconData(FontImage.MATERIAL_PIX);
    }
    public static IconData plagiarism() {
        return new IconData(FontImage.MATERIAL_PLAGIARISM);
    }
    public static IconData play_arrow() {
        return new IconData(FontImage.MATERIAL_PLAY_ARROW);
    }
    public static IconData play_circle() {
        return new IconData(FontImage.MATERIAL_PLAY_CIRCLE);
    }
    public static IconData play_circle_fill() {
        return new IconData(FontImage.MATERIAL_PLAY_CIRCLE_FILL);
    }
    public static IconData play_circle_filled() {
        return new IconData(FontImage.MATERIAL_PLAY_CIRCLE_FILLED);
    }
    public static IconData play_circle_outline() {
        return new IconData(FontImage.MATERIAL_PLAY_CIRCLE_OUTLINE);
    }
    public static IconData play_disabled() {
        return new IconData(FontImage.MATERIAL_PLAY_DISABLED);
    }
    public static IconData play_for_work() {
        return new IconData(FontImage.MATERIAL_PLAY_FOR_WORK);
    }
    public static IconData play_lesson() {
        return new IconData(FontImage.MATERIAL_PLAY_LESSON);
    }
    public static IconData playlist_add() {
        return new IconData(FontImage.MATERIAL_PLAYLIST_ADD);
    }
    public static IconData playlist_add_check() {
        return new IconData(FontImage.MATERIAL_PLAYLIST_ADD_CHECK);
    }
    public static IconData playlist_add_check_circle() {
        return new IconData(FontImage.MATERIAL_PLAYLIST_ADD_CHECK_CIRCLE);
    }
    public static IconData playlist_add_circle() {
        return new IconData(FontImage.MATERIAL_PLAYLIST_ADD_CIRCLE);
    }
    public static IconData playlist_play() {
        return new IconData(FontImage.MATERIAL_PLAYLIST_PLAY);
    }
    public static IconData playlist_remove() {
        return new IconData(FontImage.MATERIAL_PLAYLIST_REMOVE);
    }
    public static IconData plumbing() {
        return new IconData(FontImage.MATERIAL_PLUMBING);
    }
    public static IconData plus_one() {
        return new IconData(FontImage.MATERIAL_PLUS_ONE);
    }
    public static IconData podcasts() {
        return new IconData(FontImage.MATERIAL_PODCASTS);
    }
    public static IconData point_of_sale() {
        return new IconData(FontImage.MATERIAL_POINT_OF_SALE);
    }
    public static IconData policy() {
        return new IconData(FontImage.MATERIAL_POLICY);
    }
    public static IconData poll() {
        return new IconData(FontImage.MATERIAL_POLL);
    }
    public static IconData polyline() {
        return new IconData(FontImage.MATERIAL_POLYLINE);
    }
    public static IconData polymer() {
        return new IconData(FontImage.MATERIAL_POLYMER);
    }
    public static IconData pool() {
        return new IconData(FontImage.MATERIAL_POOL);
    }
    public static IconData portable_wifi_off() {
        return new IconData(FontImage.MATERIAL_PORTABLE_WIFI_OFF);
    }
    public static IconData portrait() {
        return new IconData(FontImage.MATERIAL_PORTRAIT);
    }
    public static IconData post_add() {
        return new IconData(FontImage.MATERIAL_POST_ADD);
    }
    public static IconData power() {
        return new IconData(FontImage.MATERIAL_POWER);
    }
    public static IconData power_input() {
        return new IconData(FontImage.MATERIAL_POWER_INPUT);
    }
    public static IconData power_off() {
        return new IconData(FontImage.MATERIAL_POWER_OFF);
    }
    public static IconData power_settings_new() {
        return new IconData(FontImage.MATERIAL_POWER_SETTINGS_NEW);
    }
    public static IconData precision_manufacturing() {
        return new IconData(FontImage.MATERIAL_PRECISION_MANUFACTURING);
    }
    public static IconData pregnant_woman() {
        return new IconData(FontImage.MATERIAL_PREGNANT_WOMAN);
    }
    public static IconData present_to_all() {
        return new IconData(FontImage.MATERIAL_PRESENT_TO_ALL);
    }
    public static IconData preview() {
        return new IconData(FontImage.MATERIAL_PREVIEW);
    }
    public static IconData price_change() {
        return new IconData(FontImage.MATERIAL_PRICE_CHANGE);
    }
    public static IconData price_check() {
        return new IconData(FontImage.MATERIAL_PRICE_CHECK);
    }
    public static IconData print() {
        return new IconData(FontImage.MATERIAL_PRINT);
    }
    public static IconData print_disabled() {
        return new IconData(FontImage.MATERIAL_PRINT_DISABLED);
    }
    public static IconData priority_high() {
        return new IconData(FontImage.MATERIAL_PRIORITY_HIGH);
    }
    public static IconData privacy_tip() {
        return new IconData(FontImage.MATERIAL_PRIVACY_TIP);
    }
    public static IconData private_connectivity() {
        return new IconData(FontImage.MATERIAL_PRIVATE_CONNECTIVITY);
    }
    public static IconData production_quantity_limits() {
        return new IconData(FontImage.MATERIAL_PRODUCTION_QUANTITY_LIMITS);
    }
    public static IconData propane() {
        return new IconData(FontImage.MATERIAL_PROPANE);
    }
    public static IconData propane_tank() {
        return new IconData(FontImage.MATERIAL_PROPANE_TANK);
    }
    public static IconData psychology() {
        return new IconData(FontImage.MATERIAL_PSYCHOLOGY);
    }
    public static IconData psychology_alt() {
        return new IconData(FontImage.MATERIAL_PSYCHOLOGY_ALT);
    }
    public static IconData public_() {
        return new IconData(FontImage.MATERIAL_PUBLIC);
    }
    public static IconData public_off() {
        return new IconData(FontImage.MATERIAL_PUBLIC_OFF);
    }
    public static IconData publish() {
        return new IconData(FontImage.MATERIAL_PUBLISH);
    }
    public static IconData published_with_changes() {
        return new IconData(FontImage.MATERIAL_PUBLISHED_WITH_CHANGES);
    }
    public static IconData punch_clock() {
        return new IconData(FontImage.MATERIAL_PUNCH_CLOCK);
    }
    public static IconData push_pin() {
        return new IconData(FontImage.MATERIAL_PUSH_PIN);
    }
    public static IconData qr_code() {
        return new IconData(FontImage.MATERIAL_QR_CODE);
    }
    public static IconData qr_code_2() {
        return new IconData(FontImage.MATERIAL_QR_CODE_2);
    }
    public static IconData qr_code_scanner() {
        return new IconData(FontImage.MATERIAL_QR_CODE_SCANNER);
    }
    public static IconData query_builder() {
        return new IconData(FontImage.MATERIAL_QUERY_BUILDER);
    }
    public static IconData query_stats() {
        return new IconData(FontImage.MATERIAL_QUERY_STATS);
    }
    public static IconData question_answer() {
        return new IconData(FontImage.MATERIAL_QUESTION_ANSWER);
    }
    public static IconData question_mark() {
        return new IconData(FontImage.MATERIAL_QUESTION_MARK);
    }
    public static IconData queue() {
        return new IconData(FontImage.MATERIAL_QUEUE);
    }
    public static IconData queue_music() {
        return new IconData(FontImage.MATERIAL_QUEUE_MUSIC);
    }
    public static IconData queue_play_next() {
        return new IconData(FontImage.MATERIAL_QUEUE_PLAY_NEXT);
    }
    public static IconData quick_contacts_dialer() {
        return new IconData(FontImage.MATERIAL_QUICK_CONTACTS_DIALER);
    }
    public static IconData quick_contacts_mail() {
        return new IconData(FontImage.MATERIAL_QUICK_CONTACTS_MAIL);
    }
    public static IconData quickreply() {
        return new IconData(FontImage.MATERIAL_QUICKREPLY);
    }
    public static IconData quiz() {
        return new IconData(FontImage.MATERIAL_QUIZ);
    }
    public static IconData quora() {
        return new IconData(FontImage.MATERIAL_QUORA);
    }
    public static IconData r_mobiledata() {
        return new IconData(FontImage.MATERIAL_R_MOBILEDATA);
    }
    public static IconData radar() {
        return new IconData(FontImage.MATERIAL_RADAR);
    }
    public static IconData radio() {
        return new IconData(FontImage.MATERIAL_RADIO);
    }
    public static IconData radio_button_checked() {
        return new IconData(FontImage.MATERIAL_RADIO_BUTTON_CHECKED);
    }
    public static IconData radio_button_off() {
        return new IconData(FontImage.MATERIAL_RADIO_BUTTON_OFF);
    }
    public static IconData radio_button_on() {
        return new IconData(FontImage.MATERIAL_RADIO_BUTTON_ON);
    }
    public static IconData radio_button_unchecked() {
        return new IconData(FontImage.MATERIAL_RADIO_BUTTON_UNCHECKED);
    }
    public static IconData railway_alert() {
        return new IconData(FontImage.MATERIAL_RAILWAY_ALERT);
    }
    public static IconData ramen_dining() {
        return new IconData(FontImage.MATERIAL_RAMEN_DINING);
    }
    public static IconData ramp_left() {
        return new IconData(FontImage.MATERIAL_RAMP_LEFT);
    }
    public static IconData ramp_right() {
        return new IconData(FontImage.MATERIAL_RAMP_RIGHT);
    }
    public static IconData rate_review() {
        return new IconData(FontImage.MATERIAL_RATE_REVIEW);
    }
    public static IconData raw_off() {
        return new IconData(FontImage.MATERIAL_RAW_OFF);
    }
    public static IconData raw_on() {
        return new IconData(FontImage.MATERIAL_RAW_ON);
    }
    public static IconData read_more() {
        return new IconData(FontImage.MATERIAL_READ_MORE);
    }
    public static IconData real_estate_agent() {
        return new IconData(FontImage.MATERIAL_REAL_ESTATE_AGENT);
    }
    public static IconData rebase_edit() {
        return new IconData(FontImage.MATERIAL_REBASE_EDIT);
    }
    public static IconData receipt() {
        return new IconData(FontImage.MATERIAL_RECEIPT);
    }
    public static IconData receipt_long() {
        return new IconData(FontImage.MATERIAL_RECEIPT_LONG);
    }
    public static IconData recent_actors() {
        return new IconData(FontImage.MATERIAL_RECENT_ACTORS);
    }
    public static IconData recommend() {
        return new IconData(FontImage.MATERIAL_RECOMMEND);
    }
    public static IconData record_voice_over() {
        return new IconData(FontImage.MATERIAL_RECORD_VOICE_OVER);
    }
    public static IconData rectangle() {
        return new IconData(FontImage.MATERIAL_RECTANGLE);
    }
    public static IconData recycling() {
        return new IconData(FontImage.MATERIAL_RECYCLING);
    }
    public static IconData reddit() {
        return new IconData(FontImage.MATERIAL_REDDIT);
    }
    public static IconData redeem() {
        return new IconData(FontImage.MATERIAL_REDEEM);
    }
    public static IconData redo() {
        return new IconData(FontImage.MATERIAL_REDO);
    }
    public static IconData reduce_capacity() {
        return new IconData(FontImage.MATERIAL_REDUCE_CAPACITY);
    }
    public static IconData remember_me() {
        return new IconData(FontImage.MATERIAL_REMEMBER_ME);
    }
    public static IconData remove_circle() {
        return new IconData(FontImage.MATERIAL_REMOVE_CIRCLE);
    }
    public static IconData remove_done() {
        return new IconData(FontImage.MATERIAL_REMOVE_DONE);
    }
    public static IconData remove_from_queue() {
        return new IconData(FontImage.MATERIAL_REMOVE_FROM_QUEUE);
    }
    public static IconData remove_moderator() {
        return new IconData(FontImage.MATERIAL_REMOVE_MODERATOR);
    }
    public static IconData remove_red_eye() {
        return new IconData(FontImage.MATERIAL_REMOVE_RED_EYE);
    }
    public static IconData remove_road() {
        return new IconData(FontImage.MATERIAL_REMOVE_ROAD);
    }
    public static IconData remove_shopping_cart() {
        return new IconData(FontImage.MATERIAL_REMOVE_SHOPPING_CART);
    }
    public static IconData reorder() {
        return new IconData(FontImage.MATERIAL_REORDER);
    }
    public static IconData repartition() {
        return new IconData(FontImage.MATERIAL_REPARTITION);
    }
    public static IconData repeat() {
        return new IconData(FontImage.MATERIAL_REPEAT);
    }
    public static IconData repeat_on() {
        return new IconData(FontImage.MATERIAL_REPEAT_ON);
    }
    public static IconData repeat_one() {
        return new IconData(FontImage.MATERIAL_REPEAT_ONE);
    }
    public static IconData repeat_one_on() {
        return new IconData(FontImage.MATERIAL_REPEAT_ONE_ON);
    }
    public static IconData replay_10() {
        return new IconData(FontImage.MATERIAL_REPLAY_10);
    }
    public static IconData replay_30() {
        return new IconData(FontImage.MATERIAL_REPLAY_30);
    }
    public static IconData replay_5() {
        return new IconData(FontImage.MATERIAL_REPLAY_5);
    }
    public static IconData replay_circle_filled() {
        return new IconData(FontImage.MATERIAL_REPLAY_CIRCLE_FILLED);
    }
    public static IconData reply() {
        return new IconData(FontImage.MATERIAL_REPLY);
    }
    public static IconData report() {
        return new IconData(FontImage.MATERIAL_REPORT);
    }
    public static IconData report_gmailerrorred() {
        return new IconData(FontImage.MATERIAL_REPORT_GMAILERRORRED);
    }
    public static IconData report_off() {
        return new IconData(FontImage.MATERIAL_REPORT_OFF);
    }
    public static IconData report_problem() {
        return new IconData(FontImage.MATERIAL_REPORT_PROBLEM);
    }
    public static IconData request_page() {
        return new IconData(FontImage.MATERIAL_REQUEST_PAGE);
    }
    public static IconData request_quote() {
        return new IconData(FontImage.MATERIAL_REQUEST_QUOTE);
    }
    public static IconData reset_tv() {
        return new IconData(FontImage.MATERIAL_RESET_TV);
    }
    public static IconData restart_alt() {
        return new IconData(FontImage.MATERIAL_RESTART_ALT);
    }
    public static IconData restaurant() {
        return new IconData(FontImage.MATERIAL_RESTAURANT);
    }
    public static IconData restore() {
        return new IconData(FontImage.MATERIAL_RESTORE);
    }
    public static IconData restore_from_trash() {
        return new IconData(FontImage.MATERIAL_RESTORE_FROM_TRASH);
    }
    public static IconData restore_page() {
        return new IconData(FontImage.MATERIAL_RESTORE_PAGE);
    }
    public static IconData reviews() {
        return new IconData(FontImage.MATERIAL_REVIEWS);
    }
    public static IconData rice_bowl() {
        return new IconData(FontImage.MATERIAL_RICE_BOWL);
    }
    public static IconData ring_volume() {
        return new IconData(FontImage.MATERIAL_RING_VOLUME);
    }
    public static IconData rocket() {
        return new IconData(FontImage.MATERIAL_ROCKET);
    }
    public static IconData rocket_launch() {
        return new IconData(FontImage.MATERIAL_ROCKET_LAUNCH);
    }
    public static IconData roller_shades() {
        return new IconData(FontImage.MATERIAL_ROLLER_SHADES);
    }
    public static IconData roller_shades_closed() {
        return new IconData(FontImage.MATERIAL_ROLLER_SHADES_CLOSED);
    }
    public static IconData roller_skating() {
        return new IconData(FontImage.MATERIAL_ROLLER_SKATING);
    }
    public static IconData roofing() {
        return new IconData(FontImage.MATERIAL_ROOFING);
    }
    public static IconData room() {
        return new IconData(FontImage.MATERIAL_ROOM);
    }
    public static IconData room_preferences() {
        return new IconData(FontImage.MATERIAL_ROOM_PREFERENCES);
    }
    public static IconData room_service() {
        return new IconData(FontImage.MATERIAL_ROOM_SERVICE);
    }
    public static IconData rotate_90_degrees_ccw() {
        return new IconData(FontImage.MATERIAL_ROTATE_90_DEGREES_CCW);
    }
    public static IconData rotate_90_degrees_cw() {
        return new IconData(FontImage.MATERIAL_ROTATE_90_DEGREES_CW);
    }
    public static IconData rotate_left() {
        return new IconData(FontImage.MATERIAL_ROTATE_LEFT);
    }
    public static IconData rotate_right() {
        return new IconData(FontImage.MATERIAL_ROTATE_RIGHT);
    }
    public static IconData roundabout_left() {
        return new IconData(FontImage.MATERIAL_ROUNDABOUT_LEFT);
    }
    public static IconData roundabout_right() {
        return new IconData(FontImage.MATERIAL_ROUNDABOUT_RIGHT);
    }
    public static IconData rounded_corner() {
        return new IconData(FontImage.MATERIAL_ROUNDED_CORNER);
    }
    public static IconData route() {
        return new IconData(FontImage.MATERIAL_ROUTE);
    }
    public static IconData router() {
        return new IconData(FontImage.MATERIAL_ROUTER);
    }
    public static IconData rowing() {
        return new IconData(FontImage.MATERIAL_ROWING);
    }
    public static IconData rss_feed() {
        return new IconData(FontImage.MATERIAL_RSS_FEED);
    }
    public static IconData rsvp() {
        return new IconData(FontImage.MATERIAL_RSVP);
    }
    public static IconData rtt() {
        return new IconData(FontImage.MATERIAL_RTT);
    }
    public static IconData rule() {
        return new IconData(FontImage.MATERIAL_RULE);
    }
    public static IconData rule_folder() {
        return new IconData(FontImage.MATERIAL_RULE_FOLDER);
    }
    public static IconData run_circle() {
        return new IconData(FontImage.MATERIAL_RUN_CIRCLE);
    }
    public static IconData running_with_errors() {
        return new IconData(FontImage.MATERIAL_RUNNING_WITH_ERRORS);
    }
    public static IconData rv_hookup() {
        return new IconData(FontImage.MATERIAL_RV_HOOKUP);
    }
    public static IconData safety_check() {
        return new IconData(FontImage.MATERIAL_SAFETY_CHECK);
    }
    public static IconData safety_divider() {
        return new IconData(FontImage.MATERIAL_SAFETY_DIVIDER);
    }
    public static IconData sailing() {
        return new IconData(FontImage.MATERIAL_SAILING);
    }
    public static IconData sanitizer() {
        return new IconData(FontImage.MATERIAL_SANITIZER);
    }
    public static IconData satellite() {
        return new IconData(FontImage.MATERIAL_SATELLITE);
    }
    public static IconData satellite_alt() {
        return new IconData(FontImage.MATERIAL_SATELLITE_ALT);
    }
    public static IconData save() {
        return new IconData(FontImage.MATERIAL_SAVE);
    }
    public static IconData save_alt() {
        return new IconData(FontImage.MATERIAL_SAVE_ALT);
    }
    public static IconData save_as() {
        return new IconData(FontImage.MATERIAL_SAVE_AS);
    }
    public static IconData saved_search() {
        return new IconData(FontImage.MATERIAL_SAVED_SEARCH);
    }
    public static IconData savings() {
        return new IconData(FontImage.MATERIAL_SAVINGS);
    }
    public static IconData scale() {
        return new IconData(FontImage.MATERIAL_SCALE);
    }
    public static IconData scanner() {
        return new IconData(FontImage.MATERIAL_SCANNER);
    }
    public static IconData scatter_plot() {
        return new IconData(FontImage.MATERIAL_SCATTER_PLOT);
    }
    public static IconData schedule() {
        return new IconData(FontImage.MATERIAL_SCHEDULE);
    }
    public static IconData schedule_send() {
        return new IconData(FontImage.MATERIAL_SCHEDULE_SEND);
    }
    public static IconData schema() {
        return new IconData(FontImage.MATERIAL_SCHEMA);
    }
    public static IconData school() {
        return new IconData(FontImage.MATERIAL_SCHOOL);
    }
    public static IconData science() {
        return new IconData(FontImage.MATERIAL_SCIENCE);
    }
    public static IconData score() {
        return new IconData(FontImage.MATERIAL_SCORE);
    }
    public static IconData scoreboard() {
        return new IconData(FontImage.MATERIAL_SCOREBOARD);
    }
    public static IconData screen_lock_landscape() {
        return new IconData(FontImage.MATERIAL_SCREEN_LOCK_LANDSCAPE);
    }
    public static IconData screen_lock_portrait() {
        return new IconData(FontImage.MATERIAL_SCREEN_LOCK_PORTRAIT);
    }
    public static IconData screen_lock_rotation() {
        return new IconData(FontImage.MATERIAL_SCREEN_LOCK_ROTATION);
    }
    public static IconData screen_rotation() {
        return new IconData(FontImage.MATERIAL_SCREEN_ROTATION);
    }
    public static IconData screen_rotation_alt() {
        return new IconData(FontImage.MATERIAL_SCREEN_ROTATION_ALT);
    }
    public static IconData screen_search_desktop() {
        return new IconData(FontImage.MATERIAL_SCREEN_SEARCH_DESKTOP);
    }
    public static IconData screen_share() {
        return new IconData(FontImage.MATERIAL_SCREEN_SHARE);
    }
    public static IconData screenshot() {
        return new IconData(FontImage.MATERIAL_SCREENSHOT);
    }
    public static IconData screenshot_monitor() {
        return new IconData(FontImage.MATERIAL_SCREENSHOT_MONITOR);
    }
    public static IconData scuba_diving() {
        return new IconData(FontImage.MATERIAL_SCUBA_DIVING);
    }
    public static IconData sd() {
        return new IconData(FontImage.MATERIAL_SD);
    }
    public static IconData sd_card() {
        return new IconData(FontImage.MATERIAL_SD_CARD);
    }
    public static IconData sd_card_alert() {
        return new IconData(FontImage.MATERIAL_SD_CARD_ALERT);
    }
    public static IconData sd_storage() {
        return new IconData(FontImage.MATERIAL_SD_STORAGE);
    }
    public static IconData search_off() {
        return new IconData(FontImage.MATERIAL_SEARCH_OFF);
    }
    public static IconData security_update() {
        return new IconData(FontImage.MATERIAL_SECURITY_UPDATE);
    }
    public static IconData security_update_good() {
        return new IconData(FontImage.MATERIAL_SECURITY_UPDATE_GOOD);
    }
    public static IconData security_update_warning() {
        return new IconData(FontImage.MATERIAL_SECURITY_UPDATE_WARNING);
    }
    public static IconData segment() {
        return new IconData(FontImage.MATERIAL_SEGMENT);
    }
    public static IconData select_all() {
        return new IconData(FontImage.MATERIAL_SELECT_ALL);
    }
    public static IconData self_improvement() {
        return new IconData(FontImage.MATERIAL_SELF_IMPROVEMENT);
    }
    public static IconData sell() {
        return new IconData(FontImage.MATERIAL_SELL);
    }
    public static IconData send() {
        return new IconData(FontImage.MATERIAL_SEND);
    }
    public static IconData send_and_archive() {
        return new IconData(FontImage.MATERIAL_SEND_AND_ARCHIVE);
    }
    public static IconData send_time_extension() {
        return new IconData(FontImage.MATERIAL_SEND_TIME_EXTENSION);
    }
    public static IconData send_to_mobile() {
        return new IconData(FontImage.MATERIAL_SEND_TO_MOBILE);
    }
    public static IconData sensor_door() {
        return new IconData(FontImage.MATERIAL_SENSOR_DOOR);
    }
    public static IconData sensor_occupied() {
        return new IconData(FontImage.MATERIAL_SENSOR_OCCUPIED);
    }
    public static IconData sensor_window() {
        return new IconData(FontImage.MATERIAL_SENSOR_WINDOW);
    }
    public static IconData sensors() {
        return new IconData(FontImage.MATERIAL_SENSORS);
    }
    public static IconData sensors_off() {
        return new IconData(FontImage.MATERIAL_SENSORS_OFF);
    }
    public static IconData sentiment_dissatisfied() {
        return new IconData(FontImage.MATERIAL_SENTIMENT_DISSATISFIED);
    }
    public static IconData sentiment_neutral() {
        return new IconData(FontImage.MATERIAL_SENTIMENT_NEUTRAL);
    }
    public static IconData sentiment_satisfied() {
        return new IconData(FontImage.MATERIAL_SENTIMENT_SATISFIED);
    }
    public static IconData sentiment_satisfied_alt() {
        return new IconData(FontImage.MATERIAL_SENTIMENT_SATISFIED_ALT);
    }
    public static IconData sentiment_very_dissatisfied() {
        return new IconData(FontImage.MATERIAL_SENTIMENT_VERY_DISSATISFIED);
    }
    public static IconData sentiment_very_satisfied() {
        return new IconData(FontImage.MATERIAL_SENTIMENT_VERY_SATISFIED);
    }
    public static IconData set_meal() {
        return new IconData(FontImage.MATERIAL_SET_MEAL);
    }
    public static IconData settings_accessibility() {
        return new IconData(FontImage.MATERIAL_SETTINGS_ACCESSIBILITY);
    }
    public static IconData settings_applications() {
        return new IconData(FontImage.MATERIAL_SETTINGS_APPLICATIONS);
    }
    public static IconData settings_backup_restore() {
        return new IconData(FontImage.MATERIAL_SETTINGS_BACKUP_RESTORE);
    }
    public static IconData settings_bluetooth() {
        return new IconData(FontImage.MATERIAL_SETTINGS_BLUETOOTH);
    }
    public static IconData settings_brightness() {
        return new IconData(FontImage.MATERIAL_SETTINGS_BRIGHTNESS);
    }
    public static IconData settings_cell() {
        return new IconData(FontImage.MATERIAL_SETTINGS_CELL);
    }
    public static IconData settings_display() {
        return new IconData(FontImage.MATERIAL_SETTINGS_DISPLAY);
    }
    public static IconData settings_ethernet() {
        return new IconData(FontImage.MATERIAL_SETTINGS_ETHERNET);
    }
    public static IconData settings_input_antenna() {
        return new IconData(FontImage.MATERIAL_SETTINGS_INPUT_ANTENNA);
    }
    public static IconData settings_input_component() {
        return new IconData(FontImage.MATERIAL_SETTINGS_INPUT_COMPONENT);
    }
    public static IconData settings_input_composite() {
        return new IconData(FontImage.MATERIAL_SETTINGS_INPUT_COMPOSITE);
    }
    public static IconData settings_input_hdmi() {
        return new IconData(FontImage.MATERIAL_SETTINGS_INPUT_HDMI);
    }
    public static IconData settings_input_svideo() {
        return new IconData(FontImage.MATERIAL_SETTINGS_INPUT_SVIDEO);
    }
    public static IconData settings_overscan() {
        return new IconData(FontImage.MATERIAL_SETTINGS_OVERSCAN);
    }
    public static IconData settings_phone() {
        return new IconData(FontImage.MATERIAL_SETTINGS_PHONE);
    }
    public static IconData settings_power() {
        return new IconData(FontImage.MATERIAL_SETTINGS_POWER);
    }
    public static IconData settings_remote() {
        return new IconData(FontImage.MATERIAL_SETTINGS_REMOTE);
    }
    public static IconData settings_suggest() {
        return new IconData(FontImage.MATERIAL_SETTINGS_SUGGEST);
    }
    public static IconData settings_system_daydream() {
        return new IconData(FontImage.MATERIAL_SETTINGS_SYSTEM_DAYDREAM);
    }
    public static IconData settings_voice() {
        return new IconData(FontImage.MATERIAL_SETTINGS_VOICE);
    }
    public static IconData severe_cold() {
        return new IconData(FontImage.MATERIAL_SEVERE_COLD);
    }
    public static IconData shape_line() {
        return new IconData(FontImage.MATERIAL_SHAPE_LINE);
    }
    public static IconData share_arrival_time() {
        return new IconData(FontImage.MATERIAL_SHARE_ARRIVAL_TIME);
    }
    public static IconData share_location() {
        return new IconData(FontImage.MATERIAL_SHARE_LOCATION);
    }
    public static IconData shelves() {
        return new IconData(FontImage.MATERIAL_SHELVES);
    }
    public static IconData shield() {
        return new IconData(FontImage.MATERIAL_SHIELD);
    }
    public static IconData shield_moon() {
        return new IconData(FontImage.MATERIAL_SHIELD_MOON);
    }
    public static IconData shop() {
        return new IconData(FontImage.MATERIAL_SHOP);
    }
    public static IconData shop_2() {
        return new IconData(FontImage.MATERIAL_SHOP_2);
    }
    public static IconData shop_two() {
        return new IconData(FontImage.MATERIAL_SHOP_TWO);
    }
    public static IconData shopify() {
        return new IconData(FontImage.MATERIAL_SHOPIFY);
    }
    public static IconData shopping_bag() {
        return new IconData(FontImage.MATERIAL_SHOPPING_BAG);
    }
    public static IconData shopping_basket() {
        return new IconData(FontImage.MATERIAL_SHOPPING_BASKET);
    }
    public static IconData shopping_cart_checkout() {
        return new IconData(FontImage.MATERIAL_SHOPPING_CART_CHECKOUT);
    }
    public static IconData short_text() {
        return new IconData(FontImage.MATERIAL_SHORT_TEXT);
    }
    public static IconData shortcut() {
        return new IconData(FontImage.MATERIAL_SHORTCUT);
    }
    public static IconData show_chart() {
        return new IconData(FontImage.MATERIAL_SHOW_CHART);
    }
    public static IconData shower() {
        return new IconData(FontImage.MATERIAL_SHOWER);
    }
    public static IconData shuffle() {
        return new IconData(FontImage.MATERIAL_SHUFFLE);
    }
    public static IconData shuffle_on() {
        return new IconData(FontImage.MATERIAL_SHUFFLE_ON);
    }
    public static IconData shutter_speed() {
        return new IconData(FontImage.MATERIAL_SHUTTER_SPEED);
    }
    public static IconData sick() {
        return new IconData(FontImage.MATERIAL_SICK);
    }
    public static IconData sign_language() {
        return new IconData(FontImage.MATERIAL_SIGN_LANGUAGE);
    }
    public static IconData signal_cellular_0_bar() {
        return new IconData(FontImage.MATERIAL_SIGNAL_CELLULAR_0_BAR);
    }
    public static IconData signal_cellular_4_bar() {
        return new IconData(FontImage.MATERIAL_SIGNAL_CELLULAR_4_BAR);
    }
    public static IconData signal_cellular_alt() {
        return new IconData(FontImage.MATERIAL_SIGNAL_CELLULAR_ALT);
    }
    public static IconData signal_cellular_alt_1_bar() {
        return new IconData(FontImage.MATERIAL_SIGNAL_CELLULAR_ALT_1_BAR);
    }
    public static IconData signal_cellular_alt_2_bar() {
        return new IconData(FontImage.MATERIAL_SIGNAL_CELLULAR_ALT_2_BAR);
    }
    public static IconData signal_cellular_connected_no_internet_0_bar() {
        return new IconData(FontImage.MATERIAL_SIGNAL_CELLULAR_CONNECTED_NO_INTERNET_0_BAR);
    }
    public static IconData signal_cellular_connected_no_internet_4_bar() {
        return new IconData(FontImage.MATERIAL_SIGNAL_CELLULAR_CONNECTED_NO_INTERNET_4_BAR);
    }
    public static IconData signal_cellular_no_sim() {
        return new IconData(FontImage.MATERIAL_SIGNAL_CELLULAR_NO_SIM);
    }
    public static IconData signal_cellular_nodata() {
        return new IconData(FontImage.MATERIAL_SIGNAL_CELLULAR_NODATA);
    }
    public static IconData signal_cellular_null() {
        return new IconData(FontImage.MATERIAL_SIGNAL_CELLULAR_NULL);
    }
    public static IconData signal_cellular_off() {
        return new IconData(FontImage.MATERIAL_SIGNAL_CELLULAR_OFF);
    }
    public static IconData signal_wifi_0_bar() {
        return new IconData(FontImage.MATERIAL_SIGNAL_WIFI_0_BAR);
    }
    public static IconData signal_wifi_4_bar() {
        return new IconData(FontImage.MATERIAL_SIGNAL_WIFI_4_BAR);
    }
    public static IconData signal_wifi_4_bar_lock() {
        return new IconData(FontImage.MATERIAL_SIGNAL_WIFI_4_BAR_LOCK);
    }
    public static IconData signal_wifi_bad() {
        return new IconData(FontImage.MATERIAL_SIGNAL_WIFI_BAD);
    }
    public static IconData signal_wifi_connected_no_internet_4() {
        return new IconData(FontImage.MATERIAL_SIGNAL_WIFI_CONNECTED_NO_INTERNET_4);
    }
    public static IconData signal_wifi_off() {
        return new IconData(FontImage.MATERIAL_SIGNAL_WIFI_OFF);
    }
    public static IconData signal_wifi_statusbar_4_bar() {
        return new IconData(FontImage.MATERIAL_SIGNAL_WIFI_STATUSBAR_4_BAR);
    }
    public static IconData signal_wifi_statusbar_connected_no_internet_4() {
        return new IconData(FontImage.MATERIAL_SIGNAL_WIFI_STATUSBAR_CONNECTED_NO_INTERNET_4);
    }
    public static IconData signal_wifi_statusbar_null() {
        return new IconData(FontImage.MATERIAL_SIGNAL_WIFI_STATUSBAR_NULL);
    }
    public static IconData signpost() {
        return new IconData(FontImage.MATERIAL_SIGNPOST);
    }
    public static IconData sim_card() {
        return new IconData(FontImage.MATERIAL_SIM_CARD);
    }
    public static IconData sim_card_alert() {
        return new IconData(FontImage.MATERIAL_SIM_CARD_ALERT);
    }
    public static IconData sim_card_download() {
        return new IconData(FontImage.MATERIAL_SIM_CARD_DOWNLOAD);
    }
    public static IconData single_bed() {
        return new IconData(FontImage.MATERIAL_SINGLE_BED);
    }
    public static IconData sip() {
        return new IconData(FontImage.MATERIAL_SIP);
    }
    public static IconData skateboarding() {
        return new IconData(FontImage.MATERIAL_SKATEBOARDING);
    }
    public static IconData skip_next() {
        return new IconData(FontImage.MATERIAL_SKIP_NEXT);
    }
    public static IconData skip_previous() {
        return new IconData(FontImage.MATERIAL_SKIP_PREVIOUS);
    }
    public static IconData sledding() {
        return new IconData(FontImage.MATERIAL_SLEDDING);
    }
    public static IconData slideshow() {
        return new IconData(FontImage.MATERIAL_SLIDESHOW);
    }
    public static IconData slow_motion_video() {
        return new IconData(FontImage.MATERIAL_SLOW_MOTION_VIDEO);
    }
    public static IconData smart_button() {
        return new IconData(FontImage.MATERIAL_SMART_BUTTON);
    }
    public static IconData smart_display() {
        return new IconData(FontImage.MATERIAL_SMART_DISPLAY);
    }
    public static IconData smart_screen() {
        return new IconData(FontImage.MATERIAL_SMART_SCREEN);
    }
    public static IconData smart_toy() {
        return new IconData(FontImage.MATERIAL_SMART_TOY);
    }
    public static IconData smartphone() {
        return new IconData(FontImage.MATERIAL_SMARTPHONE);
    }
    public static IconData smoke_free() {
        return new IconData(FontImage.MATERIAL_SMOKE_FREE);
    }
    public static IconData smoking_rooms() {
        return new IconData(FontImage.MATERIAL_SMOKING_ROOMS);
    }
    public static IconData sms() {
        return new IconData(FontImage.MATERIAL_SMS);
    }
    public static IconData sms_failed() {
        return new IconData(FontImage.MATERIAL_SMS_FAILED);
    }
    public static IconData snapchat() {
        return new IconData(FontImage.MATERIAL_SNAPCHAT);
    }
    public static IconData snippet_folder() {
        return new IconData(FontImage.MATERIAL_SNIPPET_FOLDER);
    }
    public static IconData snooze() {
        return new IconData(FontImage.MATERIAL_SNOOZE);
    }
    public static IconData snowboarding() {
        return new IconData(FontImage.MATERIAL_SNOWBOARDING);
    }
    public static IconData snowing() {
        return new IconData(FontImage.MATERIAL_SNOWING);
    }
    public static IconData snowmobile() {
        return new IconData(FontImage.MATERIAL_SNOWMOBILE);
    }
    public static IconData snowshoeing() {
        return new IconData(FontImage.MATERIAL_SNOWSHOEING);
    }
    public static IconData soap() {
        return new IconData(FontImage.MATERIAL_SOAP);
    }
    public static IconData social_distance() {
        return new IconData(FontImage.MATERIAL_SOCIAL_DISTANCE);
    }
    public static IconData solar_power() {
        return new IconData(FontImage.MATERIAL_SOLAR_POWER);
    }
    public static IconData sort_by_alpha() {
        return new IconData(FontImage.MATERIAL_SORT_BY_ALPHA);
    }
    public static IconData sos() {
        return new IconData(FontImage.MATERIAL_SOS);
    }
    public static IconData soup_kitchen() {
        return new IconData(FontImage.MATERIAL_SOUP_KITCHEN);
    }
    public static IconData source() {
        return new IconData(FontImage.MATERIAL_SOURCE);
    }
    public static IconData south() {
        return new IconData(FontImage.MATERIAL_SOUTH);
    }
    public static IconData south_america() {
        return new IconData(FontImage.MATERIAL_SOUTH_AMERICA);
    }
    public static IconData south_east() {
        return new IconData(FontImage.MATERIAL_SOUTH_EAST);
    }
    public static IconData south_west() {
        return new IconData(FontImage.MATERIAL_SOUTH_WEST);
    }
    public static IconData spa() {
        return new IconData(FontImage.MATERIAL_SPA);
    }
    public static IconData space_bar() {
        return new IconData(FontImage.MATERIAL_SPACE_BAR);
    }
    public static IconData space_dashboard() {
        return new IconData(FontImage.MATERIAL_SPACE_DASHBOARD);
    }
    public static IconData spatial_audio() {
        return new IconData(FontImage.MATERIAL_SPATIAL_AUDIO);
    }
    public static IconData spatial_audio_off() {
        return new IconData(FontImage.MATERIAL_SPATIAL_AUDIO_OFF);
    }
    public static IconData spatial_tracking() {
        return new IconData(FontImage.MATERIAL_SPATIAL_TRACKING);
    }
    public static IconData speaker() {
        return new IconData(FontImage.MATERIAL_SPEAKER);
    }
    public static IconData speaker_group() {
        return new IconData(FontImage.MATERIAL_SPEAKER_GROUP);
    }
    public static IconData speaker_notes() {
        return new IconData(FontImage.MATERIAL_SPEAKER_NOTES);
    }
    public static IconData speaker_notes_off() {
        return new IconData(FontImage.MATERIAL_SPEAKER_NOTES_OFF);
    }
    public static IconData speaker_phone() {
        return new IconData(FontImage.MATERIAL_SPEAKER_PHONE);
    }
    public static IconData speed() {
        return new IconData(FontImage.MATERIAL_SPEED);
    }
    public static IconData spellcheck() {
        return new IconData(FontImage.MATERIAL_SPELLCHECK);
    }
    public static IconData splitscreen() {
        return new IconData(FontImage.MATERIAL_SPLITSCREEN);
    }
    public static IconData spoke() {
        return new IconData(FontImage.MATERIAL_SPOKE);
    }
    public static IconData sports() {
        return new IconData(FontImage.MATERIAL_SPORTS);
    }
    public static IconData sports_bar() {
        return new IconData(FontImage.MATERIAL_SPORTS_BAR);
    }
    public static IconData sports_baseball() {
        return new IconData(FontImage.MATERIAL_SPORTS_BASEBALL);
    }
    public static IconData sports_basketball() {
        return new IconData(FontImage.MATERIAL_SPORTS_BASKETBALL);
    }
    public static IconData sports_cricket() {
        return new IconData(FontImage.MATERIAL_SPORTS_CRICKET);
    }
    public static IconData sports_esports() {
        return new IconData(FontImage.MATERIAL_SPORTS_ESPORTS);
    }
    public static IconData sports_football() {
        return new IconData(FontImage.MATERIAL_SPORTS_FOOTBALL);
    }
    public static IconData sports_golf() {
        return new IconData(FontImage.MATERIAL_SPORTS_GOLF);
    }
    public static IconData sports_gymnastics() {
        return new IconData(FontImage.MATERIAL_SPORTS_GYMNASTICS);
    }
    public static IconData sports_handball() {
        return new IconData(FontImage.MATERIAL_SPORTS_HANDBALL);
    }
    public static IconData sports_hockey() {
        return new IconData(FontImage.MATERIAL_SPORTS_HOCKEY);
    }
    public static IconData sports_kabaddi() {
        return new IconData(FontImage.MATERIAL_SPORTS_KABADDI);
    }
    public static IconData sports_martial_arts() {
        return new IconData(FontImage.MATERIAL_SPORTS_MARTIAL_ARTS);
    }
    public static IconData sports_mma() {
        return new IconData(FontImage.MATERIAL_SPORTS_MMA);
    }
    public static IconData sports_motorsports() {
        return new IconData(FontImage.MATERIAL_SPORTS_MOTORSPORTS);
    }
    public static IconData sports_rugby() {
        return new IconData(FontImage.MATERIAL_SPORTS_RUGBY);
    }
    public static IconData sports_score() {
        return new IconData(FontImage.MATERIAL_SPORTS_SCORE);
    }
    public static IconData sports_soccer() {
        return new IconData(FontImage.MATERIAL_SPORTS_SOCCER);
    }
    public static IconData sports_tennis() {
        return new IconData(FontImage.MATERIAL_SPORTS_TENNIS);
    }
    public static IconData sports_volleyball() {
        return new IconData(FontImage.MATERIAL_SPORTS_VOLLEYBALL);
    }
    public static IconData square() {
        return new IconData(FontImage.MATERIAL_SQUARE);
    }
    public static IconData square_foot() {
        return new IconData(FontImage.MATERIAL_SQUARE_FOOT);
    }
    public static IconData ssid_chart() {
        return new IconData(FontImage.MATERIAL_SSID_CHART);
    }
    public static IconData stacked_bar_chart() {
        return new IconData(FontImage.MATERIAL_STACKED_BAR_CHART);
    }
    public static IconData stacked_line_chart() {
        return new IconData(FontImage.MATERIAL_STACKED_LINE_CHART);
    }
    public static IconData stadium() {
        return new IconData(FontImage.MATERIAL_STADIUM);
    }
    public static IconData stairs() {
        return new IconData(FontImage.MATERIAL_STAIRS);
    }
    public static IconData star_border_purple500() {
        return new IconData(FontImage.MATERIAL_STAR_BORDER_PURPLE500);
    }
    public static IconData star_half() {
        return new IconData(FontImage.MATERIAL_STAR_HALF);
    }
    public static IconData star_outline() {
        return new IconData(FontImage.MATERIAL_STAR_OUTLINE);
    }
    public static IconData star_purple500() {
        return new IconData(FontImage.MATERIAL_STAR_PURPLE500);
    }
    public static IconData star_rate() {
        return new IconData(FontImage.MATERIAL_STAR_RATE);
    }
    public static IconData stars() {
        return new IconData(FontImage.MATERIAL_STARS);
    }
    public static IconData start() {
        return new IconData(FontImage.MATERIAL_START);
    }
    public static IconData stay_current_landscape() {
        return new IconData(FontImage.MATERIAL_STAY_CURRENT_LANDSCAPE);
    }
    public static IconData stay_current_portrait() {
        return new IconData(FontImage.MATERIAL_STAY_CURRENT_PORTRAIT);
    }
    public static IconData stay_primary_landscape() {
        return new IconData(FontImage.MATERIAL_STAY_PRIMARY_LANDSCAPE);
    }
    public static IconData stay_primary_portrait() {
        return new IconData(FontImage.MATERIAL_STAY_PRIMARY_PORTRAIT);
    }
    public static IconData sticky_note_2() {
        return new IconData(FontImage.MATERIAL_STICKY_NOTE_2);
    }
    public static IconData stop() {
        return new IconData(FontImage.MATERIAL_STOP);
    }
    public static IconData stop_circle() {
        return new IconData(FontImage.MATERIAL_STOP_CIRCLE);
    }
    public static IconData stop_screen_share() {
        return new IconData(FontImage.MATERIAL_STOP_SCREEN_SHARE);
    }
    public static IconData storage() {
        return new IconData(FontImage.MATERIAL_STORAGE);
    }
    public static IconData store() {
        return new IconData(FontImage.MATERIAL_STORE);
    }
    public static IconData store_mall_directory() {
        return new IconData(FontImage.MATERIAL_STORE_MALL_DIRECTORY);
    }
    public static IconData storefront() {
        return new IconData(FontImage.MATERIAL_STOREFRONT);
    }
    public static IconData storm() {
        return new IconData(FontImage.MATERIAL_STORM);
    }
    public static IconData straight() {
        return new IconData(FontImage.MATERIAL_STRAIGHT);
    }
    public static IconData straighten() {
        return new IconData(FontImage.MATERIAL_STRAIGHTEN);
    }
    public static IconData stream() {
        return new IconData(FontImage.MATERIAL_STREAM);
    }
    public static IconData streetview() {
        return new IconData(FontImage.MATERIAL_STREETVIEW);
    }
    public static IconData strikethrough_s() {
        return new IconData(FontImage.MATERIAL_STRIKETHROUGH_S);
    }
    public static IconData stroller() {
        return new IconData(FontImage.MATERIAL_STROLLER);
    }
    public static IconData style() {
        return new IconData(FontImage.MATERIAL_STYLE);
    }
    public static IconData subdirectory_arrow_left() {
        return new IconData(FontImage.MATERIAL_SUBDIRECTORY_ARROW_LEFT);
    }
    public static IconData subdirectory_arrow_right() {
        return new IconData(FontImage.MATERIAL_SUBDIRECTORY_ARROW_RIGHT);
    }
    public static IconData subject() {
        return new IconData(FontImage.MATERIAL_SUBJECT);
    }
    public static IconData subscript() {
        return new IconData(FontImage.MATERIAL_SUBSCRIPT);
    }
    public static IconData subscriptions() {
        return new IconData(FontImage.MATERIAL_SUBSCRIPTIONS);
    }
    public static IconData subtitles() {
        return new IconData(FontImage.MATERIAL_SUBTITLES);
    }
    public static IconData subtitles_off() {
        return new IconData(FontImage.MATERIAL_SUBTITLES_OFF);
    }
    public static IconData subway() {
        return new IconData(FontImage.MATERIAL_SUBWAY);
    }
    public static IconData summarize() {
        return new IconData(FontImage.MATERIAL_SUMMARIZE);
    }
    public static IconData sunny() {
        return new IconData(FontImage.MATERIAL_SUNNY);
    }
    public static IconData sunny_snowing() {
        return new IconData(FontImage.MATERIAL_SUNNY_SNOWING);
    }
    public static IconData superscript() {
        return new IconData(FontImage.MATERIAL_SUPERSCRIPT);
    }
    public static IconData supervised_user_circle() {
        return new IconData(FontImage.MATERIAL_SUPERVISED_USER_CIRCLE);
    }
    public static IconData supervisor_account() {
        return new IconData(FontImage.MATERIAL_SUPERVISOR_ACCOUNT);
    }
    public static IconData support() {
        return new IconData(FontImage.MATERIAL_SUPPORT);
    }
    public static IconData support_agent() {
        return new IconData(FontImage.MATERIAL_SUPPORT_AGENT);
    }
    public static IconData surfing() {
        return new IconData(FontImage.MATERIAL_SURFING);
    }
    public static IconData surround_sound() {
        return new IconData(FontImage.MATERIAL_SURROUND_SOUND);
    }
    public static IconData swap_calls() {
        return new IconData(FontImage.MATERIAL_SWAP_CALLS);
    }
    public static IconData swap_horiz() {
        return new IconData(FontImage.MATERIAL_SWAP_HORIZ);
    }
    public static IconData swap_horizontal_circle() {
        return new IconData(FontImage.MATERIAL_SWAP_HORIZONTAL_CIRCLE);
    }
    public static IconData swap_vert() {
        return new IconData(FontImage.MATERIAL_SWAP_VERT);
    }
    public static IconData swap_vert_circle() {
        return new IconData(FontImage.MATERIAL_SWAP_VERT_CIRCLE);
    }
    public static IconData swap_vertical_circle() {
        return new IconData(FontImage.MATERIAL_SWAP_VERTICAL_CIRCLE);
    }
    public static IconData swipe() {
        return new IconData(FontImage.MATERIAL_SWIPE);
    }
    public static IconData swipe_down() {
        return new IconData(FontImage.MATERIAL_SWIPE_DOWN);
    }
    public static IconData swipe_down_alt() {
        return new IconData(FontImage.MATERIAL_SWIPE_DOWN_ALT);
    }
    public static IconData swipe_left() {
        return new IconData(FontImage.MATERIAL_SWIPE_LEFT);
    }
    public static IconData swipe_left_alt() {
        return new IconData(FontImage.MATERIAL_SWIPE_LEFT_ALT);
    }
    public static IconData swipe_right() {
        return new IconData(FontImage.MATERIAL_SWIPE_RIGHT);
    }
    public static IconData swipe_right_alt() {
        return new IconData(FontImage.MATERIAL_SWIPE_RIGHT_ALT);
    }
    public static IconData swipe_up() {
        return new IconData(FontImage.MATERIAL_SWIPE_UP);
    }
    public static IconData swipe_up_alt() {
        return new IconData(FontImage.MATERIAL_SWIPE_UP_ALT);
    }
    public static IconData swipe_vertical() {
        return new IconData(FontImage.MATERIAL_SWIPE_VERTICAL);
    }
    public static IconData switch_access_shortcut() {
        return new IconData(FontImage.MATERIAL_SWITCH_ACCESS_SHORTCUT);
    }
    public static IconData switch_access_shortcut_add() {
        return new IconData(FontImage.MATERIAL_SWITCH_ACCESS_SHORTCUT_ADD);
    }
    public static IconData switch_account() {
        return new IconData(FontImage.MATERIAL_SWITCH_ACCOUNT);
    }
    public static IconData switch_camera() {
        return new IconData(FontImage.MATERIAL_SWITCH_CAMERA);
    }
    public static IconData switch_left() {
        return new IconData(FontImage.MATERIAL_SWITCH_LEFT);
    }
    public static IconData switch_right() {
        return new IconData(FontImage.MATERIAL_SWITCH_RIGHT);
    }
    public static IconData switch_video() {
        return new IconData(FontImage.MATERIAL_SWITCH_VIDEO);
    }
    public static IconData synagogue() {
        return new IconData(FontImage.MATERIAL_SYNAGOGUE);
    }
    public static IconData sync() {
        return new IconData(FontImage.MATERIAL_SYNC);
    }
    public static IconData sync_alt() {
        return new IconData(FontImage.MATERIAL_SYNC_ALT);
    }
    public static IconData sync_disabled() {
        return new IconData(FontImage.MATERIAL_SYNC_DISABLED);
    }
    public static IconData sync_lock() {
        return new IconData(FontImage.MATERIAL_SYNC_LOCK);
    }
    public static IconData sync_problem() {
        return new IconData(FontImage.MATERIAL_SYNC_PROBLEM);
    }
    public static IconData system_security_update() {
        return new IconData(FontImage.MATERIAL_SYSTEM_SECURITY_UPDATE);
    }
    public static IconData system_security_update_good() {
        return new IconData(FontImage.MATERIAL_SYSTEM_SECURITY_UPDATE_GOOD);
    }
    public static IconData system_security_update_warning() {
        return new IconData(FontImage.MATERIAL_SYSTEM_SECURITY_UPDATE_WARNING);
    }
    public static IconData system_update() {
        return new IconData(FontImage.MATERIAL_SYSTEM_UPDATE);
    }
    public static IconData system_update_alt() {
        return new IconData(FontImage.MATERIAL_SYSTEM_UPDATE_ALT);
    }
    public static IconData system_update_tv() {
        return new IconData(FontImage.MATERIAL_SYSTEM_UPDATE_TV);
    }
    public static IconData tab() {
        return new IconData(FontImage.MATERIAL_TAB);
    }
    public static IconData tab_unselected() {
        return new IconData(FontImage.MATERIAL_TAB_UNSELECTED);
    }
    public static IconData table_bar() {
        return new IconData(FontImage.MATERIAL_TABLE_BAR);
    }
    public static IconData table_restaurant() {
        return new IconData(FontImage.MATERIAL_TABLE_RESTAURANT);
    }
    public static IconData table_rows() {
        return new IconData(FontImage.MATERIAL_TABLE_ROWS);
    }
    public static IconData table_view() {
        return new IconData(FontImage.MATERIAL_TABLE_VIEW);
    }
    public static IconData tablet() {
        return new IconData(FontImage.MATERIAL_TABLET);
    }
    public static IconData tablet_android() {
        return new IconData(FontImage.MATERIAL_TABLET_ANDROID);
    }
    public static IconData tablet_mac() {
        return new IconData(FontImage.MATERIAL_TABLET_MAC);
    }
    public static IconData tag() {
        return new IconData(FontImage.MATERIAL_TAG);
    }
    public static IconData tag_faces() {
        return new IconData(FontImage.MATERIAL_TAG_FACES);
    }
    public static IconData takeout_dining() {
        return new IconData(FontImage.MATERIAL_TAKEOUT_DINING);
    }
    public static IconData tap_and_play() {
        return new IconData(FontImage.MATERIAL_TAP_AND_PLAY);
    }
    public static IconData tapas() {
        return new IconData(FontImage.MATERIAL_TAPAS);
    }
    public static IconData task() {
        return new IconData(FontImage.MATERIAL_TASK);
    }
    public static IconData task_alt() {
        return new IconData(FontImage.MATERIAL_TASK_ALT);
    }
    public static IconData taxi_alert() {
        return new IconData(FontImage.MATERIAL_TAXI_ALERT);
    }
    public static IconData telegram() {
        return new IconData(FontImage.MATERIAL_TELEGRAM);
    }
    public static IconData temple_buddhist() {
        return new IconData(FontImage.MATERIAL_TEMPLE_BUDDHIST);
    }
    public static IconData temple_hindu() {
        return new IconData(FontImage.MATERIAL_TEMPLE_HINDU);
    }
    public static IconData terminal() {
        return new IconData(FontImage.MATERIAL_TERMINAL);
    }
    public static IconData terrain() {
        return new IconData(FontImage.MATERIAL_TERRAIN);
    }
    public static IconData text_decrease() {
        return new IconData(FontImage.MATERIAL_TEXT_DECREASE);
    }
    public static IconData text_fields() {
        return new IconData(FontImage.MATERIAL_TEXT_FIELDS);
    }
    public static IconData text_format() {
        return new IconData(FontImage.MATERIAL_TEXT_FORMAT);
    }
    public static IconData text_increase() {
        return new IconData(FontImage.MATERIAL_TEXT_INCREASE);
    }
    public static IconData text_rotate_up() {
        return new IconData(FontImage.MATERIAL_TEXT_ROTATE_UP);
    }
    public static IconData text_rotate_vertical() {
        return new IconData(FontImage.MATERIAL_TEXT_ROTATE_VERTICAL);
    }
    public static IconData text_rotation_angledown() {
        return new IconData(FontImage.MATERIAL_TEXT_ROTATION_ANGLEDOWN);
    }
    public static IconData text_rotation_angleup() {
        return new IconData(FontImage.MATERIAL_TEXT_ROTATION_ANGLEUP);
    }
    public static IconData text_rotation_down() {
        return new IconData(FontImage.MATERIAL_TEXT_ROTATION_DOWN);
    }
    public static IconData text_rotation_none() {
        return new IconData(FontImage.MATERIAL_TEXT_ROTATION_NONE);
    }
    public static IconData text_snippet() {
        return new IconData(FontImage.MATERIAL_TEXT_SNIPPET);
    }
    public static IconData textsms() {
        return new IconData(FontImage.MATERIAL_TEXTSMS);
    }
    public static IconData texture() {
        return new IconData(FontImage.MATERIAL_TEXTURE);
    }
    public static IconData theater_comedy() {
        return new IconData(FontImage.MATERIAL_THEATER_COMEDY);
    }
    public static IconData theaters() {
        return new IconData(FontImage.MATERIAL_THEATERS);
    }
    public static IconData thermostat() {
        return new IconData(FontImage.MATERIAL_THERMOSTAT);
    }
    public static IconData thermostat_auto() {
        return new IconData(FontImage.MATERIAL_THERMOSTAT_AUTO);
    }
    public static IconData thumb_down() {
        return new IconData(FontImage.MATERIAL_THUMB_DOWN);
    }
    public static IconData thumb_down_alt() {
        return new IconData(FontImage.MATERIAL_THUMB_DOWN_ALT);
    }
    public static IconData thumb_down_off_alt() {
        return new IconData(FontImage.MATERIAL_THUMB_DOWN_OFF_ALT);
    }
    public static IconData thumb_up() {
        return new IconData(FontImage.MATERIAL_THUMB_UP);
    }
    public static IconData thumb_up_alt() {
        return new IconData(FontImage.MATERIAL_THUMB_UP_ALT);
    }
    public static IconData thumb_up_off_alt() {
        return new IconData(FontImage.MATERIAL_THUMB_UP_OFF_ALT);
    }
    public static IconData thumbs_up_down() {
        return new IconData(FontImage.MATERIAL_THUMBS_UP_DOWN);
    }
    public static IconData thunderstorm() {
        return new IconData(FontImage.MATERIAL_THUNDERSTORM);
    }
    public static IconData tiktok() {
        return new IconData(FontImage.MATERIAL_TIKTOK);
    }
    public static IconData time_to_leave() {
        return new IconData(FontImage.MATERIAL_TIME_TO_LEAVE);
    }
    public static IconData timelapse() {
        return new IconData(FontImage.MATERIAL_TIMELAPSE);
    }
    public static IconData timeline() {
        return new IconData(FontImage.MATERIAL_TIMELINE);
    }
    public static IconData timer() {
        return new IconData(FontImage.MATERIAL_TIMER);
    }
    public static IconData timer_10() {
        return new IconData(FontImage.MATERIAL_TIMER_10);
    }
    public static IconData timer_10_select() {
        return new IconData(FontImage.MATERIAL_TIMER_10_SELECT);
    }
    public static IconData timer_3() {
        return new IconData(FontImage.MATERIAL_TIMER_3);
    }
    public static IconData timer_3_select() {
        return new IconData(FontImage.MATERIAL_TIMER_3_SELECT);
    }
    public static IconData timer_off() {
        return new IconData(FontImage.MATERIAL_TIMER_OFF);
    }
    public static IconData tips_and_updates() {
        return new IconData(FontImage.MATERIAL_TIPS_AND_UPDATES);
    }
    public static IconData tire_repair() {
        return new IconData(FontImage.MATERIAL_TIRE_REPAIR);
    }
    public static IconData title() {
        return new IconData(FontImage.MATERIAL_TITLE);
    }
    public static IconData toc() {
        return new IconData(FontImage.MATERIAL_TOC);
    }
    public static IconData today() {
        return new IconData(FontImage.MATERIAL_TODAY);
    }
    public static IconData toggle_off() {
        return new IconData(FontImage.MATERIAL_TOGGLE_OFF);
    }
    public static IconData toggle_on() {
        return new IconData(FontImage.MATERIAL_TOGGLE_ON);
    }
    public static IconData token() {
        return new IconData(FontImage.MATERIAL_TOKEN);
    }
    public static IconData toll() {
        return new IconData(FontImage.MATERIAL_TOLL);
    }
    public static IconData tonality() {
        return new IconData(FontImage.MATERIAL_TONALITY);
    }
    public static IconData topic() {
        return new IconData(FontImage.MATERIAL_TOPIC);
    }
    public static IconData tornado() {
        return new IconData(FontImage.MATERIAL_TORNADO);
    }
    public static IconData touch_app() {
        return new IconData(FontImage.MATERIAL_TOUCH_APP);
    }
    public static IconData tour() {
        return new IconData(FontImage.MATERIAL_TOUR);
    }
    public static IconData toys() {
        return new IconData(FontImage.MATERIAL_TOYS);
    }
    public static IconData track_changes() {
        return new IconData(FontImage.MATERIAL_TRACK_CHANGES);
    }
    public static IconData traffic() {
        return new IconData(FontImage.MATERIAL_TRAFFIC);
    }
    public static IconData train() {
        return new IconData(FontImage.MATERIAL_TRAIN);
    }
    public static IconData tram() {
        return new IconData(FontImage.MATERIAL_TRAM);
    }
    public static IconData transcribe() {
        return new IconData(FontImage.MATERIAL_TRANSCRIBE);
    }
    public static IconData transfer_within_a_station() {
        return new IconData(FontImage.MATERIAL_TRANSFER_WITHIN_A_STATION);
    }
    public static IconData transform() {
        return new IconData(FontImage.MATERIAL_TRANSFORM);
    }
    public static IconData transgender() {
        return new IconData(FontImage.MATERIAL_TRANSGENDER);
    }
    public static IconData transit_enterexit() {
        return new IconData(FontImage.MATERIAL_TRANSIT_ENTEREXIT);
    }
    public static IconData translate() {
        return new IconData(FontImage.MATERIAL_TRANSLATE);
    }
    public static IconData travel_explore() {
        return new IconData(FontImage.MATERIAL_TRAVEL_EXPLORE);
    }
    public static IconData trending_down() {
        return new IconData(FontImage.MATERIAL_TRENDING_DOWN);
    }
    public static IconData trending_flat() {
        return new IconData(FontImage.MATERIAL_TRENDING_FLAT);
    }
    public static IconData trending_neutral() {
        return new IconData(FontImage.MATERIAL_TRENDING_NEUTRAL);
    }
    public static IconData trending_up() {
        return new IconData(FontImage.MATERIAL_TRENDING_UP);
    }
    public static IconData trip_origin() {
        return new IconData(FontImage.MATERIAL_TRIP_ORIGIN);
    }
    public static IconData trolley() {
        return new IconData(FontImage.MATERIAL_TROLLEY);
    }
    public static IconData troubleshoot() {
        return new IconData(FontImage.MATERIAL_TROUBLESHOOT);
    }
    public static IconData try_() {
        return new IconData(FontImage.MATERIAL_TRY);
    }
    public static IconData tsunami() {
        return new IconData(FontImage.MATERIAL_TSUNAMI);
    }
    public static IconData tty() {
        return new IconData(FontImage.MATERIAL_TTY);
    }
    public static IconData tungsten() {
        return new IconData(FontImage.MATERIAL_TUNGSTEN);
    }
    public static IconData turn_left() {
        return new IconData(FontImage.MATERIAL_TURN_LEFT);
    }
    public static IconData turn_right() {
        return new IconData(FontImage.MATERIAL_TURN_RIGHT);
    }
    public static IconData turn_sharp_left() {
        return new IconData(FontImage.MATERIAL_TURN_SHARP_LEFT);
    }
    public static IconData turn_sharp_right() {
        return new IconData(FontImage.MATERIAL_TURN_SHARP_RIGHT);
    }
    public static IconData turn_slight_left() {
        return new IconData(FontImage.MATERIAL_TURN_SLIGHT_LEFT);
    }
    public static IconData turn_slight_right() {
        return new IconData(FontImage.MATERIAL_TURN_SLIGHT_RIGHT);
    }
    public static IconData turned_in() {
        return new IconData(FontImage.MATERIAL_TURNED_IN);
    }
    public static IconData turned_in_not() {
        return new IconData(FontImage.MATERIAL_TURNED_IN_NOT);
    }
    public static IconData tv() {
        return new IconData(FontImage.MATERIAL_TV);
    }
    public static IconData tv_off() {
        return new IconData(FontImage.MATERIAL_TV_OFF);
    }
    public static IconData two_wheeler() {
        return new IconData(FontImage.MATERIAL_TWO_WHEELER);
    }
    public static IconData type_specimen() {
        return new IconData(FontImage.MATERIAL_TYPE_SPECIMEN);
    }
    public static IconData u_turn_left() {
        return new IconData(FontImage.MATERIAL_U_TURN_LEFT);
    }
    public static IconData u_turn_right() {
        return new IconData(FontImage.MATERIAL_U_TURN_RIGHT);
    }
    public static IconData umbrella() {
        return new IconData(FontImage.MATERIAL_UMBRELLA);
    }
    public static IconData unarchive() {
        return new IconData(FontImage.MATERIAL_UNARCHIVE);
    }
    public static IconData undo() {
        return new IconData(FontImage.MATERIAL_UNDO);
    }
    public static IconData unfold_less() {
        return new IconData(FontImage.MATERIAL_UNFOLD_LESS);
    }
    public static IconData unfold_less_double() {
        return new IconData(FontImage.MATERIAL_UNFOLD_LESS_DOUBLE);
    }
    public static IconData unfold_more() {
        return new IconData(FontImage.MATERIAL_UNFOLD_MORE);
    }
    public static IconData unfold_more_double() {
        return new IconData(FontImage.MATERIAL_UNFOLD_MORE_DOUBLE);
    }
    public static IconData unpublished() {
        return new IconData(FontImage.MATERIAL_UNPUBLISHED);
    }
    public static IconData unsubscribe() {
        return new IconData(FontImage.MATERIAL_UNSUBSCRIBE);
    }
    public static IconData upcoming() {
        return new IconData(FontImage.MATERIAL_UPCOMING);
    }
    public static IconData update() {
        return new IconData(FontImage.MATERIAL_UPDATE);
    }
    public static IconData update_disabled() {
        return new IconData(FontImage.MATERIAL_UPDATE_DISABLED);
    }
    public static IconData upgrade() {
        return new IconData(FontImage.MATERIAL_UPGRADE);
    }
    public static IconData upload() {
        return new IconData(FontImage.MATERIAL_UPLOAD);
    }
    public static IconData upload_file() {
        return new IconData(FontImage.MATERIAL_UPLOAD_FILE);
    }
    public static IconData usb() {
        return new IconData(FontImage.MATERIAL_USB);
    }
    public static IconData usb_off() {
        return new IconData(FontImage.MATERIAL_USB_OFF);
    }
    public static IconData vaccines() {
        return new IconData(FontImage.MATERIAL_VACCINES);
    }
    public static IconData vape_free() {
        return new IconData(FontImage.MATERIAL_VAPE_FREE);
    }
    public static IconData vaping_rooms() {
        return new IconData(FontImage.MATERIAL_VAPING_ROOMS);
    }
    public static IconData verified() {
        return new IconData(FontImage.MATERIAL_VERIFIED);
    }
    public static IconData verified_user() {
        return new IconData(FontImage.MATERIAL_VERIFIED_USER);
    }
    public static IconData vertical_align_bottom() {
        return new IconData(FontImage.MATERIAL_VERTICAL_ALIGN_BOTTOM);
    }
    public static IconData vertical_align_center() {
        return new IconData(FontImage.MATERIAL_VERTICAL_ALIGN_CENTER);
    }
    public static IconData vertical_align_top() {
        return new IconData(FontImage.MATERIAL_VERTICAL_ALIGN_TOP);
    }
    public static IconData vertical_distribute() {
        return new IconData(FontImage.MATERIAL_VERTICAL_DISTRIBUTE);
    }
    public static IconData vertical_shades() {
        return new IconData(FontImage.MATERIAL_VERTICAL_SHADES);
    }
    public static IconData vertical_shades_closed() {
        return new IconData(FontImage.MATERIAL_VERTICAL_SHADES_CLOSED);
    }
    public static IconData vibration() {
        return new IconData(FontImage.MATERIAL_VIBRATION);
    }
    public static IconData video_call() {
        return new IconData(FontImage.MATERIAL_VIDEO_CALL);
    }
    public static IconData video_camera_back() {
        return new IconData(FontImage.MATERIAL_VIDEO_CAMERA_BACK);
    }
    public static IconData video_camera_front() {
        return new IconData(FontImage.MATERIAL_VIDEO_CAMERA_FRONT);
    }
    public static IconData video_chat() {
        return new IconData(FontImage.MATERIAL_VIDEO_CHAT);
    }
    public static IconData video_collection() {
        return new IconData(FontImage.MATERIAL_VIDEO_COLLECTION);
    }
    public static IconData video_file() {
        return new IconData(FontImage.MATERIAL_VIDEO_FILE);
    }
    public static IconData video_label() {
        return new IconData(FontImage.MATERIAL_VIDEO_LABEL);
    }
    public static IconData video_library() {
        return new IconData(FontImage.MATERIAL_VIDEO_LIBRARY);
    }
    public static IconData video_settings() {
        return new IconData(FontImage.MATERIAL_VIDEO_SETTINGS);
    }
    public static IconData video_stable() {
        return new IconData(FontImage.MATERIAL_VIDEO_STABLE);
    }
    public static IconData videocam() {
        return new IconData(FontImage.MATERIAL_VIDEOCAM);
    }
    public static IconData videocam_off() {
        return new IconData(FontImage.MATERIAL_VIDEOCAM_OFF);
    }
    public static IconData videogame_asset() {
        return new IconData(FontImage.MATERIAL_VIDEOGAME_ASSET);
    }
    public static IconData videogame_asset_off() {
        return new IconData(FontImage.MATERIAL_VIDEOGAME_ASSET_OFF);
    }
    public static IconData view_agenda() {
        return new IconData(FontImage.MATERIAL_VIEW_AGENDA);
    }
    public static IconData view_array() {
        return new IconData(FontImage.MATERIAL_VIEW_ARRAY);
    }
    public static IconData view_carousel() {
        return new IconData(FontImage.MATERIAL_VIEW_CAROUSEL);
    }
    public static IconData view_column() {
        return new IconData(FontImage.MATERIAL_VIEW_COLUMN);
    }
    public static IconData view_comfortable() {
        return new IconData(FontImage.MATERIAL_VIEW_COMFORTABLE);
    }
    public static IconData view_comfy() {
        return new IconData(FontImage.MATERIAL_VIEW_COMFY);
    }
    public static IconData view_comfy_alt() {
        return new IconData(FontImage.MATERIAL_VIEW_COMFY_ALT);
    }
    public static IconData view_compact() {
        return new IconData(FontImage.MATERIAL_VIEW_COMPACT);
    }
    public static IconData view_compact_alt() {
        return new IconData(FontImage.MATERIAL_VIEW_COMPACT_ALT);
    }
    public static IconData view_cozy() {
        return new IconData(FontImage.MATERIAL_VIEW_COZY);
    }
    public static IconData view_day() {
        return new IconData(FontImage.MATERIAL_VIEW_DAY);
    }
    public static IconData view_headline() {
        return new IconData(FontImage.MATERIAL_VIEW_HEADLINE);
    }
    public static IconData view_in_ar() {
        return new IconData(FontImage.MATERIAL_VIEW_IN_AR);
    }
    public static IconData view_kanban() {
        return new IconData(FontImage.MATERIAL_VIEW_KANBAN);
    }
    public static IconData view_list() {
        return new IconData(FontImage.MATERIAL_VIEW_LIST);
    }
    public static IconData view_module() {
        return new IconData(FontImage.MATERIAL_VIEW_MODULE);
    }
    public static IconData view_quilt() {
        return new IconData(FontImage.MATERIAL_VIEW_QUILT);
    }
    public static IconData view_sidebar() {
        return new IconData(FontImage.MATERIAL_VIEW_SIDEBAR);
    }
    public static IconData view_stream() {
        return new IconData(FontImage.MATERIAL_VIEW_STREAM);
    }
    public static IconData view_timeline() {
        return new IconData(FontImage.MATERIAL_VIEW_TIMELINE);
    }
    public static IconData view_week() {
        return new IconData(FontImage.MATERIAL_VIEW_WEEK);
    }
    public static IconData vignette() {
        return new IconData(FontImage.MATERIAL_VIGNETTE);
    }
    public static IconData villa() {
        return new IconData(FontImage.MATERIAL_VILLA);
    }
    public static IconData voice_chat() {
        return new IconData(FontImage.MATERIAL_VOICE_CHAT);
    }
    public static IconData voice_over_off() {
        return new IconData(FontImage.MATERIAL_VOICE_OVER_OFF);
    }
    public static IconData voicemail() {
        return new IconData(FontImage.MATERIAL_VOICEMAIL);
    }
    public static IconData volcano() {
        return new IconData(FontImage.MATERIAL_VOLCANO);
    }
    public static IconData volume_down() {
        return new IconData(FontImage.MATERIAL_VOLUME_DOWN);
    }
    public static IconData volume_down_alt() {
        return new IconData(FontImage.MATERIAL_VOLUME_DOWN_ALT);
    }
    public static IconData volume_mute() {
        return new IconData(FontImage.MATERIAL_VOLUME_MUTE);
    }
    public static IconData volume_off() {
        return new IconData(FontImage.MATERIAL_VOLUME_OFF);
    }
    public static IconData volume_up() {
        return new IconData(FontImage.MATERIAL_VOLUME_UP);
    }
    public static IconData volunteer_activism() {
        return new IconData(FontImage.MATERIAL_VOLUNTEER_ACTIVISM);
    }
    public static IconData vpn_key() {
        return new IconData(FontImage.MATERIAL_VPN_KEY);
    }
    public static IconData vpn_key_off() {
        return new IconData(FontImage.MATERIAL_VPN_KEY_OFF);
    }
    public static IconData vpn_lock() {
        return new IconData(FontImage.MATERIAL_VPN_LOCK);
    }
    public static IconData vrpano() {
        return new IconData(FontImage.MATERIAL_VRPANO);
    }
    public static IconData wallet() {
        return new IconData(FontImage.MATERIAL_WALLET);
    }
    public static IconData wallet_giftcard() {
        return new IconData(FontImage.MATERIAL_WALLET_GIFTCARD);
    }
    public static IconData wallet_membership() {
        return new IconData(FontImage.MATERIAL_WALLET_MEMBERSHIP);
    }
    public static IconData wallet_travel() {
        return new IconData(FontImage.MATERIAL_WALLET_TRAVEL);
    }
    public static IconData wallpaper() {
        return new IconData(FontImage.MATERIAL_WALLPAPER);
    }
    public static IconData warehouse() {
        return new IconData(FontImage.MATERIAL_WAREHOUSE);
    }
    public static IconData warning() {
        return new IconData(FontImage.MATERIAL_WARNING);
    }
    public static IconData warning_amber() {
        return new IconData(FontImage.MATERIAL_WARNING_AMBER);
    }
    public static IconData wash() {
        return new IconData(FontImage.MATERIAL_WASH);
    }
    public static IconData watch() {
        return new IconData(FontImage.MATERIAL_WATCH);
    }
    public static IconData watch_later() {
        return new IconData(FontImage.MATERIAL_WATCH_LATER);
    }
    public static IconData watch_off() {
        return new IconData(FontImage.MATERIAL_WATCH_OFF);
    }
    public static IconData water() {
        return new IconData(FontImage.MATERIAL_WATER);
    }
    public static IconData water_damage() {
        return new IconData(FontImage.MATERIAL_WATER_DAMAGE);
    }
    public static IconData water_drop() {
        return new IconData(FontImage.MATERIAL_WATER_DROP);
    }
    public static IconData waterfall_chart() {
        return new IconData(FontImage.MATERIAL_WATERFALL_CHART);
    }
    public static IconData waves() {
        return new IconData(FontImage.MATERIAL_WAVES);
    }
    public static IconData waving_hand() {
        return new IconData(FontImage.MATERIAL_WAVING_HAND);
    }
    public static IconData wb_auto() {
        return new IconData(FontImage.MATERIAL_WB_AUTO);
    }
    public static IconData wb_cloudy() {
        return new IconData(FontImage.MATERIAL_WB_CLOUDY);
    }
    public static IconData wb_incandescent() {
        return new IconData(FontImage.MATERIAL_WB_INCANDESCENT);
    }
    public static IconData wb_iridescent() {
        return new IconData(FontImage.MATERIAL_WB_IRIDESCENT);
    }
    public static IconData wb_shade() {
        return new IconData(FontImage.MATERIAL_WB_SHADE);
    }
    public static IconData wb_sunny() {
        return new IconData(FontImage.MATERIAL_WB_SUNNY);
    }
    public static IconData wb_twighlight() {
        return new IconData(FontImage.MATERIAL_WB_TWIGHLIGHT);
    }
    public static IconData wb_twilight() {
        return new IconData(FontImage.MATERIAL_WB_TWILIGHT);
    }
    public static IconData wc() {
        return new IconData(FontImage.MATERIAL_WC);
    }
    public static IconData web() {
        return new IconData(FontImage.MATERIAL_WEB);
    }
    public static IconData web_asset_off() {
        return new IconData(FontImage.MATERIAL_WEB_ASSET_OFF);
    }
    public static IconData web_stories() {
        return new IconData(FontImage.MATERIAL_WEB_STORIES);
    }
    public static IconData webhook() {
        return new IconData(FontImage.MATERIAL_WEBHOOK);
    }
    public static IconData wechat() {
        return new IconData(FontImage.MATERIAL_WECHAT);
    }
    public static IconData weekend() {
        return new IconData(FontImage.MATERIAL_WEEKEND);
    }
    public static IconData west() {
        return new IconData(FontImage.MATERIAL_WEST);
    }
    public static IconData whatshot() {
        return new IconData(FontImage.MATERIAL_WHATSHOT);
    }
    public static IconData wheelchair_pickup() {
        return new IconData(FontImage.MATERIAL_WHEELCHAIR_PICKUP);
    }
    public static IconData where_to_vote() {
        return new IconData(FontImage.MATERIAL_WHERE_TO_VOTE);
    }
    public static IconData widgets() {
        return new IconData(FontImage.MATERIAL_WIDGETS);
    }
    public static IconData width_full() {
        return new IconData(FontImage.MATERIAL_WIDTH_FULL);
    }
    public static IconData width_normal() {
        return new IconData(FontImage.MATERIAL_WIDTH_NORMAL);
    }
    public static IconData width_wide() {
        return new IconData(FontImage.MATERIAL_WIDTH_WIDE);
    }
    public static IconData wifi() {
        return new IconData(FontImage.MATERIAL_WIFI);
    }
    public static IconData wifi_1_bar() {
        return new IconData(FontImage.MATERIAL_WIFI_1_BAR);
    }
    public static IconData wifi_2_bar() {
        return new IconData(FontImage.MATERIAL_WIFI_2_BAR);
    }
    public static IconData wifi_calling() {
        return new IconData(FontImage.MATERIAL_WIFI_CALLING);
    }
    public static IconData wifi_calling_3() {
        return new IconData(FontImage.MATERIAL_WIFI_CALLING_3);
    }
    public static IconData wifi_channel() {
        return new IconData(FontImage.MATERIAL_WIFI_CHANNEL);
    }
    public static IconData wifi_find() {
        return new IconData(FontImage.MATERIAL_WIFI_FIND);
    }
    public static IconData wifi_lock() {
        return new IconData(FontImage.MATERIAL_WIFI_LOCK);
    }
    public static IconData wifi_off() {
        return new IconData(FontImage.MATERIAL_WIFI_OFF);
    }
    public static IconData wifi_password() {
        return new IconData(FontImage.MATERIAL_WIFI_PASSWORD);
    }
    public static IconData wifi_protected_setup() {
        return new IconData(FontImage.MATERIAL_WIFI_PROTECTED_SETUP);
    }
    public static IconData wifi_tethering() {
        return new IconData(FontImage.MATERIAL_WIFI_TETHERING);
    }
    public static IconData wifi_tethering_error() {
        return new IconData(FontImage.MATERIAL_WIFI_TETHERING_ERROR);
    }
    public static IconData wifi_tethering_error_rounded() {
        return new IconData(FontImage.MATERIAL_WIFI_TETHERING_ERROR_ROUNDED);
    }
    public static IconData wifi_tethering_off() {
        return new IconData(FontImage.MATERIAL_WIFI_TETHERING_OFF);
    }
    public static IconData wind_power() {
        return new IconData(FontImage.MATERIAL_WIND_POWER);
    }
    public static IconData window() {
        return new IconData(FontImage.MATERIAL_WINDOW);
    }
    public static IconData wine_bar() {
        return new IconData(FontImage.MATERIAL_WINE_BAR);
    }
    public static IconData woman() {
        return new IconData(FontImage.MATERIAL_WOMAN);
    }
    public static IconData woman_2() {
        return new IconData(FontImage.MATERIAL_WOMAN_2);
    }
    public static IconData woo_commerce() {
        return new IconData(FontImage.MATERIAL_WOO_COMMERCE);
    }
    public static IconData wordpress() {
        return new IconData(FontImage.MATERIAL_WORDPRESS);
    }
    public static IconData work() {
        return new IconData(FontImage.MATERIAL_WORK);
    }
    public static IconData work_history() {
        return new IconData(FontImage.MATERIAL_WORK_HISTORY);
    }
    public static IconData work_off() {
        return new IconData(FontImage.MATERIAL_WORK_OFF);
    }
    public static IconData work_outline() {
        return new IconData(FontImage.MATERIAL_WORK_OUTLINE);
    }
    public static IconData workspace_premium() {
        return new IconData(FontImage.MATERIAL_WORKSPACE_PREMIUM);
    }
    public static IconData workspaces() {
        return new IconData(FontImage.MATERIAL_WORKSPACES);
    }
    public static IconData workspaces_filled() {
        return new IconData(FontImage.MATERIAL_WORKSPACES_FILLED);
    }
    public static IconData workspaces_outline() {
        return new IconData(FontImage.MATERIAL_WORKSPACES_OUTLINE);
    }
    public static IconData wrap_text() {
        return new IconData(FontImage.MATERIAL_WRAP_TEXT);
    }
    public static IconData wrong_location() {
        return new IconData(FontImage.MATERIAL_WRONG_LOCATION);
    }
    public static IconData wysiwyg() {
        return new IconData(FontImage.MATERIAL_WYSIWYG);
    }
    public static IconData yard() {
        return new IconData(FontImage.MATERIAL_YARD);
    }
    public static IconData youtube_searched_for() {
        return new IconData(FontImage.MATERIAL_YOUTUBE_SEARCHED_FOR);
    }
    public static IconData zoom_in() {
        return new IconData(FontImage.MATERIAL_ZOOM_IN);
    }
    public static IconData zoom_in_map() {
        return new IconData(FontImage.MATERIAL_ZOOM_IN_MAP);
    }
    public static IconData zoom_out() {
        return new IconData(FontImage.MATERIAL_ZOOM_OUT);
    }
    public static IconData zoom_out_map() {
        return new IconData(FontImage.MATERIAL_ZOOM_OUT_MAP);
    }
}
