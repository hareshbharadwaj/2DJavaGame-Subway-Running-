"""Post-process generated raw art into game-ready sprites.

Removes the flat chroma-key background, despills colour fringing, trims to
content and resizes to match the dimensions of the existing asset set.

Usage:  python tools/process_assets.py
"""
import os

import numpy as np
from PIL import Image

RAW = "raw"
OUT = "assets"


def load_rgb(path):
    return np.asarray(Image.open(path).convert("RGB")).astype(np.float32)


def key_out_background(rgb, soft_lo=55.0, soft_hi=115.0):
    """Alpha from distance to the background colour sampled at the corners."""
    h, w, _ = rgb.shape
    patch = 12
    corners = np.concatenate([
        rgb[:patch, :patch].reshape(-1, 3),
        rgb[:patch, -patch:].reshape(-1, 3),
        rgb[-patch:, :patch].reshape(-1, 3),
        rgb[-patch:, -patch:].reshape(-1, 3),
    ])
    key = np.median(corners, axis=0)
    dist = np.sqrt(((rgb - key) ** 2).sum(axis=2))
    alpha = np.clip((dist - soft_lo) / (soft_hi - soft_lo), 0.0, 1.0)
    return alpha, key


def despill(rgb, key):
    """Pull magenta/purple spill on soft edges back toward neutral."""
    out = rgb.copy()
    r, g, b = out[..., 0], out[..., 1], out[..., 2]
    # background is magenta-ish: red and blue high, green low
    if key[0] > key[1] and key[2] > key[1]:
        limit = np.maximum(g, (r + b) * 0.5 * 0.6 + g * 0.4)
        spill = np.minimum(r, b) - limit
        spill = np.clip(spill, 0, None)
        out[..., 0] = r - spill
        out[..., 2] = b - spill
    return np.clip(out, 0, 255)


def to_rgba(path, soft_lo=55.0, soft_hi=115.0):
    rgb = load_rgb(path)
    alpha, key = key_out_background(rgb, soft_lo, soft_hi)
    rgb = despill(rgb, key)
    rgba = np.dstack([rgb, alpha * 255.0]).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA")


def trim(img, threshold=8):
    a = np.asarray(img)[..., 3]
    ys, xs = np.where(a > threshold)
    if len(xs) == 0:
        return img
    return img.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))


def fit(img, width=None, height=None):
    w, h = img.size
    if width and height:
        size = (width, height)
    elif width:
        size = (width, max(1, round(h * width / w)))
    else:
        size = (max(1, round(w * height / h)), height)
    return img.resize(size, Image.LANCZOS)


def cover(img, width, height):
    """Scale + centre-crop to exactly fill width x height."""
    w, h = img.size
    scale = max(width / w, height / h)
    img = img.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)
    w, h = img.size
    left, top = (w - width) // 2, (h - height) // 2
    return img.crop((left, top, left + width, top + height))


def radial_feather(img, inner=0.74, outer=0.99):
    """Fade the outer rim so a round sprite melts into the road."""
    a = np.asarray(img).astype(np.float32)
    h, w = a.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    cy, cx = (h - 1) / 2.0, (w - 1) / 2.0
    r = np.sqrt(((yy - cy) / (h / 2.0)) ** 2 + ((xx - cx) / (w / 2.0)) ** 2)
    fade = np.clip((outer - r) / (outer - inner), 0.0, 1.0)
    a[..., 3] *= fade
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def darken(img, factor):
    a = np.asarray(img).astype(np.float32)
    a[..., :3] *= factor
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def save(img, rel):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)
    print(f"{img.size[0]:>4}x{img.size[1]:<4} {os.path.getsize(path)//1024:>4}KB  {path}")


def main():
    # --- obstacles -------------------------------------------------------
    # Pothole: trim to the asphalt disc, blend its rim into the grey road and
    # tone it down so it reads as a hole in the tarmac rather than a decal.
    pothole = trim(to_rgba(os.path.join(RAW, "pothole.png")))
    pothole = fit(pothole, width=200, height=132)
    pothole = darken(radial_feather(pothole), 0.82)
    save(pothole, "obstacles/pothole.png")

    # Fourth car so obstacle slot 3 stops re-using the blue car.
    save(fit(trim(to_rgba(os.path.join(RAW, "car_green.png"))), width=150, height=240),
         "obstacles/car_green.png")

    # --- player ----------------------------------------------------------
    save(fit(trim(to_rgba(os.path.join(RAW, "player_slide.png"))), width=160),
         "player/player_slide.png")

    # --- power-up icons ---------------------------------------------------
    save(fit(trim(to_rgba(os.path.join(RAW, "boost.png"))), width=80, height=80),
         "ui/boost.png")
    save(fit(trim(to_rgba(os.path.join(RAW, "x2.png"))), width=80, height=80),
         "ui/x2.png")

    # --- branding ---------------------------------------------------------
    save(fit(trim(to_rgba(os.path.join(RAW, "logo.png"), 70.0, 150.0)), width=300),
         "ui/logo.png")

    home = Image.open(os.path.join(RAW, "home_bg.png")).convert("RGB")
    save(cover(home, 420, 760).convert("RGBA"), "ui/home_bg.png")


if __name__ == "__main__" and not (os.environ.get("PASS2") or os.environ.get("PASS3")):
    main()


def process_traffic_and_abilities():
    """Second asset pass: oncoming traffic, a longer truck and two ability icons."""
    # --- longer truck: an articulated lorry, far taller than the existing truck ---
    save(fit(trim(to_rgba(os.path.join(RAW, "truck_long.png"))), width=140),
         "obstacles/truck_long.png")

    # --- oncoming traffic (art already faces down-screen, toward the player) ---
    save(fit(trim(to_rgba(os.path.join(RAW, "bike.png"))), width=86),
         "obstacles/bike_oncoming.png")
    save(fit(trim(to_rgba(os.path.join(RAW, "auto.png"))), width=112),
         "obstacles/auto_oncoming.png")
    save(fit(trim(to_rgba(os.path.join(RAW, "car_oncoming.png"))), width=150, height=240),
         "obstacles/car_oncoming.png")

    # --- ability icons, matched to the existing 80x80 power-up set ---
    save(fit(trim(to_rgba(os.path.join(RAW, "jetpack.png"))), width=80, height=80),
         "ui/jetpack.png")
    save(fit(trim(to_rgba(os.path.join(RAW, "slowmo.png"))), width=80, height=80),
         "ui/slowmo.png")


if os.environ.get("PASS2"):
    process_traffic_and_abilities()


def process_characters():
    """Third pass: three selectable characters for the landing-page picker.

    Each is keyed, trimmed and scaled to the same 78x160 footprint as the
    original player sprite so nothing in the game's layout has to change.
    The shirt/tunic on every character is drawn a flat blue, which the game
    recolours at runtime (see SimpleRunnerGame.recolourOutfit).
    """
    for src, dst in [
        ("char_girl_run.png", "player/char_girl.png"),
        ("char_robot_run.png", "player/char_robot.png"),
        ("char_ninja_run.png", "player/char_ninja.png"),
    ]:
        img = trim(to_rgba(os.path.join(RAW, src)))
        # letterbox into the reference 78x160 box, preserving aspect
        img.thumbnail((78, 160), Image.LANCZOS)
        canvas = Image.new("RGBA", (78, 160), (0, 0, 0, 0))
        canvas.alpha_composite(img, ((78 - img.width) // 2, 160 - img.height))
        save(canvas, dst)


if os.environ.get("PASS3"):
    process_characters()
