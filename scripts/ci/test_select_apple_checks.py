#!/usr/bin/env python3
import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('apple', Path(__file__).with_name('select-apple-checks.py'))
apple = importlib.util.module_from_spec(spec)
spec.loader.exec_module(apple)

class AppleSelectionTest(unittest.TestCase):
    def test_catalyst_only_does_not_expand_to_all_suites(self):
        for event in ['push', 'pull_request']:
            result = apple.select(['scripts/run-mac-catalyst-ui-tests.sh'], event)
            self.assertEqual({'ios':'false', 'native':'false', 'packaging':'false', 'catalyst':'true', 'any':'true'}, result)

    def test_no_native_runner_for_independent_cn1lib_or_core_unit_test(self):
        for event in ['push', 'pull_request']:
            for path in ['maven/cn1-admob/common/pom.xml', 'maven/cn1-ai-whisper/ios/src/main/objectivec/Bridge.m',
                         'maven/core-unittests/src/Test.java', 'docs/guide.md']:
                self.assertEqual('false', apple.select([path], event)['any'], (path, event))

    def test_mock_ad_library_is_a_real_sample_dependency(self):
        self.assertEqual('true', apple.select(['maven/cn1-ads-mock/src/MockAds.java'])['ios'])

    def test_port_builder_changes_test_every_consumer(self):
        for path in ['.github/workflows/_build-ios-port.yml', 'Ports/iOSPort/nativeSources/IOSNative.m']:
            self.assertTrue(all(value == 'true' for value in apple.select([path]).values()))

    def test_unknown_diff_and_selection_changes_run_all(self):
        for paths in [None, ['.github/workflows/scripts-ios.yml'], ['.github/ci/apple-checks.json'], ['scripts/ci/select-cn1lib-checks.py']]:
            self.assertTrue(all(value == 'true' for value in apple.select(paths).values()))
        self.assertEqual('false', apple.select([])['any'])

    def test_workflow_specific_changes_are_selected(self):
        for key, name in [('native', 'scripts-ios-native.yml'), ('packaging', 'ios-packaging.yml'), ('catalyst','scripts-mac-catalyst.yml')]:
            self.assertEqual('true', apple.select(['.github/workflows/' + name])[key])

    def test_globs_match_github_path_semantics(self):
        self.assertTrue(apple.matches('foo.java', '**/*.java'))
        self.assertTrue(apple.matches('a/b/foo.java', '**/*.java'))
        self.assertFalse(apple.matches('a/b/foo.java', '*.java'))
        self.assertTrue(apple.matches('maven/cn1-ai-whisper/ios/Bridge.m', 'maven/cn1-ai-*/**'))
        self.assertFalse(apple.matches('maven/cn1-admob/ios/Bridge.m', 'maven/cn1-ai-*/**'))
        self.assertTrue(apple.affected(['a/b/keep.java'], ['a/**', '!a/b/**', 'a/b/keep.java']))
        with self.assertRaises(ValueError): apple.matches('x', '[xy]')

if __name__ == '__main__': unittest.main()
