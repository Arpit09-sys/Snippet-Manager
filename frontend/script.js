const API_URL = 'http://localhost:8080/api/snippets';

// Load all snippets when page loads
window.onload = function () {
    loadSnippets();
};

// Fetch and display all snippets
function loadSnippets() {
    fetch(API_URL)
        .then(response => response.json())
        .then(snippets => displaySnippets(snippets))
        .catch(err => alert('Error loading snippets: ' + err.message));
}

// Search snippets by language and/or title
function searchSnippets() {
    const title = document.getElementById('searchTitle').value.trim();
    const language = document.getElementById('searchLanguage').value.trim();

    let url = API_URL + '/search?';
    if (title) url += 'title=' + encodeURIComponent(title) + '&';
    if (language) url += 'language=' + encodeURIComponent(language);

    fetch(url)
        .then(response => response.json())
        .then(snippets => displaySnippets(snippets))
        .catch(err => alert('Error searching: ' + err.message));
}

// Render snippet cards
function displaySnippets(snippets) {
    const list = document.getElementById('snippetList');

    if (snippets.length === 0) {
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
                <button class="btn-copy" onclick="copyCode('${snippet.id}')">Copy</button>
                <button class="btn-edit" onclick="editSnippet('${snippet.id}')">Edit</button>
                <button class="btn-delete" onclick="deleteSnippet('${snippet.id}')">Delete</button>
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
        fetch(API_URL + '/' + id, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(snippet)
        })
        .then(response => {
            if (!response.ok) throw new Error('Snippet not found');
            return response.json();
        })
        .then(() => {
            cancelEdit();
            loadSnippets();
        })
        .catch(err => alert('Error updating: ' + err.message));
    } else {
        // Create new snippet
        fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(snippet)
        })
        .then(response => response.json())
        .then(() => {
            clearForm();
            loadSnippets();
        })
        .catch(err => alert('Error saving: ' + err.message));
    }
}

// Load snippet data into form for editing
function editSnippet(id) {
    fetch(API_URL + '/' + id)
        .then(response => {
            if (!response.ok) throw new Error('Snippet not found');
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
            window.scrollTo(0, 0);
        })
        .catch(err => alert('Error loading snippet: ' + err.message));
}

// Delete a snippet
function deleteSnippet(id) {
    if (!confirm('Are you sure you want to delete this snippet?')) return;

    fetch(API_URL + '/' + id, { method: 'DELETE' })
        .then(response => {
            if (!response.ok) throw new Error('Snippet not found');
            loadSnippets();
        })
        .catch(err => alert('Error deleting: ' + err.message));
}

// Copy code to clipboard
function copyCode(id) {
    fetch(API_URL + '/' + id)
        .then(response => response.json())
        .then(snippet => {
            navigator.clipboard.writeText(snippet.code)
                .then(() => alert('Code copied to clipboard!'))
                .catch(() => alert('Copy failed. Please copy manually.'));
        });
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
        .replace(/"/g, '&quot;');
}
