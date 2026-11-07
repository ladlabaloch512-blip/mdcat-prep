<?php
declare(strict_types=1);
require_once __DIR__ . '/_common.php';
require_admin();

admin_head('Results');
$view = (int)($_GET['view'] ?? 0);
?>
<table>
<tr><th>Student</th><th>Type</th><th>Chapter</th><th>Score</th><th>Correct</th><th>Wrong</th><th>Skipped</th><th>Date</th><th></th></tr>
<?php
$stmt = db()->query('SELECT a.*, u.name AS student, c.title AS chapter_title FROM quiz_attempts a
    JOIN users u ON u.id = a.user_id LEFT JOIN chapters c ON c.id = a.chapter_id
    ORDER BY a.created_at DESC LIMIT 200');
foreach ($stmt as $a):
?>
<tr>
    <td><?= e($a['student']) ?></td><td><?= e($a['quiz_type']) ?></td>
    <td><?= e($a['chapter_title'] ?? 'Full Mock') ?></td>
    <td><?= (int)$a['score'] ?> / <?= (int)$a['total_questions'] ?></td>
    <td><?= (int)$a['correct_count'] ?></td><td><?= (int)$a['incorrect_count'] ?></td>
    <td><?= (int)$a['skipped_count'] ?></td><td><?= e($a['created_at']) ?></td>
    <td><a class="btn small" href="attempts.php?view=<?= (int)$a['id'] ?>">Answers</a></td>
</tr>
<?php endforeach; ?>
</table>

<?php if ($view > 0): ?>
<h2>Attempt #<?= $view ?> - answer review</h2>
<table>
<tr><th>Question</th><th>Student picked</th><th>Correct</th><th>Result</th></tr>
<?php
$stmt = db()->prepare('SELECT aa.*, q.question_text, q.correct_option FROM attempt_answers aa
    JOIN questions q ON q.id = aa.question_id WHERE aa.attempt_id = ? ORDER BY aa.id');
$stmt->execute([$view]);
foreach ($stmt as $row):
?>
<tr>
    <td><?= e(mb_strimwidth((string)$row['question_text'], 0, 90, '...')) ?></td>
    <td><?= e($row['selected_option'] ?? 'Skipped') ?></td>
    <td><?= e($row['correct_option']) ?></td>
    <td><?= $row['selected_option'] === null ? 'Skipped' : ($row['is_correct'] ? 'Correct' : 'Wrong') ?></td>
</tr>
<?php endforeach; ?>
</table>
<?php endif; ?>
<?php admin_foot(); ?>
