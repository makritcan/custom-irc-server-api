document.addEventListener('DOMContentLoaded', () => {
    const chatHistory = document.getElementById('chat-history');
    const messageInput = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-btn');
    const loginModal = document.getElementById('login-modal');
    const chatInterface = document.getElementById('chat-interface');

    const isAdmin = window.isAdmin || false;

    let username = isAdmin ? "ADMIN" : localStorage.getItem('irc_anon_id');


    if (!isAdmin && !username) {
        username = "Anon-" + Math.floor(Math.random() * 10000);
        localStorage.setItem('irc_anon_id', username);

        if (chatInterface) chatInterface.classList.remove('hidden');
    } else if (!isAdmin && username) {
        if (chatInterface) chatInterface.classList.remove('hidden');
    }


    if (isAdmin) {
        const loginBtn = document.getElementById('login-btn');
        const passInput = document.getElementById('password-input');
        const errorMsg = document.getElementById('error-msg');


        if (sessionStorage.getItem('admin_session') === 'ok') {
            loginModal.classList.add('hidden');
            chatInterface.classList.remove('hidden');
        }

        if (loginBtn) {
            loginBtn.addEventListener('click', async () => {
                const pass = passInput.value;
                try {
                    const res = await fetch('/auth', {
                        method: 'POST',
                        body: JSON.stringify({ password: pass })
                    });

                    if (res.status === 200) {
                        sessionStorage.setItem('admin_session', 'ok');
                        loginModal.classList.add('hidden');
                        chatInterface.classList.remove('hidden');
                        loadHistory();
                    } else {
                        errorMsg.style.display = 'block';
                    }
                } catch (e) {
                    console.error("Auth error", e);
                }
            });
        }
    } else {

        loadHistory();
    }


    setInterval(loadHistory, 2000);


    async function sendMessage() {
        const message = messageInput.value.trim();
        if (!message) return;

        try {
            const payload = {
                channel: '#genel',
                message: message
            };

            if (isAdmin) {
                payload.sender = "SERVER";
                payload.message = "[ANC] " + message;
            } else {
                payload.sender = username;
            }

            await fetch('/send', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            messageInput.value = '';
            loadHistory();
        } catch (e) {
            console.error("Send error", e);
        }
    }

    sendBtn.addEventListener('click', sendMessage);
    messageInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') sendMessage();
    });


    async function loadHistory() {
        try {
            const res = await fetch('/history?channel=genel');
            const messages = await res.json();
            renderMessages(messages);
        } catch (e) {
            console.error("Load history error", e);
        }
    }

    let lastMsgCount = 0;
    function renderMessages(messages) {
        if (messages.length === lastMsgCount) return;

        chatHistory.innerHTML = '';

        messages.forEach(msg => {
            const div = document.createElement('div');
            div.className = 'message';

            const isSelf = msg.sender === username;


            const isSystem = msg.sender === "SystemBot";
            const isAdminMsg = msg.sender === "SERVER" || msg.sender === "ADMIN" || msg.content.startsWith("[DUYURU]");

            if (isSystem) div.classList.add('system');
            else if (isAdminMsg) {
                div.classList.add('system');
                div.style.border = "1px solid red";
                div.style.color = "#ffcccc";
            }
            else if (isSelf) div.classList.add('self');
            else div.classList.add('other');

            const senderSpan = document.createElement('span');
            senderSpan.className = 'sender-name';


            if (!isAdmin && !isSelf && !isAdminMsg && !isSystem) {
                senderSpan.textContent = "Anonim";
            } else {
                senderSpan.textContent = msg.sender;
            }


            senderSpan.textContent = msg.sender;

            const contentSpan = document.createElement('div');
            contentSpan.textContent = msg.content;

            if (!isSystem) div.appendChild(senderSpan);
            div.appendChild(contentSpan);

            chatHistory.appendChild(div);
        });


        if (messages.length > lastMsgCount) {
            chatHistory.scrollTop = chatHistory.scrollHeight;
        }
        lastMsgCount = messages.length;
    }
});
