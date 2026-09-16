'use strict';

/* ==========================================================================
   研途倒计时 · 逻辑
   - 以"日历天"计算距离考试还有多少天
   - 跨零点自动刷新，显示秒数时每秒刷新
   - 屏幕常亮使用 Wake Lock API
   - 设置写入 localStorage
   ========================================================================== */

const STORAGE_KEY = 'exam-countdown:v1';

const DEFAULT_SETTINGS = {
  examDate: '2027-12-25',
  alwaysOn: true,
  showSeconds: false,
  size: 'medium',
  focusMode: false,
};

const SIZE_LABELS = {
  small: '小号 · 2 × 2',
  medium: '中号 · 4 × 2',
  large: '大号 · 4 × 4',
};

const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

const els = {
  examDate: document.querySelector('#examDate'),
  daysDisplay: document.querySelector('#daysDisplay'),
  dateChip: document.querySelector('#dateChip'),
  footerDate: document.querySelector('#footerDate'),
  todayLabel: document.querySelector('#todayLabel'),
  sizeLabel: document.querySelector('#sizeLabel'),
  sign: document.querySelector('#countdownSign'),
  precisionRow: document.querySelector('#precisionRow'),
  precisionValue: document.querySelector('#precisionValue'),
  statusText: document.querySelector('#statusText'),
  weeksLeft: document.querySelector('#weeksLeft'),
  examWeekday: document.querySelector('#examWeekday'),
  alwaysOn: document.querySelector('#alwaysOn'),
  alwaysOnHint: document.querySelector('#alwaysOnHint'),
  showSeconds: document.querySelector('#showSeconds'),
  powerLight: document.querySelector('#powerLight'),
  focusToggle: document.querySelector('#focusToggle'),
};

let settings = { ...DEFAULT_SETTINGS };
let midnightTimer = null;
let tickTimer = null;
let wakeLock = null;

/* ---------- 设置读写 ---------- */

function loadSettings() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) settings = { ...DEFAULT_SETTINGS, ...JSON.parse(raw) };
  } catch (error) {
    console.warn('读取设置失败，使用默认值', error);
  }
}

function saveSettings() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
  } catch (error) {
    console.warn('保存设置失败', error);
  }
}

/* ---------- 日期工具 ---------- */

function pad(value, length = 2) {
  return String(value).padStart(length, '0');
}

// 用"年/月/日"三个数字构造，避免 new Date('2027-12-25') 被当成 UTC 解析
function parseDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || '');
  if (!match) return null;
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  return Number.isNaN(date.getTime()) ? null : date;
}

function startOfDay(date) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

// 把本地日期转成"天序号"，用 UTC 差值避免夏令时导致的 23/25 小时误差
function dayNumber(date) {
  return Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / 86400000;
}

function calendarDaysBetween(from, to) {
  return Math.round(dayNumber(to) - dayNumber(from));
}

function formatDot(date) {
  return `${date.getFullYear()}.${pad(date.getMonth() + 1)}.${pad(date.getDate())}`;
}

function formatSlash(date) {
  return `${date.getFullYear()} / ${pad(date.getMonth() + 1)} / ${pad(date.getDate())}`;
}

function getTargetDate() {
  return parseDate(els.examDate.value) || parseDate(DEFAULT_SETTINGS.examDate);
}

/* ---------- 渲染 ---------- */

function render() {
  const target = getTargetDate();
  const now = new Date();
  const rawDays = calendarDaysBetween(startOfDay(now), target);
  const daysLeft = Math.max(0, rawDays);

  els.daysDisplay.textContent = pad(daysLeft, Math.max(3, String(daysLeft).length));
  els.dateChip.textContent = formatDot(target);
  els.footerDate.textContent = formatSlash(target);
  els.todayLabel.textContent = `今天 · ${formatDot(now)}`;

  const weeks = Math.floor(daysLeft / 7);
  const restDays = daysLeft % 7;
  els.weeksLeft.textContent = daysLeft > 0 ? `${weeks} 周 ${restDays} 天` : '—';
  els.examWeekday.textContent = WEEKDAYS[target.getDay()];

  const state = rawDays === 0 ? 'today' : rawDays > 0 ? 'counting' : 'past';
  els.sign.dataset.state = state;
  els.statusText.textContent =
    state === 'today' ? '就是今天，全力以赴' : state === 'past' ? '考试已结束' : 'KEEP GOING';

  renderPrecision();
}

// "今日剩余"：距离下一个零点的时分秒，走到 00:00:00 时主数字自动减一
function renderPrecision() {
  if (!settings.showSeconds) return;
  const now = new Date();
  const nextMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
  let remain = Math.max(0, nextMidnight - now);
  const hours = Math.floor(remain / 3600000);
  remain -= hours * 3600000;
  const minutes = Math.floor(remain / 60000);
  remain -= minutes * 60000;
  const seconds = Math.floor(remain / 1000);
  els.precisionValue.textContent = `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
}

/* ---------- 定时刷新 ---------- */

function scheduleMidnightRefresh() {
  clearTimeout(midnightTimer);
  const now = new Date();
  const nextMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1, 0, 0, 1);
  midnightTimer = setTimeout(() => {
    render();
    scheduleMidnightRefresh();
  }, nextMidnight - now);
}

function syncTicker() {
  clearInterval(tickTimer);
  els.precisionRow.hidden = !settings.showSeconds;
  if (!settings.showSeconds) return;
  renderPrecision();
  tickTimer = setInterval(renderPrecision, 1000);
}

/* ---------- 屏幕常亮 ---------- */

async function syncWakeLock() {
  if (!('wakeLock' in navigator)) {
    els.alwaysOnHint.textContent = '当前浏览器不支持常亮';
    return;
  }
  try {
    if (settings.alwaysOn && document.visibilityState === 'visible') {
      if (!wakeLock) wakeLock = await navigator.wakeLock.request('screen');
    } else if (wakeLock) {
      await wakeLock.release();
      wakeLock = null;
    }
  } catch (error) {
    console.warn('屏幕常亮不可用', error);
    els.alwaysOnHint.textContent = '常亮被系统拒绝';
  }
  els.powerLight.classList.toggle('is-off', !settings.alwaysOn);
}

/* ---------- 界面状态 ---------- */

function applySize(size) {
  els.sign.dataset.size = size;
  els.sign.classList.remove('sign--small', 'sign--medium', 'sign--large');
  els.sign.classList.add(`sign--${size}`);
  els.sizeLabel.textContent = SIZE_LABELS[size];
  document.querySelectorAll('.size-option').forEach((option) => {
    option.classList.toggle('active', option.dataset.size === size);
  });
}

function applySwitch(button, active) {
  button.classList.toggle('is-on', active);
  button.setAttribute('aria-checked', String(active));
}

async function setFocusMode(on) {
  document.body.classList.toggle('focus-mode', on);
  els.focusToggle.textContent = on ? '退出专注' : '专注模式';
  settings.focusMode = on;
  saveSettings();
  try {
    if (on && document.documentElement.requestFullscreen) {
      await document.documentElement.requestFullscreen();
    } else if (!on && document.fullscreenElement) {
      await document.exitFullscreen();
    }
  } catch (error) {
    // 全屏需要用户手势，失败时仅保留专注模式样式
  }
}

/* ---------- 事件绑定 ---------- */

function bindEvents() {
  els.examDate.addEventListener('change', () => {
    const parsed = parseDate(els.examDate.value);
    if (!parsed) {
      els.examDate.value = settings.examDate;
      return;
    }
    settings.examDate = els.examDate.value;
    saveSettings();
    render();
  });

  document.querySelectorAll('[data-date]').forEach((button) => {
    button.addEventListener('click', () => {
      els.examDate.value = button.dataset.date;
      settings.examDate = button.dataset.date;
      saveSettings();
      render();
    });
  });

  els.alwaysOn.addEventListener('click', () => {
    settings.alwaysOn = !settings.alwaysOn;
    applySwitch(els.alwaysOn, settings.alwaysOn);
    saveSettings();
    syncWakeLock();
  });

  els.showSeconds.addEventListener('click', () => {
    settings.showSeconds = !settings.showSeconds;
    applySwitch(els.showSeconds, settings.showSeconds);
    saveSettings();
    syncTicker();
  });

  document.querySelectorAll('.size-option').forEach((option) => {
    option.addEventListener('click', () => {
      settings.size = option.dataset.size;
      applySize(settings.size);
      saveSettings();
    });
  });

  els.focusToggle.addEventListener('click', () => {
    setFocusMode(!document.body.classList.contains('focus-mode'));
  });

  document.addEventListener('fullscreenchange', () => {
    if (!document.fullscreenElement && document.body.classList.contains('focus-mode')) {
      setFocusMode(false);
    }
  });

  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      render();
      scheduleMidnightRefresh();
      syncWakeLock();
    }
  });
}

/* ---------- Service Worker ---------- */

function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return;
  if (location.protocol !== 'http:' && location.protocol !== 'https:') return;
  navigator.serviceWorker.register('sw.js').catch((error) => {
    console.warn('Service Worker 注册失败', error);
  });
}

/* ---------- 启动 ---------- */

function init() {
  loadSettings();

  els.examDate.value = settings.examDate;
  applySwitch(els.alwaysOn, settings.alwaysOn);
  applySwitch(els.showSeconds, settings.showSeconds);
  applySize(settings.size);

  if (settings.focusMode) {
    document.body.classList.add('focus-mode');
    els.focusToggle.textContent = '退出专注';
  }

  bindEvents();
  render();
  syncTicker();
  scheduleMidnightRefresh();
  syncWakeLock();
  registerServiceWorker();
}

init();