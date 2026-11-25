# Component Catalog -- check here before building a widget

Codename One ships most of what Flutter and React Native developers install from
pub.dev or npm. Before you hand-build a component, or tell the user "Codename One
doesn't have X", look it up in this table under the name you know it by. Many of
these are **methods on a class you would not guess** (cropping is on `ImageViewer`,
Markdown is on `RichTextArea`, list reordering is on `Container`), so a search by
class name misses them.

If something is still not here, search the framework before concluding it is
missing: `grep -rl "<keyword>" CodenameOne/src` over the sources, plus the build
tools (`maven/build-engine`), because some features are compiled at build time and
have no runtime class named after them (Lottie and Rive are like that).

The javadoc of each class, and the `package-info.java` of its package, has a usage
example. Read it before writing code against the class.

## Lookup by the name you know it by

| You are looking for (pub.dev / npm name) | Use | Package / where |
| --- | --- | --- |
| Lottie (`lottie`, `lottie-react-native`) | Drop the `.json` next to `theme.css`; `url(spinner.json)` in CSS, or `Resources.getGlobalResources().getImage("spinner.json")` | Build-time transcoder, see `css.md` |
| Rive (`rive`, `rive-react-native`) | Drop the `.riv` next to `theme.css`; it is an `Image`. State-machine inputs: `image.setAnimationInput("isOn", true)`; data binding: `setAnimationProperty`; events: `addAnimationListener` | Build-time translator, see `css.md` |
| Markdown viewer / editor (`flutter_markdown`, `react-native-markdown-display`) | `RichTextArea.setMarkdown(md)` and `getMarkdown(callback)`; `setEditable(false)` for a viewer. Also HTML, RTF and AsciiDoc via `setContent(text, RichTextFormat.X)` | `com.codename1.ui` |
| Rich text editor (`flutter_quill`, `react-native-pell-rich-editor`) | `RichTextArea` | `com.codename1.ui` |
| Code editor with syntax highlighting | `CodeEditor(language, text)` | `com.codename1.ui` |
| Paged word processor document (DOCX-style) | `com.codename1.wordprocessing` (`Document`, `MarkdownFormat`, `RichTextDocuments`) | `com.codename1.wordprocessing` |
| Swipe actions on a row (`flutter_slidable`, `react-native-swipe-list-view`) | `new SwipeableContainer(bottomLeft, bottomRight, top)`; `openToLeft()`, `openToRight()`, `close()`; `setPreviouslyOpened()` closes the last opened row when another opens | `com.codename1.ui` |
| Swipe back gesture | `SwipeBackSupport` | `com.codename1.ui.util` |
| Drag to reorder list (`reorderables`, `react-native-draggable-flatlist`) | A `BoxLayout.y()` container with `setDropTarget(true)` and each child `setDraggable(true)`. `Container.drop` moves the child and animates the layout; dropping into another container moves it there | `com.codename1.ui` |
| Kanban board | `KanbanBoard` | `com.codename1.ui.kanban` |
| Data grid / data table (`syncfusion_flutter_datagrid`, `react-native-table-component`) | `DataGrid` (sorting, filtering, selection, aggregates, editors, tree rows) | `com.codename1.ui.table` |
| Simple table | `Table` + `TableLayout` | `com.codename1.ui.table` |
| Calendar / scheduler / agenda (`table_calendar`, `react-native-calendars`) | `Scheduler`, `DayGrid`, `MonthGrid`, `AgendaList`, `ResourceTimeline` | `com.codename1.ui.scheduler` |
| Gantt chart | `GanttChart` (model in `com.codename1.scheduling`) | `com.codename1.ui.scheduler` |
| Date / time picker | `Picker`, `CalendarPicker`, `MaterialDatePicker`, `MaterialTimePicker` | `com.codename1.ui.spinner` |
| Charts (`fl_chart`, `victory-native`) | `Chart` + series: bar, line, pie, scatter, radar, gauge, funnel, heatmap, treemap, sunburst, Sankey, candlestick, waterfall, box plot, histogram; `Sparkline` | `com.codename1.ui.chart` (the old `com.codename1.charts` is deprecated) |
| Gauge / radial meter | `GaugeSeries` in a `Chart` | `com.codename1.ui.chart.series` |
| Node graph / flow editor (`react-flow` style) | `NodeGraph` | `com.codename1.ui.graph` |
| Spreadsheet with formulas, XLSX/ODS/CSV | `com.codename1.spreadsheet` (`Workbook`, `Sheet`, `FormulaBar`) | `com.codename1.spreadsheet` |
| PDF viewer (`flutter_pdfview`, `react-native-pdf`) | `PdfViewer.open(in)` | `com.codename1.pdf.ui` |
| PDF generation | `PdfDocument`, `PdfCanvas`, `PdfTable` | `com.codename1.pdf` |
| Maps (`google_maps_flutter`, `react-native-maps`) | `MapView` (tiles, markers, polylines, vector maps, routing, geocoding) | `com.codename1.maps` |
| Photo zoom / pan (`photo_view`) | `ImageViewer` (pinch, pan, `setZoom`, swipe between images) | `com.codename1.components` |
| Image cropper (`image_cropper`) | `ImageViewer.getCroppedImage(bg)` or `getCroppedImage(w, h, bg)` crops to the current pan and zoom | `com.codename1.components` |
| Star rating bar (`flutter_rating_bar`, `react-native-ratings`) | An editable `Slider` styled with full/empty star images (`getSliderFullSelectedStyle()` etc. and `setMinValue`/`setMaxValue`); see below | `com.codename1.ui` |
| "Rate this app" prompt (`in_app_review`) | `AppReview.getInstance()` | `com.codename1.appreview` |
| Searchable dropdown / typeahead (`dropdown_search`, `flutter_typeahead`) | `AutoCompleteTextField` (`setCompletion`, a `ListModel`, override `filter(String)` for async/server results, `setCompletionRenderer`, `addListListener`) | `com.codename1.ui` |
| Dropdown / select | `Picker` (not `ComboBox`) | `com.codename1.ui.spinner` |
| Bottom sheet (`modal_bottom_sheet`, `@gorhom/bottom-sheet`) | `Sheet` | `com.codename1.ui` |
| Search in toolbar | `Toolbar.addSearchCommand`, `SearchBar` | `com.codename1.ui` |
| Context / long-press menu | `ContextMenu.show(anchor, commands...)` | `com.codename1.ui` |
| Tooltip | `TooltipManager` | `com.codename1.ui` |
| Snackbar / toast | `ToastBar` | `com.codename1.components` |
| Onboarding carousel / image slider (`carousel_slider`, `introduction_screen`) | `Carousel` + `PageIndicator` | `com.codename1.components` |
| Page dots (`smooth_page_indicator`) | `PageIndicator.bind(tabs)` | `com.codename1.components` |
| Shimmer / skeleton (`shimmer`, `skeletonizer`) | `Skeleton` | `com.codename1.components` |
| Feature tour (`showcaseview`, `react-native-copilot`) | `CoachMarks` | `com.codename1.components` |
| Empty / error state | `EmptyState` | `com.codename1.components` |
| Pull to refresh | `form.getContentPane().addPullToRefresh(runnable)` (any scrollable `Component`) | `com.codename1.ui` |
| Infinite scroll / lazy list (`infinite_scroll_pagination`, `FlashList`) | `InfiniteContainer`, `InfiniteScrollAdapter` | `com.codename1.ui`, `com.codename1.components` |
| Sticky section headers | `StickyHeaderContainer` | `com.codename1.components` |
| Accordion / expansion panel | `Accordion` | `com.codename1.components` |
| Stepper / number input (+/-) | `Stepper(value, min, max)` | `com.codename1.components` |
| Split view / master-detail | `SplitPane`, `MasterDetail` | `com.codename1.components` |
| Tree view / file browser | `Tree`, `FileTree` | `com.codename1.ui.tree`, `com.codename1.components` |
| Chat UI (`flutter_chat_ui`, `react-native-gifted-chat`) | `ChatView`, `ChatBubble`, `ChatInput` | `com.codename1.components` |
| OTP / PIN input (`pinput`) | `OtpField`, `PhoneVerification` | `com.codename1.components` |
| Phone number field (`intl_phone_field`) | `PhoneNumberField` | `com.codename1.components` |
| Signature pad (`signature`) | `SignatureComponent` | `com.codename1.components` |
| Audio recorder | `AudioRecorderComponent` | `com.codename1.components` |
| Video / audio player (`video_player`) | `MediaPlayer` | `com.codename1.components` |
| QR / barcode scanning | `com.codename1.codescan`, `com.codename1.qrcode` | |
| Share button | `ShareButton` | `com.codename1.components` |
| Switch / toggle | `Switch` | `com.codename1.components` |
| Check box / radio / switch groups | `CheckBoxList`, `RadioButtonList`, `SwitchList` | `com.codename1.components` |
| Floating action button / speed dial | `FloatingActionButton` (sub-actions via `createSubFAB`) | `com.codename1.components` |
| Text field with clear button | `ClearableTextField` | `com.codename1.components` |
| Material text input | `TextComponent` | `com.codename1.ui` |
| Embedded web view | `BrowserComponent` | `com.codename1.ui` |
| Page / hero transitions | `MorphTransition`, `CommonTransitions`, `FlipTransition`, `BubbleTransition` | `com.codename1.ui.animations` |
| Particles for games | `ParticleEmitter` | `com.codename1.gaming` |

## Star rating with `Slider`

```java
Slider stars = new Slider();
stars.setEditable(true);
stars.setMinValue(0);
stars.setMaxValue(10);           // 10 = five stars, so half stars are possible
Style s = new Style(0xffcc00, 0, null, (byte) 0);
Image full = FontImage.createMaterial(FontImage.MATERIAL_STAR, s, 5).toImage();
s.setFgColor(0xcccccc);
Image empty = FontImage.createMaterial(FontImage.MATERIAL_STAR, s, 5).toImage();
Style[] emptyStyles = {stars.getSliderEmptySelectedStyle(), stars.getSliderEmptyUnselectedStyle()};
Style[] fullStyles = {stars.getSliderFullSelectedStyle(), stars.getSliderFullUnselectedStyle()};
for (Style st : emptyStyles) { starStyle(st, empty); }
for (Style st : fullStyles) { starStyle(st, full); }
stars.setPreferredSize(new Dimension(full.getWidth() * 5, full.getHeight()));

static void starStyle(Style st, Image star) {
    st.setBackgroundType(Style.BACKGROUND_IMAGE_TILE_BOTH);
    st.setBorder(Border.createEmpty());
    st.setBgImage(star);
    st.setBgTransparency(0);
}
```

Use the value divided by two as the rating. There is no `Rating` class; this
`Slider` recipe is the supported way.

## Drag to reorder a list

```java
Container list = new Container(BoxLayout.y());
list.setScrollableY(true);
list.setDropTarget(true);
for (String item : items) {
    MultiButton row = new MultiButton(item);
    row.setDraggable(true);   // press and drag a row to move it
    list.add(row);
}
// Read the new order back from list.getComponentAt(i) when needed.
```

## Markdown viewer

```java
RichTextArea md = new RichTextArea();
md.setMarkdown("# Title\n\nSome **bold** text and a [link](https://example.com).");
md.setEditable(false);
form.add(BorderLayout.CENTER, md);
```

## Not built in yet

Checked against the sources. Build these from the existing pieces, and say so to
the user rather than inventing an API:

- Masonry / staggered grid (variable height tiles packed into the shortest
  column). `TableLayout` aligns rows, so it is not the same; a row of
  `BoxLayout.y()` columns filled by height is the usual workaround.
- Two thumb range slider.
- Color picker.
- Ring / circular percent progress indicator.
- Chips / tag input field.
- Emoji picker, @mention input, "read more" expandable text, activity timeline,
  segmented control, audio waveform view.
