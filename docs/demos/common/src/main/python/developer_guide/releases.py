# Developer guide, Python chapter. Transpiled and compiled with the demos
# so the guide's code cannot drift from what builds.

# tag::python-interop-python-002[]
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
# end::python-interop-python-002[]
