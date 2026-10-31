# Text processing: string building, splitting, dict counting and sorting.
from _harness import measure

WORDS = ["alpha", "beta", "gamma", "delta", "epsilon", "zeta", "eta", "theta",
         "iota", "kappa", "lambda", "mu", "nu", "xi", "omicron", "pi"]


def work(lines):
    parts = []
    seed = 12345
    for i in range(lines):
        row = []
        for _ in range(12):
            seed = (seed * 1103515245 + 12345) & 0x7fffffff
            row.append(WORDS[(seed >> 16) % len(WORDS)])
        parts.append(" ".join(row))
    text = "\n".join(parts)
    counts = {}
    for line in text.split("\n"):
        for w in line.split():
            key = w.upper() if len(w) > 4 else w
            counts[key] = counts.get(key, 0) + 1
    top = sorted(counts.items(), key=lambda kv: (-kv[1], kv[0]))[:5]
    return ",".join("%s=%d" % kv for kv in top) + " " + str(len(text))


measure("dicts_strings", work, 160000, 5)
