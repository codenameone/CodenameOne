// cn1:package emoji_picker_flutter
// package:emoji_picker_flutter, drawn with Codename One components from a
// built-in emoji set. Config is accepted; recents persist in Preferences.

@JavaName('com.codename1.flutter.plugins.emojipicker.Category')
enum Category { RECENT, SMILEYS, ANIMALS, FOODS, ACTIVITIES, TRAVEL, OBJECTS, SYMBOLS, FLAGS }

@JavaName('com.codename1.flutter.plugins.emojipicker.Emoji')
class Emoji {
  external Emoji(String emoji, String name, {bool hasSkinTone});
  external String get emoji;
  external String get name;
  external bool get hasSkinTone;
}

@JavaName('com.codename1.flutter.plugins.emojipicker.CategoryEmoji')
class CategoryEmoji {
  external CategoryEmoji(Category category, List<Emoji> emoji);
  external Category get category;
  external List<Emoji> get emoji;
}

@JavaName('com.codename1.flutter.plugins.emojipicker.Config')
class Config {
  external Config({double height, bool checkPlatformCompatibility, dynamic emojiViewConfig,
      dynamic skinToneConfig, dynamic categoryViewConfig, dynamic bottomActionBarConfig,
      dynamic searchViewConfig, dynamic emojiSet, dynamic locale, dynamic emojiTextStyle,
      dynamic customBackspaceIcon, dynamic customSearchIcon, dynamic viewOrderConfig});
}

@JavaName('com.codename1.flutter.plugins.emojipicker.EmojiPickerUtils')
class EmojiPickerUtils {
  external EmojiPickerUtils();
  external Future<List<Emoji>> getRecentEmojis();
  external Future<List<Emoji>> searchEmoji(String keyword, List<CategoryEmoji> data);
  external Future<void> clearRecentEmojis();
}

@JavaName('com.codename1.flutter.plugins.emojipicker.EmojiPicker')
class EmojiPicker extends Widget {
  external EmojiPicker({Key? key, void Function(Category? category, Emoji emoji)? onEmojiSelected,
      void Function()? onBackspacePressed, TextEditingController? textEditingController,
      dynamic scrollController, Config config});
  external static List<CategoryEmoji> get defaultEmojiSet;
}

// emoji_picker_flutter 4.x view configs. Config accepts them and does not apply them yet,
// so each is a plain value holder: the app compiles, the picker keeps its own look.
@JavaName('com.codename1.flutter.plugins.emojipicker.EmojiViewConfig')
class EmojiViewConfig {
  external const EmojiViewConfig({Object? columns, Object? emojiSizeMax, Object? backgroundColor, Object? verticalSpacing, Object? horizontalSpacing, Object? gridPadding, Object? recentsLimit, Object? noRecents, Object? buttonMode, Object? loadingIndicator, Object? replaceEmojiOnLimitExceed});
}

@JavaName('com.codename1.flutter.plugins.emojipicker.SearchViewConfig')
class SearchViewConfig {
  external const SearchViewConfig({Object? backgroundColor, Object? buttonIconColor, Object? hintText, Object? customSearchView});
}

@JavaName('com.codename1.flutter.plugins.emojipicker.CategoryViewConfig')
class CategoryViewConfig {
  external const CategoryViewConfig({Object? initCategory, Object? backgroundColor, Object? indicatorColor, Object? iconColor, Object? iconColorSelected, Object? backspaceColor, Object? tabBarHeight, Object? tabIndicatorAnimDuration, Object? showBackspaceButton, Object? recentTabBehavior, Object? categoryIcons, Object? dividerColor, Object? extraTab, Object? customCategoryView});
}

@JavaName('com.codename1.flutter.plugins.emojipicker.BottomActionBarConfig')
class BottomActionBarConfig {
  external const BottomActionBarConfig({Object? enabled, Object? showBackspaceButton, Object? showSearchViewButton, Object? backgroundColor, Object? buttonColor, Object? buttonIconColor, Object? customBottomActionBar});
}

@JavaName('com.codename1.flutter.plugins.emojipicker.ViewOrderConfig')
class ViewOrderConfig {
  external const ViewOrderConfig({Object? top, Object? middle, Object? bottom});
}

@JavaName('com.codename1.flutter.plugins.emojipicker.SkinToneConfig')
class SkinToneConfig {
  external const SkinToneConfig({Object? enabled, Object? dialogBackgroundColor, Object? indicatorColor});
}
