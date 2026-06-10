#!/usr/bin/env python3
import json
import os
from pathlib import Path


def required_env(name: str) -> str:
    value = os.environ.get(name, "")
    if not value:
        raise SystemExit(f"{name} is required")
    return value


def main():
    source_manifest = Path(os.environ.get("SOURCE_MANIFEST", "update/manifest.json"))
    output_dir = Path(required_env("OUTPUT_DIR"))
    release_url = required_env("RELEASE_URL")
    asset_url = required_env("ASSET_URL")
    apk_sha256 = required_env("APK_SHA256")
    apk_size = int(required_env("APK_SIZE"))

    manifest = json.loads(source_manifest.read_text(encoding="utf-8"))
    manifest["isReady"] = True
    manifest["releaseUrl"] = release_url
    manifest["assets"] = [
        {
            "platform": "android",
            "type": "universal-apk",
            "url": asset_url,
            "sha256": apk_sha256,
            "size": apk_size,
        }
    ]

    output_dir.mkdir(parents=True, exist_ok=True)
    text = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
    (output_dir / "manifest.json").write_text(text, encoding="utf-8")
    (output_dir / "stable.json").write_text(text, encoding="utf-8")


if __name__ == "__main__":
    main()
