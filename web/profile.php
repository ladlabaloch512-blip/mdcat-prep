<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Profile');
?>
<section class="profile-wrap">
    <div class="profile-card">
        <div class="avatar" id="avatar">?</div>
        <div>
            <h1 id="pName">Guest</h1>
            <p class="muted" id="pMeta">Browsing as guest. Sign in to save progress, results and MCQs.</p>
        </div>
    </div>
    <div id="guestBox">
        <a class="btn primary" href="login.php">Sign In</a>
        <a class="btn" href="register.php">Create Account</a>
    </div>
    <div id="userBox" style="display:none">
        <h2>Daily Study Goal</h2>
        <div class="review-item" id="goalBox"><p class="muted">Loading your goal...</p></div>
        <h2>My Results</h2>
        <div id="attempts"><p class="muted">Loading results...</p></div>
        <h2>Edit Profile</h2>
        <div class="review-item">
            <label>Name <input type="text" id="editName"></label>
            <label>Phone <input type="tel" id="editPhone"></label>
            <p class="muted" id="editMsg"></p>
            <button class="btn primary" id="editSave" type="button">Save changes</button>
        </div>
        <h2>Change Password</h2>
        <div class="review-item">
            <label>Current password <input type="password" id="pwCurrent"></label>
            <label>New password (at least 6 characters) <input type="password" id="pwNew"></label>
            <p class="muted" id="pwMsg"></p>
            <button class="btn" id="pwSave" type="button">Change password</button>
        </div>
        <h2>Delete Account</h2>
        <div class="review-item">
            <p class="muted">Deleting your account removes your profile, results, saved MCQs and coins. This cannot be undone.</p>
            <label>Your password <input type="password" id="delPassword"></label>
            <p class="muted" id="delMsg"></p>
            <button class="btn danger" id="delBtn" type="button">Delete my account</button>
        </div>
        <button class="btn danger" id="logoutBtn" type="button">Log Out</button>
        <p class="muted">MDCAT Prep, version 1.0</p>
    </div>

    <h2>Question of the Day</h2>
    <div class="review-item" id="qotdBox"><p class="muted">Loading today's question...</p></div>

    <h2>Choose your design</h2>
    <p class="muted">The same 10 designs as the app. Your choice is saved on this device; the admin sets the default for new visitors.</p>
    <div class="design-grid">
        <?php foreach (DESIGN_NAMES as $n => $name): ?>
        <button class="design-pick" data-design="<?= $n ?>" type="button">
            <span class="d-num"><?= $n ?></span><span><?= e($name) ?></span>
        </button>
        <?php endforeach; ?>
    </div>

    <h2>Colours for this design</h2>
    <p class="muted">Every design has its own colour set, saved per design on this device, the same as in the app.</p>
    <div class="palette-row" id="paletteRow"></div>

    <h2>Contact</h2>
    <p class="muted">Support email: <a href="mailto:<?= e(setting('support_email')) ?>"><?= e(setting('support_email')) ?></a></p>
    <p class="muted socials">
        <?php if (setting('social_facebook')): ?><a href="<?= e(setting('social_facebook')) ?>" target="_blank" rel="noopener">Facebook</a><?php endif; ?>
        <?php if (setting('social_youtube')): ?><a href="<?= e(setting('social_youtube')) ?>" target="_blank" rel="noopener">YouTube</a><?php endif; ?>
        <?php if (setting('social_instagram')): ?><a href="<?= e(setting('social_instagram')) ?>" target="_blank" rel="noopener">Instagram</a><?php endif; ?>
    </p>
</section>
<script>
document.addEventListener('DOMContentLoaded', async function () {
    markNav('profile');
    const current = document.documentElement.dataset.design;
    document.querySelectorAll('.design-pick').forEach(function (b) {
        if (b.dataset.design === current) b.classList.add('active');
        b.addEventListener('click', function () {
            applyDesign(b.dataset.design);
            document.querySelectorAll('.design-pick').forEach(x => x.classList.remove('active'));
            b.classList.add('active');
        });
    });
    // Colour palettes: one set per design, remembered per design.
    const PALETTES = {"1": [["Original", "#0a8a4d", "#0b5ed7"], ["Ocean", "#0b5ed7", "#00a3a3"], ["Royal", "#6c3ce9", "#0b5ed7"], ["Berry", "#c2255c", "#862e9c"]], "2": [["Original", "#12994f", "#111111"], ["Forest", "#14532d", "#b45309"], ["Navy", "#14213d", "#d97706"], ["Plum", "#581c87", "#db2777"]], "3": [["Original", "#0a8a4d", "#0a8a4d"], ["Slate", "#334155", "#0284c7"], ["Maroon", "#7f1d1d", "#b45309"], ["Teal", "#134e4a", "#0f766e"]], "4": [["Original", "#35d99a", "#35d99a"], ["Ice", "#7dd3fc", "#a78bfa"], ["Amber", "#fbbf24", "#fb923c"], ["Rose", "#fda4af", "#f472b6"]], "5": [["Original", "#0a9e52", "#123524"], ["Crimson", "#c92a2a", "#1864ab"], ["Indigo", "#3b5bdb", "#f76707"], ["Violet", "#7048e8", "#0ca678"]], "6": [["Original", "#0a7a43", "#0a7a43"], ["Ink Blue", "#1a1a1a", "#1864ab"], ["Rust", "#7c2d12", "#b45309"], ["Olive", "#3f6212", "#a16207"]], "7": [["Original", "#f2709c", "#0a8a4d"], ["Lavender", "#7c6bd9", "#e56b9f"], ["Peach", "#e8834e", "#3e9b8f"], ["Mint", "#2f9e77", "#e8834e"]], "8": [["Original", "#0b6e39", "#0b6e39"], ["Midnight", "#0b2545", "#2a9d8f"], ["Burgundy", "#5c1a24", "#b45309"], ["Slate Teal", "#263238", "#0e9594"]], "9": [["Original", "#0a8a4d", "#111111"], ["Red", "#d62828", "#111111"], ["Blue", "#1864ab", "#f08c00"], ["Purple", "#5f0f40", "#111111"]], "10": [["Original", "#0e9594", "#0e9594"], ["Ocean", "#0b5ed7", "#00a3a3"], ["Grape", "#7048e8", "#0ca678"], ["Coral", "#e8590c", "#0e9594"]]};
    const paletteRow = document.getElementById('paletteRow');
    function renderPalettes() {
        const n = document.documentElement.dataset.design || '1';
        const active = localStorage.getItem('mdcat_palette_' + n) || '0';
        paletteRow.innerHTML = '';
        (PALETTES[n] || []).forEach(function (pal, i) {
            const b = document.createElement('button');
            b.type = 'button';
            b.className = 'palette-dot' + (String(i) === String(active) ? ' active' : '');
            b.innerHTML = '<span class="dot" style="background:' + pal[1] + ';border-color:' + (String(i) === String(active) ? '' : pal[2]) + '"></span><small>' + pal[0] + '</small>';
            b.addEventListener('click', function () {
                applyPalette(i);
                renderPalettes();
            });
            paletteRow.appendChild(b);
        });
    }
    window.onDesignChanged = renderPalettes;
    renderPalettes();

    // Question of the Day (works for guests too).
    try {
        const qd = await Api.get('qotd');
        const q = qd.question;
        const box = document.getElementById('qotdBox');
        let answered = false;
        box.innerHTML = (q.chapter_title ? '<p class="muted">' + esc(q.chapter_title) + '</p>' : '') +
            '<p><strong>' + esc(q.question) + '</strong></p>' +
            ['A', 'B', 'C', 'D'].filter(l => q.options[l]).map(function (l) {
                return '<button class="qotd-opt" data-letter="' + l + '" type="button"><strong>' + l + '</strong> ' + esc(q.options[l]) + '</button>';
            }).join('') +
            '<p class="expl" id="qotdExpl" style="display:none"></p>';
        box.querySelectorAll('.qotd-opt').forEach(function (b) {
            b.addEventListener('click', function () {
                if (answered) return;
                answered = true;
                box.querySelectorAll('.qotd-opt').forEach(function (x) {
                    if (x.dataset.letter === q.correct_option) x.classList.add('right');
                    else if (x === b) x.classList.add('wrong');
                    x.disabled = true;
                });
                if (q.explanation) {
                    const ex = document.getElementById('qotdExpl');
                    ex.innerHTML = '<strong>Explanation:</strong> ' + esc(q.explanation);
                    ex.style.display = '';
                }
            });
        });
    } catch (err) {
        document.getElementById('qotdBox').innerHTML = '<p class="muted">Question of the Day is not available right now.</p>';
    }
    const u = Api.user();
    if (!u) return;
    // Daily Study Goal card.
    try {
        const home = await Api.get('home');
        let goal = home.daily_goal;
        const done = home.daily_completed;
        const pct = goal > 0 ? Math.min(100, Math.round(done * 100 / goal)) : 0;
        const gb = document.getElementById('goalBox');
        gb.innerHTML = '<p><strong>' + done + ' of <span id="goalVal">' + goal + '</span> questions done today (' + pct + '%)</strong></p>' +
            '<span class="bar"><span style="width:' + pct + '%"></span></span>' +
            '<div class="goal-edit"><button class="chip-btn" id="goalMinus" type="button">-10</button>' +
            '<button class="chip-btn" id="goalPlus" type="button">+10</button>' +
            '<button class="btn primary" id="goalSave" type="button">Save goal</button></div>';
        document.getElementById('goalMinus').addEventListener('click', function () {
            goal = Math.max(5, goal - 10);
            document.getElementById('goalVal').textContent = goal;
        });
        document.getElementById('goalPlus').addEventListener('click', function () {
            goal = Math.min(500, goal + 10);
            document.getElementById('goalVal').textContent = goal;
        });
        document.getElementById('goalSave').addEventListener('click', async function () {
            try {
                await Api.post('profile/goal', { goal: goal });
                location.reload();
            } catch (e) { }
        });
    } catch (err) { errorBox(document.getElementById('goalBox'), err); }
    document.getElementById('pName').textContent = u.name;
    document.getElementById('pMeta').textContent = u.email + (u.phone ? ' - ' + u.phone : '');
    document.getElementById('avatar').textContent = u.name.charAt(0).toUpperCase();
    document.getElementById('guestBox').style.display = 'none';
    document.getElementById('userBox').style.display = '';
    try {
        const attempts = await Api.get('attempts');
        const el = document.getElementById('attempts');
        if (!attempts.length) { el.innerHTML = '<p class="muted">No attempts yet. Take your first quiz.</p>'; return; }
        el.innerHTML = attempts.map(function (a) {
            return '<div class="attempt-row"><span><strong>' +
                (a.quiz_type === 'mock' ? 'Full Mock Test' : esc(a.chapter_title || 'Chapter Quiz')) +
                '</strong><small>' + esc(a.created_at) + '</small></span>' +
                '<span class="a-score">' + a.score + ' / ' + a.total + '</span></div>';
        }).join('');
    } catch (err) { errorBox(document.getElementById('attempts'), err); }
    document.getElementById('editName').value = u.name || '';
    document.getElementById('editPhone').value = u.phone || '';
    document.getElementById('editSave').addEventListener('click', async function () {
        try {
            const updated = await Api.post('profile/update', {
                name: document.getElementById('editName').value.trim(),
                phone: document.getElementById('editPhone').value.trim()
            });
            Api.setSession(Api.token(), updated);
            document.getElementById('pName').textContent = updated.name;
            document.getElementById('pMeta').textContent = updated.email + (updated.phone ? ' - ' + updated.phone : '');
            document.getElementById('editMsg').textContent = 'Saved.';
        } catch (e) { errorBox(document.getElementById('editMsg'), e); }
    });
    document.getElementById('pwSave').addEventListener('click', async function () {
        try {
            await Api.post('profile/password', {
                current_password: document.getElementById('pwCurrent').value,
                new_password: document.getElementById('pwNew').value
            });
            document.getElementById('pwCurrent').value = '';
            document.getElementById('pwNew').value = '';
            document.getElementById('pwMsg').textContent = 'Password changed.';
        } catch (e) { errorBox(document.getElementById('pwMsg'), e); }
    });
    document.getElementById('delBtn').addEventListener('click', async function () {
        if (!confirm('Delete your account permanently? This cannot be undone.')) return;
        try {
            await Api.post('auth/delete', { password: document.getElementById('delPassword').value });
            Api.clearSession();
            location.href = 'index.php';
        } catch (e) { errorBox(document.getElementById('delMsg'), e); }
    });
    document.getElementById('logoutBtn').addEventListener('click', async function () {
        try { await Api.post('auth/logout'); } catch (e) { }
        Api.clearSession();
        location.href = 'index.php';
    });
});
</script>
<?php web_foot(); ?>
