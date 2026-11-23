# Screenshot review

The executable Compose Preview Screenshot suite captures both modules via the Android Gradle plugin. Previews use synthetic fixtures, the renderer's platform fonts, 393 × 800 dp US English in light and dark, and representative 320 × 800 dp, UK English, 1.5× font-size variants. Capture the same renderer frame for the indeterminate progress indicator.

| Module | UI and variations |
| --- | --- |
| Library | Email entry, sending, code entry and paste control, invalid code, host polling, success and expiry. Email entry and invalid code include the narrow, larger-text variants. A contrasting host theme covers typography, shapes and colors. |
| App | Offline entry, live settings and live code entry. Live settings also includes the narrow, larger-text variant. The screen previews use the production theme, frame and forms with fixture state. |

Run `python3 scripts/screenshots/review.py` from the repository root or select **Run workflow** on **Verify**. This produces a PNG inventory with a source commit and hashes, and a gallery in `build/screenshot-review/index.html`. The workflow uploads PNGs, the gallery, provenance and renderer reports. The script fails on missing scenario variants, zero screenshots, unknown images and changes to approved files. Before approval the images are candidates. Review clipping, empty screens, text size and errors before accepting them.

After reviewing, run `python3 scripts/screenshots/review.py --accept "Your name"` and review the reference-image and manifest diff. Once references exist, CI runs `./gradlew :email-verification:validateDebugScreenshotTest :app:validateDebugScreenshotTest` against them. Changes show before and after PNGs and mark missing sides. Byte-identical images with changed inputs produce a manifest-only update for review. Documentation screenshots can be curated from approved captures with the matching source and image hashes.
