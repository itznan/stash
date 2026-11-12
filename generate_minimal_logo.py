#!/usr/bin/env python3
"""
Ultra-Minimalist Logo Generator for Stash Personal Finance Tracker
Procedurally rendered using Python (PIL & NumPy) with pure mathematical geometry.
Zero AI • Ultra-clean modern Swiss / Scandinavian minimalist design aesthetic.
"""

import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

def create_minimal_stash_logo(size=1024):
    """
    Renders an ultra-minimalist, iconic brand mark:
    - Deep matte obsidian squircle canvas
    - Bold, geometric dual-capsule 'S' vault mark with razor-sharp precision
    - Vibrant emerald mint to electric cyan gradient
    - Perfect mathematical symmetry and optical balance
    """
    # 4x internal supersampling for immaculate anti-aliasing edges
    scale = 4
    w = size * scale
    h = size * scale
    cx, cy = w // 2, h // 2
    
    # Base transparent canvas
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    
    # 1. Outer Squircle Geometry
    margin = int(w * 0.08)
    box = [margin, margin, w - margin, h - margin]
    radius = int((w - margin * 2) * 0.28) # Smooth squircle radius
    
    # Subtle matte elevation shadow
    shadow_offset = int(w * 0.025)
    shadow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    s_box = [margin, margin + shadow_offset, w - margin, h - margin + shadow_offset]
    s_draw.rounded_rectangle(s_box, radius=radius, fill=(0, 0, 0, 160))
    shadow = shadow.filter(ImageFilter.GaussianBlur(int(w * 0.02)))
    img = Image.alpha_composite(img, shadow)
    
    # 2. Matte Obsidian Background with Subtle Vertical Tone
    bg_mask = Image.new("L", (w, h), 0)
    bg_mask_draw = ImageDraw.Draw(bg_mask)
    bg_mask_draw.rounded_rectangle(box, radius=radius, fill=255)
    
    y_coords = np.linspace(0, 1, h)[:, None, None]
    c_top = np.array([17, 24, 39])   # Dark Slate Navy (#111827)
    c_bot = np.array([10, 15, 26])   # Pure Obsidian (#0A0F1A)
    bg_rgb = np.repeat((c_top * (1 - y_coords) + c_bot * y_coords).astype(np.uint8), w, axis=1)
    bg_alpha = np.full((h, w, 1), 255, dtype=np.uint8)
    bg_img = Image.fromarray(np.concatenate((bg_rgb, bg_alpha), axis=2), mode="RGBA")
    
    # Extremely subtle inner hairline border
    border = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    b_draw = ImageDraw.Draw(border)
    b_draw.rounded_rectangle(box, radius=radius, outline=(255, 255, 255, 18), width=int(w * 0.003))
    
    bg_img = Image.alpha_composite(bg_img, border)
    img.paste(bg_img, (0, 0), mask=bg_mask)
    
    # 3. Minimalist Icon: Dual Interlocking Geometric "S" Fold
    # Two thick, precision-rounded parallel bars joined with a diagonal 45° dynamic cut
    # Clean, recognizable at 16x16 or 1024x1024
    
    mark_w = int(w * 0.44)
    mark_h = int(h * 0.52)
    x0 = cx - mark_w // 2
    y0 = cy - mark_h // 2
    x1 = cx + mark_w // 2
    y1 = cy + mark_h // 2
    
    stroke = int(w * 0.088)
    corner_rad = int(stroke * 0.95)
    
    # We draw two pristine geometric tracks:
    # Top hook: from center (cx, cy) -> up-right -> top-left
    # Bottom hook: from center (cx, cy) -> down-left -> bottom-right
    
    # Create high-precision mask for top wing
    top_wing_mask = Image.new("L", (w, h), 0)
    tw_draw = ImageDraw.Draw(top_wing_mask)
    
    # Top horizontal bar
    tw_draw.rounded_rectangle(
        [x0, y0, x1, y0 + stroke],
        radius=corner_rad,
        fill=255
    )
    # Right vertical segment
    tw_draw.rounded_rectangle(
        [x1 - stroke, y0, x1, cy + stroke // 4],
        radius=corner_rad,
        fill=255
    )
    # Center horizontal dividing bar (upper half)
    tw_draw.rounded_rectangle(
        [cx - stroke // 2, cy - stroke // 2, x1, cy + stroke // 2],
        radius=corner_rad,
        fill=255
    )
    
    # Bottom wing mask
    bot_wing_mask = Image.new("L", (w, h), 0)
    bw_draw = ImageDraw.Draw(bot_wing_mask)
    
    # Bottom horizontal bar
    bw_draw.rounded_rectangle(
        [x0, y1 - stroke, x1, y1],
        radius=corner_rad,
        fill=255
    )
    # Left vertical segment
    bw_draw.rounded_rectangle(
        [x0, cy - stroke // 4, x0 + stroke, y1],
        radius=corner_rad,
        fill=255
    )
    # Center horizontal dividing bar (lower half)
    bw_draw.rounded_rectangle(
        [x0, cy - stroke // 2, cx + stroke // 2, cy + stroke // 2],
        radius=corner_rad,
        fill=255
    )
    
    # 4. Clean 45° Negative-Space Precision Slit at Center
    # This creates the iconic razor-sharp fintech diagonal cut across the "S"
    slit_w = int(w * 0.038)
    slit_mask = Image.new("L", (w, h), 255)
    sm_draw = ImageDraw.Draw(slit_mask)
    
    # Diagonal line coordinates through center
    diag_len = int(w * 0.3)
    dx = int(diag_len * math.cos(math.radians(45)))
    dy = int(diag_len * math.sin(math.radians(45)))
    
    # Slit polygon cutting cleanly across the center
    poly = [
        (cx - dx - slit_w // 2, cy + dy),
        (cx - dx + slit_w // 2, cy + dy),
        (cx + dx + slit_w // 2, cy - dy),
        (cx + dx - slit_w // 2, cy - dy),
    ]
    sm_draw.polygon(poly, fill=0)
    
    # Combine masks
    combined_mask = Image.new("L", (w, h), 0)
    cm_draw = ImageDraw.Draw(combined_mask)
    combined_mask.paste(255, (0, 0), mask=top_wing_mask)
    combined_mask.paste(255, (0, 0), mask=bot_wing_mask)
    
    # Apply diagonal slit
    final_mark_mask = Image.new("L", (w, h), 0)
    final_mark_mask.paste(combined_mask, (0, 0), mask=slit_mask)
    
    # 5. Render Minimalist Gradient Fill (Vibrant Emerald to Clean Electric Cyan)
    # Clean linear gradient: Top-Left (#10B981) to Bottom-Right (#06B6D4)
    x_grid, y_grid = np.meshgrid(np.linspace(0, 1, w), np.linspace(0, 1, h))
    diag_gradient = (x_grid + y_grid) / 2.0
    
    c_start = np.array([16, 185, 129])   # Emerald Mint (#10B981)
    c_end = np.array([6, 182, 212])      # Electric Cyan (#06B6D4)
    
    mark_rgb = (c_start[None, None, :] * (1 - diag_gradient[:, :, None]) +
                c_end[None, None, :] * diag_gradient[:, :, None]).astype(np.uint8)
    mark_alpha = np.full((h, w, 1), 255, dtype=np.uint8)
    mark_img = Image.fromarray(np.concatenate((mark_rgb, mark_alpha), axis=2), mode="RGBA")
    
    # Minimalist subtle drop shadow beneath the mark
    mark_shadow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    msh_draw = ImageDraw.Draw(mark_shadow)
    msh_draw.bitmap((0, int(w * 0.015)), final_mark_mask, fill=(0, 0, 0, 140))
    mark_shadow = mark_shadow.filter(ImageFilter.GaussianBlur(int(w * 0.018)))
    img = Image.alpha_composite(img, mark_shadow)
    
    # Composite the minimal mark
    mark_layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    mark_layer.paste(mark_img, (0, 0), mask=final_mark_mask)
    img = Image.alpha_composite(img, mark_layer)
    
    # 6. Downsample from 4x to target size with Lanczos (Super-sampling)
    final_logo = img.resize((size, size), Image.Resampling.LANCZOS)
    return final_logo

if __name__ == "__main__":
    print("=" * 60)
    print("Minimalist Stash Icon Generator (Pure Python)")
    print("Zero AI • 100% Procedural Geometric Minimalism")
    print("=" * 60)
    
    # 1. 1024x1024 High-Res Logo
    print("[1/3] Generating 1024x1024 Minimal Master Logo...")
    logo_1024 = create_minimal_stash_logo(1024)
    out_1024 = "/storage/emulated/0/Download/stash_logo.png"
    logo_1024.save(out_1024, "PNG", optimize=True)
    print(f"      ✓ Saved to: {out_1024}")
    
    # 2. 512x512 Clean Store Asset
    print("[2/3] Generating 512x512 Minimal Store Icon...")
    logo_512 = create_minimal_stash_logo(512)
    out_512 = "/storage/emulated/0/Download/stash_icon_512.png"
    logo_512.save(out_512, "PNG", optimize=True)
    print(f"      ✓ Saved to: {out_512}")
    
    # 3. Android Mipmap densities
    print("[3/3] Generating Android App Mipmap Assets...")
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
        icon = create_minimal_stash_logo(px)
        icon.save(os.path.join(folder_path, "ic_launcher.png"), "PNG", optimize=True)
        icon.save(os.path.join(folder_path, "ic_launcher_round.png"), "PNG", optimize=True)
        print(f"      ✓ {folder}: {px}x{px} px")

    print("=" * 60)
    print("SUCCESS: Minimalist icon generation complete!")
    print("=" * 60)
