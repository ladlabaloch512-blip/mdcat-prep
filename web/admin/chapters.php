<?php
declare(strict_types=1);
require_once __DIR__ . '/_common.php';
require_admin();
handle_delete('chapters', 'chapters.php');

$subjects = db()->query('SELECT id, name FROM subjects ORDER BY sort_order, id')->fetchAll();
$edit = null;
if (isset($_GET['edit'])) {
    $stmt = db()->prepare('SELECT * FROM chapters WHERE id = ?');
    $stmt->execute([(int)$_GET['edit']]);
    $edit = $stmt->fetch() ?: null;
}
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST' && isset($_POST['save'])) {
    csrf_check();
    $data = [
        (int)$_POST['subject_id'], trim((string)$_POST['title']),
        trim((string)($_POST['subtopics'] ?? '')), (int)($_POST['sort_order'] ?? 0),
        isset($_POST['is_active']) ? 1 : 0,
    ];
    if (!empty($_POST['id'])) {
        $data[] = (int)$_POST['id'];
        db()->prepare('UPDATE chapters SET subject_id=?, title=?, subtopics=?, sort_order=?, is_active=? WHERE id=?')
            ->execute($data);
        flash('Chapter updated.');
    } else {
        db()->prepare('INSERT INTO chapters (subject_id, title, subtopics, sort_order, is_active) VALUES (?,?,?,?,?)')
            ->execute($data);
        flash('Chapter added.');
    }
    header('Location: chapters.php');
    exit;
}
$filterSubject = (int)($_GET['subject_id'] ?? 0);

admin_head($edit ? 'Edit Chapter' : 'Chapters');
?>
<form method="post" class="form-card">
    <input type="hidden" name="csrf" value="<?= csrf_token() ?>">
    <input type="hidden" name="id" value="<?= (int)($edit['id'] ?? 0) ?>">
    <label>Subject
        <select name="subject_id" required>
            <?php foreach ($subjects as $s): ?>
            <option value="<?= (int)$s['id'] ?>" <?= (int)($edit['subject_id'] ?? $filterSubject) === (int)$s['id'] ? 'selected' : '' ?>>
                <?= e($s['name']) ?></option>
            <?php endforeach; ?>
        </select></label>
    <label>Title <input name="title" required value="<?= e($edit['title'] ?? '') ?>"></label>
    <label>Subtopics (comma separated) <input name="subtopics" value="<?= e($edit['subtopics'] ?? '') ?>"></label>
    <label>Sort order (PMDC syllabus order) <input type="number" name="sort_order" value="<?= (int)($edit['sort_order'] ?? 0) ?>"></label>
    <label class="check"><input type="checkbox" name="is_active" <?= ($edit['is_active'] ?? 1) ? 'checked' : '' ?>> Active</label>
    <button class="btn primary" name="save" type="submit">Save chapter</button>
    <?php if ($edit): ?><a class="btn" href="chapters.php">Cancel edit</a><?php endif; ?>
</form>

<form method="get" class="filter-row">
    <label>Filter by subject
        <select name="subject_id" onchange="this.form.submit()">
            <option value="0">All subjects</option>
            <?php foreach ($subjects as $s): ?>
            <option value="<?= (int)$s['id'] ?>" <?= $filterSubject === (int)$s['id'] ? 'selected' : '' ?>><?= e($s['name']) ?></option>
            <?php endforeach; ?>
        </select></label>
</form>

<table>
<tr><th>Subject</th><th>Chapter</th><th>Subtopics</th><th>Questions</th><th>Order</th><th>Active</th><th></th></tr>
<?php
$sql = 'SELECT c.*, s.name AS subject_name,
    (SELECT COUNT(*) FROM questions q WHERE q.chapter_id = c.id) AS qs
    FROM chapters c JOIN subjects s ON s.id = c.subject_id';
$params = [];
if ($filterSubject > 0) {
    $sql .= ' WHERE c.subject_id = ?';
    $params[] = $filterSubject;
}
$sql .= ' ORDER BY s.sort_order, c.sort_order, c.id';
$stmt = db()->prepare($sql);
$stmt->execute($params);
foreach ($stmt as $c):
?>
<tr>
    <td><?= e($c['subject_name']) ?></td><td><?= e($c['title']) ?></td>
    <td class="muted"><?= e($c['subtopics']) ?></td><td><?= (int)$c['qs'] ?></td>
    <td><?= (int)$c['sort_order'] ?></td><td><?= $c['is_active'] ? 'Yes' : 'No' ?></td>
    <td class="actions"><a class="btn small" href="chapters.php?edit=<?= (int)$c['id'] ?>">Edit</a>
    <?= delete_button((int)$c['id']) ?></td>
</tr>
<?php endforeach; ?>
</table>
<?php admin_foot(); ?>
