"""研途倒计时 · Android 小组件素材生成器

运行方式：
    py tools/make_android_assets.py

生成（写入 android/app/src/main/res/drawable-nodpi/）：
    scanline_tile.png   扫描线贴图（8x8 平铺）
    widget_preview.png  小组件预览图（500x220）

依赖：Pillow
"""

import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "android", "app", "src", "main", "res", "drawable-nodpi")

GREEN_TOP = (35, 77, 64)
GREEN_BOTTOM = (16, 41, 33)
FACE_BG = (29, 48, 44)
WINDOW_BG = (21, 35, 31)
WINDOW_BORDER = (222, 165, 70)
AMBER = (241, 173, 61)


def rounded_mask(size, radius):
    mask = Image.new("L", size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return mask


def vertical_gradient(size, top, bottom):
    w, h = size
    img = Image.new("RGB", size)
    for y in range(h):
        t = y / max(1, h - 1)
        img.paste(tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3)), (0, y, w, y + 1))
    return img


def make_scanline_tile():
    """8x8 平铺贴图：隔行一道极淡的亮线。"""
    size = 8
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for y in range(size):
        if y % 4 == 2:
            draw.line([(0, y), (size - 1, y)], fill=(255, 255, 255, 18))
    return img


def make_widget_preview():
    """500x220 的牌面预览图（给桌面小组件选择器用）。"""
    W, H = 500, 220
    s = 4
    Wb, Hb = W * s, H * s
    img = Image.new("RGBA", (Wb, Hb), (0, 0, 0, 0))

    gradient = vertical_gradient((Wb, Hb), GREEN_TOP, GREEN_BOTTOM).convert("RGBA")
    radius = int(40 * s)
    shell = Image.new("RGBA", (Wb, Hb), (0, 0, 0, 0))
    shell.paste(gradient, (0, 0), rounded_mask((Wb, Hb), radius))
    img.alpha_composite(shell)

    draw = ImageDraw.Draw(img)
    pad = int(22 * s)

    # 顶部铭文
    draw.text((pad, int(14 * s)), "HORIZON / 学习计时器", fill=(130, 162, 149), font=_font(11 * s))
    draw.ellipse([Wb - pad - int(8 * s), int(13 * s), Wb - pad, int(13 * s) + int(8 * s)], fill=(196, 226, 122))

    # 屏幕面板
    face = [pad, int(32 * s), Wb - pad, Hb - int(28 * s)]
    draw.rounded_rectangle(face, radius=int(6 * s), fill=FACE_BG)

    # 顶部标题行
    title_y = int(42 * s)
    draw.text((int(38 * s), title_y), "距离研究生考试还有", fill=(177, 196, 172), font=_font(12 * s))
    draw.text((Wb - int(38 * s), title_y), "2027.12.25", fill=(220, 154, 57), font=_font(12 * s), anchor="ra")

    # 液晶窗
    win = [int(36 * s), int(64 * s), Wb - int(36 * s), Hb - int(52 * s)]
    draw.rounded_rectangle(win, radius=int(5 * s), fill=WINDOW_BG, outline=WINDOW_BORDER, width=2 * s)

    # 扫描线
    tile = make_scanline_tile()
    for y in range(win[1], win[3], tile.height):
        img.alpha_composite(tile, (win[0], y))

    # 数字
    font = _font(52 * s)
    text = "888"
    bb = draw.textbbox((0, 0), text, font=font)
    cx = (win[0] + win[2]) // 2 - int(14 * s)
    cy = (win[1] + win[3]) // 2
    draw.text((cx + 4 * s, cy), text, font=font, fill=(224, 142, 44, 90))
    draw.text((cx, cy), text, font=font, fill=AMBER, anchor="mm")

    draw.text((cx + int(52 * s), cy + int(20 * s)), "天", fill=(220, 154, 57), font=_font(16 * s))

    # 底部
    draw.text((int(38 * s), Hb - int(38 * s)), "KEEP GOING", fill=(115, 143, 127), font=_font(10 * s))
    draw.text((Wb - int(38 * s), Hb - int(38 * s)), "目标日期  2027 / 12 / 25", fill=(173, 188, 152), font=_font(10 * s), anchor="ra")

    return img.resize((W, H), Image.LANCZOS)


_font_cache = {}


def _font(size):
    if size not in _font_cache:
        try:
            _font_cache[size] = ImageFont.load_default(size=int(size))
        except TypeError:
            _font_cache[size] = ImageFont.load_default()
    return _font_cache[size]


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    make_scanline_tile().save(os.path.join(OUT_DIR, "scanline_tile.png"), "PNG")
    print("已生成 scanline_tile.png")
    make_widget_preview().save(os.path.join(OUT_DIR, "widget_preview.png"), "PNG")
    print("已生成 widget_preview.png")


if __name__ == "__main__":
    main()