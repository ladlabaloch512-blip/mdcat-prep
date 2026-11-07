<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Reset Password');
?>
<section class="auth-card">
    <img class="logo-img auth-logo" src="assets/logo.png" alt="">
    <h1>Reset password</h1>
    <p class="muted">Enter your account email and we will send you a 6-digit reset code. The code stays valid for 15 minutes.</p>
    <div id="msg"></div>
    <form id="forgotForm">
        <label>Email <input type="email" id="email" required></label>
        <button class="btn block" type="submit">Send reset code</button>
    </form>
    <form id="resetForm" style="margin-top:18px">
        <label>Reset code <input type="text" id="code" inputmode="numeric" autocomplete="one-time-code" required></label>
        <label>New password (at least 6 characters) <input type="password" id="password" required></label>
        <button class="btn primary block" type="submit">Set new password</button>
    </form>
    <p class="muted center">Remembered it? <a href="login.php">Sign in</a></p>
</section>
<script>
document.addEventListener('DOMContentLoaded', function () {
    markNav('profile');
    const msg = document.getElementById('msg');
    document.getElementById('forgotForm').addEventListener('submit', async function (ev) {
        ev.preventDefault();
        try {
            const d = await Api.post('auth/forgot', { email: document.getElementById('email').value.trim() });
            msg.innerHTML = '<p class="notice">' + esc(d.message) + '</p>';
        } catch (err) { errorBox(msg, err); }
    });
    document.getElementById('resetForm').addEventListener('submit', async function (ev) {
        ev.preventDefault();
        try {
            await Api.post('auth/reset', {
                email: document.getElementById('email').value.trim(),
                code: document.getElementById('code').value.trim(),
                password: document.getElementById('password').value
            });
            msg.innerHTML = '<p class="notice">Your password has been reset. Please sign in with the new password.</p>';
            setTimeout(function () { location.href = 'login.php'; }, 1200);
        } catch (err) { errorBox(msg, err); }
    });
});
</script>
<?php web_foot(); ?>
