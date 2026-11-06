"""Stage one Vector release APK, checking its identity and exact version tag."""

import json
import os
from pathlib import Path
import re
import shutil

APPLICATION_ID = "com.justinlin099.xperiarecentsclearall"


def is_prerelease(version: str) -> bool:
    # 'standalone' identifies the module edition, not a preview channel.
    channel_version = version.removesuffix("-standalone")
    return "-" in channel_version


def prepare(root: Path, ref: str) -> Path:
    output = root / "app/build/outputs/apk/release"
    metadata = json.loads((output / "output-metadata.json").read_text(encoding="utf-8"))
    if metadata.get("applicationId") != APPLICATION_ID:
        raise ValueError("Unexpected applicationId: expected the Vector module")
    if metadata.get("variantName") != "release":
        raise ValueError("Expected the release variant")
    elements = metadata["elements"]
    if len(elements) != 1:
        raise ValueError("Expected exactly one release APK")
    apk = elements[0]
    version = apk["versionName"]
    if not isinstance(version, str) or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", version):
        raise ValueError("versionName must use letters, numbers, dots, underscores or hyphens")
    if ref.startswith("refs/tags/") and ref != f"refs/tags/v{version}":
        raise ValueError(f"Tag must be v{version} to match app/build.gradle.kts versionName")
    source = (output / apk["outputFile"]).resolve()
    if source.parent != output.resolve() or source.suffix != ".apk":
        raise ValueError("Unexpected APK output path")
    if source.name.endswith("-unsigned.apk"):
        raise ValueError("Refusing an unsigned release output; configure release signing first")
    if not source.is_file():
        raise ValueError("Release APK is missing")
    dist = root / "dist"
    dist.mkdir(exist_ok=True)
    if any(dist.iterdir()):
        raise ValueError("dist must be empty before staging a release")
    destination = dist / f"Xperia-Clear-All-Vector-{version}.apk"
    shutil.copyfile(source, destination)
    print(f"Prepared {destination.name} (versionCode {apk['versionCode']})")
    return destination


def main() -> None:
    destination = prepare(Path.cwd(), os.environ.get("GITHUB_REF", ""))
    version = destination.name.removeprefix("Xperia-Clear-All-Vector-").removesuffix(".apk")
    if output := os.environ.get("GITHUB_OUTPUT"):
        with Path(output).open("a", encoding="utf-8") as stream:
            stream.write(f"prerelease={str(is_prerelease(version)).lower()}\n")


if __name__ == "__main__":
    main()
