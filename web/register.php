<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Create Account');
?>
<section class="auth-card">
    <img class="logo-img auth-logo" src="assets/logo.png" alt="">
    <h1>Create your account</h1>
    <p class="muted"><?= e(setting('tagline')) ?></p>
    <div id="msg"></div>
    <form id="regForm">
        <label>Full name <input type="text" id="name" required></label>
        <label>Email <input type="email" id="email" required></label>
        <label>Phone <input type="tel" id="phone"></label>
        <label>Password <input type="password" id="password" required minlength="6"></label>
        <button class="btn primary block" type="submit">Sign Up</button>
    </form>
    <p class="muted center">Already registered? <a href="login.php">Sign in</a></p>
    <p class="muted center"><a href="index.php">Skip for Now (browse as guest)</a></p>
</section>
<script>
document.addEventListener('DOMContentLoaded', function () {
    markNav('profile');
    document.getElementById('regForm').addEventListener('submit', async function (ev) {
        ev.preventDefault();
        try {
            const d = await Api.post('auth/register', {
                name: document.getElementById('name').value.trim(),
                email: document.getElementById('email').value.trim(),
                phone: document.getElementById('phone').value.trim(),
                password: document.getElementById('password').value
            });
            Api.setSession(d.token, d.user);
            location.href = 'index.php';
        } catch (err) { errorBox(document.getElementById('msg'), err); }
    });
});
</script>
<?php web_foot(); ?>
