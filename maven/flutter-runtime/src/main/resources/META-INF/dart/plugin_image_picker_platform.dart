// package:image_picker_platform_interface and image_picker_android. One
// implementation serves every target; useAndroidPhotoPicker is accepted and has no
// effect, because Codename One's gallery already opens the system picker.

@JavaName('com.codename1.flutter.plugins.imagepicker.ImagePickerPlatform')
abstract class ImagePickerPlatform {
  external static ImagePickerPlatform get instance;
  external static set instance(ImagePickerPlatform instance);
  external Future<XFile?> getImageFromSource({required ImageSource source, Object? options});
  external Future<Object?> retrieveLostData();
}

@JavaName('com.codename1.flutter.plugins.imagepicker.ImagePickerAndroid')
class ImagePickerAndroid extends ImagePickerPlatform {
  external ImagePickerAndroid();
  external static void registerWith();
  external bool get useAndroidPhotoPicker;
  external set useAndroidPhotoPicker(bool value);
}
