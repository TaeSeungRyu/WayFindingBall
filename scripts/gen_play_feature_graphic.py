"""
Render Google Play feature graphic (1024x500 PNG) for 또르르 미로.

Reuses the icon's visual language (sky gradient, clouds, red ball, gold star)
plus a dashed maze path connecting the ball to the goal, with the app title
in the Jua typeface that ships with the app.
"""
from PIL import Image, ImageDraw, ImageFont
import math
import os

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONT = os.path.join(REPO, "app", "src", "main", "res", "font", "jua.ttf")
OUT = os.path.join(REPO, "app", "src", "main", "res", "market", "feature_graphic_1024x500.png")

W, H = 1024, 500
OVER = 2
RW, RH = W * OVER, H * OVER


def hexa(h):
    h = h.lstrip("#")
    if len(h) == 8:
        a, r, g, b = (int(h[i:i + 2], 16) for i in (0, 2, 4, 6))
        return (r, g, b, a)
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def lerp(a, b, t):
    return int(a + (b - a) * t)


def alpha_paint(base, draw_fn):
    layer = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
    draw_fn(ImageDraw.Draw(layer))
    return Image.alpha_composite(base, layer)


img = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# -- Sky gradient (top -> bottom) ---------------------------------------------
top, bot = hexa("#BEE6FF"), hexa("#6FB7DD")
for y in range(RH):
    t = y / (RH - 1)
    draw.line(
        [(0, y), (RW - 1, y)],
        fill=(lerp(top[0], bot[0], t), lerp(top[1], bot[1], t), lerp(top[2], bot[2], t), 255),
    )

# -- Distant grass strip at bottom --------------------------------------------
grass_top = hexa("#A8E36B")
grass_bot = hexa("#5FB23A")
g_start = int(RH * 0.86)
for y in range(g_start, RH):
    t = (y - g_start) / max(1, (RH - g_start - 1))
    draw.line(
        [(0, y), (RW - 1, y)],
        fill=(lerp(grass_top[0], grass_bot[0], t), lerp(grass_top[1], grass_bot[1], t), lerp(grass_top[2], grass_bot[2], t), 255),
    )

# -- Clouds (soft white ellipses) ---------------------------------------------
def draw_cloud(d, cx, cy, w, h, alpha=0x33):
    col = (255, 255, 255, alpha)
    d.ellipse([cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2], fill=col)
    d.ellipse([cx - w / 2 + w * 0.25, cy - h * 0.9, cx + w / 2 - w * 0.05, cy + h * 0.1], fill=col)
    d.ellipse([cx - w / 2 - w * 0.05, cy - h * 0.5, cx + w / 2 - w * 0.4, cy + h * 0.4], fill=col)

cloud_layer = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
cd = ImageDraw.Draw(cloud_layer)
for cx, cy, w, h in [
    (140 * OVER, 90 * OVER, 200 * OVER, 60 * OVER),
    (520 * OVER, 60 * OVER, 220 * OVER, 60 * OVER),
    (900 * OVER, 110 * OVER, 240 * OVER, 70 * OVER),
    (300 * OVER, 200 * OVER, 160 * OVER, 50 * OVER),
    (820 * OVER, 240 * OVER, 180 * OVER, 50 * OVER),
]:
    draw_cloud(cd, cx, cy, w, h)
img = Image.alpha_composite(img, cloud_layer)

# -- Dashed maze path from ball to star ---------------------------------------
# A blocky right-angle path evokes the maze grid.
path = [
    (260, 360), (260, 300), (420, 300), (420, 360),
    (560, 360), (560, 260), (700, 260), (700, 220),
    (800, 220),
]
path = [(x * OVER, y * OVER) for x, y in path]
dash_len, gap_len = 16 * OVER, 14 * OVER
dash_w = 10 * OVER
dash_col = (255, 255, 255, 200)
dash_layer = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
dd = ImageDraw.Draw(dash_layer)
carry = 0.0
draw_on = True
for (x1, y1), (x2, y2) in zip(path[:-1], path[1:]):
    seg_len = math.hypot(x2 - x1, y2 - y1)
    if seg_len == 0:
        continue
    ux, uy = (x2 - x1) / seg_len, (y2 - y1) / seg_len
    pos = 0.0
    while pos < seg_len:
        run = (dash_len if draw_on else gap_len) - carry
        end = min(seg_len, pos + run)
        if draw_on:
            sx, sy = x1 + ux * pos, y1 + uy * pos
            ex, ey = x1 + ux * end, y1 + uy * end
            # round-capped line
            dd.line([(sx, sy), (ex, ey)], fill=dash_col, width=dash_w)
            for px, py in [(sx, sy), (ex, ey)]:
                dd.ellipse([px - dash_w / 2, py - dash_w / 2, px + dash_w / 2, py + dash_w / 2], fill=dash_col)
        if end == pos + run:
            draw_on = not draw_on
            carry = 0.0
        else:
            carry = end - pos
        pos = end
img = Image.alpha_composite(img, dash_layer)

# -- Star glow + star (right side) --------------------------------------------
star_cx, star_cy = 800 * OVER, 220 * OVER
star_r_outer = 60 * OVER
star_r_inner = 26 * OVER

glow_layer = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
gd = ImageDraw.Draw(glow_layer)
glow_r = 100 * OVER
gd.ellipse([star_cx - glow_r, star_cy - glow_r, star_cx + glow_r, star_cy + glow_r], fill=(255, 224, 138, 0x55))
img = Image.alpha_composite(img, glow_layer)

# 5-point star vertices (tip up)
star_pts = []
for i in range(10):
    angle = -math.pi / 2 + i * math.pi / 5
    r = star_r_outer if i % 2 == 0 else star_r_inner
    star_pts.append((star_cx + r * math.cos(angle), star_cy + r * math.sin(angle)))
star_layer = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
sd = ImageDraw.Draw(star_layer)
sd.polygon(star_pts, fill=hexa("#FFCB47"), outline=hexa("#D89A1F"), width=4 * OVER)
img = Image.alpha_composite(img, star_layer)

# -- Ball: shadow + radial-gradient body + outline + highlight ----------------
ball_cx, ball_cy = 260 * OVER, 360 * OVER
ball_r = 80 * OVER
shadow_layer = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
shd = ImageDraw.Draw(shadow_layer)
shd.ellipse(
    [ball_cx - ball_r, ball_cy + ball_r * 0.25, ball_cx + ball_r, ball_cy + ball_r * 0.55],
    fill=(0, 0, 0, 0x55),
)
img = Image.alpha_composite(img, shadow_layer)

grad_cx, grad_cy = ball_cx - ball_r * 0.35, ball_cy - ball_r * 0.4
grad_r = ball_r * 1.6
start_c, end_c = (255, 255, 255), (232, 69, 69)

ball_layer = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
bpx = ball_layer.load()
x0 = max(0, int(ball_cx - ball_r - 3))
x1 = min(RW, int(ball_cx + ball_r + 3))
y0 = max(0, int(ball_cy - ball_r - 3))
y1 = min(RH, int(ball_cy + ball_r + 3))
for y in range(y0, y1):
    for x in range(x0, x1):
        d = math.hypot(x - grad_cx, y - grad_cy)
        t = min(1.0, d / grad_r)
        bpx[x, y] = (
            lerp(start_c[0], end_c[0], t),
            lerp(start_c[1], end_c[1], t),
            lerp(start_c[2], end_c[2], t),
            255,
        )
mask = Image.new("L", (RW, RH), 0)
ImageDraw.Draw(mask).ellipse([ball_cx - ball_r, ball_cy - ball_r, ball_cx + ball_r, ball_cy + ball_r], fill=255)
ball_layer.putalpha(mask)
img = Image.alpha_composite(img, ball_layer)

outline_layer = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
od = ImageDraw.Draw(outline_layer)
od.ellipse(
    [ball_cx - ball_r, ball_cy - ball_r, ball_cx + ball_r, ball_cy + ball_r],
    outline=hexa("#B53636"),
    width=5 * OVER,
)
hl_r = 18 * OVER
hl_cx, hl_cy = ball_cx - ball_r * 0.4, ball_cy - ball_r * 0.4
od.ellipse([hl_cx - hl_r, hl_cy - hl_r, hl_cx + hl_r, hl_cy + hl_r], fill=(255, 255, 255, 0xF0))
img = Image.alpha_composite(img, outline_layer)

# -- Title text ---------------------------------------------------------------
def text_with_outline(d, xy, text, font, fill, outline, outline_w):
    x, y = xy
    for dx in range(-outline_w, outline_w + 1):
        for dy in range(-outline_w, outline_w + 1):
            if dx * dx + dy * dy <= outline_w * outline_w:
                d.text((x + dx, y + dy), text, font=font, fill=outline)
    d.text(xy, text, font=font, fill=fill)


title = "또르르 미로"
subtitle = "기울여서 굴려요!"

try:
    title_font = ImageFont.truetype(FONT, 130 * OVER)
    sub_font = ImageFont.truetype(FONT, 52 * OVER)
except OSError:
    title_font = ImageFont.load_default()
    sub_font = ImageFont.load_default()

text_layer = Image.new("RGBA", (RW, RH), (0, 0, 0, 0))
td = ImageDraw.Draw(text_layer)

# Title — anchored top-center, slightly above middle, with red outline
bbox = td.textbbox((0, 0), title, font=title_font)
tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
tx = (RW - tw) // 2 - bbox[0]
ty = 70 * OVER - bbox[1]
text_with_outline(
    td, (tx, ty), title, title_font,
    fill=hexa("#FFFFFF"), outline=hexa("#E84545"), outline_w=8 * OVER,
)

# Subtitle — under the title, white with dark navy outline
bbox2 = td.textbbox((0, 0), subtitle, font=sub_font)
sw, sh = bbox2[2] - bbox2[0], bbox2[3] - bbox2[1]
sx = (RW - sw) // 2 - bbox2[0]
sy = ty + th + 18 * OVER - bbox2[1]
text_with_outline(
    td, (sx, sy), subtitle, sub_font,
    fill=hexa("#FFFFFF"), outline=hexa("#1E5B82"), outline_w=4 * OVER,
)

img = Image.alpha_composite(img, text_layer)

# -- Downscale and write opaque PNG ------------------------------------------
final = img.resize((W, H), Image.LANCZOS).convert("RGB")
os.makedirs(os.path.dirname(OUT), exist_ok=True)
final.save(OUT, "PNG", optimize=True)
print(f"wrote {OUT} ({os.path.getsize(OUT)} bytes)")
