# JSON: decode an API-style document and walk it, the work behind every HTTP
# client and LLM SDK (the OpenAI responses `llm` reads are JSON). The result
# also folds in the decoder's edge cases, so a fast path that differs from
# CPython changes the answer, not only the time.
import json
from _harness import measure


def edge_cases():
    out = []
    out.append(json.loads('{"a": 1, "b": 2, "a": 3}'))
    out.append(json.loads("[NaN, Infinity, -Infinity, 1e999, -0, -0.0]"))
    out.append(json.loads("123456789012345678901234567890"))
    out.append(json.loads('"\\ud83d\\ude00 \\u00e9 \\n"'))
    out.append(json.loads('{"x": {"y": [1, 2]}}', object_pairs_hook=lambda p: ["pairs"] + p))
    out.append(json.loads('{"k": 1}', object_hook=lambda d: sorted(d.items())))
    out.append(json.loads('"a\tb"', strict=False))
    for bad in ('{"a" 1}', "[1, 2", '"a\tb"', "01", "[1,]", ""):
        try:
            json.loads(bad)
            out.append("accepted " + bad)
        except json.JSONDecodeError as e:
            out.append(str(e))
    class S(str):
        pass

    class I(int):
        pass

    class Box:
        def __init__(self, v):
            self.v = v

    cyc = []
    cyc.append([cyc])
    out.append(json.dumps({"b": [1, 2.5, None, True, (3, "x")], "a": {"\u00e9\U0001f600": float("nan")},
                           3: 1e16, 2.5: -0.0, None: False, S("s"): I(7)}))
    out.append(json.dumps({"z": 1, "a": [1, {"y": 2, "b": 3}], "\U0001f600": 0, "\ue000": 1}, sort_keys=True,
                          indent=2, ensure_ascii=False))
    out.append(json.dumps([Box(1), {"k": Box(2)}], default=lambda o: {"box": o.v}, separators=(",", ":")))
    out.append(json.dumps("\x7f\x00\t\"\\", ensure_ascii=True))
    for bad in (lambda: json.dumps(cyc), lambda: json.dumps(float("inf"), allow_nan=False),
                lambda: json.dumps({(1, 2): 3}), lambda: json.dumps(object()), lambda: json.dumps({1: 2, "a": 3}, sort_keys=True)):
        try:
            out.append(bad())
        except (ValueError, TypeError) as e:
            out.append(type(e).__name__ + ": " + str(e).split(" at 0x")[0])
    out.append(json.dumps({(1, 2): 3, "k": 1}, skipkeys=True))
    return repr(out)


def work(n):
    items = []
    for i in range(n):
        items.append({"id": i, "object": "chat.completion", "model": "gpt-4o-mini",
                      "choices": [{"index": 0, "message": {"role": "assistant",
                                                           "content": "Answer number %d é" % i},
                                   "finish_reason": "stop"}],
                      "usage": {"prompt_tokens": i % 97, "completion_tokens": i % 13, "total": i * 1.5}})
    doc = json.dumps({"data": items, "has_more": False})
    total = 0
    chars = 0
    for it in json.loads(doc)["data"]:
        total += it["usage"]["prompt_tokens"] + it["usage"]["completion_tokens"]
        chars += len(it["choices"][0]["message"]["content"])
        if it["usage"]["total"] != it["id"] * 1.5:
            total -= 1
    return "%d %d %d %s" % (total, chars, len(doc), edge_cases())


measure("json_codec", work, 20000, 5)
