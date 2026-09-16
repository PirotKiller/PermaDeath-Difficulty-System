#!/usr/bin/env python3
"""
Generates the PermaDeath relic resource pack under resourcepack/.

Run from the repo root:  python3 tools/generate_resourcepack.py

Everything vanilla in this pack is taken from the real 1.19.2 client jar, which is
downloaded on first run and SHA-1 verified against Mojang's manifest. That matters:
the base item models we rewrite must match vanilla byte-for-byte apart from the
overrides we append, or we change how ordinary swords/apples/elytra look for every
player on the server. Reading them from the jar removes the guesswork.

Relic textures are recoloured from the genuine vanilla sprite, so each relic keeps the
silhouette and shading of the item it actually is, and reads as a special version of it.

Cross-checks the manifest against RelicManager.java (CustomModelData) and
RelicDefinitions.java (base materials) and fails loudly on drift.
"""

import copy
import hashlib
import io
import json
import os
import re
import sys
import urllib.request
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK = os.path.join(ROOT, "resourcepack")
SRC = os.path.join(ROOT, "src", "main", "java", "me", "pirot", "permaDeathDifficultySystem", "relics")
CACHE = os.path.join(ROOT, ".cache")

PACK_FORMAT = 9  # Minecraft 1.19.x

CLIENT_JAR_URL = ("https://piston-data.mojang.com/v1/objects/"
                  "055b30d860ead928cba3849ba920c88b6950b654/client.jar")
CLIENT_JAR_SHA1 = "055b30d860ead928cba3849ba920c88b6950b654"
CLIENT_JAR = os.path.join(CACHE, "minecraft-1.19.2-client.jar")

# Items whose inventory sprite the client multiplies by a hardcoded tint colour.
# Vanilla's fern item is greyscale art that ItemColors tints with
# GrassColor.getDefaultColor() -- grass colormap pixel (127, 127). Our generated art is
# already coloured, so we divide it out or the relic comes out swamp green.
TINTED_BASES = {"fern": (124, 189, 107)}

# Vanilla shield has no flat sprite (its model is builtin/entity), so we draw one.
PROCEDURAL_SPRITE = "shield"

# relic id -> base item, CustomModelData, and a 5-stop dark->light colour ramp.
# The ramp replaces the vanilla palette while the sprite's own shading decides which
# stop each pixel lands on, so the item stays recognisably a sword/helmet/totem.
RELICS = {
    # --- Tier 1 ---
    "ultra-shield": dict(base="shield", cmd=1001, ramp=[
        "#10141c", "#1d3550", "#2f6fa8", "#6fc0e8", "#e8f8ff"]),
    "ultra-totem": dict(base="totem_of_undying", cmd=1002, ramp=[
        "#07242b", "#0d4a52", "#14867f", "#38d6bd", "#d4fff4"]),
    "dragon-slayer": dict(base="diamond_sword", cmd=1003, ramp=[
        "#1a0a26", "#3d1260", "#7326a8", "#c04fe8", "#f5d6ff"]),
    "mountain-devourer": dict(base="diamond_pickaxe", cmd=1004, ramp=[
        "#1b1712", "#40352a", "#786551", "#d98a2b", "#ffdca0"]),
    "elder-turtle-helmet": dict(base="turtle_helmet", cmd=1005, ramp=[
        "#0c1f14", "#17452c", "#2c7d4e", "#55c47f", "#d8ffe2"]),

    # --- Tier 2 ---
    "propulsion-trident": dict(base="trident", cmd=2001, ramp=[
        "#06202e", "#0b4560", "#1487ad", "#46d3e8", "#e6ffff"]),
    "olympic-torch": dict(base="blaze_rod", cmd=2002, ramp=[
        "#2a1002", "#5e2a04", "#b5610a", "#f0a81e", "#fff3b0"]),
    "lucky-clover": dict(base="fern", cmd=2003, ramp=[
        "#0b2410", "#174a1d", "#2c8a2e", "#5fd44f", "#ddffb0"]),
    "infernal-elytra": dict(base="elytra", cmd=2004, ramp=[
        "#140406", "#3d0a10", "#7a1420", "#c93040", "#ffb0b8"]),
    "sacred-pearl": dict(base="ender_pearl", cmd=2005, ramp=[
        "#2a2208", "#574512", "#a08428", "#e0c766", "#fffce0"]),
    "sacred-tree-apple": dict(base="enchanted_golden_apple", cmd=2006, ramp=[
        "#0d2416", "#1c4d2c", "#37944f", "#9fd45f", "#fff0b8"]),
    "world-eater": dict(base="netherite_pickaxe", cmd=2007, ramp=[
        "#0a0510", "#1d0f2e", "#3c2159", "#6b40a0", "#c9a8f0"]),
    "blade-of-olympus": dict(base="netherite_sword", cmd=2008, ramp=[
        "#2b2004", "#57400c", "#a3791a", "#e6c04a", "#fff8d0"]),
    "poseidons-trident": dict(base="trident", cmd=2009, ramp=[
        "#041826", "#0a3552", "#14689e", "#3fb0e0", "#d8f4ff"]),

    # --- Tier 3 ---
    "nautilus-helmet": dict(base="diamond_helmet", cmd=3001, ramp=[
        "#0a2428", "#14484e", "#248a86", "#55ccb8", "#e0fff8"]),
    "hercules-chestplate": dict(base="netherite_chestplate", cmd=3002, ramp=[
        "#200c06", "#4a1d0d", "#8a3c17", "#cf7030", "#ffca8a"]),
    "helmet-of-hades": dict(base="netherite_helmet", cmd=3003, ramp=[
        "#060407", "#150d1c", "#2c1d3d", "#543a6e", "#9a7fc0"]),
    "aegis": dict(base="shield", cmd=3004, ramp=[
        "#2a2408", "#564a12", "#9c8a28", "#ddc95e", "#fffde8"]),
    "hermes-boots": dict(base="netherite_boots", cmd=3005, ramp=[
        "#0c1826", "#163049", "#2d6389", "#6fb4d8", "#ffe9a8"]),
    "spartan-greaves": dict(base="netherite_leggings", cmd=3006, ramp=[
        "#1c0a08", "#431611", "#7d2c1e", "#c05a36", "#f0a878"]),
    "curse-remover": dict(base="nether_star", cmd=3007, ramp=[
        "#1a1030", "#2e1c55", "#5a3d96", "#9b7fd6", "#ffffff"]),
    "totem-of-true-god": dict(base="totem_of_undying", cmd=3008, ramp=[
        "#2e2204", "#5c4310", "#a87f1e", "#f0cb52", "#fffbe0"]),
    "onigari-no-ryuuou": dict(base="netherite_sword", cmd=3009, ramp=[
        "#12040a", "#380a14", "#6e1220", "#b82638", "#ff9aa8"]),
}


# --------------------------------------------------------------------------- jar


def ensure_client_jar():
    """Downloads the 1.19.2 client jar into .cache/ if absent; always verifies SHA-1."""
    if not os.path.exists(CLIENT_JAR):
        os.makedirs(CACHE, exist_ok=True)
        print(f"Downloading 1.19.2 client jar (~21 MB) to {os.path.relpath(CLIENT_JAR, ROOT)} ...")
        with urllib.request.urlopen(CLIENT_JAR_URL, timeout=300) as resp:
            data = resp.read()
        with open(CLIENT_JAR, "wb") as fh:
            fh.write(data)

    with open(CLIENT_JAR, "rb") as fh:
        digest = hashlib.sha1(fh.read()).hexdigest()
    if digest != CLIENT_JAR_SHA1:
        raise SystemExit(
            f"client jar SHA-1 mismatch\n  expected {CLIENT_JAR_SHA1}\n  got      {digest}\n"
            f"Delete {CLIENT_JAR} and re-run.")
    return zipfile.ZipFile(CLIENT_JAR)


def vanilla_model(jar, name):
    return json.loads(jar.read(f"assets/minecraft/models/item/{name}.json"))


def vanilla_texture(jar, ref):
    """Loads a texture by its model reference, e.g. 'minecraft:item/diamond_sword'."""
    path = ref.split(":", 1)[-1]
    return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


# ----------------------------------------------------------------------- colour


def parse_hex(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def ramp_lookup(stops, t):
    """Linear interpolation across the ramp at position t in [0, 1]."""
    t = min(1.0, max(0.0, t))
    span = 1.0 / (len(stops) - 1)
    idx = min(len(stops) - 2, int(t / span))
    local = (t - idx * span) / span
    a, b = stops[idx], stops[idx + 1]
    return tuple(round(a[i] + (b[i] - a[i]) * local) for i in range(3))


def recolour(sprite, ramp_hex, tint=None):
    """
    Re-palettes a vanilla sprite through the relic's ramp.

    Pixel luminance picks the ramp position, so the source art's shading, outline and
    silhouette survive intact -- the result still reads as a sword, helmet or totem,
    just in the relic's colours.
    """
    stops = [parse_hex(c) for c in ramp_hex]
    px = sprite.load()
    w, h = sprite.size

    lums = []
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a > 0:
                lums.append(0.2126 * r + 0.7152 * g + 0.0722 * b)
    if not lums:
        raise ValueError("source sprite is fully transparent")

    # Stretch against the sprite's own range so low-contrast art still uses the full ramp.
    lo, hi = min(lums), max(lums)
    spread = max(1.0, hi - lo)

    clipped = 0
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    opx = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
            colour = ramp_lookup(stops, (lum - lo) / spread)
            if tint:
                # The client multiplies by `tint`; pre-divide so the final pixel is `colour`.
                adjusted = []
                for chan, tchan in zip(colour, tint):
                    val = round(chan * 255 / tchan)
                    if val > 255:
                        clipped += 1
                        val = 255
                    adjusted.append(val)
                colour = tuple(adjusted)
            opx[x, y] = (colour[0], colour[1], colour[2], a)
    return out, clipped


def shield_sprite():
    """
    A 16x16 heater-shield silhouette, shaded so `recolour` has something to work with.

    Vanilla's shield model is builtin/entity and has no flat sprite to borrow, so this
    stands in as the source art for the two shield relics.
    """
    rows = {1: (3, 12), 2: (2, 13), 3: (2, 13), 4: (2, 13), 5: (2, 13), 6: (2, 13),
            7: (2, 13), 8: (2, 13), 9: (3, 12), 10: (3, 12), 11: (4, 11),
            12: (5, 10), 13: (6, 9), 14: (7, 8)}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, (x0, x1) in rows.items():
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y == min(rows) or y == max(rows)
            if edge:
                value = 40                        # dark rim
            else:
                # Diagonal light from the upper left, plus a raised central boss.
                value = 200 - (x - x0) * 8 - (y - 1) * 6
                if 6 <= x <= 9 and 5 <= y <= 9:
                    value += 45
                value = max(60, min(245, value))
            px[x, y] = (value, value, value, 255)
    return img


# ------------------------------------------------------------------------ write


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        json.dump(data, fh, indent=2)
        fh.write("\n")


def main():
    jar = ensure_client_jar()

    write_json(os.path.join(PACK, "pack.mcmeta"), {
        "pack": {
            "pack_format": PACK_FORMAT,
            "description": "PermaDeath Difficulty System — relic models",
        }
    })

    shield_display = vanilla_model(jar, "shield")["display"]
    # Hand poses only. Leaving gui/ground/fixed to item/generated's defaults keeps the
    # inventory icon a clean upright sprite instead of the tilted 3D shield pose.
    hand_display = {k: v for k, v in shield_display.items() if "person" in k}
    # Vanilla's raised-shield hand transforms (translation puts it in front of the face).
    # Without swapping to these while blocking, the flat sprite stays at the player's side
    # and there is no visual feedback that the shield went up -- which reads as "right-click
    # does nothing." The mechanic is server-authoritative and was working; only the render
    # was missing.
    shield_blocking_display = vanilla_model(jar, "shield_blocking")["display"]
    blocking_hand_display = {k: v for k, v in shield_blocking_display.items() if "person" in k}

    overrides_by_base = {}
    tint_notes = []

    for relic_id, spec in sorted(RELICS.items(), key=lambda kv: kv[1]["cmd"]):
        base, cmd = spec["base"], spec["cmd"]

        if base == PROCEDURAL_SPRITE:
            source, parent = shield_sprite(), "item/generated"
        else:
            model = vanilla_model(jar, base)
            source = vanilla_texture(jar, model["textures"]["layer0"])
            parent = model["parent"]

        art, clipped = recolour(source, spec["ramp"], TINTED_BASES.get(base))
        if clipped:
            tint_notes.append(f"{relic_id}: {clipped} channel(s) clipped by tint compensation")

        tex_path = os.path.join(PACK, "assets", "pdds", "textures", "item", relic_id + ".png")
        os.makedirs(os.path.dirname(tex_path), exist_ok=True)
        art.save(tex_path)

        relic_model = {"parent": parent, "textures": {"layer0": "pdds:item/" + relic_id}}
        if base == PROCEDURAL_SPRITE:
            relic_model["display"] = copy.deepcopy(hand_display)
        write_json(os.path.join(PACK, "assets", "pdds", "models", "item", relic_id + ".json"),
                   relic_model)

        # Shield relics also get a "-blocking" variant with the raised-hand transforms,
        # referenced by shield.json's {blocking:1, custom_model_data:X} overrides below.
        if base == PROCEDURAL_SPRITE:
            blocking_model = {
                "parent": parent,
                "textures": {"layer0": "pdds:item/" + relic_id},
                "display": copy.deepcopy(blocking_hand_display),
            }
            write_json(
                os.path.join(PACK, "assets", "pdds", "models", "item", relic_id + "-blocking.json"),
                blocking_model,
            )

        overrides_by_base.setdefault(base, []).append((cmd, relic_id))

    # Rewrite each vanilla base model verbatim, appending only our overrides.
    for base, entries in sorted(overrides_by_base.items()):
        model = vanilla_model(jar, base)
        vanilla_overrides = model.get("overrides", [])
        ours = [{"predicate": {"custom_model_data": cmd}, "model": "pdds:item/" + rid}
                for cmd, rid in sorted(entries)]

        if base == "shield":
            # Minecraft takes the LAST override whose predicates all match (vanilla's own
            # crossbow.json relies on this). So: our plain variants, then vanilla's
            # blocking rule, then our blocking variants -- which therefore win while
            # blocking. Without the third group a relic shield would turn back into an
            # ordinary shield the moment the player raised it. The blocking variant points
            # at "-blocking" model so the sprite actually raises to the face, giving the
            # player the visual "shield is up" feedback they'd otherwise miss entirely.
            blocking = [{"predicate": {"blocking": 1, "custom_model_data": cmd},
                         "model": "pdds:item/" + rid + "-blocking"}
                        for cmd, rid in sorted(entries)]
            model["overrides"] = ours + vanilla_overrides + blocking
        else:
            # Vanilla's own state overrides go last so they keep winning. That way a
            # broken Infernal Elytra still shows the broken sprite instead of silently
            # rendering intact -- the player needs to see the damage state.
            model["overrides"] = ours + vanilla_overrides

        write_json(os.path.join(PACK, "assets", "minecraft", "models", "item", base + ".json"),
                   model)

    # --- Drift checks against the Java sources -------------------------------
    errors = []

    with open(os.path.join(SRC, "RelicManager.java"), encoding="utf-8") as fh:
        java_cmd = {rid: int(v) for rid, v in
                    re.findall(r'Map\.entry\("([a-z0-9-]+)",\s*(\d+)\)', fh.read())}

    for rid, spec in RELICS.items():
        if java_cmd.get(rid) != spec["cmd"]:
            errors.append(f"CustomModelData drift for {rid}: "
                          f"pack={spec['cmd']} java={java_cmd.get(rid)}")
    for rid in java_cmd:
        if rid not in RELICS:
            errors.append(f"{rid} has CustomModelData in Java but no pack manifest entry")

    with open(os.path.join(SRC, "RelicDefinitions.java"), encoding="utf-8") as fh:
        java_bases = [m.lower() for m in
                      re.findall(r"new ItemStack\(Material\.([A-Z_]+)", fh.read())]
    for rid, spec in RELICS.items():
        if spec["base"] not in java_bases:
            errors.append(f"{rid}: base item '{spec['base']}' not in RelicDefinitions.java")

    if errors:
        print("DRIFT DETECTED:", file=sys.stderr)
        for err in errors:
            print("  - " + err, file=sys.stderr)
        return 1

    print(f"Wrote {len(RELICS)} relic models + textures (all recoloured from vanilla 1.19.2 art)")
    print(f"Rewrote {len(overrides_by_base)} vanilla base models: "
          + ", ".join(sorted(overrides_by_base)))
    for note in tint_notes:
        print("  note: " + note)
    print("Manifest matches RelicManager.java and RelicDefinitions.java.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
