#!/usr/bin/env python3
"""
Pure Procedural Python Logo Generator for Stash Personal Finance Tracker
Created using standard Python libraries: PIL (Pillow), NumPy, and Math.
Zero AI, zero external APIs, 100% mathematical vector geometry and rendering.
"""

import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

def create_stash_logo(size=1024):
    """
    Renders the Stash logo using pure mathematical geometry, Bezier curves,
    and anti-aliased multi-pass drawing with supersampling.
    """
    # 2x supersampling for high-quality antialiasing
    scale = 2
    w = size * scale
    h = size * scale
    
    # 1. Base RGBA Canvas
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    
    # 2. Outer Rounded Squircle Canvas Dimensions
    margin = int(w * 0.06)
    box = [margin, margin, w - margin, h - margin]
    radius = int((w - margin * 2) * 0.28) # Continuous smooth squircle curvature
    
    # 3. Soft Elevation Drop Shadow (Procedural Gaussian Convolution)
    shadow_offset = int(w * 0.04)
    shadow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    s_box = [margin, margin + shadow_offset, w - margin, h - margin + shadow_offset]
    s_draw.rounded_rectangle(s_box, radius=radius, fill=(5, 8, 18, 180))
    shadow = shadow.filter(ImageFilter.GaussianBlur(int(w * 0.035)))
    img = Image.alpha_composite(img, shadow)
    
    # 4. Background Gradient (Deep Obsidian Navy to Dark Teal Slate)
    bg_mask = Image.new("L", (w, h), 0)
    bg_mask_draw = ImageDraw.Draw(bg_mask)
    bg_mask_draw.rounded_rectangle(box, radius=radius, fill=255)
    
    # Linear Gradient array using NumPy
    y_coords = np.linspace(0, 1, h)[:, None, None]
    c_top = np.array([13, 20, 36])      # Obsidian Slate
    c_bot = np.array([5, 10, 22])       # Midnight Sapphire
    gradient_rgb = (c_top * (1 - y_coords) + c_bot * y_coords).astype(np.uint8)
    gradient_rgb = np.repeat(gradient_rgb, w, axis=1)
    
    # Alpha channel
    alpha = np.full((h, w, 1), 255, dtype=np.uint8)
    bg_array = np.concatenate((gradient_rgb, alpha), axis=2)
    bg_image = Image.fromarray(bg_array, mode="RGBA")
    
    # Ambient Center Radial Glow (Emerald Green #10B981)
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    cx, cy = w // 2, h // 2
    max_r = int(w * 0.45)
    for r in range(max_r, 0, -4):
        t = r / max_r
        alpha_val = int(55 * (1 - t ** 1.3))
        g_draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(16, 185, 129, alpha_val))
    glow = glow.filter(ImageFilter.GaussianBlur(int(w * 0.06)))
    bg_image = Image.alpha_composite(bg_image, glow)
    
    # Paste masked background onto canvas
    img.paste(bg_image, (0, 0), mask=bg_mask)
    
    # 5. Metallic Inner Glass Border
    border = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    b_draw = ImageDraw.Draw(border)
    b_draw.rounded_rectangle(box, radius=radius, outline=(255, 255, 255, 28), width=int(w * 0.004))
    img = Image.alpha_composite(img, border)

    # 6. Mathematical Geometry: Procedural Parametric S-Ribbon Emblem
    # We construct smooth cubic Bezier curves for the stylized S
    def cubic_bezier(p0, p1, p2, p3, steps=100):
        t = np.linspace(0, 1, steps)
        pts = []
        for ti in t:
            x = (1-ti)**3 * p0[0] + 3*(1-ti)**2 * ti * p1[0] + 3*(1-ti) * ti**2 * p2[0] + ti**3 * p3[0]
            y = (1-ti)**3 * p0[1] + 3*(1-ti)**2 * ti * p1[1] + 3*(1-ti) * ti**2 * p2[1] + ti**3 * p3[1]
            pts.append((x, y))
        return pts

    emblem_w = int(w * 0.48)
    emblem_h = int(h * 0.56)
    x0 = cx - emblem_w // 2
    y0 = cy - emblem_h // 2
    stroke_w = int(w * 0.095)
    
    # Segment 1: Upper Ribbon (Sweeps from left, arches over top right, cuts toward center)
    p0 = (x0 + int(emblem_w * 0.22), y0 + stroke_w // 2 + 8)
    p1 = (x0 + emblem_w * 0.95, y0 + stroke_w // 2)
    p2 = (x0 + emblem_w * 1.05, y0 + emblem_h * 0.40)
    p3 = (cx, cy)
    curve_top = cubic_bezier(p0, p1, p2, p3, steps=80)
    
    # Segment 2: Lower Ribbon (Sweeps from center diagonal, arches around bottom left, hooks right)
    p4 = (cx, cy)
    p5 = (x0 - int(emblem_w * 0.05), y0 + emblem_h * 0.60)
    p6 = (x0 + int(emblem_w * 0.05), y0 + emblem_h - stroke_w // 2)
    p7 = (x0 + int(emblem_w * 0.78), y0 + emblem_h - stroke_w // 2 - 8)
    curve_bot = cubic_bezier(p4, p5, p6, p7, steps=80)
    
    full_curve = curve_top + curve_bot[1:]
    
    # 7. Ribbon Drop Shadow
    ribbon_sh = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    rsh_draw = ImageDraw.Draw(ribbon_sh)
    sh_dy = int(w * 0.024)
    for i in range(len(full_curve) - 1):
        pt1 = (full_curve[i][0], full_curve[i][1] + sh_dy)
        pt2 = (full_curve[i+1][0], full_curve[i+1][1] + sh_dy)
        rsh_draw.line([pt1, pt2], fill=(0, 0, 0, 190), width=stroke_w)
    for pt in full_curve:
        sh_pt = (pt[0], pt[1] + sh_dy)
        rad = stroke_w // 2
        rsh_draw.ellipse([sh_pt[0] - rad, sh_pt[1] - rad, sh_pt[0] + rad, sh_pt[1] + rad], fill=(0, 0, 0, 190))
    ribbon_sh = ribbon_sh.filter(ImageFilter.GaussianBlur(int(w * 0.026)))
    img = Image.alpha_composite(img, ribbon_sh)

    # 8. Gradient Rendering for S-Ribbon (Cyan -> Emerald -> Bright Mint)
    c_cyan = np.array([6, 182, 212])     # Cyan #06B6D4
    c_emerald = np.array([16, 185, 129]) # Emerald #10B981
    c_mint = np.array([52, 211, 153])    # Mint #34D399
    
    ribbon = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    r_draw = ImageDraw.Draw(ribbon)
    n_pts = len(full_curve)
    
    for i in range(n_pts - 1):
        t = i / (n_pts - 1)
        if t < 0.5:
            col = (c_cyan * (1 - t * 2) + c_emerald * (t * 2)).astype(int)
        else:
            col = (c_emerald * (1 - (t - 0.5) * 2) + c_mint * ((t - 0.5) * 2)).astype(int)
        
        color_tuple = (int(col[0]), int(col[1]), int(col[2]), 255)
        pt1 = full_curve[i]
        pt2 = full_curve[i+1]
        r_draw.line([pt1, pt2], fill=color_tuple, width=stroke_w)
        
    for i, pt in enumerate(full_curve):
        t = i / (n_pts - 1)
        if t < 0.5:
            col = (c_cyan * (1 - t * 2) + c_emerald * (t * 2)).astype(int)
        else:
            col = (c_emerald * (1 - (t - 0.5) * 2) + c_mint * ((t - 0.5) * 2)).astype(int)
        color_tuple = (int(col[0]), int(col[1]), int(col[2]), 255)
        rad = stroke_w // 2
        r_draw.ellipse([pt[0] - rad, pt[1] - rad, pt[0] + rad, pt[1] + rad], fill=color_tuple)
        
    img = Image.alpha_composite(img, ribbon)

    # 9. Golden Currency / Vault Coin in upper curve
    coin_x = cx + int(emblem_w * 0.08)
    coin_y = cy - int(emblem_h * 0.09)
    coin_r = int(w * 0.095)
    
    # Coin drop shadow
    coin_sh = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    csh_draw = ImageDraw.Draw(coin_sh)
    csh_draw.ellipse([coin_x - coin_r, coin_y - coin_r + int(w * 0.02),
                      coin_x + coin_r, coin_y + coin_r + int(w * 0.02)], fill=(0, 0, 0, 170))
    coin_sh = coin_sh.filter(ImageFilter.GaussianBlur(int(w * 0.02)))
    img = Image.alpha_composite(img, coin_sh)
    
    # Coin disc with radial gold gradient
    coin = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    c_draw = ImageDraw.Draw(coin)
    c_gold1 = np.array([251, 191, 36]) # Amber 400
    c_gold2 = np.array([217, 119, 6])  # Amber 600
    for r in range(coin_r, 0, -1):
        t = r / coin_r
        c_rgb = (c_gold1 * t + c_gold2 * (1 - t)).astype(int)
        c_draw.ellipse([coin_x - r, coin_y - r, coin_x + r, coin_y + r],
                       fill=(int(c_rgb[0]), int(c_rgb[1]), int(c_rgb[2]), 255))
        
    # Inner gold border
    inner_r = int(coin_r * 0.76)
    c_draw.ellipse([coin_x - inner_r, coin_y - inner_r, coin_x + inner_r, coin_y + inner_r],
                   outline=(254, 240, 138, 190), width=int(w * 0.007))
    
    # Upward financial growth chevron inside coin
    a_size = int(coin_r * 0.52)
    chevron = [
        (coin_x, coin_y - a_size),
        (coin_x + int(a_size * 0.65), coin_y - int(a_size * 0.05)),
        (coin_x + int(a_size * 0.28), coin_y - int(a_size * 0.05)),
        (coin_x + int(a_size * 0.28), coin_y + a_size),
        (coin_x - int(a_size * 0.28), coin_y + a_size),
        (coin_x - int(a_size * 0.28), coin_y - int(a_size * 0.05)),
        (coin_x - int(a_size * 0.65), coin_y - int(a_size * 0.05))
    ]
    c_draw.polygon(chevron, fill=(255, 255, 255, 250))
    img = Image.alpha_composite(img, coin)

    # 10. Specular Light Flare on Upper Curve
    spec = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    sp_draw = ImageDraw.Draw(spec)
    flare_x = int(p1[0] - stroke_w * 0.3)
    flare_y = int(p1[1] + stroke_w * 0.3)
    flare_rad = int(w * 0.12)
    for r in range(flare_rad, 0, -2):
        t = r / flare_rad
        alpha_val = int(80 * (1 - t ** 2))
        sp_draw.ellipse([flare_x - r, flare_y - r, flare_x + r, flare_y + r], fill=(255, 255, 255, alpha_val))
    img = Image.alpha_composite(img, spec)

    # 11. Final High-Quality Lanczos Downsampling
    final_output = img.resize((size, size), Image.Resampling.LANCZOS)
    return final_output

if __name__ == "__main__":
    print("=" * 60)
    print("Pure Python Procedural Logo Generator for Stash")
    print("Zero AI • Pure Mathematics, Parametric Splines & PIL")
    print("=" * 60)
    
    # 1. Render 1024x1024 High-Resolution Master
    print("[1/3] Generating 1024x1024 Master Logo...")
    logo_1024 = create_stash_logo(1024)
    out_1024 = "/storage/emulated/0/Download/stash_logo.png"
    logo_1024.save(out_1024, "PNG", optimize=True)
    print(f"      ✓ Saved to: {out_1024}")
    
    # 2. Render 512x512 Store / Web Icon
    print("[2/3] Generating 512x512 Standard Icon...")
    logo_512 = create_stash_logo(512)
    out_512 = "/storage/emulated/0/Download/stash_icon_512.png"
    logo_512.save(out_512, "PNG", optimize=True)
    print(f"      ✓ Saved to: {out_512}")
    
    # 3. Generate Android Mipmap densities
    print("[3/3] Generating Android App Mipmap Launchers...")
    import os
    res_base = "/data/data/com.termux/files/home/Stash/app/src/main/res"
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192
    }
    for folder, px in densities.items():
        folder_path = os.path.join(res_base, folder)
        os.makedirs(folder_path, exist_ok=True)
        icon = create_stash_logo(px)
        icon.save(os.path.join(folder_path, "ic_launcher.png"), "PNG", optimize=True)
        icon.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG", optimize=True)
        print(f"      ✓ {folder}: {px}x{px} px")

    print("=" * 60)
    print("SUCCESS: Pure Python logo creation complete!")
    print("=" * 60)
