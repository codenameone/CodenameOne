"""LLM Mobile: Simon Willison's `llm` with a Codename One user interface.

The `llm` package, its OpenAI plugin, sqlite-utils and the OpenAI SDK are
the unmodified releases pinned in requirements.txt. Everything in this file
is the new part: a chat screen that streams the reply, a browser over the
same `logs.db` the command line writes, and a settings screen for the model,
the API key and an OpenAI-compatible endpoint (Ollama, LM Studio, a proxy).

Network calls run on a worker thread; `callSerially` hands every chunk back
to the event dispatch thread, which owns the components.
"""
import json
import os
import threading

# Before llm is imported: its user directory decides where keys.json,
# default_model.txt and logs.db live, as they do for the command line.
os.environ.setdefault("LLM_USER_PATH", os.path.join(os.path.expanduser("~"), "llm"))

import llm  # noqa: E402
import sqlite_utils  # noqa: E402
from llm.logs import LogStore, log_rows  # noqa: E402

from java.com.codename1.components import InfiniteProgress, MultiButton, SpanLabel  # noqa: E402
from java.com.codename1.ui import (  # noqa: E402
    Button, ComboBox, Container, Dialog, Display, FontImage, Form, TextArea, TextField,
)
from java.com.codename1.ui.layouts import BorderLayout, BoxLayout  # noqa: E402

SETTINGS = "mobile.json"


def user_dir():
    path = llm.user_dir()
    path.mkdir(parents=True, exist_ok=True)
    return path


def logs_db():
    # The command line's own path, llm.cli.logs_db_path(), without importing
    # the click application to get it.
    return sqlite_utils.Database(str(user_dir() / "logs.db"))


def load_settings():
    try:
        return json.loads((user_dir() / SETTINGS).read_text())
    except (OSError, ValueError):
        return {}


def save_settings(settings):
    (user_dir() / SETTINGS).write_text(json.dumps(settings, indent=2))


def set_key(name, value):
    # The format `llm keys set` writes, so a keys.json copied from a laptop works.
    path = user_dir() / "keys.json"
    try:
        keys = json.loads(path.read_text())
    except (OSError, ValueError):
        keys = {"// Note": "This file stores secret API credentials. Do not share!"}
    keys[name] = value
    path.write_text(json.dumps(keys, indent=2) + "\n")


def apply_settings(settings):
    base = settings.get("base_url")
    if base:
        os.environ["OPENAI_BASE_URL"] = base
    else:
        os.environ.pop("OPENAI_BASE_URL", None)


def edt(fn):
    Display.getInstance().callSerially(fn)


class Chat:
    def __init__(self):
        self.settings = load_settings()
        apply_settings(self.settings)
        self.conversation = None
        self.busy = False
        self.form = Form("LLM", BorderLayout())
        tb = self.form.getToolbar()
        tb.addMaterialCommandToRightBar("", FontImage.MATERIAL_ADD_COMMENT, lambda e: self.new_chat())
        tb.addMaterialCommandToOverflowMenu("History", FontImage.MATERIAL_HISTORY, lambda e: History(self).show())
        tb.addMaterialCommandToOverflowMenu("Settings", FontImage.MATERIAL_SETTINGS, lambda e: Settings(self).show())
        self.thread = Container(BoxLayout.y())
        self.thread.setScrollableY(True)
        self.input = TextField("", "Ask anything")
        self.input.setSingleLineTextArea(False)
        self.input.setMaxSize(4000)
        send = Button(FontImage.createMaterial(FontImage.MATERIAL_SEND, "Button", 4))
        send.addActionListener(lambda e: self.send())
        self.input.addActionListener(lambda e: self.send())
        south = BorderLayout.centerEastWest(self.input, send, None)
        self.form.add(BorderLayout.CENTER, self.thread).add(BorderLayout.SOUTH, south)

    def show(self):
        self.form.show()

    def model(self):
        return llm.get_model(self.settings.get("model") or llm.get_default_model())

    def new_chat(self):
        self.conversation = None
        self.thread.removeAll()
        self.form.revalidate()

    def bubble(self, text):
        label = SpanLabel(text)
        self.thread.add(label)
        self.form.revalidate()
        self.thread.scrollComponentToVisible(label)
        return label

    def send(self):
        text = self.input.getText().strip()
        if not text or self.busy:
            return
        self.input.setText("")
        self.bubble("> " + text)
        reply = self.bubble("...")
        try:
            model = self.model()
        except llm.UnknownModelError as e:
            reply.setText(str(e))
            return
        if self.conversation is None or self.conversation.model.model_id != model.model_id:
            self.conversation = model.conversation()
        conversation = self.conversation
        system = self.settings.get("system") or None
        self.busy = True

        def work():
            shown = []
            try:
                response = conversation.prompt(text, system=system)
                for chunk in response:
                    shown.append(chunk)
                    so_far = "".join(shown)
                    edt(lambda t=so_far: (reply.setText(t), self.form.revalidate()))
                db = logs_db()
                response.log_to_db(db)
                db.close()
                final = response.text()
            except llm.NeedsKeyException as e:
                final = "%s\n\nAdd a key in Settings." % e
            except Exception as e:  # noqa: BLE001 - the reply bubble is where errors are read
                final = "%s: %s" % (type(e).__name__, e)

            def done():
                self.busy = False
                reply.setText(final)
                self.form.revalidate()
                self.thread.scrollComponentToVisible(reply)

            edt(done)

        threading.Thread(target=work, daemon=True).start()


class History:
    """The `llm logs` view: newest turns first, search through the index the
    command line maintains, and a tap opens the whole exchange."""

    def __init__(self, chat):
        self.chat = chat
        self.form = Form("History", BoxLayout.y())
        self.form.getToolbar().setBackCommand("", lambda e: chat.form.showBack())
        self.form.getToolbar().addSearchCommand(lambda e: self.load(self.form.getToolbar().getSearchField().getText()))

    def show(self):
        self.form.show()
        self.load(None)

    def load(self, query):
        progress = InfiniteProgress().showInfiniteBlocking()

        def work():
            try:
                db = logs_db()
                rows = log_rows(LogStore(db), count=100, query=query or None)
                db.close()
            except Exception as e:  # noqa: BLE001
                rows = [{"prompt": "Couldn't read the log", "response": str(e)}]
            edt(lambda: (progress.dispose(), self.fill(rows)))

        threading.Thread(target=work, daemon=True).start()

    def fill(self, rows):
        self.form.removeAll()
        if not rows:
            self.form.add(SpanLabel("Nothing logged yet."))
        for row in rows:
            prompt = (row.get("prompt") or "").strip()
            item = MultiButton(prompt[:80] or "(no prompt)")
            item.setTextLine2("%s  %s" % (row.get("model") or "", (row.get("datetime_utc") or "")[:16]))
            item.addActionListener(lambda e, r=row: self.open(r))
            self.form.add(item)
        self.form.revalidate()

    def open(self, row):
        f = Form(row.get("model") or "Turn", BoxLayout.y())
        f.getToolbar().setBackCommand("", lambda e: self.form.showBack())
        if row.get("system"):
            f.add(SpanLabel("System: " + row["system"]))
        f.add(SpanLabel(row.get("prompt") or ""))
        f.add(SpanLabel(row.get("response") or ""))
        f.show()


class Settings:
    def __init__(self, chat):
        self.chat = chat
        s = chat.settings
        self.form = Form("Settings", BoxLayout.y())
        self.form.getToolbar().setBackCommand("", lambda e: self.save())
        ids = [m.model_id for m in llm.get_models()]
        current = s.get("model") or llm.get_default_model()
        if current not in ids:
            ids.insert(0, current)
        self.model = ComboBox()
        for model_id in ids:
            self.model.addItem(model_id)
        self.model.setSelectedItem(current)
        self.key = TextField("", "OpenAI API key", 40, TextArea.PASSWORD)
        self.base = TextField(s.get("base_url", ""), "Endpoint (blank for api.openai.com)")
        self.system = TextField(s.get("system", ""), "System prompt")
        self.system.setSingleLineTextArea(False)
        for label, field in (("Model", self.model), ("API key", self.key),
                             ("OpenAI-compatible endpoint", self.base), ("System prompt", self.system)):
            self.form.add(SpanLabel(label)).add(field)

    def show(self):
        self.form.show()

    def save(self):
        s = self.chat.settings
        s["model"] = str(self.model.getSelectedItem())
        s["base_url"] = self.base.getText().strip()
        s["system"] = self.system.getText().strip()
        key = self.key.getText().strip()
        if key:
            set_key("openai", key)
        save_settings(s)
        apply_settings(s)
        self.chat.conversation = None
        self.chat.form.showBack()


def main():
    try:
        Chat().show()
    except Exception as e:  # noqa: BLE001 - nothing else could show it
        Dialog.show("LLM", "%s: %s" % (type(e).__name__, e), "OK", None)


main()
