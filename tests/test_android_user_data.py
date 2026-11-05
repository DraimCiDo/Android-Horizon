"""Exercise the APK's real archive implementation on a JVM, without Android stubs."""
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


@unittest.skipUnless(shutil.which('javac') and shutil.which('java'), 'JDK is not installed')
class AndroidUserDataTests(unittest.TestCase):
    def test_archive_roundtrip_and_restore_isolation(self):
        with tempfile.TemporaryDirectory() as output:
            subprocess.run(['javac', '--release', '8', '-d', output,
                            str(ROOT / 'packaging/android/java/dev/horizonlab/horizon/UserDataArchive.java'),
                            str(ROOT / 'tests/java/UserDataArchiveTest.java')], check=True, capture_output=True)
            subprocess.run(['java', '-ea', '-cp', output, 'dev.horizonlab.horizon.UserDataArchiveTest'],
                           check=True, capture_output=True, timeout=30)
