// Codename One Flutter runtime API stubs (M1).
//
// These signature-only declarations tell the Dart transpiler how the
// hand-written Java runtime (codenameone-flutter-runtime) looks from Dart:
// which classes exist, their Java names, and — crucially — parameter shapes.
//
// Conventions the emitter applies to stub classes:
//  - positional constructor parameters  -> Java constructor arguments
//  - named constructor parameters       -> void setter methods of the same name
//  - instance getters                   -> no-arg method calls (name())
//  - static getters                     -> static field access (Name.field)
//  - named parameters of methods        -> canonical positional order as declared
//  - the VoidCallback type              -> dart.runtime.Funcs.VoidFunc0
//
// M1 scope only. This file is parsed with the transpiler's own Dart parser.

// --- entry points -----------------------------------------------------

@JavaName('com.codename1.flutter.FlutterUI.runApp')
external void runApp(Widget app);

// --- framework core ---------------------------------------------------

@JavaName('com.codename1.flutter.Key')
abstract class Key {}

@JavaName('com.codename1.flutter.ValueKey')
class ValueKey extends Key {
  external ValueKey(Object value);
}

@JavaName('com.codename1.flutter.BuildContext')
abstract class BuildContext {
  // Return type `T` is a method type parameter: the transpiler recovers the
  // Dart <T> witness (dropped from the call) as a trailing Class<T> token and
  // casts the result, so the Java runtime methods take a trailing Class<T>.
  external T dependOnInheritedWidgetOfExactType();
  external T watch();
  external T read();
  // `witness` is filled in with the class of <T>, from the selector's parameter type.
  external R select<T, R>(R Function(T value) selector, [Type? witness]);
  // The render object of this element — Flutter's `BuildContext.findRenderObject`.
  external RenderObject? findRenderObject();
  // The nearest ancestor State of the given type (recovered via the trailing
  // Class<T> witness) — Flutter's `BuildContext.findAncestorStateOfType<T>`.
  external T findAncestorStateOfType();
  external Size? get size;
}

@JavaName('com.codename1.flutter.Widget')
abstract class Widget {
  external Widget({Key? key});
}

@JavaName('com.codename1.flutter.StatelessWidget')
abstract class StatelessWidget extends Widget {
  external StatelessWidget({Key? key});
  Widget build(BuildContext context);
}

@JavaName('com.codename1.flutter.StatefulWidget')
abstract class StatefulWidget extends Widget {
  external StatefulWidget({Key? key});
  State createState();
}

@JavaName('com.codename1.flutter.State')
abstract class State<T> {
  external T get widget;
  external BuildContext get context;
  external void setState(VoidCallback fn);
  external void initState();
  external void dispose();
  Widget build(BuildContext context);
}

// --- value types ------------------------------------------------------

@JavaName('com.codename1.flutter.Color')
class Color {
  external Color(int value);
  // The 32-bit ARGB integer this color was built from — Flutter's `Color.value`.
  external int get value;
  // Cascade fixers: member access on a Color must stay statically typed (P2).
  external Color withOpacity(double opacity);
  external Color withAlpha(int a);
  external Color copyWith({int? alpha, int? red, int? green, int? blue});
  external static Color alphaBlend(Color foreground, Color background);
  external static Color fromRGBO(int r, int g, int b, double opacity);
}

@JavaName('com.codename1.flutter.Colors')
abstract class Colors {
  external static Color get transparent;
  external static Color get red;
  external static Color get redAccent;
  external static Color get pink;
  external static Color get pinkAccent;
  external static Color get purple;
  external static Color get purpleAccent;
  external static Color get deepPurple;
  external static Color get deepPurpleAccent;
  external static Color get indigo;
  external static Color get indigoAccent;
  external static Color get blue;
  external static Color get blueAccent;
  external static Color get lightBlue;
  external static Color get lightBlueAccent;
  external static Color get cyan;
  external static Color get cyanAccent;
  external static Color get teal;
  external static Color get tealAccent;
  external static Color get green;
  external static Color get greenAccent;
  external static Color get lightGreen;
  external static Color get lightGreenAccent;
  external static Color get lime;
  external static Color get limeAccent;
  external static Color get yellow;
  external static Color get yellowAccent;
  external static Color get amber;
  external static Color get amberAccent;
  external static Color get orange;
  external static Color get orangeAccent;
  external static Color get deepOrange;
  external static Color get deepOrangeAccent;
  external static Color get brown;
  external static Color get grey;
  external static Color get blueGrey;
  external static Color get white;
  external static Color get white70;
  external static Color get white60;
  external static Color get white54;
  external static Color get white38;
  external static Color get white30;
  external static Color get white24;
  external static Color get white12;
  external static Color get white10;
  external static Color get black;
  external static Color get black87;
  external static Color get black54;
  external static Color get black45;
  external static Color get black38;
  external static Color get black26;
  external static Color get black12;
}

@JavaName('com.codename1.flutter.EdgeInsets')
class EdgeInsets extends EdgeInsetsGeometry {
  external static EdgeInsets get zero;
  external static EdgeInsets all(double value);
  external static EdgeInsets only({double left, double top, double right, double bottom});
  external static EdgeInsets symmetric({double horizontal, double vertical});
  external static EdgeInsets fromLTRB(double left, double top, double right, double bottom);
  // The four resolved edge insets — Flutter's `EdgeInsets.left/top/right/bottom`.
  external double get left;
  external double get top;
  external double get right;
  external double get bottom;
  // Summed horizontal (left+right) and vertical (top+bottom) insets.
  external double get horizontal;
  external double get vertical;
  external EdgeInsets copyWith({double? left, double? top, double? right, double? bottom});
  external EdgeInsets add(EdgeInsetsGeometry other);
}

@JavaName('com.codename1.flutter.MainAxisAlignment')
enum MainAxisAlignment { start, end, center, spaceBetween, spaceAround, spaceEvenly }

@JavaName('com.codename1.flutter.CrossAxisAlignment')
enum CrossAxisAlignment { start, end, center, stretch }

@JavaName('com.codename1.flutter.MainAxisSize')
enum MainAxisSize { min, max }

@JavaName('com.codename1.flutter.TextAlign')
enum TextAlign { left, right, center, start, end }

@JavaName('com.codename1.flutter.FontWeight')
abstract class FontWeight {
  external static FontWeight get w100;
  external static FontWeight get w200;
  external static FontWeight get w300;
  external static FontWeight get w400;
  external static FontWeight get w500;
  external static FontWeight get w600;
  external static FontWeight get w700;
  external static FontWeight get w800;
  external static FontWeight get w900;
  external static FontWeight get normal;
  external static FontWeight get bold;
}

@JavaName('com.codename1.flutter.TextStyle')
class TextStyle {
  external TextStyle({double? fontSize, FontWeight? fontWeight, Color? color, String? fontFamily, double? letterSpacing, double? height});
  external Color? get color;
  external double? get fontSize;
  external FontWeight? get fontWeight;
  external String? get fontFamily;
  external double? get letterSpacing;
  external double? get height;
  external TextStyle copyWith({bool? inherit, Color? color, Color? backgroundColor, String? fontFamily,
      double? fontSize, FontWeight? fontWeight, dynamic fontStyle, double? letterSpacing,
      double? wordSpacing, double? height, dynamic background, dynamic foreground, dynamic decoration});
  external TextStyle apply({Color? color, Color? backgroundColor, String? fontFamily,
      double? fontSizeFactor, double? fontSizeDelta, dynamic decoration});
}

@JavaName('com.codename1.flutter.IconData')
class IconData {}

@JavaName('com.codename1.flutter.Icons')
abstract class Icons {
  external static IconData get abc;
  external static IconData get ac_unit;
  external static IconData get access_alarms;
  external static IconData get access_time_filled;
  external static IconData get accessibility;
  external static IconData get accessibility_new;
  external static IconData get accessible;
  external static IconData get accessible_forward;
  external static IconData get account_balance;
  external static IconData get account_balance_wallet;
  external static IconData get account_box;
  external static IconData get account_tree;
  external static IconData get ad_units;
  external static IconData get adb;
  external static IconData get add_a_photo;
  external static IconData get add_alarm;
  external static IconData get add_alert;
  external static IconData get add_box;
  external static IconData get add_business;
  external static IconData get add_call;
  external static IconData get add_card;
  external static IconData get add_chart;
  external static IconData get add_home;
  external static IconData get add_home_work;
  external static IconData get add_ic_call;
  external static IconData get add_link;
  external static IconData get add_location;
  external static IconData get add_location_alt;
  external static IconData get add_moderator;
  external static IconData get add_photo_alternate;
  external static IconData get add_reaction;
  external static IconData get add_road;
  external static IconData get add_task;
  external static IconData get add_to_drive;
  external static IconData get add_to_home_screen;
  external static IconData get add_to_photos;
  external static IconData get add_to_queue;
  external static IconData get addchart;
  external static IconData get adf_scanner;
  external static IconData get adjust;
  external static IconData get admin_panel_settings;
  external static IconData get adobe;
  external static IconData get ads_click;
  external static IconData get agriculture;
  external static IconData get air;
  external static IconData get airline_seat_flat;
  external static IconData get airline_seat_flat_angled;
  external static IconData get airline_seat_individual_suite;
  external static IconData get airline_seat_legroom_extra;
  external static IconData get airline_seat_legroom_normal;
  external static IconData get airline_seat_legroom_reduced;
  external static IconData get airline_seat_recline_extra;
  external static IconData get airline_seat_recline_normal;
  external static IconData get airline_stops;
  external static IconData get airlines;
  external static IconData get airplane_ticket;
  external static IconData get airplanemode_inactive;
  external static IconData get airplanemode_off;
  external static IconData get airplanemode_on;
  external static IconData get airplay;
  external static IconData get airport_shuttle;
  external static IconData get alarm;
  external static IconData get alarm_add;
  external static IconData get alarm_off;
  external static IconData get album;
  external static IconData get align_horizontal_center;
  external static IconData get align_horizontal_left;
  external static IconData get align_horizontal_right;
  external static IconData get align_vertical_bottom;
  external static IconData get align_vertical_center;
  external static IconData get align_vertical_top;
  external static IconData get all_inbox;
  external static IconData get all_inclusive;
  external static IconData get all_out;
  external static IconData get alt_route;
  external static IconData get alternate_email;
  external static IconData get amp_stories;
  external static IconData get analytics;
  external static IconData get anchor;
  external static IconData get android;
  external static IconData get animation;
  external static IconData get announcement;
  external static IconData get aod;
  external static IconData get apartment;
  external static IconData get api;
  external static IconData get app_blocking;
  external static IconData get app_registration;
  external static IconData get app_settings_alt;
  external static IconData get app_shortcut;
  external static IconData get apple;
  external static IconData get approval;
  external static IconData get apps;
  external static IconData get apps_outage;
  external static IconData get architecture;
  external static IconData get archive;
  external static IconData get area_chart;
  external static IconData get arrow_back_ios_new;
  external static IconData get arrow_circle_down;
  external static IconData get arrow_circle_left;
  external static IconData get arrow_circle_right;
  external static IconData get arrow_circle_up;
  external static IconData get arrow_downward;
  external static IconData get arrow_drop_down_circle;
  external static IconData get arrow_outward;
  external static IconData get arrow_right;
  external static IconData get arrow_right_alt;
  external static IconData get arrow_upward;
  external static IconData get art_track;
  external static IconData get article;
  external static IconData get aspect_ratio;
  external static IconData get assessment;
  external static IconData get assignment;
  external static IconData get assignment_add;
  external static IconData get assignment_ind;
  external static IconData get assignment_late;
  external static IconData get assignment_return;
  external static IconData get assignment_returned;
  external static IconData get assignment_turned_in;
  external static IconData get assist_walker;
  external static IconData get assistant;
  external static IconData get assistant_direction;
  external static IconData get assistant_navigation;
  external static IconData get assistant_photo;
  external static IconData get assured_workload;
  external static IconData get atm;
  external static IconData get attach_email;
  external static IconData get attach_file;
  external static IconData get attachment;
  external static IconData get attractions;
  external static IconData get attribution;
  external static IconData get audio_file;
  external static IconData get audiotrack;
  external static IconData get auto_awesome;
  external static IconData get auto_awesome_mosaic;
  external static IconData get auto_awesome_motion;
  external static IconData get auto_delete;
  external static IconData get auto_fix_high;
  external static IconData get auto_fix_normal;
  external static IconData get auto_fix_off;
  external static IconData get auto_graph;
  external static IconData get auto_mode;
  external static IconData get auto_stories;
  external static IconData get autofps_select;
  external static IconData get autorenew;
  external static IconData get av_timer;
  external static IconData get baby_changing_station;
  external static IconData get back_hand;
  external static IconData get backpack;
  external static IconData get backspace;
  external static IconData get backup;
  external static IconData get backup_table;
  external static IconData get badge;
  external static IconData get bakery_dining;
  external static IconData get balance;
  external static IconData get balcony;
  external static IconData get ballot;
  external static IconData get bar_chart;
  external static IconData get barcode_reader;
  external static IconData get batch_prediction;
  external static IconData get bathroom;
  external static IconData get bathtub;
  external static IconData get battery_0_bar;
  external static IconData get battery_1_bar;
  external static IconData get battery_2_bar;
  external static IconData get battery_3_bar;
  external static IconData get battery_4_bar;
  external static IconData get battery_5_bar;
  external static IconData get battery_6_bar;
  external static IconData get battery_alert;
  external static IconData get battery_charging_full;
  external static IconData get battery_full;
  external static IconData get battery_saver;
  external static IconData get battery_std;
  external static IconData get battery_unknown;
  external static IconData get beach_access;
  external static IconData get bed;
  external static IconData get bedroom_baby;
  external static IconData get bedroom_child;
  external static IconData get bedroom_parent;
  external static IconData get bedtime;
  external static IconData get bedtime_off;
  external static IconData get beenhere;
  external static IconData get bento;
  external static IconData get bike_scooter;
  external static IconData get biotech;
  external static IconData get blender;
  external static IconData get blind;
  external static IconData get blinds;
  external static IconData get blinds_closed;
  external static IconData get block;
  external static IconData get block_flipped;
  external static IconData get bloodtype;
  external static IconData get bluetooth;
  external static IconData get bluetooth_audio;
  external static IconData get bluetooth_connected;
  external static IconData get bluetooth_disabled;
  external static IconData get bluetooth_drive;
  external static IconData get bluetooth_searching;
  external static IconData get blur_circular;
  external static IconData get blur_linear;
  external static IconData get blur_off;
  external static IconData get blur_on;
  external static IconData get bolt;
  external static IconData get book_online;
  external static IconData get bookmark;
  external static IconData get bookmark_add;
  external static IconData get bookmark_added;
  external static IconData get bookmark_outline;
  external static IconData get bookmark_remove;
  external static IconData get bookmarks;
  external static IconData get border_all;
  external static IconData get border_bottom;
  external static IconData get border_clear;
  external static IconData get border_color;
  external static IconData get border_horizontal;
  external static IconData get border_inner;
  external static IconData get border_left;
  external static IconData get border_outer;
  external static IconData get border_right;
  external static IconData get border_style;
  external static IconData get border_top;
  external static IconData get border_vertical;
  external static IconData get boy;
  external static IconData get branding_watermark;
  external static IconData get breakfast_dining;
  external static IconData get brightness_1;
  external static IconData get brightness_2;
  external static IconData get brightness_3;
  external static IconData get brightness_4;
  external static IconData get brightness_6;
  external static IconData get brightness_7;
  external static IconData get brightness_auto;
  external static IconData get brightness_high;
  external static IconData get brightness_low;
  external static IconData get brightness_medium;
  external static IconData get broadcast_on_home;
  external static IconData get broadcast_on_personal;
  external static IconData get broken_image;
  external static IconData get browse_gallery;
  external static IconData get browser_not_supported;
  external static IconData get browser_updated;
  external static IconData get brunch_dining;
  external static IconData get brush;
  external static IconData get bubble_chart;
  external static IconData get bug_report;
  external static IconData get build;
  external static IconData get build_circle;
  external static IconData get bungalow;
  external static IconData get burst_mode;
  external static IconData get bus_alert;
  external static IconData get business;
  external static IconData get business_center;
  external static IconData get cabin;
  external static IconData get cable;
  external static IconData get cached;
  external static IconData get cake;
  external static IconData get calculate;
  external static IconData get calendar_month;
  external static IconData get calendar_view_day;
  external static IconData get calendar_view_month;
  external static IconData get calendar_view_week;
  external static IconData get call;
  external static IconData get call_end;
  external static IconData get call_made;
  external static IconData get call_merge;
  external static IconData get call_missed;
  external static IconData get call_missed_outgoing;
  external static IconData get call_received;
  external static IconData get call_split;
  external static IconData get call_to_action;
  external static IconData get camera;
  external static IconData get camera_alt;
  external static IconData get camera_front;
  external static IconData get camera_indoor;
  external static IconData get camera_outdoor;
  external static IconData get camera_rear;
  external static IconData get camera_roll;
  external static IconData get cameraswitch;
  external static IconData get campaign;
  external static IconData get cancel;
  external static IconData get cancel_presentation;
  external static IconData get cancel_schedule_send;
  external static IconData get candlestick_chart;
  external static IconData get car_crash;
  external static IconData get car_rental;
  external static IconData get car_repair;
  external static IconData get card_giftcard;
  external static IconData get card_membership;
  external static IconData get card_travel;
  external static IconData get carpenter;
  external static IconData get cases;
  external static IconData get casino;
  external static IconData get cast;
  external static IconData get cast_connected;
  external static IconData get cast_for_education;
  external static IconData get castle;
  external static IconData get catching_pokemon;
  external static IconData get category;
  external static IconData get celebration;
  external static IconData get cell_tower;
  external static IconData get cell_wifi;
  external static IconData get center_focus_strong;
  external static IconData get center_focus_weak;
  external static IconData get chair;
  external static IconData get chair_alt;
  external static IconData get chalet;
  external static IconData get change_circle;
  external static IconData get change_history;
  external static IconData get charging_station;
  external static IconData get chat;
  external static IconData get chat_bubble;
  external static IconData get chat_bubble_outline;
  external static IconData get check_box;
  external static IconData get check_box_outline_blank;
  external static IconData get checklist;
  external static IconData get checklist_rtl;
  external static IconData get checkroom;
  external static IconData get child_care;
  external static IconData get child_friendly;
  external static IconData get chrome_reader_mode;
  external static IconData get church;
  external static IconData get circle;
  external static IconData get circle_notifications;
  external static IconData get class_;
  external static IconData get clean_hands;
  external static IconData get cleaning_services;
  external static IconData get clear;
  external static IconData get clear_all;
  external static IconData get close_fullscreen;
  external static IconData get closed_caption;
  external static IconData get closed_caption_disabled;
  external static IconData get closed_caption_off;
  external static IconData get cloud;
  external static IconData get cloud_circle;
  external static IconData get cloud_done;
  external static IconData get cloud_download;
  external static IconData get cloud_off;
  external static IconData get cloud_queue;
  external static IconData get cloud_sync;
  external static IconData get cloud_upload;
  external static IconData get cloudy_snowing;
  external static IconData get co2;
  external static IconData get co_present;
  external static IconData get code_off;
  external static IconData get coffee;
  external static IconData get coffee_maker;
  external static IconData get collections;
  external static IconData get collections_bookmark;
  external static IconData get color_lens;
  external static IconData get colorize;
  external static IconData get comment_bank;
  external static IconData get comments_disabled;
  external static IconData get commit;
  external static IconData get commute;
  external static IconData get compare;
  external static IconData get compare_arrows;
  external static IconData get compass_calibration;
  external static IconData get compost;
  external static IconData get compress;
  external static IconData get computer;
  external static IconData get confirmation_num;
  external static IconData get confirmation_number;
  external static IconData get connect_without_contact;
  external static IconData get connected_tv;
  external static IconData get connecting_airports;
  external static IconData get construction;
  external static IconData get contact_emergency;
  external static IconData get contact_mail;
  external static IconData get contact_page;
  external static IconData get contact_phone;
  external static IconData get contact_support;
  external static IconData get contactless;
  external static IconData get contacts;
  external static IconData get content_copy;
  external static IconData get content_cut;
  external static IconData get content_paste;
  external static IconData get content_paste_go;
  external static IconData get content_paste_off;
  external static IconData get content_paste_search;
  external static IconData get contrast;
  external static IconData get control_camera;
  external static IconData get control_point;
  external static IconData get control_point_duplicate;
  external static IconData get conveyor_belt;
  external static IconData get cookie;
  external static IconData get copy_all;
  external static IconData get copyright;
  external static IconData get coronavirus;
  external static IconData get corporate_fare;
  external static IconData get cottage;
  external static IconData get countertops;
  external static IconData get create_new_folder;
  external static IconData get credit_card_off;
  external static IconData get credit_score;
  external static IconData get crib;
  external static IconData get crisis_alert;
  external static IconData get crop;
  external static IconData get crop_16_9;
  external static IconData get crop_3_2;
  external static IconData get crop_5_4;
  external static IconData get crop_7_5;
  external static IconData get crop_din;
  external static IconData get crop_free;
  external static IconData get crop_landscape;
  external static IconData get crop_original;
  external static IconData get crop_portrait;
  external static IconData get crop_rotate;
  external static IconData get crop_square;
  external static IconData get cruelty_free;
  external static IconData get css;
  external static IconData get currency_bitcoin;
  external static IconData get currency_exchange;
  external static IconData get currency_franc;
  external static IconData get currency_lira;
  external static IconData get currency_pound;
  external static IconData get currency_ruble;
  external static IconData get currency_rupee;
  external static IconData get currency_yen;
  external static IconData get currency_yuan;
  external static IconData get curtains;
  external static IconData get curtains_closed;
  external static IconData get cyclone;
  external static IconData get dangerous;
  external static IconData get dark_mode;
  external static IconData get dashboard;
  external static IconData get dashboard_customize;
  external static IconData get data_array;
  external static IconData get data_exploration;
  external static IconData get data_object;
  external static IconData get data_saver_off;
  external static IconData get data_saver_on;
  external static IconData get data_thresholding;
  external static IconData get data_usage;
  external static IconData get dataset;
  external static IconData get dataset_linked;
  external static IconData get deblur;
  external static IconData get deck;
  external static IconData get dehaze;
  external static IconData get delete_forever;
  external static IconData get delete_outline;
  external static IconData get delete_sweep;
  external static IconData get delivery_dining;
  external static IconData get density_large;
  external static IconData get density_medium;
  external static IconData get density_small;
  external static IconData get departure_board;
  external static IconData get description;
  external static IconData get deselect;
  external static IconData get design_services;
  external static IconData get desk;
  external static IconData get desktop_access_disabled;
  external static IconData get desktop_mac;
  external static IconData get desktop_windows;
  external static IconData get details;
  external static IconData get developer_board;
  external static IconData get developer_board_off;
  external static IconData get developer_mode;
  external static IconData get device_hub;
  external static IconData get device_thermostat;
  external static IconData get device_unknown;
  external static IconData get devices;
  external static IconData get devices_fold;
  external static IconData get devices_other;
  external static IconData get dew_point;
  external static IconData get dialer_sip;
  external static IconData get dialpad;
  external static IconData get diamond;
  external static IconData get difference;
  external static IconData get dining;
  external static IconData get dinner_dining;
  external static IconData get directions;
  external static IconData get directions_boat;
  external static IconData get directions_boat_filled;
  external static IconData get directions_bus;
  external static IconData get directions_bus_filled;
  external static IconData get directions_car;
  external static IconData get directions_car_filled;
  external static IconData get directions_ferry;
  external static IconData get directions_off;
  external static IconData get directions_railway;
  external static IconData get directions_railway_filled;
  external static IconData get directions_run;
  external static IconData get directions_subway;
  external static IconData get directions_subway_filled;
  external static IconData get directions_train;
  external static IconData get directions_transit;
  external static IconData get directions_transit_filled;
  external static IconData get directions_walk;
  external static IconData get dirty_lens;
  external static IconData get disabled_by_default;
  external static IconData get disabled_visible;
  external static IconData get disc_full;
  external static IconData get discord;
  external static IconData get discount;
  external static IconData get display_settings;
  external static IconData get diversity_1;
  external static IconData get diversity_2;
  external static IconData get diversity_3;
  external static IconData get dnd_forwardslash;
  external static IconData get dns;
  external static IconData get do_disturb;
  external static IconData get do_disturb_alt;
  external static IconData get do_disturb_off;
  external static IconData get do_disturb_on;
  external static IconData get do_not_disturb;
  external static IconData get do_not_disturb_alt;
  external static IconData get do_not_disturb_off;
  external static IconData get do_not_disturb_on;
  external static IconData get do_not_disturb_on_total_silence;
  external static IconData get do_not_step;
  external static IconData get do_not_touch;
  external static IconData get dock;
  external static IconData get document_scanner;
  external static IconData get domain;
  external static IconData get domain_add;
  external static IconData get domain_disabled;
  external static IconData get domain_verification;
  external static IconData get done;
  external static IconData get done_all;
  external static IconData get done_outline;
  external static IconData get donut_large;
  external static IconData get donut_small;
  external static IconData get door_back;
  external static IconData get door_front;
  external static IconData get door_sliding;
  external static IconData get doorbell;
  external static IconData get double_arrow;
  external static IconData get downhill_skiing;
  external static IconData get download;
  external static IconData get download_done;
  external static IconData get download_for_offline;
  external static IconData get downloading;
  external static IconData get drafts;
  external static IconData get drag_handle;
  external static IconData get drag_indicator;
  external static IconData get draw;
  external static IconData get drive_eta;
  external static IconData get drive_file_move;
  external static IconData get drive_file_move_outline;
  external static IconData get drive_file_move_rtl;
  external static IconData get drive_file_rename_outline;
  external static IconData get drive_folder_upload;
  external static IconData get dry;
  external static IconData get dry_cleaning;
  external static IconData get duo;
  external static IconData get dvr;
  external static IconData get dynamic_feed;
  external static IconData get dynamic_form;
  external static IconData get e_mobiledata;
  external static IconData get earbuds;
  external static IconData get earbuds_battery;
  external static IconData get east;
  external static IconData get eco;
  external static IconData get edgesensor_high;
  external static IconData get edgesensor_low;
  external static IconData get edit_attributes;
  external static IconData get edit_calendar;
  external static IconData get edit_document;
  external static IconData get edit_location;
  external static IconData get edit_location_alt;
  external static IconData get edit_note;
  external static IconData get edit_notifications;
  external static IconData get edit_off;
  external static IconData get edit_road;
  external static IconData get edit_square;
  external static IconData get egg;
  external static IconData get egg_alt;
  external static IconData get eject;
  external static IconData get elderly;
  external static IconData get elderly_woman;
  external static IconData get electric_bike;
  external static IconData get electric_bolt;
  external static IconData get electric_car;
  external static IconData get electric_meter;
  external static IconData get electric_moped;
  external static IconData get electric_rickshaw;
  external static IconData get electric_scooter;
  external static IconData get electrical_services;
  external static IconData get elevator;
  external static IconData get emergency;
  external static IconData get emergency_recording;
  external static IconData get emergency_share;
  external static IconData get emoji_emotions;
  external static IconData get emoji_events;
  external static IconData get emoji_flags;
  external static IconData get emoji_food_beverage;
  external static IconData get emoji_nature;
  external static IconData get emoji_objects;
  external static IconData get emoji_people;
  external static IconData get emoji_symbols;
  external static IconData get emoji_transportation;
  external static IconData get energy_savings_leaf;
  external static IconData get engineering;
  external static IconData get enhance_photo_translate;
  external static IconData get enhanced_encryption;
  external static IconData get equalizer;
  external static IconData get error;
  external static IconData get error_outline;
  external static IconData get escalator;
  external static IconData get escalator_warning;
  external static IconData get euro;
  external static IconData get euro_symbol;
  external static IconData get ev_station;
  external static IconData get event;
  external static IconData get event_available;
  external static IconData get event_busy;
  external static IconData get event_note;
  external static IconData get event_repeat;
  external static IconData get event_seat;
  external static IconData get exit_to_app;
  external static IconData get expand;
  external static IconData get expand_circle_down;
  external static IconData get expand_less;
  external static IconData get expand_more;
  external static IconData get explicit;
  external static IconData get explore;
  external static IconData get explore_off;
  external static IconData get exposure;
  external static IconData get exposure_minus_1;
  external static IconData get exposure_minus_2;
  external static IconData get exposure_neg_1;
  external static IconData get exposure_neg_2;
  external static IconData get exposure_plus_1;
  external static IconData get exposure_plus_2;
  external static IconData get exposure_zero;
  external static IconData get extension;
  external static IconData get extension_off;
  external static IconData get face;
  external static IconData get face_2;
  external static IconData get face_3;
  external static IconData get face_4;
  external static IconData get face_5;
  external static IconData get face_6;
  external static IconData get face_retouching_natural;
  external static IconData get face_retouching_off;
  external static IconData get facebook;
  external static IconData get fact_check;
  external static IconData get factory;
  external static IconData get family_restroom;
  external static IconData get fast_forward;
  external static IconData get fast_rewind;
  external static IconData get fastfood;
  external static IconData get favorite_outline;
  external static IconData get fax;
  external static IconData get featured_play_list;
  external static IconData get featured_video;
  external static IconData get feed;
  external static IconData get female;
  external static IconData get fence;
  external static IconData get festival;
  external static IconData get fiber_dvr;
  external static IconData get fiber_manual_record;
  external static IconData get fiber_new;
  external static IconData get fiber_pin;
  external static IconData get fiber_smart_record;
  external static IconData get file_copy;
  external static IconData get file_download;
  external static IconData get file_download_done;
  external static IconData get file_download_off;
  external static IconData get file_open;
  external static IconData get file_present;
  external static IconData get file_upload;
  external static IconData get file_upload_off;
  external static IconData get filter;
  external static IconData get filter_1;
  external static IconData get filter_2;
  external static IconData get filter_3;
  external static IconData get filter_4;
  external static IconData get filter_5;
  external static IconData get filter_6;
  external static IconData get filter_7;
  external static IconData get filter_8;
  external static IconData get filter_9;
  external static IconData get filter_9_plus;
  external static IconData get filter_alt;
  external static IconData get filter_alt_off;
  external static IconData get filter_b_and_w;
  external static IconData get filter_center_focus;
  external static IconData get filter_drama;
  external static IconData get filter_frames;
  external static IconData get filter_hdr;
  external static IconData get filter_list;
  external static IconData get filter_list_alt;
  external static IconData get filter_list_off;
  external static IconData get filter_none;
  external static IconData get filter_tilt_shift;
  external static IconData get filter_vintage;
  external static IconData get find_in_page;
  external static IconData get find_replace;
  external static IconData get fingerprint;
  external static IconData get fire_extinguisher;
  external static IconData get fire_hydrant;
  external static IconData get fire_hydrant_alt;
  external static IconData get fire_truck;
  external static IconData get fireplace;
  external static IconData get first_page;
  external static IconData get fit_screen;
  external static IconData get fitbit;
  external static IconData get fitness_center;
  external static IconData get flag;
  external static IconData get flag_circle;
  external static IconData get flaky;
  external static IconData get flare;
  external static IconData get flash_auto;
  external static IconData get flash_off;
  external static IconData get flash_on;
  external static IconData get flashlight_off;
  external static IconData get flashlight_on;
  external static IconData get flatware;
  external static IconData get flight;
  external static IconData get flight_class;
  external static IconData get flight_land;
  external static IconData get flight_takeoff;
  external static IconData get flip;
  external static IconData get flip_camera_android;
  external static IconData get flip_camera_ios;
  external static IconData get flip_to_back;
  external static IconData get flip_to_front;
  external static IconData get flood;
  external static IconData get flourescent;
  external static IconData get fluorescent;
  external static IconData get flutter_dash;
  external static IconData get fmd_bad;
  external static IconData get fmd_good;
  external static IconData get foggy;
  external static IconData get folder;
  external static IconData get folder_copy;
  external static IconData get folder_delete;
  external static IconData get folder_off;
  external static IconData get folder_open;
  external static IconData get folder_shared;
  external static IconData get folder_special;
  external static IconData get folder_zip;
  external static IconData get follow_the_signs;
  external static IconData get font_download;
  external static IconData get font_download_off;
  external static IconData get food_bank;
  external static IconData get forest;
  external static IconData get fork_left;
  external static IconData get fork_right;
  external static IconData get forklift;
  external static IconData get format_align_center;
  external static IconData get format_align_justify;
  external static IconData get format_align_left;
  external static IconData get format_align_right;
  external static IconData get format_clear;
  external static IconData get format_color_fill;
  external static IconData get format_color_reset;
  external static IconData get format_color_text;
  external static IconData get format_indent_decrease;
  external static IconData get format_indent_increase;
  external static IconData get format_line_spacing;
  external static IconData get format_list_bulleted;
  external static IconData get format_list_bulleted_add;
  external static IconData get format_list_numbered;
  external static IconData get format_list_numbered_rtl;
  external static IconData get format_overline;
  external static IconData get format_paint;
  external static IconData get format_quote;
  external static IconData get format_shapes;
  external static IconData get format_size;
  external static IconData get format_strikethrough;
  external static IconData get format_textdirection_l_to_r;
  external static IconData get format_textdirection_r_to_l;
  external static IconData get format_underlined;
  external static IconData get fort;
  external static IconData get forum;
  external static IconData get forward;
  external static IconData get forward_10;
  external static IconData get forward_30;
  external static IconData get forward_5;
  external static IconData get forward_to_inbox;
  external static IconData get foundation;
  external static IconData get free_breakfast;
  external static IconData get free_cancellation;
  external static IconData get front_hand;
  external static IconData get front_loader;
  external static IconData get fullscreen_exit;
  external static IconData get functions;
  external static IconData get g_mobiledata;
  external static IconData get g_translate;
  external static IconData get gamepad;
  external static IconData get games;
  external static IconData get garage;
  external static IconData get gas_meter;
  external static IconData get gavel;
  external static IconData get generating_tokens;
  external static IconData get gesture;
  external static IconData get get_app;
  external static IconData get gif;
  external static IconData get gif_box;
  external static IconData get girl;
  external static IconData get gite;
  external static IconData get golf_course;
  external static IconData get gpp_bad;
  external static IconData get gpp_good;
  external static IconData get gpp_maybe;
  external static IconData get gps_fixed;
  external static IconData get gps_not_fixed;
  external static IconData get gps_off;
  external static IconData get grade;
  external static IconData get gradient;
  external static IconData get grading;
  external static IconData get grain;
  external static IconData get graphic_eq;
  external static IconData get grass;
  external static IconData get grid_3x3;
  external static IconData get grid_4x4;
  external static IconData get grid_goldenratio;
  external static IconData get grid_off;
  external static IconData get grid_on;
  external static IconData get grid_view;
  external static IconData get group;
  external static IconData get group_add;
  external static IconData get group_off;
  external static IconData get group_remove;
  external static IconData get group_work;
  external static IconData get groups;
  external static IconData get groups_2;
  external static IconData get groups_3;
  external static IconData get h_mobiledata;
  external static IconData get h_plus_mobiledata;
  external static IconData get hail;
  external static IconData get handshake;
  external static IconData get handyman;
  external static IconData get hardware;
  external static IconData get hd;
  external static IconData get hdr_auto;
  external static IconData get hdr_auto_select;
  external static IconData get hdr_enhanced_select;
  external static IconData get hdr_off;
  external static IconData get hdr_off_select;
  external static IconData get hdr_on;
  external static IconData get hdr_on_select;
  external static IconData get hdr_plus;
  external static IconData get hdr_strong;
  external static IconData get hdr_weak;
  external static IconData get headphones;
  external static IconData get headphones_battery;
  external static IconData get headset;
  external static IconData get headset_mic;
  external static IconData get headset_off;
  external static IconData get healing;
  external static IconData get health_and_safety;
  external static IconData get hearing;
  external static IconData get hearing_disabled;
  external static IconData get heart_broken;
  external static IconData get heat_pump;
  external static IconData get height;
  external static IconData get help_center;
  external static IconData get help_outline;
  external static IconData get hevc;
  external static IconData get hexagon;
  external static IconData get hide_image;
  external static IconData get hide_source;
  external static IconData get high_quality;
  external static IconData get highlight;
  external static IconData get highlight_alt;
  external static IconData get highlight_off;
  external static IconData get highlight_remove;
  external static IconData get hiking;
  external static IconData get history;
  external static IconData get history_edu;
  external static IconData get history_toggle_off;
  external static IconData get hive;
  external static IconData get hls;
  external static IconData get hls_off;
  external static IconData get holiday_village;
  external static IconData get home_filled;
  external static IconData get home_max;
  external static IconData get home_mini;
  external static IconData get home_repair_service;
  external static IconData get home_work;
  external static IconData get horizontal_distribute;
  external static IconData get horizontal_rule;
  external static IconData get horizontal_split;
  external static IconData get hot_tub;
  external static IconData get hotel_class;
  external static IconData get hourglass_bottom;
  external static IconData get hourglass_disabled;
  external static IconData get hourglass_empty;
  external static IconData get hourglass_full;
  external static IconData get hourglass_top;
  external static IconData get house;
  external static IconData get house_siding;
  external static IconData get houseboat;
  external static IconData get how_to_reg;
  external static IconData get how_to_vote;
  external static IconData get html;
  external static IconData get http;
  external static IconData get https;
  external static IconData get hub;
  external static IconData get hvac;
  external static IconData get ice_skating;
  external static IconData get icecream;
  external static IconData get image;
  external static IconData get image_aspect_ratio;
  external static IconData get image_not_supported;
  external static IconData get image_search;
  external static IconData get imagesearch_roller;
  external static IconData get import_contacts;
  external static IconData get import_export;
  external static IconData get important_devices;
  external static IconData get inbox;
  external static IconData get incomplete_circle;
  external static IconData get indeterminate_check_box;
  external static IconData get input;
  external static IconData get insert_chart;
  external static IconData get insert_chart_outlined;
  external static IconData get insert_comment;
  external static IconData get insert_drive_file;
  external static IconData get insert_emoticon;
  external static IconData get insert_invitation;
  external static IconData get insert_link;
  external static IconData get insert_page_break;
  external static IconData get insert_photo;
  external static IconData get insights;
  external static IconData get install_desktop;
  external static IconData get install_mobile;
  external static IconData get integration_instructions;
  external static IconData get interests;
  external static IconData get interpreter_mode;
  external static IconData get inventory;
  external static IconData get inventory_2;
  external static IconData get invert_colors;
  external static IconData get invert_colors_off;
  external static IconData get invert_colors_on;
  external static IconData get ios_share;
  external static IconData get iron;
  external static IconData get iso;
  external static IconData get javascript;
  external static IconData get join_full;
  external static IconData get join_inner;
  external static IconData get join_left;
  external static IconData get join_right;
  external static IconData get kayaking;
  external static IconData get kebab_dining;
  external static IconData get key;
  external static IconData get key_off;
  external static IconData get keyboard;
  external static IconData get keyboard_alt;
  external static IconData get keyboard_arrow_left;
  external static IconData get keyboard_arrow_right;
  external static IconData get keyboard_backspace;
  external static IconData get keyboard_capslock;
  external static IconData get keyboard_command;
  external static IconData get keyboard_command_key;
  external static IconData get keyboard_control;
  external static IconData get keyboard_control_key;
  external static IconData get keyboard_double_arrow_down;
  external static IconData get keyboard_double_arrow_left;
  external static IconData get keyboard_double_arrow_right;
  external static IconData get keyboard_double_arrow_up;
  external static IconData get keyboard_hide;
  external static IconData get keyboard_option;
  external static IconData get keyboard_option_key;
  external static IconData get keyboard_return;
  external static IconData get keyboard_tab;
  external static IconData get keyboard_voice;
  external static IconData get king_bed;
  external static IconData get kitchen;
  external static IconData get kitesurfing;
  external static IconData get label;
  external static IconData get label_important;
  external static IconData get label_important_outline;
  external static IconData get label_off;
  external static IconData get label_outline;
  external static IconData get lan;
  external static IconData get landscape;
  external static IconData get landslide;
  external static IconData get language;
  external static IconData get laptop;
  external static IconData get laptop_chromebook;
  external static IconData get laptop_mac;
  external static IconData get laptop_windows;
  external static IconData get last_page;
  external static IconData get launch;
  external static IconData get layers;
  external static IconData get layers_clear;
  external static IconData get leaderboard;
  external static IconData get leak_add;
  external static IconData get leak_remove;
  external static IconData get leave_bags_at_home;
  external static IconData get legend_toggle;
  external static IconData get lens;
  external static IconData get lens_blur;
  external static IconData get library_add;
  external static IconData get library_add_check;
  external static IconData get library_music;
  external static IconData get light;
  external static IconData get light_mode;
  external static IconData get lightbulb;
  external static IconData get lightbulb_circle;
  external static IconData get lightbulb_outline;
  external static IconData get line_axis;
  external static IconData get line_style;
  external static IconData get line_weight;
  external static IconData get linear_scale;
  external static IconData get link_off;
  external static IconData get linked_camera;
  external static IconData get liquor;
  external static IconData get list;
  external static IconData get list_alt;
  external static IconData get live_help;
  external static IconData get live_tv;
  external static IconData get living;
  external static IconData get local_activity;
  external static IconData get local_airport;
  external static IconData get local_atm;
  external static IconData get local_attraction;
  external static IconData get local_bar;
  external static IconData get local_cafe;
  external static IconData get local_car_wash;
  external static IconData get local_convenience_store;
  external static IconData get local_dining;
  external static IconData get local_drink;
  external static IconData get local_fire_department;
  external static IconData get local_florist;
  external static IconData get local_gas_station;
  external static IconData get local_grocery_store;
  external static IconData get local_hospital;
  external static IconData get local_hotel;
  external static IconData get local_laundry_service;
  external static IconData get local_library;
  external static IconData get local_mall;
  external static IconData get local_movies;
  external static IconData get local_offer;
  external static IconData get local_parking;
  external static IconData get local_pharmacy;
  external static IconData get local_phone;
  external static IconData get local_pizza;
  external static IconData get local_play;
  external static IconData get local_police;
  external static IconData get local_post_office;
  external static IconData get local_print_shop;
  external static IconData get local_printshop;
  external static IconData get local_restaurant;
  external static IconData get local_see;
  external static IconData get local_shipping;
  external static IconData get local_taxi;
  external static IconData get location_city;
  external static IconData get location_disabled;
  external static IconData get location_history;
  external static IconData get location_off;
  external static IconData get location_on;
  external static IconData get location_pin;
  external static IconData get location_searching;
  external static IconData get lock_clock;
  external static IconData get lock_open;
  external static IconData get lock_outline;
  external static IconData get lock_person;
  external static IconData get lock_reset;
  external static IconData get login;
  external static IconData get logo_dev;
  external static IconData get logout;
  external static IconData get looks;
  external static IconData get looks_3;
  external static IconData get looks_4;
  external static IconData get looks_5;
  external static IconData get looks_6;
  external static IconData get looks_one;
  external static IconData get looks_two;
  external static IconData get loop;
  external static IconData get loupe;
  external static IconData get low_priority;
  external static IconData get loyalty;
  external static IconData get lte_mobiledata;
  external static IconData get lte_plus_mobiledata;
  external static IconData get luggage;
  external static IconData get lunch_dining;
  external static IconData get lyrics;
  external static IconData get macro_off;
  external static IconData get mail;
  external static IconData get mail_lock;
  external static IconData get mail_outline;
  external static IconData get male;
  external static IconData get man;
  external static IconData get man_2;
  external static IconData get man_3;
  external static IconData get man_4;
  external static IconData get manage_accounts;
  external static IconData get manage_history;
  external static IconData get manage_search;
  external static IconData get map;
  external static IconData get maps_home_work;
  external static IconData get maps_ugc;
  external static IconData get margin;
  external static IconData get mark_as_unread;
  external static IconData get mark_chat_read;
  external static IconData get mark_chat_unread;
  external static IconData get mark_email_read;
  external static IconData get mark_email_unread;
  external static IconData get mark_unread_chat_alt;
  external static IconData get markunread;
  external static IconData get markunread_mailbox;
  external static IconData get masks;
  external static IconData get maximize;
  external static IconData get media_bluetooth_off;
  external static IconData get media_bluetooth_on;
  external static IconData get mediation;
  external static IconData get medical_information;
  external static IconData get medical_services;
  external static IconData get medication;
  external static IconData get medication_liquid;
  external static IconData get meeting_room;
  external static IconData get memory;
  external static IconData get menu_book;
  external static IconData get menu_open;
  external static IconData get merge;
  external static IconData get merge_type;
  external static IconData get message;
  external static IconData get messenger;
  external static IconData get messenger_outline;
  external static IconData get mic_external_off;
  external static IconData get mic_external_on;
  external static IconData get mic_none;
  external static IconData get mic_off;
  external static IconData get microwave;
  external static IconData get military_tech;
  external static IconData get minimize;
  external static IconData get minor_crash;
  external static IconData get miscellaneous_services;
  external static IconData get missed_video_call;
  external static IconData get mms;
  external static IconData get mobile_friendly;
  external static IconData get mobile_off;
  external static IconData get mobile_screen_share;
  external static IconData get mobiledata_off;
  external static IconData get mode;
  external static IconData get mode_comment;
  external static IconData get mode_edit;
  external static IconData get mode_edit_outline;
  external static IconData get mode_fan_off;
  external static IconData get mode_night;
  external static IconData get mode_of_travel;
  external static IconData get mode_standby;
  external static IconData get model_training;
  external static IconData get monetization_on;
  external static IconData get money;
  external static IconData get money_off_csred;
  external static IconData get monitor;
  external static IconData get monitor_heart;
  external static IconData get monitor_weight;
  external static IconData get monochrome_photos;
  external static IconData get mood;
  external static IconData get mood_bad;
  external static IconData get moped;
  external static IconData get more;
  external static IconData get more_time;
  external static IconData get mosque;
  external static IconData get motion_photos_auto;
  external static IconData get motion_photos_off;
  external static IconData get motion_photos_on;
  external static IconData get motion_photos_pause;
  external static IconData get motion_photos_paused;
  external static IconData get motorcycle;
  external static IconData get mouse;
  external static IconData get move_down;
  external static IconData get move_to_inbox;
  external static IconData get move_up;
  external static IconData get movie;
  external static IconData get movie_creation;
  external static IconData get movie_edit;
  external static IconData get movie_filter;
  external static IconData get moving;
  external static IconData get mp;
  external static IconData get multiline_chart;
  external static IconData get multiple_stop;
  external static IconData get multitrack_audio;
  external static IconData get museum;
  external static IconData get music_note;
  external static IconData get music_off;
  external static IconData get music_video;
  external static IconData get my_library_add;
  external static IconData get my_library_books;
  external static IconData get my_library_music;
  external static IconData get my_location;
  external static IconData get nat;
  external static IconData get nature;
  external static IconData get nature_people;
  external static IconData get navigate_before;
  external static IconData get navigate_next;
  external static IconData get navigation;
  external static IconData get near_me;
  external static IconData get near_me_disabled;
  external static IconData get nearby_error;
  external static IconData get nearby_off;
  external static IconData get nest_cam_wired_stand;
  external static IconData get network_cell;
  external static IconData get network_check;
  external static IconData get network_locked;
  external static IconData get network_ping;
  external static IconData get network_wifi;
  external static IconData get network_wifi_1_bar;
  external static IconData get network_wifi_2_bar;
  external static IconData get network_wifi_3_bar;
  external static IconData get new_label;
  external static IconData get new_releases;
  external static IconData get newspaper;
  external static IconData get next_plan;
  external static IconData get next_week;
  external static IconData get nfc;
  external static IconData get night_shelter;
  external static IconData get nightlife;
  external static IconData get nightlight;
  external static IconData get nightlight_round;
  external static IconData get nights_stay;
  external static IconData get no_accounts;
  external static IconData get no_adult_content;
  external static IconData get no_backpack;
  external static IconData get no_cell;
  external static IconData get no_crash;
  external static IconData get no_drinks;
  external static IconData get no_encryption;
  external static IconData get no_encryption_gmailerrorred;
  external static IconData get no_flash;
  external static IconData get no_food;
  external static IconData get no_luggage;
  external static IconData get no_meals;
  external static IconData get no_meals_ouline;
  external static IconData get no_meeting_room;
  external static IconData get no_photography;
  external static IconData get no_sim;
  external static IconData get no_stroller;
  external static IconData get no_transfer;
  external static IconData get noise_aware;
  external static IconData get noise_control_off;
  external static IconData get nordic_walking;
  external static IconData get north;
  external static IconData get north_east;
  external static IconData get north_west;
  external static IconData get not_accessible;
  external static IconData get not_listed_location;
  external static IconData get not_started;
  external static IconData get note;
  external static IconData get note_add;
  external static IconData get note_alt;
  external static IconData get notes;
  external static IconData get notification_add;
  external static IconData get notification_important;
  external static IconData get notifications_active;
  external static IconData get notifications_none;
  external static IconData get notifications_off;
  external static IconData get notifications_on;
  external static IconData get notifications_paused;
  external static IconData get now_wallpaper;
  external static IconData get now_widgets;
  external static IconData get numbers;
  external static IconData get offline_bolt;
  external static IconData get offline_pin;
  external static IconData get offline_share;
  external static IconData get oil_barrel;
  external static IconData get on_device_training;
  external static IconData get ondemand_video;
  external static IconData get online_prediction;
  external static IconData get opacity;
  external static IconData get open_in_browser;
  external static IconData get open_in_full;
  external static IconData get open_in_new;
  external static IconData get open_in_new_off;
  external static IconData get open_with;
  external static IconData get other_houses;
  external static IconData get outbond;
  external static IconData get outbound;
  external static IconData get outbox;
  external static IconData get outdoor_grill;
  external static IconData get outgoing_mail;
  external static IconData get outlet;
  external static IconData get outlined_flag;
  external static IconData get output;
  external static IconData get padding;
  external static IconData get pages;
  external static IconData get pageview;
  external static IconData get paid;
  external static IconData get palette;
  external static IconData get pallet;
  external static IconData get pan_tool;
  external static IconData get pan_tool_alt;
  external static IconData get panorama;
  external static IconData get panorama_fish_eye;
  external static IconData get panorama_fisheye;
  external static IconData get panorama_horizontal;
  external static IconData get panorama_horizontal_select;
  external static IconData get panorama_photosphere;
  external static IconData get panorama_photosphere_select;
  external static IconData get panorama_vertical;
  external static IconData get panorama_vertical_select;
  external static IconData get panorama_wide_angle;
  external static IconData get panorama_wide_angle_select;
  external static IconData get paragliding;
  external static IconData get park;
  external static IconData get party_mode;
  external static IconData get password;
  external static IconData get pattern;
  external static IconData get pause;
  external static IconData get pause_circle;
  external static IconData get pause_circle_filled;
  external static IconData get pause_circle_outline;
  external static IconData get pause_presentation;
  external static IconData get payment;
  external static IconData get payments;
  external static IconData get paypal;
  external static IconData get pedal_bike;
  external static IconData get pending;
  external static IconData get pending_actions;
  external static IconData get pentagon;
  external static IconData get people;
  external static IconData get people_alt;
  external static IconData get people_outline;
  external static IconData get percent;
  external static IconData get perm_camera_mic;
  external static IconData get perm_contact_cal;
  external static IconData get perm_contact_calendar;
  external static IconData get perm_data_setting;
  external static IconData get perm_device_info;
  external static IconData get perm_device_information;
  external static IconData get perm_identity;
  external static IconData get perm_media;
  external static IconData get perm_phone_msg;
  external static IconData get perm_scan_wifi;
  external static IconData get person_2;
  external static IconData get person_3;
  external static IconData get person_4;
  external static IconData get person_add_alt;
  external static IconData get person_add_alt_1;
  external static IconData get person_add_disabled;
  external static IconData get person_off;
  external static IconData get person_outline;
  external static IconData get person_pin;
  external static IconData get person_pin_circle;
  external static IconData get person_remove;
  external static IconData get person_remove_alt_1;
  external static IconData get person_search;
  external static IconData get personal_injury;
  external static IconData get personal_video;
  external static IconData get pest_control;
  external static IconData get pest_control_rodent;
  external static IconData get pets;
  external static IconData get phishing;
  external static IconData get phone_android;
  external static IconData get phone_bluetooth_speaker;
  external static IconData get phone_callback;
  external static IconData get phone_disabled;
  external static IconData get phone_enabled;
  external static IconData get phone_forwarded;
  external static IconData get phone_in_talk;
  external static IconData get phone_iphone;
  external static IconData get phone_locked;
  external static IconData get phone_missed;
  external static IconData get phone_paused;
  external static IconData get phonelink;
  external static IconData get phonelink_erase;
  external static IconData get phonelink_lock;
  external static IconData get phonelink_off;
  external static IconData get phonelink_ring;
  external static IconData get phonelink_setup;
  external static IconData get photo_album;
  external static IconData get photo_camera;
  external static IconData get photo_camera_back;
  external static IconData get photo_camera_front;
  external static IconData get photo_filter;
  external static IconData get photo_size_select_actual;
  external static IconData get photo_size_select_large;
  external static IconData get photo_size_select_small;
  external static IconData get php;
  external static IconData get piano;
  external static IconData get piano_off;
  external static IconData get picture_as_pdf;
  external static IconData get picture_in_picture;
  external static IconData get picture_in_picture_alt;
  external static IconData get pie_chart_outline;
  external static IconData get pie_chart_outlined;
  external static IconData get pin;
  external static IconData get pin_drop;
  external static IconData get pin_end;
  external static IconData get pin_invoke;
  external static IconData get pinch;
  external static IconData get pivot_table_chart;
  external static IconData get pix;
  external static IconData get plagiarism;
  external static IconData get play_arrow;
  external static IconData get play_circle;
  external static IconData get play_circle_fill;
  external static IconData get play_circle_filled;
  external static IconData get play_circle_outline;
  external static IconData get play_disabled;
  external static IconData get play_for_work;
  external static IconData get play_lesson;
  external static IconData get playlist_add;
  external static IconData get playlist_add_check;
  external static IconData get playlist_add_check_circle;
  external static IconData get playlist_add_circle;
  external static IconData get playlist_play;
  external static IconData get playlist_remove;
  external static IconData get plumbing;
  external static IconData get plus_one;
  external static IconData get podcasts;
  external static IconData get point_of_sale;
  external static IconData get policy;
  external static IconData get poll;
  external static IconData get polyline;
  external static IconData get polymer;
  external static IconData get pool;
  external static IconData get portable_wifi_off;
  external static IconData get portrait;
  external static IconData get post_add;
  external static IconData get power;
  external static IconData get power_input;
  external static IconData get power_off;
  external static IconData get power_settings_new;
  external static IconData get precision_manufacturing;
  external static IconData get pregnant_woman;
  external static IconData get present_to_all;
  external static IconData get preview;
  external static IconData get price_change;
  external static IconData get price_check;
  external static IconData get print;
  external static IconData get print_disabled;
  external static IconData get priority_high;
  external static IconData get privacy_tip;
  external static IconData get private_connectivity;
  external static IconData get production_quantity_limits;
  external static IconData get propane;
  external static IconData get propane_tank;
  external static IconData get psychology;
  external static IconData get psychology_alt;
  external static IconData get public_;
  external static IconData get public_off;
  external static IconData get publish;
  external static IconData get published_with_changes;
  external static IconData get punch_clock;
  external static IconData get push_pin;
  external static IconData get qr_code;
  external static IconData get qr_code_2;
  external static IconData get qr_code_scanner;
  external static IconData get query_builder;
  external static IconData get query_stats;
  external static IconData get question_answer;
  external static IconData get question_mark;
  external static IconData get queue;
  external static IconData get queue_music;
  external static IconData get queue_play_next;
  external static IconData get quick_contacts_dialer;
  external static IconData get quick_contacts_mail;
  external static IconData get quickreply;
  external static IconData get quiz;
  external static IconData get quora;
  external static IconData get r_mobiledata;
  external static IconData get radar;
  external static IconData get radio;
  external static IconData get radio_button_checked;
  external static IconData get radio_button_off;
  external static IconData get radio_button_on;
  external static IconData get radio_button_unchecked;
  external static IconData get railway_alert;
  external static IconData get ramen_dining;
  external static IconData get ramp_left;
  external static IconData get ramp_right;
  external static IconData get rate_review;
  external static IconData get raw_off;
  external static IconData get raw_on;
  external static IconData get read_more;
  external static IconData get real_estate_agent;
  external static IconData get rebase_edit;
  external static IconData get receipt;
  external static IconData get receipt_long;
  external static IconData get recent_actors;
  external static IconData get recommend;
  external static IconData get record_voice_over;
  external static IconData get rectangle;
  external static IconData get recycling;
  external static IconData get reddit;
  external static IconData get redeem;
  external static IconData get redo;
  external static IconData get reduce_capacity;
  external static IconData get remember_me;
  external static IconData get remove_circle;
  external static IconData get remove_done;
  external static IconData get remove_from_queue;
  external static IconData get remove_moderator;
  external static IconData get remove_red_eye;
  external static IconData get remove_road;
  external static IconData get remove_shopping_cart;
  external static IconData get reorder;
  external static IconData get repartition;
  external static IconData get repeat;
  external static IconData get repeat_on;
  external static IconData get repeat_one;
  external static IconData get repeat_one_on;
  external static IconData get replay_10;
  external static IconData get replay_30;
  external static IconData get replay_5;
  external static IconData get replay_circle_filled;
  external static IconData get reply;
  external static IconData get report;
  external static IconData get report_gmailerrorred;
  external static IconData get report_off;
  external static IconData get report_problem;
  external static IconData get request_page;
  external static IconData get request_quote;
  external static IconData get reset_tv;
  external static IconData get restart_alt;
  external static IconData get restaurant;
  external static IconData get restore;
  external static IconData get restore_from_trash;
  external static IconData get restore_page;
  external static IconData get reviews;
  external static IconData get rice_bowl;
  external static IconData get ring_volume;
  external static IconData get rocket;
  external static IconData get rocket_launch;
  external static IconData get roller_shades;
  external static IconData get roller_shades_closed;
  external static IconData get roller_skating;
  external static IconData get roofing;
  external static IconData get room;
  external static IconData get room_preferences;
  external static IconData get room_service;
  external static IconData get rotate_90_degrees_ccw;
  external static IconData get rotate_90_degrees_cw;
  external static IconData get rotate_left;
  external static IconData get rotate_right;
  external static IconData get roundabout_left;
  external static IconData get roundabout_right;
  external static IconData get rounded_corner;
  external static IconData get route;
  external static IconData get router;
  external static IconData get rowing;
  external static IconData get rss_feed;
  external static IconData get rsvp;
  external static IconData get rtt;
  external static IconData get rule;
  external static IconData get rule_folder;
  external static IconData get run_circle;
  external static IconData get running_with_errors;
  external static IconData get rv_hookup;
  external static IconData get safety_check;
  external static IconData get safety_divider;
  external static IconData get sailing;
  external static IconData get sanitizer;
  external static IconData get satellite;
  external static IconData get satellite_alt;
  external static IconData get save;
  external static IconData get save_alt;
  external static IconData get save_as;
  external static IconData get saved_search;
  external static IconData get savings;
  external static IconData get scale;
  external static IconData get scanner;
  external static IconData get scatter_plot;
  external static IconData get schedule;
  external static IconData get schedule_send;
  external static IconData get schema;
  external static IconData get school;
  external static IconData get science;
  external static IconData get score;
  external static IconData get scoreboard;
  external static IconData get screen_lock_landscape;
  external static IconData get screen_lock_portrait;
  external static IconData get screen_lock_rotation;
  external static IconData get screen_rotation;
  external static IconData get screen_rotation_alt;
  external static IconData get screen_search_desktop;
  external static IconData get screen_share;
  external static IconData get screenshot;
  external static IconData get screenshot_monitor;
  external static IconData get scuba_diving;
  external static IconData get sd;
  external static IconData get sd_card;
  external static IconData get sd_card_alert;
  external static IconData get sd_storage;
  external static IconData get search_off;
  external static IconData get security_update;
  external static IconData get security_update_good;
  external static IconData get security_update_warning;
  external static IconData get segment;
  external static IconData get select_all;
  external static IconData get self_improvement;
  external static IconData get sell;
  external static IconData get send;
  external static IconData get send_and_archive;
  external static IconData get send_time_extension;
  external static IconData get send_to_mobile;
  external static IconData get sensor_door;
  external static IconData get sensor_occupied;
  external static IconData get sensor_window;
  external static IconData get sensors;
  external static IconData get sensors_off;
  external static IconData get sentiment_dissatisfied;
  external static IconData get sentiment_neutral;
  external static IconData get sentiment_satisfied;
  external static IconData get sentiment_satisfied_alt;
  external static IconData get sentiment_very_dissatisfied;
  external static IconData get sentiment_very_satisfied;
  external static IconData get set_meal;
  external static IconData get settings_accessibility;
  external static IconData get settings_applications;
  external static IconData get settings_backup_restore;
  external static IconData get settings_bluetooth;
  external static IconData get settings_brightness;
  external static IconData get settings_cell;
  external static IconData get settings_display;
  external static IconData get settings_ethernet;
  external static IconData get settings_input_antenna;
  external static IconData get settings_input_component;
  external static IconData get settings_input_composite;
  external static IconData get settings_input_hdmi;
  external static IconData get settings_input_svideo;
  external static IconData get settings_overscan;
  external static IconData get settings_phone;
  external static IconData get settings_power;
  external static IconData get settings_remote;
  external static IconData get settings_suggest;
  external static IconData get settings_system_daydream;
  external static IconData get settings_voice;
  external static IconData get severe_cold;
  external static IconData get shape_line;
  external static IconData get share_arrival_time;
  external static IconData get share_location;
  external static IconData get shelves;
  external static IconData get shield;
  external static IconData get shield_moon;
  external static IconData get shop;
  external static IconData get shop_2;
  external static IconData get shop_two;
  external static IconData get shopify;
  external static IconData get shopping_bag;
  external static IconData get shopping_basket;
  external static IconData get shopping_cart_checkout;
  external static IconData get short_text;
  external static IconData get shortcut;
  external static IconData get show_chart;
  external static IconData get shower;
  external static IconData get shuffle;
  external static IconData get shuffle_on;
  external static IconData get shutter_speed;
  external static IconData get sick;
  external static IconData get sign_language;
  external static IconData get signal_cellular_0_bar;
  external static IconData get signal_cellular_4_bar;
  external static IconData get signal_cellular_alt;
  external static IconData get signal_cellular_alt_1_bar;
  external static IconData get signal_cellular_alt_2_bar;
  external static IconData get signal_cellular_connected_no_internet_0_bar;
  external static IconData get signal_cellular_connected_no_internet_4_bar;
  external static IconData get signal_cellular_no_sim;
  external static IconData get signal_cellular_nodata;
  external static IconData get signal_cellular_null;
  external static IconData get signal_cellular_off;
  external static IconData get signal_wifi_0_bar;
  external static IconData get signal_wifi_4_bar;
  external static IconData get signal_wifi_4_bar_lock;
  external static IconData get signal_wifi_bad;
  external static IconData get signal_wifi_connected_no_internet_4;
  external static IconData get signal_wifi_off;
  external static IconData get signal_wifi_statusbar_4_bar;
  external static IconData get signal_wifi_statusbar_connected_no_internet_4;
  external static IconData get signal_wifi_statusbar_null;
  external static IconData get signpost;
  external static IconData get sim_card;
  external static IconData get sim_card_alert;
  external static IconData get sim_card_download;
  external static IconData get single_bed;
  external static IconData get sip;
  external static IconData get skateboarding;
  external static IconData get skip_next;
  external static IconData get skip_previous;
  external static IconData get sledding;
  external static IconData get slideshow;
  external static IconData get slow_motion_video;
  external static IconData get smart_button;
  external static IconData get smart_display;
  external static IconData get smart_screen;
  external static IconData get smart_toy;
  external static IconData get smartphone;
  external static IconData get smoke_free;
  external static IconData get smoking_rooms;
  external static IconData get sms;
  external static IconData get sms_failed;
  external static IconData get snapchat;
  external static IconData get snippet_folder;
  external static IconData get snooze;
  external static IconData get snowboarding;
  external static IconData get snowing;
  external static IconData get snowmobile;
  external static IconData get snowshoeing;
  external static IconData get soap;
  external static IconData get social_distance;
  external static IconData get solar_power;
  external static IconData get sort_by_alpha;
  external static IconData get sos;
  external static IconData get soup_kitchen;
  external static IconData get source;
  external static IconData get south;
  external static IconData get south_america;
  external static IconData get south_east;
  external static IconData get south_west;
  external static IconData get spa;
  external static IconData get space_bar;
  external static IconData get space_dashboard;
  external static IconData get spatial_audio;
  external static IconData get spatial_audio_off;
  external static IconData get spatial_tracking;
  external static IconData get speaker;
  external static IconData get speaker_group;
  external static IconData get speaker_notes;
  external static IconData get speaker_notes_off;
  external static IconData get speaker_phone;
  external static IconData get speed;
  external static IconData get spellcheck;
  external static IconData get splitscreen;
  external static IconData get spoke;
  external static IconData get sports;
  external static IconData get sports_bar;
  external static IconData get sports_baseball;
  external static IconData get sports_basketball;
  external static IconData get sports_cricket;
  external static IconData get sports_esports;
  external static IconData get sports_football;
  external static IconData get sports_golf;
  external static IconData get sports_gymnastics;
  external static IconData get sports_handball;
  external static IconData get sports_hockey;
  external static IconData get sports_kabaddi;
  external static IconData get sports_martial_arts;
  external static IconData get sports_mma;
  external static IconData get sports_motorsports;
  external static IconData get sports_rugby;
  external static IconData get sports_score;
  external static IconData get sports_soccer;
  external static IconData get sports_tennis;
  external static IconData get sports_volleyball;
  external static IconData get square;
  external static IconData get square_foot;
  external static IconData get ssid_chart;
  external static IconData get stacked_bar_chart;
  external static IconData get stacked_line_chart;
  external static IconData get stadium;
  external static IconData get stairs;
  external static IconData get star_border_purple500;
  external static IconData get star_half;
  external static IconData get star_outline;
  external static IconData get star_purple500;
  external static IconData get star_rate;
  external static IconData get stars;
  external static IconData get start;
  external static IconData get stay_current_landscape;
  external static IconData get stay_current_portrait;
  external static IconData get stay_primary_landscape;
  external static IconData get stay_primary_portrait;
  external static IconData get sticky_note_2;
  external static IconData get stop;
  external static IconData get stop_circle;
  external static IconData get stop_screen_share;
  external static IconData get storage;
  external static IconData get store;
  external static IconData get store_mall_directory;
  external static IconData get storefront;
  external static IconData get storm;
  external static IconData get straight;
  external static IconData get straighten;
  external static IconData get stream;
  external static IconData get streetview;
  external static IconData get strikethrough_s;
  external static IconData get stroller;
  external static IconData get style;
  external static IconData get subdirectory_arrow_left;
  external static IconData get subdirectory_arrow_right;
  external static IconData get subject;
  external static IconData get subscript;
  external static IconData get subscriptions;
  external static IconData get subtitles;
  external static IconData get subtitles_off;
  external static IconData get subway;
  external static IconData get summarize;
  external static IconData get sunny;
  external static IconData get sunny_snowing;
  external static IconData get superscript;
  external static IconData get supervised_user_circle;
  external static IconData get supervisor_account;
  external static IconData get support;
  external static IconData get support_agent;
  external static IconData get surfing;
  external static IconData get surround_sound;
  external static IconData get swap_calls;
  external static IconData get swap_horiz;
  external static IconData get swap_horizontal_circle;
  external static IconData get swap_vert;
  external static IconData get swap_vert_circle;
  external static IconData get swap_vertical_circle;
  external static IconData get swipe;
  external static IconData get swipe_down;
  external static IconData get swipe_down_alt;
  external static IconData get swipe_left;
  external static IconData get swipe_left_alt;
  external static IconData get swipe_right;
  external static IconData get swipe_right_alt;
  external static IconData get swipe_up;
  external static IconData get swipe_up_alt;
  external static IconData get swipe_vertical;
  external static IconData get switch_access_shortcut;
  external static IconData get switch_access_shortcut_add;
  external static IconData get switch_account;
  external static IconData get switch_camera;
  external static IconData get switch_left;
  external static IconData get switch_right;
  external static IconData get switch_video;
  external static IconData get synagogue;
  external static IconData get sync;
  external static IconData get sync_alt;
  external static IconData get sync_disabled;
  external static IconData get sync_lock;
  external static IconData get sync_problem;
  external static IconData get system_security_update;
  external static IconData get system_security_update_good;
  external static IconData get system_security_update_warning;
  external static IconData get system_update;
  external static IconData get system_update_alt;
  external static IconData get system_update_tv;
  external static IconData get tab;
  external static IconData get tab_unselected;
  external static IconData get table_bar;
  external static IconData get table_restaurant;
  external static IconData get table_rows;
  external static IconData get table_view;
  external static IconData get tablet;
  external static IconData get tablet_android;
  external static IconData get tablet_mac;
  external static IconData get tag;
  external static IconData get tag_faces;
  external static IconData get takeout_dining;
  external static IconData get tap_and_play;
  external static IconData get tapas;
  external static IconData get task;
  external static IconData get task_alt;
  external static IconData get taxi_alert;
  external static IconData get telegram;
  external static IconData get temple_buddhist;
  external static IconData get temple_hindu;
  external static IconData get terminal;
  external static IconData get terrain;
  external static IconData get text_decrease;
  external static IconData get text_fields;
  external static IconData get text_format;
  external static IconData get text_increase;
  external static IconData get text_rotate_up;
  external static IconData get text_rotate_vertical;
  external static IconData get text_rotation_angledown;
  external static IconData get text_rotation_angleup;
  external static IconData get text_rotation_down;
  external static IconData get text_rotation_none;
  external static IconData get text_snippet;
  external static IconData get textsms;
  external static IconData get texture;
  external static IconData get theater_comedy;
  external static IconData get theaters;
  external static IconData get thermostat;
  external static IconData get thermostat_auto;
  external static IconData get thumb_down;
  external static IconData get thumb_down_alt;
  external static IconData get thumb_down_off_alt;
  external static IconData get thumb_up;
  external static IconData get thumb_up_alt;
  external static IconData get thumb_up_off_alt;
  external static IconData get thumbs_up_down;
  external static IconData get thunderstorm;
  external static IconData get tiktok;
  external static IconData get time_to_leave;
  external static IconData get timelapse;
  external static IconData get timeline;
  external static IconData get timer;
  external static IconData get timer_10;
  external static IconData get timer_10_select;
  external static IconData get timer_3;
  external static IconData get timer_3_select;
  external static IconData get timer_off;
  external static IconData get tips_and_updates;
  external static IconData get tire_repair;
  external static IconData get title;
  external static IconData get toc;
  external static IconData get today;
  external static IconData get toggle_off;
  external static IconData get toggle_on;
  external static IconData get token;
  external static IconData get toll;
  external static IconData get tonality;
  external static IconData get topic;
  external static IconData get tornado;
  external static IconData get touch_app;
  external static IconData get tour;
  external static IconData get toys;
  external static IconData get track_changes;
  external static IconData get traffic;
  external static IconData get train;
  external static IconData get tram;
  external static IconData get transcribe;
  external static IconData get transfer_within_a_station;
  external static IconData get transform;
  external static IconData get transgender;
  external static IconData get transit_enterexit;
  external static IconData get translate;
  external static IconData get travel_explore;
  external static IconData get trending_down;
  external static IconData get trending_flat;
  external static IconData get trending_neutral;
  external static IconData get trending_up;
  external static IconData get trip_origin;
  external static IconData get trolley;
  external static IconData get troubleshoot;
  external static IconData get try_;
  external static IconData get tsunami;
  external static IconData get tty;
  external static IconData get tungsten;
  external static IconData get turn_left;
  external static IconData get turn_right;
  external static IconData get turn_sharp_left;
  external static IconData get turn_sharp_right;
  external static IconData get turn_slight_left;
  external static IconData get turn_slight_right;
  external static IconData get turned_in;
  external static IconData get turned_in_not;
  external static IconData get tv;
  external static IconData get tv_off;
  external static IconData get two_wheeler;
  external static IconData get type_specimen;
  external static IconData get u_turn_left;
  external static IconData get u_turn_right;
  external static IconData get umbrella;
  external static IconData get unarchive;
  external static IconData get undo;
  external static IconData get unfold_less;
  external static IconData get unfold_less_double;
  external static IconData get unfold_more;
  external static IconData get unfold_more_double;
  external static IconData get unpublished;
  external static IconData get unsubscribe;
  external static IconData get upcoming;
  external static IconData get update;
  external static IconData get update_disabled;
  external static IconData get upgrade;
  external static IconData get upload;
  external static IconData get upload_file;
  external static IconData get usb;
  external static IconData get usb_off;
  external static IconData get vaccines;
  external static IconData get vape_free;
  external static IconData get vaping_rooms;
  external static IconData get verified;
  external static IconData get verified_user;
  external static IconData get vertical_align_bottom;
  external static IconData get vertical_align_center;
  external static IconData get vertical_align_top;
  external static IconData get vertical_distribute;
  external static IconData get vertical_shades;
  external static IconData get vertical_shades_closed;
  external static IconData get vibration;
  external static IconData get video_call;
  external static IconData get video_camera_back;
  external static IconData get video_camera_front;
  external static IconData get video_chat;
  external static IconData get video_collection;
  external static IconData get video_file;
  external static IconData get video_label;
  external static IconData get video_library;
  external static IconData get video_settings;
  external static IconData get video_stable;
  external static IconData get videocam;
  external static IconData get videocam_off;
  external static IconData get videogame_asset;
  external static IconData get videogame_asset_off;
  external static IconData get view_agenda;
  external static IconData get view_array;
  external static IconData get view_carousel;
  external static IconData get view_column;
  external static IconData get view_comfortable;
  external static IconData get view_comfy;
  external static IconData get view_comfy_alt;
  external static IconData get view_compact;
  external static IconData get view_compact_alt;
  external static IconData get view_cozy;
  external static IconData get view_day;
  external static IconData get view_headline;
  external static IconData get view_in_ar;
  external static IconData get view_kanban;
  external static IconData get view_list;
  external static IconData get view_module;
  external static IconData get view_quilt;
  external static IconData get view_sidebar;
  external static IconData get view_stream;
  external static IconData get view_timeline;
  external static IconData get view_week;
  external static IconData get vignette;
  external static IconData get villa;
  external static IconData get voice_chat;
  external static IconData get voice_over_off;
  external static IconData get voicemail;
  external static IconData get volcano;
  external static IconData get volume_down;
  external static IconData get volume_down_alt;
  external static IconData get volume_mute;
  external static IconData get volume_off;
  external static IconData get volume_up;
  external static IconData get volunteer_activism;
  external static IconData get vpn_key;
  external static IconData get vpn_key_off;
  external static IconData get vpn_lock;
  external static IconData get vrpano;
  external static IconData get wallet;
  external static IconData get wallet_giftcard;
  external static IconData get wallet_membership;
  external static IconData get wallet_travel;
  external static IconData get wallpaper;
  external static IconData get warehouse;
  external static IconData get warning;
  external static IconData get warning_amber;
  external static IconData get wash;
  external static IconData get watch;
  external static IconData get watch_later;
  external static IconData get watch_off;
  external static IconData get water;
  external static IconData get water_damage;
  external static IconData get water_drop;
  external static IconData get waterfall_chart;
  external static IconData get waves;
  external static IconData get waving_hand;
  external static IconData get wb_auto;
  external static IconData get wb_cloudy;
  external static IconData get wb_incandescent;
  external static IconData get wb_iridescent;
  external static IconData get wb_shade;
  external static IconData get wb_sunny;
  external static IconData get wb_twighlight;
  external static IconData get wb_twilight;
  external static IconData get wc;
  external static IconData get web;
  external static IconData get web_asset_off;
  external static IconData get web_stories;
  external static IconData get webhook;
  external static IconData get wechat;
  external static IconData get weekend;
  external static IconData get west;
  external static IconData get whatshot;
  external static IconData get wheelchair_pickup;
  external static IconData get where_to_vote;
  external static IconData get widgets;
  external static IconData get width_full;
  external static IconData get width_normal;
  external static IconData get width_wide;
  external static IconData get wifi;
  external static IconData get wifi_1_bar;
  external static IconData get wifi_2_bar;
  external static IconData get wifi_calling;
  external static IconData get wifi_calling_3;
  external static IconData get wifi_channel;
  external static IconData get wifi_find;
  external static IconData get wifi_lock;
  external static IconData get wifi_off;
  external static IconData get wifi_password;
  external static IconData get wifi_protected_setup;
  external static IconData get wifi_tethering;
  external static IconData get wifi_tethering_error;
  external static IconData get wifi_tethering_error_rounded;
  external static IconData get wifi_tethering_off;
  external static IconData get wind_power;
  external static IconData get window;
  external static IconData get wine_bar;
  external static IconData get woman;
  external static IconData get woman_2;
  external static IconData get woo_commerce;
  external static IconData get wordpress;
  external static IconData get work;
  external static IconData get work_history;
  external static IconData get work_off;
  external static IconData get work_outline;
  external static IconData get workspace_premium;
  external static IconData get workspaces;
  external static IconData get workspaces_filled;
  external static IconData get workspaces_outline;
  external static IconData get wrap_text;
  external static IconData get wrong_location;
  external static IconData get wysiwyg;
  external static IconData get yard;
  external static IconData get youtube_searched_for;
  external static IconData get zoom_in;
  external static IconData get zoom_in_map;
  external static IconData get zoom_out;
  external static IconData get zoom_out_map;
  external static IconData get access_alarm;
  external static IconData get access_time;
  external static IconData get account_circle;
  external static IconData get add;
  external static IconData get add_circle;
  external static IconData get add_circle_outline;
  external static IconData get add_comment;
  external static IconData get add_shopping_cart;
  external static IconData get airplanemode_active;
  external static IconData get alarm_on;
  external static IconData get arrow_back;
  external static IconData get arrow_back_ios;
  external static IconData get arrow_drop_down;
  external static IconData get arrow_drop_up;
  external static IconData get arrow_forward;
  external static IconData get arrow_forward_ios;
  external static IconData get arrow_left;
  external static IconData get attach_money;
  external static IconData get book;
  external static IconData get bookmark_border;
  external static IconData get brightness_5;
  external static IconData get calendar_today;
  external static IconData get camera_enhance;
  external static IconData get check;
  external static IconData get check_circle;
  external static IconData get check_circle_outline;
  external static IconData get chevron_right;
  external static IconData get close;
  external static IconData get code;
  external static IconData get comment;
  external static IconData get create;
  external static IconData get credit_card;
  external static IconData get date_range;
  external static IconData get delete;
  external static IconData get directions_bike;
  external static IconData get edit;
  external static IconData get email;
  external static IconData get favorite;
  external static IconData get favorite_border;
  external static IconData get feedback;
  external static IconData get format_bold;
  external static IconData get format_italic;
  external static IconData get format_underline;
  external static IconData get fullscreen;
  external static IconData get help;
  external static IconData get home;
  external static IconData get hotel;
  external static IconData get info;
  external static IconData get info_outline;
  external static IconData get keyboard_arrow_down;
  external static IconData get keyboard_arrow_up;
  external static IconData get library_books;
  external static IconData get link;
  external static IconData get lock;
  external static IconData get menu;
  external static IconData get mic;
  external static IconData get money_off;
  external static IconData get more_vert;
  external static IconData get not_interested;
  external static IconData get notifications;
  external static IconData get person;
  external static IconData get person_add;
  external static IconData get phone;
  external static IconData get photo;
  external static IconData get photo_library;
  external static IconData get pie_chart;
  external static IconData get place;
  external static IconData get remove;
  external static IconData get remove_circle_outline;
  external static IconData get refresh;
  external static IconData get replay;
  external static IconData get reply_all;
  external static IconData get restaurant_menu;
  external static IconData get search;
  external static IconData get security;
  external static IconData get settings;
  external static IconData get share;
  external static IconData get shopping_cart;
  external static IconData get sort;
  external static IconData get star;
  external static IconData get star_border;
  external static IconData get table_chart;
  external static IconData get tune;
  external static IconData get vertical_split;
  external static IconData get visibility;
  external static IconData get visibility_off;
  external static IconData get web_asset;
}

// --- basic widgets ----------------------------------------------------

@JavaName('com.codename1.flutter.widgets.Text')
class Text extends Widget {
  external Text(String data, {Key? key, TextStyle? style, TextAlign? textAlign});
}

@JavaName('com.codename1.flutter.widgets.Icon')
class Icon extends Widget {
  external Icon(IconData icon, {Key? key, double? size, Color? color});
}

@JavaName('com.codename1.flutter.widgets.Column')
class Column extends Widget {
  external Column({Key? key, MainAxisAlignment? mainAxisAlignment, CrossAxisAlignment? crossAxisAlignment, MainAxisSize? mainAxisSize, List<Widget> children});
}

@JavaName('com.codename1.flutter.widgets.Row')
class Row extends Widget {
  external Row({Key? key, MainAxisAlignment? mainAxisAlignment, CrossAxisAlignment? crossAxisAlignment, MainAxisSize? mainAxisSize, List<Widget> children});
}

@JavaName('com.codename1.flutter.widgets.Center')
class Center extends Widget {
  external Center({Key? key, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Padding')
class Padding extends Widget {
  external Padding({Key? key, EdgeInsets padding, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.SizedBox')
class SizedBox extends Widget {
  external SizedBox({Key? key, double? width, double? height, Widget? child});
  external static SizedBox shrink({Key? key, Widget? child});
  external static SizedBox expand({Key? key, Widget? child});
  external static SizedBox fromSize({Key? key, Size? size, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Expanded')
class Expanded extends Widget {
  external Expanded({Key? key, int flex, Widget child});
}

// --- material ---------------------------------------------------------

@JavaName('com.codename1.flutter.material.MaterialApp')
class MaterialApp extends Widget {
  external MaterialApp({Key? key, String? title, ThemeData? theme, ThemeData? darkTheme, ThemeMode? themeMode, Widget? home, Route Function(RouteSettings)? onGenerateRoute});
  // go_router's MaterialApp.router(routerConfig: GoRouter(...)).
  external static MaterialApp router({Key? key, Object? routerConfig, String? title, ThemeData? theme,
      ThemeData? darkTheme, ThemeMode? themeMode, bool? debugShowCheckedModeBanner,
      dynamic localizationsDelegates, dynamic supportedLocales, Locale? locale});
}

@JavaName('com.codename1.flutter.material.Scaffold')
class Scaffold extends Widget {
  external Scaffold({Key? key, Widget? appBar, Widget? body, Widget? floatingActionButton, Widget? drawer, Widget? bottomNavigationBar});
  external static ScaffoldState of(BuildContext context);
}

@JavaName('com.codename1.flutter.material.AppBar')
class AppBar extends Widget {
  external AppBar({Key? key, Widget? title, Color? backgroundColor, bool? centerTitle,
      List<Widget>? actions, Widget? leading, bool? automaticallyImplyLeading, Widget? bottom,
      double? elevation, SystemUiOverlayStyle? systemOverlayStyle, double? titleSpacing,
      double? toolbarHeight, IconThemeData? iconTheme, Color? foregroundColor,
      Widget? flexibleSpace, bool? primary, TextStyle? titleTextStyle});
  // The height this app bar prefers to occupy — Flutter's
  // `AppBar.preferredSize` (an app bar implements PreferredSizeWidget).
  external Size get preferredSize;
}

@JavaName('com.codename1.flutter.material.FloatingActionButton')
class FloatingActionButton extends Widget {
  external FloatingActionButton({Key? key, VoidCallback? onPressed, String? tooltip, Widget? child});
  external static FloatingActionButton extended({Key? key, VoidCallback? onPressed, Widget? label, Widget? icon, String? tooltip, Object? heroTag, Color? backgroundColor});
}

@JavaName('com.codename1.flutter.material.ThemeData')
class ThemeData {
  external ThemeData({ColorScheme? colorScheme, Color? colorSchemeSeed, bool? useMaterial3, Brightness? brightness,
      TextTheme? textTheme, TextTheme? primaryTextTheme, Color? primaryColor,
      Color? scaffoldBackgroundColor, Color? canvasColor, Color? cardColor, Color? dividerColor,
      Color? focusColor, Color? highlightColor, Color? splashColor, Color? hintColor,
      Color? disabledColor, Color? shadowColor, Color? indicatorColor, Color? secondaryHeaderColor,
      IconThemeData? iconTheme, IconThemeData? primaryIconTheme, AppBarTheme? appBarTheme,
      ChipThemeData? chipTheme, CheckboxThemeData? checkboxTheme, CardTheme? cardTheme,
      BottomAppBarThemeData? bottomAppBarTheme, DividerThemeData? dividerTheme,
      dynamic snackBarTheme, dynamic inputDecorationTheme, dynamic radioTheme, dynamic switchTheme,
      dynamic tooltipTheme, dynamic bottomSheetTheme, dynamic floatingActionButtonTheme,
      dynamic elevatedButtonTheme, dynamic textButtonTheme, dynamic outlinedButtonTheme,
      dynamic pageTransitionsTheme, dynamic visualDensity, dynamic typography, dynamic platform,
      NavigationRailThemeData? navigationRailTheme,
      bool? applyElevationOverlayColor, String? fontFamily});
  external static ThemeData dark({bool? useMaterial3});
  external ColorScheme get colorScheme;
  external TextTheme get textTheme;
  external TextTheme get primaryTextTheme;
  external Brightness get brightness;
  external IconThemeData? get iconTheme;
  external AppBarTheme? get appBarTheme;
  external ChipThemeData? get chipTheme;
  external CardTheme? get cardTheme;
  external DividerThemeData? get dividerTheme;
  external Color? get primaryColor;
  external Color? get scaffoldBackgroundColor;
  external Color? get canvasColor;
  external Color? get cardColor;
  external Color? get dividerColor;
  external Color? get focusColor;
  external Color? get highlightColor;
  external Color? get splashColor;
  external Color? get hintColor;
  external Color? get disabledColor;
  external dynamic get platform;
  // The ambient NavigationRail theme — Flutter's `ThemeData.navigationRailTheme`.
  external NavigationRailThemeData get navigationRailTheme;
  // The ambient Slider / BottomSheet themes — Flutter's `ThemeData.sliderTheme`
  // / `ThemeData.bottomSheetTheme`.
  external SliderThemeData get sliderTheme;
  external BottomSheetThemeData get bottomSheetTheme;
  external ThemeData copyWith({ColorScheme? colorScheme, TextTheme? textTheme, TextTheme? primaryTextTheme,
      Brightness? brightness, Color? primaryColor, Color? scaffoldBackgroundColor, Color? canvasColor,
      Color? cardColor, Color? dividerColor, Color? focusColor, Color? highlightColor, Color? splashColor,
      Color? hintColor, Color? disabledColor, IconThemeData? iconTheme, AppBarTheme? appBarTheme,
      ChipThemeData? chipTheme, CardTheme? cardTheme, DividerThemeData? dividerTheme, dynamic platform,
      NavigationRailThemeData? navigationRailTheme,
      bool? applyElevationOverlayColor, BottomAppBarThemeData? bottomAppBarTheme, BottomSheetThemeData? bottomSheetTheme, Object? inputDecorationTheme, IconThemeData? primaryIconTheme, PageTransitionsTheme? pageTransitionsTheme, Color? indicatorColor, TextSelectionThemeData? textSelectionTheme, Object? tabBarTheme, Object? snackBarTheme, Object? tooltipTheme});
}

@JavaName('com.codename1.flutter.material.ColorScheme')
class ColorScheme {
  external ColorScheme({Brightness? brightness, Color? primary, Color? onPrimary,
      Color? primaryContainer, Color? onPrimaryContainer, Color? inversePrimary, Color? secondary,
      Color? onSecondary, Color? secondaryContainer, Color? onSecondaryContainer, Color? tertiary,
      Color? onTertiary, Color? tertiaryContainer, Color? onTertiaryContainer, Color? error,
      Color? onError, Color? errorContainer, Color? onErrorContainer, Color? surface, Color? onSurface,
      Color? surfaceVariant, Color? onSurfaceVariant, Color? background, Color? onBackground,
      Color? outline, Color? outlineVariant, Color? shadow, Color? scrim, Color? inverseSurface,
      Color? onInverseSurface});
  external static ColorScheme fromSeed({Color seedColor, Brightness? brightness});
  external static ColorScheme light({Color? primary, Color? onPrimary, Color? primaryContainer, Color? onPrimaryContainer, Color? secondary, Color? onSecondary, Color? secondaryContainer, Color? onSecondaryContainer, Color? tertiary, Color? onTertiary, Color? error, Color? onError, Color? errorContainer, Color? onErrorContainer, Color? surface, Color? onSurface, Color? surfaceVariant, Color? onSurfaceVariant, Color? background, Color? onBackground, Color? outline, Color? shadow, Color? inverseSurface, Color? onInverseSurface, Color? inversePrimary, Brightness? brightness});
  external static ColorScheme dark({Color? primary, Color? onPrimary, Color? primaryContainer, Color? onPrimaryContainer, Color? secondary, Color? onSecondary, Color? secondaryContainer, Color? onSecondaryContainer, Color? tertiary, Color? onTertiary, Color? error, Color? onError, Color? errorContainer, Color? onErrorContainer, Color? surface, Color? onSurface, Color? surfaceVariant, Color? onSurfaceVariant, Color? background, Color? onBackground, Color? outline, Color? shadow, Color? inverseSurface, Color? onInverseSurface, Color? inversePrimary, Brightness? brightness});
  external Brightness get brightness;
  external Color get primary;
  external Color get onPrimary;
  external Color get primaryContainer;
  external Color get onPrimaryContainer;
  external Color get inversePrimary;
  external Color get secondary;
  external Color get onSecondary;
  external Color get secondaryContainer;
  external Color get onSecondaryContainer;
  external Color get tertiary;
  external Color get onTertiary;
  external Color get tertiaryContainer;
  external Color get onTertiaryContainer;
  external Color get error;
  external Color get onError;
  external Color get errorContainer;
  external Color get onErrorContainer;
  external Color get surface;
  external Color get onSurface;
  external Color get surfaceVariant;
  external Color get onSurfaceVariant;
  external Color get background;
  external Color get onBackground;
  external Color get outline;
  external Color get outlineVariant;
  external Color get shadow;
  external Color get scrim;
  external Color get inverseSurface;
  external Color get onInverseSurface;
  external ColorScheme copyWith({Brightness? brightness, Color? primary, Color? onPrimary,
      Color? primaryContainer, Color? onPrimaryContainer, Color? secondary, Color? onSecondary,
      Color? secondaryContainer, Color? tertiary, Color? error, Color? onError, Color? surface,
      Color? onSurface, Color? surfaceVariant, Color? onSurfaceVariant, Color? background,
      Color? onBackground, Color? outline, Color? inversePrimary, Color? inverseSurface, Color? shadow});
}

@JavaName('com.codename1.flutter.material.TextTheme')
class TextTheme {
  external TextStyle get displayLarge;
  external TextStyle get displayMedium;
  external TextStyle get displaySmall;
  external TextStyle get headlineLarge;
  external TextStyle get headlineMedium;
  external TextStyle get headlineSmall;
  external TextStyle get titleLarge;
  external TextStyle get titleMedium;
  external TextStyle get titleSmall;
  external TextStyle get bodyLarge;
  external TextStyle get bodyMedium;
  external TextStyle get bodySmall;
  external TextStyle get labelLarge;
  external TextStyle get labelMedium;
  external TextStyle get labelSmall;
  external TextTheme copyWith({TextStyle? displayLarge, TextStyle? displayMedium, TextStyle? displaySmall,
      TextStyle? headlineLarge, TextStyle? headlineMedium, TextStyle? headlineSmall, TextStyle? titleLarge,
      TextStyle? titleMedium, TextStyle? titleSmall, TextStyle? bodyLarge, TextStyle? bodyMedium,
      TextStyle? bodySmall, TextStyle? labelLarge, TextStyle? labelMedium, TextStyle? labelSmall});
  external TextTheme apply({String? fontFamily, double? fontSizeFactor, double? fontSizeDelta,
      Color? displayColor, Color? bodyColor, dynamic decoration, dynamic decorationColor});
}

@JavaName('com.codename1.flutter.material.Theme')
abstract class Theme {
  external static ThemeData of(BuildContext context);
  external static Brightness brightnessOf(BuildContext context);
}

// --- M2 additions -------------------------------------------------------

@JavaName('com.codename1.flutter.BoxFit')
enum BoxFit { fill, contain, cover, fitWidth, fitHeight, none }

@JavaName('com.codename1.flutter.Alignment')
abstract class Alignment {
  external static Alignment get topLeft;
  external static Alignment get topCenter;
  external static Alignment get topRight;
  external static Alignment get centerLeft;
  external static Alignment get center;
  external static Alignment get centerRight;
  external static Alignment get bottomLeft;
  external static Alignment get bottomCenter;
  external static Alignment get bottomRight;
}

@JavaName('com.codename1.flutter.widgets.ListView')
class ListView extends Widget {
  external ListView({Key? key, List<Widget> children, EdgeInsets? padding, bool? shrinkWrap});
  external static ListView builder({Key? key, int? itemCount, IndexedWidgetBuilder itemBuilder, EdgeInsets? padding, bool? shrinkWrap, Object? physics, Object? scrollDirection, Object? controller, String? restorationId, bool? primary, double? itemExtent, bool? reverse});
  external static ListView separated({Key? key, bool? primary, int? itemCount, IndexedWidgetBuilder itemBuilder, IndexedWidgetBuilder separatorBuilder, EdgeInsets? padding, bool? shrinkWrap});
}

@JavaName('com.codename1.flutter.widgets.GridView')
class GridView extends Widget {
  external static GridView count({Key? key, String? restorationId, Object? physics, bool? primary, int crossAxisCount, double? childAspectRatio, double? mainAxisSpacing, double? crossAxisSpacing, EdgeInsets? padding, List<Widget> children});
  external static GridView builder({Key? key, int? itemCount, IndexedWidgetBuilder itemBuilder, Object? gridDelegate, EdgeInsets? padding, bool? shrinkWrap, Object? physics});
}

@JavaName('com.codename1.flutter.widgets.SingleChildScrollView')
class SingleChildScrollView extends Widget {
  external SingleChildScrollView({Key? key, EdgeInsets? padding, Axis? scrollDirection, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Image')
class Image extends Widget {
  external Image({Key? key, ImageProvider? image, double? width, double? height, BoxFit? fit, bool? excludeFromSemantics, Object? frameBuilder});
  external static Image asset(String name, {Key? key, double? width, double? height, BoxFit? fit, String? package, bool? excludeFromSemantics, bool? gaplessPlayback, int? cacheWidth, int? cacheHeight, Color? color, Object? colorBlendMode, Object? alignment, Object? semanticLabel});
  external static Image network(String src, {Key? key, double? width, double? height, BoxFit? fit});
}

@JavaName('com.codename1.flutter.widgets.Stack')
class Stack extends Widget {
  external Stack({Key? key, Alignment? alignment, List<Widget> children});
}

@JavaName('com.codename1.flutter.widgets.Positioned')
class Positioned extends Widget {
  external Positioned({Key? key, double? left, double? top, double? right, double? bottom, double? width, double? height, Widget child});
  external static Positioned fill({Key? key, double? left, double? top, double? right, double? bottom, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.Align')
class Align extends Widget {
  external Align({Key? key, Alignment? alignment, Widget? child});
}

@JavaName('com.codename1.flutter.widgets.ConstrainedBox')
class ConstrainedBox extends Widget {
  external ConstrainedBox({Key? key, BoxConstraints constraints, Widget? child});
}

@JavaName('com.codename1.flutter.rendering.BoxConstraints')
class BoxConstraints {
  external BoxConstraints({double? minWidth, double? maxWidth, double? minHeight, double? maxHeight});
  external double get minWidth;
  external double get maxWidth;
  external double get minHeight;
  external double get maxHeight;
  external bool get hasBoundedWidth;
  external bool get hasBoundedHeight;
  external Size get biggest;
  external Size get smallest;
}

@JavaName('com.codename1.flutter.material.Card')
class Card extends Widget {
  external Card({Key? key, Color? color, double? elevation, EdgeInsets? margin, Widget? child});
}

@JavaName('com.codename1.flutter.material.Divider')
class Divider extends Widget {
  external Divider({Key? key, double? height, double? thickness, Color? color});
}

@JavaName('com.codename1.flutter.material.ElevatedButton')
class ElevatedButton extends Widget {
  external ElevatedButton({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? child});
  external static ButtonStyle styleFrom({Color? foregroundColor, Color? backgroundColor,
      Color? shadowColor, double? elevation, TextStyle? textStyle, EdgeInsets? padding,
      dynamic side, dynamic shape, dynamic alignment, dynamic tapTargetSize, dynamic visualDensity});
  external static ElevatedButton icon({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? icon, Widget? label});
}

@JavaName('com.codename1.flutter.material.TextButton')
class TextButton extends Widget {
  external TextButton({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? child});
  external static ButtonStyle styleFrom({Color? foregroundColor, Color? backgroundColor,
      Color? shadowColor, double? elevation, TextStyle? textStyle, EdgeInsets? padding,
      dynamic side, dynamic shape, dynamic alignment, dynamic tapTargetSize, dynamic visualDensity});
  external static TextButton icon({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? icon, Widget? label});
}

@JavaName('com.codename1.flutter.material.FilledButton')
class FilledButton extends ElevatedButton {
  external FilledButton({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? child});
  external static FilledButton tonal({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? child});
  external static FilledButton icon({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? icon, Widget? label});
}

@JavaName('com.codename1.flutter.material.OutlinedButton')
class OutlinedButton extends Widget {
  external OutlinedButton({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? child});
  external static ButtonStyle styleFrom({Color? foregroundColor, Color? backgroundColor,
      Color? shadowColor, double? elevation, TextStyle? textStyle, EdgeInsets? padding,
      dynamic side, dynamic shape, dynamic alignment, dynamic tapTargetSize, dynamic visualDensity});
  external static OutlinedButton icon({Key? key, VoidCallback? onPressed, ButtonStyle? style, Widget? icon, Widget? label});
}

@JavaName('com.codename1.flutter.material.IconButton')
class IconButton extends Widget {
  external IconButton({Key? key, VoidCallback? onPressed, Widget? icon, double? iconSize, Color? color});
}

@JavaName('com.codename1.flutter.widgets.GestureDetector')
class GestureDetector extends Widget {
  external GestureDetector({Key? key, VoidCallback? onTap, VoidCallback? onLongPress, Widget? child});
}

@JavaName('com.codename1.flutter.material.InkWell')
class InkWell extends Widget {
  external InkWell({Key? key, VoidCallback? onTap, VoidCallback? onLongPress, Widget? child});
}

// --- M3 additions -------------------------------------------------------
// Callback typedefs below (StringCallback etc.) are transpiler-internal
// names mapped to dart.runtime.Funcs SAMs; they type untyped lambda params.

@JavaName('com.codename1.flutter.material.TextEditingController')
class TextEditingController {
  external TextEditingController({String? text});
  external String get text;
  // `controller.text = v` lowers to the overloaded Java accessor `text(String)` --
  // the same name as this Dart setter, the way every other stub get/set pair here
  // is routed -- so the notifying behaviour has to live on THAT method, not under a
  // differently named one. TextEditingController.text(String) delegates to setText,
  // which is why declaring this setter is enough to fix the assignment.
  external set text(String value);
  external void setText(String value);
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
  external void clear();
  // Releases the controller's listeners — Flutter's `ChangeNotifier.dispose`.
  external void dispose();
}

@JavaName('com.codename1.flutter.material.InputDecoration')
class InputDecoration {
  external InputDecoration({String? labelText, String? hintText});
  external static InputDecoration collapsed({String? hintText, dynamic hintStyle, dynamic border, bool? filled, Color? fillColor});
}

@JavaName('com.codename1.flutter.material.TextField')
class TextField extends Widget {
  external TextField({Key? key, TextEditingController? controller, InputDecoration? decoration, bool? obscureText, bool? enabled, StringCallback? onChanged, StringCallback? onSubmitted});
}

@JavaName('com.codename1.flutter.material.Checkbox')
class Checkbox extends Widget {
  external Checkbox({Key? key, bool? value, bool tristate, BoolCallback? onChanged});
}

@JavaName('com.codename1.flutter.material.Radio')
class Radio<T> extends Widget {
  external Radio({Key? key, Object value, Object? groupValue, DynamicCallback? onChanged});
}

@JavaName('com.codename1.flutter.material.Switch')
class Switch extends Widget {
  external Switch({Key? key, bool value, BoolCallback? onChanged});
}

@JavaName('com.codename1.flutter.material.Slider')
class Slider extends Widget {
  external Slider({Key? key, double value, double? min, double? max, int? divisions, DoubleCallback? onChanged});
}

@JavaName('com.codename1.flutter.navigation.MaterialPageRoute')
class MaterialPageRoute<T> extends Route<T> {
  external MaterialPageRoute({WidgetBuilder builder});
}

@JavaName('com.codename1.flutter.navigation.Navigator')
class Navigator extends Widget {
  // The nested-Navigator constructor parameters the Java class actually implements
  // (see Navigator.java's initialRoute/onGenerateRoute/onUnknownRoute/
  // restorationScopeId/observers/pages/onPopPage setters) -- a plain push/pop app
  // never constructs a Navigator directly and only reaches the statics below.
  external Navigator({Key? key, String? initialRoute,
      Object? Function(RouteSettings)? onGenerateRoute,
      Object? Function(RouteSettings)? onUnknownRoute,
      String? restorationScopeId, Object? observers, Object? pages, Object? onPopPage});
  // push completes with the value the route is popped with, as Flutter's does.
  external static Future<Object?> push(BuildContext context, MaterialPageRoute route);
  external static void pop(BuildContext context, [Object? result]);
  external static NavigatorState of(BuildContext context, {bool? rootNavigator});
  external static String restorablePush(BuildContext context, dynamic routeBuilder, {dynamic arguments});
  // Future<bool>, matching Flutter's own Navigator.maybePop(context, [result]) --
  // this used to answer a synchronous bool, so `await Navigator.maybePop(context)`
  // did not compile against the stub.
  external static Future<bool> maybePop(BuildContext context, [Object? result]);
}

@JavaName('com.codename1.flutter.material.Dialogs.showDialog')
external Future<dynamic> showDialog({BuildContext context, WidgetBuilder builder});

@JavaName('com.codename1.flutter.material.AlertDialog')
class AlertDialog extends Widget {
  external AlertDialog({Key? key, Widget? title, Widget? content, List<Widget>? actions});
}

@JavaName('com.codename1.flutter.material.SnackBar')
class SnackBar extends Widget {
  external SnackBar({Key? key, Widget content, Duration? duration});
}

@JavaName('com.codename1.flutter.material.ScaffoldMessenger')
abstract class ScaffoldMessenger {
  external static ScaffoldMessengerState of(BuildContext context);
}

@JavaName('com.codename1.flutter.material.ScaffoldMessengerState')
abstract class ScaffoldMessengerState {
  external void showSnackBar(SnackBar snackBar);
  // Dismisses the visible SnackBar immediately — Flutter's `hideCurrentSnackBar`.
  external void hideCurrentSnackBar({dynamic reason});
}

@JavaName('com.codename1.flutter.material.Drawer')
class Drawer extends Widget {
  external Drawer({Key? key, Widget? child});
}

@JavaName('com.codename1.flutter.material.BottomNavigationBarItem')
class BottomNavigationBarItem {
  external BottomNavigationBarItem({Widget? icon, Widget? activeIcon, String? label,
      Color? backgroundColor, String? tooltip});
  // The item's glyph and label — Flutter's `BottomNavigationBarItem.icon/label`.
  external Widget get icon;
  external String? get label;
}

@JavaName('com.codename1.flutter.material.BottomNavigationBar')
class BottomNavigationBar extends Widget {
  external BottomNavigationBar({Key? key, List<BottomNavigationBarItem> items, int? currentIndex, IntCallback? onTap});
}

@JavaName('com.codename1.flutter.material.ListTile')
class ListTile extends Widget {
  external ListTile({Key? key, Widget? leading, Widget? title, Widget? subtitle, Widget? trailing, VoidCallback? onTap});
}

// --- M4 additions -------------------------------------------------------

@JavaName('com.codename1.flutter.ThemeMode')
enum ThemeMode { system, light, dark }

@JavaName('com.codename1.flutter.Brightness')
enum Brightness { light, dark }

@JavaName('com.codename1.flutter.MediaQuery')
abstract class MediaQuery {
  external static MediaQueryData of(BuildContext context);
  // Targeted inherited-lookup helpers that depend only on one MediaQueryData
  // aspect — Flutter's `MediaQuery.sizeOf` / `paddingOf` / `viewInsetsOf`.
  external static Size sizeOf(BuildContext context);
  external static EdgeInsets paddingOf(BuildContext context);
  external static EdgeInsets viewInsetsOf(BuildContext context);
  external static Widget removePadding({BuildContext context, bool? removeLeft, bool? removeTop, bool? removeRight, bool? removeBottom, Widget? child});
}

@JavaName('com.codename1.flutter.MediaQueryData')
abstract class MediaQueryData {
  external Size get size;
  external double get devicePixelRatio;
  external Brightness get platformBrightness;
  external double get textScaleFactor;
  external EdgeInsets get padding;
  // The insets covered by system UI (keyboard, notches) — Flutter's
  // `MediaQueryData.viewInsets` / `viewPadding`.
  external EdgeInsets get viewInsets;
  external EdgeInsets get viewPadding;
  external MediaQueryData copyWith({Size? size, double? devicePixelRatio, double? textScaleFactor,
      EdgeInsets? padding, Brightness? platformBrightness});
  // Returns a copy with one or more padding edges removed — Flutter's
  // `MediaQueryData.removePadding`.
  external MediaQueryData removePadding({bool? removeLeft, bool? removeTop,
      bool? removeRight, bool? removeBottom});
  external MediaQueryData removeViewInsets({bool? removeLeft, bool? removeTop,
      bool? removeRight, bool? removeBottom});
}

@JavaName('com.codename1.flutter.rendering.Size')
class Size {
  external Size(double width, double height);
  external static Size fromRadius(double radius);
  external static Size fromHeight(double height);
  external static Size fromWidth(double width);
  external double get width;
  external double get height;
  external double get shortestSide;
  external double get longestSide;
  external double get aspectRatio;
  external bool get isEmpty;
  external Size get flipped;
  // The offset to the center of a rect of this size with the given origin —
  // Flutter's `Size.center(Offset origin)`.
  external Offset center(Offset origin);
  external Offset topLeft(Offset origin);
  external Offset topCenter(Offset origin);
  external Offset bottomCenter(Offset origin);
  external bool contains(Offset offset);
}

@JavaName('com.codename1.flutter.widgets.RichText')
class RichText extends Widget {
  external RichText({Key? key, TextSpan text, TextAlign? textAlign});
}

// The base of the styled-text tree — Flutter's `InlineSpan` (TextSpan's supertype).
@JavaName('com.codename1.flutter.widgets.InlineSpan')
class InlineSpan {
  external String toPlainText();
}

@JavaName('com.codename1.flutter.widgets.TextSpan')
class TextSpan extends InlineSpan {
  external TextSpan({String? text, TextStyle? style, List<TextSpan>? children});
  // Flattens this span tree to its raw text — Flutter's `InlineSpan.toPlainText`.
  external String toPlainText();
}

// Colours for text selection. Recorded rather than applied - selection is drawn with
// Codename One's own theme colours - but three of the four studies name one, so the type
// has to exist for their themes to transpile.
@JavaName('com.codename1.flutter.material.TextSelectionThemeData')
class TextSelectionThemeData {
  external TextSelectionThemeData({Color? cursorColor, Color? selectionColor, Color? selectionHandleColor});
}

// Per-platform page transition builders. Route transitions come from Codename One's own
// machinery; Rally configures one of these, so the type must resolve.
@JavaName('com.codename1.flutter.material.PageTransitionsTheme')
class PageTransitionsTheme {
  external PageTransitionsTheme({Object? builders});
}

// A route transition for one platform. Recorded, not run - Codename One drives its own
// transitions - but Rally builds a Map<TargetPlatform, PageTransitionsBuilder>, so the
// type must resolve for its theme to compile.
@JavaName('com.codename1.flutter.material.PageTransitionsBuilder')
class PageTransitionsBuilder {}
