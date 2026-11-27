# Loops over literal names whose attribute name is a pure string
# transform of the loop variable, folded to a literal at build time.


class Box:
    def __init__(self, attrs):
        for name in ["CropBox", "TrimBox"]:
            # a comment between the header and the body
            if name in attrs:
                setattr(self, name.lower(), attrs[name])
        for t in ("rect", "line"):
            setattr(self, t + "s", [t])
        for u in ("a", "b"):
            setattr(self, "pre_" + u, (u + "x", 2 * len(u + "y")))

    def report(self):
        out = []
        for cs in ["cropbox", "trimbox", "rects", "lines"]:

            # blank and comment lines before the body
            if hasattr(self, cs):
                out.append((cs, getattr(self, cs)))
        for k in ("PRE_A", "PRE_B"):
            out.append(getattr(self, k.lower()))
        return out


print(Box({"CropBox": 1}).report())
print(Box({"CropBox": 1, "TrimBox": 2}).report())
