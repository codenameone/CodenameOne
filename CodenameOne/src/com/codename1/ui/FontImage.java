/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import com.codename1.components.MultiButton;
import com.codename1.components.SpanButton;
import com.codename1.components.SpanLabel;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.ImageIO;

/**
 * <p>{@code FontImage} allows using an icon font as if it was an image. You can specify the character, color and 
 * size and then treat the `FontImage` as if its a regular image. The huge benefits are that the font image can 
 * adapt to platform conventions in terms of color and easily scale to adapt to DPI.</p>
 *
 * <p>You can generate icon fonts using free tools on the Internet such as 
 * <a href="http://fontello.com/">this</a>. Icon fonts are a remarkably simple and powerful technique to 
 * create a small, modern applications.</p>
 * 
 * <p>Icon fonts can be created in 2 basic ways the first is explicitly by defining all of the elements within the font:</p>
 * 
 * <script src="https://gist.github.com/codenameone/9c881350e1d142081aba.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/graphics-fontimage-fixed.png" alt="Icon font from material design icons created with the fixed size of display width" />
 * 
 * <p>The second approach uses a {@link com.codename1.ui.plaf.Style} object thru either {@link #create(java.lang.String, com.codename1.ui.plaf.Style)}
 * or {@link #create(java.lang.String, com.codename1.ui.plaf.Style, com.codename1.ui.Font)}:</p>
 * 
 * <script src="https://gist.github.com/codenameone/da3912b9ccef03f58058.js"></script>
 * <img src="https://www.codenameone.com/img/developer-guide/graphics-fontimage-style.png" alt="An image created from the Style object" />
 * 
 * <h3>Material Design Icon Fonts</h3>
 * <p>
 * There are many icon fonts in the web, the field is rather volatile and constantly changing. However, we wanted 
 * to have builtin icons that would allow us to create better looking demos and builtin components.
 * </p>
 * <p>
 * That's why we picked the material design icon font for inclusion in the Codename One distribution. It features 
 * a relatively stable core set of icons, that aren't IP encumbered.
 * </p>
 * <p>
 * You can use the builtin font directly as demonstrated above but there are far better ways to create a material 
 * design icon. To find the icon you want you can check out the 
 * <a href="https://design.google.com/icons/" target="_blank">material design icon gallery</a>. 
 * E.g. we used the save icon in the samples above.<br>
 * To recreate the save icon from above we can do something like:
 * </p>
 * 
 * <script src="https://gist.github.com/codenameone/34fd9e519ec3d305a015.js"></script>
 * 
 * <p>This can also be expressed using the shorthand syntax:</p>
 * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
 *
 * @author Shai Almog
 */
public class FontImage extends Image {
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_10K = '\uE951';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_10MP = '\uE952';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_11MP = '\uE953';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_12MP = '\uE954';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_13MP = '\uE955';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_14MP = '\uE956';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_15MP = '\uE957';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_16MP = '\uE958';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_17MP = '\uE959';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_18MP = '\uE95A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_19MP = '\uE95B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_1K = '\uE95C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_1K_PLUS = '\uE95D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_20MP = '\uE95E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_21MP = '\uE95F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_22MP = '\uE960';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_23MP = '\uE961';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_24MP = '\uE962';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_2K = '\uE963';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_2K_PLUS = '\uE964';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_2MP = '\uE965';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_360 = '\uE577';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_3D_ROTATION = '\uE84D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_3K = '\uE966';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_3K_PLUS = '\uE967';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_3MP = '\uE968';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_4K = '\uE072';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_4K_PLUS = '\uE969';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_4MP = '\uE96A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_5G = '\uEF38';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_5K = '\uE96B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_5K_PLUS = '\uE96C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_5MP = '\uE96D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_6_FT_APART = '\uF21E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_6K = '\uE96E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_6K_PLUS = '\uE96F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_6MP = '\uE970';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_7K = '\uE971';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_7K_PLUS = '\uE972';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_7MP = '\uE973';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_8K = '\uE974';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_8K_PLUS = '\uE975';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_8MP = '\uE976';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_9K = '\uE977';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_9K_PLUS = '\uE978';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_9MP = '\uE979';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AC_UNIT = '\uEB3B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ACCESS_ALARM = '\uE190';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ACCESS_ALARMS = '\uE191';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ACCESS_TIME = '\uE192';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ACCESSIBILITY = '\uE84E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ACCESSIBILITY_NEW = '\uE92C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ACCESSIBLE = '\uE914';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ACCESSIBLE_FORWARD = '\uE934';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ACCOUNT_BALANCE = '\uE84F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ACCOUNT_BALANCE_WALLET = '\uE850';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ACCOUNT_BOX = '\uE851';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ACCOUNT_CIRCLE = '\uE853';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ACCOUNT_TREE = '\uE97A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AD_UNITS = '\uEF39';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADB = '\uE60E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD = '\uE145';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_A_PHOTO = '\uE439';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_ALARM = '\uE193';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_ALERT = '\uE003';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_BOX = '\uE146';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_BUSINESS = '\uE729';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_CALL = '\uE0E8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_CHART = '\uE97B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_CIRCLE = '\uE147';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_CIRCLE_OUTLINE = '\uE148';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_COMMENT = '\uE266';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_IC_CALL = '\uE97C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_LINK = '\uE178';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_LOCATION = '\uE567';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_LOCATION_ALT = '\uEF3A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_MODERATOR = '\uE97D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_PHOTO_ALTERNATE = '\uE43E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_ROAD = '\uEF3B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_SHOPPING_CART = '\uE854';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_TASK = '\uF23A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_TO_DRIVE = '\uE65C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_TO_HOME_SCREEN = '\uE1FE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_TO_PHOTOS = '\uE39D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADD_TO_QUEUE = '\uE05C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADDCHART = '\uEF3C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADJUST = '\uE39E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ADMIN_PANEL_SETTINGS = '\uEF3D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AGRICULTURE = '\uEA79';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRLINE_SEAT_FLAT = '\uE630';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRLINE_SEAT_FLAT_ANGLED = '\uE631';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRLINE_SEAT_INDIVIDUAL_SUITE = '\uE632';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRLINE_SEAT_LEGROOM_EXTRA = '\uE633';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRLINE_SEAT_LEGROOM_NORMAL = '\uE634';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRLINE_SEAT_LEGROOM_REDUCED = '\uE635';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRLINE_SEAT_RECLINE_EXTRA = '\uE636';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRLINE_SEAT_RECLINE_NORMAL = '\uE637';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRPLANEMODE_ACTIVE = '\uE195';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRPLANEMODE_INACTIVE = '\uE194';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRPLANEMODE_OFF = '\uE194';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRPLANEMODE_ON = '\uE195';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRPLAY = '\uE055';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AIRPORT_SHUTTLE = '\uEB3C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALARM = '\uE855';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALARM_ADD = '\uE856';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALARM_OFF = '\uE857';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALARM_ON = '\uE858';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALBUM = '\uE019';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALIGN_HORIZONTAL_CENTER = '\uE00F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALIGN_HORIZONTAL_LEFT = '\uE00D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALIGN_HORIZONTAL_RIGHT = '\uE010';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALIGN_VERTICAL_BOTTOM = '\uE015';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALIGN_VERTICAL_CENTER = '\uE011';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALIGN_VERTICAL_TOP = '\uE00C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALL_INBOX = '\uE97F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALL_INCLUSIVE = '\uEB3D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALL_OUT = '\uE90B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALT_ROUTE = '\uF184';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ALTERNATE_EMAIL = '\uE0E6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AMP_STORIES = '\uEA13';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ANALYTICS = '\uEF3E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ANCHOR = '\uF1CD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ANDROID = '\uE859';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ANIMATION = '\uE71C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ANNOUNCEMENT = '\uE85A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_APARTMENT = '\uEA40';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_API = '\uF1B7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_APP_BLOCKING = '\uEF3F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_APP_REGISTRATION = '\uEF40';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_APP_SETTINGS_ALT = '\uEF41';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_APPROVAL = '\uE982';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_APPS = '\uE5C3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARCHITECTURE = '\uEA3B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARCHIVE = '\uE149';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_BACK = '\uE5C4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_BACK_IOS = '\uE5E0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_CIRCLE_DOWN = '\uF181';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_CIRCLE_UP = '\uF182';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_DOWNWARD = '\uE5DB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_DROP_DOWN = '\uE5C5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_DROP_DOWN_CIRCLE = '\uE5C6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_DROP_UP = '\uE5C7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_FORWARD = '\uE5C8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_FORWARD_IOS = '\uE5E1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_LEFT = '\uE5DE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_RIGHT = '\uE5DF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_RIGHT_ALT = '\uE941';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARROW_UPWARD = '\uE5D8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ART_TRACK = '\uE060';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ARTICLE = '\uEF42';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ASPECT_RATIO = '\uE85B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ASSESSMENT = '\uE85C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ASSIGNMENT = '\uE85D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ASSIGNMENT_IND = '\uE85E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ASSIGNMENT_LATE = '\uE85F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ASSIGNMENT_RETURN = '\uE860';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ASSIGNMENT_RETURNED = '\uE861';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ASSIGNMENT_TURNED_IN = '\uE862';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ASSISTANT = '\uE39F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ASSISTANT_DIRECTION = '\uE988';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ASSISTANT_NAVIGATION = '\uE989';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ASSISTANT_PHOTO = '\uE3A0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ATM = '\uE573';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ATTACH_EMAIL = '\uEA5E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ATTACH_FILE = '\uE226';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ATTACH_MONEY = '\uE227';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ATTACHMENT = '\uE2BC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ATTRACTIONS = '\uEA52';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AUDIOTRACK = '\uE3A1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AUTO_AWESOME = '\uE65F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AUTO_AWESOME_MOSAIC = '\uE660';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AUTO_AWESOME_MOTION = '\uE661';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AUTO_DELETE = '\uEA4C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AUTO_FIX_HIGH = '\uE663';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AUTO_FIX_NORMAL = '\uE664';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AUTO_FIX_OFF = '\uE665';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AUTO_STORIES = '\uE666';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AUTORENEW = '\uE863';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_AV_TIMER = '\uE01B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BABY_CHANGING_STATION = '\uF19B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BACKPACK = '\uF19C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BACKSPACE = '\uE14A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BACKUP = '\uE864';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BACKUP_TABLE = '\uEF43';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BADGE = '\uEA67';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BAKERY_DINING = '\uEA53';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BALLOT = '\uE172';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BAR_CHART = '\uE26B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BATCH_PREDICTION = '\uF0F5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BATHTUB = '\uEA41';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BATTERY_ALERT = '\uE19C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BATTERY_CHARGING_FULL = '\uE1A3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BATTERY_FULL = '\uE1A4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BATTERY_STD = '\uE1A5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BATTERY_UNKNOWN = '\uE1A6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BEACH_ACCESS = '\uEB3E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BEDTIME = '\uEF44';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BEENHERE = '\uE52D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BENTO = '\uF1F4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BIKE_SCOOTER = '\uEF45';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BIOTECH = '\uEA3A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BLOCK = '\uE14B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BLOCK_FLIPPED = '\uEF46';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BLUETOOTH = '\uE1A7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BLUETOOTH_AUDIO = '\uE60F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BLUETOOTH_CONNECTED = '\uE1A8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BLUETOOTH_DISABLED = '\uE1A9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BLUETOOTH_SEARCHING = '\uE1AA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BLUR_CIRCULAR = '\uE3A2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BLUR_LINEAR = '\uE3A3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BLUR_OFF = '\uE3A4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BLUR_ON = '\uE3A5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BOLT = '\uEA0B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BOOK = '\uE865';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BOOK_ONLINE = '\uF217';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BOOKMARK = '\uE866';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BOOKMARK_BORDER = '\uE867';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BOOKMARK_OUTLINE = '\uE867';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BOOKMARKS = '\uE98B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BORDER_ALL = '\uE228';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BORDER_BOTTOM = '\uE229';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BORDER_CLEAR = '\uE22A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BORDER_COLOR = '\uE22B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BORDER_HORIZONTAL = '\uE22C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BORDER_INNER = '\uE22D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BORDER_LEFT = '\uE22E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BORDER_OUTER = '\uE22F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BORDER_RIGHT = '\uE230';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BORDER_STYLE = '\uE231';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BORDER_TOP = '\uE232';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BORDER_VERTICAL = '\uE233';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRANDING_WATERMARK = '\uE06B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BREAKFAST_DINING = '\uEA54';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRIGHTNESS_1 = '\uE3A6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRIGHTNESS_2 = '\uE3A7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRIGHTNESS_3 = '\uE3A8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRIGHTNESS_4 = '\uE3A9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRIGHTNESS_5 = '\uE3AA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRIGHTNESS_6 = '\uE3AB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRIGHTNESS_7 = '\uE3AC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRIGHTNESS_AUTO = '\uE1AB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRIGHTNESS_HIGH = '\uE1AC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRIGHTNESS_LOW = '\uE1AD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRIGHTNESS_MEDIUM = '\uE1AE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BROKEN_IMAGE = '\uE3AD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BROWSER_NOT_SUPPORTED = '\uEF47';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRUNCH_DINING = '\uEA73';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BRUSH = '\uE3AE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BUBBLE_CHART = '\uE6DD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BUG_REPORT = '\uE868';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BUILD = '\uE869';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BUILD_CIRCLE = '\uEF48';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BURST_MODE = '\uE43C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BUS_ALERT = '\uE98F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BUSINESS = '\uE0AF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_BUSINESS_CENTER = '\uEB3F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CACHED = '\uE86A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAKE = '\uE7E9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CALCULATE = '\uEA5F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CALENDAR_TODAY = '\uE935';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CALENDAR_VIEW_DAY = '\uE936';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CALL = '\uE0B0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CALL_END = '\uE0B1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CALL_MADE = '\uE0B2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CALL_MERGE = '\uE0B3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CALL_MISSED = '\uE0B4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CALL_MISSED_OUTGOING = '\uE0E4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CALL_RECEIVED = '\uE0B5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CALL_SPLIT = '\uE0B6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CALL_TO_ACTION = '\uE06C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAMERA = '\uE3AF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAMERA_ALT = '\uE3B0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAMERA_ENHANCE = '\uE8FC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAMERA_FRONT = '\uE3B1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAMERA_REAR = '\uE3B2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAMERA_ROLL = '\uE3B3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAMPAIGN = '\uEF49';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CANCEL = '\uE5C9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CANCEL_PRESENTATION = '\uE0E9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CANCEL_SCHEDULE_SEND = '\uEA39';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAR_RENTAL = '\uEA55';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAR_REPAIR = '\uEA56';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CARD_GIFTCARD = '\uE8F6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CARD_MEMBERSHIP = '\uE8F7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CARD_TRAVEL = '\uE8F8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CARPENTER = '\uF1F8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CASES = '\uE992';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CASINO = '\uEB40';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAST = '\uE307';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAST_CONNECTED = '\uE308';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CAST_FOR_EDUCATION = '\uEFEC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CATEGORY = '\uE574';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CELEBRATION = '\uEA65';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CELL_WIFI = '\uE0EC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CENTER_FOCUS_STRONG = '\uE3B4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CENTER_FOCUS_WEAK = '\uE3B5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHANGE_HISTORY = '\uE86B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHARGING_STATION = '\uF19D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHAT = '\uE0B7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHAT_BUBBLE = '\uE0CA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHAT_BUBBLE_OUTLINE = '\uE0CB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHECK = '\uE5CA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHECK_BOX = '\uE834';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHECK_BOX_OUTLINE_BLANK = '\uE835';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHECK_CIRCLE = '\uE86C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHECK_CIRCLE_OUTLINE = '\uE92D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHECKROOM = '\uF19E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHEVRON_LEFT = '\uE5CB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHEVRON_RIGHT = '\uE5CC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHILD_CARE = '\uEB41';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHILD_FRIENDLY = '\uEB42';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CHROME_READER_MODE = '\uE86D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CIRCLE = '\uEF4A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CIRCLE_NOTIFICATIONS = '\uE994';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLASS = '\uE86E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLEAN_HANDS = '\uF21F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLEANING_SERVICES = '\uF0FF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLEAR = '\uE14C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLEAR_ALL = '\uE0B8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLOSE = '\uE5CD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLOSE_FULLSCREEN = '\uF1CF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLOSED_CAPTION = '\uE01C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLOSED_CAPTION_DISABLED = '\uF1DC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLOSED_CAPTION_OFF = '\uE996';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLOUD = '\uE2BD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLOUD_CIRCLE = '\uE2BE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLOUD_DONE = '\uE2BF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLOUD_DOWNLOAD = '\uE2C0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLOUD_OFF = '\uE2C1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLOUD_QUEUE = '\uE2C2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CLOUD_UPLOAD = '\uE2C3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CODE = '\uE86F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COLLECTIONS = '\uE3B6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COLLECTIONS_BOOKMARK = '\uE431';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COLOR_LENS = '\uE3B7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COLORIZE = '\uE3B8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COMMENT = '\uE0B9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COMMENT_BANK = '\uEA4E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COMMUTE = '\uE940';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COMPARE = '\uE3B9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COMPARE_ARROWS = '\uE915';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COMPASS_CALIBRATION = '\uE57C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COMPRESS = '\uE94D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COMPUTER = '\uE30A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONFIRMATION_NUM = '\uE638';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONFIRMATION_NUMBER = '\uE638';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONNECT_WITHOUT_CONTACT = '\uF223';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONNECTED_TV = '\uE998';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONSTRUCTION = '\uEA3C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONTACT_MAIL = '\uE0D0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONTACT_PAGE = '\uF22E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONTACT_PHONE = '\uE0CF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONTACT_SUPPORT = '\uE94C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONTACTLESS = '\uEA71';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONTACTS = '\uE0BA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONTENT_COPY = '\uE14D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONTENT_CUT = '\uE14E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONTENT_PASTE = '\uE14F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONTROL_CAMERA = '\uE074';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONTROL_POINT = '\uE3BA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CONTROL_POINT_DUPLICATE = '\uE3BB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COPYRIGHT = '\uE90C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CORONAVIRUS = '\uF221';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CORPORATE_FARE = '\uF1D0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_COUNTERTOPS = '\uF1F7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CREATE = '\uE150';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CREATE_NEW_FOLDER = '\uE2CC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CREDIT_CARD = '\uE870';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CROP = '\uE3BE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CROP_16_9 = '\uE3BC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CROP_3_2 = '\uE3BD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CROP_5_4 = '\uE3BF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CROP_7_5 = '\uE3C0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CROP_DIN = '\uE3C1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CROP_FREE = '\uE3C2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CROP_LANDSCAPE = '\uE3C3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CROP_ORIGINAL = '\uE3C4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CROP_PORTRAIT = '\uE3C5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CROP_ROTATE = '\uE437';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_CROP_SQUARE = '\uE3C6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DANGEROUS = '\uE99A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DASHBOARD = '\uE871';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DASHBOARD_CUSTOMIZE = '\uE99B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DATA_USAGE = '\uE1AF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DATE_RANGE = '\uE916';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DECK = '\uEA42';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DEHAZE = '\uE3C7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DELETE = '\uE872';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DELETE_FOREVER = '\uE92B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DELETE_OUTLINE = '\uE92E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DELETE_SWEEP = '\uE16C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DELIVERY_DINING = '\uEA72';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DEPARTURE_BOARD = '\uE576';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DESCRIPTION = '\uE873';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DESIGN_SERVICES = '\uF10A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DESKTOP_ACCESS_DISABLED = '\uE99D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DESKTOP_MAC = '\uE30B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DESKTOP_WINDOWS = '\uE30C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DETAILS = '\uE3C8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DEVELOPER_BOARD = '\uE30D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DEVELOPER_MODE = '\uE1B0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DEVICE_HUB = '\uE335';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DEVICE_THERMOSTAT = '\uE1FF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DEVICE_UNKNOWN = '\uE339';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DEVICES = '\uE1B1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DEVICES_OTHER = '\uE337';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIALER_SIP = '\uE0BB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIALPAD = '\uE0BC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DINNER_DINING = '\uEA57';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS = '\uE52E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS_BIKE = '\uE52F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS_BOAT = '\uE532';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS_BUS = '\uE530';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS_CAR = '\uE531';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS_FERRY = '\uE532';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS_OFF = '\uF10F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS_RAILWAY = '\uE534';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS_RUN = '\uE566';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS_SUBWAY = '\uE533';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS_TRAIN = '\uE534';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS_TRANSIT = '\uE535';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRECTIONS_WALK = '\uE536';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DIRTY_LENS = '\uEF4B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DISABLED_BY_DEFAULT = '\uF230';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DISC_FULL = '\uE610';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DND_FORWARDSLASH = '\uE611';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DNS = '\uE875';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DO_NOT_DISTURB = '\uE612';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DO_NOT_DISTURB_ALT = '\uE611';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DO_NOT_DISTURB_OFF = '\uE643';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DO_NOT_DISTURB_ON = '\uE644';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DO_NOT_STEP = '\uF19F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DO_NOT_TOUCH = '\uF1B0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DOCK = '\uE30E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DOMAIN = '\uE7EE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DOMAIN_DISABLED = '\uE0EF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DOMAIN_VERIFICATION = '\uEF4C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DONE = '\uE876';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DONE_ALL = '\uE877';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DONE_OUTLINE = '\uE92F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DONUT_LARGE = '\uE917';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DONUT_SMALL = '\uE918';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DOUBLE_ARROW = '\uEA50';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DRAFTS = '\uE151';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DRAG_HANDLE = '\uE25D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DRAG_INDICATOR = '\uE945';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DRIVE_ETA = '\uE613';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DRIVE_FILE_MOVE = '\uE675';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DRIVE_FILE_MOVE_OUTLINE = '\uE9A1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DRIVE_FILE_RENAME_OUTLINE = '\uE9A2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DRIVE_FOLDER_UPLOAD = '\uE9A3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DRY = '\uF1B3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DRY_CLEANING = '\uEA58';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DUO = '\uE9A5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DVR = '\uE1B2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DYNAMIC_FEED = '\uEA14';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_DYNAMIC_FORM = '\uF1BF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EAST = '\uF1DF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ECO = '\uEA35';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EDIT = '\uE3C9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EDIT_ATTRIBUTES = '\uE578';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EDIT_LOCATION = '\uE568';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EDIT_OFF = '\uE950';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EDIT_ROAD = '\uEF4D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EJECT = '\uE8FB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ELDERLY = '\uF21A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ELECTRIC_BIKE = '\uEB1B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ELECTRIC_CAR = '\uEB1C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ELECTRIC_MOPED = '\uEB1D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ELECTRIC_RICKSHAW = '\uEB1E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ELECTRIC_SCOOTER = '\uEB1F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ELECTRICAL_SERVICES = '\uF102';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ELEVATOR = '\uF1A0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EMAIL = '\uE0BE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EMOJI_EMOTIONS = '\uEA22';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EMOJI_EVENTS = '\uEA23';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EMOJI_FLAGS = '\uEA1A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EMOJI_FOOD_BEVERAGE = '\uEA1B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EMOJI_NATURE = '\uEA1C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EMOJI_OBJECTS = '\uEA24';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EMOJI_PEOPLE = '\uEA1D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EMOJI_SYMBOLS = '\uEA1E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EMOJI_TRANSPORTATION = '\uEA1F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ENGINEERING = '\uEA3D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ENHANCE_PHOTO_TRANSLATE = '\uE8FC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ENHANCED_ENCRYPTION = '\uE63F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EQUALIZER = '\uE01D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ERROR = '\uE000';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ERROR_OUTLINE = '\uE001';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ESCALATOR = '\uF1A1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ESCALATOR_WARNING = '\uF1AC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EURO = '\uEA15';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EURO_SYMBOL = '\uE926';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EV_STATION = '\uE56D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EVENT = '\uE878';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EVENT_AVAILABLE = '\uE614';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EVENT_BUSY = '\uE615';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EVENT_NOTE = '\uE616';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EVENT_SEAT = '\uE903';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXIT_TO_APP = '\uE879';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPAND = '\uE94F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPAND_LESS = '\uE5CE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPAND_MORE = '\uE5CF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPLICIT = '\uE01E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPLORE = '\uE87A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPLORE_OFF = '\uE9A8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPOSURE = '\uE3CA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPOSURE_MINUS_1 = '\uE3CB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPOSURE_MINUS_2 = '\uE3CC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPOSURE_NEG_1 = '\uE3CB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPOSURE_NEG_2 = '\uE3CC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPOSURE_PLUS_1 = '\uE3CD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPOSURE_PLUS_2 = '\uE3CE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXPOSURE_ZERO = '\uE3CF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_EXTENSION = '\uE87B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FACE = '\uE87C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FACE_RETOUCHING_NATURAL = '\uEF4E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FACEBOOK = '\uF234';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FACT_CHECK = '\uF0C5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FAMILY_RESTROOM = '\uF1A2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FAST_FORWARD = '\uE01F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FAST_REWIND = '\uE020';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FASTFOOD = '\uE57A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FAVORITE = '\uE87D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FAVORITE_BORDER = '\uE87E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FAVORITE_OUTLINE = '\uE87E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FEATURED_PLAY_LIST = '\uE06D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FEATURED_VIDEO = '\uE06E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FEEDBACK = '\uE87F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FENCE = '\uF1F6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FESTIVAL = '\uEA68';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FIBER_DVR = '\uE05D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FIBER_MANUAL_RECORD = '\uE061';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FIBER_NEW = '\uE05E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FIBER_PIN = '\uE06A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FIBER_SMART_RECORD = '\uE062';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILE_COPY = '\uE173';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILE_DOWNLOAD = '\uE2C4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILE_DOWNLOAD_DONE = '\uE9AA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILE_PRESENT = '\uEA0E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILE_UPLOAD = '\uE2C6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER = '\uE3D3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_1 = '\uE3D0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_2 = '\uE3D1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_3 = '\uE3D2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_4 = '\uE3D4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_5 = '\uE3D5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_6 = '\uE3D6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_7 = '\uE3D7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_8 = '\uE3D8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_9 = '\uE3D9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_9_PLUS = '\uE3DA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_ALT = '\uEF4F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_B_AND_W = '\uE3DB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_CENTER_FOCUS = '\uE3DC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_DRAMA = '\uE3DD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_FRAMES = '\uE3DE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_HDR = '\uE3DF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_LIST = '\uE152';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_LIST_ALT = '\uE94E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_NONE = '\uE3E0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_TILT_SHIFT = '\uE3E2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FILTER_VINTAGE = '\uE3E3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FIND_IN_PAGE = '\uE880';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FIND_REPLACE = '\uE881';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FINGERPRINT = '\uE90D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FIRE_EXTINGUISHER = '\uF1D8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FIRE_HYDRANT = '\uF1A3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FIREPLACE = '\uEA43';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FIRST_PAGE = '\uE5DC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FIT_SCREEN = '\uEA10';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FITNESS_CENTER = '\uEB43';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLAG = '\uE153';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLAKY = '\uEF50';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLARE = '\uE3E4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLASH_AUTO = '\uE3E5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLASH_OFF = '\uE3E6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLASH_ON = '\uE3E7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLIGHT = '\uE539';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLIGHT_LAND = '\uE904';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLIGHT_TAKEOFF = '\uE905';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLIP = '\uE3E8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLIP_CAMERA_ANDROID = '\uEA37';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLIP_CAMERA_IOS = '\uEA38';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLIP_TO_BACK = '\uE882';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FLIP_TO_FRONT = '\uE883';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FOLDER = '\uE2C7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FOLDER_OPEN = '\uE2C8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FOLDER_SHARED = '\uE2C9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FOLDER_SPECIAL = '\uE617';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FOLLOW_THE_SIGNS = '\uF222';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FONT_DOWNLOAD = '\uE167';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FOOD_BANK = '\uF1F2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_ALIGN_CENTER = '\uE234';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_ALIGN_JUSTIFY = '\uE235';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_ALIGN_LEFT = '\uE236';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_ALIGN_RIGHT = '\uE237';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_BOLD = '\uE238';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_CLEAR = '\uE239';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_COLOR_FILL = '\uE23A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_COLOR_RESET = '\uE23B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_COLOR_TEXT = '\uE23C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_INDENT_DECREASE = '\uE23D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_INDENT_INCREASE = '\uE23E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_ITALIC = '\uE23F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_LINE_SPACING = '\uE240';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_LIST_BULLETED = '\uE241';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_LIST_NUMBERED = '\uE242';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_LIST_NUMBERED_RTL = '\uE267';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_PAINT = '\uE243';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_QUOTE = '\uE244';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_SHAPES = '\uE25E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_SIZE = '\uE245';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_STRIKETHROUGH = '\uE246';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_TEXTDIRECTION_L_TO_R = '\uE247';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_TEXTDIRECTION_R_TO_L = '\uE248';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_UNDERLINE = '\uE249';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORMAT_UNDERLINED = '\uE249';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORUM = '\uE0BF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORWARD = '\uE154';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORWARD_10 = '\uE056';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORWARD_30 = '\uE057';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORWARD_5 = '\uE058';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FORWARD_TO_INBOX = '\uF187';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FOUNDATION = '\uF200';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FREE_BREAKFAST = '\uEB44';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FULLSCREEN = '\uE5D0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FULLSCREEN_EXIT = '\uE5D1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_FUNCTIONS = '\uE24A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_G_TRANSLATE = '\uE927';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GAMEPAD = '\uE30F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GAMES = '\uE021';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GAVEL = '\uE90E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GESTURE = '\uE155';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GET_APP = '\uE884';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GIF = '\uE908';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GOLF_COURSE = '\uEB45';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GPS_FIXED = '\uE1B3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GPS_NOT_FIXED = '\uE1B4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GPS_OFF = '\uE1B5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GRADE = '\uE885';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GRADIENT = '\uE3E9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GRADING = '\uEA4F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GRAIN = '\uE3EA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GRAPHIC_EQ = '\uE1B8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GRASS = '\uF205';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GRID_OFF = '\uE3EB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GRID_ON = '\uE3EC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GRID_VIEW = '\uE9B0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GROUP = '\uE7EF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GROUP_ADD = '\uE7F0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GROUP_WORK = '\uE886';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_GROUPS = '\uF233';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HAIL = '\uE9B1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HANDYMAN = '\uF10B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HARDWARE = '\uEA59';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HD = '\uE052';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HDR_ENHANCED_SELECT = '\uEF51';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HDR_OFF = '\uE3ED';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HDR_ON = '\uE3EE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HDR_STRONG = '\uE3F1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HDR_WEAK = '\uE3F2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HEADSET = '\uE310';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HEADSET_MIC = '\uE311';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HEADSET_OFF = '\uE33A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HEALING = '\uE3F3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HEARING = '\uE023';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HEARING_DISABLED = '\uF104';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HEIGHT = '\uEA16';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HELP = '\uE887';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HELP_CENTER = '\uF1C0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HELP_OUTLINE = '\uE8FD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HIGH_QUALITY = '\uE024';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HIGHLIGHT = '\uE25F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HIGHLIGHT_ALT = '\uEF52';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HIGHLIGHT_OFF = '\uE888';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HIGHLIGHT_REMOVE = '\uE888';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HISTORY = '\uE889';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HISTORY_EDU = '\uEA3E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HISTORY_TOGGLE_OFF = '\uF17D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOME = '\uE88A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOME_FILLED = '\uE9B2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOME_REPAIR_SERVICE = '\uF100';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOME_WORK = '\uEA09';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HORIZONTAL_DISTRIBUTE = '\uE014';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HORIZONTAL_RULE = '\uF108';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HORIZONTAL_SPLIT = '\uE947';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOT_TUB = '\uEB46';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOTEL = '\uE53A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOURGLASS_BOTTOM = '\uEA5C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOURGLASS_DISABLED = '\uEF53';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOURGLASS_EMPTY = '\uE88B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOURGLASS_FULL = '\uE88C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOURGLASS_TOP = '\uEA5B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOUSE = '\uEA44';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOUSE_SIDING = '\uF202';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOW_TO_REG = '\uE174';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HOW_TO_VOTE = '\uE175';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HTTP = '\uE902';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HTTPS = '\uE88D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_HVAC = '\uF10E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ICECREAM = '\uEA69';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_IMAGE = '\uE3F4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_IMAGE_ASPECT_RATIO = '\uE3F5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_IMAGE_NOT_SUPPORTED = '\uF116';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_IMAGE_SEARCH = '\uE43F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_IMAGESEARCH_ROLLER = '\uE9B4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_IMPORT_CONTACTS = '\uE0E0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_IMPORT_EXPORT = '\uE0C3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_IMPORTANT_DEVICES = '\uE912';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INBOX = '\uE156';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INDETERMINATE_CHECK_BOX = '\uE909';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INFO = '\uE88E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INFO_OUTLINE = '\uE88F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INPUT = '\uE890';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INSERT_CHART = '\uE24B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INSERT_CHART_OUTLINED = '\uE26A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INSERT_COMMENT = '\uE24C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INSERT_DRIVE_FILE = '\uE24D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INSERT_EMOTICON = '\uE24E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INSERT_INVITATION = '\uE24F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INSERT_LINK = '\uE250';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INSERT_PHOTO = '\uE251';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INSIGHTS = '\uF092';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INTEGRATION_INSTRUCTIONS = '\uEF54';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INVENTORY = '\uE179';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INVERT_COLORS = '\uE891';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INVERT_COLORS_OFF = '\uE0C4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_INVERT_COLORS_ON = '\uE891';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_IOS_SHARE = '\uE6B8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ISO = '\uE3F6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KEYBOARD = '\uE312';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KEYBOARD_ARROW_DOWN = '\uE313';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KEYBOARD_ARROW_LEFT = '\uE314';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KEYBOARD_ARROW_RIGHT = '\uE315';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KEYBOARD_ARROW_UP = '\uE316';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KEYBOARD_BACKSPACE = '\uE317';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KEYBOARD_CAPSLOCK = '\uE318';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KEYBOARD_CONTROL = '\uE5D3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KEYBOARD_HIDE = '\uE31A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KEYBOARD_RETURN = '\uE31B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KEYBOARD_TAB = '\uE31C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KEYBOARD_VOICE = '\uE31D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KING_BED = '\uEA45';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_KITCHEN = '\uEB47';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LABEL = '\uE892';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LABEL_IMPORTANT = '\uE937';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LABEL_IMPORTANT_OUTLINE = '\uE948';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LABEL_OFF = '\uE9B6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LABEL_OUTLINE = '\uE893';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LANDSCAPE = '\uE3F7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LANGUAGE = '\uE894';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LAPTOP = '\uE31E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LAPTOP_CHROMEBOOK = '\uE31F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LAPTOP_MAC = '\uE320';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LAPTOP_WINDOWS = '\uE321';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LAST_PAGE = '\uE5DD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LAUNCH = '\uE895';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LAYERS = '\uE53B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LAYERS_CLEAR = '\uE53C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LEADERBOARD = '\uF20C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LEAK_ADD = '\uE3F8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LEAK_REMOVE = '\uE3F9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LEAVE_BAGS_AT_HOME = '\uF21B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LEGEND_TOGGLE = '\uF11B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LENS = '\uE3FA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LIBRARY_ADD = '\uE02E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LIBRARY_ADD_CHECK = '\uE9B7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LIBRARY_BOOKS = '\uE02F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LIBRARY_MUSIC = '\uE030';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LIGHTBULB = '\uE0F0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LIGHTBULB_OUTLINE = '\uE90F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LINE_STYLE = '\uE919';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LINE_WEIGHT = '\uE91A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LINEAR_SCALE = '\uE260';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LINK = '\uE157';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LINK_OFF = '\uE16F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LINKED_CAMERA = '\uE438';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LIQUOR = '\uEA60';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LIST = '\uE896';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LIST_ALT = '\uE0EE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LIVE_HELP = '\uE0C6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LIVE_TV = '\uE639';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_ACTIVITY = '\uE53F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_AIRPORT = '\uE53D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_ATM = '\uE53E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_ATTRACTION = '\uE53F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_BAR = '\uE540';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_CAFE = '\uE541';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_CAR_WASH = '\uE542';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_CONVENIENCE_STORE = '\uE543';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_DINING = '\uE556';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_DRINK = '\uE544';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_FIRE_DEPARTMENT = '\uEF55';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_FLORIST = '\uE545';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_GAS_STATION = '\uE546';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_GROCERY_STORE = '\uE547';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_HOSPITAL = '\uE548';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_HOTEL = '\uE549';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_LAUNDRY_SERVICE = '\uE54A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_LIBRARY = '\uE54B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_MALL = '\uE54C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_MOVIES = '\uE54D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_OFFER = '\uE54E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_PARKING = '\uE54F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_PHARMACY = '\uE550';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_PHONE = '\uE551';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_PIZZA = '\uE552';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_PLAY = '\uE553';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_POLICE = '\uEF56';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_POST_OFFICE = '\uE554';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_PRINT_SHOP = '\uE555';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_PRINTSHOP = '\uE555';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_RESTAURANT = '\uE556';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_SEE = '\uE557';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_SHIPPING = '\uE558';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCAL_TAXI = '\uE559';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCATION_CITY = '\uE7F1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCATION_DISABLED = '\uE1B6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCATION_HISTORY = '\uE55A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCATION_OFF = '\uE0C7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCATION_ON = '\uE0C8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCATION_PIN = '\uF1DB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCATION_SEARCHING = '\uE1B7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCK = '\uE897';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCK_CLOCK = '\uEF57';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCK_OPEN = '\uE898';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOCK_OUTLINE = '\uE899';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOGIN = '\uEA77';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOGOUT = '\uE9BA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOOKS = '\uE3FC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOOKS_3 = '\uE3FB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOOKS_4 = '\uE3FD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOOKS_5 = '\uE3FE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOOKS_6 = '\uE3FF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOOKS_ONE = '\uE400';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOOKS_TWO = '\uE401';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOOP = '\uE028';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOUPE = '\uE402';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOW_PRIORITY = '\uE16D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LOYALTY = '\uE89A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LUGGAGE = '\uF235';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_LUNCH_DINING = '\uEA61';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MAIL = '\uE158';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MAIL_OUTLINE = '\uE0E1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MAP = '\uE55B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MAPS_UGC = '\uEF58';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MARGIN = '\uE9BB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MARK_AS_UNREAD = '\uE9BC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MARK_CHAT_READ = '\uF18B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MARK_CHAT_UNREAD = '\uF189';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MARK_EMAIL_READ = '\uF18C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MARK_EMAIL_UNREAD = '\uF18A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MARKUNREAD = '\uE159';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MARKUNREAD_MAILBOX = '\uE89B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MASKS = '\uF218';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MAXIMIZE = '\uE930';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MEDIATION = '\uEFA7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MEDICAL_SERVICES = '\uF109';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MEETING_ROOM = '\uEB4F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MEMORY = '\uE322';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MENU = '\uE5D2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MENU_BOOK = '\uEA19';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MENU_OPEN = '\uE9BD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MERGE_TYPE = '\uE252';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MESSAGE = '\uE0C9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MESSENGER = '\uE0CA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MESSENGER_OUTLINE = '\uE0CB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MIC = '\uE029';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MIC_EXTERNAL_OFF = '\uEF59';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MIC_EXTERNAL_ON = '\uEF5A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MIC_NONE = '\uE02A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MIC_OFF = '\uE02B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MICROWAVE = '\uF204';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MILITARY_TECH = '\uEA3F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MINIMIZE = '\uE931';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MISCELLANEOUS_SERVICES = '\uF10C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MISSED_VIDEO_CALL = '\uE073';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MMS = '\uE618';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOBILE_FRIENDLY = '\uE200';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOBILE_OFF = '\uE201';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOBILE_SCREEN_SHARE = '\uE0E7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MODE_COMMENT = '\uE253';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MODE_EDIT = '\uE254';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MODEL_TRAINING = '\uF0CF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MONETIZATION_ON = '\uE263';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MONEY = '\uE57D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MONEY_OFF = '\uE25C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MONITOR = '\uEF5B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MONOCHROME_PHOTOS = '\uE403';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOOD = '\uE7F2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOOD_BAD = '\uE7F3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOPED = '\uEB28';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MORE = '\uE619';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MORE_HORIZ = '\uE5D3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MORE_TIME = '\uEA5D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MORE_VERT = '\uE5D4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOTION_PHOTOS_OFF = '\uE9C0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOTION_PHOTOS_ON = '\uE9C1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOTION_PHOTOS_PAUSE = '\uF227';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOTION_PHOTOS_PAUSED = '\uE9C2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOTORCYCLE = '\uE91B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOUSE = '\uE323';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOVE_TO_INBOX = '\uE168';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOVIE = '\uE02C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOVIE_CREATION = '\uE404';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MOVIE_FILTER = '\uE43A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MP = '\uE9C3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MULTILINE_CHART = '\uE6DF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MULTIPLE_STOP = '\uF1B9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MULTITRACK_AUDIO = '\uE1B8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MUSEUM = '\uEA36';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MUSIC_NOTE = '\uE405';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MUSIC_OFF = '\uE440';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MUSIC_VIDEO = '\uE063';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MY_LIBRARY_ADD = '\uE02E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MY_LIBRARY_BOOKS = '\uE02F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MY_LIBRARY_MUSIC = '\uE030';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_MY_LOCATION = '\uE55C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NAT = '\uEF5C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NATURE = '\uE406';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NATURE_PEOPLE = '\uE407';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NAVIGATE_BEFORE = '\uE408';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NAVIGATE_NEXT = '\uE409';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NAVIGATION = '\uE55D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NEAR_ME = '\uE569';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NEAR_ME_DISABLED = '\uF1EF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NETWORK_CELL = '\uE1B9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NETWORK_CHECK = '\uE640';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NETWORK_LOCKED = '\uE61A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NETWORK_WIFI = '\uE1BA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NEW_RELEASES = '\uE031';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NEXT_PLAN = '\uEF5D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NEXT_WEEK = '\uE16A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NFC = '\uE1BB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NIGHT_SHELTER = '\uF1F1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NIGHTLIFE = '\uEA62';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NIGHTLIGHT_ROUND = '\uEF5E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NIGHTS_STAY = '\uEA46';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_BACKPACK = '\uF237';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_CELL = '\uF1A4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_DRINKS = '\uF1A5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_ENCRYPTION = '\uE641';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_FLASH = '\uF1A6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_FOOD = '\uF1A7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_LUGGAGE = '\uF23B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_MEALS = '\uF1D6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_MEALS_OULINE = '\uF229';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_MEETING_ROOM = '\uEB4E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_PHOTOGRAPHY = '\uF1A8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_SIM = '\uE0CC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_STROLLER = '\uF1AF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NO_TRANSFER = '\uF1D5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NORTH = '\uF1E0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NORTH_EAST = '\uF1E1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NORTH_WEST = '\uF1E2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOT_ACCESSIBLE = '\uF0FE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOT_INTERESTED = '\uE033';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOT_LISTED_LOCATION = '\uE575';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOT_STARTED = '\uF0D1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOTE = '\uE06F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOTE_ADD = '\uE89C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOTES = '\uE26C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOTIFICATION_IMPORTANT = '\uE004';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOTIFICATIONS = '\uE7F4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOTIFICATIONS_ACTIVE = '\uE7F7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOTIFICATIONS_NONE = '\uE7F5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOTIFICATIONS_OFF = '\uE7F6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOTIFICATIONS_ON = '\uE7F7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOTIFICATIONS_PAUSED = '\uE7F8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOW_WALLPAPER = '\uE1BC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_NOW_WIDGETS = '\uE1BD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OFFLINE_BOLT = '\uE932';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OFFLINE_PIN = '\uE90A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OFFLINE_SHARE = '\uE9C5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ONDEMAND_VIDEO = '\uE63A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ONLINE_PREDICTION = '\uF0EB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OPACITY = '\uE91C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OPEN_IN_BROWSER = '\uE89D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OPEN_IN_FULL = '\uF1CE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OPEN_IN_NEW = '\uE89E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OPEN_WITH = '\uE89F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OUTBOND = '\uF228';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OUTBOX = '\uEF5F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OUTDOOR_GRILL = '\uEA47';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OUTGOING_MAIL = '\uF0D2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OUTLET = '\uF1D4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_OUTLINED_FLAG = '\uE16E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PADDING = '\uE9C8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PAGES = '\uE7F9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PAGEVIEW = '\uE8A0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PALETTE = '\uE40A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PAN_TOOL = '\uE925';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PANORAMA = '\uE40B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PANORAMA_FISH_EYE = '\uE40C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PANORAMA_FISHEYE = '\uE40C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PANORAMA_HORIZONTAL = '\uE40D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PANORAMA_HORIZONTAL_SELECT = '\uEF60';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PANORAMA_PHOTOSPHERE = '\uE9C9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PANORAMA_PHOTOSPHERE_SELECT = '\uE9CA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PANORAMA_VERTICAL = '\uE40E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PANORAMA_VERTICAL_SELECT = '\uEF61';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PANORAMA_WIDE_ANGLE = '\uE40F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PANORAMA_WIDE_ANGLE_SELECT = '\uEF62';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PARK = '\uEA63';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PARTY_MODE = '\uE7FA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PAUSE = '\uE034';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PAUSE_CIRCLE_FILLED = '\uE035';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PAUSE_CIRCLE_OUTLINE = '\uE036';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PAUSE_PRESENTATION = '\uE0EA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PAYMENT = '\uE8A1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PAYMENTS = '\uEF63';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PEDAL_BIKE = '\uEB29';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PENDING = '\uEF64';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PENDING_ACTIONS = '\uF1BB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PEOPLE = '\uE7FB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PEOPLE_ALT = '\uEA21';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PEOPLE_OUTLINE = '\uE7FC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERM_CAMERA_MIC = '\uE8A2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERM_CONTACT_CAL = '\uE8A3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERM_CONTACT_CALENDAR = '\uE8A3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERM_DATA_SETTING = '\uE8A4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERM_DEVICE_INFO = '\uE8A5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERM_DEVICE_INFORMATION = '\uE8A5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERM_IDENTITY = '\uE8A6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERM_MEDIA = '\uE8A7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERM_PHONE_MSG = '\uE8A8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERM_SCAN_WIFI = '\uE8A9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERSON = '\uE7FD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERSON_ADD = '\uE7FE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERSON_ADD_ALT = '\uEA4D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERSON_ADD_ALT_1 = '\uEF65';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERSON_ADD_DISABLED = '\uE9CB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERSON_OUTLINE = '\uE7FF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERSON_PIN = '\uE55A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERSON_PIN_CIRCLE = '\uE56A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERSON_REMOVE = '\uEF66';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERSON_REMOVE_ALT_1 = '\uEF67';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERSON_SEARCH = '\uF106';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PERSONAL_VIDEO = '\uE63B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PEST_CONTROL = '\uF0FA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PEST_CONTROL_RODENT = '\uF0FD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PETS = '\uE91D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONE = '\uE0CD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONE_ANDROID = '\uE324';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONE_BLUETOOTH_SPEAKER = '\uE61B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONE_CALLBACK = '\uE649';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONE_DISABLED = '\uE9CC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONE_ENABLED = '\uE9CD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONE_FORWARDED = '\uE61C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONE_IN_TALK = '\uE61D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONE_IPHONE = '\uE325';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONE_LOCKED = '\uE61E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONE_MISSED = '\uE61F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONE_PAUSED = '\uE620';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONELINK = '\uE326';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONELINK_ERASE = '\uE0DB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONELINK_LOCK = '\uE0DC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONELINK_OFF = '\uE327';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONELINK_RING = '\uE0DD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHONELINK_SETUP = '\uE0DE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHOTO = '\uE410';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHOTO_ALBUM = '\uE411';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHOTO_CAMERA = '\uE412';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHOTO_CAMERA_BACK = '\uEF68';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHOTO_CAMERA_FRONT = '\uEF69';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHOTO_FILTER = '\uE43B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHOTO_LIBRARY = '\uE413';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHOTO_SIZE_SELECT_ACTUAL = '\uE432';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHOTO_SIZE_SELECT_LARGE = '\uE433';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PHOTO_SIZE_SELECT_SMALL = '\uE434';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PICTURE_AS_PDF = '\uE415';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PICTURE_IN_PICTURE = '\uE8AA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PICTURE_IN_PICTURE_ALT = '\uE911';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PIE_CHART = '\uE6C4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PIE_CHART_OUTLINED = '\uE6C5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PIN_DROP = '\uE55E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PIVOT_TABLE_CHART = '\uE9CE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLACE = '\uE55F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLAGIARISM = '\uEA5A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLAY_ARROW = '\uE037';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLAY_CIRCLE_FILL = '\uE038';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLAY_CIRCLE_FILLED = '\uE038';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLAY_CIRCLE_OUTLINE = '\uE039';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLAY_DISABLED = '\uEF6A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLAY_FOR_WORK = '\uE906';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLAYLIST_ADD = '\uE03B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLAYLIST_ADD_CHECK = '\uE065';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLAYLIST_PLAY = '\uE05F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLUMBING = '\uF107';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PLUS_ONE = '\uE800';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_POINT_OF_SALE = '\uF17E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_POLICY = '\uEA17';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_POLL = '\uE801';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_POLYMER = '\uE8AB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_POOL = '\uEB48';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PORTABLE_WIFI_OFF = '\uE0CE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PORTRAIT = '\uE416';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_POST_ADD = '\uEA20';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_POWER = '\uE63C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_POWER_INPUT = '\uE336';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_POWER_OFF = '\uE646';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_POWER_SETTINGS_NEW = '\uE8AC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PREGNANT_WOMAN = '\uE91E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PRESENT_TO_ALL = '\uE0DF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PREVIEW = '\uF1C5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PRINT = '\uE8AD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PRINT_DISABLED = '\uE9CF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PRIORITY_HIGH = '\uE645';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PRIVACY_TIP = '\uF0DC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PSYCHOLOGY = '\uEA4A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PUBLIC = '\uE80B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PUBLIC_OFF = '\uF1CA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PUBLISH = '\uE255';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PUBLISHED_WITH_CHANGES = '\uF232';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_PUSH_PIN = '\uF10D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_QR_CODE = '\uEF6B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_QR_CODE_2 = '\uE00A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_QR_CODE_SCANNER = '\uF206';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_QUERY_BUILDER = '\uE8AE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_QUESTION_ANSWER = '\uE8AF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_QUEUE = '\uE03C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_QUEUE_MUSIC = '\uE03D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_QUEUE_PLAY_NEXT = '\uE066';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_QUICK_CONTACTS_DIALER = '\uE0CF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_QUICK_CONTACTS_MAIL = '\uE0D0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_QUICKREPLY = '\uEF6C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RADIO = '\uE03E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RADIO_BUTTON_CHECKED = '\uE837';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RADIO_BUTTON_OFF = '\uE836';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RADIO_BUTTON_ON = '\uE837';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RADIO_BUTTON_UNCHECKED = '\uE836';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RAILWAY_ALERT = '\uE9D1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RAMEN_DINING = '\uEA64';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RATE_REVIEW = '\uE560';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_READ_MORE = '\uEF6D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RECEIPT = '\uE8B0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RECEIPT_LONG = '\uEF6E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RECENT_ACTORS = '\uE03F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RECOMMEND = '\uE9D2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RECORD_VOICE_OVER = '\uE91F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REDEEM = '\uE8B1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REDO = '\uE15A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REDUCE_CAPACITY = '\uF21C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REFRESH = '\uE5D5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REMOVE = '\uE15B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REMOVE_CIRCLE = '\uE15C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REMOVE_CIRCLE_OUTLINE = '\uE15D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REMOVE_DONE = '\uE9D3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REMOVE_FROM_QUEUE = '\uE067';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REMOVE_MODERATOR = '\uE9D4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REMOVE_RED_EYE = '\uE417';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REMOVE_SHOPPING_CART = '\uE928';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REORDER = '\uE8FE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPEAT = '\uE040';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPEAT_ON = '\uE9D6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPEAT_ONE = '\uE041';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPEAT_ONE_ON = '\uE9D7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPLAY = '\uE042';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPLAY_10 = '\uE059';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPLAY_30 = '\uE05A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPLAY_5 = '\uE05B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPLAY_CIRCLE_FILLED = '\uE9D8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPLY = '\uE15E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPLY_ALL = '\uE15F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPORT = '\uE160';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPORT_OFF = '\uE170';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REPORT_PROBLEM = '\uE8B2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REQUEST_PAGE = '\uF22C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_REQUEST_QUOTE = '\uF1B6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RESET_TV = '\uE9D9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RESTAURANT = '\uE56C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RESTAURANT_MENU = '\uE561';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RESTORE = '\uE8B3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RESTORE_FROM_TRASH = '\uE938';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RESTORE_PAGE = '\uE929';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RICE_BOWL = '\uF1F5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RING_VOLUME = '\uE0D1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ROOFING = '\uF201';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ROOM = '\uE8B4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ROOM_PREFERENCES = '\uF1B8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ROOM_SERVICE = '\uEB49';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ROTATE_90_DEGREES_CCW = '\uE418';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ROTATE_LEFT = '\uE419';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ROTATE_RIGHT = '\uE41A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ROUNDED_CORNER = '\uE920';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ROUTER = '\uE328';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ROWING = '\uE921';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RSS_FEED = '\uE0E5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RTT = '\uE9AD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RULE = '\uF1C2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RULE_FOLDER = '\uF1C9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RUN_CIRCLE = '\uEF6F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_RV_HOOKUP = '\uE642';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SANITIZER = '\uF21D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SATELLITE = '\uE562';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SAVE = '\uE161';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SAVE_ALT = '\uE171';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SAVED_SEARCH = '\uEA11';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCANNER = '\uE329';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCATTER_PLOT = '\uE268';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCHEDULE = '\uE8B5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCHEDULE_SEND = '\uEA0A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCHOOL = '\uE80C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCIENCE = '\uEA4B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCORE = '\uE269';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCREEN_LOCK_LANDSCAPE = '\uE1BE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCREEN_LOCK_PORTRAIT = '\uE1BF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCREEN_LOCK_ROTATION = '\uE1C0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCREEN_ROTATION = '\uE1C1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCREEN_SEARCH_DESKTOP = '\uEF70';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SCREEN_SHARE = '\uE0E2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SD = '\uE9DD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SD_CARD = '\uE623';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SD_STORAGE = '\uE1C2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SEARCH = '\uE8B6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SEARCH_OFF = '\uEA76';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SECURITY = '\uE32A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SEGMENT = '\uE94B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SELECT_ALL = '\uE162';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SELF_IMPROVEMENT = '\uEA78';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SEND = '\uE163';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SEND_AND_ARCHIVE = '\uEA0C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SEND_TO_MOBILE = '\uF05C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SENSOR_DOOR = '\uF1B5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SENSOR_WINDOW = '\uF1B4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SENTIMENT_DISSATISFIED = '\uE811';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SENTIMENT_NEUTRAL = '\uE812';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SENTIMENT_SATISFIED = '\uE813';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SENTIMENT_SATISFIED_ALT = '\uE0ED';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SENTIMENT_VERY_DISSATISFIED = '\uE814';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SENTIMENT_VERY_SATISFIED = '\uE815';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SET_MEAL = '\uF1EA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS = '\uE8B8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_APPLICATIONS = '\uE8B9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_BACKUP_RESTORE = '\uE8BA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_BLUETOOTH = '\uE8BB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_BRIGHTNESS = '\uE8BD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_CELL = '\uE8BC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_DISPLAY = '\uE8BD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_ETHERNET = '\uE8BE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_INPUT_ANTENNA = '\uE8BF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_INPUT_COMPONENT = '\uE8C0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_INPUT_COMPOSITE = '\uE8C1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_INPUT_HDMI = '\uE8C2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_INPUT_SVIDEO = '\uE8C3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_OVERSCAN = '\uE8C4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_PHONE = '\uE8C5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_POWER = '\uE8C6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_REMOTE = '\uE8C7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_SYSTEM_DAYDREAM = '\uE1C3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SETTINGS_VOICE = '\uE8C8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SHARE = '\uE80D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SHIELD = '\uE9E0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SHOP = '\uE8C9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SHOP_TWO = '\uE8CA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SHOPPING_BAG = '\uF1CC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SHOPPING_BASKET = '\uE8CB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SHOPPING_CART = '\uE8CC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SHORT_TEXT = '\uE261';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SHOW_CHART = '\uE6E1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SHUFFLE = '\uE043';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SHUFFLE_ON = '\uE9E1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SHUTTER_SPEED = '\uE43D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SICK = '\uF220';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIGNAL_CELLULAR_0_BAR = '\uF0A8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIGNAL_CELLULAR_4_BAR = '\uE1C8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIGNAL_CELLULAR_ALT = '\uE202';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIGNAL_CELLULAR_CONNECTED_NO_INTERNET_4_BAR = '\uE1CD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIGNAL_CELLULAR_NO_SIM = '\uE1CE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIGNAL_CELLULAR_NULL = '\uE1CF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIGNAL_CELLULAR_OFF = '\uE1D0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIGNAL_WIFI_0_BAR = '\uF0B0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIGNAL_WIFI_4_BAR = '\uE1D8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIGNAL_WIFI_4_BAR_LOCK = '\uE1D9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIGNAL_WIFI_OFF = '\uE1DA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIM_CARD = '\uE32B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SIM_CARD_ALERT = '\uE624';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SINGLE_BED = '\uEA48';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SKIP_NEXT = '\uE044';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SKIP_PREVIOUS = '\uE045';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SLIDESHOW = '\uE41B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SLOW_MOTION_VIDEO = '\uE068';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SMART_BUTTON = '\uF1C1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SMARTPHONE = '\uE32C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SMOKE_FREE = '\uEB4A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SMOKING_ROOMS = '\uEB4B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SMS = '\uE625';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SMS_FAILED = '\uE626';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SNIPPET_FOLDER = '\uF1C7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SNOOZE = '\uE046';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SOAP = '\uF1B2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SORT = '\uE164';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SORT_BY_ALPHA = '\uE053';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SOURCE = '\uF1C4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SOUTH = '\uF1E3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SOUTH_EAST = '\uF1E4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SOUTH_WEST = '\uF1E5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPA = '\uEB4C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPACE_BAR = '\uE256';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPEAKER = '\uE32D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPEAKER_GROUP = '\uE32E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPEAKER_NOTES = '\uE8CD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPEAKER_NOTES_OFF = '\uE92A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPEAKER_PHONE = '\uE0D2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPEED = '\uE9E4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPELLCHECK = '\uE8CE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS = '\uEA30';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_BAR = '\uF1F3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_BASEBALL = '\uEA51';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_BASKETBALL = '\uEA26';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_CRICKET = '\uEA27';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_ESPORTS = '\uEA28';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_FOOTBALL = '\uEA29';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_GOLF = '\uEA2A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_HANDBALL = '\uEA33';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_HOCKEY = '\uEA2B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_KABADDI = '\uEA34';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_MMA = '\uEA2C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_MOTORSPORTS = '\uEA2D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_RUGBY = '\uEA2E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_SOCCER = '\uEA2F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_TENNIS = '\uEA32';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SPORTS_VOLLEYBALL = '\uEA31';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SQUARE_FOOT = '\uEA49';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STACKED_BAR_CHART = '\uE9E6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STACKED_LINE_CHART = '\uF22B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STAIRS = '\uF1A9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STAR = '\uE838';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STAR_BORDER = '\uE83A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STAR_HALF = '\uE839';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STAR_OUTLINE = '\uF06F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STAR_RATE = '\uF0EC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STARS = '\uE8D0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STAY_CURRENT_LANDSCAPE = '\uE0D3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STAY_CURRENT_PORTRAIT = '\uE0D4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STAY_PRIMARY_LANDSCAPE = '\uE0D5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STAY_PRIMARY_PORTRAIT = '\uE0D6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STICKY_NOTE_2 = '\uF1FC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STOP = '\uE047';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STOP_CIRCLE = '\uEF71';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STOP_SCREEN_SHARE = '\uE0E3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STORAGE = '\uE1DB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STORE = '\uE8D1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STORE_MALL_DIRECTORY = '\uE563';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STOREFRONT = '\uEA12';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STRAIGHTEN = '\uE41C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STREAM = '\uE9E9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STREETVIEW = '\uE56E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STRIKETHROUGH_S = '\uE257';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STROLLER = '\uF1AE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_STYLE = '\uE41D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUBDIRECTORY_ARROW_LEFT = '\uE5D9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUBDIRECTORY_ARROW_RIGHT = '\uE5DA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUBJECT = '\uE8D2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUBSCRIPT = '\uF111';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUBSCRIPTIONS = '\uE064';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUBTITLES = '\uE048';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUBTITLES_OFF = '\uEF72';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUBWAY = '\uE56F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUPERSCRIPT = '\uF112';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUPERVISED_USER_CIRCLE = '\uE939';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUPERVISOR_ACCOUNT = '\uE8D3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUPPORT = '\uEF73';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SUPPORT_AGENT = '\uF0E2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SURROUND_SOUND = '\uE049';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SWAP_CALLS = '\uE0D7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SWAP_HORIZ = '\uE8D4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SWAP_HORIZONTAL_CIRCLE = '\uE933';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SWAP_VERT = '\uE8D5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SWAP_VERT_CIRCLE = '\uE8D6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SWAP_VERTICAL_CIRCLE = '\uE8D6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SWIPE = '\uE9EC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SWITCH_ACCOUNT = '\uE9ED';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SWITCH_CAMERA = '\uE41E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SWITCH_LEFT = '\uF1D1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SWITCH_RIGHT = '\uF1D2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SWITCH_VIDEO = '\uE41F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SYNC = '\uE627';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SYNC_ALT = '\uEA18';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SYNC_DISABLED = '\uE628';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SYNC_PROBLEM = '\uE629';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SYSTEM_UPDATE = '\uE62A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SYSTEM_UPDATE_ALT = '\uE8D7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_SYSTEM_UPDATE_TV = '\uE8D7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TAB = '\uE8D8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TAB_UNSELECTED = '\uE8D9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TABLE_CHART = '\uE265';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TABLE_ROWS = '\uF101';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TABLE_VIEW = '\uF1BE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TABLET = '\uE32F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TABLET_ANDROID = '\uE330';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TABLET_MAC = '\uE331';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TAG = '\uE9EF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TAG_FACES = '\uE420';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TAKEOUT_DINING = '\uEA74';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TAP_AND_PLAY = '\uE62B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TAPAS = '\uF1E9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TAXI_ALERT = '\uEF74';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TERRAIN = '\uE564';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TEXT_FIELDS = '\uE262';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TEXT_FORMAT = '\uE165';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TEXT_ROTATE_UP = '\uE93A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TEXT_ROTATE_VERTICAL = '\uE93B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TEXT_ROTATION_ANGLEDOWN = '\uE93C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TEXT_ROTATION_ANGLEUP = '\uE93D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TEXT_ROTATION_DOWN = '\uE93E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TEXT_ROTATION_NONE = '\uE93F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TEXT_SNIPPET = '\uF1C6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TEXTSMS = '\uE0D8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TEXTURE = '\uE421';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_THEATER_COMEDY = '\uEA66';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_THEATERS = '\uE8DA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_THUMB_DOWN = '\uE8DB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_THUMB_DOWN_ALT = '\uE816';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_THUMB_DOWN_OFF_ALT = '\uE9F2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_THUMB_UP = '\uE8DC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_THUMB_UP_ALT = '\uE817';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_THUMB_UP_OFF_ALT = '\uE9F3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_THUMBS_UP_DOWN = '\uE8DD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TIME_TO_LEAVE = '\uE62C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TIMELAPSE = '\uE422';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TIMELINE = '\uE922';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TIMER = '\uE425';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TIMER_10 = '\uE423';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TIMER_3 = '\uE424';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TIMER_OFF = '\uE426';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TITLE = '\uE264';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TOC = '\uE8DE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TODAY = '\uE8DF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TOGGLE_OFF = '\uE9F5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TOGGLE_ON = '\uE9F6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TOLL = '\uE8E0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TONALITY = '\uE427';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TOPIC = '\uF1C8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TOUCH_APP = '\uE913';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TOUR = '\uEF75';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TOYS = '\uE332';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRACK_CHANGES = '\uE8E1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRAFFIC = '\uE565';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRAIN = '\uE570';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRAM = '\uE571';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRANSFER_WITHIN_A_STATION = '\uE572';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRANSFORM = '\uE428';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRANSIT_ENTEREXIT = '\uE579';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRANSLATE = '\uE8E2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRENDING_DOWN = '\uE8E3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRENDING_FLAT = '\uE8E4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRENDING_NEUTRAL = '\uE8E4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRENDING_UP = '\uE8E5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TRIP_ORIGIN = '\uE57B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TTY = '\uF1AA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TUNE = '\uE429';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TURNED_IN = '\uE8E6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TURNED_IN_NOT = '\uE8E7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TV = '\uE333';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TV_OFF = '\uE647';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_TWO_WHEELER = '\uE9F9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_UMBRELLA = '\uF1AD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_UNARCHIVE = '\uE169';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_UNDO = '\uE166';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_UNFOLD_LESS = '\uE5D6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_UNFOLD_MORE = '\uE5D7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_UNPUBLISHED = '\uF236';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_UNSUBSCRIBE = '\uE0EB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_UPDATE = '\uE923';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_UPDATE_DISABLED = '\uE075';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_UPGRADE = '\uF0FB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_UPLOAD_FILE = '\uE9FC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_USB = '\uE1E0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VERIFIED = '\uEF76';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VERIFIED_USER = '\uE8E8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VERTICAL_ALIGN_BOTTOM = '\uE258';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VERTICAL_ALIGN_CENTER = '\uE259';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VERTICAL_ALIGN_TOP = '\uE25A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VERTICAL_DISTRIBUTE = '\uE076';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VERTICAL_SPLIT = '\uE949';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIBRATION = '\uE62D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIDEO_CALL = '\uE070';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIDEO_COLLECTION = '\uE04A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIDEO_LABEL = '\uE071';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIDEO_LIBRARY = '\uE04A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIDEO_SETTINGS = '\uEA75';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIDEOCAM = '\uE04B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIDEOCAM_OFF = '\uE04C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIDEOGAME_ASSET = '\uE338';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_AGENDA = '\uE8E9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_ARRAY = '\uE8EA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_CAROUSEL = '\uE8EB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_COLUMN = '\uE8EC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_COMFORTABLE = '\uE42A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_COMFY = '\uE42A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_COMPACT = '\uE42B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_DAY = '\uE8ED';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_HEADLINE = '\uE8EE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_IN_AR = '\uE9FE';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_LIST = '\uE8EF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_MODULE = '\uE8F0';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_QUILT = '\uE8F1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_SIDEBAR = '\uF114';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_STREAM = '\uE8F2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIEW_WEEK = '\uE8F3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VIGNETTE = '\uE435';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VISIBILITY = '\uE8F4';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VISIBILITY_OFF = '\uE8F5';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VOICE_CHAT = '\uE62E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VOICE_OVER_OFF = '\uE94A';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VOICEMAIL = '\uE0D9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VOLUME_DOWN = '\uE04D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VOLUME_MUTE = '\uE04E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VOLUME_OFF = '\uE04F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VOLUME_UP = '\uE050';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VOLUNTEER_ACTIVISM = '\uEA70';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VPN_KEY = '\uE0DA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_VPN_LOCK = '\uE62F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WALLET_GIFTCARD = '\uE8F6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WALLET_MEMBERSHIP = '\uE8F7';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WALLET_TRAVEL = '\uE8F8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WALLPAPER = '\uE1BC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WARNING = '\uE002';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WASH = '\uF1B1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WATCH = '\uE334';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WATCH_LATER = '\uE924';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WATER_DAMAGE = '\uF203';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WATERFALL_CHART = '\uEA00';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WAVES = '\uE176';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WB_AUTO = '\uE42C';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WB_CLOUDY = '\uE42D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WB_INCANDESCENT = '\uE42E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WB_IRIDESCENT = '\uE436';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WB_SHADE = '\uEA01';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WB_SUNNY = '\uE430';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WB_TWIGHLIGHT = '\uEA02';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WC = '\uE63D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WEB = '\uE051';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WEB_ASSET = '\uE069';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WEEKEND = '\uE16B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WEST = '\uF1E6';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WHATSHOT = '\uE80E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WHEELCHAIR_PICKUP = '\uF1AB';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WHERE_TO_VOTE = '\uE177';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WIDGETS = '\uE1BD';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WIFI = '\uE63E';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WIFI_CALLING = '\uEF77';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WIFI_LOCK = '\uE1E1';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WIFI_OFF = '\uE648';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WIFI_PROTECTED_SETUP = '\uF0FC';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WIFI_TETHERING = '\uE1E2';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WINE_BAR = '\uF1E8';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WORK = '\uE8F9';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WORK_OFF = '\uE942';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WORK_OUTLINE = '\uE943';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WORKSPACES_FILLED = '\uEA0D';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WORKSPACES_OUTLINE = '\uEA0F';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WRAP_TEXT = '\uE25B';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WRONG_LOCATION = '\uEF78';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_WYSIWYG = '\uF1C3';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_YOUTUBE_SEARCHED_FOR = '\uE8FA';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ZOOM_IN = '\uE8FF';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ZOOM_OUT = '\uE900';
    /**
     * Material design icon font character code see
     * https://www.material.io/resources/icons/ for full list
     */
    public static final char MATERIAL_ZOOM_OUT_MAP = '\uE56B';

    private static Font materialDesignFont;

    /**
     * The material design icon font allows creating icons based on the material
     * design icon catalog
     *
     * @return the font that can be used to create font image instances.
     */
    public static Font getMaterialDesignFont() {
        if (materialDesignFont == null) {
            if(Font.isTrueTypeFileSupported()) {
                materialDesignFont = Font.createTrueTypeFont("Material Icons", "material-design-font.ttf");
            } else {
                materialDesignFont = Font.getDefaultFont();
            }
        } 
        return materialDesignFont;
    }

    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icon constants) to the given label using the 
     * styling of the label. Notice that when the argument is a button the pressed/selected &amp; disabled states
     * will be set appropriately.</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * 
     * @param l a label or subclass (e.g. Button etc.)
     * @param icon one of the MATERIAL_* icons
     */
    public static void setMaterialIcon(Label l, char icon) {
        setMaterialIcon(l, icon, -1);
    }
    
    private static int rightSize(Style s, float size) {
        if(size > 0) {
            return Display.getInstance().convertToPixels(size);
        }
        return s.getFont().getHeight();
    }
    
    /**
     * Helper method that generalizes icon setting code for various component types
     * @param cmp the component, currently supports Labels, MultiButton &amp; SpanButton. If the type isn't supported 
     * this method return false
     * @param icon the material icon
     * @param size the size of the icon
     * @return false if the type isn't supported
     */
    public static boolean setMaterialIcon(Component cmp, char icon, float size) {
        if(cmp instanceof IconHolder) {
            setIcon((IconHolder)cmp, icon, size);
            return true;
        }
       
        return false;
    }
    
    /**
     * Helper method that generalizes icon setting code for various component types
     * @param cmp the component, currently supports Labels, MultiButton &amp; SpanButton. If the type isn't supported 
     * this method return false
     * @param icons icons List of icons to use.  [Unselected, Selected, Pressed, Pressed-Selected, Disabled].  If fewer icons are supplied than requested, it will use the last icon.
     * @param size the size of the icon
     * @return false if the type isn't supported
     * @since 7.0
     */
    public static boolean setMaterialIcon(Component cmp, char[] icons, float size) {
        if(cmp instanceof IconHolder) {
            setIcon((IconHolder)cmp, getMaterialDesignFont(), icons, size);
            return true;
        }
        
        return false;
    }

    /**
     * <p>Applies an icon from the font to the given label using the 
     * styling of the label. Notice that when the argument is a button the pressed/selected &amp; disabled states
     * will be set appropriately.</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * 
     * @param l a label or subclass (e.g. Button etc.)
     * @param font a font with icons
     * @param icon an icon in the font
     * @param size the size of the icon in millimeters, or -1 to use the default height of the font
     */
    public static void setFontIcon(Label l, Font font, char icon, float size) {
        setIcon((IconHolder)l, font, icon, size);
    }
    
    
        /**
     * <p>Applies an icon from the font to the given label using the 
     * styling of the label. Notice that when the argument is a button the pressed/selected &amp; disabled states
     * will be set appropriately.</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * 
     * @param l a label or subclass (e.g. Button etc.)
     * @param font a font with icons
     * @param icon an icon in the font
     * @param size the size of the icon in millimeters, or -1 to use the default height of the font
     */
    public static void setIcon(IconHolder l, Font font, char icon, float size) {
        Style s = new Style(l.getIconStyleComponent().getUnselectedStyle());
        s.setFont(font.derive(rightSize(s, size), Font.STYLE_PLAIN));
        l.setIcon(FontImage.create("" + icon, s));
        if (l instanceof SelectableIconHolder) {
            SelectableIconHolder b = (SelectableIconHolder) l;
            Style sel = b.getIconStyleComponent().getSelectedStyle();
            Style pre = b.getIconStyleComponent().getPressedStyle();
            Style dis = b.getIconStyleComponent().getDisabledStyle();
            if (sel.getFgColor() != s.getFgColor() || (sel.getBgColor() != s.getBgColor()) || (sel.getBgTransparency() != s.getBgTransparency())) {
                sel = new Style(sel);
                sel.setFont(font.derive(rightSize(sel, size), Font.STYLE_PLAIN));
                b.setRolloverIcon(FontImage.create("" + icon, sel));
            } else {
                b.setRolloverIcon(null);
            }
            if (pre.getFgColor() != s.getFgColor() || (pre.getBgColor() != s.getBgColor()) || (pre.getBgTransparency() != s.getBgTransparency())) {
                pre = new Style(pre);
                pre.setFont(font.derive(rightSize(pre, size), Font.STYLE_PLAIN));
                b.setPressedIcon(FontImage.create("" + icon, pre));
                b.setRolloverPressedIcon(FontImage.create("" + icon, pre));
            } else {
                b.setPressedIcon(null);
            }
            if (dis.getFgColor() != s.getFgColor() || (dis.getBgColor() != s.getBgColor()) || (dis.getBgTransparency() != s.getBgTransparency())) {
                dis = new Style(dis);
                dis.setFont(font.derive(rightSize(dis, size), Font.STYLE_PLAIN));
                b.setDisabledIcon(FontImage.create("" + icon, dis));
            } else {
                b.setDisabledIcon(null);
            }
        }
    }
    
    /**
     * <p>Applies icon font from the font to the given label using the styling of the label.</p>
     * @param l The label or subclass(e.g. Button etc.)
     * @param font The font with icons.
     * @param icons List of icons to use.  [Unselected, Selected, Pressed, Pressed-Selected, Disabled].  If fewer icons are supplied than requested, it will use the last icon,
     * in the list for the remaining expected icons.
     * @param size The font size.
     * @since 7.0
     */
    public static void setFontIcon(Label l, Font font, char[] icons, float size) {
        FontImage.setIcon((IconHolder)l, font, icons, size);
    }
    
    /**
     * <p>Applies icon font from the font to the given label using the styling of the label.</p>
     * @param l The label or subclass(e.g. Button etc.)
     * @param font The font with icons.
     * @param icons List of icons to use.  [Unselected, Selected, Pressed, Pressed-Selected, Disabled].  If fewer icons are supplied than requested, it will use the last icon,
     * in the list for the remaining expected icons.
     * @param size The font size.
     * @since 7.0
     */
    public static void setIcon(IconHolder l, Font font, char[] icons, float size) {
        Style s = new Style(l.getIconStyleComponent().getUnselectedStyle());
        s.setFont(font.derive(rightSize(s, size), Font.STYLE_PLAIN));
        l.setIcon(FontImage.create("" + icons[0], s));
        if (l instanceof SelectableIconHolder) {
            SelectableIconHolder b = (SelectableIconHolder) l;
            Style sel = b.getIconStyleComponent().getSelectedStyle();
            Style pre = b.getIconStyleComponent().getPressedStyle();
            Style dis = b.getIconStyleComponent().getDisabledStyle();
            if (icons[Math.min(1, icons.length-1)] != icons[0] || sel.getFgColor() != s.getFgColor() || (sel.getBgColor() != s.getBgColor()) || (sel.getBgTransparency() != s.getBgTransparency())) {
                sel = new Style(sel);
                sel.setFont(font.derive(rightSize(sel, size), Font.STYLE_PLAIN));
                b.setRolloverIcon(FontImage.create("" + icons[Math.min(1, icons.length-1)], sel));
            } else {
                b.setRolloverIcon(null);
            }
            if (icons[Math.min(2, icons.length-1)] != icons[0] || pre.getFgColor() != s.getFgColor() || (pre.getBgColor() != s.getBgColor()) || (pre.getBgTransparency() != s.getBgTransparency())) {
                pre = new Style(pre);
                pre.setFont(font.derive(rightSize(pre, size), Font.STYLE_PLAIN));
                b.setPressedIcon(FontImage.create("" + icons[Math.min(2, icons.length-1)], pre));
                b.setRolloverPressedIcon(FontImage.create("" + icons[Math.min(3, icons.length-1)], pre));
            } else {
                b.setPressedIcon(null);
            }
            if (icons[Math.min(4, icons.length-1)] != icons[0] || dis.getFgColor() != s.getFgColor() || (dis.getBgColor() != s.getBgColor()) || (dis.getBgTransparency() != s.getBgTransparency())) {
                dis = new Style(dis);
                dis.setFont(font.derive(rightSize(dis, size), Font.STYLE_PLAIN));
                b.setDisabledIcon(FontImage.create("" + icons[Math.min(4, icons.length-1)], dis));
            } else {
                b.setDisabledIcon(null);
            }
        }
    }
    
    public static void setFontIcon(Label l, Font font, char icon) {
        setFontIcon(l, font, icon, -1);
    }
    
    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icon constants) to the given label using the 
     * styling of the label. Notice that when the argument is a button the pressed/selected &amp; disabled states
     * will be set appropriately.</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * 
     * @param l a label or subclass (e.g. Button etc.)
     * @param icon an icon in the font
     * @param size the size of the icon in millimeters, or -1 to use the default height of the font
    */
    public static void setMaterialIcon(Label l, char icon, float size) {
        setFontIcon(l, getMaterialDesignFont(), icon, size);
    }
    
    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icon constants) to the given label using the 
     * styling of the label. Notice that when the argument is a button the pressed/selected &amp; disabled states
     * will be set appropriately.</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * 
     * @param l a label or subclass (e.g. Button etc.)
     * @param icons List of icons to use.  [Unselected, Selected, Pressed, Pressed-Selected, Disabled].  If fewer icons are supplied than requested, it will use the last icon.
     * @param size the size of the icon in millimeters, or -1 to use the default height of the font
     * @since 7.0
    */
    public static void setMaterialIcon(Label l, char[] icons, float size) {
        setFontIcon(l, getMaterialDesignFont(), icons, size);
    }


    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icon constants) to the given label using the 
     * styling of the label. Notice that when the argument is a button the pressed/selected &amp; disabled states
     * will be set appropriately.</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * 
     * @param l a label or subclass (e.g. Button etc.)
     * @param icon one of the MATERIAL_* icons
     * @param size the size of the icon in millimeters
     */
    public static void setMaterialIcon(Label l, Font font, char icon, float size) {
        setFontIcon(l, getMaterialDesignFont(), icon, size);
    }

    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icon constants) to the given command using the 
     * given UIID. Notice that the pressed/selected &amp; disabled states will be set appropriately.</p>
     * 
     * @param c a command 
     * @param icon one of the MATERIAL_* icons
     * @param uiid the UIID for the command (e.g. TitleCommand)
     */
    public static void setMaterialIcon(Command c, char icon, String uiid) {
        setMaterialIcon(c, icon, uiid, -1);
    }

    
    /**
     * <p>Applies an icon from the font to the given command using the 
     * given UIID. Notice that the pressed/selected &amp; disabled states will be set appropriately.</p>
     * 
     * @param c a command 
     * @param font a font with icons
     * @param icon an icon in the font
     * @param uiid the UIID for the command (e.g. TitleCommand)
     * @param size the size of the icon in millimeters
     */
    public static void setFontIcon(Command c, Font font, char icon, String uiid, float size) {
        UIManager uim = UIManager.getInstance();
        Style s = uim.getComponentStyle(uiid);
        s.setFont(font.derive(rightSize(s, size), Font.STYLE_PLAIN));
        c.setIcon(FontImage.create("" + icon, s));
        Style sel = uim.getComponentSelectedStyle(uiid);
        Style pre = uim.getComponentCustomStyle(uiid, "press");
        Style dis = uim.getComponentCustomStyle(uiid, "dis");;
        if(sel.getFgColor() != s.getFgColor() || (sel.getBgColor() != s.getBgColor()) || (sel.getBgTransparency() != s.getBgTransparency())) {
            sel = new Style(sel);
            sel.setFont(font.derive(rightSize(sel, size), Font.STYLE_PLAIN));
            c.setRolloverIcon(FontImage.create("" + icon, sel));
        }
        if(pre.getFgColor() != s.getFgColor() || (pre.getBgColor() != s.getBgColor()) || (pre.getBgTransparency() != s.getBgTransparency())) {
            pre = new Style(pre);
            pre.setFont(font.derive(rightSize(pre, size), Font.STYLE_PLAIN));
            c.setPressedIcon(FontImage.create("" + icon, pre));
        }
        if(dis.getFgColor() != s.getFgColor() || (dis.getBgColor() != s.getBgColor()) || (dis.getBgTransparency() != s.getBgTransparency())) {
            dis = new Style(dis);
            dis.setFont(font.derive(rightSize(dis, size), Font.STYLE_PLAIN));
            c.setDisabledIcon(FontImage.create("" + icon, dis));
        }
    }
    
    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icon constants) to the given command using the 
     * given UIID. Notice that the pressed/selected &amp; disabled states will be set appropriately.</p>
     * 
     * @param c a command 
     * @param icon one of the MATERIAL_* icons
     * @param uiid the UIID for the command (e.g. TitleCommand)
     * @param size the size of the icon in millimeters
     */
     public static void setMaterialIcon(Command c, char icon, String uiid, float size) {
        if (Font.isTrueTypeFileSupported()) {
            setFontIcon(c, getMaterialDesignFont(), icon, uiid, size);
        }
    }
    
    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icons above) to the given component using the 
     * styling of the label</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * @param l a multibutton
     * @param icon one of the MATERIAL_* icons
     */
    public static void setMaterialIcon(MultiButton l, char icon) {
        setMaterialIcon(l, icon, -1);
    }
    
    /**
     * <p>Applies an icon from the font to the given component using the 
     * styling of the label</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * @param l a multibutton
     * @param font a font with icons
     * @param icon an icon in the font
     * @param size the size of the icon in millimeters
     */
    public static void setFontIcon(MultiButton l, Font font, char icon, float size) {
        setIcon((IconHolder)l, font, icon, size);
    }
    
    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icons above) to the given component using the 
     * styling of the label</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * @param l a multibutton
     * @param icon one of the MATERIAL_* icons
     * @param size the size of the icon in millimeters
     */
    public static void setMaterialIcon(MultiButton l, char icon, float size) {
        setFontIcon(l, getMaterialDesignFont(), icon, size);
    }
    
    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icons above) to the given component using the 
     * styling of the label</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * @param l a SpanLabel
     * @param icon one of the MATERIAL_* icons
     */
    public static void setMaterialIcon(SpanLabel l, char icon) {
        setMaterialIcon(l, icon, -1);
    }

    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icons above) to the given component using the 
     * styling of the label</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * @param l a IconHolder
     * @param icon one of the MATERIAL_* icons
     * @since 7.0
     */
    public static void setMaterialIcon(IconHolder l, char icon) {
        FontImage.setIcon(l, icon, -1);
    }
    
    /**
     * <p>Applies an icon from the font to the given component using the 
     * styling of the label</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * @param l a SpanLabel
     * @param font a font with icons
     * @param icon an icon in the font
     * @param size the size of the icon in millimeters
     */
    public static void setFontIcon(SpanLabel l, Font font, char icon, float size) {
        setIcon((IconHolder)l, font, icon, size);
    }
    
    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icons above) to the given component using the 
     * styling of the label</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * @param l a SpanLabel
     * @param icon one of the MATERIAL_* icons
     * @param size the size of the icon in millimeters
     */
    public static void setMaterialIcon(SpanLabel l, char icon, float size) {
        setFontIcon(l, getMaterialDesignFont(), icon, size);
    } 
    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icons above) to the given component using the 
     * styling of the label</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * @param l a SpanLabel
     * @param icon one of the MATERIAL_* icons
     * @param size the size of the icon in millimeters
     */
    public static void setIcon(IconHolder l, char icon, float size) {
        setIcon(l, getMaterialDesignFont(), icon, size);
    }
    
    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icons above) to the given component using the 
     * styling of the label</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * @param l a SpanButton
     * @param icon one of the MATERIAL_* icons
     */
    public static void setMaterialIcon(SpanButton l, char icon) {
        setMaterialIcon(l, icon, -1);
    }
    
   
    /**
     * <p>Applies an icon from the font to the given component using the 
     * styling of the label</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * @param l a SpanButton
     * @param font a font with icons
     * @param icon an icon in the font
     */
    public static void setFontIcon(SpanButton l, Font font, char icon) {
        setFontIcon(l, font, icon, -1);
    }  
    
    /**
     * <p>Applies an icon from the font to the given component using the 
     * styling of the label</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * @param l a SpanButton
     * @param font a font with icons
     * @param icon an icon in the font
     * @param size the size of the icon in millimeters
     */
    public static void setFontIcon(SpanButton l, Font font, char icon, float size) {
        setIcon((SelectableIconHolder)l, font, icon, size);
    }
    
    
    
    
    
    /**
     * <p>Applies a material design icon (one of the MATERIAL_* icons above) to the given component using the 
     * styling of the label</p>
     * <script src="https://gist.github.com/codenameone/8cf6f70188959524474b.js"></script>
     * @param l a SpanButton
     * @param icon one of the MATERIAL_* icons
     * @param size the size of the icon in millimeters
     */
    public static void setMaterialIcon(SpanButton l, char icon, float size) {
        setFontIcon(l, getMaterialDesignFont(), icon, size);
    }
    

    
    /**
     * Default factor for image size, icons without a given size are sized as
     * defaultSize X default font height.
     *
     * @return the defaultSize
     */
    public static float getDefaultSize() {
        return defaultSize;
    }

    /**
     * Default factor for image size, icons without a given size are sized as
     * defaultSize X default font height.
     *
     * @param aDefaultSize the defaultSize to set
     */
    public static void setDefaultSize(float aDefaultSize) {
        defaultSize = aDefaultSize;
    }

    private static int defaultPadding = 1;

    /**
     * Indicates the default value for the padding in millimeters
     *
     * @return the defaultPadding
     */
    public static int getDefaultPadding() {
        return defaultPadding;
    }

    /**
     * Indicates the default value for the padding in millimeters
     *
     * @param aDefaultPadding the defaultPadding to set
     */
    public static void setDefaultPadding(int aDefaultPadding) {
        defaultPadding = aDefaultPadding;
    }

    /**
     * The padding for the image in millimeters
     */
    private int padding = defaultPadding;

    private int width;
    private int height;
    private int color;
    private Font fnt;
    private String text;
    private int rotated;
    private int backgroundColor;
    private byte backgroundOpacity;
    private int opacity=-1;

    private Motion rotationMotion;
    
    /**
     * Default factor for image size, icons without a given size are sized as
     * defaultSize X default font height.
     */
    private static float defaultSize = 2.5f;

    private FontImage() {
        super(null);
    }

    /**
     * <p>Creates a font image with a fixed size/appearance</p>
     *
     * <script src="https://gist.github.com/codenameone/9c881350e1d142081aba.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/graphics-fontimage-fixed.png" alt="Icon font from material design icons created with the fixed size of display width" />
     * 
     * @param text the text of the font image
     * @param fnt the font
     * @param color the color for the image foreground
     * @param width the width in pixels
     * @param height the height in pixels
     * @return the image instance
     */
    public static FontImage createFixed(String text, Font fnt, int color, int width, int height) {
        FontImage f = new FontImage();
        f.text = text;
        f.color = color;
        f.width = width;
        f.fnt = sizeFont(fnt, Math.min(width, height), f.padding);
        f.height = height;
        return f;
    }

    /**
     * <p>Creates a font image with a fixed size/appearance</p>
     *
     * <script src="https://gist.github.com/codenameone/9c881350e1d142081aba.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/graphics-fontimage-fixed.png" alt="Icon font from material design icons created with the fixed size of display width" />
     * 
     * @param text the text of the font image
     * @param fnt the font
     * @param color the color for the image foreground
     * @param width the width in pixels
     * @param height the height in pixels
     * @param padding the padding size in pixels
     * @return the image instance
     */
    public static FontImage createFixed(String text, Font fnt, int color, int width, int height, int padding) {
        FontImage f = new FontImage();
        f.text = text;
        f.color = color;
        f.width = width;
        f.fnt = sizeFont(fnt, Math.min(width, height), padding);
        f.height = height;
        f.padding = padding;
        return f;
    }
    
    /**
     * <p>Creates the font image based on the given style, the font in the style is assumed to be an icon font</p>
     *  
     * <script src="https://gist.github.com/codenameone/da3912b9ccef03f58058.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/graphics-fontimage-style.png" alt="An image created from the Style object" />
     *
     * @param text the text for the font image
     * @param s the style
     * @return the font image
     */
    public static FontImage create(String text, Style s) {
        return create(text, s, s.getFont());
    }

    /**
     * <p>Creates the font image with the given style settings but uses the given font, notice that the
     * size of the given font determines the size of the icon!</p>
     *  
     * <script src="https://gist.github.com/codenameone/da3912b9ccef03f58058.js"></script>
     * <img src="https://www.codenameone.com/img/developer-guide/graphics-fontimage-style.png" alt="An image created from the Style object" />
     *
     * @param text the text for the font image
     * @param s the style
     * @param fnt the icon font used (needs to be sized correctly!)
     * @return the font image
     */
    public static FontImage create(String text, Style s, Font fnt) {
        FontImage f = new FontImage();
        f.backgroundOpacity = s.getBgTransparency();
        f.backgroundColor = s.getBgColor();
        f.text = text;
        f.color = s.getFgColor();
        f.opacity = s.getOpacity();
        f.fnt = fnt;
        int w = Math.max(f.getHeight(), f.fnt.stringWidth(text)) + (f.padding * 2);
        f.width = w;
        f.height = w;
        return f;
    }
    
    /**
     * <p>Creates a material design icon font for the given style but size it in millimeters based
     * on the size argument and not the font</p>
     * <script src="https://gist.github.com/codenameone/34fd9e519ec3d305a015.js"></script>
     * 
     * @param icon the icon, one of the MATERIAL_* constants
     * @param s the style to use, notice the font in the style only matters in terms of size and nothing else
     * @param size the size in millimeters
     * @return a new icon
     */
    public static FontImage createMaterial(char icon, Style s, float size) {
        Font f = getMaterialDesignFont().derive(Display.getInstance().convertToPixels(size), Font.STYLE_PLAIN);
        return create("" + icon, s, f);
    }
    
    /**
     * <p>Creates a material design icon font for the given style but size it in millimeters based
     * on the size argument and not the font</p>
     * <script src="https://gist.github.com/codenameone/34fd9e519ec3d305a015.js"></script>
     * 
     * @param icon the icon, one of the MATERIAL_* constants
     * @param style the style to use, notice the font in the style only matters in terms of size and nothing else
     * @param size the size in millimeters
     * @return a new icon
     */
    public static FontImage createMaterial(char icon, String style, float size) {
        Style s = UIManager.getInstance().getComponentStyle(style);
        return createMaterial(icon, s, size);
    }

    /**
     * <p>Creates a material design icon font for the given style</p>
     * <script src="https://gist.github.com/codenameone/34fd9e519ec3d305a015.js"></script>
     * 
     * @param icon the icon, one of the MATERIAL_* constants
     * @param s the style to use, notice the font in the style only matters in terms of size and nothing else
     * @return a new icon
     */
    public static FontImage createMaterial(char icon, Style s) {
        Font f = getMaterialDesignFont().derive(s.getFont().getHeight(), Font.STYLE_PLAIN);
        return create("" + icon, s, f);
    }

    private static Font sizeFont(Font fnt, int w, int padding) {
        if(!Font.isTrueTypeFileSupported()) {
            return Font.getDefaultFont();
        }
        int paddingPixels = Display.getInstance().convertToPixels(padding, true);
        w -= paddingPixels;
        int h = fnt.getHeight();
        if (h != w) {
            return fnt.derive(w, Font.STYLE_PLAIN);
        }
        return fnt;
    }

    /**
     * Throws a runtime exception
     */
    public Graphics getGraphics() {
        throw new RuntimeException();
    }

    /**
     * Returns the width of the image
     *
     * @return the width of the image
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of the image
     *
     * @return the height of the image
     */
    public int getHeight() {
        return height;
    }

    /**
     * {@inheritDoc}
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        int oldColor = g.getColor();
        int oldAlpha = g.getAlpha();
        Font oldFont = g.getFont();

        if (opacity > 0 && opacity < 255) {
            g.setAlpha(opacity);
        }
        
        if (backgroundOpacity != 0) {
            g.setColor(backgroundColor);
            g.fillRect(x, y, width, height, (byte) backgroundOpacity);
        }

        g.setColor(color);
        g.setFont(fnt);
        int w = fnt.stringWidth(text);
        int h = Math.round(fnt.getPixelSize());
        if (h <= 0) h = fnt.getHeight();
        //int paddingPixels = Display.getInstance().convertToPixels(padding, true);
        if (rotated != 0) {
            int tX = g.getTranslateX();
            int tY = g.getTranslateY();
            g.translate(-tX, -tY);
            g.rotate((float) Math.toRadians(rotated % 360), tX + x + width / 2, tY + y + height / 2);
            g.drawString(text, tX + x + width / 2 - w / 2, tY + y + height / 2 - h / 2);
            g.resetAffine();
            g.translate(tX, tY);
        } else {
            g.drawString(text, x + width / 2 - w / 2, y + height / 2 - h / 2);
        }
        g.setFont(oldFont);
        g.setColor(oldColor);
        g.setAlpha(oldAlpha);
    }
    
    /**
     * {@inheritDoc}
     */
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
        if (w == width && h == height) {
            drawImage(g, nativeGraphics, x, y);
            return;
        }
        int oldColor = g.getColor();

        if (backgroundOpacity != 0) {
            g.setColor(backgroundColor);
            g.fillRect(x, y, w, h, (byte) backgroundOpacity);
        }

        Font oldFont = g.getFont();
        Font t = sizeFont(fnt, Math.min(h, w), padding);
        g.setColor(color);
        g.setFont(t);
        int ww = t.stringWidth(text);
        int hh = Math.round(t.getPixelSize());
        if (hh <= 0) {
            hh = t.getHeight();
        }
        //int paddingPixels = Display.getInstance().convertToPixels(padding, true);
        if (rotated != 0) {
            int tX = g.getTranslateX();
            int tY = g.getTranslateY();
            g.translate(-tX, -tY);
            g.rotate((float) Math.toRadians(rotated % 360), tX + x + w / 2, tY + y + h / 2);
            g.drawString(text, tX + x + w / 2 - ww / 2, tY + y);
            g.resetAffine();
            g.translate(tX, tY);
        } else {
            g.drawString(text, x + w / 2 - ww / 2, y + h/2 - hh/2);
        }
        g.setFont(oldFont);
        g.setColor(oldColor);
    }

    /**
     * The padding for the image in millimeters
     *
     * @return the padding
     */
    public int getPadding() {
        return padding;
    }

    /**
     * The padding for the image in millimeters
     *
     * @param padding the padding to set
     */
    public void setPadding(int padding) {
        if (this.padding != padding) {
            this.padding = padding;
            fnt = sizeFont(fnt, Math.min(width, height), padding);
        }
    }

    /**
     * Useful method to reuse the Font object when creating multiple image
     * objects
     *
     * @return the font used
     */
    public Font getFont() {
        return fnt;
    }

    void getRGB(int[] rgbData,
            int offset,
            int x,
            int y,
            int width,
            int height) {
        throw new RuntimeException("Unsupported Operation");
    }

    int[] getRGBImpl() {
        throw new RuntimeException("Unsupported Operation");
    }

    Image scaledImpl(int width, int height) {
        FontImage i = createFixed(text, fnt, color, width, height);
        i.backgroundColor = backgroundColor;
        i.backgroundOpacity = backgroundOpacity;
        i.padding = padding;
        i.rotated = rotated;
        return i;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAnimation() {
        return rotationMotion != null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean animate() {
        if(rotationMotion != null) {
            rotated = rotationMotion.getValue();
            if(rotationMotion.isFinished()) {
                rotationMotion = Motion.createLinearMotion(0, 360, 1500);
                rotationMotion.start();
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isOpaque() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public String getImageName() {
        return text;
    }

    /**
     * Does nothing
     */
    public void dispose() {
    }

    /**
     * {@inheritDoc}
     */
    public Image rotate(int degrees) {
        FontImage f = createFixed(text, fnt, color, width, height);
        f.rotated = degrees;
        f.opacity = opacity;
        return f;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image rotate90Degrees(boolean maintainOpacity) {
        FontImage f = createFixed(text, fnt, color, width, height);
        f.rotated = 90;
        f.opacity = maintainOpacity ? opacity : 255;
        return f; 
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image rotate270Degrees(boolean maintainOpacity) {
        FontImage f = createFixed(text, fnt, color, width, height);
        f.rotated = 270;
        f.opacity = maintainOpacity ? opacity : 255;
        return f; 
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Image rotate180Degrees(boolean maintainOpacity) {
        FontImage f = createFixed(text, fnt, color, width, height);
        f.rotated = 180;
        f.opacity = maintainOpacity ? opacity : 255;
        return f; 
    }
    
    

        
    /**
     * Converts the icon image to an encoded image if possible
     *
     * @return the encoded image or null if the operation failed
     */
    public EncodedImage toEncodedImage() {
        ImageIO io = ImageIO.getImageIO();
        if (io != null && io.isFormatSupported(ImageIO.FORMAT_PNG)) {
            Image img = toImage();
            if (img != null) {
                return EncodedImage.createFromImage(img, false);
            }
        }
        return null;
    }

    /**
     * Converts the icon image to an image if possible
     *
     * @return the encoded image or null if the operation failed
     */
    public Image toImage() {
        if (Image.isAlphaMutableImageSupported()) {
            Image img = Image.createImage(width, height, 0);
            Graphics g = img.getGraphics();
            g.drawImage(this, 0, 0);
            return img;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requiresDrawImage() {
        return true;
    }
    
    /**
     * Returns the text of the icon
     * @return the text used to draw the icon
     */
    public String getText() {
        return text;
    }
    
    /**
     * Allows manipulating the background transparency value of the image
     * @param t a value between 0 and 255 where 0 is fully transparent
     */
    public void setBgTransparency(int t) {
        backgroundOpacity = (byte)t;
    }
    
    /**
     * Creates a copy of this image that rotates itself in an animation
     * @return a copy of the image
     */
    public FontImage rotateAnimation() {
        FontImage newImage = (FontImage)rotate(0);
        newImage.rotationMotion = Motion.createLinearMotion(0, 360, 1500);
        newImage.rotationMotion.start();
        return newImage;
    }
}
