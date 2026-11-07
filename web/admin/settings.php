<?php
declare(strict_types=1);
require_once __DIR__ . '/_common.php';
require_admin();

if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
    csrf_check();
    foreach (array_keys(SETTING_DEFAULTS) as $key) {
        if (isset($_POST[$key])) {
            set_setting($key, trim((string)$_POST[$key]));
        }
    }
    flash('Settings saved. Changes apply on the app and website immediately.');
    header('Location: settings.php');
    exit;
}

$s = settings_all();
admin_head('Settings');
?>
<p class="muted">Every value here is read live by the app and the website. Changing a value
here never needs a code edit or an app update.</p>
<form method="post" class="form-card wide">
    <input type="hidden" name="csrf" value="<?= csrf_token() ?>">
    <div class="grid2">
        <label>App name <input name="app_name" value="<?= e($s['app_name']) ?>"></label>
        <label>Tagline <input name="tagline" value="<?= e($s['tagline']) ?>"></label>
        <label>Support email <input name="support_email" value="<?= e($s['support_email']) ?>"></label>
        <label>Default design (1 to 10) <input type="number" min="1" max="10" name="default_design" value="<?= e($s['default_design']) ?>"></label>
        <label>Daily goal (MCQs per day) <input type="number" min="1" name="daily_goal" value="<?= e($s['daily_goal']) ?>"></label>
        <label>Full mock test questions <input type="number" min="1" name="mock_question_count" value="<?= e($s['mock_question_count']) ?>"></label>
        <label>Timer: seconds per question <input type="number" min="5" name="timer_seconds_per_question" value="<?= e($s['timer_seconds_per_question']) ?>"></label>
        <label>Weak topic threshold (%) <input type="number" min="1" max="100" name="weak_threshold_percent" value="<?= e($s['weak_threshold_percent']) ?>"></label>
        <label>Weak topic minimum attempts <input type="number" min="1" name="weak_min_attempts" value="<?= e($s['weak_min_attempts']) ?>"></label>
        <label>Google client ID (web) <input name="google_client_id" value="<?= e($s['google_client_id']) ?>" placeholder="For Google sign-in"></label>
        <label>Facebook link <input name="social_facebook" value="<?= e($s['social_facebook']) ?>"></label>
        <label>YouTube link <input name="social_youtube" value="<?= e($s['social_youtube']) ?>"></label>
        <label>Instagram link <input name="social_instagram" value="<?= e($s['social_instagram']) ?>"></label>
        <label>Privacy policy page <input name="privacy_url" value="<?= e($s['privacy_url']) ?>"></label>
        <label>Daily Reward coins <input type="number" min="0" name="daily_reward_coins" value="<?= e($s['daily_reward_coins']) ?>"></label>
        <label>Daily Reward on (1) / off (0) <input type="number" min="0" max="1" name="rewards_enabled" value="<?= e($s['rewards_enabled']) ?>"></label>
        <label>WhatsApp Channel join link <input name="whatsapp_channel_url" value="<?= e($s['whatsapp_channel_url']) ?>" placeholder="https://whatsapp.com/channel/..."></label>
    </div>
    <button class="btn primary" type="submit">Save settings</button>
</form>
<?php admin_foot(); ?>
