<?php
declare(strict_types=1);
require_once __DIR__ . '/_common.php';
require_admin();
handle_delete('notifications', 'notifications.php');

if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST' && isset($_POST['save'])) {
    csrf_check();
    db()->prepare('INSERT INTO notifications (title, body, is_active) VALUES (?, ?, ?)')
        ->execute([trim((string)$_POST['title']), trim((string)($_POST['body'] ?? '')), isset($_POST['is_active']) ? 1 : 0]);
    flash('Notification posted. It now shows in the app and on the website.');
    header('Location: notifications.php');
    exit;
}
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST' && isset($_POST['toggle_id'])) {
    csrf_check();
    db()->prepare('UPDATE notifications SET is_active = 1 - is_active WHERE id = ?')
        ->execute([(int)$_POST['toggle_id']]);
    header('Location: notifications.php');
    exit;
}

admin_head('Notifications');
?>
<p class="muted">Notifications are an in-app display list. Posting here shows the message
in the app and on the website immediately.</p>
<form method="post" class="form-card">
    <input type="hidden" name="csrf" value="<?= csrf_token() ?>">
    <label>Title <input name="title" required></label>
    <label>Message <textarea name="body" rows="3"></textarea></label>
    <label class="check"><input type="checkbox" name="is_active" checked> Active</label>
    <button class="btn primary" name="save" type="submit">Post notification</button>
</form>

<table>
<tr><th>Title</th><th>Message</th><th>Active</th><th>Date</th><th></th></tr>
<?php foreach (db()->query('SELECT * FROM notifications ORDER BY id DESC') as $n): ?>
<tr>
    <td><?= e($n['title']) ?></td><td class="muted"><?= e(mb_strimwidth((string)$n['body'], 0, 80, '...')) ?></td>
    <td><?= $n['is_active'] ? 'Yes' : 'No' ?></td><td><?= e($n['created_at']) ?></td>
    <td class="actions">
        <form method="post" class="inline">
            <input type="hidden" name="csrf" value="<?= csrf_token() ?>">
            <input type="hidden" name="toggle_id" value="<?= (int)$n['id'] ?>">
            <button class="btn small" type="submit"><?= $n['is_active'] ? 'Hide' : 'Show' ?></button>
        </form>
        <?= delete_button((int)$n['id']) ?>
    </td>
</tr>
<?php endforeach; ?>
</table>
<?php admin_foot(); ?>
