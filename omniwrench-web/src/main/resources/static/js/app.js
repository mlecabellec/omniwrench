/**
 * Omniwrench Web HUD Client
 * Reactive WebSocket streaming, session lifecycle management, and OpenCode commands.
 */

(function () {
    'use strict';

    // Application State
    const state = {
        currentSessionId: null,
        activeGoal: "Autonomous pair-programming and tool assistance",
        sessions: [],
        tools: [],
        ws: null,
        wsReconnectTimer: null,
        isStreaming: false,
        activeMessageCard: null
    };

    // DOM Elements
    const elements = {
        messagesContainer: document.getElementById('messages-container'),
        welcomeBanner: document.getElementById('welcome-banner'),
        composerForm: document.getElementById('composer-form'),
        promptInput: document.getElementById('prompt-input'),
        sendBtn: document.getElementById('send-btn'),
        sessionsList: document.getElementById('sessions-list'),
        newSessionBtn: document.getElementById('new-session-btn'),
        toggleSidebarBtn: document.getElementById('toggle-sidebar-btn'),
        appSidebar: document.getElementById('app-sidebar'),
        currentGoalText: document.getElementById('current-goal-text'),
        sessionGoalPill: document.getElementById('session-goal-pill'),
        editGoalBtn: document.getElementById('edit-goal-btn'),
        goalDialog: document.getElementById('goal-dialog'),
        goalForm: document.getElementById('goal-form'),
        goalInputField: document.getElementById('goal-input-field'),
        helpDialog: document.getElementById('help-dialog'),
        openHelpBtn: document.getElementById('open-help-btn'),
        inspectorDrawer: document.getElementById('inspector-drawer'),
        closeDrawerBtn: document.getElementById('close-drawer-btn'),
        openDiagnosticsBtn: document.getElementById('open-diagnostics-btn'),
        openToolsBtn: document.getElementById('open-tools-btn'),
        tabDiagnosticsBtn: document.getElementById('tab-diagnostics-btn'),
        tabToolsBtn: document.getElementById('tab-tools-btn'),
        tabDiagnosticsPanel: document.getElementById('tab-diagnostics-panel'),
        tabToolsPanel: document.getElementById('tab-tools-panel'),
        diagnosticsDetails: document.getElementById('diagnostics-details'),
        toolsList: document.getElementById('tools-list'),
        toolsCountBadge: document.getElementById('tools-count-badge'),
        workbenchMode: document.getElementById('workbench-mode'),
        jvmVersionBadge: document.getElementById('jvm-version-badge'),
        wsBadge: document.getElementById('ws-badge'),
        wsLabel: document.getElementById('ws-label'),
        envBadge: document.getElementById('env-badge'),
        envLabel: document.getElementById('env-label'),
        clearChatBtn: document.getElementById('clear-chat-btn'),
        reasoningEffortSelect: document.getElementById('reasoning-effort-select')
    };

    // Initialize Application
    async function init() {
        setupEventListeners();
        detectEnvironment();
        await loadStatus();
        await loadTools();
        await loadSessions();
        connectWebSocket();
    }

    // Detect browser / client environment
    function detectEnvironment() {
        const isSecure = window.location.protocol === 'https:';
        const isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';
        if (elements.envLabel) {
            elements.envLabel.textContent = isLocal ? "ENV: LOCAL/WEB" : "ENV: REMOTE/WEB";
        }
    }

    // Setup Event Listeners
    function setupEventListeners() {
        // Composer Submission
        elements.composerForm.addEventListener('submit', handlePromptSubmit);
        elements.promptInput.addEventListener('keydown', (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                elements.composerForm.requestSubmit();
            }
        });

        // Toggle Sidebar
        elements.toggleSidebarBtn.addEventListener('click', () => {
            elements.appSidebar.classList.toggle('collapsed');
        });

        // New Session
        elements.newSessionBtn.addEventListener('click', createNewSession);

        // Goal Management
        elements.sessionGoalPill.addEventListener('click', openGoalModal);
        elements.editGoalBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            openGoalModal();
        });
        elements.goalForm.addEventListener('submit', (e) => {
            e.preventDefault();
            const newGoal = elements.goalInputField.value.trim();
            if (newGoal) {
                setGoal(newGoal);
            }
            elements.goalDialog.close();
        });

        // Dialog Close Buttons
        document.querySelectorAll('.close-modal-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const dialog = e.target.closest('dialog');
                if (dialog) dialog.close();
            });
        });

        // Help Modal
        elements.openHelpBtn.addEventListener('click', () => elements.helpDialog.showModal());

        // Inspector Drawer Toggles
        elements.openDiagnosticsBtn.addEventListener('click', () => openInspectorTab('diagnostics'));
        elements.openToolsBtn.addEventListener('click', () => openInspectorTab('tools'));
        elements.closeDrawerBtn.addEventListener('click', () => elements.inspectorDrawer.hidden = true);

        elements.tabDiagnosticsBtn.addEventListener('click', () => switchDrawerTab('diagnostics'));
        elements.tabToolsBtn.addEventListener('click', () => switchDrawerTab('tools'));

        // Quick Command Buttons
        document.querySelectorAll('.quick-cmd-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const cmd = btn.getAttribute('data-cmd');
                if (cmd) {
                    elements.promptInput.value = cmd;
                    elements.composerForm.requestSubmit();
                }
            });
        });

        // Chip / Toolbar Buttons
        document.querySelectorAll('.chip-btn[data-insert]').forEach(btn => {
            btn.addEventListener('click', () => {
                const insertText = btn.getAttribute('data-insert');
                elements.promptInput.value = insertText;
                elements.promptInput.focus();
            });
        });

        // Clear Chat
        elements.clearChatBtn.addEventListener('click', handleClearCommand);
    }

    // WebSocket Client
    function connectWebSocket() {
        if (state.ws) {
            try { state.ws.close(); } catch (_) {}
        }

        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/ws/agent-stream`;

        updateWsStatus('connecting', 'WS CONNECTING...');

        try {
            state.ws = new WebSocket(wsUrl);

            state.ws.onopen = () => {
                updateWsStatus('connected', '● WS LIVE');
                if (state.wsReconnectTimer) {
                    clearTimeout(state.wsReconnectTimer);
                    state.wsReconnectTimer = null;
                }
            };

            state.ws.onmessage = (event) => {
                try {
                    const frame = JSON.parse(event.data);
                    handleWebSocketFrame(frame);
                } catch (err) {
                    console.error("Failed to parse WebSocket message", err);
                }
            };

            state.ws.onclose = () => {
                updateWsStatus('disconnected', 'WS OFFLINE');
                scheduleReconnect();
            };

            state.ws.onerror = (err) => {
                console.warn("WebSocket error occurred", err);
                updateWsStatus('disconnected', 'WS ERROR');
            };
        } catch (err) {
            console.error("Error creating WebSocket connection", err);
            updateWsStatus('disconnected', 'WS OFFLINE');
            scheduleReconnect();
        }
    }

    function updateWsStatus(status, label) {
        elements.wsBadge.className = `status-pill ws-pill ${status}`;
        elements.wsLabel.textContent = label;
    }

    function scheduleReconnect() {
        if (!state.wsReconnectTimer) {
            state.wsReconnectTimer = setTimeout(() => {
                state.wsReconnectTimer = null;
                connectWebSocket();
            }, 4000);
        }
    }

    // Handle WebSocket event frames
    function handleWebSocketFrame(frame) {
        if (!frame || !frame.type) return;

        if (frame.type === 'thought.delta') {
            const thought = frame.data?.thinking || '';
            if (thought && state.activeMessageCard) {
                appendThinkingStream(state.activeMessageCard, thought);
            }
        } else if (frame.type === 'chat.delta') {
            const text = frame.data?.content || '';
            if (state.activeMessageCard) {
                appendMessageContent(state.activeMessageCard, text);
            }
        } else if (frame.type === 'chat.complete') {
            state.isStreaming = false;
            state.activeMessageCard = null;
            elements.sendBtn.disabled = false;
            scrollToBottom();
        }
    }

    // Prompt Submission & Command Dispatch
    async function handlePromptSubmit(e) {
        e.preventDefault();
        const rawText = elements.promptInput.value.trim();
        if (!rawText || state.isStreaming) return;

        elements.promptInput.value = '';

        // Hide welcome banner on first prompt
        if (elements.welcomeBanner) {
            elements.welcomeBanner.style.display = 'none';
        }

        // Check for client-side / slash commands
        if (rawText === '/help') {
            elements.helpDialog.showModal();
            return;
        }

        if (rawText === '/clear') {
            await handleClearCommand();
            return;
        }

        if (rawText === '/env') {
            await handleEnvCommand();
            return;
        }

        if (rawText === '/tools') {
            openInspectorTab('tools');
            return;
        }

        if (rawText.startsWith('/goal')) {
            const newGoal = rawText.substring(5).trim();
            if (newGoal) {
                setGoal(newGoal);
                renderSystemNotice(`Session Goal updated to: "${newGoal}"`);
            } else {
                renderSystemNotice(`Current Session Goal: "${state.activeGoal}"`);
            }
            return;
        }

        if (rawText.startsWith('/btw')) {
            const query = rawText.substring(4).trim();
            if (query) {
                await handleBtwQuery(query);
            } else {
                renderSystemNotice("Usage: /btw <question> (Ask an ephemeral side-channel question)");
            }
            return;
        }

        // Regular Conversational Prompt
        appendUserMessage(rawText);

        const effort = elements.reasoningEffortSelect ? elements.reasoningEffortSelect.value : 'medium';
        const payloadPrompt = (effort !== 'medium')
            ? `[REASONING_EFFORT:${effort}] ${rawText}`
            : rawText;

        state.isStreaming = true;
        elements.sendBtn.disabled = true;

        // Create empty agent message card awaiting stream deltas
        state.activeMessageCard = createAgentMessageCard();

        if (state.ws && state.ws.readyState === WebSocket.OPEN) {
            // Stream over WebSocket
            state.ws.send(JSON.stringify({
                sessionId: state.currentSessionId,
                prompt: payloadPrompt
            }));
        } else {
            // Fallback to REST API
            try {
                const res = await fetch(`/api/v1/sessions/${state.currentSessionId}/prompt`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ prompt: payloadPrompt })
                });

                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                const data = await res.json();

                if (data.thinking) {
                    appendThinkingStream(state.activeMessageCard, data.thinking);
                }
                appendMessageContent(state.activeMessageCard, data.content || "");
            } catch (err) {
                appendMessageContent(state.activeMessageCard, `Error: Failed to process prompt via REST (${err.message}).`);
            } finally {
                state.isStreaming = false;
                state.activeMessageCard = null;
                elements.sendBtn.disabled = false;
                scrollToBottom();
            }
        }
    }

    // Handle /btw Ephemeral Side-Channel Query
    async function handleBtwQuery(query) {
        renderBtwUserCard(query);

        try {
            const res = await fetch(`/api/v1/sessions/${state.currentSessionId}/prompt`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ prompt: `[SIDE-CHANNEL QUESTION] ${query}` })
            });

            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            renderBtwResponseCard(query, data.content || "");
        } catch (err) {
            renderBtwResponseCard(query, `Side-channel query failed: ${err.message}`);
        }
    }

    // Handle /env Command
    async function handleEnvCommand() {
        await loadStatus();
        openInspectorTab('diagnostics');
        renderSystemNotice("Displayed runtime environment and system diagnostics in the right inspector drawer.");
    }

    // Handle /clear Command
    async function handleClearCommand() {
        if (!state.currentSessionId) return;

        try {
            await fetch(`/api/v1/sessions/${state.currentSessionId}/messages`, { method: 'DELETE' });
        } catch (_) {}

        elements.messagesContainer.innerHTML = '';
        renderSystemNotice("Conversation history cleared.");
    }

    // Render Methods
    function appendUserMessage(text) {
        const card = document.createElement('div');
        card.className = 'message-card user';

        const meta = document.createElement('div');
        meta.className = 'message-meta';
        meta.innerHTML = `<span class="message-role">USER</span> <span>${formatTimestamp()}</span>`;

        const bubble = document.createElement('div');
        bubble.className = 'message-bubble';
        bubble.textContent = text;

        card.appendChild(meta);
        card.appendChild(bubble);
        elements.messagesContainer.appendChild(card);
        scrollToBottom();
    }

    function createAgentMessageCard() {
        const card = document.createElement('div');
        card.className = 'message-card agent';

        const meta = document.createElement('div');
        meta.className = 'message-meta';
        meta.innerHTML = `<span class="message-role">AGENT / OMNIWRENCH</span> <span>${formatTimestamp()}</span>`;

        const bubble = document.createElement('div');
        bubble.className = 'message-bubble';
        bubble.innerHTML = '<span class="typing-indicator">Analyzing and thinking...</span>';

        card.appendChild(meta);
        card.appendChild(bubble);
        elements.messagesContainer.appendChild(card);
        scrollToBottom();
        return card;
    }

    function appendThinkingStream(card, thoughtText) {
        let accordion = card.querySelector('.thinking-accordion');
        if (!accordion) {
            accordion = document.createElement('div');
            accordion.className = 'thinking-accordion open';

            const header = document.createElement('div');
            header.className = 'thinking-header';
            header.innerHTML = `
                <span class="thinking-title">
                    <span class="thinking-arrow">▶</span>
                    <span>REASONING / THINKING STREAM</span>
                </span>
                <span class="pill-dot ws-dot"></span>
            `;
            header.addEventListener('click', () => {
                accordion.classList.toggle('open');
            });

            const body = document.createElement('div');
            body.className = 'thinking-body';
            body.textContent = thoughtText;

            accordion.appendChild(header);
            accordion.appendChild(body);

            // Insert accordion before message bubble
            const bubble = card.querySelector('.message-bubble');
            card.insertBefore(accordion, bubble);
        } else {
            const body = accordion.querySelector('.thinking-body');
            body.textContent = thoughtText;
        }
    }

    function appendMessageContent(card, contentText) {
        const bubble = card.querySelector('.message-bubble');
        if (!bubble) return;

        // Remove placeholder indicator
        const indicator = bubble.querySelector('.typing-indicator');
        if (indicator) indicator.remove();

        bubble.innerHTML = formatMarkdown(contentText);

        // Attach copy button handlers
        bubble.querySelectorAll('.copy-code-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const code = e.target.closest('.code-block').querySelector('code')?.innerText || '';
                navigator.clipboard.writeText(code).then(() => {
                    btn.textContent = 'Copied!';
                    setTimeout(() => btn.textContent = 'Copy', 1800);
                });
            });
        });
    }

    function renderBtwUserCard(query) {
        const card = document.createElement('div');
        card.className = 'btw-card';
        card.innerHTML = `
            <div class="btw-header">💡 [BY THE WAY / SIDE-CHANNEL QUERY]</div>
            <div class="btw-content">${escapeHtml(query)}</div>
        `;
        elements.messagesContainer.appendChild(card);
        scrollToBottom();
    }

    function renderBtwResponseCard(query, answer) {
        const card = document.createElement('div');
        card.className = 'btw-card';
        card.innerHTML = `
            <div class="btw-header">⚡ [SIDE-CHANNEL RESPONSE: ${escapeHtml(query)}]</div>
            <div class="btw-content">${formatMarkdown(answer)}</div>
        `;
        elements.messagesContainer.appendChild(card);
        scrollToBottom();
    }

    function renderSystemNotice(msg) {
        const card = document.createElement('div');
        card.className = 'card card-subtle';
        card.style.margin = '8px 0';
        card.innerHTML = `<span class="highlight-cyan font-mono">⚡ [SYSTEM]</span> ${escapeHtml(msg)}`;
        elements.messagesContainer.appendChild(card);
        scrollToBottom();
    }

    // Sessions Management
    async function loadSessions() {
        try {
            const res = await fetch('/api/v1/sessions');
            if (!res.ok) throw new Error("Failed to load sessions");
            state.sessions = await res.json();

            renderSessionsList();

            if (state.sessions.length > 0) {
                if (!state.currentSessionId || !state.sessions.some(s => s.sessionId === state.currentSessionId)) {
                    selectSession(state.sessions[0].sessionId);
                }
            } else {
                await createNewSession();
            }
        } catch (err) {
            elements.sessionsList.innerHTML = `<div class="error-state">Failed to load sessions: ${err.message}</div>`;
        }
    }

    function renderSessionsList() {
        elements.sessionsList.innerHTML = '';
        state.sessions.forEach(session => {
            const item = document.createElement('div');
            item.className = `session-item ${session.sessionId === state.currentSessionId ? 'active' : ''}`;
            item.innerHTML = `
                <div class="session-item-header">
                    <span class="session-id">${session.sessionId.substring(0, 8)}...</span>
                    <span class="session-msg-badge">${session.messageCount || 0} msgs</span>
                </div>
                <div class="session-path">${session.workspaceRoot || './'}</div>
            `;
            item.addEventListener('click', () => selectSession(session.sessionId));
            elements.sessionsList.appendChild(item);
        });
    }

    async function selectSession(sessionId) {
        state.currentSessionId = sessionId;
        renderSessionsList();

        try {
            const res = await fetch(`/api/v1/sessions/${sessionId}/messages`);
            if (res.ok) {
                const messages = await res.json();
                elements.messagesContainer.innerHTML = '';

                if (messages.length === 0) {
                    if (elements.welcomeBanner) {
                        elements.welcomeBanner.style.display = 'block';
                        elements.messagesContainer.appendChild(elements.welcomeBanner);
                    }
                } else {
                    messages.forEach(msg => {
                        if (msg.role === 'user') {
                            appendUserMessage(msg.content);
                        } else {
                            const card = createAgentMessageCard();
                            if (msg.thinking) {
                                appendThinkingStream(card, msg.thinking);
                            }
                            appendMessageContent(card, msg.content || "");
                        }
                    });
                }
            }
        } catch (err) {
            console.error("Failed to load session messages", err);
        }
    }

    async function createNewSession() {
        try {
            const res = await fetch('/api/v1/sessions', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({})
            });
            if (res.ok) {
                const newSession = await res.json();
                state.sessions.unshift(newSession);
                await selectSession(newSession.sessionId);
            }
        } catch (err) {
            console.error("Failed to create new session", err);
        }
    }

    // Status and Tools Management
    async function loadStatus() {
        try {
            const res = await fetch('/api/v1/status');
            if (res.ok) {
                const status = await res.json();
                if (elements.workbenchMode) elements.workbenchMode.textContent = (status.mode || "DUAL").toUpperCase();
                if (elements.toolsCountBadge) elements.toolsCountBadge.textContent = status.registeredTools || 0;
                if (elements.jvmVersionBadge) elements.jvmVersionBadge.textContent = status.jvmVersion || "Java 21";
                if (elements.diagnosticsDetails) {
                    elements.diagnosticsDetails.textContent = JSON.stringify(status, null, 2);
                }
            }
        } catch (err) {
            console.error("Failed to load status", err);
        }
    }

    async function loadTools() {
        try {
            const res = await fetch('/api/v1/tools');
            if (res.ok) {
                state.tools = await res.json();
                if (elements.toolsCountBadge) elements.toolsCountBadge.textContent = state.tools.length;
                renderToolsCatalog();
            }
        } catch (err) {
            console.error("Failed to load tools", err);
        }
    }

    function renderToolsCatalog() {
        if (!elements.toolsList) return;
        elements.toolsList.innerHTML = '';
        state.tools.forEach(tool => {
            const card = document.createElement('div');
            card.className = 'tool-item';
            card.innerHTML = `
                <div class="tool-name">⚡ ${escapeHtml(tool.name)}</div>
                <div class="tool-desc">${escapeHtml(tool.description)}</div>
                <pre class="code-block" style="font-size: 10px;">${JSON.stringify(tool.parameterSchema || {}, null, 2)}</pre>
            `;
            elements.toolsList.appendChild(card);
        });
    }

    // Goal Management
    function setGoal(goal) {
        state.activeGoal = goal;
        if (elements.currentGoalText) elements.currentGoalText.textContent = goal;
    }

    function openGoalModal() {
        elements.goalInputField.value = state.activeGoal;
        elements.goalDialog.showModal();
    }

    // Inspector Drawer
    function openInspectorTab(tab) {
        elements.inspectorDrawer.hidden = false;
        switchDrawerTab(tab);
    }

    function switchDrawerTab(tab) {
        if (tab === 'diagnostics') {
            elements.tabDiagnosticsBtn.classList.add('active');
            elements.tabToolsBtn.classList.remove('active');
            elements.tabDiagnosticsPanel.classList.add('active');
            elements.tabToolsPanel.classList.remove('active');
        } else {
            elements.tabToolsBtn.classList.add('active');
            elements.tabDiagnosticsBtn.classList.remove('active');
            elements.tabToolsPanel.classList.add('active');
            elements.tabDiagnosticsPanel.classList.remove('active');
        }
    }

    // Utilities
    function scrollToBottom() {
        elements.messagesContainer.scrollTop = elements.messagesContainer.scrollHeight;
    }

    function formatTimestamp() {
        const d = new Date();
        return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function formatMarkdown(content) {
        if (!content) return '';

        // Code blocks: ```lang ... ```
        let formatted = content.replace(/```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g, (match, lang, code) => {
            const langLabel = lang ? lang.toUpperCase() : 'CODE';
            return `<pre class="code-block"><div class="code-header"><span>${langLabel}</span><button class="copy-code-btn">Copy</button></div><code>${escapeHtml(code.trim())}</code></pre>`;
        });

        // Inline code: `code`
        formatted = formatted.replace(/`([^`]+)`/g, '<code>$1</code>');

        // Bold: **text**
        formatted = formatted.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

        // Line breaks to <br> (except inside pre)
        const parts = formatted.split(/(<pre[\s\S]*?<\/pre>)/g);
        for (let i = 0; i < parts.length; i++) {
            if (!parts[i].startsWith('<pre')) {
                parts[i] = parts[i].replace(/\n/g, '<br>');
            }
        }

        return parts.join('');
    }

    // Boot
    window.addEventListener('DOMContentLoaded', init);
})();
