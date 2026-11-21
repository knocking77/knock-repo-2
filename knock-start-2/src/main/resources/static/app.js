'use strict';

const state = { days: 30, project: '', q: '', starred: false, demo: false };
const charts = {};
const $ = (id) => document.getElementById(id);
const nf = new Intl.NumberFormat('ko-KR');

function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function fmtDate(iso) {
  if (!iso) return '-';
  const d = new Date(iso);
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}
function fmtDur(sec) {
  if (!sec) return '-';
  const h = Math.floor(sec / 3600), m = Math.round((sec % 3600) / 60);
  return h ? `${h}시간 ${m}분` : `${m}분`;
}
function fmtTokens(n) {
  if (n >= 1e6) return (n / 1e6).toFixed(1) + 'M';
  if (n >= 1e3) return (n / 1e3).toFixed(1) + 'K';
  return nf.format(n);
}
function qs(extra = {}) {
  const p = new URLSearchParams();
  const all = { days: state.days, project: state.project, ...extra };
  for (const [k, v] of Object.entries(all)) if (v !== '' && v !== null && v !== undefined && v !== false) p.set(k, v);
  return p.toString();
}
async function api(path, opts) {
  const r = await fetch(path, opts);
  if (!r.ok) throw new Error(`${r.status} ${path}`);
  return r.json();
}

/* ---------- 차트 ---------- */
function css(name) { return getComputedStyle(document.documentElement).getPropertyValue(name).trim(); }

function barChart(id, labels, data, { horizontal = false, valueLabel = '요청' } = {}) {
  const color = css('--series-1'), grid = css('--grid'), ink = css('--text-2'), surface = css('--surface');
  const cfg = {
    type: 'bar',
    data: { labels, datasets: [{ data, backgroundColor: color, hoverBackgroundColor: color, borderRadius: 4,
      borderSkipped: 'start', borderColor: surface, borderWidth: 1, maxBarThickness: 28 }] },
    options: {
      indexAxis: horizontal ? 'y' : 'x',
      maintainAspectRatio: false,
      animation: false,
      plugins: {
        legend: { display: false },
        tooltip: { callbacks: { label: (c) => `${valueLabel} ${nf.format(c.parsed[horizontal ? 'x' : 'y'])}` } },
      },
      scales: {
        x: { grid: { display: horizontal, color: grid }, border: { display: false }, ticks: { color: ink, maxRotation: 0, autoSkipPadding: 12, precision: 0 }, beginAtZero: true },
        y: { grid: { display: !horizontal, color: grid }, border: { display: false }, ticks: { color: ink, precision: 0 }, beginAtZero: true },
      },
    },
  };
  if (charts[id]) charts[id].destroy();
  charts[id] = new Chart($(id), cfg);
}

/* ---------- 렌더링 ---------- */
async function loadStats() {
  const s = await api('/api/stats?' + qs());
  const t = s.totals;
  const tiles = [
    ['세션', nf.format(t.sessions)],
    ['요청', nf.format(t.prompts)],
    ['도구 호출', nf.format(t.toolCalls)],
    ['출력 토큰', fmtTokens(t.outputTokens)],
    ['작업 시간', fmtDur(t.activeSec)],
    ['프로젝트', nf.format(t.projects)],
  ];
  $('tiles').innerHTML = tiles.map(([l, v]) => `<div class="tile"><div class="label">${l}</div><div class="value">${v}</div></div>`).join('');

  barChart('dailyChart', s.daily.map((d) => { const [, m, dd] = d.date.split('-'); return `${+m}/${+dd}`; }), s.daily.map((d) => d.prompts));
  barChart('projectChart', s.byProject.map((d) => d.name), s.byProject.map((d) => d.value), { horizontal: true });
  barChart('toolChart', s.byTool.map((d) => d.name), s.byTool.map((d) => d.value), { horizontal: true, valueLabel: '호출' });
}

async function loadSessions() {
  const list = await api('/api/sessions?' + qs({ q: state.q, starred: state.starred }));
  $('empty').hidden = list.length > 0;
  $('rows').innerHTML = list.map((s) => `
    <tr data-id="${esc(s.id)}">
      <td>${s.starred ? '<span class="star" aria-label="즐겨찾기">★</span>' : ''}</td>
      <td>${fmtDate(s.startedAt)}</td>
      <td class="title">${esc(s.title)}${s.hasMemo ? '<span class="memo-dot">메모</span>' : ''}</td>
      <td>${esc(s.projectName)}</td>
      <td class="num">${s.promptCount}</td>
      <td class="num">${s.toolCallCount}</td>
      <td class="num">${fmtDur(s.durationSec)}</td>
      <td>${(s.tags || '').split(',').filter(Boolean).map((t) => `<span class="tag">${esc(t)}</span>`).join('')}</td>
    </tr>`).join('');
}

async function loadProjects() {
  const ps = await api('/api/projects');
  const sel = $('projectSel');
  sel.innerHTML = '<option value="">전체 프로젝트</option>' + ps.map((p) => `<option ${p === state.project ? 'selected' : ''}>${esc(p)}</option>`).join('');
}

async function loadCollectInfo(r) {
  if (state.demo) { $('collectInfo').textContent = '예시 데이터로 보여주는 미리보기 화면입니다'; return; }
  r = r || await fetch('/api/collect/last').then((x) => x.ok ? x.text() : '').then((t) => t ? JSON.parse(t) : null);
  $('collectInfo').textContent = r
    ? `마지막 수집 ${new Date(r.finishedAt).toLocaleString('ko-KR')} · 파일 ${r.scanned}개 · 반영 ${r.imported}개${r.errors.length ? ` · 오류 ${r.errors.length}개` : ''}`
    : 'Claude Code 대화 기록을 자동으로 수집합니다 (5분 간격)';
}

async function refreshAll() {
  await Promise.all([loadStats(), loadSessions(), loadProjects(), loadCollectInfo()]);
}

/* ---------- 상세 ---------- */
let currentId = null;
async function openDetail(id) {
  const d = await api('/api/sessions/' + encodeURIComponent(id));
  const s = d.summary;
  currentId = id;
  $('dTitle').textContent = s.title;
  $('dMeta').textContent = [s.projectName, s.gitBranch, s.model, `${fmtDate(s.startedAt)} ~ ${fmtDate(s.endedAt)}`,
    `요청 ${s.promptCount} · 도구 ${s.toolCallCount} · 출력 토큰 ${fmtTokens(s.outputTokens)}`].filter(Boolean).join(' · ');
  $('dStar').checked = s.starred;
  $('dTags').value = s.tags || '';
  $('dMemo').value = d.memo || '';
  $('dPrompts').innerHTML = d.prompts.map((p) => `<li><time>${fmtDate(p.timestamp)}</time>${esc(p.text)}</li>`).join('') || '<li>요청 없음</li>';
  const tools = Object.entries(d.toolUsage).sort((a, b) => b[1] - a[1]);
  $('dTools').innerHTML = tools.map(([k, v]) => `<span class="tag">${esc(k)} ${v}</span>`).join('') || '없음';
  $('dFiles').innerHTML = d.filesTouched.map((f) => `<li>${esc(f)}</li>`).join('') || '<li>없음</li>';
  $('detail').showModal();
}

async function saveDetail() {
  await api('/api/sessions/' + encodeURIComponent(currentId), {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ memo: $('dMemo').value, tags: $('dTags').value, starred: $('dStar').checked }),
  });
  $('detail').close();
  loadSessions();
}

/* ---------- 이벤트 ---------- */
document.querySelectorAll('.seg button').forEach((b) => b.addEventListener('click', () => {
  document.querySelectorAll('.seg button').forEach((x) => x.classList.toggle('on', x === b));
  state.days = +b.dataset.days;
  loadStats(); loadSessions();
}));
$('projectSel').addEventListener('change', (e) => { state.project = e.target.value; loadStats(); loadSessions(); });
let qTimer;
$('q').addEventListener('input', (e) => { clearTimeout(qTimer); qTimer = setTimeout(() => { state.q = e.target.value; loadSessions(); }, 250); });
$('starredOnly').addEventListener('change', (e) => { state.starred = e.target.checked; loadSessions(); });
$('rows').addEventListener('click', (e) => { const tr = e.target.closest('tr[data-id]'); if (tr) openDetail(tr.dataset.id); });
$('dSave').addEventListener('click', saveDetail);
$('collectBtn').addEventListener('click', async () => {
  const b = $('collectBtn');
  b.disabled = true; b.textContent = '수집 중…';
  try {
    const r = await api('/api/collect', { method: 'POST' });
    await refreshAll();
    loadCollectInfo(r);
  } finally { b.disabled = false; b.textContent = '지금 수집'; }
});
matchMedia('(prefers-color-scheme: dark)').addEventListener('change', loadStats);

api('/api/info').then((i) => {
  $('badge').textContent = 'v' + i.version + (i.demo ? ' · 데모' : '');
  $('demoNote').hidden = !i.demo;
  if (i.demo) { state.demo = true; $('collectBtn').hidden = true; loadCollectInfo(); }
}).catch(() => {});

refreshAll();
setInterval(refreshAll, 60_000);
