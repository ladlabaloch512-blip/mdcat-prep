// MDCAT Prep website API client. One place talks to the shared backend.
'use strict';

const Api = {
    base: 'api/index.php?r=',
    token() { return localStorage.getItem('mdcat_token') || ''; },
    user() {
        try { return JSON.parse(localStorage.getItem('mdcat_user') || 'null'); }
        catch (e) { return null; }
    },
    setSession(token, user) {
        localStorage.setItem('mdcat_token', token);
        localStorage.setItem('mdcat_user', JSON.stringify(user));
    },
    clearSession() {
        localStorage.removeItem('mdcat_token');
        localStorage.removeItem('mdcat_user');
    },
    async call(route, options) {
        options = options || {};
        const headers = { 'Content-Type': 'application/json' };
        const t = this.token();
        if (t) headers['Authorization'] = 'Bearer ' + t;
        const res = await fetch(this.base + route, {
            method: options.method || 'GET',
            headers: headers,
            body: options.body ? JSON.stringify(options.body) : undefined
        });
        let json = null;
        try { json = await res.json(); } catch (e) { /* non-JSON response */ }
        if (!json || json.ok !== true) {
            const msg = (json && json.error && json.error.message) || ('Request failed (' + res.status + ')');
            const err = new Error(msg);
            err.code = json && json.error ? json.error.code : 'http_' + res.status;
            err.status = res.status;
            throw err;
        }
        return json.data;
    },
    get(route) { return this.call(route); },
    post(route, body) { return this.call(route, { method: 'POST', body: body || {} }); }
};

function esc(s) {
    return String(s == null ? '' : s)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

/** Render a banner slot; collapses to nothing when no banner is active. */
function renderBanners(el, banners) {
    if (!el) return;
    if (!banners || !banners.length) { el.innerHTML = ''; el.style.display = 'none'; return; }
    el.style.display = '';
    el.innerHTML = banners.map(function (b) {
        const img = '<img src="' + esc(b.image_url) + '" alt="' + esc(b.title) + '">';
        return b.target_link
            ? '<a class="banner" href="' + esc(b.target_link) + '" target="_blank" rel="noopener">' + img + '</a>'
            : '<div class="banner">' + img + '</div>';
    }).join('');
}

/** Apply a design (1-10): persists per visitor and re-skins instantly. */
function applyDesign(n) {
    n = parseInt(n, 10);
    if (!(n >= 1 && n <= 10)) n = 1;
    localStorage.setItem('mdcat_design', String(n));
    document.documentElement.dataset.design = String(n);
    document.documentElement.dataset.palette = localStorage.getItem('mdcat_palette_' + n) || '0';
    if (window.onDesignChanged) window.onDesignChanged(n);
}

/** Apply a colour palette (0-3) for the current design; remembered per design. */
function applyPalette(p) {
    const n = document.documentElement.dataset.design || '1';
    localStorage.setItem('mdcat_palette_' + n, String(p));
    document.documentElement.dataset.palette = String(p);
}

function markNav(key) {
    document.querySelectorAll('.bottom-nav a, .topnav a').forEach(function (a) {
        if (a.dataset.nav === key) a.classList.add('active');
    });
    const who = document.getElementById('whoBox');
    if (who) {
        const u = Api.user();
        who.innerHTML = u
            ? '<span class="who-name">' + esc(u.name) + '</span>'
            : '<a class="btn small" href="login.php">Sign in</a>';
    }
}

function requireLogin() {
    if (!Api.token()) {
        location.href = 'login.php?next=' + encodeURIComponent(location.pathname.split('/').pop());
        return false;
    }
    return true;
}

function errorBox(el, err) {
    if (el) el.innerHTML = '<div class="error">' + esc(err.message || 'Something went wrong.') + '</div>';
}
