import json
from pathlib import Path
import tempfile
import unittest
import zipfile

from tools.android_bundle import pack


class AndroidBundleTests(unittest.TestCase):
    def test_full_bundle_includes_resources_and_space_metadata(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source, output = root / 'prepared', root / 'bundle'
            (source / 'assets').mkdir(parents=True)
            (source / 'horizon-resources.json').write_text('{}')
            (source / 'facts.json').write_text('{}')
            (source / 'assets/test.txt').write_text('owned fixture')
            pack(source, output)
            metadata = json.loads((output / 'horizon-bundle.json').read_text())
            with zipfile.ZipFile(output / 'horizon-resources.zip') as archive:
                self.assertEqual(archive.read('assets/test.txt'), b'owned fixture')
                self.assertEqual(metadata['unpacked_bytes'], sum(i.file_size for i in archive.infolist()))
            self.assertEqual(metadata['archive_bytes'], (output / 'horizon-resources.zip').stat().st_size)

    def test_incomplete_preparation_is_not_packaged(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / 'horizon-resources.json').write_text('{}')
            with self.assertRaisesRegex(ValueError, 'facts.json'):
                pack(root, root / 'bundle')
            (root / 'facts.json').write_text('{}')
            (root / '.horizon-setup-incomplete').touch()
            with self.assertRaisesRegex(ValueError, 'incomplete'):
                pack(root, root / 'bundle')
