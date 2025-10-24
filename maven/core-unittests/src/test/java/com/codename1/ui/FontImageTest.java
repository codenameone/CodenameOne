package com.codename1.ui;

import com.codename1.test.UITestBase;
import com.codename1.ui.plaf.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class FontImageTest extends UITestBase {
    @BeforeEach
    void resetMaterialFont() throws Exception {
        when(implementation.isTrueTypeSupported()).thenReturn(true);
        Field field = FontImage.class.getDeclaredField("materialDesignFont");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    void testCreateCopiesStyleState() {
        Font iconFont = Font.createTrueTypeFont("IconFont", "icon.ttf");
        Style style = new Style();
        style.setFont(iconFont);
        style.setBgTransparency((byte) 77);
        style.setBgColor(0x112233);
        style.setFgColor(0x445566);
        style.setOpacity(200);
        style.setFgAlpha(180);

        FontImage image = FontImage.create("A", style, iconFont);
        assertEquals(10, image.getWidth());
        assertEquals(10, image.getHeight());
        assertEquals(1, image.getPadding());

        assertEquals(0x445566, getPrivateInt(image, "color"));
        assertEquals(0x112233, getPrivateInt(image, "backgroundColor"));
        assertEquals(77, getPrivateInt(image, "backgroundOpacity"));
        assertEquals(200, getPrivateInt(image, "opacity"));
        assertEquals(180, getPrivateInt(image, "fgAlpha"));
        assertEquals("A", getPrivateString(image, "text"));
        assertSame(iconFont, image.getFont());
    }

    @Test
    void testSetPaddingAdjustsFontSize() {
        Font iconFont = Font.createTrueTypeFont("PaddingFont", "padding.ttf");
        Style style = new Style();
        style.setFont(iconFont);
        FontImage image = FontImage.create("A", style, iconFont);

        image.setPadding(3);
        assertEquals(3, image.getPadding());
        assertNotSame(iconFont, image.getFont());
        assertEquals(7f, image.getFont().getPixelSize(), 0.001f);
    }

    @Test
    void testGetMaterialDesignFontCachesValueWhenSupported() {
        Font first = FontImage.getMaterialDesignFont();
        Font second = FontImage.getMaterialDesignFont();
        assertSame(first, second);
        assertTrue(first.isTTFNativeFont());
    }

    @Test
    void testGetMaterialDesignFontFallsBackWhenNotSupported() throws Exception {
        when(implementation.isTrueTypeSupported()).thenReturn(false);
        Field field = FontImage.class.getDeclaredField("materialDesignFont");
        field.setAccessible(true);
        field.set(null, null);
        Font font = FontImage.getMaterialDesignFont();
        assertSame(Font.getDefaultFont(), font);
    }

    @Test
    void testSetIconOnSelectableIconHolderCreatesStateIcons() {
        Font iconFont = Font.createTrueTypeFont("ButtonFont", "button.ttf");
        Button button = new Button("Action");
        button.getUnselectedStyle().setFgColor(0x111111);
        button.getSelectedStyle().setFgColor(0x222222);
        button.getPressedStyle().setFgColor(0x333333);
        button.getDisabledStyle().setFgColor(0x444444);

        char[] icons = new char[]{'a', 'b', 'c', 'd', 'e'};
        FontImage.setIcon(button, iconFont, icons, 4f);

        assertTrue(button.getIcon() instanceof FontImage);
        assertTrue(button.getPressedIcon() instanceof FontImage);
        assertTrue(button.getDisabledIcon() instanceof FontImage);
        assertTrue(button.getRolloverPressedIcon() instanceof FontImage);

        assertEquals("a", getPrivateString(button.getIcon(), "text"));
        assertEquals("c", getPrivateString(button.getPressedIcon(), "text"));
        assertEquals("e", getPrivateString(button.getDisabledIcon(), "text"));
    }

    private int getPrivateInt(Object target, String name) {
        try {
            Field field = FontImage.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private String getPrivateString(Object target, String name) {
        try {
            Field field = FontImage.class.getDeclaredField(name);
            field.setAccessible(true);
            return (String) field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
