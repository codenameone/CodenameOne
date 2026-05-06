// Run _perf-detail N times sequentially (no concurrency) and report
// min/median/max of cn1Started. Each run gets a fresh server +
// browser (different port) so cache state is the same across runs.
import { spawnSync } from 'node:child_process';

const N = Number(process.argv[2] || 5);
const results = [];
for (let i = 0; i < N; i++) {
  const r = spawnSync('node', ['/Users/shai/dev/cn1/scripts/_perf-detail.mjs'], {
    encoding: 'utf8',
  });
  const out = r.stdout || '';
  const m = out.match(/cn1Started:\s+(\d+)\s+ms/);
  if (m) {
    results.push(Number(m[1]));
    process.stdout.write(`${m[1]}  `);
  } else {
    process.stdout.write(`?(${(r.stderr || '').slice(0, 100)})  `);
  }
}
process.stdout.write('\n');
results.sort((a, b) => a - b);
const median = results.length ? results[Math.floor(results.length / 2)] : -1;
console.log(`min=${results[0]} median=${median} max=${results[results.length-1]} N=${results.length}`);
