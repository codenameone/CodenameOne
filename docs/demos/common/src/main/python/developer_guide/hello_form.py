# Developer guide, Python chapter. Transpiled and compiled with the demos
# so the guide's code cannot drift from what builds.

# tag::python-interop-python-001[]
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
# end::python-interop-python-001[]
