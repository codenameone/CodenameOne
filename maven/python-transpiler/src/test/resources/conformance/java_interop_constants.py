# A Java class with thousands of constants (FontImage's icon set) imported
# whole. CPython cannot import java.*, so java_interop_constants.out is
# written by hand -- do not regenerate it.
from java.com.codename1.ui import FontImage

print(hex(ord(FontImage.MATERIAL_10K)))
print(hex(ord(FontImage.MATERIAL_ADD)))
print(hex(ord(FontImage.MATERIAL_REFRESH)))
print(hex(ord(FontImage.MATERIAL_SWITCH_ACCESS_SHORTCUT_ADD)))
try:
    FontImage.MATERIAL_NO_SUCH_ICON
except AttributeError:
    print("AttributeError")
