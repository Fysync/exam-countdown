"""研途倒计时 · 应用图标生成器

运行方式（在项目根目录）：
    py tools/make_icons.py

生成：
    icons/icon-192.png             常规图标 192x192
    icons/icon-512.png             常规图标 512x512
    icons/icon-maskable-512.png    遮罩图标 512x512（内容位于安全区内）
    icons/apple-touch-icon.png     iOS 主屏图标 180x180

依赖：Pillow
"""

import os
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(ROOT, "icons")

# 与网页端一致的配色
GREEN_TOP = (35, 77, 64)
GREEN_BOTTOM = (16, 41, 33)
WINDOW_BG = (21, 35, 31)
WINDOW_BORDER = (222, 165, 70)
AMBER = (241, 173, 61)
AMBER_DIM = (160, 105, 30)


def vertical_gradient(size, top, bottom):
    """返回自上而下的垂直渐变图。"""
    width, height = size
    img = Image.new("RGB", size)
    for y in range(height):
        t = y / max(1, height - 1)
        color = tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        img.paste(color, (0, y, width, y + 1))
    return img


def rounded_mask(size, radius):
    mask = Image.new("L", size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return mask


def draw_icon(size, full_bleed=False):
    """绘制一枚图标。full_bleed=True 时绿色外壳铺满全图（供 maskable 使用）。"""
    s = 8  # 超采样倍率
    big = size * s
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))

    # 外壳：渐变绿圆角矩形
    shell_gradient = vertical_gradient((big, big), GREEN_TOP, GREEN_BOTTOM).convert("RGBA")
    shell_pad = 0 if full_bleed else int(big * 0.035)
    shell_rect = [shell_pad, shell_pad, big - shell_pad, big - shell_pad]
    shell_radius = 0 if full_bleed else int(big * 0.16)
    shell = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    shell.paste(shell_gradient, (0, 0), rounded_mask((big, big), shell_radius))

    # 只保留外壳圆角矩形区域
    mask = rounded_mask((big, big), shell_radius if not full_bleed else big)
    outer = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    outer.paste(shell, (0, 0), mask)
    img.alpha_composite(outer)

    draw = ImageDraw.Draw(img)

    # 液晶窗：深色圆角矩形 + 琥珀描边（内容保持在中央安全区）
    unit = big / 100.0
    window_margin = int(unit * (26 if full_bleed else 24))
    win = [window_margin, window_margin, big - window_margin, big - window_margin]
    window_rect = [win[0] + int(unit * 4), win[1] + int(unit * 4),
                   win[2] - int(unit * 4), win[3] - int(unit * 4)]
    draw.rounded_rectangle(window_rect, radius=int(unit * 8), fill=WINDOW_BG)

    # 数字 "2027"
    text_size = int(unit * (58 if full_bleed else 62))
    try:
        font = ImageFont.load_default(size=text_size)
    except TypeError:
        font = ImageFont.load_default()

    text = "2027"
    bbox = draw.textbbox((0, 0), text, font=font, anchor="mm")
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    cx = big // 2
    cy = big // 2 + int(unit * 2)

    # 琥珀色光晕（多层叠加）
    glow = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    for offset, alpha in ((5, 40), (3, 90), (1, 150)):
        gd.text((cx + offset, cy + offset), text, font=font, anchor="mm",
                fill=(224, 142, 44, alpha))
    img.alpha_composite(glow)

    # 主文字
    gd = ImageDraw.Draw(img)
    gd.text((cx, cy), text, font=font, anchor="mm", fill=AMBER)

    # 缩回目标尺寸
    img = img.resize((size, size), Image.LANCZOS)
    return img


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    targets = {
        "icon-192.png": (192, False),
        "icon-512.png": (512, False),
        "icon-maskable-512.png": (512, True),
        "apple-touch-icon.png": (180, False),
    }
    for name, (size, full_bleed) in targets.items():
        path = os.path.join(OUT_DIR, name)
        draw_icon(size, full_bleed=full_bleed).save(path, "PNG")
        print(f"已生成 {path}")


if __name__ == "__main__":
    main()