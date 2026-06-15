#!/usr/bin/env python3
"""Generate Clover App Market PNG assets (icon, feature graphic, screenshots)."""

from __future__ import annotations

import sys
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    print("Install Pillow: pip install pillow")
    sys.exit(1)

ROOT = Path(__file__).resolve().parent.parent
STORE = ROOT / "store"
SCREENSHOTS = ROOT / "docs" / "screenshots"

PRIMARY = (26, 35, 126)  # #1A237E
ACCENT = (0, 200, 83)  # #00C853
SURFACE = (245, 247, 250)


def draw_shield(draw: ImageDraw.ImageDraw, cx: int, cy: int, size: int) -> None:
    w = size
    h = int(size * 1.15)
    left = cx - w // 2
    top = cy - h // 2
    draw.polygon(
        [
            (left, top + h * 0.12),
            (left + w, top + h * 0.12),
            (left + w, top + h * 0.55),
            (cx, top + h * 0.92),
            (left, top + h * 0.55),
        ],
        fill=ACCENT,
    )
    check = [
        (cx - w * 0.22, cy + h * 0.02),
        (cx - w * 0.05, cy + h * 0.18),
        (cx + w * 0.28, cy - h * 0.12),
    ]
    draw.line(check, fill=PRIMARY, width=max(4, size // 18), joint="curve")


def load_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for name in ("arialbd.ttf", "arial.ttf", "segoeui.ttf"):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def make_icon() -> None:
    size = 512
    img = Image.new("RGB", (size, size), PRIMARY)
    draw = ImageDraw.Draw(img)
    draw_shield(draw, size // 2, size // 2 - 20, 200)
    font = load_font(52)
    draw.text((size // 2, size - 96), "BP", fill=(255, 255, 255), font=font, anchor="mm")
    out = STORE / "icon-512.png"
    img.save(out, "PNG")
    print(f"Wrote {out}")


def make_feature_graphic() -> None:
    w, h = 1024, 500
    img = Image.new("RGB", (w, h), PRIMARY)
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, h - 8, w, h), fill=ACCENT)
    draw_shield(draw, 180, h // 2, 140)
    title = load_font(56)
    sub = load_font(28)
    draw.text((320, h // 2 - 50), "BP Audit Shield", fill=(255, 255, 255), font=title)
    draw.text(
        (320, h // 2 + 20),
        "Blockchain audit trail for Sunmi POS",
        fill=(200, 210, 255),
        font=sub,
    )
    draw.text(
        (320, h // 2 + 70),
        "branchlesspay.com/verify",
        fill=ACCENT,
        font=sub,
    )
    out = STORE / "feature-graphic-1024x500.png"
    img.save(out, "PNG")
    print(f"Wrote {out}")


def fit_screenshot(src: Path, out: Path, label: str) -> None:
    target_w, target_h = 1280, 800
    canvas = Image.new("RGB", (target_w, target_h), PRIMARY)
    draw = ImageDraw.Draw(canvas)

    if not src.exists():
        draw.text((80, 360), f"Missing: {src.name}", fill=(255, 255, 255), font=load_font(36))
        canvas.save(out, "PNG")
        print(f"Placeholder {out}")
        return

    shot = Image.open(src).convert("RGB")
    sw, sh = shot.size
    scale = min((target_w - 160) / sw, (target_h - 120) / sh)
    nw, nh = int(sw * scale), int(sh * scale)
    resized = shot.resize((nw, nh), Image.Resampling.LANCZOS)
    x = (target_w - nw) // 2
    y = 70 + (target_h - 120 - nh) // 2
    canvas.paste(resized, (x, y))

    draw.rectangle((0, 0, target_w, 56), fill=ACCENT)
    draw.text((24, 28), label, fill=PRIMARY, font=load_font(28), anchor="lm")
    canvas.save(out, "PNG")
    print(f"Wrote {out}")


def main() -> None:
    STORE.mkdir(parents=True, exist_ok=True)
    make_icon()
    make_feature_graphic()
    fit_screenshot(
        SCREENSHOTS / "m3-main-emulator.png",
        STORE / "screenshot-01-main.png",
        "BP Audit Shield — Home",
    )
    fit_screenshot(
        SCREENSHOTS / "m3-history-emulator.png",
        STORE / "screenshot-02-history.png",
        "Transaction History",
    )
    fit_screenshot(
        SCREENSHOTS / "m3-verify-emulator.png",
        STORE / "screenshot-03-verify.png",
        "Blockchain Verify",
    )


if __name__ == "__main__":
    main()
