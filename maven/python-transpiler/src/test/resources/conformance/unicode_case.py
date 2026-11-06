# Case mapping and character classes beyond ASCII, which a device's
# java.lang.Character (CLDC: Latin-1 at most) cannot answer.
words = ["M\u00dcNCHEN", "stra\u00dfe", "\u0391\u0392\u0393 \u03b1\u03b2\u03b3",
         "\u041f\u0440\u0438\u0432\u0435\u0442", "\u01c5ungla", "\u0130stanbul", "\uff21\uff42\uff43"]
for w in words:
    print(ascii(w.lower()), ascii(w.upper()), ascii(w.casefold()), ascii(w.title()),
          ascii(w.swapcase()), ascii(w.capitalize()))
    print(w.isupper(), w.islower(), w.isalpha(), w.istitle(), w.isidentifier())
for s in ["\u0663\u0664", "\u00b2", "\u2167", "\u00bd", "\u3000\u2003", "\u00a0", "abc\u0661"]:
    print(ascii(s), s.isdigit(), s.isdecimal(), s.isnumeric(), s.isspace(), s.isalnum())
