/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.brickyard.rbx;

import com.codenameone.examples.brickyard.luau.Interp;
import com.codenameone.examples.brickyard.luau.LuaError;
import com.codenameone.examples.brickyard.luau.LuaTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The `Enum` global: every enumeration the engine API exposes. Values that
/// place files store numerically (Material, PartType, NormalId, KeyCode...)
/// carry their file values; the rest are numbered in order.
public final class Enums extends RbxType {
    private static final String[] DEFS = {
        "Material:Plastic=256,SmoothPlastic=272,Neon=288,Wood=512,WoodPlanks=528,Marble=784,Basalt=788,"
            + "Slate=800,CrackedLava=804,Concrete=816,Limestone=820,Granite=832,Pavement=836,Brick=848,"
            + "Pebble=864,Cobblestone=880,Rock=896,Sandstone=912,CorrodedMetal=1040,DiamondPlate=1056,"
            + "Foil=1072,Metal=1088,Grass=1280,LeafyGrass=1284,Sand=1296,Fabric=1312,Snow=1328,Mud=1344,"
            + "Ground=1360,Asphalt=1376,Salt=1392,Ice=1536,Glacier=1552,Glass=1568,ForceField=1584,Air=1792,"
            + "Water=2048,Cardboard=2304,Carpet=2305,CeramicTiles=2306,ClayRoofTiles=2307,RoofShingles=2308,"
            + "Leather=2309,Plaster=2310,Rubber=2311",
        "PartType:Ball=0,Block=1,Cylinder=2,Wedge=3,CornerWedge=4",
        "NormalId:Right=0,Top=1,Back=2,Left=3,Bottom=4,Front=5",
        "SurfaceType:Smooth=0,Glue=1,Weld=2,Studs=3,Inlet=4,Universal=5,Hinge=6,Motor=7,SteppingMotor=8,SmoothNoOutlines=10",
        "KeyCode:Unknown=0,Backspace=8,Tab=9,Return=13,Escape=27,Space=32,Zero=48,One=49,Two=50,Three=51,"
            + "Four=52,Five=53,Six=54,Seven=55,Eight=56,Nine=57,A=97,B=98,C=99,D=100,E=101,F=102,G=103,"
            + "H=104,I=105,J=106,K=107,L=108,M=109,N=110,O=111,P=112,Q=113,R=114,S=115,T=116,U=117,V=118,"
            + "W=119,X=120,Y=121,Z=122,Delete=127,Up=273,Down=274,Right=275,Left=276,F1=282,F2=283,F3=284,"
            + "RightShift=303,LeftShift=304,RightControl=305,LeftControl=306,RightAlt=307,LeftAlt=308,"
            + "ButtonX=1000,ButtonY=1001,ButtonA=1002,ButtonB=1003,ButtonR1=1004,ButtonL1=1005,ButtonR2=1006,"
            + "ButtonL2=1007,ButtonR3=1008,ButtonL3=1009,ButtonStart=1010,ButtonSelect=1011,DPadLeft=1012,"
            + "DPadRight=1013,DPadUp=1014,DPadDown=1015,Thumbstick1=1016,Thumbstick2=1017",
        "UserInputType:MouseButton1,MouseButton2,MouseButton3,MouseWheel,MouseMovement,Touch,Keyboard,Focus,"
            + "Accelerometer,Gyro,Gamepad1,Gamepad2,Gamepad3,Gamepad4,Gamepad5,Gamepad6,Gamepad7,Gamepad8,"
            + "TextInput,InputMethod,None",
        "UserInputState:Begin,Change,End,Cancel,None",
        "EasingStyle:Linear,Sine,Back,Quad,Quart,Quint,Bounce,Elastic,Exponential,Circular,Cubic",
        "EasingDirection:In,Out,InOut",
        "TweenStatus:Completed,Canceled",
        "CurrencyType:Default,Robux,Tix",
        "TextChatMessageStatus:Success,Sending,TextFilterFailed,Floodchecked,InvalidPrivacySettings,"
            + "InvalidTextChannelPermissions,MessageTooLong,Unknown",
        "ListenerType:Camera,CFrame,ObjectPosition,ObjectCFrame",
        "Platform:Windows,OSX,IOS,Android,XBoxOne,PS4,Linux,None",
        "PlaybackState:Begin,Delayed,Playing,Paused,Completed,Cancelled",
        "HumanoidStateType:FallingDown,Ragdoll,GettingUp,Jumping,Swimming,Freefall,Flying,Landed,Running,"
            + "RunningNoPhysics,StrafingNoPhysics,Climbing,Seated,PlatformStanding,Dead,Physics,None=18",
        "HumanoidRigType:R6,R15",
        "Font:Legacy,Arial,ArialBold,SourceSans,SourceSansBold,SourceSansLight,SourceSansItalic,Bodoni,"
            + "Garamond,Cartoon,Code,Highway,SciFi,Arcade,Fantasy,Antique,SourceSansSemibold,Gotham,"
            + "GothamMedium,GothamBold,GothamBlack,AmaticSC,Bangers,Creepster,DenkOne,Fondamento,FredokaOne,"
            + "GrenzeGotisch,IndieFlower,JosefinSans,Jura,Kalam,LuckiestGuy,Merriweather,Michroma,Nunito,"
            + "Oswald,PatrickHand,PermanentMarker,Roboto,RobotoCondensed,RobotoMono,Sarpanch,SpecialElite,"
            + "TitilliumWeb,Ubuntu,BuilderSans,BuilderSansMedium,BuilderSansBold,BuilderSansExtraBold",
        "TextXAlignment:Left,Right,Center",
        "TextYAlignment:Top,Center,Bottom",
        "SortOrder:Name,Custom,LayoutOrder",
        "FillDirection:Horizontal,Vertical",
        "HorizontalAlignment:Center,Left,Right",
        "VerticalAlignment:Center,Top,Bottom",
        "ScaleType:Stretch,Slice,Tile,Fit,Crop",
        "SizeConstraint:RelativeXY,RelativeXX,RelativeYY",
        "ZIndexBehavior:Global,Sibling",
        "AutomaticSize:None,X,Y,XY",
        "RaycastFilterType:Exclude,Include",
        "ThumbnailType:HeadShot,AvatarBust,AvatarThumbnail",
        "ThumbnailSize:Size48x48,Size180x180,Size420x420,Size60x60,Size100x100,Size150x150,Size352x352",
        "InfoType:Asset,Product,GamePass,Subscription,Bundle",
        "ProductPurchaseDecision:NotProcessedYet,PurchaseGranted",
        "CameraType:Fixed,Attach,Watch,Track,Follow,Custom,Scriptable,Orbital",
        "MouseBehavior:Default,LockCenter,LockCurrentPosition",
        "ContextActionResult:Pass,Sink",
        "PathStatus:Success,ClosestNoPath,ClosestOutOfRange,FailStartNotEmpty,FailFinishNotEmpty,NoPath",
        "PathWaypointAction:Walk,Jump,Custom",
        "MessageType:MessageOutput,MessageInfo,MessageWarning,MessageError",
        "AnimationPriority:Idle,Movement,Action,Core=1000,Action2=4,Action3=5,Action4=6",
        "PoseEasingStyle:Linear,Constant,Elastic,Cubic,Bounce,CubicV2",
        "Technology:Legacy,Voxel,Compatibility,ShadowMap,Future,Unified",
        "ParticleOrientation:FacingCamera,FacingCameraWorldUp,VelocityParallel,VelocityPerpendicular",
        "MeshType:Head,Torso,Wedge,Sphere,Cylinder,FileMesh,Brick,Prism,Pyramid,ParallelRamp,RightAngleRamp,CornerWedge",
        "RollOffMode:Inverse,Linear,InverseTapered,LinearSquare",
        "ChatPrivacyMode:AllUsers,NoOne,Friends",
        "DevComputerMovementMode:UserChoice,KeyboardMouse,ClickToMove,Scriptable",
        "CoreGuiType:PlayerList,Health,Backpack,Chat,All,EmotesMenu,SelfView,Captures",
        "AssetType:Image=1,TShirt=2,Audio=3,Mesh=4,Lua=5,Hat=8,Place=9,Model=10,Shirt=11,Pants=12,Decal=13,"
            + "Head=17,Face=18,Gear=19,Badge=21,Animation=24,GamePass=34,Plugin=38,MeshPart=40",
        "ActuatorRelativeTo:Attachment0,Attachment1,World",
        "ActuatorType:None,Motor,Servo",
    };

    private static final Map<String, EnumType> TYPES = new HashMap<String, EnumType>();
    private static final List<String> ORDER = new ArrayList<String>();
    public static final Enums ROOT = new Enums();

    static {
        for (String d : DEFS) {
            int c = d.indexOf(':');
            String type = d.substring(0, c);
            EnumType et = new EnumType(type);
            int value = 0;
            int start = c + 1;
            while (start < d.length()) {
                int comma = d.indexOf(',', start);
                if (comma < 0) {
                    comma = d.length();
                }
                String part = d.substring(start, comma);
                int eq = part.indexOf('=');
                String name = part;
                if (eq >= 0) {
                    name = part.substring(0, eq);
                    value = Integer.parseInt(part.substring(eq + 1));
                }
                et.add(new EnumItem(et, name, value));
                value++;
                start = comma + 1;
            }
            TYPES.put(type, et);
            ORDER.add(type);
        }
    }

    private Enums() {
    }

    @Override
    public String typeName() {
        return "Enums";
    }

    /// The enumeration `name`, or null.
    public static EnumType type(String name) {
        return TYPES.get(name);
    }

    /// `Enum.type.name`; throws when either is unknown.
    public static EnumItem item(String type, String name) {
        EnumType t = TYPES.get(type);
        if (t == null) {
            throw LuaError.rt(type + " is not a valid member of Enums");
        }
        EnumItem i = t.byName(name);
        if (i == null) {
            throw LuaError.rt(name + " is not a valid member of Enum." + type);
        }
        return i;
    }

    private static final Method GET_ENUMS = new Method("GetEnums", 0);

    @Override
    public Object get(Interp in, String k) {
        if ("GetEnums".equals(k)) {
            return GET_ENUMS;
        }
        EnumType t = TYPES.get(k);
        if (t == null) {
            return super.get(in, k);
        }
        return t;
    }

    @Override
    protected Object[] invoke(int id, Interp in, Object[] a) {
        LuaTable t = new LuaTable();
        for (int i = 0; i < ORDER.size(); i++) {
            t.rawseti(i + 1, TYPES.get(ORDER.get(i)));
        }
        return ret(t);
    }

    @Override
    public String str() {
        return "Enums";
    }

    /// One enumeration.
    public static final class EnumType extends RbxType {
        public final String name;
        private final List<EnumItem> items = new ArrayList<EnumItem>();
        private final Map<String, EnumItem> byName = new HashMap<String, EnumItem>();

        EnumType(String name) {
            this.name = name;
        }

        void add(EnumItem i) {
            items.add(i);
            byName.put(i.name, i);
        }

        public EnumItem byName(String n) {
            return byName.get(n);
        }

        public EnumItem byValue(int v) {
            for (EnumItem i : items) {
                if (i.value == v) {
                    return i;
                }
            }
            return null;
        }

        @Override
        public String typeName() {
            return "Enum";
        }

        private static final Method ITEMS = new Method("GetEnumItems", 0);
        private static final Method FROM_NAME = new Method("FromName", 1);
        private static final Method FROM_VALUE = new Method("FromValue", 2);

        @Override
        public Object get(Interp in, String k) {
            EnumItem i = byName.get(k);
            if (i != null) {
                return i;
            }
            if ("GetEnumItems".equals(k)) {
                return ITEMS;
            }
            if ("FromName".equals(k)) {
                return FROM_NAME;
            }
            if ("FromValue".equals(k)) {
                return FROM_VALUE;
            }
            throw LuaError.rt(k + " is not a valid member of \"Enum." + name + "\"");
        }

        @Override
        protected Object[] invoke(int id, Interp in, Object[] a) {
            if (id == 1) {
                return ret(byName.get(Args.str(a, 1, "FromName")));
            }
            if (id == 2) {
                return ret(byValue(Args.integer(a, 1, "FromValue")));
            }
            LuaTable t = new LuaTable();
            for (int i = 0; i < items.size(); i++) {
                t.rawseti(i + 1, items.get(i));
            }
            return ret(t);
        }

        @Override
        public String str() {
            return name;
        }
    }

    /// One member of an enumeration.
    public static final class EnumItem extends RbxType {
        public final EnumType type;
        public final String name;
        public final int value;

        EnumItem(EnumType type, String name, int value) {
            this.type = type;
            this.name = name;
            this.value = value;
        }

        @Override
        public String typeName() {
            return "EnumItem";
        }

        private static final Method IS_A = new Method("IsA", 0);

        @Override
        public Object get(Interp in, String k) {
            switch (k) {
                case "Name":
                    return name;
                case "Value":
                    return Args.n(value);
                case "EnumType":
                    return type;
                case "IsA":
                    return IS_A;
                default:
                    return super.get(in, k);
            }
        }

        @Override
        protected Object[] invoke(int id, Interp in, Object[] a) {
            return ret(Boolean.valueOf(type.name.equals(Args.str(a, 1, "IsA"))));
        }

        @Override
        public String str() {
            return "Enum." + type.name + "." + name;
        }
    }
}
