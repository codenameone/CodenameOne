import fs from 'node:fs';
import path from 'node:path';

let chromium;
try {
  ({ chromium } = await import('playwright'));
} catch (playwrightError) {
  try {
    ({ chromium } = await import('@playwright/test'));
  } catch (playwrightTestError) {
    console.error('Unable to load Playwright. Install either "playwright" or "@playwright/test".');
    console.error('Import from "playwright" failed:', String(playwrightError));
    console.error('Import from "@playwright/test" failed:', String(playwrightTestError));
    process.exit(2);
  }
}

const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..');
const bridgePath = path.join(repoRoot, 'vm', 'ByteCodeTranslator', 'src', 'javascript', 'browser_bridge.js');
const outputDir = path.resolve(process.argv[2] || process.env.ARTIFACTS_DIR || '/tmp/javascript-stage-harness');
fs.mkdirSync(outputDir, { recursive: true });

const browserBridgeSource = fs.readFileSync(bridgePath, 'utf8');

function writePng(name, dataUrl) {
  const comma = String(dataUrl || '').indexOf(',');
  if (comma < 0) {
    throw new Error(`Missing PNG payload for ${name}`);
  }
  const payload = dataUrl.substring(comma + 1);
  fs.writeFileSync(path.join(outputDir, `${name}.png`), Buffer.from(payload, 'base64'));
}

const browser = await chromium.launch({
  headless: true,
  args: [
    '--autoplay-policy=no-user-gesture-required',
    '--disable-web-security',
    '--allow-file-access-from-files'
  ]
});

try {
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
  await page.setContent(`
    <!doctype html>
    <html>
      <head>
        <meta charset="utf-8" />
        <style>
          html, body { margin: 0; padding: 0; background: #d9d9d9; }
          canvas { display: block; width: 1280px; height: 900px; }
        </style>
      </head>
      <body>
        <canvas id="stage" width="1280" height="900"></canvas>
      </body>
    </html>
  `);
  await page.addScriptTag({ content: browserBridgeSource });

  async function runStage(name, mode) {
    const meta = await page.evaluate(async (stageMode) => {
      const canvas = document.getElementById('stage');
      const ctx = canvas.getContext('2d');

      function nextColor(index) {
        const colors = ['#ff0000', '#00aa00', '#0000ff', '#333333'];
        return colors[index % colors.length];
      }

      function drawPortApproxArc(ctx, x, y, w, h, startAngle, sweepAngle) {
        const cx = x + w / 2;
        const cy = y + h / 2;
        const a = w / 2;
        const b = h / 2;

        function getPointAtAngle(theta) {
          const tanTheta = Math.tan(theta);
          const tanThetaSq = tanTheta * tanTheta;
          const bs = b * b;
          const as = a * a;
          let px = a * b / Math.sqrt(bs + as * tanThetaSq);
          if (Math.cos(theta) < 0) {
            px = -px;
          }
          let py = a * b / Math.sqrt(as + bs / tanThetaSq);
          if (Math.sin(theta) < 0) {
            py = -py;
          }
          return { x: px + cx, y: py + cy };
        }

        function calculateBezierControlPoint(start, sweep) {
          const p1 = getPointAtAngle(start);
          const p2 = getPointAtAngle(start + sweep);
          const rp1x = p1.x - cx;
          const rp1y = p1.y - cy;
          const rp2x = p2.x - cx;
          const rp2y = p2.y - cy;
          const x1s = rp1x * rp1x;
          const y1s = rp1y * rp1y;
          const x2s = rp2x * rp2x;
          const y2s = rp2y * rp2y;
          const as = a * a;
          const bs = b * b;
          return {
            x: (-(rp1y * (-as * y2s - bs * x2s) + as * y1s * rp2y + bs * x1s * rp2y) / (bs * rp2x * rp1y - bs * rp1x * rp2y)) + cx,
            y: ((rp1x * (-as * y2s - bs * x2s) + as * rp2x * y1s + bs * x1s * rp2x) / (as * rp2x * rp1y - as * rp1x * rp2y)) + cy
          };
        }

        function addArcSegment(start, sweep) {
          const absSweep = Math.abs(sweep);
          if (absSweep < 0.0001) {
            return;
          }
          if (absSweep > Math.PI / 4) {
            const diff = sweep < 0 ? -Math.PI / 4 : Math.PI / 4;
            addArcSegment(start, diff);
            addArcSegment(start + diff, sweep - diff);
            return;
          }
          const end = getPointAtAngle(start + sweep);
          const control = calculateBezierControlPoint(start, sweep);
          ctx.quadraticCurveTo(control.x, control.y, end.x, end.y);
        }

        const start = -startAngle;
        const sweep = -sweepAngle;
        const startPoint = getPointAtAngle(start);
        ctx.moveTo(startPoint.x, startPoint.y);
        addArcSegment(start, sweep);
      }

      function drawCellArcs(ctx, x, y, w, h, usePortApprox) {
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(x, y, w, h);
        for (let iter = 0; iter < Math.floor(w / 2); iter++) {
          ctx.strokeStyle = nextColor(iter);
          ctx.lineWidth = 1;
          ctx.beginPath();
          if (usePortApprox) {
            drawPortApproxArc(ctx, x + iter, y + iter, w - iter * 2, h - iter * 2, iter * Math.PI / 180, Math.PI);
          } else {
            const arcX = x + iter;
            const arcY = y + iter;
            const arcW = w - iter * 2;
            const arcH = h - iter * 2;
            if (arcW <= 0 || arcH <= 0) {
              break;
            }
            ctx.ellipse(arcX + arcW / 2, arcY + arcH / 2, arcW / 2, arcH / 2, 0, -iter * Math.PI / 180, -(iter * Math.PI / 180 + Math.PI), true);
          }
          ctx.stroke();
        }
      }

      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.fillStyle = '#d9d9d9';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      drawCellArcs(ctx, 40, 40, 560, 360, stageMode === 'port-arc');
      drawCellArcs(ctx, 680, 40, 560, 360, stageMode === 'port-arc');
      drawCellArcs(ctx, 40, 500, 560, 320, stageMode === 'port-arc');
      drawCellArcs(ctx, 680, 500, 560, 320, stageMode === 'port-arc');

      window.__cn1LastScreenshotSignature = '';
      return await window.cn1HostBridge.handlers.__cn1_capture_canvas_png__({ includeMeta: true });
    }, mode);
    if (!meta || !meta.dataUrl) {
      throw new Error(`Stage ${name} did not produce a PNG`);
    }
    writePng(name, meta.dataUrl);
    return {
      stage: name,
      canvasScore: meta.canvasScore,
      canvasLastPaintSeq: meta.canvasLastPaintSeq,
      canvasPaintedSinceStart: meta.canvasPaintedSinceStart,
      canvasSignature: meta.canvasSignature
    };
  }

  const results = [];
  results.push(await runStage('pure-js-canvas-arc', 'direct-arc'));
  results.push(await runStage('pure-js-port-approx-arc', 'port-arc'));

  fs.writeFileSync(path.join(outputDir, 'summary.json'), JSON.stringify(results, null, 2));
  console.log(JSON.stringify({ outputDir, results }, null, 2));
} finally {
  await browser.close();
}
