"""Rebuild Aurora # SVGs, Android vectors, and PNG previews (PNG requires Pillow)."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
OUT = ROOT / "branding/aurora-root"
COLORS = ["#00AFC8", "#3767E8", "#7544D8"]
BACKGROUND = "#EDF5FF"
BADGE = "#18263D"
# Original Aurora geometry from the non-root Xperia_Clear_All project.
PATHS = [
    "M57.912,37.373 Q58.400,36.500 59.400,36.500 L76.500,36.500 Q77.500,36.500 77.012,37.373 L73.288,44.027 Q72.800,44.900 71.800,44.900 L54.700,44.900 Q53.700,44.900 54.188,44.027 Z",
    "M45.712,49.973 Q46.200,49.100 47.200,49.100 L76.500,49.100 Q77.500,49.100 77.012,49.973 L73.288,56.627 Q72.800,57.500 71.800,57.500 L42.500,57.500 Q41.500,57.500 41.988,56.627 Z",
    "M33.712,62.773 Q34.200,61.900 35.200,61.900 L76.500,61.900 Q77.500,61.900 77.012,62.773 L73.288,69.427 Q72.800,70.300 71.800,70.300 L30.500,70.300 Q29.500,70.300 29.988,69.427 Z",
]
HASH = "M38,34.5 L36,45.5 M44,34.5 L42,45.5 M34.5,38 L46,38 M34,42 L45.5,42"
CIRCLE = "M50,40 A10,10 0,1 0,30,40 A10,10 0,1 0,50,40 Z"


def svg_content(monochrome=False):
    inks = ["#FFFFFF"] * 3 if monochrome else COLORS
    body = "".join(f'<path fill="{color}" d="{path}"/>' for color, path in zip(inks, PATHS))
    if not monochrome:
        body += f'<circle cx="40" cy="40" r="10" fill="{BADGE}"/>'
    return body + f'<path d="{HASH}" fill="none" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round"/>'


def vector(monochrome=False):
    inks = ["#FFFFFF"] * 3 if monochrome else COLORS
    body = "\n".join(f'    <path android:fillColor="{color}" android:pathData="{path}" />' for color, path in zip(inks, PATHS))
    if not monochrome:
        body += f'\n    <path android:fillColor="{BADGE}" android:pathData="{CIRCLE}" />'
    body += f'\n    <path android:fillColor="#00000000" android:strokeColor="#FFFFFF" android:strokeWidth="2" android:strokeLineCap="round" android:pathData="{HASH}" />'
    return '<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n' + body + '\n</vector>\n'


def polygon(path):
    """Flatten these M/L/Q/Z logo paths for the optional PNG preview only."""
    tokens = iter(re.findall(r"[MLQZ]|-?\d+(?:\.\d+)?", path))
    points = []
    for command in tokens:
        if command in ("M", "L"):
            points.append((float(next(tokens)), float(next(tokens))))
        elif command == "Q":
            start = points[-1]
            control = (float(next(tokens)), float(next(tokens)))
            end = (float(next(tokens)), float(next(tokens)))
            for step in range(1, 17):
                t = step / 16
                points.append(tuple((1-t)**2 * start[i] + 2*(1-t)*t * control[i] + t*t * end[i] for i in range(2)))
    return points


def png(size, circle=True, monochrome=False):
    from PIL import Image, ImageDraw
    scale = size * 4 / 108
    image = Image.new("RGBA", (size * 4, size * 4))
    draw = ImageDraw.Draw(image)
    bounds = (0, 0, size * 4 - 1, size * 4 - 1)
    background = "#1D2636" if monochrome else BACKGROUND
    if circle:
        draw.ellipse(bounds, fill=background)
    else:
        draw.rectangle(bounds, fill=background)
    for path, color in zip(PATHS, ["#DCEBFF"]*3 if monochrome else COLORS):
        draw.polygon([(x*scale, y*scale) for x, y in polygon(path)], fill=color)
    if not monochrome:
        draw.ellipse(tuple(v*scale for v in (30, 30, 50, 50)), fill=BADGE)
    for x1, y1, x2, y2 in [(38,34.5,36,45.5),(44,34.5,42,45.5),(34.5,38,46,38),(34,42,45.5,42)]:
        color = "#DCEBFF" if monochrome else "#FFFFFF"
        draw.line((x1*scale,y1*scale,x2*scale,y2*scale), fill=color, width=round(2*scale))
        for x,y in [(x1,y1),(x2,y2)]:
            draw.ellipse(((x-1)*scale,(y-1)*scale,(x+1)*scale,(y+1)*scale), fill=color)
    return image.resize((size,size), Image.Resampling.LANCZOS)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, background, mono in [
        ("icon-circle", f'<circle cx="54" cy="54" r="54" fill="{BACKGROUND}"/>', False),
        ("icon-square", f'<rect width="108" height="108" fill="{BACKGROUND}"/>', False),
        ("logo-monochrome", "", True),
    ]:
        (OUT / f"{name}.svg").write_text('<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108">' + background + svg_content(mono) + '</svg>\n', encoding="utf-8")
    (RES / "drawable/ic_launcher_foreground.xml").write_text(vector(), encoding="utf-8")
    (RES / "drawable/ic_launcher_monochrome.xml").write_text(vector(True), encoding="utf-8")
    (RES / "values/icon_colors.xml").write_text(f'<resources><color name="launcher_background">{BACKGROUND}</color></resources>\n', encoding="utf-8")
    adaptive = '''<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
'''
    for name in ["ic_launcher", "ic_launcher_round"]:
        (RES / f"mipmap-anydpi/{name}.xml").write_text(adaptive, encoding="utf-8")
    try:
        from PIL import Image
    except ImportError:
        print("Vectors generated. Install Pillow to also rebuild PNG previews.")
        return
    png(512).save(OUT / "icon-circle.png")
    png(512, circle=False).save(OUT / "icon-square.png")
    preview = Image.new("RGBA", (800, 320), "#F8FAFD")
    preview.alpha_composite(png(240), (24, 24))
    preview.alpha_composite(png(192, circle=False), (300, 48))
    preview.alpha_composite(png(192, monochrome=True), (552, 48))
    for index, size in enumerate([48, 32, 24]):
        preview.alpha_composite(png(size), (320 + index*64, 266))
    preview.convert("RGB").save(OUT / "preview.png")
    # Parse generated XML as a fast authoring check; Android rendering is tested separately.
    for path in [RES / "drawable/ic_launcher_foreground.xml", RES / "drawable/ic_launcher_monochrome.xml"]:
        ET.parse(path)
    print("Aurora # vectors and PNG previews generated.")


if __name__ == "__main__":
    main()
