#!/usr/bin/env python3
"""Writes tokens.css from tokens.json. Run after editing tokens.json: python3 build-tokens-css.py"""
import json
import re
from pathlib import Path

here = Path(__file__).parent
d = json.loads((here / "tokens.json").read_text())


def css_value(v):
    # "{surface}" refers to another token.
    return re.sub(r"\{([a-z-]+)\}", r"var(--\1)", v)


lines = ["/* My Music App design tokens. Generated from tokens.json; edit that file, not this one. */", ":root {"]
lines += [f"  --{t['name']}: {css_value(t['value'])};" for t in d["color"]["tokens"]]
for group in ("spacing", "radius", "size", "textScale"):
    lines += [f"  --{t['name']}: {t['value']};" for t in d[group]["tokens"]]
lines += [f"  --font-{k}: {v};" for k, v in d["type"]["families"].items()]
lines.append("}")
for g in d["type"]["groups"]:
    for s in g["styles"]:
        extra = " font-variant-numeric: tabular-nums;" if s["name"] == "time" else ""
        lines.append(
            f".{s['name']} {{ font-family: var(--font-{g['family']}); font-size: {s['fontSize']}; "
            f"line-height: {s['lineHeight']}; font-weight: {s['fontWeight']};{extra} }}"
        )
(here / "tokens.css").write_text("\n".join(lines) + "\n")
