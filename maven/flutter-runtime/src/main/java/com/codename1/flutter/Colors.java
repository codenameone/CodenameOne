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

import com.codename1.generated.flutter.MaterialAccentColor;
import com.codename1.generated.flutter.MaterialColor;

/**
 * The material color swatch primaries (500 values), accent variants, and the
 * black/white opacity constants, mirroring Flutter's {@code Colors}.
 *
 * <p>The named primaries are typed {@link MaterialColor} and the accents
 * {@link MaterialAccentColor} (both extend {@link Color}) so the colors demo can
 * index their shades — matching Flutter, where {@code Colors.red} is a swatch,
 * not a plain color.</p>
 */
public final class Colors {

    private Colors() {
    }

    public static final Color transparent = new Color(0x00000000);

    public static final MaterialColor red = new MaterialColor(0xFFF44336L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFFFEBEEL, 0xFFFFCDD2L, 0xFFEF9A9AL, 0xFFE57373L, 0xFFEF5350L, 0xFFF44336L, 0xFFE53935L, 0xFFD32F2FL, 0xFFC62828L, 0xFFB71C1CL});
    public static final MaterialAccentColor redAccent = new MaterialAccentColor(0xFFFF5252L,
            new long[] {100, 200, 400, 700},
            new long[] {0xFFFF8A80L, 0xFFFF5252L, 0xFFFF1744L, 0xFFD50000L});
    public static final MaterialColor pink = new MaterialColor(0xFFE91E63L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFFCE4ECL, 0xFFF8BBD0L, 0xFFF48FB1L, 0xFFF06292L, 0xFFEC407AL, 0xFFE91E63L, 0xFFD81B60L, 0xFFC2185BL, 0xFFAD1457L, 0xFF880E4FL});
    public static final MaterialAccentColor pinkAccent = new MaterialAccentColor(0xFFFF4081L,
            new long[] {100, 200, 400, 700},
            new long[] {0xFFFF80ABL, 0xFFFF4081L, 0xFFF50057L, 0xFFC51162L});
    public static final MaterialColor purple = new MaterialColor(0xFF9C27B0L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFF3E5F5L, 0xFFE1BEE7L, 0xFFCE93D8L, 0xFFBA68C8L, 0xFFAB47BCL, 0xFF9C27B0L, 0xFF8E24AAL, 0xFF7B1FA2L, 0xFF6A1B9AL, 0xFF4A148CL});
    public static final MaterialAccentColor purpleAccent = new MaterialAccentColor(0xFFE040FBL,
            new long[] {100, 200, 400, 700},
            new long[] {0xFFEA80FCL, 0xFFE040FBL, 0xFFD500F9L, 0xFFAA00FFL});
    public static final MaterialColor deepPurple = new MaterialColor(0xFF673AB7L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFEDE7F6L, 0xFFD1C4E9L, 0xFFB39DDBL, 0xFF9575CDL, 0xFF7E57C2L, 0xFF673AB7L, 0xFF5E35B1L, 0xFF512DA8L, 0xFF4527A0L, 0xFF311B92L});
    public static final MaterialAccentColor deepPurpleAccent = new MaterialAccentColor(0xFF7C4DFFL,
            new long[] {100, 200, 400, 700},
            new long[] {0xFFB388FFL, 0xFF7C4DFFL, 0xFF651FFFL, 0xFF6200EAL});
    public static final MaterialColor indigo = new MaterialColor(0xFF3F51B5L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFE8EAF6L, 0xFFC5CAE9L, 0xFF9FA8DAL, 0xFF7986CBL, 0xFF5C6BC0L, 0xFF3F51B5L, 0xFF3949ABL, 0xFF303F9FL, 0xFF283593L, 0xFF1A237EL});
    public static final MaterialAccentColor indigoAccent = new MaterialAccentColor(0xFF536DFEL,
            new long[] {100, 200, 400, 700},
            new long[] {0xFF8C9EFFL, 0xFF536DFEL, 0xFF3D5AFEL, 0xFF304FFEL});
    public static final MaterialColor blue = new MaterialColor(0xFF2196F3L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFE3F2FDL, 0xFFBBDEFBL, 0xFF90CAF9L, 0xFF64B5F6L, 0xFF42A5F5L, 0xFF2196F3L, 0xFF1E88E5L, 0xFF1976D2L, 0xFF1565C0L, 0xFF0D47A1L});
    public static final MaterialAccentColor blueAccent = new MaterialAccentColor(0xFF448AFFL,
            new long[] {100, 200, 400, 700},
            new long[] {0xFF82B1FFL, 0xFF448AFFL, 0xFF2979FFL, 0xFF2962FFL});
    public static final MaterialColor lightBlue = new MaterialColor(0xFF03A9F4L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFE1F5FEL, 0xFFB3E5FCL, 0xFF81D4FAL, 0xFF4FC3F7L, 0xFF29B6F6L, 0xFF03A9F4L, 0xFF039BE5L, 0xFF0288D1L, 0xFF0277BDL, 0xFF01579BL});
    public static final MaterialAccentColor lightBlueAccent = new MaterialAccentColor(0xFF40C4FFL,
            new long[] {100, 200, 400, 700},
            new long[] {0xFF80D8FFL, 0xFF40C4FFL, 0xFF00B0FFL, 0xFF0091EAL});
    public static final MaterialColor cyan = new MaterialColor(0xFF00BCD4L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFE0F7FAL, 0xFFB2EBF2L, 0xFF80DEEAL, 0xFF4DD0E1L, 0xFF26C6DAL, 0xFF00BCD4L, 0xFF00ACC1L, 0xFF0097A7L, 0xFF00838FL, 0xFF006064L});
    public static final MaterialAccentColor cyanAccent = new MaterialAccentColor(0xFF18FFFFL,
            new long[] {100, 200, 400, 700},
            new long[] {0xFF84FFFFL, 0xFF18FFFFL, 0xFF00E5FFL, 0xFF00B8D4L});
    public static final MaterialColor teal = new MaterialColor(0xFF009688L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFE0F2F1L, 0xFFB2DFDBL, 0xFF80CBC4L, 0xFF4DB6ACL, 0xFF26A69AL, 0xFF009688L, 0xFF00897BL, 0xFF00796BL, 0xFF00695CL, 0xFF004D40L});
    public static final MaterialAccentColor tealAccent = new MaterialAccentColor(0xFF64FFDAL,
            new long[] {100, 200, 400, 700},
            new long[] {0xFFA7FFEBL, 0xFF64FFDAL, 0xFF1DE9B6L, 0xFF00BFA5L});
    public static final MaterialColor green = new MaterialColor(0xFF4CAF50L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFE8F5E9L, 0xFFC8E6C9L, 0xFFA5D6A7L, 0xFF81C784L, 0xFF66BB6AL, 0xFF4CAF50L, 0xFF43A047L, 0xFF388E3CL, 0xFF2E7D32L, 0xFF1B5E20L});
    public static final MaterialAccentColor greenAccent = new MaterialAccentColor(0xFF69F0AEL,
            new long[] {100, 200, 400, 700},
            new long[] {0xFFB9F6CAL, 0xFF69F0AEL, 0xFF00E676L, 0xFF00C853L});
    public static final MaterialColor lightGreen = new MaterialColor(0xFF8BC34AL,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFF1F8E9L, 0xFFDCEDC8L, 0xFFC5E1A5L, 0xFFAED581L, 0xFF9CCC65L, 0xFF8BC34AL, 0xFF7CB342L, 0xFF689F38L, 0xFF558B2FL, 0xFF33691EL});
    public static final MaterialAccentColor lightGreenAccent = new MaterialAccentColor(0xFFB2FF59L,
            new long[] {100, 200, 400, 700},
            new long[] {0xFFCCFF90L, 0xFFB2FF59L, 0xFF76FF03L, 0xFF64DD17L});
    public static final MaterialColor lime = new MaterialColor(0xFFCDDC39L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFF9FBE7L, 0xFFF0F4C3L, 0xFFE6EE9CL, 0xFFDCE775L, 0xFFD4E157L, 0xFFCDDC39L, 0xFFC0CA33L, 0xFFAFB42BL, 0xFF9E9D24L, 0xFF827717L});
    public static final MaterialAccentColor limeAccent = new MaterialAccentColor(0xFFEEFF41L,
            new long[] {100, 200, 400, 700},
            new long[] {0xFFF4FF81L, 0xFFEEFF41L, 0xFFC6FF00L, 0xFFAEEA00L});
    public static final MaterialColor yellow = new MaterialColor(0xFFFFEB3BL,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFFFFDE7L, 0xFFFFF9C4L, 0xFFFFF59DL, 0xFFFFF176L, 0xFFFFEE58L, 0xFFFFEB3BL, 0xFFFDD835L, 0xFFFBC02DL, 0xFFF9A825L, 0xFFF57F17L});
    public static final MaterialAccentColor yellowAccent = new MaterialAccentColor(0xFFFFFF00L,
            new long[] {100, 200, 400, 700},
            new long[] {0xFFFFFF8DL, 0xFFFFFF00L, 0xFFFFEA00L, 0xFFFFD600L});
    public static final MaterialColor amber = new MaterialColor(0xFFFFC107L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFFFF8E1L, 0xFFFFECB3L, 0xFFFFE082L, 0xFFFFD54FL, 0xFFFFCA28L, 0xFFFFC107L, 0xFFFFB300L, 0xFFFFA000L, 0xFFFF8F00L, 0xFFFF6F00L});
    public static final MaterialAccentColor amberAccent = new MaterialAccentColor(0xFFFFD740L,
            new long[] {100, 200, 400, 700},
            new long[] {0xFFFFE57FL, 0xFFFFD740L, 0xFFFFC400L, 0xFFFFAB00L});
    public static final MaterialColor orange = new MaterialColor(0xFFFF9800L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFFFF3E0L, 0xFFFFE0B2L, 0xFFFFCC80L, 0xFFFFB74DL, 0xFFFFA726L, 0xFFFF9800L, 0xFFFB8C00L, 0xFFF57C00L, 0xFFEF6C00L, 0xFFE65100L});
    public static final MaterialAccentColor orangeAccent = new MaterialAccentColor(0xFFFFAB40L,
            new long[] {100, 200, 400, 700},
            new long[] {0xFFFFD180L, 0xFFFFAB40L, 0xFFFF9100L, 0xFFFF6D00L});
    public static final MaterialColor deepOrange = new MaterialColor(0xFFFF5722L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFFBE9E7L, 0xFFFFCCBCL, 0xFFFFAB91L, 0xFFFF8A65L, 0xFFFF7043L, 0xFFFF5722L, 0xFFF4511EL, 0xFFE64A19L, 0xFFD84315L, 0xFFBF360CL});
    public static final MaterialAccentColor deepOrangeAccent = new MaterialAccentColor(0xFFFF6E40L,
            new long[] {100, 200, 400, 700},
            new long[] {0xFFFF9E80L, 0xFFFF6E40L, 0xFFFF3D00L, 0xFFDD2C00L});
    public static final MaterialColor brown = new MaterialColor(0xFF795548L,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFEFEBE9L, 0xFFD7CCC8L, 0xFFBCAAA4L, 0xFFA1887FL, 0xFF8D6E63L, 0xFF795548L, 0xFF6D4C41L, 0xFF5D4037L, 0xFF4E342EL, 0xFF3E2723L});
    public static final MaterialColor grey = new MaterialColor(0xFF9E9E9EL,
            new long[] {50, 100, 200, 300, 350, 400, 500, 600, 700, 800, 850, 900},
            new long[] {0xFFFAFAFAL, 0xFFF5F5F5L, 0xFFEEEEEEL, 0xFFE0E0E0L, 0xFFD6D6D6L, 0xFFBDBDBDL, 0xFF9E9E9EL, 0xFF757575L, 0xFF616161L, 0xFF424242L, 0xFF303030L, 0xFF212121L});
    public static final MaterialColor blueGrey = new MaterialColor(0xFF607D8BL,
            new long[] {50, 100, 200, 300, 400, 500, 600, 700, 800, 900},
            new long[] {0xFFECEFF1L, 0xFFCFD8DCL, 0xFFB0BEC5L, 0xFF90A4AEL, 0xFF78909CL, 0xFF607D8BL, 0xFF546E7AL, 0xFF455A64L, 0xFF37474FL, 0xFF263238L});

    public static final Color white = new Color(0xFFFFFFFF);
    public static final Color white70 = new Color(0xB3FFFFFF);
    public static final Color white60 = new Color(0x99FFFFFF);
    public static final Color white54 = new Color(0x8AFFFFFF);
    public static final Color white38 = new Color(0x62FFFFFF);
    public static final Color white30 = new Color(0x4DFFFFFF);
    public static final Color white24 = new Color(0x3DFFFFFF);
    public static final Color white12 = new Color(0x1FFFFFFF);
    public static final Color white10 = new Color(0x1AFFFFFF);

    public static final Color black = new Color(0xFF000000);
    public static final Color black87 = new Color(0xDD000000);
    public static final Color black54 = new Color(0x8A000000);
    public static final Color black45 = new Color(0x73000000);
    public static final Color black38 = new Color(0x61000000);
    public static final Color black26 = new Color(0x42000000);
    public static final Color black12 = new Color(0x1F000000);
}
