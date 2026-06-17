"""
Render Google Play 512x512 hi-res icon from the adaptive-icon vector drawables
(`ic_launcher_background.xml` + `ic_launcher_foreground.xml`).

Rendered at 4x oversampling then downscaled with LANCZOS for clean antialiasing.
Output: app/src/main/res/market/ic_launcher_play_512.png
"""
from PIL import Image, ImageDraw
import math
import os

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(REPO, "app", "src", "main", "res", "market", "ic_launcher_play_512.png")

SIZE = 512
VP = 108
OVER = 4
RENDER = SIZE * OVER
SCALE = RENDER / VP


def s(v):
    return v * SCALE


def hexa(h):
    h = h.lstrip("#")
    if len(h) == 8:
        a, r, g, b = (int(h[i:i + 2], 16) for i in (0, 2, 4, 6))
        return (r, g, b, a)
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def lerp(a, b, t):
    return int(a + (b - a) * t)


img = Image.new("RGBA", (RENDER, RENDER), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# ---------- Background: vertical linear gradient #BEE6FF -> #6FB7DD ----------
top = hexa("#BEE6FF")
bot = hexa("#6FB7DD")
for y in range(RENDER):
    t = y / (RENDER - 1)
    draw.line(
        [(0, y), (RENDER - 1, y)],
        fill=(lerp(top[0], bot[0], t), lerp(top[1], bot[1], t), lerp(top[2], bot[2], t), 255),
    )

# ---------- Cloud ellipses (white 20% alpha) ----------
cloud = hexa("#33FFFFFF")
layer = Image.new("RGBA", (RENDER, RENDER), (0, 0, 0, 0))
ld = ImageDraw.Draw(layer)
ld.ellipse([s(16 - 10), s(22 - 7), s(16 + 10), s(22 + 7)], fill=cloud)
ld.ellipse([s(92 - 9), s(86 - 6), s(92 + 9), s(86 + 6)], fill=cloud)
img = Image.alpha_composite(img, layer)

# ---------- Star glow (yellow ~33% alpha disk) ----------
glow = hexa("#55FFE08A")
layer = Image.new("RGBA", (RENDER, RENDER), (0, 0, 0, 0))
ld = ImageDraw.Draw(layer)
ld.ellipse([s(74 - 22), s(38 - 22), s(74 + 22), s(38 + 22)], fill=glow)
img = Image.alpha_composite(img, layer)

# ---------- Star (filled + stroked) ----------
star_pts = [
    (74, 24), (77.5, 33.2), (87.3, 33.7), (79.7, 39.9), (82.2, 49.3),
    (74, 44), (65.8, 49.3), (68.3, 39.9), (60.7, 33.7), (70.5, 33.2),
]
star_scaled = [(s(x), s(y)) for x, y in star_pts]
layer = Image.new("RGBA", (RENDER, RENDER), (0, 0, 0, 0))
ld = ImageDraw.Draw(layer)
ld.polygon(star_scaled, fill=hexa("#FFCB47"), outline=hexa("#D89A1F"), width=max(1, int(round(s(1.8)))))
img = Image.alpha_composite(img, layer)

# ---------- Ball drop shadow (black 20% alpha disk) ----------
shadow = hexa("#33000000")
layer = Image.new("RGBA", (RENDER, RENDER), (0, 0, 0, 0))
ld = ImageDraw.Draw(layer)
ld.ellipse([s(46 - 24), s(68 - 24), s(46 + 24), s(68 + 24)], fill=shadow)
img = Image.alpha_composite(img, layer)

# ---------- Ball body: circle filled with radial gradient white(38,52) -> red ----------
ball_cx, ball_cy = s(46), s(60)
ball_r = s(24)
grad_cx, grad_cy = s(38), s(52)
grad_r = s(36)
start_c = (255, 255, 255)
end_c = (232, 69, 69)

ball_layer = Image.new("RGBA", (RENDER, RENDER), (0, 0, 0, 0))
px = ball_layer.load()
x0 = max(0, int(ball_cx - ball_r - 3))
x1 = min(RENDER, int(ball_cx + ball_r + 3))
y0 = max(0, int(ball_cy - ball_r - 3))
y1 = min(RENDER, int(ball_cy + ball_r + 3))
for y in range(y0, y1):
    for x in range(x0, x1):
        d = math.hypot(x - grad_cx, y - grad_cy)
        t = min(1.0, d / grad_r)
        px[x, y] = (lerp(start_c[0], end_c[0], t), lerp(start_c[1], end_c[1], t), lerp(start_c[2], end_c[2], t), 255)

# Antialiased circular mask
mask = Image.new("L", (RENDER, RENDER), 0)
ImageDraw.Draw(mask).ellipse(
    [ball_cx - ball_r, ball_cy - ball_r, ball_cx + ball_r, ball_cy + ball_r], fill=255
)
ball_layer.putalpha(mask)
img = Image.alpha_composite(img, ball_layer)

# ---------- Ball outline (#B53636, width 2) ----------
layer = Image.new("RGBA", (RENDER, RENDER), (0, 0, 0, 0))
ld = ImageDraw.Draw(layer)
ld.ellipse(
    [ball_cx - ball_r, ball_cy - ball_r, ball_cx + ball_r, ball_cy + ball_r],
    outline=hexa("#B53636"),
    width=max(1, int(round(s(2)))),
)
img = Image.alpha_composite(img, layer)

# ---------- Ball top-left highlight ----------
hl_cx, hl_cy, hl_r = s(38.5), s(52), s(5.5)
layer = Image.new("RGBA", (RENDER, RENDER), (0, 0, 0, 0))
ld = ImageDraw.Draw(layer)
ld.ellipse([hl_cx - hl_r, hl_cy - hl_r, hl_cx + hl_r, hl_cy + hl_r], fill=hexa("#F0FFFFFF"))
img = Image.alpha_composite(img, layer)

# ---------- Downscale to 512x512 (LANCZOS) and write as opaque PNG ----------
final_rgba = img.resize((SIZE, SIZE), Image.LANCZOS)
# Play Store requires 32-bit PNG; flatten onto opaque background to remove alpha (Play rejects transparency on hi-res icon)
final = Image.new("RGB", (SIZE, SIZE), (190, 230, 255))  # match gradient top color so any rounding bleed is invisible
final.paste(final_rgba, mask=final_rgba.split()[-1])

os.makedirs(os.path.dirname(OUT), exist_ok=True)
final.save(OUT, "PNG", optimize=True)
print(f"wrote {OUT} ({os.path.getsize(OUT)} bytes)")
