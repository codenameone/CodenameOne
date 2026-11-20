#!/usr/bin/env python3
#
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Use of this source code is governed by the license in the LICENSE file beside
# this project's pom.xml.
#
"""Writes every image and sound Papercut ships into common/src/main/resources.

    tools/make-art.py <downloads-dir>

<downloads-dir> holds the CC0 sources named in ART.md, unzipped as they come:

    Paper001/Paper001_1K-JPG_Color.jpg     ambientCG, the sheet the arena is drawn on
    Paper003/Paper003_1K-JPG_Color.jpg     ambientCG, crumpled paper for the menus
    Paper006/Paper006_1K-JPG_Color.jpg     ambientCG, kraft paper for the desk
    100-CC0-SFX_0/*.ogg                    rubberduck, OpenGameArt
    book_flips_-_starninjas/*.ogg          StarNinjas, OpenGameArt

The creatures are drawn here, from polygons: every cut-out is a flat colour
with the sheet's own fibre multiplied in, a white paper edge and a crease or
two. The game draws each one twice, the second time tinted black and offset,
which is the drop shadow that lifts it off the page -- so no image carries its
own shadow. Images that are tinted at run time (ink, scraps, bullets) are
drawn white.

Needs Pillow and ffmpeg. Run Python with -I when the downloads are untrusted.
"""
import math
import os
import random
import subprocess
import sys

from PIL import Image, ImageChops, ImageDraw, ImageFilter

SS = 4  # supersampling factor for smooth polygon edges
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'common', 'src', 'main', 'resources')

INK = (38, 44, 66)
PALETTE = {
    'quill': (250, 247, 238),
    'dart': (226, 74, 60),
    'pin_a': (244, 180, 52),
    'pin_b': (64, 160, 196),
    'pin_c': (232, 108, 140),
    'pin_d': (120, 186, 104),
    'crane': (140, 112, 196),
    'snip': (90, 98, 116),
    'snip_grip': (232, 96, 52),
    'eraser': (240, 150, 170),
    'drop': (40, 150, 170),
}


def fibre(src, size):
    """A grey fibre tile from a paper photo, centred on 255 so multiplying by it
    only darkens a little."""
    tex = Image.open(src).convert('L').resize((size, size), Image.LANCZOS)
    lo, hi = tex.getextrema()
    return tex.point(lambda v: int(225 + 30 * (v - lo) / max(1, hi - lo)))


class Cut:
    """One cut-out, drawn at SS x scale, then reduced."""

    def __init__(self, w, h, fib):
        self.w, self.h = w, h
        self.fill = Image.new('RGBA', (w * SS, h * SS), (0, 0, 0, 0))
        self.draw = ImageDraw.Draw(self.fill)
        self.mask = Image.new('L', (w * SS, h * SS), 0)
        self.mdraw = ImageDraw.Draw(self.mask)
        self.fib = fib

    def poly(self, pts, colour, edge=True):
        p = [(x * SS, y * SS) for x, y in pts]
        self.draw.polygon(p, fill=colour + (255,))
        self.mdraw.polygon(p, fill=255)
        if edge:
            self.draw.line(p + [p[0]], fill=(255, 255, 255, 255), width=int(1.6 * SS), joint='curve')

    def ellipse(self, box, colour):
        b = [v * SS for v in box]
        self.draw.ellipse(b, fill=colour + (255,))
        self.mdraw.ellipse(b, fill=255)

    def crease(self, a, b, alpha=70, dark=True):
        c = (0, 0, 0, alpha) if dark else (255, 255, 255, alpha)
        self.draw.line([(a[0] * SS, a[1] * SS), (b[0] * SS, b[1] * SS)], fill=c, width=int(1.2 * SS))

    def shade(self, pts, alpha=40, dark=True):
        """Darkens (or lightens) one face of a fold."""
        layer = Image.new('RGBA', self.fill.size, (0, 0, 0, 0))
        c = (0, 0, 0, alpha) if dark else (255, 255, 255, alpha)
        ImageDraw.Draw(layer).polygon([(x * SS, y * SS) for x, y in pts], fill=c)
        clip = Image.new('RGBA', self.fill.size, (0, 0, 0, 0))
        clip.paste(layer, (0, 0), self.mask)
        self.fill = Image.alpha_composite(self.fill, clip)
        self.draw = ImageDraw.Draw(self.fill)

    def save(self, name):
        img = self.fill.resize((self.w, self.h), Image.LANCZOS)
        r, g, b, a = img.split()
        f = self.fib.resize((self.w, self.h))
        rgb = ImageChops.multiply(Image.merge('RGB', (r, g, b)), Image.merge('RGB', (f, f, f)))
        out = Image.merge('RGBA', rgb.split() + (a,))
        out.save(os.path.join(OUT, name + '.png'), optimize=True)


def star(cx, cy, r_out, r_in, n, rot=0.0):
    pts = []
    for i in range(n * 2):
        r = r_out if i % 2 == 0 else r_in
        t = rot + math.pi * i / n
        pts.append((cx + r * math.cos(t), cy + r * math.sin(t)))
    return pts


def quill(fib, colour, name):
    # A paper plane seen from above, nose to the right (angle 0 in the game).
    c = Cut(96, 96, fib)
    nose, tail_l, tail_r, mid = (90, 48), (8, 10), (8, 86), (22, 48)
    c.poly([nose, tail_l, mid, tail_r], colour)
    c.shade([nose, tail_l, mid], 34)
    c.crease(nose, mid, 110)
    c.crease((60, 48), (16, 30), 50, dark=False)
    c.save(name)


def dart(fib):
    c = Cut(64, 64, fib)
    c.poly([(60, 32), (6, 6), (20, 32), (6, 58)], PALETTE['dart'])
    c.shade([(60, 32), (20, 32), (6, 58)], 45)
    c.crease((60, 32), (20, 32), 90)
    c.save('e_dart')


def pinwheel(fib):
    c = Cut(96, 96, fib)
    cols = [PALETTE['pin_a'], PALETTE['pin_b'], PALETTE['pin_c'], PALETTE['pin_d']]
    for i in range(4):
        t = i * math.pi / 2
        def p(r, a):
            return (48 + r * math.cos(t + a), 48 + r * math.sin(t + a))
        blade = [(48, 48), p(44, 0), p(30, math.pi / 4 + 0.25)]
        c.poly(blade, cols[i])
        c.shade([(48, 48), p(44, 0), p(22, 0.32)], 40)
    c.ellipse((43, 43, 53, 53), (250, 246, 236))
    c.save('e_pinwheel')


def crane(fib, size, name):
    # Origami crane from above, head to the right.
    c = Cut(size, size, fib)
    s = size / 112.0
    def P(x, y):
        return (x * s, y * s)
    col = PALETTE['crane']
    c.poly([P(30, 4), P(72, 50), P(56, 58), P(36, 50)], col)        # upper wing, swept back
    c.poly([P(30, 108), P(72, 62), P(56, 54), P(36, 62)], col)      # lower wing
    c.shade([P(30, 4), P(72, 50), P(56, 58)], 40)
    c.shade([P(30, 108), P(36, 62), P(56, 54)], 40)
    c.poly([P(10, 50), P(56, 46), P(56, 66), P(10, 62)], col)       # tail
    c.poly([P(56, 46), P(98, 50), P(108, 40), P(100, 58), P(56, 66)], col)  # neck and head
    c.crease(P(14, 56), P(100, 54), 80)
    c.save(name)


def snipper(fib, opened, name):
    # Scissors from above, points to the right.
    c = Cut(96, 96, fib)
    a = 0.32 if opened else 0.06
    for sgn in (-1, 1):
        t = sgn * a
        def R(x, y):
            dx, dy = x - 44, y - 48
            return (44 + dx * math.cos(t) - dy * math.sin(t), 48 + dx * math.sin(t) + dy * math.cos(t))
        c.poly([R(42, 48 - sgn * 7), R(94, 48 - sgn * 1), R(42, 48 + sgn * 2)], PALETTE['snip'])
        c.crease(R(46, 48), R(90, 48), 60, dark=False)
        gx, gy = R(22, 48 + sgn * 12)
        c.ellipse((gx - 15, gy - 11, gx + 15, gy + 11), PALETTE['snip_grip'])
        c.ellipse((gx - 8, gy - 5, gx + 8, gy + 5), (250, 246, 236))
    c.ellipse((40, 44, 48, 52), (220, 220, 226))
    c.save(name)


def blot(seed, size, spikes, name, colour=(255, 255, 255), droplets=True):
    """An ink splat, white so the game can tint it."""
    rnd = random.Random(seed)
    img = Image.new('L', (size * SS, size * SS), 0)
    d = ImageDraw.Draw(img)
    cx = cy = size * SS / 2
    base = size * SS * 0.26
    pts = []
    n = 48
    lobes = [(rnd.uniform(0, 2 * math.pi), rnd.uniform(0.2, 0.6)) for _ in range(spikes)]
    for i in range(n):
        t = 2 * math.pi * i / n
        r = base * (1 + 0.12 * math.sin(t * 3 + seed))
        for lt, amp in lobes:
            dt = math.atan2(math.sin(t - lt), math.cos(t - lt))
            r += base * amp * math.exp(-(dt * 6) ** 2)
        pts.append((cx + r * math.cos(t), cy + r * math.sin(t)))
    d.polygon(pts, fill=255)
    if droplets:
        for _ in range(spikes + 4):
            t = rnd.uniform(0, 2 * math.pi)
            r = base * rnd.uniform(1.5, 1.95)
            dr = size * SS * rnd.uniform(0.012, 0.04)
            x, y = cx + r * math.cos(t), cy + r * math.sin(t)
            d.ellipse((x - dr, y - dr, x + dr, y + dr), fill=255)
    img = img.filter(ImageFilter.GaussianBlur(SS * 0.6)).resize((size, size), Image.LANCZOS)
    out = Image.new('RGBA', (size, size), colour + (0,))
    out.putalpha(img)
    out.save(os.path.join(OUT, name + '.png'), optimize=True)


def inkwell(fib):
    # The gravity well: a living blot of ink with a highlight, drawn in colour.
    blot(11, 128, 7, 'e_inkwell', colour=INK, droplets=False)
    img = Image.open(os.path.join(OUT, 'e_inkwell.png'))
    d = ImageDraw.Draw(img)
    d.ellipse((46, 40, 62, 52), fill=(120, 130, 170, 160))
    img.save(os.path.join(OUT, 'e_inkwell.png'), optimize=True)


def simple(name, w, h, drawfn):
    img = Image.new('RGBA', (w * SS, h * SS), (0, 0, 0, 0))
    drawfn(ImageDraw.Draw(img), SS)
    img.resize((w, h), Image.LANCZOS).save(os.path.join(OUT, name + '.png'), optimize=True)


def scraps():
    # Torn paper bits, white: the game tints them to whatever burst.
    simple('fx_scrap', 24, 24, lambda d, s: d.polygon([(2 * s, 5 * s), (21 * s, 2 * s), (16 * s, 21 * s), (6 * s, 17 * s)], fill=(255, 255, 255, 255)))
    simple('fx_dot', 16, 16, lambda d, s: d.ellipse((1 * s, 1 * s, 15 * s, 15 * s), fill=(255, 255, 255, 255)))
    simple('fx_px', 4, 4, lambda d, s: d.rectangle((0, 0, 4 * s, 4 * s), fill=(255, 255, 255, 255)))
    # The pencil stroke the quill fires: graphite with a lighter core.
    def stroke(d, s):
        d.rounded_rectangle((1 * s, 2 * s, 31 * s, 10 * s), radius=4 * s, fill=(255, 255, 255, 255))
    simple('fx_stroke', 32, 12, stroke)
    # A ring, for shockwaves and the eraser's sweep.
    simple('fx_ring', 128, 128, lambda d, s: d.ellipse((4 * s, 4 * s, 124 * s, 124 * s), outline=(255, 255, 255, 255), width=5 * s))


def drop(fib):
    c = Cut(40, 40, fib)
    c.poly([(20, 3)] + [(20 + 12 * math.cos(t), 25 + 12 * math.sin(t)) for t in [i * math.pi / 10 for i in range(-1, 12)]], PALETTE['drop'])
    c.crease((15, 21), (17, 30), 90, dark=False)
    c.save('p_drop')


def eraser(fib):
    c = Cut(56, 40, fib)
    c.poly([(4, 10), (40, 4), (52, 26), (16, 34)], PALETTE['eraser'])
    c.poly([(30, 6), (40, 4), (52, 26), (42, 28)], (90, 120, 200))
    c.save('p_eraser')


def tiles(src_dir):
    for name, src, size in (('tex_sheet', 'Paper001/Paper001_1K-JPG_Color.jpg', 512),
                            ('tex_crumple', 'Paper003/Paper003_1K-JPG_Color.jpg', 512),
                            ('tex_kraft', 'Paper006/Paper006_1K-JPG_Color.jpg', 512)):
        Image.open(os.path.join(src_dir, src)).convert('RGB').resize((size, size), Image.LANCZOS) \
            .save(os.path.join(OUT, name + '.jpg'), quality=86)


SOUNDS = {
    'sfx_shot': '100-CC0-SFX_0/paper_02.ogg',
    'sfx_pop': '100-CC0-SFX_0/plop_01.ogg',
    'sfx_pop2': '100-CC0-SFX_0/plop_02.ogg',
    'sfx_splash': '100-CC0-SFX_0/splash_01.ogg',
    'sfx_tear': '100-CC0-SFX_0/paper_04.ogg',
    'sfx_hit': '100-CC0-SFX_0/hit_02.ogg',
    'sfx_boom': '100-CC0-SFX_0/explosion.ogg',
    'sfx_bell': '100-CC0-SFX_0/bell_01.ogg',
    'sfx_spring': '100-CC0-SFX_0/spring_03.ogg',
    'sfx_flip': 'book_flips_-_starninjas/book_flip.3.ogg',
    'sfx_flip2': 'book_flips_-_starninjas/book_flip.7.ogg',
}


def sounds(src_dir):
    for name, src in SOUNDS.items():
        subprocess.check_call(['ffmpeg', '-v', 'error', '-y', '-i', os.path.join(src_dir, src),
                               '-ac', '1', '-ar', '22050', '-t', '1.5', '-sample_fmt', 's16',
                               os.path.join(OUT, name + '.wav')])


def icon(fib, path, size=512):
    bg = Image.open(os.path.join(OUT, 'tex_kraft.jpg')).resize((size, size)).convert('RGBA')
    quill(fib, PALETTE['quill'], '_icon_tmp')
    q = Image.open(os.path.join(OUT, '_icon_tmp.png')).resize((int(size * 0.78),) * 2, Image.LANCZOS).rotate(28, Image.BICUBIC, expand=False)
    os.remove(os.path.join(OUT, '_icon_tmp.png'))
    splat = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    blot(5, size, 6, '_blot_tmp', colour=PALETTE['dart'])
    b = Image.open(os.path.join(OUT, '_blot_tmp.png'))
    os.remove(os.path.join(OUT, '_blot_tmp.png'))
    splat.paste(b, (int(size * 0.12), int(size * 0.14)), b)
    bg = Image.alpha_composite(bg, splat)
    shadow = Image.new('RGBA', q.size, (0, 0, 0, 0))
    shadow.putalpha(q.split()[3].point(lambda v: v * 90 // 255))
    off = ((size - q.width) // 2, (size - q.height) // 2)
    bg.alpha_composite(shadow, (off[0] + 14, off[1] + 20))
    bg.alpha_composite(q, off)
    bg.convert('RGB').save(path)


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    src = sys.argv[1]
    os.makedirs(OUT, exist_ok=True)
    fib = fibre(os.path.join(src, 'Paper001/Paper001_1K-JPG_Color.jpg'), 256)
    tiles(src)
    quill(fib, PALETTE['quill'], 'quill')
    dart(fib)
    pinwheel(fib)
    crane(fib, 112, 'e_crane')
    crane(fib, 64, 'e_cranelet')
    snipper(fib, True, 'e_snipper_open')
    snipper(fib, False, 'e_snipper_shut')
    inkwell(fib)
    drop(fib)
    eraser(fib)
    scraps()
    for i in range(6):
        blot(100 + i, 128, 4 + i % 4, 'ink_%d' % i)
    sounds(src)
    icon(fib, os.path.join(HERE, '..', 'common', 'icon.png'))


if __name__ == '__main__':
    main()
