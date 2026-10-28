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
package com.codename1.image;

import java.io.IOException;
import java.util.zip.Inflater;

/// Aileron Regular, embedded for [TrueTypeFont#createDefault(double)].
///
/// The font is the subset Pillow embeds for `ImageFont.load_default()`
/// (12676 bytes): Aileron by Sora Sagano (dotcolon.net), released into the
/// public domain under CC0 1.0 ("No Rights Reserved", as its `name` table
/// says). The license is kept in `CodenameOne/third-party/aileron` and the
/// root `NOTICE`. The bytes are zlib-compressed and base64-encoded here
/// because classes are what the backend shares with the core, not
/// resources. It is data, not code: replace it whole, never edit it.
@com.codename1.impl.SharedWithBackend
final class DefaultFontData {
    private static final int SIZE = 12676;

    private static final String DATA = ""
            + "eNq1ewlgVEW2aFV1ujvpdDrp9JI96aSz70lvSSeEkHQ6JCELWTqQjYTsIRshINtTQHggIIqijsoT"
            + "RR+ODPIBwXFBXnREEZfnY4AZGVRcB+f7GQYZ5Edz7z9V93YSwHHk/f871L11q05Vnf2cqntBGCHk"
            + "i9YgCRp2OmvLt0oeykcoowdaQ4sdRU70a/RnhOIvw3N0cVVlzb8GbapEKOE6QvLB4pq6gsV/WzEO"
            + "z1ro/7yyJi2z59jadxHCy+G5tX2gbfjruVUZCEl+DW17u9uWDPM8rITiL0K/Z3f/iq7YopW+CMlg"
            + "zbDrPZ1tHZoFwe9A349QrD3QILtIDsJYM12/Z2B0+Rdq/Wp4/iOUD/uH2tte+vPB/QhJa6H/LwNt"
            + "y4fRq8gJffPg2TDYNtCZNB7eCUOzAIfdw0NLRn1X+pYjFHceYBSI0o6DQ0tUuyMX+Ob+HXlLKF7o"
            + "w+BdH9H7Z4qwp/g+PlVyUGKg+CKChB+Mk5zkg+H6EvT3SQ6ymab/RlnLKCpEUvZM2Cjali9CSPA4"
            + "vh96PclmQrEtE+64BWXiPAognZpNPn3qurL6CpjF8AOREMDhhOQk+hrQe5At408OUcph9qsiqkAt"
            + "Am6hKygb1yAXcSAnFAv5EiWQbTDPAZSEc1AHlHCSjuqgrwqd5z/D/jDLeRSD0/gfoC0ByjoouVBS"
            + "oERCyYaSBCUTSjKUDApPx0K9m84DxYG/Q0ZJLbKSu1AYWYI6yKsojRyHexkUG5S34PkcrO8JdQOy"
            + "kTa4z0QdkoVw/wDKZ9DvgvvHcF8E9xJkJh0oiuxFLWQQGSQvoWCyGAWRIqQhlSgVm9BCijOOAxjA"
            + "AfiQiZMBfw2KJu2olKiBzm4oMSgcRyEdq89FpegSykPf8N8RBdS/R05JPcBCOymFfjoO4HApKsXv"
            + "okCcjbSkChUQgrBEjTD+nB8nIF38AdLiNODpOGqFO+VRucQJ66ag5TgRhTJ8kkErTiIDtGXDOlYo"
            + "evwUskLRk37g990Aezf0/U++j4TAOgqkwxNIRzVGLKGitj0DT5g9e6BNcA9HftCiQnei59Bv0EF0"
            + "GP0WvWoIMoQZYg12gyMy6gcC1ke1g/YfEPsDDaHQn836Mc/zX/Av8y/xh/lD/AH++QtzL1ReKL5g"
            + "uZD8aaV08S1afssPy9AkECai3t8AACR4SGVyTy+Ft9JH5eun9tdodfqAwKDgkNCw8AhDZJQxOiY2"
            + "Lj4hMSk5JTUtPSPTZLZYbVnZ9pzcGXkz82cVFIJXKp5dUlo2p7yismpudU1tnat+3vyGxqbmlgWt"
            + "beif/jrE+8LFXeze+ZNQ7T0D7uqi/t4+eh9Gg0MjS1D3P18CxYp/eagarUMnsQJX4CY8gjfhPfgo"
            + "PonP4ov4GlESAyklm8geclaikkRJOiQ7Ja9ITksuSn708PNweuz2OOoxLo2VVkv7pGukB6QnpWel"
            + "4zKtLFo2R9Yj2yw7LPtcHiyvlXfJ75fvlo/Jz8uveCo8Qz0TPe2eFZ5Nnn2euz1PeY57JXrN81rt"
            + "9YbXXxREEa/IV7QoHlYcVZz3Vnrneru8t3of8j7nfUmpVMYqZytblU8q9yn/4qPwmePT5bPe50mf"
            + "/T5jPud8LvpcUylVUapSVZOqX7VStVP1pupHt5vCV8lR0D0Z8kJKiCYoQx2plmgiNQFqHOmN1Rp8"
            + "dXwvt+/EXm4brt17Yi85OuHAWxDPneYxN3EG8TiZI4gHbcnmr6CXwYdJkDdCNqPERDL1WllUrPnJ"
            + "CmVXZVZ0dFb0YRgyFk2rAO/CX+Fc8inAg4/UGONMAVD2OBUN66Dgr8bHxwXvS2MCB/OGoAioyU06"
            + "o9xoY8ViYsUkZ0WXBJ34V2Gdi9PsaY1QGrqm1Xc1hjfiXVuz/gS/LPeNWaWF30NUEiXMHouSEYrR"
            + "yuQ6oyUVx5nCcYA6FZutNotJB3VjbBzcsFYfYFFhjS4PW8xEtXievXXEo0meZ2lZ0FNnr2tXNHk3"
            + "DjRxO1PMqabHTfYjc7u9KsplffVWhydGwY3PdMCjfFHVkOqSsRN7Zlj9vvLM4r67JrObW4TQkcBf"
            + "Ia+CTOTARx0KQ3EI+Yuc1BixySNKptPqM60Ws7vVOtmyYlNV1aaqh+rxWD3XZDca7cavWctBVidH"
            + "XQ+4XA9MPEYOZZrS08zmtHTuC9YULD4yfufD+oNkDEWhVOC3zqiWGaNiLcCFVCDYasoM0MUa2YoB"
            + "+hwMnJFq9aZMm8UYJVdhMtja1Hb3NtfsnOJN/QXWrEJHuSPPPznXxT1QuyA7OTdBW12O1zq593S1"
            + "3fkdvqq5OfMGFUXehfaCLAkeHn67eIbXj0RexLxNEv4KbWD6wbRjlaAVgifq4A/gAJCZF/SZZH4B"
            + "Riqmj+1Ll9qz8/Mlyre49Lfqvvj2CwYbzh9A/+WGDccgXqPVL5wCZtuXSpQUru4t/CGDrUPV+E/4"
            + "T+CJEbYFyAPkcfI4W5wtIM6EUx7X7Fqc2d6euXiX5nFFHo6fkZOwOebRR2M2J+TMeAHGVqE6sKUY"
            + "qv3YCBoEqgncMeGrR+xH4B+OOHky64MPGP78Z/wmtBW8mQdYCmiyaWvD3IrmhlM0tQEfz6Mr2CbQ"
            + "bTFdOXUK8VnQHgP29e9ohFqLVZR8CbOqEcGi6Lw/8J9jM/mQjg0wakzZeT/khZNLgh1RvQoBuTJ7"
            + "8zf4GaxQEvAVTtWKM7kPyNhEpoM4Bf6ug8vDYHOecFcb1XKLzWrCD7uOPWCMJ4cmKkj94sZGPcDl"
            + "wsQqmDMI6IjURVo01ChAPXyx0ZaHTZl6HbD7ZVzNZYwXWGfl3OmHXVixuqdzoLTF6cxAvKMuLbvs"
            + "VwrFosX91YWLm9naKYCnHuZMhIdwDBMYo9KwKSAcpgMVTwKVo3eqg9NXwVtnlTzQ85EOFpDXF8ya"
            + "+4xanV9baMsulwtLLhguxEP+jQXNiz9UKJzO4ry7nLNnmmcVFSgUC/pbK+3zGI8i4RINdCuoJ4yh"
            + "Uoy0YJPaBEbgi0l0vYvbi12u+ufDQg/s378/CX/EFeCyzNjvKN7g/0gQjA2DB4aSgLfeja7cqAEW"
            + "yW1WbBq6Y3vnGS12keA7iqoritrSqtPxJu5YRFI6fntN98Da4wrF2vryisaFeLvzcmqYIL8kmF8D"
            + "fAlAxqkV1NQi9RHYGAcrSGxWjZs3YJ84tW/V5r6rj4zOH/XETSSwe6cizmKfVVNSllddhU+t7R24"
            + "BzuP/66zTqFYxn2bl8x1NJdVLGysKWljcsiERYOBHpoX6LARR1pIMPfrJlzGrSWH4ieWkX4n85/J"
            + "/N9IIOClB81NnpLZlMtgqMZOOinJdBR3ZJfd39W/udKenrPi5IqZbasLC1Y7NpSUQYpQVoEHNM25"
            + "vd1dfbnN/poqW11nZ52taqLIbHM4bOYJrjTPXl5un1EGvMlgvJ/kjZohIHJfIxexuYE3MTfxpYTy"
            + "CviSD/whY9wrIl+wNi+ZMorroHyh/GGyYLYIOa+c6onbD/vfYJXCP2Jgd26Z20YJs31hrHKaZ6de"
            + "QBjY01Bd0eIe13XuayoLmsFeh3yYysIH62IsMfg6VUVuCG/HB/bscb7jeIfiVYVexVfxq4KNg8Qi"
            + "1VDwVa4JP0MLvjuLR9SXOGC+M+J8OqklBVvwGZiLqneS4x0HTCjonJG/gk8DXwNBtoLOgZOXG/Ow"
            + "TTR0GX1wE4E9Fg72phiISxqZaI3Ps2QXezRIVpkFRox1V7X3pthiUgrT88qSzTNLilpHJg652QJr"
            + "WflfSfTkV5BXF8BaEpCS23JUREdjjNUWIEgyj5hE9Y9zy9aUGYFtYFuCeAGWrJzpEZtZ8vBw/uqO"
            + "vCdX5f+rJfDuB+97e/ayByu2vbB67nxvXao1u3ZVc26ol9rm5RsTb3c64mbi/+Kuq32ju1rWb8+Y"
            + "f2/NlkcS4jLmH9nTurvX/tJDd66bnRBXXtv1aET6jLNxhbmDJZ1OwBvMk9wBdiIHrwE+A8zcROO0"
            + "BJ/g5jZdH73UzJ34z46rV6nfxHa2G5bQPJbMgTF+kNXEuW1GLkvCaoivQFqMDkIqEA9WzVxqXGwS"
            + "hiZ8rLi4a06U9ZN3syP3chs3hIa09fnHlJjyhiMC73tIj4f8KovmLVRKySGPgO1DZ77w8G1foCQT"
            + "56V5tkFfjwnBt6eBrdwHMg1hEhVdh4xxeCY2gXwFjuKld93nDS4qanfP6hWdu6MIuNGH1j645zAZ"
            + "W9OnUOx5+q61T+9RKDo3nNi5610qP0rTLqBJSaPBDRQJUYHSAA9427/seGy10fL+KYuuobt9fkj2"
            + "HSNZZL/frx/a8YqvB3DpUd/7li/eqJJOnBNjPcybBvNCXI3RgULrhD+Sxlnw69xDuJx7wekkh5yX"
            + "nZcn4WMAnsZ7CgnxK1L3Ci7kXnPh10n/ZSd3EqBv5AXNLE1qo2VKnW5kjNVixPd9Pycibykw4IHp"
            + "jKkdeopisDzfSfkwyZmlTZO4bBVwp3GU4mKCK9nq4ta7XHg1U4pU7hQ5xL2JcyfpRa+wPBpB3DG9"
            + "4nLRgwGMbGCLF6GqoXNpBTmJLg5URQ190uG+1gECqMn/ZVSBYU5XRlRjz1BbQsLW9doZ+Nkpfo4K"
            + "OHlhowSSXcAJm3AOjlvxvwoAq7pO3MbtwnHFF2CK7XhomgykggxMasp9F2X7xA5xTokW+rUCnWab"
            + "TC4xSmLj9DpKcQiWaF3EmLGyf7UthLi2vfcuo7v9xbKRbdxO3PVkU9lh7t+g7RHSO4XjGpjPB6FI"
            + "IxZmwbEWtYmsWc+9KnGtx0WSWtKycgv3LAzbvWIrns98SBrLncdA9hqa41AnYYilmTJ1JFSWf8Lm"
            + "ffu49/ad++PKlX9cScaO737qrbee2s3l7Ny46fHHN20U9bgY1laB15uux0aqyVazW4131C7orIjI"
            + "WeM6mxPc1BRo2bTJQvZ7d7vqehUe3NfAuSO60zopd13E6z3yGuDlDzt+FKMPkNN8QNQ1imWMEBoB"
            + "RTJjRmFGlS0iMSL+sX37ANuJ0ysZrrO9snKsXnJA9vjx3UeObNzJMBbxnQf4stgHc8M2RbA1inCA"
            + "gL8+BpTE7UhIojJqQZDSp8Evcp3r85yIqianpYJ7fH26JnNGZ1Dy1iObUrJHR+WY+wP4EUVjTX61"
            + "OjqfSPOz+vw8QDZm4HE80BLD/AdNvcXtgDvw042T4LrjWPzH9u5liiZZ2Sx7SeHcpUMbu0Y2ezf5"
            + "NjhmVJc3dSxaT8aGmwuKrWab3T+k0zV/cLDDUZpnySvUBcyrql8k6EMU0NjBfCzk1kAY5GWk45rr"
            + "Gikg/RM7aCaCUQvg1cH4INoHYEJVRq0FzCB4gI1Av+fay2s9iYvI5j02T0ZATOWJ2qYlS5q0ifgF"
            + "ri7J31FV5fBPwr8R1oWoR1YKOVAk7GhDwEzWgUd//3QTqN19ZDGFoWeJB5nfg6hl9MYCHOj8wbHF"
            + "O3ePvLhw+5OjOx9laXPNxPNwf5w0TjxDuuhY8JRkSLBF2GwbNWyoREMUJ99sv/blwt8ea788jqO5"
            + "87gJYvPXOJh7nnuKjgPtJnOn8nMBs2bX1faxV9qwH3cZ++Hd3EFcSWFhF0eyBX8ItmuhWYEOk+yJ"
            + "v+OnuFX4G67N6Swgic6iiY8EmhfC3koK+yW5sPuD/bYaS3t7n5Yosya0WZKmm/YZUqAXmyc3GiAr"
            + "2G8dF8YHwP4FZGU6/jSMr8oi32axfdIfYJ/UQ3kaQ2k2afCBe+91HnAc+AOezz2Lt3NDQv7J78Vv"
            + "8J+zNWgW8wY3jO93sX0OO9N8UfCRdG/4YlZdFXEMCnlENPjJDyG/CqMaGjmZIeQw7adpON0yBOjD"
            + "sDYgLtM6A8fqtHilV1h6XVnr/Y9FEXlrr1eDbGD+fpVE8eWM4ktzsiOrihsbVcnaucO1LVsj4t6e"
            + "UWquKLb6r20S1isFvSuCfaoahU/l5eCZmV+dlpHDgnpcu3jNQ4PGEo3R5YrPnF3SVFNe0tJGDI8s"
            + "37JD42cuIZ9yn2QnctrBzpYVnYvCFrSw/esV/D3QEyja21RQspkmg3VAyzJlQ2h/QX1NvqXYp0Gz"
            + "srFv3f3E0DPvzr6ugYHc7IbmLVvWPsLwDWe5shvfm4OeWkJ54k6TSRCg2eDG+VnAdRbgTD6diKzL"
            + "ThSRxvqJOoYrRVqYH08AvjqWYWjlkcwbTOEKy8RB3hgZC1DcGll1vqXMq8F3bX3/hm2Di5Z/lV8n"
            + "x4k+OFCvaZ83M6uxZdvGux7bXtlQ8zyVu44e+pMrsNdAGmAo1U3qDEwWM9sIBuhin5UMDLiysyXy"
            + "ktyI8B6yXBdpLuDM+L2C5JiQ7JnSYo3Ig1TggQH8StpUNJ3cZdqAAzcwxXITU7DH+tHzalyPpRWF"
            + "FXUS/FM88ueGS8OWrPwfCq/M3LryoEivSYaVcFFT/MKoFBBSkC+Z32KRzgxuExAKw4ICAQaEeiyP"
            + "HHtRrcsVmT23biHeEJLPncXRGSGZOZiM3PUN+ZL7/YzkWkobfQOxD/wzPT+adnKkNrWkGgyphoWQ"
            + "T4wZaHWilfjDfuQ7PhU9y+B1U/ABwBOaX1v01G/OF0ZKvBeH+pSC1xTGb5sYiwnpIE65SRUO9ohE"
            + "WjxBt7yZ5TEL0JjkGvzg65ku1/eD349fsD0HOh6Pvce5iwDv5H9EnwA87KI0zE/bdCyCgN/eiUmo"
            + "MSg6MZVg0LeXI9XB/h55HtFp/mZS6l5L4gcyjJ7iGwjohopJx9gXwK4Sb6nLQ9kyKlxcUbGlORV+"
            + "abGljlq8KyqbO4pDC6Pnn+XG3XfiX54bYU1xRBSjSdoUsJ7+hvVkTEy6KTF5DTvqbpFS/yiIyL+R"
            + "Schtg8lsD6hmeQpNU6ZlKf+O0caNPNr40hMNDU80EMOuy7t2XeZeWLagdenS1gXM5+SCzzHc4nNE"
            + "mm/H53DfkbN1N/ocil8usw+3j1D/rDn8pI/wn0jEQT+j8ziPhNP8DlOrlbmlhYP1quJ6lyvdSMLl"
            + "s6SuesiN/WuKA2FMIPiV5wEnyHI00/zgtLRDPz3r+LK+3atB05yUkpXbXD8wzzWobND0ptizHdXz"
            + "O4ihoaw9KTom3i+osmB2Ra2rJz4mNtVfPzt/dg2TtZa/jrXkHmqTN+uleKynO4glsaaIxEyTBy4u"
            + "drXj97j/DFHF6mUzpckZKjO2FKxaVcDmKgBZE7C0gGnedioxMVFWEkIVprJNARoqy7c45gL3LjXk"
            + "JZbbo7JxHncqJjrVtmTNN8j9JhL/CPPJWL5BQz/+8Z3edxbs3kVHgd9Jwb8X4STeAHdLXuL9XNey"
            + "ZV3Pta8aXbRsFR2Dj3EFMC4fj3GF+DUa38fhchbG0n0LBFg5zUwCbCYcfLRjy+b2Y7/t2rChHau4"
            + "K2fOYCWWvf8+W49PxT/AGDVdj/pTcB5A3UzMUPzhXOT8/AgfH/8Eq1b++7Yn/g0Wfh2ithMnmjQk"
            + "hUvFpyjfYexBoroxV8EHuXZs5j7H67gPHI4CvMlRxK0SeJEPuQo9u6fnTkwdAtiJI1UJOVWOHIjC"
            + "VG/ZXtrCNtQsFcAqZaLR2yc1ISHVx9uYqPR2OJTe1dXeSofDm5wODNJXNBqSLFKpLcnQWKEPCsxK"
            + "ywmsmxNV5JnlWRQ1py4wJ01Yv5XPQo9JDMJ5q9r0mN1+QkL9WiTkQH8V8NLo3Gi5jy7jJo/GKFqx"
            + "rFkvIPxXAYWfQA6vnFw66x/jR3EqR5/ia7iD7RNNWqZs4A2NZuoI9fQMIWswJDllVliG+447NqgD"
            + "jXPohZ5NRPOfSKLIReabEug7R70feCe/aANlqAEwjnFb381JCMaOjz/mjn5M5Nzll1/Gfi9zx6u6"
            + "fZv859nzZtkSs2VN/l2upiXLyMUL3IsXLuDSPf+x9d5jx+7d2jivpLn+4fmW+DLXnd2L7gT/sxxV"
            + "46v4CdhvUg2UBxjjbCZ2xaX33FNyzz3fsSt+Yo/4oz41FJ/CKhbHpp3JWSmWmVZza5/D0ed40BwZ"
            + "aY4kYwV9BQV9CmOW0SiePVWx92DesJov5Zpu8vxeQzPPmUeEI3x2hNaYdfhw1oGJv2dRXo8SB8bu"
            + "PNRm2pc1w52H0r3Dt2gvXsZO20VsZtvj4+3xeFk8vdN1s4HOI246Y+RGW5xJyq7YmxJYgk3sdn2S"
            + "TJjXir9Cn0HM9IA1gStGa0t5RTP+6+Vx6NND3+tiHz1RfJ32kTPQR+g4vIX1eYkj6XGPMHyvew4F"
            + "gAIszIMXTcLSmQImp9vrnpPBSuhZKLGw9xK+1Me5303cfBcON7+ddh2JnnZQOvkOg8m+lPFElLwo"
            + "c0HaAm8p37wFmJjp/LrOIDDfB5e1glySMDbNpG/E2OcVQJsWn8DnyQ7AV0utUxC1VGe0yY1xdMMS"
            + "YDtXmlmKT2SSJHMizixLSiI7Ep5+OgG/u23zPfe+C3tuIQ89CnlooBAdp7JQ4f2YTA6RFx91vUEz"
            + "UfXbEeELXXIJUU1cgShBc1FVTMjD0mLP6HRdpJ7Ro+N/xF/CfLBjiZkebNgxwY15Ll1BrcVPT2ZH"
            + "4XEtSpr0pqVJ1C8027uC1R4zhTxJJ3PBAgWTS2ogjRLeceuxavJlvwbFi3UMPJkj1gnUO8S6BPa4"
            + "u8W6B7S/L9alKBe2mEJdhnzxPrHuibzxabHuBfXrYt0bBRCzWFdCfTX9CsLDi71zR2IdIxXKFesE"
            + "6rViXYLMqF+se0D7a2JdCvpyRqzLUDjeKtY9gcajYt0L6hfEujdKJR5iXQn1OagCDYGtVqNe1I16"
            + "0Chawp464d4JWr0Mrh10D10xZKju7e4ZXWKo7lzSObKsswMaZ8GofgY3hMDsZ/X2d44MQYWO70ZL"
            + "oa8N+lB1Z/fS/jaoZMBMGSgdZaIcVIfKUD2snnPTLCk3j85IzUjPzKHfDuWIC6RMTviPEHCxtiXQ"
            + "S9sNNyxchWqgJR3+prf2AOQoamfwy8QR6VBscM1BA4DKIpiTwnRBaz/MvBDGpSILK1nIBE/ZsHDn"
            + "yJLeoUGDgHRVjSE9Xaj2DI22Dw0ug470VFt6zkDbos6h0a7U/t6FmamWVEuWKTMb/TNO3EJ+DcCN"
            + "QJ8Bam0A2QajhqB5aKTNUNPW3TY49MtgqNxH0TCyw34wDd3B/lJB7m6W9DN8UqF0QgvqGR0dtqel"
            + "3XHHHakdlKz+ocHUwc7R/3fzTH59w99N38P9xG+U2SsBTfYAG5CBVXqCnitAv5WQVavAF/tB/PYH"
            + "69aCt9KDZw5EQWDHISgUhUFWHwH8iIT9rxH2TzFgf3HgBRJQIkqCnCUFMExj6pEJYjWDeK2gBlkg"
            + "XjuoQi6aAXvMmSgfpFWACpEDFSEnKkazUQnk9WXgQcpBqytBzeaC/GrAgutAHevRPDQfNaBG1ISa"
            + "UQtagFpBElvQc+hu9CC6F/0O7UUvot+iw+gI+g06hv4DHUVb0Rh6XfBMPP3CBDO/Qb+aovyhtNPv"
            + "mqiaExrxxC/1EHBhDigpcTjn1CJVf9voINDtwbjJs349khTOqjWg8PLKObCfq64sZ7s6sRextRh3"
            + "+3u722CGofZ+GivErwAxW0kG/JWxmj9wyIO1z2HfeBHooe/GQ8URlezaLdZ/CiJtGkQaO1HE7ItF"
            + "GVBLaasEGgnzzBh47KbRCRycTmPoJI0C9m4q9bdQKVLXPrxkGKkWdY4MitS59xd0ZZVIqz8SvgUL"
            + "Bx3AsKoU/hRMFtGgBwKEQL0SShb/ZzZDNCKyzbRdGi09CfQEwbrX0W3++EP8S/y3/CX+PXTbP/7N"
            + "Xwh3mmffjfJ/g4sf/9ovGHHppufvfjFS/pNj/vdtUnNBKLfNhSv81Z/pvShG6P+Pv9vgjgD/9U3P"
            + "n0O5zGrX/uEYUbP4D37hGh/wf2H3qyxXuV2KPoKyEsp89nQ/v/8fwF39v+AaUMy/yF+GXSTU+Tf5"
            + "T1nr7/jr/FV+/FbKb0ePbnPE7/g3+N/D/RSU738W8gSUb/57lAsz8xO3MeLaba5w/oanF//bkvnx"
            + "n9rTL/9RP1kFsYBm1bD/gxgtE3s86RdoLGP2n8zSgyB2+LBRN1ts4C1fmuvET9ADWZ+WxQsaOwnk"
            + "BWoaBX7xTwrrqSFiy34hRVN4yEVKpn8JL2eUeAF+NEOh9PndtFogi+xCzAmk+0lGgR7+iFgQ4CMB"
            + "erQs0/H4WW9LT8kwxK5oluHQ/EbIbWhe485oaD5TBNkLzVlovlIHucpUprJkki507pM/HL/5/xj8"
            + "H8OKwa8=";

    private DefaultFontData() {
    }

    /// Returns a fresh copy of the font file.
    static byte[] aileron() throws IOException {
        byte[] z = decode(DATA);
        Inflater inf = new Inflater();
        try {
            inf.setInput(z);
            byte[] out = new byte[SIZE];
            int n = 0;
            while (n < SIZE && !inf.finished()) {
                int r = inf.inflate(out, n, SIZE - n);
                if (r == 0 && (inf.needsInput() || inf.needsDictionary())) {
                    break;
                }
                n += r;
            }
            if (n != SIZE) {
                throw new IOException("embedded font is truncated");
            }
            return out;
        } catch (java.util.zip.DataFormatException e) {
            IOException x = new IOException("embedded font is corrupt");
            x.initCause(e);
            throw x;
        } finally {
            inf.end();
        }
    }

    private static byte[] decode(String s) {
        int n = s.length();
        byte[] out = new byte[n / 4 * 3];
        int o = 0;
        int acc = 0;
        int bits = 0;
        for (int i = 0; i < n; i++) {
            int c = s.charAt(i);
            int v;
            if (c >= 'A' && c <= 'Z') {
                v = c - 'A';
            } else if (c >= 'a' && c <= 'z') {
                v = c - 'a' + 26;
            } else if (c >= '0' && c <= '9') {
                v = c - '0' + 52;
            } else if (c == '+') {
                v = 62;
            } else if (c == '/') {
                v = 63;
            } else {
                continue;
            }
            acc = (acc << 6) | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[o++] = (byte) (acc >> bits);
            }
        }
        if (o == out.length) {
            return out;
        }
        byte[] r = new byte[o];
        System.arraycopy(out, 0, r, 0, o);
        return r;
    }
}
