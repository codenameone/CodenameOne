"""A Codename One user interface written in Python.

The Form, the components and the layouts are the Codename One API itself,
imported from Java; a Python function passed where the API takes a
listener becomes that listener.
"""
from java.com.codename1.ui import Form, Button, Label, TextField, Dialog
from java.com.codename1.ui.layouts import BoxLayout


def greeting(name):
    name = name.strip()
    return f"Hello, {name}!" if name else "Hello, world!"


def main():
    form = Form("Hello Python", BoxLayout.y())
    name = TextField("", "Your name")
    out = Label(greeting(""))
    count = [0]

    def on_greet(event):
        count[0] += 1
        out.setText(greeting(name.getText()))
        form.revalidate()

    button = Button("Greet")
    button.addActionListener(on_greet)
    about = Button("About")
    about.addActionListener(lambda e: Dialog.show("About", "Written in Python", "OK", None))
    form.add(name).add(button).add(out).add(about)
    form.show()


main()
