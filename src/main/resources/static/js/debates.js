/**
 * debates.js — Home page logic.
 * Loads debates, handles search, filtering, pagination, and joining.
 */

requireAuth();

// ── State ──────────────────────────────────────────────────────

let currentPage   = 0;
let currentStatus = 'ALL';
let searchTimer   = null;
let totalPages    = 1;

const user = getUser();

// ── Init ───────────────────────────────────────────────────────

document.getElementById('nav-username').textContent =
  user ? `@${user.username}` : '';

document.getElementById('logout-btn')
  .addEventListener('click', logout);

// ── Load debates ───────────────────────────────────────────────

async function loadDebates(page = 0) {
  const grid = document.getElementById('debates-grid');
  grid.innerHTML = '<div class="loading-spinner">Loading debates...</div>';

  try {
    const keyword = document.getElementById('search-input').value.trim();
    let data;

    if (keyword) {
      data = await DebateAPI.search(keyword, page);
    } else if (currentStatus === 'ALL') {
      data = await DebateAPI.getAll(page);
    } else {
      data = await DebateAPI.getByStatus(currentStatus, page);
    }

    totalPages   = data.totalPages;
    currentPage  = data.number;

    renderDebates(data.content);
    renderPagination(data.totalPages, data.number);

    document.getElementById("total-debates").textContent =
        data.totalElements;

    document.getElementById("live-debates").textContent =
        data.content.filter(d => d.status === "ONGOING").length;

    document.getElementById("total-users").textContent =
        "1000+";

  } catch (err) {
    grid.innerHTML =
      `<div class="loading-spinner">Failed to load debates: ${err.message}</div>`;
  }
}

// ── Render debate cards ────────────────────────────────────────

function renderDebates(debates) {
  const grid = document.getElementById('debates-grid');
  const template = document.getElementById('debate-card-template');

  grid.innerHTML = '';

  if (!debates || debates.length === 0) {
    grid.innerHTML =
      '<div class="loading-spinner">No debates found.</div>';
    return;
  }

  debates.forEach(debate => {
    const card = template.content.cloneNode(true);

    // Status badge
    const badge = card.querySelector('.card-status-badge');
if (debate.status === "ONGOING") {
    badge.textContent = "🟢 LIVE";
}
else if (debate.status === "OPEN") {
    badge.textContent = "🟡 OPEN";
}
else {
    badge.textContent = "🔴 ENDED";
}
    badge.classList.add(`badge-${debate.status}`);

    // Topic and description
    const category =
        card.querySelector(".card-category");

    category.textContent =
        debate.category || "General";
    card.querySelector('.card-topic').textContent = debate.topic;
    card.querySelector('.card-description').textContent =
      debate.description || 'No description provided.';

    // Participants
    const favorName = card.querySelector('.favor .participant-name');
    const againstName = card.querySelector('.against .participant-name');

    favorName.textContent =
      debate.favorUser?.username || 'Open';

    againstName.textContent =
      debate.againstUser?.username || 'Open';

    // Buttons
    const favorBtn = card.querySelector('.join-favor-btn');
    const againstBtn = card.querySelector('.join-against-btn');
    const watchBtn = card.querySelector('.watch-btn');

    const isClosed = debate.status === 'CLOSED';

    const favorFull = !!debate.favorUser;
    const againstFull = !!debate.againstUser;

    const isParticipant =
      debate.favorUser?.id === user?.id ||
      debate.againstUser?.id === user?.id;

    // Show/hide join buttons
    favorBtn.classList.toggle(
      'hidden',
      isClosed || favorFull || isParticipant
    );

    againstBtn.classList.toggle(
      'hidden',
      isClosed || againstFull || isParticipant
    );

    // Change Watch button text for participants
    if (debate.status === 'ONGOING' && isParticipant) {
      watchBtn.textContent = 'Continue Debate';
    } else {
      watchBtn.textContent = 'Watch';
    }

    // Events
    favorBtn.addEventListener('click', () =>
      joinDebate(debate.id, 'FAVOR')
    );

    againstBtn.addEventListener('click', () =>
      joinDebate(debate.id, 'AGAINST')
    );

    watchBtn.addEventListener('click', () =>
      enterRoom(debate.id)
    );

    grid.appendChild(card);
  });
}
// ── Join debate ────────────────────────────────────────────────

async function joinDebate(debateId, side) {
  try {
    await DebateAPI.join(debateId, side);
    enterRoom(debateId);
  } catch (err) {
    alert(err.message || 'Could not join debate.');
  }
}

function enterRoom(debateId) {
  window.location.href = `/debate.html?id=${debateId}`;
}

// ── Pagination ─────────────────────────────────────────────────

function renderPagination(total, current) {
  const container = document.getElementById('pagination');
  container.innerHTML = '';

  for (let i = 0; i < total; i++) {
    const btn = document.createElement('button');
    btn.className  = `page-btn${i === current ? ' active' : ''}`;
    btn.textContent = i + 1;
    btn.addEventListener('click', () => loadDebates(i));
    container.appendChild(btn);
  }
}

// ── Search ─────────────────────────────────────────────────────

document.getElementById('search-input')
  .addEventListener('input', () => {
    clearTimeout(searchTimer);
    searchTimer = setTimeout(() => loadDebates(0), 400);
  });

// ── Status filter tabs ─────────────────────────────────────────

document.querySelectorAll('.filter-tab').forEach(tab => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.filter-tab')
      .forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    currentStatus = tab.dataset.status;
    loadDebates(0);
  });
});

// ── Initial load ───────────────────────────────────────────────

loadDebates(0);