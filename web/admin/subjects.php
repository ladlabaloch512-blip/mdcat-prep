<?php
declare(strict_types=1);
require_once __DIR__ . '/_common.php';
require_admin();
handle_delete('subjects', 'subjects.php');

$edit = null;
if (isset($_GET['edit'])) {
    $stmt = db()->prepare('SELECT * FROM subjects WHERE id = ?');
    $stmt->execute([(int)$_GET['edit']]);
    $edit = $stmt->fetch() ?: null;
}
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST' && isset($_POST['save'])) {
    csrf_check();
    $data = [
        trim((string)$_POST['name']), trim((string)($_POST['tagline'] ?? '')),
        trim((string)($_POST['icon'] ?? 'book')), trim((string)($_POST['color'] ?? '')),
        (int)($_POST['sort_order'] ?? 0), isset($_POST['is_active']) ? 1 : 0,
    ];
    if (!empty($_POST['id'])) {
        $data[] = (int)$_POST['id'];
        db()->prepare('UPDATE subjects SET name=?, tagline=?, icon=?, color=?, sort_order=?, is_active=? WHERE id=?')
            ->execute($data);
        flash('Subject updated.');
    } else {
        db()->prepare('INSERT INTO subjects (name, tagline, icon, color, sort_order, is_active) VALUES (?,?,?,?,?,?)')
            ->execute($data);
        flash('Subject added.');
    }
    header('Location: subjects.php');
    exit;
}

admin_head($edit ? 'Edit Subject' : 'Subjects');
?>
<form method="post" class="form-card">
    <input type="hidden" name="csrf" value="<?= csrf_token() ?>">
    <input type="hidden" name="id" value="<?= (int)($edit['id'] ?? 0) ?>">
    <label>Name <input name="name" required value="<?= e($edit['name'] ?? '') ?>"></label>
    <label>Tagline <input name="tagline" value="<?= e($edit['tagline'] ?? '') ?>"></label>
    <label>Icon (leaf, flask, atom, book)
        <input name="icon" value="<?= e($edit['icon'] ?? 'book') ?>"></label>
    <label>Colour (hex) <input name="color" value="<?= e($edit['color'] ?? '') ?>" placeholder="#22a06b"></label>
    <label>Sort order <input type="number" name="sort_order" value="<?= (int)($edit['sort_order'] ?? 0) ?>"></label>
    <label class="check"><input type="checkbox" name="is_active" <?= ($edit['is_active'] ?? 1) ? 'checked' : '' ?>> Active</label>
    <button class="btn primary" name="save" type="submit">Save subject</button>
    <?php if ($edit): ?><a class="btn" href="subjects.php">Cancel edit</a><?php endif; ?>
</form>

<table>
<tr><th>Name</th><th>Tagline</th><th>Chapters</th><th>Questions</th><th>Order</th><th>Active</th><th></th></tr>
<?php
$stmt = db()->query('SELECT s.*,
    (SELECT COUNT(*) FROM chapters c WHERE c.subject_id = s.id) AS ch,
    (SELECT COUNT(*) FROM questions q JOIN chapters c ON c.id = q.chapter_id WHERE c.subject_id = s.id) AS qs
    FROM subjects s ORDER BY s.sort_order, s.id');
foreach ($stmt as $s):
?>
<tr>
    <td><?= e($s['name']) ?></td><td><?= e($s['tagline']) ?></td>
    <td><?= (int)$s['ch'] ?></td><td><?= (int)$s['qs'] ?></td>
    <td><?= (int)$s['sort_order'] ?></td><td><?= $s['is_active'] ? 'Yes' : 'No' ?></td>
    <td class="actions"><a class="btn small" href="subjects.php?edit=<?= (int)$s['id'] ?>">Edit</a>
    <?= delete_button((int)$s['id']) ?></td>
</tr>
<?php endforeach; ?>
</table>
<?php admin_foot(); ?>
