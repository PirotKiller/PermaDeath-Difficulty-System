#!/usr/bin/env python3
"""
Validates resourcepack/ and packages it as target/pdds-relics.zip.

Run from the repo root:  python3 tools/build_resourcepack_zip.py

A resource pack fails silently: a typo'd texture path renders as the black-and-magenta
"missing texture" on every client and nothing appears in any log. Since these overrides
also apply to ordinary swords and elytra, a bad reference is a server-wide cosmetic
outage. So every model and texture reference is resolved before anything is zipped --
against the pack for `pdds:`, and against the real 1.19.2 client jar for `minecraft:`.
"""

import hashlib
import json
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from generate_resourcepack import PACK, RELICS, ROOT, ensure_client_jar  # noqa: E402

OUT = os.path.join(ROOT, "target", "pdds-relics.zip")
EXCLUDE = {"README.md"}


def split_ref(ref):
    """'minecraft:item/foo' or 'item/foo' -> ('minecraft', 'item/foo')."""
    return tuple(ref.split(":", 1)) if ":" in ref else ("minecraft", ref)


def validate(jar):
    errors = []
    vanilla = set(jar.namelist())

    def model_exists(ns, path):
        if ns == "pdds":
            return os.path.exists(os.path.join(PACK, "assets", "pdds", "models", path + ".json"))
        if path.startswith("builtin/"):
            return True
        local = os.path.join(PACK, "assets", "minecraft", "models", path + ".json")
        return os.path.exists(local) or f"assets/minecraft/models/{path}.json" in vanilla

    def texture_exists(ns, path):
        if ns == "pdds":
            return os.path.exists(os.path.join(PACK, "assets", "pdds", "textures", path + ".png"))
        local = os.path.join(PACK, "assets", "minecraft", "textures", path + ".png")
        return os.path.exists(local) or f"assets/minecraft/textures/{path}.png" in vanilla

    meta_path = os.path.join(PACK, "pack.mcmeta")
    if not os.path.exists(meta_path):
        errors.append("pack.mcmeta is missing")
    else:
        with open(meta_path, encoding="utf-8") as fh:
            meta = json.load(fh)
        if meta.get("pack", {}).get("pack_format") != 9:
            errors.append(f"pack_format is {meta.get('pack', {}).get('pack_format')}, expected 9 "
                          "for Minecraft 1.19.x")

    models_seen = 0
    for dirpath, _, files in os.walk(PACK):
        for name in files:
            if not name.endswith(".json") or name == "pack.mcmeta":
                continue
            full = os.path.join(dirpath, name)
            rel = os.path.relpath(full, PACK).replace("\\", "/")
            try:
                with open(full, encoding="utf-8") as fh:
                    model = json.load(fh)
            except json.JSONDecodeError as exc:
                errors.append(f"{rel}: invalid JSON ({exc})")
                continue
            models_seen += 1

            parent = model.get("parent")
            if parent and not model_exists(*split_ref(parent)):
                errors.append(f"{rel}: parent '{parent}' does not resolve")

            for key, ref in (model.get("textures") or {}).items():
                if not texture_exists(*split_ref(ref)):
                    errors.append(f"{rel}: texture '{ref}' ({key}) does not resolve")

            for override in model.get("overrides") or []:
                ref = override.get("model")
                if not ref or not model_exists(*split_ref(ref)):
                    errors.append(f"{rel}: override model '{ref}' does not resolve")

    # Every relic must actually be reachable: its CustomModelData has to appear in the
    # rewritten base model, or the custom art is dead weight nothing will ever select.
    reachable = set()
    base_dir = os.path.join(PACK, "assets", "minecraft", "models", "item")
    for name in os.listdir(base_dir) if os.path.isdir(base_dir) else []:
        with open(os.path.join(base_dir, name), encoding="utf-8") as fh:
            for override in json.load(fh).get("overrides") or []:
                cmd = override.get("predicate", {}).get("custom_model_data")
                if cmd is not None:
                    reachable.add(cmd)
    for relic_id, spec in RELICS.items():
        if spec["cmd"] not in reachable:
            errors.append(f"{relic_id}: CustomModelData {spec['cmd']} is not referenced by "
                          f"any base model override")

    return errors, models_seen


def main():
    jar = ensure_client_jar()
    errors, models_seen = validate(jar)
    if errors:
        print("PACK VALIDATION FAILED:", file=sys.stderr)
        for err in errors:
            print("  - " + err, file=sys.stderr)
        return 1
    print(f"Validated {models_seen} model(s); all references resolve.")

    # Deterministic: fixed timestamps and sorted entries, so identical content always
    # hashes identically. Otherwise every rebuild changes resource-pack-sha1 and there's
    # no way to tell a real content change from a fresh mtime.
    entries = []
    for dirpath, _, files in os.walk(PACK):
        for name in files:
            if name in EXCLUDE:
                continue
            full = os.path.join(dirpath, name)
            entries.append((os.path.relpath(full, PACK).replace("\\", "/"), full))
    entries.sort()

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as zf:
        for arc, full in entries:
            info = zipfile.ZipInfo(arc, date_time=(1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            with open(full, "rb") as fh:
                zf.writestr(info, fh.read())
    count = len(entries)

    with open(OUT, "rb") as fh:
        digest = hashlib.sha1(fh.read()).hexdigest()

    print(f"Wrote {os.path.relpath(OUT, ROOT)} ({count} entries, {os.path.getsize(OUT)} bytes)")
    print()
    print("server.properties:")
    print(f"  resource-pack-sha1={digest}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
