<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Sign In');
?>
<section class="auth-card">
    <img class="logo-img auth-logo" src="assets/logo.png" alt="">
    <h1>Welcome back</h1>
    <p class="muted"><?= e(setting('tagline')) ?></p>
    <div id="msg"></div>
    <form id="loginForm">
        <label>Email <input type="email" id="email" required></label>
        <label>Password <input type="password" id="password" required></label>
        <button class="btn primary block" type="submit">Sign In</button>
    </form>
    <button class="btn google block" id="googleBtn" type="button">Continue with Google</button>
    <p class="muted center"><a href="reset.php">Forgot your password?</a></p>
    <p class="muted center">New here? <a href="register.php">Create an account</a></p>
    <p class="muted center"><a href="index.php">Skip for Now (browse as guest)</a></p>
</section>
<script>
document.addEventListener('DOMContentLoaded', function () {
    markNav('profile');
    const next = new URLSearchParams(location.search).get('next') || 'index.php';
    document.getElementById('loginForm').addEventListener('submit', async function (ev) {
        ev.preventDefault();
        try {
            const d = await Api.post('auth/login', {
                email: document.getElementById('email').value.trim(),
                password: document.getElementById('password').value
            });
            Api.setSession(d.token, d.user);
            location.href = next;
        } catch (err) { errorBox(document.getElementById('msg'), err); }
    });
    document.getElementById('googleBtn').addEventListener('click', async function () {
        try {
            const s = await Api.get('settings');
            if (!s.google_client_id) {
                errorBox(document.getElementById('msg'),
                    new Error('Google sign-in is not configured yet. Please use email sign-in.'));
                return;
            }
            errorBox(document.getElementById('msg'),
                new Error('Google sign-in opens from the Android app, or add the Google web flow here once the client ID is registered for this domain.'));
        } catch (err) { errorBox(document.getElementById('msg'), err); }
    });
});
</script>
<?php web_foot(); ?>
