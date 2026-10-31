from jinja2 import Environment, DictLoader

templates = {
    "forms.html": (
        "{% macro input(name, value='', type='text', size=20) -%}\n"
        "<input type=\"{{ type }}\" name=\"{{ name }}\" value=\"{{ value|e }}\" size=\"{{ size }}\">\n"
        "{%- endmacro %}"
        "{% macro label(text) %}<label>{{ text }}</label>{% endmacro %}"
        "{% set version = '1.2' %}"
    ),
    "page.html": (
        "{% import 'forms.html' as forms %}"
        "{{ forms.input('user') }}|{{ forms.input('pw', type='password', size=8) }}|"
        "{{ forms.version }}"
    ),
    "from.html": (
        "{% from 'forms.html' import input as field, label %}"
        "{{ label('Name') }}{{ field('n', value='<b>') }}"
    ),
}

env = Environment(loader=DictLoader(templates))
print(env.get_template("page.html").render())
print(env.get_template("from.html").render())
aenv = Environment(loader=DictLoader(templates), autoescape=True)
print(aenv.get_template("from.html").render())

src = """{% macro listing(title) -%}
<h1>{{ title }}</h1>{% for a in varargs %}[{{ a }}]{% endfor %}{% for k, v in kwargs|dictsort %}{{ k }}={{ v }};{% endfor %}
{%- endmacro -%}
{{ listing('T', 1, 2, x=3, a='b') }}
{% macro wrap(cls='box') -%}
<div class="{{ cls }}">{{ caller() }}</div>
{%- endmacro -%}
{% call wrap() %}inner {{ who }}{% endcall %}
{% call wrap('big') %}second{% endcall %}
{% macro each(items) -%}
{% for i in items %}{{ caller(i, loop.index) }}{% endfor %}
{%- endmacro -%}
{% call(item, n) each(['a', 'b']) %}<{{ n }}:{{ item|upper }}>{% endcall %}
{% filter upper %}shout {{ who }}{% endfilter %}
{% filter replace('o', '0')|title %}hello world foo{% endfilter %}
{% set block_var %}captured {{ who }}{% endset %}{{ block_var }}
{% set trimmed | trim | upper %}   padded   {% endset %}[{{ trimmed }}]
{% for x in ['a', 'b', 'c', 'd'] %}{{ loop.cycle('odd', 'even') }}-{{ x }}{% if not loop.last %},{% endif %}{% endfor %}
{% for x in [1, 1, 2, 3, 3] %}{% if loop.changed(x) %}{{ x }}{% endif %}{% endfor %}
{% for x in 'xyz' %}{{ loop.index }}/{{ loop.revindex }}/{{ loop.length }}/{{ loop.depth }} {% endfor %}
{% set c = cycler('r', 'g', 'b') %}{% for i in range(4) %}{{ c.next() }}{% endfor %} {{ c.current }}
{{ listing.name }} {{ wrap.arguments }} {{ wrap.caller }} {{ listing.catch_varargs }}
"""
print(env.from_string(src).render(who="me"))
try:
    env.from_string("{% macro m(a) %}{{ a }}{% endmacro %}{{ m(1, 2) }}").render()
except TypeError as e:
    print("TypeError", e)
