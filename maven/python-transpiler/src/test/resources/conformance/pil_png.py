# Pillow PNG: decoding files Pillow wrote (every filter, gray, gray+alpha,
# RGB, RGBA, palette with and without transparency, 1-bit) and round trips
# through BytesIO and a file path.
import hashlib
import io
import os

from PIL import Image

PNGS = {
    'rgb': bytes.fromhex(
        '89504e470d0a1a0a0000000d494844520000000d0000000908020000006618313d000000'
        '6649444154789c85ccc10a80201084e11115a925242aa24b1719e8d2a5f77fb82831a4ac'
        'e03b0ceccf2ac051811ab4a0032b50c006f4600b76e0008e30a221ca8afe6152577f3359'
        'e73fdcba3e621a9767371515bb39b3c551e8781cc2694d23bcfd0ba297dc0ee85a0f3336'
        '0aa31f0000000049454e44ae426082'
    ),
    'rgba': bytes.fromhex(
        '89504e470d0a1a0a0000000d494844520000000d000000090806000000e97aa66a000000'
        '7949444154789c8dccc10ac2301084e1096b29ba0ca5a88887f55206bc78f1fd1faea442'
        '08d6480e1fd985fd9380114aa00cd4006a047504e5a0086a0235833a83ba82ba81073784'
        'a7816efdaae8d4ed2b9abafc882e85aab9d688ee2d91df3fd1a3166eefb237236d074bb8'
        '2d747be5b96845f1f9351f3d77561def15d7edd13cae0000000049454e44ae426082'
    ),
    'l': bytes.fromhex(
        '89504e470d0a1a0a0000000d494844520000000d000000090800000000cc11f9b6000000'
        '5a49444154789c25cbcb0980301444510ce67d66a22228c1fedb08b8b31aed415eb23ccc'
        'dc69e2b26efbfe1c67ad57ca226ae60e824c5954d50c004b49b9d3636489a7983547688e'
        '6e849f623c3b7a1768e62fc83b36510d80e5071a4f0ab3e941397c0000000049454e44ae'
        '426082'
    ),
    'la': bytes.fromhex(
        '89504e470d0a1a0a0000000d494844520000000d00000009080400000043736ee1000000'
        '7749444154789c4d8e5d0a833010062bd4c4fc4c4550d4c2deff1285bef534f60eb29b44'
        'e4639f8699a4eb1e0978313231f16366616565e7d98bc3e11918080422c92e15e46d0aa3'
        '2d91c98a7ab3746a154f610d3a733e2cd5baa1162cd61f2ff11ebc80bcdb5b0d682ec8c1'
        '6ec16fb30c4be060ab9fcf9c31cc11aa5b8c049e0000000049454e44ae426082'
    ),
    'p': bytes.fromhex(
        '89504e470d0a1a0a0000000d49484452000000070000000504030000007bb4ebeb000000'
        '30504c5445f3295ada2252e42616c61d3bb91907aa162f981325850f25700d1e5b09164b'
        '09133b07111d172f23050a18020700000770e3c8270000002449444154789c011900e6ff'
        '00fda8642002fef0ff0002f0fff0e002efefde000200efdfc0b15d0e284316c091000000'
        '0049454e44ae426082'
    ),
    'pt': bytes.fromhex(
        '89504e470d0a1a0a0000000d49484452000000070000000504030000007bb4ebeb000000'
        '30504c5445f3295ada2252e42616c61d3bb91907aa162f981325850f25700d1e5b09164b'
        '09133b07111d172f23050a18020700000770e3c8270000000474524e53ffffff00402aa9'
        'f40000002449444154789c011900e6ff00fda8642002fef0ff0002f0fff0e002efefde00'
        '0200efdfc0b15d0e284316c0910000000049454e44ae426082'
    ),
    'bit': bytes.fromhex(
        '89504e470d0a1a0a0000000d494844520000000900000004010000000074742863000000'
        '0b49444154789c6360400000000c00017c4c44c20000000049454e44ae426082'
    ),
}


def h(im):
    return hashlib.sha256(im.tobytes()).hexdigest()[:16]


for k in sorted(PNGS):
    im = Image.open(io.BytesIO(PNGS[k]))
    rgba = im.convert("RGBA")
    print(k, im.size, im.format, h(rgba), rgba.getpixel((1, 1)), rgba.getpixel((im.width - 1, im.height - 1)))

src = Image.open(io.BytesIO(PNGS["rgba"]))
for mode in ("L", "RGB", "RGBA"):
    im = src.convert(mode)
    buf = io.BytesIO()
    im.save(buf, "PNG")
    back = Image.open(io.BytesIO(buf.getvalue()))
    print(mode, back.mode, back.size, back.tobytes() == im.tobytes(), h(back))

path = "pil_png_conformance_tmp.png"
src.save(path)
with Image.open(path) as again:
    print("path", again.mode, again.size, again.tobytes() == src.tobytes())
os.remove(path)

try:
    Image.open(io.BytesIO(b"not an image at all"))
except Exception as e:
    print(type(e).__name__, isinstance(e, OSError))
