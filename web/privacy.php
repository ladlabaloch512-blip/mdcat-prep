<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Privacy Policy');
$app = e(setting('app_name'));
$email = e(setting('support_email'));
?>
<section class="doc">
    <h1>Privacy Policy</h1>
    <p class="muted">Last updated: 2026-11-06</p>
    <p>This Privacy Policy explains what information the <?= $app ?> app and website
    (together, "the Service") collect, why, and the choices you have. By using the
    Service you agree to this policy.</p>

    <h2>Information we collect</h2>
    <ul>
        <li><strong>Account information:</strong> your name, email address and phone number
        when you register, and a securely hashed password. If you use Google sign-in, we
        receive your name, email address and Google account identifier.</li>
        <li><strong>Learning activity:</strong> the quizzes you attempt, your answers, scores,
        saved MCQs and progress, so the Service can show your results, weak topics and
        saved questions on any device where you sign in.</li>
        <li><strong>Guest use:</strong> if you browse as a guest, no account is created and no
        progress or results are stored for you on our servers.</li>
    </ul>

    <h2>How we use information</h2>
    <ul>
        <li>To provide the Service: sign-in, quizzes, results, saved MCQs and notifications.</li>
        <li>To show your own progress to you, and aggregated results to administrators
        who manage the learning content.</li>
        <li>To respond to support requests sent to <?= $email ?>.</li>
    </ul>

    <h2>What we do not do</h2>
    <ul>
        <li>We do not sell your personal information.</li>
        <li>We do not use third-party advertising networks in this app. Any banners shown
        inside the Service are managed directly by the Service operator.</li>
        <li>We do not collect precise location, contacts, photos or files from your device.</li>
    </ul>

    <h2>Data storage and security</h2>
    <p>Data is stored on the Service's hosting servers. Passwords are stored only as
    cryptographic hashes. Sign-in sessions use tokens that expire, and you can sign out
    at any time from the Profile screen.</p>

    <h2>Your choices</h2>
    <p>You may ask us to correct or delete your account and associated learning data at
    any time by writing to <?= $email ?>. Deleting your account removes your profile,
    attempts and saved questions.</p>

    <h2>Children</h2>
    <p>The Service is an exam-preparation tool intended for students preparing for the
    MDCAT entry test. Students under 18 should use the Service with the consent of a
    parent or guardian.</p>

    <h2>Changes to this policy</h2>
    <p>If this policy changes, the updated version will be published on this page with a
    new date.</p>

    <h2>Contact</h2>
    <p>Questions about this policy: <a href="mailto:<?= $email ?>"><?= $email ?></a></p>
</section>
<?php web_foot(); ?>
