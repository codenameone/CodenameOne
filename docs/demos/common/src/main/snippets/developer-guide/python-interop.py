// Generated from docs/developer-guide source blocks. Edit the guide snippets here, not inline.

// tag::python-interop-python-001[]
from java.com.codename1.ui import Form, Button, Label, TextField, Dialog
from java.com.codename1.ui.layouts import BoxLayout


def main():
    form = Form("Hello Python", BoxLayout.y())
    name = TextField("", "Your name")
    out = Label("Hello, world!")

    def on_greet(event):
        out.setText(f"Hello, {name.getText().strip()}!")
        form.revalidate()

    button = Button("Greet")
    button.addActionListener(on_greet)
    form.add(name).add(button).add(out)
    form.show()


main()
// end::python-interop-python-001[]

// tag::python-interop-python-002[]
import threading

import requests

from java.com.codename1.components import InfiniteProgress, MultiButton
from java.com.codename1.ui import Display, FontImage, Form
from java.com.codename1.ui.layouts import BoxLayout

API = "https://api.github.com/repos/codenameone/CodenameOne/releases"


def fetch_releases():
    response = requests.get(API, params={"per_page": 20}, timeout=15)
    response.raise_for_status()
    return [(r["name"] or r["tag_name"], r["published_at"][:10]) for r in response.json()]


def show_releases(form, releases):
    form.removeAll()
    for name, date in releases:
        row = MultiButton(name)
        row.setTextLine2(date)
        row.addActionListener(lambda e, n=name: Display.getInstance().execute(
            "https://github.com/codenameone/CodenameOne/releases/tag/" + n))
        form.add(row)
    form.revalidate()


def load(form):
    progress = InfiniteProgress().showInfiniteBlocking()

    def work():
        try:
            releases = fetch_releases()
        except requests.RequestException as e:
            releases = [("Couldn't load releases", str(e))]
        # Components belong to the EDT: hand the result back to it.
        Display.getInstance().callSerially(lambda: (progress.dispose(), show_releases(form, releases)))

    threading.Thread(target=work, daemon=True).start()


def main():
    form = Form("Releases", BoxLayout.y())
    form.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_REFRESH, lambda e: load(form))
    form.getContentPane().addPullToRefresh(lambda: load(form))
    form.show()
    load(form)


main()
// end::python-interop-python-002[]

// tag::python-interop-python-003[]
import sqlite3
from dataclasses import dataclass
from datetime import date

from java.com.codename1.components import MultiButton
from java.com.codename1.ui import Button, Dialog, FontImage, Form, TextArea, TextField
from java.com.codename1.ui.layouts import BorderLayout, BoxLayout


@dataclass
class Note:
    id: int | None
    title: str
    body: str
    day: str


class Notes:
    def __init__(self, path: str = "notes.db"):
        self.db = sqlite3.connect(path)
        self.db.execute("CREATE TABLE IF NOT EXISTS note"
                        " (id INTEGER PRIMARY KEY, title TEXT, body TEXT, day TEXT)")

    def all(self) -> list[Note]:
        rows = self.db.execute("SELECT id, title, body, day FROM note ORDER BY id DESC")
        return [Note(*row) for row in rows]

    def save(self, note: Note) -> None:
        with self.db:
            if note.id is None:
                self.db.execute("INSERT INTO note (title, body, day) VALUES (?, ?, ?)",
                                (note.title, note.body, note.day))
            else:
                self.db.execute("UPDATE note SET title = ?, body = ? WHERE id = ?",
                                (note.title, note.body, note.id))

    def delete(self, note: Note) -> None:
        with self.db:
            self.db.execute("DELETE FROM note WHERE id = ?", (note.id,))


class NotesApp:
    def __init__(self):
        self.store = Notes()
        self.home = Form("Notes", BoxLayout.y())
        self.home.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_ADD,
                                                            lambda e: self.edit(Note(None, "", "", str(date.today()))))
        self.refresh()

    def refresh(self):
        self.home.removeAll()
        for note in self.store.all():
            row = MultiButton(note.title or "Untitled")
            row.setTextLine2(note.day)
            row.addActionListener(lambda e, n=note: self.edit(n))
            self.home.add(row)
        self.home.revalidate()

    def edit(self, note: Note):
        form = Form("Edit", BorderLayout())
        title = TextField(note.title, "Title")
        body = TextArea(note.body, 8, 40)
        form.add(BorderLayout.NORTH, title).add(BorderLayout.CENTER, body)

        def back(e):
            note.title, note.body = title.getText(), body.getText()
            self.store.save(note)
            self.refresh()
            self.home.showBack()

        form.getToolbar().setBackCommand("", back)
        if note.id is not None:
            def delete(e):
                if Dialog.show("Delete", f"Delete '{note.title}'?", "Delete", "Cancel"):
                    self.store.delete(note)
                    self.refresh()
                    self.home.showBack()
            delete_button = Button("Delete")
            delete_button.addActionListener(delete)
            form.add(BorderLayout.SOUTH, delete_button)
        form.show()

    def start(self):
        self.home.show()


NotesApp().start()
// end::python-interop-python-003[]

// tag::python-interop-python-004[]
from flask import Flask, jsonify

app = Flask(__name__)


@app.get("/fib/<int:n>")
def fib(n):
    a, b = 0, 1
    for _ in range(n):
        a, b = b, a + b
    return jsonify({"n": n, "fib": str(a)})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
// end::python-interop-python-004[]

// tag::python-interop-python-005[]
M32 = 0xFFFFFFFF

def i32(v: int) -> int:
    return ((v + 0x80000000) & M32) - 0x80000000

def int_arithmetic() -> int:
    a: int = 0x12345678
    b: int = i32(0x9E3779B9)
    checksum: int = 0
    for _ in range(40000000):
        a = i32((a * 1103515245 + 12345) ^ ((b & M32) >> 3))
        b = i32((b + a) * 5 - (a << 7))
        checksum += (a ^ b) & 0xFFFF
    return checksum + a + b
// end::python-interop-python-005[]

// tag::python-interop-python-006[]
from java.com.codenameone.developerguide.python import CommonWorkloads

checksum = CommonWorkloads.intArithmetic()
// end::python-interop-python-006[]
