/**
 * debateRoom.js — Real-time debate room logic.
 *
 * Responsibilities:
 *   1. Load debate details and chat history via REST
 *   2. Connect to WebSocket via SockJS + STOMP
 *   3. Send messages as a debater
 *   4. Receive and render real-time messages
 *   5. Handle voting as a spectator
 *   6. Handle debate end
 */

requireAuth();

console.log("DEBATE ROOM JS LOADED");
console.log("SockJS =", typeof SockJS);
console.log("StompJs =", typeof StompJs);


// ── State ──────────────────────────────────────────────────────

const params   = new URLSearchParams(window.location.search);
const debateId = params.get('id');
const user     = getUser();
const token    = getToken();

let debate      = null;
let stompClient = null;
let userRole    = 'SPECTATOR'; // 'FAVOR' | 'AGAINST' | 'SPECTATOR'
let hasVoted    = false;

if (!debateId) window.location.href = '/index.html';

// ── Boot sequence ──────────────────────────────────────────────

async function init() {
  try {
    // 1. Load debate details
    debate = await DebateAPI.getById(debateId);
    renderDebateInfo();

    // 2. Determine user's role in this debate
    userRole = resolveRole();

    // 3. Show/hide UI elements based on role
    renderRoleUI();

    // 4. Load full chat history
    const history = await MessageAPI.getHistory(debateId);
    history.forEach(appendMessage);
    scrollToBottom();

    // 5. Load vote counts
    await refreshVoteCounts();

    // 6. Connect to WebSocket
    connectWebSocket();

  } catch (err) {
    alert('Failed to load debate room: ' + err.message);
    window.location.href = '/index.html';
  }
}

// ── Debate info rendering ──────────────────────────────────────

function renderDebateInfo() {
  document.title = debate.topic + ' — Argue With Stranger';
  document.getElementById('debate-topic').textContent = debate.topic;

  const badge = document.getElementById('debate-status-badge');
  badge.textContent = debate.status;
  badge.className   = `status-badge badge-${debate.status}`;

  document.getElementById('favor-name').textContent =
    debate.favorUser?.username || '—';
  document.getElementById('against-name').textContent =
    debate.againstUser?.username || '—';
}

// ── Role resolution ────────────────────────────────────────────
function resolveRole() {

  console.log("Current User:", user);
  console.log("Favor User:", debate.favorUser);
  console.log("Against User:", debate.againstUser);

  if (debate.favorUser?.id === user?.id) return 'FAVOR';
  if (debate.againstUser?.id === user?.id) return 'AGAINST';

  return 'SPECTATOR';
}

// ── Role-based UI ──────────────────────────────────────────────

function renderRoleUI() {
  const isDebater   = userRole !== 'SPECTATOR';
  const isOngoing   = debate.status === 'ONGOING';
  const isClosed    = debate.status === 'CLOSED';

  // Message input — only for debaters in ONGOING debates
  const inputArea = document.getElementById('message-input-area');
  inputArea.classList.toggle('hidden', !isDebater || !isOngoing);

  // Spectator notice
  const notice = document.getElementById('spectator-notice');
  notice.classList.toggle('hidden', isDebater);

  // Voting buttons — only for spectators in non-OPEN debates
  const canVote = !isDebater && !isClosed && debate.status !== 'OPEN';
  document.getElementById('vote-favor-btn')
    .classList.toggle('hidden', !canVote);
  document.getElementById('vote-against-btn')
    .classList.toggle('hidden', !canVote);

  // End debate button — only for debaters in ONGOING debates
  const endBtn = document.getElementById('end-debate-btn');
  endBtn.classList.toggle('hidden', !isDebater || !isOngoing);

  // Result banner if closed
  if (isClosed) showResultBanner();
}

// ── WebSocket connection ───────────────────────────────────────

function connectWebSocket() {

  console.log("Connecting WebSocket...");

  const socket = new SockJS('http://localhost:8080/chat');

  stompClient = StompJs.Stomp.over(() => socket);

  stompClient.debug = () => {};

  stompClient.connect(
    { Authorization: `Bearer ${token}` },
    () => {
      console.log("WEBSOCKET CONNECTED");
      onConnected();
    },
    (error) => {
      console.error("WEBSOCKET ERROR:", error);
      onError(error);
    }
  );
}

function onConnected() {

  console.log("Subscribed to debate:", debateId);

  stompClient.subscribe(
    `/topic/debate/${debateId}`,
    (frame) => {

      console.log("MESSAGE RECEIVED:", frame.body);

      const message = JSON.parse(frame.body);
      appendMessage(message);
      scrollToBottom();
    }
  );

  stompClient.subscribe(
    '/user/queue/errors',
    (frame) => {
      console.warn("SERVER ERROR:", frame.body);
    }
  );
}

function onError(err) {
  console.error('WebSocket connection error:', err);
}

// ── Send message ───────────────────────────────────────────────

document.getElementById('send-btn')
  ?.addEventListener('click', sendMessage);

document.getElementById('message-input')
  ?.addEventListener('keydown', (e) => {
    // Ctrl+Enter or Cmd+Enter to send
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      sendMessage();
    }
  });

document.getElementById('message-input')
  ?.addEventListener('input', (e) => {
    const len = e.target.value.length;
    document.getElementById('char-count').textContent =
      `${len} / 2000`;
  });

function sendMessage() {
  const textarea = document.getElementById('message-input');
  const content  = textarea.value.trim();

  if (!content || !stompClient?.connected) return;

  stompClient.send(
    '/app/sendMessage',
    {},
    JSON.stringify({ debateId: parseInt(debateId), content })
  );

  textarea.value = '';
  document.getElementById('char-count').textContent = '0 / 2000';
}

// ── Message rendering ──────────────────────────────────────────

function appendMessage(msg) {
  const container = document.getElementById('chat-messages');

  // Clear placeholder on first message
  const placeholder = container.querySelector('.chat-placeholder');
  if (placeholder) placeholder.remove();

  const side  = (msg.senderSide || 'SPECTATOR').toLowerCase();
  const time  = new Date(msg.timestamp).toLocaleTimeString([], {
    hour: '2-digit', minute: '2-digit'
  });

  const bubble = document.createElement('div');
  bubble.className = `message-bubble ${side}`;
  bubble.innerHTML = `
    <div class="bubble-sender">${escapeHtml(msg.senderUsername)}</div>
    <div class="bubble-content">${escapeHtml(msg.content)}</div>
    <div class="bubble-time">${time}</div>
  `;

  container.appendChild(bubble);
}

function scrollToBottom() {
  const container = document.getElementById('chat-messages');
  container.scrollTop = container.scrollHeight;
}

// ── Voting ─────────────────────────────────────────────────────

document.getElementById('vote-favor-btn')
  ?.addEventListener('click', () => castVote('FAVOR'));
document.getElementById('vote-against-btn')
  ?.addEventListener('click', () => castVote('AGAINST'));

async function castVote(side) {
  if (hasVoted) return;
  try {
    await VoteAPI.castVote(debateId, side);
    hasVoted = true;
    document.getElementById('vote-favor-btn').classList.add('hidden');
    document.getElementById('vote-against-btn').classList.add('hidden');
    await refreshVoteCounts();
  } catch (err) {
    alert(err.message || 'Could not cast vote.');
  }
}

async function refreshVoteCounts() {
  try {
    const result = await VoteAPI.getResult(debateId);
    document.getElementById('favor-vote-count').textContent =
      `${result.favorCount} votes`;
    document.getElementById('against-vote-count').textContent =
      `${result.againstCount} votes`;
  } catch (_) { /* ignore if not yet available */ }
}

// ── End debate ─────────────────────────────────────────────────

document.getElementById('end-debate-btn')
  ?.addEventListener('click', async () => {
    if (!confirm('Are you sure you want to end this debate?')) return;
    try {
      debate = await DebateAPI.end(debateId);
      renderDebateInfo();
      renderRoleUI();
      showResultBanner();
      document.getElementById('message-input-area')
        .classList.add('hidden');
    } catch (err) {
      alert(err.message || 'Could not end debate.');
    }
  });

// ── Result banner ──────────────────────────────────────────────

async function showResultBanner() {
  const banner = document.getElementById('result-banner');
  banner.classList.remove('hidden');

  try {
    const result = await VoteAPI.getResult(debateId);
    const winnerText =
      result.winner === 'TIE'
        ? '🤝 It\'s a tie!'
        : `🏆 Winner: <strong>${result.winner}</strong>`;

    document.getElementById('result-content').innerHTML = `
      <p>${winnerText}</p>
      <p style="margin-top:8px;color:var(--text-muted);font-size:14px">
        Favor: ${result.favorCount} votes &nbsp;|&nbsp;
        Against: ${result.againstCount} votes &nbsp;|&nbsp;
        Total: ${result.totalVotes}
      </p>
    `;
  } catch (_) { /* votes not available */ }
}

// ── Profile JS ─────────────────────────────────────────────────

function escapeHtml(text) {
  const div = document.createElement('div');
  div.appendChild(document.createTextNode(text));
  return div.innerHTML;
}

// ── Start ──────────────────────────────────────────────────────

init();