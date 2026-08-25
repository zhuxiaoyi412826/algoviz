#!/usr/bin/env node
/**
 * 给前台所有 HTML 的 </footer> 后面注入 js/site-footer.js 引用。
 *
 * 用法：
 *   node scripts/inject-site-footer.js            一次性扫描注入并退出（CI/构建用）
 *   node scripts/inject-site-footer.js --watch    常驻模式，监听新增/变更的 HTML 自动注入
 *   node scripts/inject-site-footer.js -w         同上
 *
 * 每个 HTML 根据自身相对 qianduan 根目录的层级计算 ../ 的个数。
 * 幂等：页面里已经有 "site-footer.js" 的 <script>，则跳过不重复插入。
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const FOOTER_JS = 'js/site-footer.js';
const FOOTER_RE = /(<\/footer\s*>)/i;
const INJECTED_RE = /<script[^>]+src=["'][^"']*site-footer\.js["']/i;

function walk(dir, list = []) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const e of entries) {
    const full = path.join(dir, e.name);
    if (e.isDirectory()) walk(full, list);
    else if (e.name.endsWith('.html')) list.push(full);
  }
  return list;
}

function relativeJsPath(htmlFile) {
  const rel = path.relative(ROOT, htmlFile).replace(/\\/g, '/');
  const parts = rel.split('/');
  parts.pop();
  let up = '';
  for (let i = 0; i < parts.length; i++) up += '../';
  return up + FOOTER_JS;
}

let changed = 0;
let skipped = 0;
let noFooter = 0;

function injectOne(f, silent = false) {
  let html;
  try {
    html = fs.readFileSync(f, 'utf8');
  } catch (e) {
    if (!silent) console.error(`[read err] ${f}: ${e.message}`);
    return 'readerr';
  }
  if (INJECTED_RE.test(html)) {
    skipped++;
    if (!silent) process.stdout.write(`s ${path.relative(ROOT, f)}\r`);
    return 'skipped';
  }
  const m = html.match(FOOTER_RE);
  if (!m) {
    noFooter++;
    if (!silent) console.warn(`[no footer tag] ${path.relative(ROOT, f)}`);
    return 'nofooter';
  }
  const insertAt = m.index + m[0].length;
  const scriptTag = `\n    <script src="${relativeJsPath(f)}" defer></script>`;
  html = html.slice(0, insertAt) + scriptTag + html.slice(insertAt);
  try {
    fs.writeFileSync(f, html, 'utf8');
  } catch (e) {
    if (!silent) console.error(`[write err] ${f}: ${e.message}`);
    return 'writeerr';
  }
  changed++;
  if (!silent) console.log(`+ injected  ${path.relative(ROOT, f)}`);
  return 'injected';
}

function runOnce() {
  changed = 0; skipped = 0; noFooter = 0;
  const files = walk(ROOT);
  const t0 = Date.now();
  for (const f of files) injectOne(f, true);
  const dt = Date.now() - t0;
  console.log(`[once] done in ${dt}ms. changed=${changed}, skipped=${skipped}, no-footer=${noFooter}, total=${files.length}`);
}

// 防抖毫秒（IDE 保存时可能连续触发两次 write 事件）
const DEBOUNCE_MS = 300;
const pending = new Map();

function scheduleInject(f) {
  if (!f.endsWith('.html')) return;
  const abs = path.resolve(f);
  if (!abs.startsWith(ROOT)) return;
  const key = abs;
  if (pending.has(key)) clearTimeout(pending.get(key));
  const id = setTimeout(() => {
    pending.delete(key);
    const stat = injectOne(abs);
    if (stat === 'injected') console.log(`[watch] auto-injected: ${path.relative(ROOT, abs)}`);
  }, DEBOUNCE_MS);
  pending.set(key, id);
}

function runWatch() {
  runOnce();
  console.log(`[watch] listening for html changes under ${ROOT} ... (Ctrl+C to quit)`);
  fs.watch(ROOT, { recursive: true, persistent: true }, (evt, filename) => {
    if (!filename) return;
    scheduleInject(path.join(ROOT, filename));
  });
}

const args = new Set(process.argv.slice(2));
const wantWatch = args.has('--watch') || args.has('-w');

if (wantWatch) runWatch();
else runOnce();
