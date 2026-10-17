// Dynamically detect base URL: works both when served from Spring Boot (:8080) and file:/// or live-server
const API_BASE = window.location.origin.includes(':8080')
    ? '/api'
    : 'http://localhost:8080/api';

let currentAuthMode = 'login'; // 'login' or 'register'

// Check authentication on page load
window.onload = function () {
    checkAuthStatus();
};

// ----------------------------------------------------
// Authentication & Session Management
// ----------------------------------------------------

// Check if user has an active session
function checkAuthStatus() {
    fetch(`${API_BASE}/auth/status`, { credentials: 'include' })
        .then(response => response.json())
        .then(data => {
            if (data.authenticated) {
                showLoggedIn(data.username);
                loadSnippets();
            } else {
                showLoggedOut();
            }
        })
        .catch(() => {
            showLoggedOut();
        });
}

function showLoggedIn(username) {
    document.getElementById('authHeader').style.display = 'flex';
    document.getElementById('loggedInUsername').textContent = username;
    document.getElementById('authSection').style.display = 'none';
    document.getElementById('snippetAppSection').style.display = 'block';
    hideAuthMessage();
}

function showLoggedOut() {
    document.getElementById('authHeader').style.display = 'none';
    document.getElementById('authSection').style.display = 'block';
    document.getElementById('snippetAppSection').style.display = 'none';
}

function switchAuthTab(mode) {
    currentAuthMode = mode;
    hideAuthMessage();
    if (mode === 'login') {
        document.getElementById('tabLogin').classList.add('active');
        document.getElementById('tabRegister').classList.remove('active');
        document.getElementById('authSubmitBtn').textContent = 'Login';
    } else {
        document.getElementById('tabRegister').classList.add('active');
        document.getElementById('tabLogin').classList.remove('active');
        document.getElementById('authSubmitBtn').textContent = 'Register';
    }
}

function handleAuthSubmit() {
    const username = document.getElementById('authUsername').value.trim();
    const password = document.getElementById('authPassword').value;

    if (!username || !password) {
        showAuthMessage('Please enter both username and password.', 'error');
        return;
    }

    if (currentAuthMode === 'register') {
        // Register new account
        fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        })
        .then(async response => {
            const data = await response.json();
            if (!response.ok) {
                throw new Error(data.error || 'Registration failed');
            }
            return data;
        })
        .then(() => {
            // Auto login after registration
            loginUser(username, password);
        })
        .catch(err => {
            showAuthMessage(err.message, 'error');
        });
    } else {
        // Login existing account
        loginUser(username, password);
    }
}

function loginUser(username, password) {
    fetch(`${API_BASE}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ username, password })
    })
    .then(async response => {
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error || 'Login failed');
        }
        return data;
    })
    .then(data => {
        document.getElementById('authUsername').value = '';
        document.getElementById('authPassword').value = '';
        showLoggedIn(data.username);
        loadSnippets();
    })
    .catch(err => {
        showAuthMessage(err.message, 'error');
    });
}

function logout() {
    authFetch(`${API_BASE}/auth/logout`, { method: 'POST' })
        .then(() => {
            showLoggedOut();
            showAuthMessage('Logged out successfully.', 'success');
        })
        .catch(() => {
            showLoggedOut();
        });
}

function showAuthMessage(msg, type) {
    const el = document.getElementById('authMessage');
    el.textContent = msg;
    el.className = 'auth-message ' + type;
    el.style.display = 'block';
}

function hideAuthMessage() {
    const el = document.getElementById('authMessage');
    el.style.display = 'none';
    el.textContent = '';
}

// ----------------------------------------------------
// CSRF & Authenticated Fetch Helper
// ----------------------------------------------------

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return decodeURIComponent(parts.pop().split(';').shift());
    return null;
}

async function getCsrfToken() {
    let token = getCookie('XSRF-TOKEN');
    if (!token) {
        try {
            const res = await fetch(`${API_BASE}/auth/csrf`, { credentials: 'include' });
            if (res.ok) {
                const data = await res.json();
                token = data.token || '';
            }
        } catch (e) {
            // ignore
        }
    }
    return token || getCookie('XSRF-TOKEN') || '';
}

async function authFetch(url, options = {}) {
    options.credentials = 'include';
    if (!options.headers) {
        options.headers = {};
    }

    const method = (options.method || 'GET').toUpperCase();
    if (['POST', 'PUT', 'DELETE'].includes(method)) {
        const token = await getCsrfToken();
        if (token) {
            options.headers['X-XSRF-TOKEN'] = token;
        }
    }

    const response = await fetch(url, options);
    if (response.status === 401) {
        showLoggedOut();
        throw new Error('Unauthorized. Please log in.');
    }
    return response;
}

// ----------------------------------------------------
// Snippet CRUD Operations (User-Scoped)
// ----------------------------------------------------

// Fetch and display snippets belonging to logged in user
function loadSnippets() {
    authFetch(`${API_BASE}/snippets`)
        .then(response => response.json())
        .then(snippets => displaySnippets(snippets))
        .catch(err => {
            if (err.message !== 'Unauthorized. Please log in.') {
                alert('Error loading snippets: ' + err.message);
            }
        });
}

// Search snippets by language and/or title
function searchSnippets() {
    const title = document.getElementById('searchTitle').value.trim();
    const language = document.getElementById('searchLanguage').value.trim();

    let url = `${API_BASE}/snippets/search?`;
    if (title) url += 'title=' + encodeURIComponent(title) + '&';
    if (language) url += 'language=' + encodeURIComponent(language);

    authFetch(url)
        .then(response => response.json())
        .then(snippets => displaySnippets(snippets))
        .catch(err => alert('Error searching: ' + err.message));
}

function clearSearch() {
    document.getElementById('searchTitle').value = '';
    document.getElementById('searchLanguage').value = '';
    loadSnippets();
}

// Render snippet cards
function displaySnippets(snippets) {
    const list = document.getElementById('snippetList');

    if (!snippets || snippets.length === 0) {
        list.innerHTML = '<p class="no-snippets">No snippets found.</p>';
        return;
    }

    list.innerHTML = snippets.map(snippet => `
        <div class="snippet-card">
            <div class="snippet-header">
                <h3>${escapeHtml(snippet.title)}</h3>
                <span class="language-tag">${escapeHtml(snippet.language)}</span>
            </div>
            ${snippet.description ? `<p class="snippet-description">${escapeHtml(snippet.description)}</p>` : ''}
            <pre class="snippet-code">${escapeHtml(snippet.code)}</pre>
            <div class="snippet-actions">
                <button class="btn-copy" onclick="copyCode(${snippet.id})">Copy</button>
                <button class="btn-edit" onclick="editSnippet(${snippet.id})">Edit</button>
                <button class="btn-delete" onclick="deleteSnippet(${snippet.id})">Delete</button>
            </div>
        </div>
    `).join('');
}

// Save snippet - handles both create and update
function saveSnippet() {
    const id = document.getElementById('editId').value;
    const title = document.getElementById('title').value.trim();
    const language = document.getElementById('language').value.trim();
    const description = document.getElementById('description').value.trim();
    const code = document.getElementById('code').value.trim();

    if (!title || !language || !code) {
        alert('Title, language, and code are required.');
        return;
    }

    const snippet = { title, language, description, code };

    if (id) {
        // Update existing snippet
        authFetch(`${API_BASE}/snippets/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(snippet)
        })
        .then(async response => {
            if (!response.ok) {
                const data = await response.json();
                throw new Error(data.error || 'Failed to update snippet');
            }
            return response.json();
        })
        .then(() => {
            cancelEdit();
            loadSnippets();
        })
        .catch(err => alert('Error updating snippet: ' + err.message));
    } else {
        // Create new snippet
        authFetch(`${API_BASE}/snippets`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(snippet)
        })
        .then(async response => {
            if (!response.ok) {
                const data = await response.json();
                throw new Error(data.error || 'Failed to save snippet');
            }
            return response.json();
        })
        .then(() => {
            clearForm();
            loadSnippets();
        })
        .catch(err => alert('Error saving snippet: ' + err.message));
    }
}

// Load snippet data into form for editing
function editSnippet(id) {
    authFetch(`${API_BASE}/snippets/${id}`)
        .then(async response => {
            if (!response.ok) throw new Error('Snippet not found or access denied');
            return response.json();
        })
        .then(snippet => {
            document.getElementById('editId').value = snippet.id;
            document.getElementById('title').value = snippet.title;
            document.getElementById('language').value = snippet.language;
            document.getElementById('description').value = snippet.description || '';
            document.getElementById('code').value = snippet.code;
            document.getElementById('formTitle').textContent = 'Edit Snippet';
            document.getElementById('cancelBtn').style.display = 'inline-block';
            window.scrollTo({ top: 0, behavior: 'smooth' });
        })
        .catch(err => alert('Error loading snippet: ' + err.message));
}

// Delete a snippet
function deleteSnippet(id) {
    if (!confirm('Are you sure you want to delete this snippet?')) return;

    authFetch(`${API_BASE}/snippets/${id}`, { method: 'DELETE' })
        .then(async response => {
            if (!response.ok) throw new Error('Snippet not found or access denied');
            loadSnippets();
        })
        .catch(err => alert('Error deleting snippet: ' + err.message));
}

// Copy code to clipboard
function copyCode(id) {
    authFetch(`${API_BASE}/snippets/${id}`)
        .then(response => response.json())
        .then(snippet => {
            navigator.clipboard.writeText(snippet.code)
                .then(() => alert('Code copied to clipboard!'))
                .catch(() => alert('Copy failed. Please copy manually.'));
        })
        .catch(err => alert('Error loading snippet code: ' + err.message));
}

// Reset form to "Add" mode
function cancelEdit() {
    clearForm();
    document.getElementById('editId').value = '';
    document.getElementById('formTitle').textContent = 'Add New Snippet';
    document.getElementById('cancelBtn').style.display = 'none';
}

// Clear all form inputs
function clearForm() {
    document.getElementById('title').value = '';
    document.getElementById('language').value = '';
    document.getElementById('description').value = '';
    document.getElementById('code').value = '';
}

// Prevent XSS when rendering user content
function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
