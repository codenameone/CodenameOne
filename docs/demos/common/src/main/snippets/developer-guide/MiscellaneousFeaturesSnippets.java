// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::miscellaneous-features-java-001[]
try {
    Display.getInstance().sendSMS("+999999999", "My SMS Message");
    // Or: CN.sendSMS("+999999999", "My SMS Message");
} catch(IOException err) {
    Log.e(err);
    Dialog.show("SMS Failed", "Unable to send the SMS", "OK", null);
}
// end::miscellaneous-features-java-001[]

// tag::miscellaneous-features-java-002[]
try {
    switch(Display.getInstance().getSMSSupport()) {
        case Display.SMS_NOT_SUPPORTED:
            return;
        case Display.SMS_SEAMLESS:
            showUIDialogToEditMessageData();
            Display.getInstance().sendSMS(phone, data);
            return;
        default:
            Display.getInstance().sendSMS(phone, data);
            return;
    }
} catch(IOException err) {
    Log.e(err);
    Dialog.show("SMS Failed", "Unable to send the SMS", "OK", null);
}
// end::miscellaneous-features-java-002[]

// tag::miscellaneous-features-java-003[]
Display.getInstance().dial("+999999999");
// end::miscellaneous-features-java-003[]

// tag::miscellaneous-features-java-004[]
Display display = Display.getInstance();
if (display.isCallDetectionSupported() && display.isInCall()) {
    Log.p("Call interruption is currently active (heuristic)");
}
// end::miscellaneous-features-java-004[]

// tag::miscellaneous-features-java-005[]
Message m = new Message("Body of message");
Display.getInstance().sendMessage(new String[] {"someone@gmail.com"}, "Subject of message", m);
// end::miscellaneous-features-java-005[]

// tag::miscellaneous-features-java-006[]
Message m = new Message("Body of message");
m.getAttachments().put(textAttachmentUri, "text/plain");
m.getAttachments().put(imageAttachmentUri, "image/png");
Display.getInstance().sendMessage(new String[] {"someone@gmail.com"}, "Subject of message", m);
// end::miscellaneous-features-java-006[]

// tag::miscellaneous-features-java-007[]
UIManager.getInstance().setUseLargerTextScale(true);
// end::miscellaneous-features-java-007[]

// tag::miscellaneous-features-java-008[]
Display display = Display.getInstance();
if (display.isLargerTextEnabled()) {
    float scale = display.getLargerTextScale();
    Font base = UIManager.getInstance().getLookAndFeel().getDefaultStyle().getFont();
    Font scaled = base.derive(base.getHeight() * scale, base.getStyle());
    someComponent.getUnselectedStyle().setFont(scaled);
}
// end::miscellaneous-features-java-008[]

// tag::miscellaneous-features-java-009[]
getContactById(String id, boolean includesFullName,
            boolean includesPicture, boolean includesNumbers, boolean includesEmail,
            boolean includeAddress)
// end::miscellaneous-features-java-009[]

// tag::miscellaneous-features-java-010[]
Form hi = new Form("Contacts", new BoxLayout(BoxLayout.Y_AXIS));
hi.add(new InfiniteProgress());
Display.getInstance().scheduleBackgroundTask(() -> {
    Contact[] contacts = ContactsManager.getContacts(true, true, false, true, false, false);
    Display.getInstance().callSerially(() -> {
        hi.removeAll();
        for(Contact c : contacts) {
            MultiButton mb = new MultiButton(c.getDisplayName());
            mb.setTextLine2(c.getPrimaryPhoneNumber());
            hi.add(mb);
            mb.putClientProperty("id", c.getId());
        }
        hi.getContentPane().animateLayout(150);
    });
});
hi.show();
// end::miscellaneous-features-java-010[]

// tag::miscellaneous-features-java-011[]
Form hi = new Form("Contacts", new BoxLayout(BoxLayout.Y_AXIS));
hi.add(new InfiniteProgress());
int size = Display.getInstance().convertToPixels(5, true);
FontImage fi = FontImage.createFixed("" + FontImage.MATERIAL_PERSON, FontImage.getMaterialDesignFont(), 0xff, size, size);

Display.getInstance().scheduleBackgroundTask(() -> {
    Contact[] contacts = ContactsManager.getContacts(true, true, false, true, false, false);
    Display.getInstance().callSerially(() -> {
        hi.removeAll();
        for(Contact c : contacts) {
            MultiButton mb = new MultiButton(c.getDisplayName());
            mb.setIcon(fi);
            mb.setTextLine2(c.getPrimaryPhoneNumber());
            hi.add(mb);
            mb.putClientProperty("id", c.getId());
            Display.getInstance().scheduleBackgroundTask(() -> {
                Contact cc = ContactsManager.getContactById(c.getId(), false, true, false, false, false);
                Display.getInstance().callSerially(() -> {
                    Image photo = cc.getPhoto();
                    if(photo != null) {
                        mb.setIcon(photo.fill(size, size));
                        mb.revalidate();
                    }
                });
            });
        }
        hi.getContentPane().animateLayout(150);
    });
});
// end::miscellaneous-features-java-011[]

// tag::miscellaneous-features-java-012[]
UIManager.getInstance().setBundle(res.getL10N("l10n", local));
// end::miscellaneous-features-java-012[]

// tag::miscellaneous-features-java-013[]
String local = L10NManager.getInstance().getLanguage();
// end::miscellaneous-features-java-013[]

// tag::miscellaneous-features-java-014[]
UIManager.getInstance().localize( "KeyInBundle", "DefaultValue");
// end::miscellaneous-features-java-014[]

// tag::miscellaneous-features-java-015[]
Resources res = fetchResourceFile();
Enumeration locales = res.listL10NLocales( "l10n" );
// end::miscellaneous-features-java-015[]

// tag::miscellaneous-features-java-016[]
Form hi = new Form("L10N", new BoxLayout(BoxLayout.Y_AXIS));
HashMap<String, String> resourceBudle = new HashMap<String, String>();
resourceBudle.put("Localize", "This Label is localized");
UIManager.getInstance().setBundle(resourceBudle);
hi.add(new Label("Localize"));
hi.show();
// end::miscellaneous-features-java-016[]

// tag::miscellaneous-features-java-017[]
Form hi = new Form("L10N", new TableLayout(16, 2));
L10NManager l10n = L10NManager.getInstance();
hi.add("format(double)").add(l10n.format(11.11)).
    add("format(int)").add(l10n.format(33)).
    add("formatCurrency").add(l10n.formatCurrency(53.267)).
    add("formatDateLongStyle").add(l10n.formatDateLongStyle(new Date())).
    add("formatDateShortStyle").add(l10n.formatDateShortStyle(new Date())).
    add("formatDateTime").add(l10n.formatDateTime(new Date())).
    add("formatDateTimeMedium").add(l10n.formatDateTimeMedium(new Date())).
    add("formatDateTimeShort").add(l10n.formatDateTimeShort(new Date())).
    add("getCurrencySymbol").add(l10n.getCurrencySymbol()).
    add("getLanguage").add(l10n.getLanguage()).
    add("getLocale").add(l10n.getLocale()).
    add("isRTLLocale").add("" + l10n.isRTLLocale()).
    add("parseCurrency").add(l10n.formatCurrency(l10n.parseCurrency("33.77$"))).
    add("parseDouble").add(l10n.format(l10n.parseDouble("34.35"))).
    add("parseInt").add(l10n.format(l10n.parseInt("56"))).
    add("parseLong").add("" + l10n.parseLong("4444444"));
hi.show();
// end::miscellaneous-features-java-017[]

// tag::miscellaneous-features-java-018[]
Location position = LocationManager.getLocationManager().getCurrentLocationSync();
// end::miscellaneous-features-java-018[]

// tag::miscellaneous-features-java-019[]
class MyListener implements LocationListener {
    public void locationUpdated(Location location) {
        // update UI etc.
    }

    public void providerStateChanged(int newState) {
        // handle status changes/errors appropriately
    }
}
LocationManager.getLocationManager().setLocationListener(new MyListener());
// end::miscellaneous-features-java-019[]

// tag::miscellaneous-features-java-020[]
Geofence gf = new Geofence("test", loc, 100, 100000);

LocationManager.getLocationManager()
        .addGeoFencing(GeofenceListenerImpl.class, gf);
// end::miscellaneous-features-java-020[]

// tag::miscellaneous-features-java-021[]
public class GeofenceListenerImpl implements GeofenceListener {
    @Override
    public void onExit(String id) {
    }

    @Override
    public void onEntered(String id) {
        if(Display.getInstance().isMinimized()) {
            Display.getInstance().callSerially(() -> {
                Dialog.show("Welcome", "Thanks for arriving", "OK", null);
            });
        } else {
            LocalNotification ln = new LocalNotification();
            ln.setAlertTitle("Welcome");
            ln.setAlertBody("Thanks for arriving!");
            Display.getInstance().scheduleLocalNotification(ln, 10, false);
        }
    }
}
// end::miscellaneous-features-java-021[]

// tag::miscellaneous-features-java-022[]
String filePath = Capture.capturePhoto();
// end::miscellaneous-features-java-022[]

// tag::miscellaneous-features-java-023[]
String filePath = Capture.capturePhoto();
if(filePath != null) {
    Util.copy(FileSystemStorage.getInstance().openInputStream(filePath), Storage.getInstance().createOutputStream(myImageFileName));
}
// end::miscellaneous-features-java-023[]

// tag::miscellaneous-features-java-024[]
Form hi = new Form("Capture", new BorderLayout());
hi.setToolbar(new Toolbar());
Style s = UIManager.getInstance().getComponentStyle("Title");
FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_CAMERA, s);

ImageViewer iv = new ImageViewer(icon);

hi.getToolbar().addCommandToRightBar("", icon, (ev) -> {
    String filePath = Capture.capturePhoto();
    if(filePath != null) {
        try {
            DefaultListModel<Image> m = (DefaultListModel<Image>)iv.getImageList();
            Image img = Image.createImage(filePath);
            if(m == null) {
                m = new DefaultListModel<>(img);
                iv.setImageList(m);
                iv.setImage(img);
            } else {
                m.addItem(img);
            }
            m.setSelectedIndex(m.getSize() - 1);
        } catch(IOException err) {
            Log.e(err);
        }
    }
});

hi.add(BorderLayout.CENTER, iv);
hi.show();
// end::miscellaneous-features-java-024[]

// tag::miscellaneous-features-java-025[]
Form hi = new Form("Capture", BoxLayout.y());
hi.setToolbar(new Toolbar());
Style s = UIManager.getInstance().getComponentStyle("Title");
FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_MIC, s);

FileSystemStorage fs = FileSystemStorage.getInstance();
String recordingsDir = fs.getAppHomePath() + "recordings/";
fs.mkdir(recordingsDir);
try {
    for(String file : fs.listFiles(recordingsDir)) {
        MultiButton mb = new MultiButton(file.substring(file.lastIndexOf("/") + 1));
        mb.addActionListener((e) -> {
            try {
                Media m = MediaManager.createMedia(recordingsDir + file, false);
                m.play();
            } catch(IOException err) {
                Log.e(err);
            }
        });
        hi.add(mb);
    }

    hi.getToolbar().addCommandToRightBar("", icon, (ev) -> {
        try {
            String file = Capture.captureAudio();
            if(file != null) {
                SimpleDateFormat sd = new SimpleDateFormat("yyyy-MMM-dd-kk-mm");
                String fileName =sd.format(new Date());
                String filePath = recordingsDir + fileName;
                Util.copy(fs.openInputStream(file), fs.openOutputStream(filePath));
                MultiButton mb = new MultiButton(fileName);
                mb.addActionListener((e) -> {
                    try {
                        Media m = MediaManager.createMedia(filePath, false);
                        m.play();
                    } catch(IOException err) {
                        Log.e(err);
                    }
                });
                hi.add(mb);
                hi.revalidate();
            }
        } catch(IOException err) {
            Log.e(err);
        }
    });
} catch(IOException err) {
    Log.e(err);
}
hi.show();
// end::miscellaneous-features-java-025[]

// tag::miscellaneous-features-java-026[]
    private static final EasyThread countTime = EasyThread.start("countTime");

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form hi = new Form("Recording audio", BoxLayout.y());
        hi.add(new SpanLabel("Example of recording and playback audio using the Media, MediaManager and MediaRecorderBuilder APIs"));
        hi.add(recordAudio((String filePath) -> {
            ToastBar.showInfoMessage("Do something with the recorded audio file: " + filePath);
        }));
        hi.show();
    }

    public static Component recordAudio(OnComplete<String> callback) {
        try {
            // mime types supported by Android: audio/amr, audio/aac, audio/mp4
            // mime types supported by iOS: audio/mp4, audio/aac, audio/m4a
            // mime type supported by Simulator: audio/wav
            // more info: https://www.iana.org/assignments/media-types/media-types.xhtml

            List<String> availableMimetypes = Arrays.asList(MediaManager.getAvailableRecordingMimeTypes());
            String mimetype;
            if (availableMimetypes.contains("audio/aac")) {
                // Android and iOS
                mimetype = "audio/aac";
            } else if (availableMimetypes.contains("audio/wav")) {
                // Simulator
                mimetype = "audio/wav";
            } else {
                // others
                mimetype = availableMimetypes.get(0);
            }
            String fileName = "audioExample." + mimetype.substring(mimetype.indexOf("/") + 1);
            String output = FileSystemStorage.getInstance().getAppHomePath() + "/" + fileName;
            // https://tritondigitalcommunity.force.com/s/article/Choosing-Audio-Bitrate-Settings
            MediaRecorderBuilder options = new MediaRecorderBuilder()
                    .mimeType(mimetype)
                    .path(output)
                    .bitRate(64000)
                    .samplingRate(44100);
            Media[] microphone = {MediaManager.createMediaRecorder(options)};
            Media[] speaker = {null};

            Container recordingUI = new Container(BoxLayout.y());
            Label time = new Label("0:00");
            Button recordBtn = new Button("", FontImage.MATERIAL_FIBER_MANUAL_RECORD, "Button");
            Button playBtn = new Button("", FontImage.MATERIAL_PLAY_ARROW, "Button");
            Button stopBtn = new Button("", FontImage.MATERIAL_STOP, "Button");
            Button sendBtn = new Button("Send");
            sendBtn.setEnabled(false);
            Container buttons = GridLayout.encloseIn(3, recordBtn, stopBtn, sendBtn);
            recordingUI.addAll(FlowLayout.encloseCenter(time), FlowLayout.encloseCenter(buttons));

            recordBtn.addActionListener(l -> {
                try {
                    // every time we have to create a new instance of Media to make it working correctly (as reported in the Javadoc)
                    microphone[0] = MediaManager.createMediaRecorder(options);
                    if (speaker[0] != null && speaker[0].isPlaying()) {
                        return; // do nothing if the audio is currently recorded or played
                    }
                    recordBtn.setEnabled(false);
                    sendBtn.setEnabled(true);
                    Log.p("Audio recording started", Log.DEBUG);
                    if (buttons.contains(playBtn)) {
                        buttons.replace(playBtn, stopBtn, CommonTransitions.createEmpty());
                        buttons.revalidateWithAnimationSafety();
                    }
                    if (speaker[0] != null) {
                        speaker[0].pause();
                    }

                    microphone[0].play();
                    startWatch(time);
                } catch (IOException ex) {
                    Log.p("ERROR recording audio", Log.ERROR);
                    Log.e(ex);
                }
            });

            stopBtn.addActionListener(l -> {
                if (!microphone[0].isPlaying() && (speaker[0] == null || !speaker[0].isPlaying())) {
                    return; // do nothing if the audio is NOT currently recorded or played
                }
                recordBtn.setEnabled(true);
                sendBtn.setEnabled(true);
                Log.p("Audio recording stopped");
                if (microphone[0].isPlaying()) {
                    microphone[0].pause();
                } else if (speaker[0] != null) {
                    speaker[0].pause();
                } else {
                    return;
                }
                stopWatch(time);
                if (buttons.contains(stopBtn)) {
                    buttons.replace(stopBtn, playBtn, CommonTransitions.createEmpty());
                    buttons.revalidateWithAnimationSafety();
                }
                if (FileSystemStorage.getInstance().exists(output)) {
                    Log.p("Audio saved to: " + output);
                } else {
                    ToastBar.showErrorMessage("Error recording audio", 5000);
                    Log.p("ERROR SAVING AUDIO");
                }
            });

            playBtn.addActionListener(l -> {
                // every time we have to create a new instance of Media to make it working correctly (as reported in the Javadoc)
                if (microphone[0].isPlaying() || (speaker[0] != null && speaker[0].isPlaying())) {
                    return; // do nothing if the audio is currently recorded or played
                }
                recordBtn.setEnabled(false);
                sendBtn.setEnabled(true);
                if (buttons.contains(playBtn)) {
                    buttons.replace(playBtn, stopBtn, CommonTransitions.createEmpty());
                    buttons.revalidateWithAnimationSafety();
                }
                if (FileSystemStorage.getInstance().exists(output)) {
                    try {
                        speaker[0] = MediaManager.createMedia(output, false, () -> {
                            // callback on completation
                            recordBtn.setEnabled(true);
                            if (speaker[0].isPlaying()) {
                                speaker[0].pause();
                            }
                            stopWatch(time);
                            if (buttons.contains(stopBtn)) {
                                buttons.replace(stopBtn, playBtn, CommonTransitions.createEmpty());
                                buttons.revalidateWithAnimationSafety();
                            }
                        });
                        speaker[0].play();
                        startWatch(time);
                    } catch (IOException ex) {
                        Log.p("ERROR playing audio", Log.ERROR);
                        Log.e(ex);
                    }
                }
            });

            sendBtn.addActionListener(l -> {
                if (microphone[0].isPlaying()) {
                    microphone[0].pause();
                }
                if (speaker[0] != null && speaker[0].isPlaying()) {
                    speaker[0].pause();
                }
                if (buttons.contains(stopBtn)) {
                    buttons.replace(stopBtn, playBtn, CommonTransitions.createEmpty());
                    buttons.revalidateWithAnimationSafety();
                }
                stopWatch(time);
                recordBtn.setEnabled(true);

                callback.completed(output);
            });

            return FlowLayout.encloseCenter(recordingUI);

        } catch (IOException ex) {
            Log.p("ERROR recording audio", Log.ERROR);
            Log.e(ex);
            return new Label("Error recording audio");
        }

    }

    private static void startWatch(Label label) {
        label.putClientProperty("stopTime", Boolean.FALSE);
        countTime.run(() -> {
            long startTime = System.currentTimeMillis();
            while (label.getClientProperty("stopTime") == Boolean.FALSE) {
                // the sleep is every 200ms instead of 1000ms to make the app more reactive when stop is tapped
                Util.sleep(200);
                int seconds = (int) ((System.currentTimeMillis() - startTime) / 1000);
                String min = (seconds / 60) + "";
                String sec = (seconds % 60) + "";
                if (sec.length() == 1) {
                    sec = "0" + sec;
                }
                String newTime = min + ":" + sec;
                if (!label.getText().equals(newTime)) {
                    CN.callSerially(() -> {
                        label.setText(newTime);
                        if (label.getParent() != null) {
                            label.getParent().revalidateWithAnimationSafety();
                        }
                    });
                }
            }
        });
    }

    private static void stopWatch(Label label) {
        label.putClientProperty("stopTime", Boolean.TRUE);
    }
// end::miscellaneous-features-java-026[]

// tag::miscellaneous-features-java-027[]
hi.getToolbar().addCommandToRightBar("", icon, (ev) -> {
    Capture.capturePhoto((e) -> {
        if(e != null && e.getSource() != null) {
            try {
                DefaultListModel<Image> m = (DefaultListModel<Image>)iv.getImageList();
                Image img = Image.createImage((String)e.getSource());
                if(m == null) {
                    m = new DefaultListModel<>(img);
                    iv.setImageList(m);
                    iv.setImage(img);
                } else {
                    m.addItem(img);
                }
                m.setSelectedIndex(m.getSize() - 1);
            } catch(IOException err) {
                Log.e(err);
            }
        }
    });
});
// end::miscellaneous-features-java-027[]

// tag::miscellaneous-features-java-028[]
Form hi = new Form("Capture", new BorderLayout());
hi.setToolbar(new Toolbar());
Style s = UIManager.getInstance().getComponentStyle("Title");
FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_CAMERA, s);

ImageViewer iv = new ImageViewer(icon);

hi.getToolbar().addCommandToRightBar("", icon, (ev) -> {
    Display.getInstance().openGallery((e) -> {
        if(e != null && e.getSource() != null) {
            try {
                DefaultListModel<Image> m = (DefaultListModel<Image>)iv.getImageList();
                Image img = Image.createImage((String)e.getSource());
                if(m == null) {
                    m = new DefaultListModel<>(img);
                    iv.setImageList(m);
                    iv.setImage(img);
                } else {
                    m.addItem(img);
                }
                m.setSelectedIndex(m.getSize() - 1);
            } catch(IOException err) {
                Log.e(err);
            }
        }
    }, Display.GALLERY_IMAGE);
});

hi.add(BorderLayout.CENTER, iv);
// end::miscellaneous-features-java-028[]

// tag::miscellaneous-features-java-029[]
myMultiButton.addActionListener((e) -> {
    if(e.getComponent() == myMultiButton) {
        // this won't occur since the source component is really a button!
    }
    if(e.getActualComponent() == myMultiButton) {
        // this will happen...
    }
});
// end::miscellaneous-features-java-029[]

// tag::miscellaneous-features-java-030[]
public class SpanButton extends Container {
    private Button actualButton;
    private TextArea text;

    public SpanButton(String txt) {
        setUIID("Button");
        setLayout(new BorderLayout());
        text = new TextArea(getUIManager().localize(txt, txt));
        text.setUIID("Button");
        text.setEditable(false);
        text.setFocusable(false);
        actualButton = new Button();
        addComponent(BorderLayout.WEST, actualButton);
        addComponent(BorderLayout.CENTER, text);
        setLeadComponent(actualButton);
    }


    public void setText(String t) {
        text.setText(getUIManager().localize(t, t));
    }

    public void setIcon(Image i) {
        actualButton.setIcon(i);
    }

    public String getText() {
        return text.getText();
    }

    public Image getIcon() {
        return actualButton.getIcon();
    }

    public void addActionListener(ActionListener l) {
        actualButton.addActionListener(l);
    }

    public void removeActionListener(ActionListener l) {
        actualButton.removeActionListener(l);
    }

}
// end::miscellaneous-features-java-030[]

// tag::miscellaneous-features-java-031[]
Form f = new Form("Accordion", new BorderLayout());
Accordion accr = new Accordion();
f.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_ADD, e -> addEntry(accr));
addEntry(accr);
f.add(BorderLayout.CENTER, accr);
f.show();

void addEntry(Accordion accr) {
    TextArea t = new TextArea("New Entry");
    Button delete = new Button();
    FontImage.setMaterialIcon(delete, FontImage.MATERIAL_DELETE);
    Label title = new Label(t.getText());
    t.addActionListener(ee -> title.setText(t.getText()));
    delete.addActionListener(ee -> {
        accr.removeContent(t);
        accr.animateLayout(200);
    });
    delete.setBlockLead(true);
    delete.setUIID("Label");
    Container header = BorderLayout.center(title).
            add(BorderLayout.EAST, delete);
    accr.addContent(header, t);
    accr.animateLayout(200);
}
// end::miscellaneous-features-java-031[]

// tag::miscellaneous-features-java-032[]
Form hi = new Form("Pull To Refresh", BoxLayout.y());
hi.getContentPane().addPullToRefresh(() -> {
    hi.add("Pulled at " + L10NManager.getInstance().formatDateTimeShort(new Date()));
});
hi.show();
// end::miscellaneous-features-java-032[]

// tag::miscellaneous-features-java-033[]
Boolean can = Display.getInstance().canExecute("imdb:///find?q=godfather");
if(can != null && can) {
    Display.getInstance().execute("imdb:///find?q=godfather");
} else {
    Display.getInstance().execute("http://www.imdb.com");
}
// end::miscellaneous-features-java-033[]

// tag::miscellaneous-features-java-034[]
/**
 * Returns the build hints for the simulator, this will only work in the debug environment and it's
 * designed to allow extensions/APIs to verify user settings/build hints exist
 * @return map of the build hints that isn't modified without the codename1.arg. prefix
 */
public Map<String, String> getProjectBuildHints() {}

/**
 * Sets a build hint into the settings while overwriting any previous value. This will only work in the
 * debug environment and it's designed to allow extensions/APIs to verify user settings/build hints exist.
 * Important: this will throw an exception outside of the simulator!
 * @param key the build hint without the codename1.arg. prefix
 * @param value the value for the hint
 */
public void setProjectBuildHint(String key, String value) {}
// end::miscellaneous-features-java-034[]

// tag::miscellaneous-features-java-035[]
EasyThread e = EasyThread.start("ThreadName");
// end::miscellaneous-features-java-035[]

// tag::miscellaneous-features-java-036[]
e.run(() -> doThisOnTheThread());
// end::miscellaneous-features-java-036[]

// tag::miscellaneous-features-java-037[]
e.run((success) -> success.onSuccess(doThisOnTheThread()), (myResult) -> onEDTGotResult(myRsult));
// end::miscellaneous-features-java-037[]

// tag::miscellaneous-features-java-038[]
EasyThread e = EasyThread.start("Hi");
int result = e.run(() -> {
    System.out.println("This is a thread");
    return 3;
});
// end::miscellaneous-features-java-038[]

// tag::miscellaneous-features-java-039[]
@Override
protected void initComponent() {
    super.initComponent();
    getComponentForm().setEnableCursors(true);
}
// end::miscellaneous-features-java-039[]
