# Papercut art, sound and type

Papercut is drawn as cut paper on a school notebook. The creatures, the quill,
the ink and the scraps are drawn by `tools/make-art.py` from polygons, in this
project's own style; what gives them the feel of paper is real paper, photographed,
multiplied into every cut-out. Everything that came from somewhere else is
listed here.

| What | Source | Author | Licence |
|---|---|---|---|
| The page (`tex_sheet.jpg`) and the paper fibre in every cut-out | Paper 001 | ambientCG (ambientcg.com) | CC0 1.0 |
| Crumpled paper (`tex_crumple.jpg`) | Paper 003 | ambientCG (ambientcg.com) | CC0 1.0 |
| The desk (`tex_kraft.jpg`) | Paper 006 | ambientCG (ambientcg.com) | CC0 1.0 |
| Pencil, plops, splashes, tear, hit, bell, spring, burst (`sfx_*.wav` except the flips) | 100 CC0 SFX | rubberduck (opengameart.org/content/100-cc0-sfx) | CC0 1.0 |
| Page flips (`sfx_flip.wav`, `sfx_flip2.wav`) | 10 Book Page Flips | StarNinjas (opengameart.org/content/10-book-page-flips) | CC0 1.0 |
| Creatures, quill, ink drops, eraser, ink stains, scraps, icon | drawn by `tools/make-art.py` | this project | as the sample |

CC0 asks for no credit. We give it anyway.

The type is **Gochi Hand** by Juan Pablo del Peral (Huerta Tipografica) for
numbers and titles, and **Patrick Hand** by Patrick Wagesreiter for everything
else, both under the SIL Open Font License 1.1 and both from Google Fonts. The
licences are beside the font files in `common/src/main/css/fonts/OFL.txt`.

## Re-making or re-skinning

1. Download into one directory, each unzipped where it lands:
   - ambientCG `Paper001`, `Paper003` and `Paper006`, 1K-JPG
     (`https://ambientcg.com/get?file=Paper001_1K-JPG.zip`, and so on);
   - `100-CC0-SFX_0.zip` and `book_flips_-_starninjas.zip` from the two
     OpenGameArt pages above.
2. Run `python3 -I tools/make-art.py <that directory>`. It needs Pillow and
   ffmpeg, and rewrites every image and sound in
   `common/src/main/resources` and the app icon.

The colours of the creatures are `PALETTE` at the top of the script; the game
uses the same colours for their bursts and ink stains, in `Enemy.COLOUR`.
Change both together. A creature's shape is one function in the script, and its
size on the page is `Enemy.RADIUS`.

The images tinted at run time -- ink stains, scraps, pencil strokes, rings --
are drawn white on purpose. The game draws every cut-out a second time, tinted
black and offset, as its shadow, so no image carries a shadow of its own.
