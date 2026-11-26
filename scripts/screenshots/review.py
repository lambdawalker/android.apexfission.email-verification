"""Capture and compare the module's Compose screenshots without overwriting approved references."""

import argparse
from collections import Counter
import hashlib
import html
import json
import os
from pathlib import Path
import shutil
import struct
import subprocess
import tempfile
import tomllib

ROOT = Path(__file__).resolve().parents[2]
DOCS = ROOT / "docs/screenshots"
OUTPUT = ROOT / "build/screenshot-review"
MANIFEST = DOCS / "baselines.json"
REFERENCE = "src/screenshotTestDebug/reference"
SCENARIOS = json.loads((DOCS / "scenarios.json").read_text())
INPUTS = [
    "app/src/main", "app/src/screenshotTest", "app/build.gradle.kts",
    "email-verification/src/main", "email-verification/src/screenshotTest",
    "email-verification/build.gradle.kts", "gradle", "gradle.properties",
    "settings.gradle.kts", "docs/screenshots/scenarios.json",
    ".github/workflows/verify.yml",
]

def ui_common_dependency():
    with (ROOT / "gradle/libs.versions.toml").open("rb") as catalog:
        dependency = tomllib.load(catalog)["libraries"]["ui-common"]
    return {"repository": "https://jitpack.io", **dependency}


def ui_common_source():
    source = os.environ.get("ORG_GRADLE_PROJECT_uiCommonSourceDir")
    if not source:
        return None
    checkout = Path(source).resolve()
    paths = subprocess.check_output(
        ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard", "--",
         "ui-common/src/main", "ui-common/build.gradle.kts", "gradle", "settings.gradle.kts",
         "build.gradle.kts", "gradle.properties"], cwd=checkout,
    )
    digest = hashlib.sha256()
    for name in sorted(set(paths.rstrip(b"\0").split(b"\0"))):
        path = checkout / name.decode()
        digest.update(name + b"\0" + hashlib.sha256(path.read_bytes()).digest())
    return {
        "repository": "lambdawalker/android.ui.common",
        "source": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=checkout, text=True).strip(),
        "input_sha256": digest.hexdigest(),
        "dirty": bool(subprocess.check_output(["git", "status", "--short"], cwd=checkout)),
    }


def capture_input():
    listing = subprocess.check_output(
        ["git", "ls-files", "-z", "--cached", "--others", "--exclude-standard", "--", *INPUTS],
        cwd=ROOT,
    )
    digest = hashlib.sha256()
    for name in sorted(set(listing.rstrip(b"\0").split(b"\0"))):
        path = ROOT / name.decode()
        data = path.read_bytes() if path.exists() else b"<deleted>"
        digest.update(name + b"\0" + hashlib.sha256(data).digest())
    digest.update(json.dumps(ui_common_source(), sort_keys=True).encode())
    return digest.hexdigest()


def inventory(directory, require_current=True):
    images = {}
    for module, expected in SCENARIOS.items():
        counts = Counter()
        for path in sorted((directory / module).rglob("*.png")):
            name = path.stem.split("_", 1)[0]
            if require_current and name not in expected:
                raise ValueError(f"Unexpected screenshot {path}")
            data = path.read_bytes()
            if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
                raise ValueError(f"Invalid PNG {path}")
            width, height = struct.unpack(">II", data[16:24])
            if not width or not height:
                raise ValueError(f"Invalid dimensions for {path}")
            counts[name] += 1
            images[path.relative_to(directory).as_posix()] = {
                "sha256": hashlib.sha256(data).hexdigest(), "width": width, "height": height,
            }
        if require_current and counts != expected:
            raise ValueError(f"{module} expected {expected}, rendered {dict(counts)}")
    return images


def write_json(path, contents):
    path.write_text(json.dumps(contents, indent=2) + "\n")


def collect_approved(destination):
    for module in SCENARIOS:
        source = ROOT / module / REFERENCE
        if source.exists():
            shutil.copytree(source, destination / module)
    if not MANIFEST.exists():
        if any(destination.glob("**/*.png")):
            raise ValueError("Reference images require an approved manifest.")
        return None
    approved = json.loads(MANIFEST.read_text())
    if approved["images"] != inventory(destination, require_current=False):
        raise ValueError("Approved reference files do not match their manifest.")
    return approved


def gallery(before, after):
    labels = {}
    for name in sorted(before.keys() | after.keys()):
        if name not in before:
            labels[name] = "Added"
        elif name not in after:
            labels[name] = "Removed"
        elif before[name] != after[name]:
            labels[name] = "Changed"
    names = list(labels or {name: "Identical" for name in after})
    rows = []
    for name in names:
        cells = []
        for files, folder in [(before, "approved"), (after, "candidates")]:
            image = f'<a href="{folder}/{html.escape(name)}"><img width="240" src="{folder}/{html.escape(name)}"></a>'
            cells.append(image if name in files else "Missing")
        rows.append(f'<tr><td>{labels.get(name, "Identical")}<br>{html.escape(name)}</td>'
                    f'<td>{cells[0]}</td><td>{cells[1]}</td></tr>')
    (OUTPUT / "index.html").write_text(
        '<!doctype html><html lang="en"><meta charset="utf-8"><title>Screenshot review</title>'
        '<h1>Screenshot review</h1><table><thead><tr><th>Scenario</th><th>Approved</th>'
        '<th>Candidate</th></tr></thead><tbody>' + "".join(rows) + "</tbody></table></html>"
    )


def capture():
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    OUTPUT.mkdir(parents=True)
    approved = collect_approved(OUTPUT / "approved")
    candidates = OUTPUT / "candidates"
    inputs = capture_input()
    # The renderer writes to module reference directories. Back them up and restore them even on failure.
    with tempfile.TemporaryDirectory(dir=OUTPUT) as backup:
        for module in SCENARIOS:
            target = ROOT / module / REFERENCE
            if target.exists():
                shutil.move(target, Path(backup) / module)
        try:
            subprocess.run(
                ["bash", "gradlew", *(f":{module}:updateDebugScreenshotTest" for module in SCENARIOS)],
                cwd=ROOT, check=True,
            )
            for module in SCENARIOS:
                rendered = ROOT / module / REFERENCE
                if rendered.exists():
                    shutil.copytree(rendered, candidates / module)
        finally:
            for module in SCENARIOS:
                target = ROOT / module / REFERENCE
                if target.exists():
                    shutil.rmtree(target)
                saved = Path(backup) / module
                if saved.exists():
                    shutil.move(saved, target)
    if inputs != capture_input():
        raise ValueError("Render inputs changed during capture.")
    images = inventory(candidates)
    current = {
        "ui_common_dependency": ui_common_dependency(),
        "ui_common_source": ui_common_source(),
        "source": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
        "input_sha256": inputs,
        "dirty": bool(subprocess.check_output(["git", "status", "--short", "--", *INPUTS], cwd=ROOT)),
        "images": images,
    }
    write_json(OUTPUT / "candidates.json", current)
    gallery(approved["images"] if approved else {}, images)
    if approved is None:
        classification = "Initial candidates. No approved baselines."
    elif images != approved["images"]:
        classification = "Image changes require review and explicit acceptance."
    elif inputs != approved["input_sha256"]:
        classification = "Images are byte-identical. Review the provenance-only manifest update."
        write_json(OUTPUT / "provenance-only-baselines.json", {**approved, **current})
    else:
        classification = "Images and render inputs are identical."
    print(f"{len(images)} screenshots. {classification}")


def accept(reviewer):
    manifest = json.loads((OUTPUT / "candidates.json").read_text())
    candidates = OUTPUT / "candidates"
    if manifest["input_sha256"] != capture_input() or manifest["images"] != inventory(candidates):
        raise ValueError("Candidate images or render inputs changed. Capture again.")
    for module in SCENARIOS:
        target = ROOT / module / REFERENCE
        if target.exists():
            shutil.rmtree(target)
        shutil.copytree(candidates / module, target)
    write_json(MANIFEST, {**manifest, "reviewer": reviewer})


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--accept", metavar="REVIEWER", help="Accept candidates after visually reviewing the gallery.")
    args = parser.parse_args()
    if args.accept:
        accept(args.accept)
    else:
        capture()
