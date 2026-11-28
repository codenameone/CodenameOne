import re


class Detects(object):
    """Stores on self only, like bs4's DetectsXMLParsedAsHTML."""

    PREFIX: str = "<?xml"
    LOOKS = re.compile("<[^ +]html", re.I)
    _first: str
    count = 0

    @classmethod
    def looks_like(cls, markup):
        return markup.startswith(cls.PREFIX) or cls.LOOKS.search(markup) is not None

    def _init_detector(self):
        self._first = None
        self.count = 0

    def _seen(self, name):
        if self._first is None:
            self._first = name
        self.count += 1
