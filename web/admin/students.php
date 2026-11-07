<?php
declare(strict_types=1);
require_once __DIR__ . '/_common.php';
require_admin();

admin_head('Students');
$selected = (int)($_GET['student'] ?? 0);
?>
<table>
<tr><th>Name</th><th>Email</th><th>Phone</th><th>Signed up</th><th>Attempts</th><th>MCQs done</th><th></th></tr>
<?php
$stmt = db()->query('SELECT u.*,
    (SELECT COUNT(*) FROM quiz_attempts a WHERE a.user_id = u.id) AS attempts,
    (SELECT COALESCE(SUM(a.total_questions),0) FROM quiz_attempts a WHERE a.user_id = u.id) AS mcqs
    FROM users u ORDER BY u.created_at DESC');
foreach ($stmt as $u):
?>
<tr>
    <td><?= e($u['name']) ?></td><td><?= e($u['email']) ?></td><td><?= e($u['phone']) ?></td>
    <td><?= e($u['created_at']) ?></td><td><?= (int)$u['attempts'] ?></td><td><?= (int)$u['mcqs'] ?></td>
    <td><a class="btn small" href="students.php?student=<?= (int)$u['id'] ?>">View results</a></td>
</tr>
<?php endforeach; ?>
</table>

<?php if ($selected > 0): ?>
<h2>Results</h2>
<table>
<tr><th>Type</th><th>Chapter</th><th>Score</th><th>Correct</th><th>Wrong</th><th>Skipped</th><th>Time</th><th>Date</th></tr>
<?php
$stmt = db()->prepare('SELECT a.*, c.title AS chapter_title FROM quiz_attempts a
    LEFT JOIN chapters c ON c.id = a.chapter_id WHERE a.user_id = ? ORDER BY a.created_at DESC');
$stmt->execute([$selected]);
foreach ($stmt as $a):
?>
<tr>
    <td><?= e($a['quiz_type']) ?></td><td><?= e($a['chapter_title'] ?? 'Full Mock') ?></td>
    <td><?= (int)$a['score'] ?> / <?= (int)$a['total_questions'] ?></td>
    <td><?= (int)$a['correct_count'] ?></td><td><?= (int)$a['incorrect_count'] ?></td>
    <td><?= (int)$a['skipped_count'] ?></td><td><?= (int)$a['time_taken_seconds'] ?>s</td>
    <td><?= e($a['created_at']) ?></td>
</tr>
<?php endforeach; ?>
</table>
<?php endif; ?>
<?php admin_foot(); ?>
