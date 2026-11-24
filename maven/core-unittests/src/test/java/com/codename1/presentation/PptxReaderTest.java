package com.codename1.presentation;

import com.codename1.junit.UITestBase;
import com.codename1.wordprocessing.Paragraph;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The PowerPoint reader resolves masters and layouts into slides and survives hostile files.
class PptxReaderTest extends UITestBase {
    private static final String NS = " xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\""
            + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\""
            + " xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\""
            + " xmlns:c=\"http://schemas.openxmlformats.org/drawingml/2006/chart\"";
    private static final String R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/";

    private static String pt(double points) {
        return String.valueOf((long) (points * 12700));
    }

    private static String xfrm(double x, double y, double w, double h) {
        return "<a:xfrm><a:off x=\"" + pt(x) + "\" y=\"" + pt(y) + "\"/><a:ext cx=\"" + pt(w)
                + "\" cy=\"" + pt(h) + "\"/></a:xfrm>";
    }

    private static String rels(String... idTypeTarget) {
        StringBuilder sb = new StringBuilder("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 0; i < idTypeTarget.length; i += 3) {
            sb.append("<Relationship Id=\"").append(idTypeTarget[i]).append("\" Type=\"")
                    .append(R).append(idTypeTarget[i + 1]).append("\" Target=\"")
                    .append(idTypeTarget[i + 2]).append("\"/>");
        }
        return sb.append("</Relationships>").toString();
    }

    private static byte[] zip(String... nameContent) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ZipOutputStream z = new ZipOutputStream(bo);
        for (int i = 0; i < nameContent.length; i += 2) {
            z.putNextEntry(new ZipEntry(nameContent[i]));
            z.write(nameContent[i + 1].getBytes("UTF-8"));
            z.closeEntry();
        }
        z.close();
        return bo.toByteArray();
    }

    private static Presentation read(byte[] b) throws IOException {
        return PptxReader.read(new ByteArrayInputStream(b));
    }

    private static final String THEME = "<a:theme" + NS + " name=\"Office\"><a:themeElements>"
            + "<a:clrScheme name=\"x\"><a:dk1><a:sysClr val=\"windowText\" lastClr=\"000000\"/></a:dk1>"
            + "<a:lt1><a:srgbClr val=\"FFFFFF\"/></a:lt1><a:dk2><a:srgbClr val=\"44546A\"/></a:dk2>"
            + "<a:lt2><a:srgbClr val=\"E7E6E6\"/></a:lt2><a:accent1><a:srgbClr val=\"FF0000\"/></a:accent1>"
            + "</a:clrScheme><a:fontScheme name=\"f\"><a:majorFont><a:latin typeface=\"Arial\"/></a:majorFont>"
            + "<a:minorFont><a:latin typeface=\"Calibri\"/></a:minorFont></a:fontScheme></a:themeElements></a:theme>";

    private static final String MASTER = "<p:sldMaster" + NS + "><p:cSld><p:bg><p:bgPr><a:solidFill>"
            + "<a:srgbClr val=\"112233\"/></a:solidFill></p:bgPr></p:bg><p:spTree>"
            + "<p:nvGrpSpPr/><p:grpSpPr/>"
            + "<p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"Title\"/><p:cNvSpPr/><p:nvPr><p:ph type=\"title\"/></p:nvPr></p:nvSpPr>"
            + "<p:spPr>" + xfrm(1, 1, 2, 2) + "</p:spPr></p:sp>"
            + "<p:sp><p:nvSpPr><p:cNvPr id=\"9\" name=\"Logo\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>"
            + "<p:spPr>" + xfrm(900, 500, 40, 30) + "<a:prstGeom prst=\"roundRect\"><a:avLst><a:gd name=\"adj\" fmla=\"val 50000\"/></a:avLst></a:prstGeom>"
            + "<a:solidFill><a:schemeClr val=\"accent1\"><a:lumMod val=\"50000\"/></a:schemeClr></a:solidFill></p:spPr></p:sp>"
            + "</p:spTree></p:cSld><p:txStyles><p:titleStyle><a:lvl1pPr algn=\"ctr\"><a:defRPr sz=\"4400\">"
            + "<a:solidFill><a:schemeClr val=\"tx1\"/></a:solidFill><a:latin typeface=\"+mj-lt\"/></a:defRPr></a:lvl1pPr></p:titleStyle>"
            + "<p:bodyStyle><a:lvl1pPr marL=\"342900\"><a:buChar char=\"x\"/><a:defRPr sz=\"2800\"/></a:lvl1pPr></p:bodyStyle>"
            + "</p:txStyles></p:sldMaster>";

    private static final String LAYOUT = "<p:sldLayout" + NS + "><p:cSld name=\"Title Only\"><p:spTree>"
            + "<p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"Title\"/><p:cNvSpPr/><p:nvPr><p:ph type=\"title\"/></p:nvPr></p:nvSpPr>"
            + "<p:spPr>" + xfrm(10, 20, 300, 50) + "</p:spPr><p:txBody><a:bodyPr anchor=\"b\"><a:normAutofit/></a:bodyPr></p:txBody></p:sp>"
            + "<p:sp><p:nvSpPr><p:cNvPr id=\"3\" name=\"Body\"/><p:cNvSpPr/><p:nvPr><p:ph idx=\"1\"/></p:nvPr></p:nvSpPr>"
            + "<p:spPr>" + xfrm(10, 100, 500, 300) + "</p:spPr></p:sp>"
            + "</p:spTree></p:cSld></p:sldLayout>";

    private static final String SLIDE1 = "<p:sld" + NS + "><p:cSld><p:spTree><p:nvGrpSpPr/><p:grpSpPr/>"
            + "<p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"Title 1\"/><p:cNvSpPr/><p:nvPr><p:ph type=\"title\"/></p:nvPr></p:nvSpPr>"
            + "<p:spPr/><p:txBody><a:bodyPr/><a:p><a:r><a:rPr lang=\"en\" b=\"1\"/><a:t>Hello</a:t></a:r>"
            + "<a:r><a:rPr><a:hlinkClick r:id=\"rIdL\"/></a:rPr><a:t> link</a:t></a:r></a:p></p:txBody></p:sp>"
            + "<p:sp><p:nvSpPr><p:cNvPr id=\"3\" name=\"Body\"/><p:cNvSpPr/><p:nvPr><p:ph idx=\"1\"/></p:nvPr></p:nvSpPr>"
            + "<p:spPr/><p:txBody><a:bodyPr/><a:p><a:r><a:t>Point</a:t></a:r></a:p></p:txBody></p:sp>"
            + "<p:pic><p:nvPicPr><p:cNvPr id=\"4\" name=\"Pic\"/><p:cNvPicPr/><p:nvPr/></p:nvPicPr>"
            + "<p:blipFill><a:blip r:embed=\"rIdI\"/><a:srcRect l=\"10000\" b=\"25000\"/></p:blipFill>"
            + "<p:spPr>" + xfrm(0, 0, 100, 100) + "</p:spPr></p:pic>"
            + "<p:graphicFrame><p:nvGraphicFramePr><p:cNvPr id=\"5\" name=\"Table\"/><p:cNvGraphicFramePr/><p:nvPr/></p:nvGraphicFramePr>"
            + "<p:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + pt(200) + "\" cy=\"" + pt(60) + "\"/></p:xfrm>"
            + "<a:graphic><a:graphicData uri=\"t\"><a:tbl><a:tblPr firstRow=\"1\"/><a:tblGrid><a:gridCol w=\"" + pt(100)
            + "\"/><a:gridCol w=\"" + pt(100) + "\"/></a:tblGrid>"
            + "<a:tr h=\"1\"><a:tc gridSpan=\"2\"><a:txBody><a:bodyPr/><a:p><a:r><a:t>Head</a:t></a:r></a:p></a:txBody></a:tc>"
            + "<a:tc hMerge=\"1\"><a:txBody><a:bodyPr/><a:p/></a:txBody></a:tc></a:tr>"
            + "<a:tr h=\"1\"><a:tc><a:txBody><a:bodyPr/><a:p><a:r><a:t>A</a:t></a:r></a:p></a:txBody></a:tc>"
            + "<a:tc><a:txBody><a:bodyPr/><a:p><a:r><a:t>B</a:t></a:r></a:p></a:txBody></a:tc></a:tr>"
            + "</a:tbl></a:graphicData></a:graphic></p:graphicFrame>"
            + "<p:grpSp><p:nvGrpSpPr><p:cNvPr id=\"6\" name=\"Group\"/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>"
            + "<p:grpSpPr><a:xfrm><a:off x=\"" + pt(100) + "\" y=\"" + pt(100) + "\"/><a:ext cx=\"" + pt(200)
            + "\" cy=\"" + pt(200) + "\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"" + pt(100) + "\" cy=\"" + pt(100)
            + "\"/></a:xfrm></p:grpSpPr>"
            + "<p:cxnSp><p:nvCxnSpPr><p:cNvPr id=\"7\" name=\"Line\"/><p:cNvCxnSpPr/><p:nvPr/></p:nvCxnSpPr>"
            + "<p:spPr>" + xfrm(50, 50, 10, 10) + "<a:prstGeom prst=\"line\"><a:avLst/></a:prstGeom>"
            + "<a:ln w=\"25400\"><a:solidFill><a:srgbClr val=\"00FF00\"/></a:solidFill><a:prstDash val=\"dash\"/>"
            + "<a:tailEnd type=\"triangle\"/></a:ln></p:spPr></p:cxnSp></p:grpSp>"
            + "<p:graphicFrame><p:nvGraphicFramePr><p:cNvPr id=\"8\" name=\"Chart\"/><p:cNvGraphicFramePr/><p:nvPr/></p:nvGraphicFramePr>"
            + "<p:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"" + pt(300) + "\" cy=\"" + pt(200) + "\"/></p:xfrm>"
            + "<a:graphic><a:graphicData uri=\"c\"><c:chart r:id=\"rIdC\"/></a:graphicData></a:graphic></p:graphicFrame>"
            + "</p:spTree></p:cSld>"
            + "<mc:AlternateContent xmlns:mc=\"http://schemas.openxmlformats.org/markup-compatibility/2006\">"
            + "<mc:Choice Requires=\"p14\"><p:transition spd=\"slow\" p14:dur=\"1200\" advTm=\"3000\"><p:push dir=\"u\"/></p:transition></mc:Choice>"
            + "<mc:Fallback><p:transition spd=\"slow\" advTm=\"3000\"><p:fade/></p:transition></mc:Fallback></mc:AlternateContent>"
            + "<p:timing><p:tnLst><p:par><p:cTn id=\"1\" dur=\"indefinite\" nodeType=\"tmRoot\"><p:childTnLst><p:seq><p:cTn id=\"2\">"
            + "<p:childTnLst><p:par><p:cTn id=\"3\" presetID=\"10\" presetClass=\"entr\" nodeType=\"clickEffect\">"
            + "<p:childTnLst><p:animEffect><p:cBhvr><p:cTn id=\"4\" dur=\"700\"/><p:tgtEl><p:spTgt spid=\"3\"/></p:tgtEl>"
            + "</p:cBhvr></p:animEffect></p:childTnLst></p:cTn></p:par></p:childTnLst></p:cTn></p:seq></p:childTnLst>"
            + "</p:cTn></p:par></p:tnLst></p:timing></p:sld>";

    private static final String CHART = "<c:chartSpace" + NS + "><c:chart><c:title><c:tx><c:rich><a:p><a:r><a:t>Sales</a:t></a:r></a:p></c:rich></c:tx></c:title>"
            + "<c:plotArea><c:barChart><c:barDir val=\"col\"/><c:grouping val=\"stacked\"/>"
            + "<c:ser><c:idx val=\"0\"/><c:tx><c:strRef><c:strCache><c:pt idx=\"0\"><c:v>2024</c:v></c:pt></c:strCache></c:strRef></c:tx>"
            + "<c:spPr><a:solidFill><a:srgbClr val=\"0000FF\"/></a:solidFill></c:spPr>"
            + "<c:cat><c:strRef><c:strCache><c:ptCount val=\"2\"/><c:pt idx=\"0\"><c:v>Q1</c:v></c:pt><c:pt idx=\"1\"><c:v>Q2</c:v></c:pt></c:strCache></c:strRef></c:cat>"
            + "<c:val><c:numRef><c:numCache><c:ptCount val=\"2\"/><c:pt idx=\"0\"><c:v>3.5</c:v></c:pt><c:pt idx=\"1\"><c:v>NaN</c:v></c:pt></c:numCache></c:numRef></c:val>"
            + "</c:ser></c:barChart></c:plotArea><c:legend/></c:chart></c:chartSpace>";

    private static final String NOTES = "<p:notes" + NS + "><p:cSld><p:spTree><p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"n\"/><p:cNvSpPr/>"
            + "<p:nvPr><p:ph type=\"body\" idx=\"1\"/></p:nvPr></p:nvSpPr><p:spPr/><p:txBody><a:bodyPr/><a:p><a:r><a:t>Say hi</a:t></a:r></a:p>"
            + "</p:txBody></p:sp></p:spTree></p:cSld></p:notes>";

    private static final String SLIDE2 = "<p:sld" + NS + " show=\"0\"><p:cSld><p:spTree>"
            + "<p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"Bad\"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>"
            + "<p:spPr><a:xfrm rot=\"abc\"><a:off x=\"NaN\" y=\"1e999\"/><a:ext cx=\"-5\" cy=\"Infinity\"/></a:xfrm>"
            + "<a:custGeom/></p:spPr><p:txBody><a:bodyPr lIns=\"zz\"/><a:p><a:r><a:rPr sz=\"-99\"/><a:t>x</a:t></a:r></a:p></p:txBody></p:sp>"
            + "</p:spTree></p:cSld></p:sld>";

    private static byte[] deck() throws IOException {
        return zip("[Content_Types].xml", "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"/>",
                "_rels/.rels", rels("rId1", "officeDocument", "ppt/presentation.xml",
                        "rId2", "../package/2006/relationships/metadata/core-properties", "docProps/core.xml"),
                "docProps/core.xml", "<cp:coreProperties xmlns:cp=\"x\" xmlns:dc=\"y\"><dc:title>Deck</dc:title></cp:coreProperties>",
                "ppt/presentation.xml", "<p:presentation" + NS + "><p:sldIdLst><p:sldId id=\"256\" r:id=\"rId2\"/>"
                        + "<p:sldId id=\"257\" r:id=\"rId3\"/><p:sldId id=\"258\" r:id=\"rIdMissing\"/></p:sldIdLst>"
                        + "<p:sldSz cx=\"12192000\" cy=\"6858000\"/></p:presentation>",
                "ppt/_rels/presentation.xml.rels", rels("rId1", "slideMaster", "slideMasters/slideMaster1.xml",
                        "rId2", "slide", "slides/slide1.xml", "rId3", "slide", "slides/slide2.xml",
                        "rId4", "theme", "theme/theme1.xml"),
                "ppt/theme/theme1.xml", THEME,
                "ppt/slideMasters/slideMaster1.xml", MASTER,
                "ppt/slideMasters/_rels/slideMaster1.xml.rels", rels("rId1", "theme", "../theme/theme1.xml"),
                "ppt/slideLayouts/slideLayout1.xml", LAYOUT,
                "ppt/slideLayouts/_rels/slideLayout1.xml.rels", rels("rId1", "slideMaster", "../slideMasters/slideMaster1.xml"),
                "ppt/slides/slide1.xml", SLIDE1,
                "ppt/slides/_rels/slide1.xml.rels", rels("rId1", "slideLayout", "../slideLayouts/slideLayout1.xml",
                        "rIdI", "image", "../media/image1.png", "rIdC", "chart", "../charts/chart1.xml",
                        "rIdN", "notesSlide", "../notesSlides/notesSlide1.xml")
                        .replace("</Relationships>", "<Relationship Id=\"rIdL\" Type=\"" + R
                                + "hyperlink\" Target=\"https://example.com\" TargetMode=\"External\"/></Relationships>"),
                "ppt/media/image1.png", "PNGDATA",
                "ppt/charts/chart1.xml", CHART,
                "ppt/notesSlides/notesSlide1.xml", NOTES,
                "ppt/slides/slide2.xml", SLIDE2,
                "ppt/slides/_rels/slide2.xml.rels", rels("rId1", "slideLayout", "../slideLayouts/missing.xml",
                        "rId2", "image", "../../../../etc/passwd"));
    }

    @Test
    void readsResolvedSlides() throws IOException {
        Presentation p = read(deck());
        assertEquals(960, p.width, 0.01);
        assertEquals(540, p.height, 0.01);
        assertEquals("Deck", p.title);
        assertEquals("Arial", p.theme.majorFont);
        assertEquals(2, p.slides.size());
        Slide s = p.slides.get(0);
        assertEquals("s1", s.id);
        assertEquals("Title Only", s.layout);
        assertEquals(Fill.SOLID, s.background.kind);
        assertEquals(0x112233, s.background.color);

        ShapeElement logo = (ShapeElement) s.elements.get(0);
        assertEquals("roundRect", logo.geometry);
        assertEquals(0.5f, logo.adjust(0, 0), 0.0001);
        assertEquals(0x800000, logo.fill.color);

        ShapeElement title = (ShapeElement) s.find("e2");
        assertEquals(ShapeElement.TITLE, title.placeholder);
        assertEquals(10, title.x, 0.01);
        assertEquals(20, title.y, 0.01);
        assertEquals(300, title.width, 0.01);
        assertEquals(50, title.height, 0.01);
        assertEquals(TextFrame.BOTTOM, title.text.anchor);
        assertEquals(TextFrame.FIT_SHRINK, title.text.autofit);
        Paragraph tp = (Paragraph) title.text.blocks.get(0);
        assertEquals("Hello link", tp.plainText());
        assertEquals(Paragraph.CENTER, tp.align);
        assertEquals(44, tp.text.runStyle(0).size, 0.01);
        assertEquals("Arial", tp.text.runStyle(0).font);
        assertTrue(tp.text.runStyle(0).bold);
        assertEquals("https://example.com", tp.text.runStyle(1).link);

        ShapeElement body = (ShapeElement) s.find("e3");
        assertEquals(100, body.y, 0.01);
        Paragraph bp = (Paragraph) body.text.blocks.get(0);
        assertEquals(Paragraph.BULLET, bp.list);
        assertEquals(28, bp.text.runStyle(0).size, 0.01);
        assertEquals("Calibri", bp.text.runStyle(0).font);

        PictureElement pic = (PictureElement) s.find("e4");
        assertEquals(0.1f, pic.cropLeft, 0.0001);
        assertEquals(0.25f, pic.cropBottom, 0.0001);

        TableElement table = (TableElement) s.find("e5");
        assertEquals(2, table.table.rows());
        assertEquals(2, table.table.columns());
        assertEquals(2, table.table.span(0, 0).columns);
        assertEquals("B", table.table.cellText(1, 1));

        GroupElement group = (GroupElement) s.find("e6");
        ShapeElement line = (ShapeElement) group.children.get(0);
        assertEquals(200, line.x, 0.01);
        assertEquals(20, line.width, 0.01);
        assertEquals(2, line.line.width, 0.01);
        assertEquals(LineStyle.DASH, line.line.dash);
        assertEquals(LineStyle.ARROW_TRIANGLE, line.line.tail);
        assertEquals(0x00ff00, line.line.color);

        ChartElement chart = (ChartElement) s.find("e8");
        assertEquals(ChartElement.COLUMN, chart.kind);
        assertEquals(2, chart.categoryCount());
        assertEquals("Q2", chart.category(1));
        assertEquals("2024", chart.seriesName(0));
        assertEquals(3.5, chart.values(0)[0], 0.0001);
        assertEquals(0, chart.values(0)[1], 0.0001);
        assertEquals(0x0000ff, chart.seriesColor(0));

        assertEquals("Say hi", ((Paragraph) s.notes.get(0)).plainText());
        assertEquals(Transition.PUSH, s.transition.type);
        assertEquals(Transition.FROM_TOP, s.transition.direction);
        assertEquals(1200, s.transition.duration);
        assertEquals(3000, s.transition.advanceAfter);
        assertEquals(1, s.builds.size());
        assertEquals("e3", s.builds.get(0).elementId);
        assertEquals(Build.FADE, s.builds.get(0).effect);
        assertEquals(700, s.builds.get(0).duration);

        Slide hidden = p.slides.get(1);
        assertTrue(hidden.hidden);
        assertFalse(p.getWarnings().isEmpty());
        assertTrue(p.getWarnings().contains("A slide whose layout is missing was read without one"));
        assertTrue(p.getWarnings().contains("A relationship pointing outside the file was ignored"));
        assertTrue(p.getWarnings().contains("A slide the presentation lists is missing"));
        assertTrue(p.nextId > 9);
        assertNotNull(p.newId('e'));
    }

    @Test
    void nonZipIsAnIOException() {
        assertThrows(IOException.class, () -> read("not a zip at all".getBytes("UTF-8")));
        assertThrows(IOException.class, () -> read(zip("hello.txt", "x")));
    }

    @Test
    void hostilePackagesGiveWarnings() throws IOException {
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            deep.append("<p:grpSp><p:nvGrpSpPr><p:cNvPr id=\"").append(i + 10).append("\" name=\"g\"/></p:nvGrpSpPr>");
        }
        deep.append("<p:sp><p:nvSpPr><p:cNvPr id=\"2\" name=\"x\"/></p:nvSpPr><p:spPr>").append(xfrm(1, 1, 1, 1))
                .append("</p:spPr></p:sp>");
        for (int i = 0; i < 40; i++) {
            deep.append("</p:grpSp>");
        }
        byte[] b = zip("ppt/presentation.xml", "<p:presentation" + NS + "><p:sldIdLst><p:sldId id=\"1\" r:id=\"a\"/>"
                        + "<p:sldId id=\"2\" r:id=\"b\"/></p:sldIdLst><p:sldSz cx=\"-1\" cy=\"garbage\"/></p:presentation>",
                "ppt/_rels/presentation.xml.rels", rels("a", "slide", "slides/slide1.xml", "b", "slide", "slides/slide2.xml"),
                "ppt/slides/slide1.xml", "<p:sld" + NS + "><p:cSld><p:spTree>" + deep + "</p:spTree></p:cSld></p:sld>",
                "ppt/slides/_rels/slide1.xml.rels", rels("r1", "slideLayout", "slide1.xml"),
                "ppt/slides/slide2.xml", SLIDE2.replace("show=\"0\"", "") + "<trailing",
                "ppt/slides/_rels/slide2.xml.rels", rels("r1", "slideLayout", "../slideLayouts/l.xml"),
                "ppt/slideLayouts/l.xml", "<p:sldLayout" + NS + "/>",
                "ppt/slideLayouts/_rels/l.xml.rels", rels("r1", "slideMaster", "l.xml"));
        Presentation p = read(b);
        assertEquals(2, p.slides.size(), p.getWarnings().toString());
        assertTrue(p.getWarnings().contains("Groups nested deeper than 32 were left out"), p.getWarnings().toString());
        Slide s2 = p.slides.get(1);
        ShapeElement bad = (ShapeElement) s2.elements.get(0);
        assertEquals(0, bad.x, 0);
        assertEquals("rect", bad.geometry);
        assertTrue(p.getWarnings().contains("A freeform shape was read as a rectangle"));
        assertTrue(p.width > 0);
    }

    @Test
    void emptyPresentationReads() throws IOException {
        Presentation p = read(zip("ppt/presentation.xml", "<p:presentation" + NS + "/>"));
        assertEquals(0, p.slides.size());
    }
}
