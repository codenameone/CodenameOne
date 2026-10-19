import copy
from datetime import datetime, timezone, timedelta
import json
from pathlib import Path
import tempfile
import unittest

from linkedin_page_calendar import calendar, conflicts, reserve
from social_campaigns import campaign_link, summarize
from install_linkedin_coordinator_guard import guarded


class CalendarTests(unittest.TestCase):
    def setUp(self):
        self.now = datetime(2026, 10, 19, 23, 5, tzinfo=timezone.utc)
        self.state = {"account": "linkedin:5041007", "slots": [], "native_audit": {
            "observed_at": self.now.isoformat(), "from": "2026-10-18T00:00:00Z",
            "through": "2026-10-23T00:00:00Z", "evidence": "feed and scheduled queue"}}

    def test_catches_future_native_schedule_and_published_post(self):
        self.state['slots'] = [{"id": "outside-producer", "at": "2026-10-20T15:30:00+03:00", "status": "scheduled"}]
        with self.assertRaisesRegex(ValueError, 'collision'):
            reserve(self.state, 'tabs', '2026-10-20T20:30:00+03:00', 'chat approval', self.now)
        self.state['slots'][0]['status'] = 'published'
        self.assertTrue(conflicts(self.state, 'tabs', '2026-10-20T20:30:00+03:00'))

    def test_exact_twenty_hours_and_timezone_offsets(self):
        self.state['slots'] = [{"id": "a", "at": "2026-10-20T02:00:00+03:00", "status": "published"}]
        self.assertFalse(conflicts(self.state, 'b', '2026-10-20T19:00:00Z'))
        self.assertTrue(conflicts(self.state, 'b', '2026-10-20T18:59:59Z'))
        # Fall-back DST offsets are compared as instants, not local wall times.
        self.state['slots'][0]['at'] = '2026-10-24T20:30:00+03:00'
        self.assertFalse(conflicts(self.state, 'b', '2026-10-25T16:30:00+02:00'))

    def test_crashed_reservation_blocks_retry_and_other_producers(self):
        reserve(self.state, 'a', '2026-10-20T20:30:00+03:00', 'chat approval', self.now)
        for item in ['a', 'b']:
            with self.assertRaises(ValueError):
                reserve(self.state, item, '2026-10-20T20:30:00+03:00', 'chat approval', self.now)

    def test_stale_audit_missing_future_coverage_and_naive_time_fail_closed(self):
        with self.assertRaisesRegex(ValueError, 'Fresh'):
            reserve(self.state, 'a', '2026-10-20T20:30:00+03:00', 'approval', self.now+timedelta(minutes=16))
        self.state['native_audit']['through'] = '2026-10-20T22:00:00+03:00'
        with self.assertRaisesRegex(ValueError, 'before AND after'):
            reserve(self.state, 'a', '2026-10-20T20:30:00+03:00', 'approval', self.now)
        with self.assertRaises(ValueError): conflicts(self.state, 'a', '2026-10-20T20:30:00')

    def test_atomic_persistence_and_missing_calendar(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td)/'calendar.json'
            with self.assertRaises(FileNotFoundError):
                with calendar(path): pass
            path.write_text(json.dumps(self.state))
            with calendar(path) as state:
                reserve(state, 'a', '2026-10-20T20:30:00+03:00', 'approval', self.now)
            with self.assertRaises(ValueError):
                with calendar(path) as state:
                    reserve(state, 'b', '2026-10-20T21:30:00+03:00', 'approval', self.now)
            self.assertEqual(['a'],[s['id'] for s in json.loads(path.read_text())['slots']])


class AttributionTests(unittest.TestCase):
    def test_link_preserves_destination_and_has_unique_item(self):
        result=campaign_link('https://www.codenameone.com/demo/?mode=java#code','linkedin-page','social-2026-10','2026-10-20-v2-l1')
        self.assertIn('mode=java&',result)
        self.assertIn('utm_content=2026-10-20-v2-l1',result)
        self.assertTrue(result.endswith('#code'))
        with self.assertRaises(ValueError): campaign_link('https://evil.example/','x','campaign','post')

    def payload(self):
        return {'windows':{'d30':{'days':30,'activated':1,'diagnosticsAvailable':True,'acquisitionAttribution':{
            'eligibleSignups':2,'attributed':1,'coveragePct':50,'byCampaign':[
                {'source':'x','medium':'social','campaign':'social-2026-10','content':'2026-10-20-v2-x1',
                 'signedUp':1,'reachedConsole':1,'attempted':1,'successful':1,'active30d':1,'email':'NEVER EXPORT'},
                {'source':'unattributed','medium':'','campaign':'','content':'','signedUp':1,'reachedConsole':0,'attempted':0,'successful':0,'active30d':0}]}}}}

    def test_aggregate_activation_and_identity_allowlist(self):
        report=summarize(self.payload(),'d30')
        self.assertEqual(1,report['social_attributed_new_users_with_successful_build'])
        self.assertTrue(report['successes_reconcile_with_headline'])
        self.assertNotIn('NEVER EXPORT',json.dumps(report))
        p=self.payload();p['windows']['d30']['activated']=0
        self.assertFalse(summarize(p,'d30')['successes_reconcile_with_headline'])

    def test_missing_diagnostics_is_unknown_not_zero(self):
        for p in [{}, {'windows': {'d30': {'diagnosticsAvailable':False}}}]:
            with self.assertRaises(ValueError):summarize(p,'d30')
        p=self.payload();del p['windows']['d30']['acquisitionAttribution']['byCampaign'][0]['successful']
        with self.assertRaises(ValueError): summarize(p,'d30')


class LegacyHandoffTests(unittest.TestCase):
    def test_guard_blocks_before_any_browser_initialization(self):
        source = '''
DRIVERS = {}
def process_linkedin(driver, task, log):
    """Publish one due LinkedIn post as Shai or the Codename One page."""
    raise AssertionError("browser reached")
def process_task(task, log, profile_dir, chrome_version):
    site = task["site"]
    driver_fn = DRIVERS.get(site)
    raise AssertionError("browser reached")
def main():
    if True:
        pending = [t for t in unfinished if is_due(t, now)]
'''
        code = guarded(source)
        self.assertEqual(code, guarded(code))
        ns={};exec(code,ns)
        self.assertFalse(ns['process_task']({'site':'linkedin'}, None, None, None)[0])
        with self.assertRaisesRegex(RuntimeError, 'coordinator'):
            ns['process_linkedin'](None,{},None)
        with self.assertRaises(AssertionError):
            ns['process_task']({'site':'medium'},None,None,None)


if __name__ == '__main__': unittest.main()
