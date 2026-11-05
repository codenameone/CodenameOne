# Developer guide, Python chapter. Transpiled and compiled with the demos
# so the guide's code cannot drift from what builds.

# tag::python-interop-python-003[]
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
# end::python-interop-python-003[]
