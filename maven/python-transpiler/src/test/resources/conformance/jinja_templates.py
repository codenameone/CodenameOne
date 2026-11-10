from jinja2 import Environment, DictLoader, Template, select_autoescape
from markupsafe import Markup, escape

print(Template("Hello {{ name }}!").render(name="World"))
print(Template("{{ x }}|{{ y }}|{{ n }}|{{ missing }}|").render(x=None, y=True, n=3.5))
print(Template("{{ 'a' ~ 1 ~ 'b' }} {{ 2 + 3 * 4 }} {{ 7 // 2 }} {{ 7 % 3 }} {{ -n }}").render(n=4))
print(Template("{{ s|upper }} {{ s|lower }} {{ s|title }} {{ s|length }} {{ xs|join(', ') }}").render(
    s="hello wORLD", xs=[1, 2, 3]))
print(Template("{{ m|default('none') }} {{ e|default('empty', true) }} {{ v|d('x') }}").render(e="", v=0))
print(Template("{% if n > 5 %}big{% elif n > 2 %}mid{% else %}small{% endif %}").render(n=3))
print(Template("{% if a and not b %}yes{% endif %}{% if 'x' in s %}+{% endif %}{% if k is defined %}D{% else %}U{% endif %}").render(
    a=1, b=0, s="xyz"))

loop_t = Template("""{% for x in xs -%}
{{ loop.index }}/{{ loop.length }}:{{ x }}{% if loop.first %} first{% endif %}{% if loop.last %} last{% endif %}
{% else %}empty
{% endfor %}""")
print(loop_t.render(xs=["a", "b", "c"]))
print(loop_t.render(xs=[]))
print(Template("{% for k, v in d.items() %}{{ k }}={{ v }};{% endfor %}").render(d={"a": 1, "b": 2}))
print(Template("{% for x in xs if x % 2 %}{{ x }}{{ loop.cycle('a', 'b') }} {% endfor %}").render(xs=range(7)))
print(Template("{% set total = xs|length * 2 %}{{ total }} {% set a, b = 1, 2 %}{{ a + b }}").render(xs=[1, 2]))
print(Template("A{# a comment #}B  {%- if true %}  C  {%- endif %}  D").render())
print(Template("{{ user.name }} {{ user['age'] }} {{ items[1] }} {{ 'abc'.upper() }}").render(
    user={"name": "Ann", "age": 30}, items=[5, 6]))
print(Template("{{ 'yes' if flag else 'no' }} {{ [1, 2] + [3] }} {{ (1, 2) }}").render(flag=False))

env = Environment(autoescape=True)
print(env.from_string("{{ v }} {{ v|safe }} {{ v|e }} {{ m }}").render(v="<b>&'\"</b>", m=Markup("<i>ok</i>")))
print(env.from_string("{{ xs|join('<br>') }}").render(xs=["<a>", Markup("<b>")]))
print(Template("{{ v }}").render(v="<raw>"))
print(escape("<x>"), escape(Markup("<y>")), Markup("<p>%s</p>") % "<z>")

env = Environment(loader=DictLoader({
    "base.html": "<title>{% block title %}Base{% endblock %}</title>\n<main>{% block body %}{% endblock %}</main>\n",
    "child.html": "{% extends 'base.html' %}{% block title %}Child - {{ super() }}{% endblock %}"
                  "{% block body %}{% include 'part.html' %} {{ who }}{% endblock %}",
    "part.html": "<p>part {{ who }}</p>",
    "plain.txt": "{{ who }}",
}), autoescape=select_autoescape(["html"]))
print(env.get_template("child.html").render(who="<me>"))
print(env.get_template("plain.txt").render(who="<me>"))
try:
    env.get_template("nope.html")
except Exception as e:
    print(type(e).__name__, e)
