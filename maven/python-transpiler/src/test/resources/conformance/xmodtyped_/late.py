"""A module that writes its own namespace: its globals stay unstable."""
value = 1


def setup():
    globals()["value"] = 42


def read():
    return value
