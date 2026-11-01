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
