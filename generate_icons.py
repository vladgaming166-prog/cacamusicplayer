#!/usr/bin/env python3
"""Generate 2013-era CacaMusicPlayer PNG icons without third-party libraries."""
import os, struct, zlib, math

ROOT = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(ROOT, "app", "src", "main", "res")


def write_png(path, pixels, w, h):
    raw = bytearray()
    for y in range(h):
        raw.append(0)
        row = pixels[y]
        for x in range(w):
            r, g, b, a = row[x]
            raw.extend((r & 255, g & 255, b & 255, a & 255))
    compressed = zlib.compress(bytes(raw), 9)

    def chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", compressed) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


class Canvas(object):
    def __init__(self, w, h):
        self.w = w
        self.h = h
        self.px = [[(0, 0, 0, 0) for _ in range(w)] for _ in range(h)]

    def _blend(self, x, y, color):
        if x < 0 or y < 0 or x >= self.w or y >= self.h:
            return
        r, g, b, a = color
        if a <= 0:
            return
        if a >= 255:
            self.px[y][x] = (r, g, b, 255)
            return
        br, bg, bb, ba = self.px[y][x]
        alpha = a / 255.0
        inv = 1.0 - alpha
        out_a = a + ba * inv
        if out_a <= 0:
            return
        self.px[y][x] = (
            int(r * alpha + br * inv),
            int(g * alpha + bg * inv),
            int(b * alpha + bb * inv),
            int(min(255, out_a)),
        )

    def fill(self, color):
        for y in range(self.h):
            for x in range(self.w):
                self.px[y][x] = color

    def rounded_rect(self, x0, y0, x1, y1, rad, color):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                dx = 0
                dy = 0
                if x < x0 + rad:
                    dx = x0 + rad - x
                elif x > x1 - rad:
                    dx = x - (x1 - rad)
                if y < y0 + rad:
                    dy = y0 + rad - y
                elif y > y1 - rad:
                    dy = y - (y1 - rad)
                if dx > 0 and dy > 0:
                    if dx * dx + dy * dy > rad * rad + rad:
                        continue
                    dist = math.sqrt(dx * dx + dy * dy)
                    edge = rad - dist
                    if edge < 1:
                        a = int(color[3] * max(0, edge))
                        self._blend(x, y, (color[0], color[1], color[2], a))
                    else:
                        self._blend(x, y, color)
                else:
                    self._blend(x, y, color)

    def circle(self, cx, cy, r, color):
        r2 = r * r
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                d2 = (x - cx) * (x - cx) + (y - cy) * (y - cy)
                if d2 <= r2:
                    self._blend(x, y, color)
                else:
                    dist = math.sqrt(d2) - r
                    if dist < 1:
                        a = int(color[3] * max(0.0, 1.0 - dist))
                        self._blend(x, y, (color[0], color[1], color[2], a))

    def ellipse(self, cx, cy, rx, ry, color):
        for y in range(int(cy - ry - 1), int(cy + ry + 2)):
            for x in range(int(cx - rx - 1), int(cx + rx + 2)):
                nx = (x - cx) / float(rx) if rx else 999
                ny = (y - cy) / float(ry) if ry else 999
                d = nx * nx + ny * ny
                if d <= 1.0:
                    self._blend(x, y, color)

    def rect(self, x0, y0, x1, y1, color):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                self._blend(x, y, color)

    def triangle(self, p1, p2, p3, color):
        xs = [p1[0], p2[0], p3[0]]
        ys = [p1[1], p2[1], p3[1]]
        minx, maxx = int(min(xs)), int(max(xs))
        miny, maxy = int(min(ys)), int(max(ys))

        def sign(ax, ay, bx, by, cx, cy):
            return (ax - cx) * (by - cy) - (bx - cx) * (ay - cy)

        for y in range(miny, maxy + 1):
            for x in range(minx, maxx + 1):
                b1 = sign(x, y, p1[0], p1[1], p2[0], p2[1]) < 0.0
                b2 = sign(x, y, p2[0], p2[1], p3[0], p3[1]) < 0.0
                b3 = sign(x, y, p3[0], p3[1], p1[0], p1[1]) < 0.0
                if b1 == b2 == b3:
                    self._blend(x, y, color)

    def line(self, x0, y0, x1, y1, thickness, color):
        steps = int(max(abs(x1 - x0), abs(y1 - y0), 1)) * 2
        for i in range(steps + 1):
            t = i / float(steps)
            x = x0 + (x1 - x0) * t
            y = y0 + (y1 - y0) * t
            self.circle(x, y, thickness / 2.0, color)

    def punch_circle(self, cx, cy, r):
        r2 = r * r
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                if x < 0 or y < 0 or x >= self.w or y >= self.h:
                    continue
                if (x - cx) * (x - cx) + (y - cy) * (y - cy) <= r2:
                    self.px[y][x] = (0, 0, 0, 0)

    def save(self, path):
        write_png(path, self.px, self.w, self.h)


WHITE = (255, 255, 255, 255)
RED = (221, 25, 29, 255)
RED_DARK = (183, 28, 28, 255)
RED_LIGHT = (239, 83, 80, 255)
GLOSS = (255, 255, 255, 70)
SHADOW = (0, 0, 0, 50)
GRAY = (117, 117, 117, 255)


def draw_note(c, cx, cy, s, color):
    """Classic eighth-note used by many 2012-2014 Android music apps."""
    head_r = s * 0.16
    c.ellipse(cx - s * 0.16, cy + s * 0.22, head_r * 1.25, head_r, color)
    stem_x = cx - s * 0.16 + head_r * 1.15
    c.rect(stem_x - s * 0.035, cy - s * 0.32, stem_x + s * 0.035, cy + s * 0.22, color)
    # flag
    c.triangle(
        (stem_x, cy - s * 0.32),
        (stem_x + s * 0.28, cy - s * 0.18),
        (stem_x, cy - s * 0.10),
        color,
    )


def launcher(size, pad_ratio=0.08):
    c = Canvas(size, size)
    pad = int(size * pad_ratio)
    rad = int(size * 0.18)
    c.rounded_rect(pad, pad, size - pad - 1, size - pad - 1, rad, RED)
    # inner highlight like ICS/JB icons
    c.ellipse(size * 0.5, size * 0.28, size * 0.42, size * 0.22, GLOSS)
    # subtle bottom shade
    c.ellipse(size * 0.5, size * 0.82, size * 0.40, size * 0.16, SHADOW)
    draw_note(c, size * 0.52, size * 0.50, size * 0.52, WHITE)
    return c


def foreground(size):
    c = Canvas(size, size)
    # adaptive safe zone ~ 66%
    draw_note(c, size * 0.52, size * 0.50, size * 0.42, WHITE)
    return c


def icon_play(size):
    c = Canvas(size, size)
    m = size * 0.22
    c.triangle((m, m), (m, size - m), (size - m * 0.7, size * 0.5), WHITE)
    return c


def icon_pause(size):
    c = Canvas(size, size)
    w = size * 0.18
    g = size * 0.12
    x0 = size * 0.28
    c.rect(x0, size * 0.22, x0 + w, size * 0.78, WHITE)
    c.rect(x0 + w + g, size * 0.22, x0 + w + g + w, size * 0.78, WHITE)
    return c


def icon_next(size):
    c = Canvas(size, size)
    c.triangle((size * 0.18, size * 0.22), (size * 0.18, size * 0.78), (size * 0.62, size * 0.5), WHITE)
    c.rect(size * 0.66, size * 0.22, size * 0.80, size * 0.78, WHITE)
    return c


def icon_prev(size):
    c = Canvas(size, size)
    c.triangle((size * 0.82, size * 0.22), (size * 0.82, size * 0.78), (size * 0.38, size * 0.5), WHITE)
    c.rect(size * 0.20, size * 0.22, size * 0.34, size * 0.78, WHITE)
    return c


def icon_shuffle(size):
    c = Canvas(size, size)
    col = WHITE
    t = max(2, size * 0.08)
    c.line(size * 0.18, size * 0.32, size * 0.42, size * 0.32, t, col)
    c.line(size * 0.42, size * 0.32, size * 0.62, size * 0.68, t, col)
    c.line(size * 0.62, size * 0.68, size * 0.82, size * 0.68, t, col)
    c.triangle((size * 0.78, size * 0.55), (size * 0.78, size * 0.82), (size * 0.95, size * 0.68), col)
    c.line(size * 0.18, size * 0.68, size * 0.38, size * 0.68, t, col)
    c.line(size * 0.38, size * 0.68, size * 0.55, size * 0.45, t, col)
    return c


def icon_repeat(size):
    c = Canvas(size, size)
    t = max(2, size * 0.08)
    c.line(size * 0.25, size * 0.38, size * 0.75, size * 0.38, t, WHITE)
    c.line(size * 0.75, size * 0.38, size * 0.75, size * 0.62, t, WHITE)
    c.triangle((size * 0.62, size * 0.30), (size * 0.88, size * 0.30), (size * 0.75, size * 0.14), WHITE)
    c.line(size * 0.75, size * 0.62, size * 0.25, size * 0.62, t, WHITE)
    c.line(size * 0.25, size * 0.62, size * 0.25, size * 0.38, t, WHITE)
    c.triangle((size * 0.12, size * 0.70), (size * 0.38, size * 0.70), (size * 0.25, size * 0.86), WHITE)
    return c


def icon_repeat_one(size):
    c = icon_repeat(size)
    # simple "1"
    c.rect(size * 0.46, size * 0.42, size * 0.54, size * 0.58, WHITE)
    return c


def icon_heart(size, filled=True):
    c = Canvas(size, size)
    col = WHITE if filled else (221, 25, 29, 255)
    c.circle(size * 0.36, size * 0.40, size * 0.18, col)
    c.circle(size * 0.64, size * 0.40, size * 0.18, col)
    c.triangle((size * 0.18, size * 0.46), (size * 0.82, size * 0.46), (size * 0.50, size * 0.86), col)
    return c


def icon_search(size):
    c = Canvas(size, size)
    c.circle(size * 0.42, size * 0.42, size * 0.20, WHITE)
    # hole
    c.circle(size * 0.42, size * 0.42, size * 0.12, (0, 0, 0, 0))
    # clear hole by punching transparent - better redraw ring
    return _search_ring(size)


def _search_ring(size):
    c = Canvas(size, size)
    cx, cy, r, t = size * 0.42, size * 0.42, size * 0.18, size * 0.07
    for y in range(size):
        for x in range(size):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if abs(d - r) <= t:
                c._blend(x, y, WHITE)
    c.line(size * 0.55, size * 0.55, size * 0.82, size * 0.82, size * 0.10, WHITE)
    return c


def icon_folder(size):
    c = Canvas(size, size)
    c.rounded_rect(size * 0.16, size * 0.38, size * 0.84, size * 0.78, size * 0.06, WHITE)
    c.rounded_rect(size * 0.16, size * 0.28, size * 0.48, size * 0.42, size * 0.05, WHITE)
    return c


def icon_person(size):
    c = Canvas(size, size)
    c.circle(size * 0.5, size * 0.34, size * 0.16, WHITE)
    c.ellipse(size * 0.5, size * 0.72, size * 0.28, size * 0.18, WHITE)
    return c


def icon_disc(size):
    c = Canvas(size, size)
    c.circle(size * 0.5, size * 0.5, size * 0.32, WHITE)
    c.circle(size * 0.5, size * 0.5, size * 0.12, RED)
    c.circle(size * 0.5, size * 0.5, size * 0.05, WHITE)
    return c


def icon_clock(size):
    c = Canvas(size, size)
    cx = cy = size * 0.5
    r = size * 0.32
    # ring
    for y in range(size):
        for x in range(size):
            d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2)
            if abs(d - r) <= size * 0.06:
                c._blend(x, y, WHITE)
    c.rect(cx - 1, cy - r * 0.55, cx + 1, cy, WHITE)
    c.rect(cx, cy - 1, cx + r * 0.35, cy + 1, WHITE)
    return c


def icon_gear(size):
    c = Canvas(size, size)
    cx = cy = size * 0.5
    c.circle(cx, cy, size * 0.22, WHITE)
    for i in range(8):
        ang = i * math.pi / 4.0
        x = cx + math.cos(ang) * size * 0.30
        y = cy + math.sin(ang) * size * 0.30
        c.circle(x, y, size * 0.07, WHITE)
    c.punch_circle(cx, cy, size * 0.09)
    return c


def icon_cloud(size):
    c = Canvas(size, size)
    c.circle(size * 0.38, size * 0.52, size * 0.16, WHITE)
    c.circle(size * 0.58, size * 0.48, size * 0.20, WHITE)
    c.circle(size * 0.72, size * 0.55, size * 0.14, WHITE)
    c.rect(size * 0.30, size * 0.52, size * 0.78, size * 0.70, WHITE)
    return c


def icon_settings_row(size):
    return icon_gear(size)


def icon_notification(size):
    c = Canvas(size, size)
    draw_note(c, size * 0.52, size * 0.50, size * 0.70, WHITE)
    return c


DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}


def save_density(name, drawer, base=24, mipmap=False, extra_folder=None):
    for dens, scale in DENSITIES.items():
        size = int(round(base * scale))
        folder = ("mipmap-" + dens) if mipmap else ("drawable-" + dens)
        path = os.path.join(RES, folder, name + ".png")
        drawer(size).save(path)
    if extra_folder:
        drawer(int(base * 2)).save(os.path.join(RES, extra_folder, name + ".png"))


def main():
    # launcher 48dp
    save_density("ic_launcher", lambda s: launcher(s), base=48, mipmap=True)
    save_density("ic_launcher_foreground", lambda s: foreground(s), base=108, mipmap=True)
    # also drawable fallback
    launcher(96).save(os.path.join(RES, "drawable", "ic_launcher.png"))

    save_density("ic_play", icon_play, 32)
    save_density("ic_pause", icon_pause, 32)
    save_density("ic_next", icon_next, 32)
    save_density("ic_prev", icon_prev, 32)
    save_density("ic_shuffle", icon_shuffle, 24)
    save_density("ic_repeat", icon_repeat, 24)
    save_density("ic_repeat_one", icon_repeat_one, 24)
    save_density("ic_heart", lambda s: icon_heart(s, True), 24)
    save_density("ic_search", icon_search, 24)
    save_density("ic_playlist", icon_folder, 24)
    save_density("ic_artist", icon_person, 24)
    save_density("ic_album", icon_disc, 24)
    save_density("ic_song", lambda s: _note_only(s), 24)
    save_density("ic_recent", icon_clock, 24)
    save_density("ic_online", icon_cloud, 24)
    save_density("ic_settings", icon_gear, 24)
    save_density("ic_stat_note", icon_notification, 24)
    # default artwork
    save_density("ic_default_art", lambda s: launcher(s, 0.0), 48)
    print("icons ok")


def _note_only(size):
    c = Canvas(size, size)
    draw_note(c, size * 0.52, size * 0.50, size * 0.62, WHITE)
    return c


if __name__ == "__main__":
    main()
