<?php
declare(strict_types=1);
require_once __DIR__ . '/_common.php';
$admin = require_admin();

$counts = [
    'Students' => 'SELECT COUNT(*) FROM users',
    'Subjects' => 'SELECT COUNT(*) FROM subjects',
    'Chapters' => 'SELECT COUNT(*) FROM chapters',
    'Questions' => 'SELECT COUNT(*) FROM questions WHERE is_active = 1',
    'Quiz attempts' => 'SELECT COUNT(*) FROM quiz_attempts',
    'Active banners' => 'SELECT COUNT(*) FROM banners WHERE is_active = 1',
    'Notifications' => 'SELECT COUNT(*) FROM notifications WHERE is_active = 1',
];
admin_head('Dashboard');
echo '<div class="cards">';
foreach ($counts as $label => $sql) {
    $n = (int)db()->query($sql)->fetchColumn();
    echo '<div class="stat"><strong>' . $n . '</strong><span>' . e($label) . '</span></div>';
}
echo '</div>';
echo '<h2>Recent attempts</h2>';
$stmt = db()->query('SELECT a.*, u.name AS student, c.title AS chapter_title
    FROM quiz_attempts a JOIN users u ON u.id = a.user_id
    LEFT JOIN chapters c ON c.id = a.chapter_id
    ORDER BY a.created_at DESC LIMIT 10');
echo '<table><tr><th>Student</th><th>Type</th><th>Chapter</th><th>Score</th><th>Date</th></tr>';
foreach ($stmt as $a) {
    echo '<tr><td>' . e($a['student']) . '</td><td>' . e($a['quiz_type']) . '</td><td>'
        . e($a['chapter_title'] ?? 'Full Mock') . '</td><td>' . (int)$a['score'] . ' / ' . (int)$a['total_questions']
        . '</td><td>' . e($a['created_at']) . '</td></tr>';
}
echo '</table>';
admin_foot();
