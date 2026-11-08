// Browser client: sign in, recommended rooms and booking, own bookings, occupancy for ADMIN.
// Material Web components are imported one by one, as the library recommends.
import '@material/web/button/filled-button.js';
import '@material/web/button/filled-tonal-button.js';
import '@material/web/button/outlined-button.js';
import '@material/web/button/text-button.js';
import '@material/web/iconbutton/icon-button.js';
import '@material/web/icon/icon.js';
import '@material/web/textfield/outlined-text-field.js';
import '@material/web/select/outlined-select.js';
import '@material/web/select/select-option.js';
import '@material/web/dialog/dialog.js';
import '@material/web/progress/linear-progress.js';
import '@material/web/tabs/tabs.js';
import '@material/web/tabs/primary-tab.js';
import '@material/web/ripple/ripple.js';
import { styles as typescaleStyles } from '@material/web/typography/md-typescale-styles.js';
import { applyTheme } from './theme.js';
import { api, ApiError, session } from './api.js';

document.adoptedStyleSheets.push(typescaleStyles.styleSheet);
applyTheme();

const $ = (selector) => document.querySelector(selector);
const PAGE_SIZE = 10;
const STATUS = {
  CONFIRMED: ['Подтверждено', 'tag--confirmed'],
  PENDING: ['Ожидает', 'tag--pending'],
  CANCELLED: ['Отменено', 'tag--cancelled'],
};
const TITLES = { rooms: 'Номера', bookings: 'Мои брони', occupancy: 'Загрузка номеров' };

const state = { hotels: new Map(), rooms: new Map(), bookingsPage: 0, search: null };

// ---------- helpers ----------

const escapeHtml = (value) =>
  String(value ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

const isoDate = (daysFromToday) => {
  const date = new Date();
  date.setDate(date.getDate() + daysFromToday);
  return date.toISOString().slice(0, 10);
};

const formatDate = (iso) => new Date(`${iso}T00:00:00`).toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' });

const field = (form, name) => form.querySelector(`[name="${name}"]`).value;

let snackbarTimer;
function notify(message) {
  const snackbar = $('#snackbar');
  snackbar.textContent = message;
  snackbar.hidden = false;
  clearTimeout(snackbarTimer);
  snackbarTimer = setTimeout(() => (snackbar.hidden = true), 5000);
}

/** Runs a call with the progress bar on; errors end up in the snackbar with the trace id for support. */
async function busy(action) {
  $('#busy').hidden = false;
  try {
    return await action();
  } catch (error) {
    if (error instanceof ApiError && error.status === 401 && session.token) {
      signOut();
      notify('Сессия истекла, войдите снова');
    } else {
      const trace = error instanceof ApiError && error.traceId ? ` (trace ${error.traceId.slice(0, 8)})` : '';
      notify(`${error.message}${trace}`);
    }
    return undefined;
  } finally {
    $('#busy').hidden = true;
  }
}

async function roomOf(id) {
  if (!state.rooms.has(id)) state.rooms.set(id, await api.room(id));
  return state.rooms.get(id);
}

const hotelName = (hotelId) => state.hotels.get(hotelId)?.name ?? `Отель ${hotelId}`;

// ---------- auth ----------

let registering = false;

$('#auth-tabs').addEventListener('change', (event) => {
  registering = event.target.activeTabIndex === 1;
  $('#auth-submit').textContent = registering ? 'Зарегистрироваться' : 'Войти';
});

$('#auth-form').addEventListener('submit', (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const credentials = { username: field(form, 'username'), password: field(form, 'password') };
  signIn(() => (registering ? api.register(credentials) : api.login(credentials)));
});

/** Demo accounts created by Booking Service when demo data is on (DEMO_DATA, see README). */
const DEMO_PASSWORD = 'demo-password';

document.querySelectorAll('[data-demo]').forEach((button) =>
  button.addEventListener('click', () => signIn(() => api.login({ username: button.dataset.demo, password: DEMO_PASSWORD }))),
);

async function signIn(call) {
  const token = await busy(call);
  if (token) {
    session.token = token.accessToken;
    await start();
  }
}

$('#logout').addEventListener('click', signOut);

function signOut() {
  session.token = null;
  $('#shell').hidden = true;
  $('#auth').hidden = false;
}

// ---------- navigation ----------

function show(view) {
  document.querySelectorAll('.nav__item').forEach((item) => {
    if (item.dataset.view === view) item.setAttribute('aria-current', 'page');
    else item.removeAttribute('aria-current');
  });
  document.querySelectorAll('main.view').forEach((section) => (section.hidden = section.dataset.view !== view));
  $('#view-title').textContent = TITLES[view];
  if (view === 'bookings') loadBookings(true);
}

document.querySelectorAll('.nav__item').forEach((item) => item.addEventListener('click', () => show(item.dataset.view)));

// ---------- rooms ----------

$('#search-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  state.search = {
    startDate: field(form, 'startDate'),
    endDate: field(form, 'endDate'),
    hotelId: $('#hotel-select').value,
  };
  const rooms = await busy(() => api.recommendations(state.search));
  if (rooms) renderRooms(rooms);
});

function renderRooms(rooms) {
  rooms.forEach((room) => state.rooms.set(room.id, room));
  $('#rooms-empty').hidden = rooms.length > 0;
  $('#rooms-empty').textContent = 'На эти даты свободных номеров нет.';
  $('#auto-select').hidden = rooms.length === 0;
  $('#rooms').innerHTML = rooms
    .map(
      (room, index) => `
      <article class="card ${index === 0 ? 'card--recommended' : ''}">
        <div class="card__header">
          <span class="material-symbols-outlined" aria-hidden="true">bed</span>
          <h2 class="md-typescale-title-medium card__title">Номер ${escapeHtml(room.number)}</h2>
          ${index === 0 ? '<span class="tag tag--best md-typescale-label-small">Рекомендуем</span>' : ''}
        </div>
        <p class="md-typescale-body-medium card__meta">${escapeHtml(hotelName(room.hotelId))}</p>
        <p class="md-typescale-body-small card__meta">Бронирований: ${room.timesBooked}</p>
        <div class="card__actions">
          <md-filled-tonal-button data-room="${room.id}">Забронировать</md-filled-tonal-button>
        </div>
      </article>`,
    )
    .join('');
}

$('#rooms').addEventListener('click', (event) => {
  const button = event.target.closest('[data-room]');
  if (button) book({ roomId: Number(button.dataset.room) });
});

$('#auto-book').addEventListener('click', () => book({ autoSelect: true, hotelId: state.search.hotelId || undefined }));

async function book(choice) {
  const { startDate, endDate } = state.search;
  const booking = await busy(() => api.book({ ...choice, startDate, endDate, requestId: crypto.randomUUID() }));
  if (!booking) return;
  const room = await roomOf(booking.roomId);
  notify(`Номер ${room.number} забронирован: ${formatDate(startDate)} — ${formatDate(endDate)}`);
  const rooms = await busy(() => api.recommendations(state.search));
  if (rooms) renderRooms(rooms);
}

// ---------- bookings ----------

$('#bookings-more').addEventListener('click', () => loadBookings(false));

async function loadBookings(fromStart) {
  if (fromStart) {
    state.bookingsPage = 0;
    $('#bookings').innerHTML = '';
  }
  const page = await busy(() => api.bookings(state.bookingsPage, PAGE_SIZE));
  if (!page) return;
  await busy(() => Promise.all(page.content.map((booking) => roomOf(booking.roomId))));
  $('#bookings').insertAdjacentHTML('beforeend', page.content.map(bookingRow).join(''));
  $('#bookings-empty').hidden = page.totalElements > 0;
  state.bookingsPage += 1;
  $('#bookings-more').hidden = state.bookingsPage >= page.totalPages;
}

function bookingRow(booking) {
  const room = state.rooms.get(booking.roomId);
  const [label, style] = STATUS[booking.status];
  const cancellable = booking.status !== 'CANCELLED';
  return `
    <article class="row" data-booking="${booking.id}">
      <div>
        <h2 class="md-typescale-title-medium">Номер ${escapeHtml(room?.number)} · ${escapeHtml(hotelName(room?.hotelId))}</h2>
        <p class="md-typescale-body-medium row__meta">
          ${formatDate(booking.startDate)} — ${formatDate(booking.endDate)} ·
          <span class="tag ${style} md-typescale-label-small">${label}</span>
        </p>
      </div>
      ${cancellable ? `<md-icon-button data-cancel="${booking.id}" aria-label="Отменить бронирование"><md-icon>event_busy</md-icon></md-icon-button>` : ''}
    </article>`;
}

$('#bookings').addEventListener('click', (event) => {
  const button = event.target.closest('[data-cancel]');
  if (!button) return;
  const dialog = $('#cancel-dialog');
  dialog.dataset.booking = button.dataset.cancel;
  $('#cancel-dialog-text').textContent = 'Слот в отеле освободится, восстановить бронь будет нельзя.';
  dialog.show();
});

$('#cancel-dialog').addEventListener('close', async (event) => {
  const dialog = event.currentTarget;
  if (dialog.returnValue !== 'cancel') return;
  const done = await busy(async () => (await api.cancel(dialog.dataset.booking)) ?? true);
  if (done) {
    notify('Бронирование отменено');
    await loadBookings(true);
  }
});

// ---------- occupancy ----------

$('#occupancy-form').addEventListener('submit', async (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  const report = await busy(() => api.occupancy({ startDate: field(form, 'startDate'), endDate: field(form, 'endDate') }));
  if (report) renderOccupancy(report);
});

function renderOccupancy(report) {
  const summary = $('#occupancy-summary');
  summary.hidden = false;
  summary.innerHTML = `
    <div><p class="md-typescale-label-medium">Средняя загрузка</p>
      <p class="md-typescale-headline-medium summary__value">${Math.round(report.averageOccupancy * 100)}%</p></div>
    <div><p class="md-typescale-label-medium">Номеров</p>
      <p class="md-typescale-headline-medium summary__value">${report.totalRooms}</p></div>
    <div><p class="md-typescale-label-medium">Ночей в периоде</p>
      <p class="md-typescale-headline-medium summary__value">${report.periodNights}</p></div>`;
  $('#occupancy').innerHTML = report.rooms
    .map(
      (room) => `
      <article class="row">
        <h2 class="md-typescale-title-small">Номер ${escapeHtml(room.number)} · ${escapeHtml(hotelName(room.hotelId))}</h2>
        <span class="md-typescale-label-large">${Math.round(room.occupancyRate * 100)}%</span>
        <p class="md-typescale-body-small row__meta">Занято ночей: ${room.bookedNights}, бронирований: ${room.timesBooked}</p>
        <span></span>
        <md-linear-progress value="${room.occupancyRate}" aria-label="Загрузка номера ${escapeHtml(room.number)}"></md-linear-progress>
      </article>`,
    )
    .join('');
}

// ---------- start ----------

async function start() {
  const claims = session.claims;
  if (!claims) {
    signOut();
    return;
  }
  $('#auth').hidden = true;
  $('#shell').hidden = false;
  $('#user-name').textContent = claims.sub;
  document.querySelectorAll('[data-role]').forEach((item) => (item.hidden = !session.hasRole(item.dataset.role)));

  const hotels = await busy(() => api.hotels());
  state.hotels = new Map((hotels ?? []).map((hotel) => [hotel.id, hotel]));
  $('#hotel-select').innerHTML =
    '<md-select-option value="" selected><div slot="headline">Все отели</div></md-select-option>' +
    (hotels ?? [])
      .map((hotel) => `<md-select-option value="${hotel.id}"><div slot="headline">${escapeHtml(hotel.name)}</div></md-select-option>`)
      .join('');

  const search = $('#search-form');
  search.querySelector('[name="startDate"]').value = isoDate(7);
  search.querySelector('[name="endDate"]').value = isoDate(9);
  const occupancy = $('#occupancy-form');
  occupancy.querySelector('[name="startDate"]').value = isoDate(0);
  occupancy.querySelector('[name="endDate"]').value = isoDate(30);
  show(session.hasRole('ADMIN') ? 'occupancy' : 'rooms');
}

start();
