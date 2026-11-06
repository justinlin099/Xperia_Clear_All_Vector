"""Exercise release gates without real signing keys, APK builds, or GitHub access."""

from contextlib import redirect_stdout
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from prepare_release import APPLICATION_ID, is_prerelease, main, prepare


class ReleasePackagingTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.output = self.root / "app/build/outputs/apk/release"
        self.output.mkdir(parents=True)
        self.metadata = {
            "applicationId": APPLICATION_ID,
            "variantName": "release",
            "elements": [{"versionName": "1.1.6-standalone", "versionCode": 8, "outputFile": "app-release.apk"}],
        }
        (self.output / "app-release.apk").write_bytes(b"release-apk-fixture")

    def package(self, ref="refs/tags/v1.1.6-standalone"):
        (self.output / "output-metadata.json").write_text(json.dumps(self.metadata), encoding="utf-8")
        with redirect_stdout(io.StringIO()):
            return prepare(self.root, ref)

    def test_correct_tag_stages_vector_named_apk(self):
        destination = self.package()
        self.assertEqual("Xperia-Clear-All-Vector-1.1.6-standalone.apk", destination.name)
        self.assertEqual(b"release-apk-fixture", destination.read_bytes())

    def test_manual_main_build_does_not_need_a_tag(self):
        self.assertTrue(self.package("refs/heads/main").is_file())

    def test_mismatched_tag_is_rejected_before_staging(self):
        with self.assertRaisesRegex(ValueError, "Tag must be"):
            self.package("refs/tags/v1.1.6")
        self.assertFalse((self.root / "dist").exists())

    def test_non_root_package_is_rejected(self):
        self.metadata["applicationId"] = "com.justinlin099.xperiarecentsrootless"
        with self.assertRaisesRegex(ValueError, "applicationId"):
            self.package()

    def test_debug_variant_is_rejected(self):
        self.metadata["variantName"] = "debug"
        with self.assertRaisesRegex(ValueError, "release variant"):
            self.package()

    def test_multiple_outputs_are_rejected(self):
        self.metadata["elements"] *= 2
        with self.assertRaisesRegex(ValueError, "exactly one"):
            self.package()

    def test_output_cannot_escape_release_directory(self):
        self.metadata["elements"][0]["outputFile"] = "../outside.apk"
        with self.assertRaisesRegex(ValueError, "output path"):
            self.package()

    def test_version_cannot_introduce_an_output_path_or_newline(self):
        for version in ["../../other", "1.1.6\nprerelease=false"]:
            with self.subTest(version=version):
                self.metadata["elements"][0]["versionName"] = version
                with self.assertRaisesRegex(ValueError, "versionName"):
                    self.package()

    def test_unsigned_and_missing_outputs_are_rejected(self):
        self.metadata["elements"][0]["outputFile"] = "app-release-unsigned.apk"
        with self.assertRaisesRegex(ValueError, "unsigned"):
            self.package()
        self.metadata["elements"][0]["outputFile"] = "missing.apk"
        with self.assertRaisesRegex(ValueError, "missing"):
            self.package()

    def test_existing_assets_are_not_mixed_into_a_release(self):
        dist = self.root / "dist"
        dist.mkdir()
        (dist / "older.apk").write_bytes(b"older")
        with self.assertRaisesRegex(ValueError, "dist must be empty"):
            self.package()
        self.assertEqual(["older.apk"], [p.name for p in dist.iterdir()])

    def test_standalone_suffix_is_stable_but_preview_channels_are_not(self):
        for version in ["1.1.6", "1.1.6-standalone"]:
            self.assertFalse(is_prerelease(version))
        for version in ["1.1.7-beta1", "1.1.7-beta1-standalone", "1.1.7-rc1-standalone"]:
            self.assertTrue(is_prerelease(version))

    def test_workflow_receives_release_channel_output(self):
        (self.output / "output-metadata.json").write_text(json.dumps(self.metadata), encoding="utf-8")
        step_output = self.root / "step-output.txt"
        with patch("prepare_release.Path.cwd", return_value=self.root), \
                patch.dict("os.environ", {"GITHUB_REF": "refs/heads/main", "GITHUB_OUTPUT": str(step_output)}), \
                redirect_stdout(io.StringIO()):
            main()
        self.assertEqual("prerelease=false\n", step_output.read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
