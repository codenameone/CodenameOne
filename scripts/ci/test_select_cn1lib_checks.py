#!/usr/bin/env python3
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[2]

def load(name, path):
    spec = importlib.util.spec_from_file_location(name, ROOT / path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module

selector = load('selector', 'scripts/ci/select-cn1lib-checks.py')
probe = load('probe', 'scripts/ci/check-ios-cn1lib.py')
coverage = load('coverage', 'scripts/check-cn1lib-native-coverage.py')

class SelectionTest(unittest.TestCase):
    def test_backend_and_docs_do_not_build_libraries(self):
        result = selector.select(['vm/backend/src/Foo.java', 'docs/example.md'])
        self.assertTrue(all(value == '' for key, value in result.items() if key != 'admob_link'))
        self.assertEqual('false', result['admob_link'])

    def test_empty_diff_skips_but_unknown_diff_runs_all(self):
        self.assertEqual('', selector.select([])['libs'])
        self.assertEqual(set(selector.PACKAGES), set(selector.select(None)['libs'].split(',')))
        self.assertEqual('true', selector.select(None)['admob_link'])

    def test_admob_change_does_not_compile_other_providers(self):
        result = selector.select(['maven/cn1-admob/ios/src/main/objectivec/Bridge.m'])
        self.assertEqual('cn1-admob', result['ios_ads'])
        self.assertEqual('true', result['admob_link'])
        self.assertEqual('', result['android'])
        self.assertEqual('cn1-admob/common', result['modules'])

    def test_translator_java_only_runs_link_probe(self):
        result = selector.select(['vm/ByteCodeTranslator/src/com/codename1/tools/ByteCodeMethod.java'])
        self.assertEqual('', result['ios_ads'])
        self.assertEqual('true', result['admob_link'])
        self.assertEqual('', result['desktop'])

    def test_runtime_header_checks_all_ad_bridges(self):
        result = selector.select(['vm/ByteCodeTranslator/src/cn1_globals.h'])
        self.assertEqual(set(selector.ADS), set(result['ios_ads'].split(',')))
        self.assertNotEqual('', result['desktop'])

    def test_android_api_change_keeps_coverage(self):
        result = selector.select(['CodenameOne/src/com/codename1/ui/Component.java'])
        self.assertIn('cn1-admob', result['android'])
        self.assertEqual('', result['ios_ads'])
        self.assertEqual('', result['libs'])

    def test_generator_checks_ai_not_ads(self):
        result = selector.select(['scripts/gen-ai-cn1libs.py'])
        self.assertEqual(set(selector.AI), set(result['ios_ai'].split(',')))
        self.assertEqual('', result['ios_ads'])

    def test_packaging_plugin_keeps_common_tests(self):
        result = selector.select(['maven/codenameone-maven-plugin/src/main/java/com/codename1/maven/Cn1libMojo.java'])
        self.assertEqual(set(selector.PACKAGES), set(result['libs'].split(',')))

    def test_checker_changes_run_corresponding_checks(self):
        for file, key in [('scripts/check-cn1lib-native-sources.py', 'desktop'),
                          ('scripts/check-cn1lib-android-api.py', 'android')]:
            self.assertNotEqual('', selector.select([file])[key])

    def test_no_base_and_bad_base_are_conservative(self):
        for base in ['', '0'*40, 'refs/heads/master', '$(echo unsafe)']:
            with patch.dict('os.environ', {'BASE_SHA': base}):
                self.assertIsNone(selector.changed_files())

class ProbeTest(unittest.TestCase):
    def test_arc_still_runs_after_mrr_failure_and_pods_resolved_once(self):
        calls = []
        def run(command, **kwargs):
            calls.append(command)
            class Result:
                returncode = 1 if 'DebugMRR' in command else 0
            return Result()
        with tempfile.TemporaryDirectory() as directory, patch.object(probe.subprocess, 'run', side_effect=run):
            work = Path(directory)
            self.assertEqual(1, probe.check('cn1-admob', work))
            self.assertTrue(list((work / 'Sources').glob('*.m')))
            self.assertIn('DebugARC', (work / 'project.yml').read_text())
            self.assertIn('DebugMRR', (work / 'Podfile').read_text())
        self.assertEqual(1, sum(c[0] == 'pod' for c in calls))
        self.assertEqual(2, sum(c[0] == 'xcodebuild' for c in calls))

    def test_unknown_library_fails(self):
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(ValueError): probe.check('../../bad', Path(directory))

    def test_native_coverage_reads_actual_probe_steps(self):
        covered, untriggered = coverage.covered_libraries()
        self.assertFalse(untriggered)
        self.assertTrue(set(selector.AI + selector.ADS) <= set(covered))
        self.assertFalse(coverage.compiles_cn1lib_natives('run: echo cn1-admob'))

if __name__ == '__main__':
    unittest.main()
