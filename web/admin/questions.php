<?php
declare(strict_types=1);
require_once __DIR__ . '/_common.php';
require_admin();
handle_delete('questions', 'questions.php');

$chapters = db()->query('SELECT c.id, c.title, s.name AS subject_name FROM chapters c
    JOIN subjects s ON s.id = c.subject_id ORDER BY s.sort_order, c.sort_order, c.id')->fetchAll();
$edit = null;
if (isset($_GET['edit'])) {
    $stmt = db()->prepare('SELECT * FROM questions WHERE id = ?');
    $stmt->execute([(int)$_GET['edit']]);
    $edit = $stmt->fetch() ?: null;
}
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST' && isset($_POST['save'])) {
    csrf_check();
    $correct = strtoupper((string)($_POST['correct_option'] ?? 'A'));
    if (!in_array($correct, ['A', 'B', 'C', 'D'], true)) {
        $correct = 'A';
    }
    $data = [
        (int)$_POST['chapter_id'], trim((string)($_POST['subtopic'] ?? '')),
        trim((string)$_POST['question_text']),
        trim((string)$_POST['option_a']), trim((string)$_POST['option_b']),
        trim((string)$_POST['option_c']), trim((string)$_POST['option_d']),
        $correct, trim((string)($_POST['explanation'] ?? '')),
        isset($_POST['is_active']) ? 1 : 0,
    ];
    if (!empty($_POST['id'])) {
        $data[] = (int)$_POST['id'];
        db()->prepare('UPDATE questions SET chapter_id=?, subtopic=?, question_text=?, option_a=?, option_b=?,
            option_c=?, option_d=?, correct_option=?, explanation=?, is_active=? WHERE id=?')->execute($data);
        flash('Question updated.');
    } else {
        db()->prepare('INSERT INTO questions (chapter_id, subtopic, question_text, option_a, option_b,
            option_c, option_d, correct_option, explanation, is_active) VALUES (?,?,?,?,?,?,?,?,?,?)')->execute($data);
        flash('Question added.');
    }
    header('Location: questions.php?chapter_id=' . (int)$_POST['chapter_id']);
    exit;
}
$filterChapter = (int)($_GET['chapter_id'] ?? ($edit['chapter_id'] ?? 0));

admin_head($edit ? 'Edit Question' : 'Questions (MCQs)');
?>
<form method="post" class="form-card wide">
    <input type="hidden" name="csrf" value="<?= csrf_token() ?>">
    <input type="hidden" name="id" value="<?= (int)($edit['id'] ?? 0) ?>">
    <label>Chapter
        <select name="chapter_id" required>
            <?php foreach ($chapters as $c): ?>
            <option value="<?= (int)$c['id'] ?>" <?= (int)($edit['chapter_id'] ?? $filterChapter) === (int)$c['id'] ? 'selected' : '' ?>>
                <?= e($c['subject_name']) ?> - <?= e($c['title']) ?></option>
            <?php endforeach; ?>
        </select></label>
    <label>Subtopic <input name="subtopic" value="<?= e($edit['subtopic'] ?? '') ?>"></label>
    <label>Question <textarea name="question_text" required rows="2"><?= e($edit['question_text'] ?? '') ?></textarea></label>
    <div class="grid2">
        <label>Option A <input name="option_a" required value="<?= e($edit['option_a'] ?? '') ?>"></label>
        <label>Option B <input name="option_b" required value="<?= e($edit['option_b'] ?? '') ?>"></label>
        <label>Option C <input name="option_c" required value="<?= e($edit['option_c'] ?? '') ?>"></label>
        <label>Option D <input name="option_d" required value="<?= e($edit['option_d'] ?? '') ?>"></label>
    </div>
    <label>Correct option
        <select name="correct_option">
            <?php foreach (['A', 'B', 'C', 'D'] as $o): ?>
            <option value="<?= $o ?>" <?= ($edit['correct_option'] ?? 'A') === $o ? 'selected' : '' ?>><?= $o ?></option>
            <?php endforeach; ?>
        </select></label>
    <label>Explanation <textarea name="explanation" rows="2"><?= e($edit['explanation'] ?? '') ?></textarea></label>
    <label class="check"><input type="checkbox" name="is_active" <?= ($edit['is_active'] ?? 1) ? 'checked' : '' ?>> Active</label>
    <button class="btn primary" name="save" type="submit">Save question</button>
    <?php if ($edit): ?><a class="btn" href="questions.php">Cancel edit</a><?php endif; ?>
</form>

<form method="get" class="filter-row">
    <label>Filter by chapter
        <select name="chapter_id" onchange="this.form.submit()">
            <option value="0">All chapters</option>
            <?php foreach ($chapters as $c): ?>
            <option value="<?= (int)$c['id'] ?>" <?= $filterChapter === (int)$c['id'] ? 'selected' : '' ?>>
                <?= e($c['subject_name']) ?> - <?= e($c['title']) ?></option>
            <?php endforeach; ?>
        </select></label>
</form>

<table>
<tr><th>Chapter</th><th>Question</th><th>Correct</th><th>Active</th><th></th></tr>
<?php
$sql = 'SELECT q.*, c.title AS chapter_title FROM questions q JOIN chapters c ON c.id = q.chapter_id';
$params = [];
if ($filterChapter > 0) {
    $sql .= ' WHERE q.chapter_id = ?';
    $params[] = $filterChapter;
}
$sql .= ' ORDER BY q.chapter_id, q.id LIMIT 300';
$stmt = db()->prepare($sql);
$stmt->execute($params);
foreach ($stmt as $q):
?>
<tr>
    <td><?= e($q['chapter_title']) ?></td>
    <td><?= e(mb_strimwidth((string)$q['question_text'], 0, 90, '...')) ?></td>
    <td><?= e($q['correct_option']) ?></td><td><?= $q['is_active'] ? 'Yes' : 'No' ?></td>
    <td class="actions"><a class="btn small" href="questions.php?edit=<?= (int)$q['id'] ?>">Edit</a>
    <?= delete_button((int)$q['id']) ?></td>
</tr>
<?php endforeach; ?>
</table>
<?php admin_foot(); ?>
