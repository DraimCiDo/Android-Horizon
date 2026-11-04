import unittest

from tools.android_auth import configuration


class AndroidAuthConfigurationTests(unittest.TestCase):
    def test_owner_config_is_normalized_for_embedding(self):
        self.assertEqual(configuration('  11111111-2222-4333-8444-AAAAAAAAAAAA  '),
                         {'microsoft_client_id': '11111111-2222-4333-8444-aaaaaaaaaaaa'})

    def test_offline_build_has_no_invented_client_id(self):
        self.assertEqual(configuration(''), {'microsoft_client_id': ''})

    def test_invalid_ids_do_not_reach_an_apk(self):
        for value in ('not-a-client-id', '00000000-0000-0000-0000-000000000000'):
            with self.subTest(value=value), self.assertRaises(ValueError):
                configuration(value)
