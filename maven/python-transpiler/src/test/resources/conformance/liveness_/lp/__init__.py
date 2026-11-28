from importlib import import_module


def backend(name):
    return import_module(f"lp.backends._{name}")


def __getattr__(name):
    if name == "late":
        return import_module(".lazyattr", __name__).LATE
    raise AttributeError(name)
