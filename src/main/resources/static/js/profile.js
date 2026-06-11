/**
 * profile.js — User profile and leaderboard page logic.
 */

requireAuth();

const user = getUser();

document.getElementById('logout-btn')
  ?.addEventListener('click', logout);

// ── Load profile ───────────────────────────────────────────────

async function loadProfile() {
  try {
    const profile = await UserAPI.getProfile();

    document.getElementById('profile-username').textContent =
      profile.username;
    document.getElementById('profile-email').textContent =
      profile.email;
    document.getElementById('profile-role').textContent =
      profile.role;
    document.getElementById('profile-avatar').textContent =
      profile.username.charAt(0).toUpperCase();
    document.getElementById('stat-debates').textContent =
      profile.debateCount;
    document.getElementById('stat-messages').textContent =
      profile.messageCount;
    document.getElementById('stat-since').textContent =
      new Date(profile.createdAt).getFullYear();

  } catch (err) {
    console.error('Failed to load profile:', err);
  }
}

// ── Load my debates ────────────────────────────────────────────

async function loadMyDebates() {
  const list = document.getElementById('my-debates-list');
  try {
    const debates = await DebateAPI.myDebates();
    list.innerHTML = '';

    if (!debates.length) {
      list.innerHTML =
        '<p style="color:var(--text-muted);padding:24px">You have not participated in any debates yet.</p>';
      return;
    }

    debates.forEach(debate => {
      const item = document.createElement('a');
      item.className = 'debate-list-item';
      item.href = `/debate.html?id=${debate.id}`;

      const side =
        debate.favorUser?.id   === user?.id ? 'FAVOR' :
        debate.againstUser?.id === user?.id ? 'AGAINST' : '';

      item.innerHTML = `
        <span class="list-item-topic">${debate.topic}</span>
        <span class="card-status-badge badge-${debate.status}"
          style="font-size:11px;padding:2px 8px">
          ${debate.status}
        </span>
        ${side ? `<span class="list-item-meta">${side}</span>` : ''}
      `;
      list.appendChild(item);
    });

  } catch (err) {
    list.innerHTML = `<p style="color:var(--text-muted);padding:24px">
      Failed to load debates.</p>`;
  }
}

// ── Load leaderboard ───────────────────────────────────────────

async function loadLeaderboard() {
  const tbody = document.getElementById('leaderboard-body');
  try {
    const leaders = await UserAPI.getLeaderboard();
    tbody.innerHTML = '';

    leaders.forEach((leader, index) => {
      const tr = document.createElement('tr');
      tr.className = `rank-${index + 1}`;
      tr.innerHTML = `
        <td>${index + 1}</td>
        <td>${leader.username}</td>
        <td>${leader.debateCount}</td>
        <td>${leader.messageCount}</td>
      `;
      tbody.appendChild(tr);
    });

  } catch (err) {
    tbody.innerHTML =
      '<tr><td colspan="4">Failed to load leaderboard.</td></tr>';
  }
}

// ── Tab switching ──────────────────────────────────────────────

document.querySelectorAll('.profile-tab').forEach(tab => {
  tab.addEventListener('click', () => {
    document.querySelectorAll('.profile-tab')
      .forEach(t => t.classList.remove('active'));
    tab.classList.add('active');

    const tabId = tab.dataset.tab;
    document.querySelectorAll('.tab-content')
      .forEach(c => c.classList.add('hidden'));
    document.getElementById(`${tabId}-tab`)
      .classList.remove('hidden');

    if (tabId === 'leaderboard') loadLeaderboard();
  });
});

// ── Init ───────────────────────────────────────────────────────

loadProfile();
loadMyDebates();