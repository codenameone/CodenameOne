package com.codename1.ui.util.xml;

import com.codename1.ui.util.xml.comps.ArrayEntry;
import com.codename1.ui.util.xml.comps.CommandEntry;
import com.codename1.ui.util.xml.comps.ComponentEntry;
import com.codename1.ui.util.xml.comps.Custom;
import com.codename1.ui.util.xml.comps.LayoutConstraint;
import com.codename1.ui.util.xml.comps.MapItems;
import com.codename1.ui.util.xml.comps.StringEntry;
import java.io.File;
import java.io.FileWriter;
import org.junit.Test;

import static org.junit.Assert.*;

public class SimpleXmlParserTest {

    @Test
    public void parseResourceFileXml() throws Exception {
        File xml = File.createTempFile("resource", ".xml");
        xml.deleteOnExit();
        try (FileWriter out = new FileWriter(xml)) {
            out.write("<resource majorVersion=\"1\" minorVersion=\"6\" useXmlUI=\"true\">\n");
            out.write("  <theme name=\"MainTheme\">\n");
            out.write("    <val key=\"Button.bgColor\" value=\"ff00ff\"/>\n");
            out.write("    <gradient key=\"Gradient.bg\" color1=\"111\" color2=\"222\" posX=\"0.2\" posY=\"0.3\" radius=\"0.9\"/>\n");
            out.write("    <font key=\"Label.font\" type=\"system\" name=\"native:Main\" face=\"1\" style=\"2\" size=\"3\" family=\"native:MainRegular\" sizeSettings=\"1\" actualSize=\"11.5\"/>\n");
            out.write("    <border key=\"Label.border\" type=\"round\" roundBorderColor=\"1234\" strokeOpacity=\"111\" strokeThickness=\"1.25\" shadowMM=\"true\" bezierCorners=\"true\"/>\n");
            out.write("  </theme>\n");
            out.write("  <ui name=\"MyForm\"/>\n");
            out.write("  <legacyFont name=\"bitmap.fnt\"/>\n");
            out.write("  <data name=\"payload.dat\"/>\n");
            out.write("  <image name=\"logo.png\"/>\n");
            out.write("  <image name=\"icon.svg\" type=\"svg\"/>\n");
            out.write("  <l10n name=\"Strings\">\n");
            out.write("    <lang name=\"en\">\n");
            out.write("      <entry key=\"hello\" value=\"Hello\"/>\n");
            out.write("    </lang>\n");
            out.write("  </l10n>\n");
            out.write("</resource>\n");
        }

        ResourceFileXML parsed = SimpleXmlParser.parse(xml, ResourceFileXML.class);
        assertEquals(1, parsed.getMajorVersion());
        assertEquals(6, parsed.getMinorVersion());
        assertTrue(parsed.isUseXmlUI());

        assertNotNull(parsed.getTheme());
        assertEquals("MainTheme", parsed.getTheme()[0].getName());
        assertEquals("Button.bgColor", parsed.getTheme()[0].getVal()[0].getKey());
        assertEquals("ff00ff", parsed.getTheme()[0].getVal()[0].getValue());

        assertEquals(Integer.valueOf(111), parsed.getTheme()[0].getGradient()[0].getColor1());
        assertEquals(Integer.valueOf(222), parsed.getTheme()[0].getGradient()[0].getColor2());
        assertEquals(Float.valueOf(0.2f), parsed.getTheme()[0].getGradient()[0].getPosX());

        Font font = parsed.getTheme()[0].getFont()[0];
        assertEquals("system", font.getType());
        assertEquals(Integer.valueOf(1), font.getFace());
        assertEquals(Float.valueOf(11.5f), font.getActualSize());

        Border border = parsed.getTheme()[0].getBorder()[0];
        assertEquals(1234, border.getRoundBorderColor());

        assertEquals("MyForm", parsed.getUi()[0].getName());
        assertEquals("bitmap.fnt", parsed.getLegacyFont()[0].getName());
        assertEquals("payload.dat", parsed.getData()[0].getName());
        assertEquals("logo.png", parsed.getImage()[0].getName());
        assertEquals("svg", parsed.getImage()[1].getType());

        assertEquals("Strings", parsed.getL10n()[0].getName());
        assertEquals("en", parsed.getL10n()[0].getLang()[0].getName());
        assertEquals("hello", parsed.getL10n()[0].getLang()[0].getEntry()[0].getKey());
        assertEquals("Hello", parsed.getL10n()[0].getLang()[0].getEntry()[0].getValue());
    }

    @Test
    public void parseComponentEntryXml() throws Exception {
        File xml = File.createTempFile("component", ".xml");
        xml.deleteOnExit();
        try (FileWriter out = new FileWriter(xml)) {
            out.write("<component name=\"Main\" type=\"Container\" layout=\"FlowLayout\" focusable=\"true\" enabled=\"false\" columns=\"3\" rows=\"2\" tabPlacement=\"1\">\n");
            out.write("  <layoutConstraint row=\"2\" column=\"3\" width=\"10\" height=\"20\" align=\"4\" valign=\"5\" spanHorizontal=\"6\" spanVertical=\"7\"/>\n");
            out.write("  <stringItem>Title</stringItem>\n");
            out.write("  <mapItems>\n");
            out.write("    <stringItem key=\"title\" value=\"Hello\"/>\n");
            out.write("    <imageItem key=\"icon\" value=\"logo.png\"/>\n");
            out.write("    <actionItem key=\"tap\" value=\"doTap\"/>\n");
            out.write("  </mapItems>\n");
            out.write("  <custom name=\"items\" type=\"String[]\" dimensions=\"1\" value=\"direct\" selectedRenderer=\"SelR\" unselectedRenderer=\"UnselR\" selectedRendererEven=\"SelEven\" unselectedRendererEven=\"UnselEven\">\n");
            out.write("    <str>customStr</str>\n");
            out.write("    <arr>\n");
            out.write("      <value>row1</value>\n");
            out.write("      <value>row2</value>\n");
            out.write("    </arr>\n");
            out.write("    <stringItem>si1</stringItem>\n");
            out.write("    <mapItems><stringItem key=\"k\" value=\"v\"/></mapItems>\n");
            out.write("  </custom>\n");
            out.write("  <command name=\"Back\" id=\"9\" action=\"backAction\" argument=\"arg1\" backCommand=\"true\"/>\n");
            out.write("  <component name=\"ChildLabel\" type=\"Label\" text=\"Text\"/>\n");
            out.write("</component>\n");
        }

        ComponentEntry parsed = SimpleXmlParser.parse(xml, ComponentEntry.class);
        assertEquals("Main", parsed.getName());
        assertEquals("Container", parsed.getType());
        assertEquals("FlowLayout", parsed.getLayout());
        assertTrue(parsed.isFocusable());
        assertFalse(parsed.isEnabled());
        assertEquals(Integer.valueOf(3), parsed.getColumns());
        assertEquals(Integer.valueOf(2), parsed.getRows());
        assertEquals(Integer.valueOf(1), parsed.getTabPlacement());

        LayoutConstraint constraint = parsed.getLayoutConstraint();
        assertEquals(2, constraint.getRow());
        assertEquals(3, constraint.getColumn());
        assertEquals(10, constraint.getWidth());
        assertEquals(20, constraint.getHeight());
        assertEquals(4, constraint.getAlign());
        assertEquals(5, constraint.getValign());
        assertEquals(6, constraint.getSpanHorizontal());
        assertEquals(7, constraint.getSpanVertical());

        assertEquals("Title", parsed.getStringItem()[0].getValue());

        MapItems mapItems = parsed.getMapItems()[0];
        assertEquals("title", mapItems.getStringItem()[0].getKey());
        assertEquals("Hello", mapItems.getStringItem()[0].getValue());
        assertEquals("icon", mapItems.getImageItem()[0].getKey());
        assertEquals("tap", mapItems.getActionItem()[0].getKey());

        Custom custom = parsed.getCustom()[0];
        assertEquals("items", custom.getName());
        assertEquals("String[]", custom.getType());
        assertEquals(1, custom.getDimensions());
        assertNotNull(custom.getValue());
        assertEquals("customStr", custom.getStr()[0].getValue());
        assertEquals("row1", custom.getArr()[0].getValue()[0].getValue());
        assertEquals("row2", custom.getArr()[0].getValue()[1].getValue());
        assertEquals("si1", custom.getStringItem()[0].getValue());
        assertEquals("k", custom.getMapItems()[0].getStringItem()[0].getKey());
        assertEquals("SelR", custom.getSelectedRenderer());
        assertEquals("UnselR", custom.getUnselectedRenderer());
        assertEquals("SelEven", custom.getSelectedRendererEven());
        assertEquals("UnselEven", custom.getUnselectedRendererEven());

        CommandEntry command = parsed.getCommand()[0];
        assertEquals("Back", command.getName());
        assertEquals(9, command.getId());
        assertEquals("backAction", command.getAction());
        assertEquals("arg1", command.getArgument());
        assertTrue(command.isBackCommand());

        assertEquals(1, parsed.getComponent().length);
        assertEquals("ChildLabel", parsed.getComponent()[0].getName());
        assertEquals("Label", parsed.getComponent()[0].getType());
        assertEquals("Text", parsed.getComponent()[0].getText());
    }
}
