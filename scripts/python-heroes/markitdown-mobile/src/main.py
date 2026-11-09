"""MarkItDown Mobile: Microsoft's MarkItDown with a Codename One interface.

`markitdown` and its converters' dependencies (pdfminer.six, mammoth,
python-pptx, openpyxl, beautifulsoup4, markdownify) are the unmodified
releases pinned in requirements.txt. This file is the new part: pick a
document or enter a URL, convert it on the device, and read, copy or share
the Markdown. Nothing is uploaded: conversion runs in the application.
"""
import io
import os
import threading
import time

from markitdown import MarkItDown, StreamInfo

from java.com.codename1.components import InfiniteProgress, MultiButton, SpanLabel
from java.com.codename1.ui import Container, Dialog, Display, FontImage, Form, TextArea, TextField
from java.com.codename1.ui.layouts import BorderLayout, BoxLayout

ACCEPT = "pdf,docx,pptx,xlsx,html,htm,csv,json,xml,rss,atom,epub,zip,ipynb,txt,md"

_converter = []


def converter():
    # One instance, made on first use: it registers every converter once.
    if not _converter:
        _converter.append(MarkItDown(enable_plugins=False))
    return _converter[0]


def local_path(path):
    # The chooser answers a FileSystemStorage path; Python's file system
    # takes the same location without the scheme.
    return path[len("file://"):] if path.startswith("file://") else path


def convert_file(path):
    name = os.path.basename(path)
    ext = os.path.splitext(name)[1].lower() or None
    with open(local_path(path), "rb") as f:
        data = f.read()
    started = time.perf_counter()
    result = converter().convert_stream(io.BytesIO(data), stream_info=StreamInfo(extension=ext, filename=name))
    return name, result, len(data), time.perf_counter() - started


def convert_url(url):
    started = time.perf_counter()
    result = converter().convert(url)
    return url, result, None, time.perf_counter() - started


def edt(fn):
    Display.getInstance().callSerially(fn)


class Home:
    def __init__(self):
        self.recent = []
        self.form = Form("MarkItDown", BoxLayout.y())
        tb = self.form.getToolbar()
        tb.addMaterialCommandToRightBar("", FontImage.MATERIAL_FOLDER_OPEN, lambda e: self.pick())
        intro = SpanLabel("Turn PDF, Word, PowerPoint, Excel, HTML, EPUB and more into Markdown "
                          "for LLMs, on this device.")
        self.url = TextField("", "https://... (a page, a feed, a document)")
        self.url.addActionListener(lambda e: self.run(convert_url, self.url.getText().strip()))
        pick = MultiButton("Open a document")
        pick.setTextLine2("PDF, DOCX, PPTX, XLSX, HTML, EPUB, CSV, ZIP")
        FontImage.setMaterialIcon(pick, FontImage.MATERIAL_DESCRIPTION)
        pick.addActionListener(lambda e: self.pick())
        self.recent_list = Container(BoxLayout.y())
        self.form.add(intro).add(pick).add(self.url).add(self.recent_list)

    def show(self):
        self.form.show()

    def pick(self):
        def chosen(event):
            path = event.getSource() if event is not None else None
            if path:
                self.run(convert_file, str(path))

        Display.getInstance().openFileChooser(chosen, ACCEPT)

    def run(self, job, arg):
        if not arg:
            return
        progress = InfiniteProgress().showInfiniteBlocking()

        def work():
            try:
                outcome = job(arg)
                edt(lambda: (progress.dispose(), Result(self, *outcome).show()))
            except Exception as e:  # noqa: BLE001 - the dialog is where errors are read
                message = "%s: %s" % (type(e).__name__, e)
                edt(lambda: (progress.dispose(), Dialog.show("Couldn't convert", message, "OK", None)))

        threading.Thread(target=work, daemon=True).start()

    def remember(self, name, seconds):
        self.recent.insert(0, (name, seconds))
        del self.recent[8:]
        self.recent_list.removeAll()
        for item_name, item_seconds in self.recent:
            row = MultiButton(item_name)
            row.setTextLine2("converted in %.2f s" % item_seconds)
            self.recent_list.add(row)
        self.form.revalidate()


class Result:
    def __init__(self, home, name, result, size, seconds):
        self.home = home
        self.markdown = result.markdown
        home.remember(name, seconds)
        self.form = Form(result.title or name, BorderLayout())
        tb = self.form.getToolbar()
        tb.setBackCommand("", lambda e: home.form.showBack())
        tb.addMaterialCommandToRightBar("", FontImage.MATERIAL_SHARE,
                                        lambda e: Display.getInstance().share(self.markdown))
        tb.addMaterialCommandToRightBar("", FontImage.MATERIAL_CONTENT_COPY, lambda e: self.copy())
        stats = "%d characters, %.2f s" % (len(self.markdown), seconds)
        if size is not None:
            stats = "%s from %d KB, " % (name, max(1, size // 1024)) + stats
        text = TextArea(self.markdown)
        text.setEditable(False)
        text.setGrowByContent(True)
        text.setUIID("Label")
        body = BoxLayout.encloseY(SpanLabel(stats), text)
        body.setScrollableY(True)
        self.form.add(BorderLayout.CENTER, body)

    def show(self):
        self.form.show()

    def copy(self):
        Display.getInstance().copyToClipboard(self.markdown)
        Dialog.show("Copied", "The Markdown is on the clipboard.", "OK", None)


Home().show()
