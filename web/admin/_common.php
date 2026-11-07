<?php
// Admin panel shared bootstrap: session auth, layout, small CRUD helpers.
declare(strict_types=1);
session_start();
require_once dirname(__DIR__) . '/includes/helpers.php';

function admin_user(): ?array
{
    return $_SESSION['admin'] ?? null;
}

function require_admin(): array
{
    $admin = admin_user();
    if ($admin === null) {
        header('Location: login.php');
        exit;
    }
    return $admin;
}

function csrf_token(): string
{
    if (empty($_SESSION['csrf'])) {
        $_SESSION['csrf'] = bin2hex(random_bytes(16));
    }
    return $_SESSION['csrf'];
}

function csrf_check(): void
{
    if (!hash_equals($_SESSION['csrf'] ?? '', (string)($_POST['csrf'] ?? ''))) {
        http_response_code(403);
        exit('Invalid form token. Please go back and try again.');
    }
}

function flash(string $msg): void
{
    $_SESSION['flash'] = $msg;
}

function take_flash(): ?string
{
    $msg = $_SESSION['flash'] ?? null;
    unset($_SESSION['flash']);
    return $msg;
}

function admin_head(string $title): void
{
    $admin = admin_user();
    $flash = take_flash();
    $app = e(setting('app_name'));
    echo '<!doctype html><html lang="en"><head><meta charset="utf-8">';
    echo '<meta name="viewport" content="width=device-width, initial-scale=1">';
    echo '<title>' . e($title) . ' - ' . $app . ' Admin</title>';
    echo '<link rel="stylesheet" href="../assets/css/admin.css"></head><body>';
    echo '<div class="layout"><aside class="side">';
    echo '<div class="brand">' . $app . '<span>Admin Panel</span></div>';
    $links = [
        'index.php' => 'Dashboard', 'subjects.php' => 'Subjects',
        'chapters.php' => 'Chapters', 'questions.php' => 'Questions (MCQs)',
        'banners.php' => 'Banners', 'notifications.php' => 'Notifications',
        'students.php' => 'Students', 'attempts.php' => 'Results',
        'settings.php' => 'Settings',
    ];
    $current = basename($_SERVER['SCRIPT_NAME'] ?? '');
    foreach ($links as $href => $label) {
        $cls = $href === $current ? ' class="active"' : '';
        echo '<a href="' . $href . '"' . $cls . '>' . $label . '</a>';
    }
    echo '<a href="logout.php" class="logout">Log out (' . e($admin['name'] ?? '') . ')</a>';
    echo '</aside><main class="main">';
    echo '<h1>' . e($title) . '</h1>';
    if ($flash) {
        echo '<div class="flash">' . e($flash) . '</div>';
    }
}

function admin_foot(): void
{
    echo '</main></div></body></html>';
}

/** Simple POST delete handler shared by CRUD pages. */
function handle_delete(string $table, string $redirect): void
{
    if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST' && isset($_POST['delete_id'])) {
        csrf_check();
        $stmt = db()->prepare("DELETE FROM $table WHERE id = ?");
        $stmt->execute([(int)$_POST['delete_id']]);
        flash('Deleted.');
        header('Location: ' . $redirect);
        exit;
    }
}

function delete_button(int $id): string
{
    return '<form method="post" class="inline" onsubmit="return confirm(\'Delete this item?\')">'
        . '<input type="hidden" name="csrf" value="' . csrf_token() . '">'
        . '<input type="hidden" name="delete_id" value="' . $id . '">'
        . '<button class="btn danger small" type="submit">Delete</button></form>';
}
