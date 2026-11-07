<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Notifications');
?>
<section>
    <div class="section-head"><h1>Notifications</h1></div>
    <div id="notifList"><p class="muted">Loading notifications...</p></div>
</section>
<script>
document.addEventListener('DOMContentLoaded', async function () {
    markNav('home');
    try {
        const list = await Api.get('notifications');
        const el = document.getElementById('notifList');
        if (!list.length) { el.innerHTML = '<p class="muted">No notifications yet.</p>'; return; }
        el.innerHTML = list.map(function (n) {
            return '<div class="notif"><strong>' + esc(n.title) + '</strong>' +
                (n.body ? '<p>' + esc(n.body) + '</p>' : '') +
                '<small class="muted">' + esc(n.created_at) + '</small></div>';
        }).join('');
    } catch (err) {
        document.getElementById('notifList').innerHTML = '<div class="error">' + esc(err.message) + '</div>';
    }
});
</script>
<?php web_foot(); ?>
