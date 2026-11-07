<?php
declare(strict_types=1);
session_start();
require_once dirname(__DIR__) . '/includes/helpers.php';

$error = '';
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
    $email = strtolower(trim((string)($_POST['email'] ?? '')));
    $password = (string)($_POST['password'] ?? '');
    $stmt = db()->prepare('SELECT * FROM admins WHERE email = ?');
    $stmt->execute([$email]);
    $admin = $stmt->fetch();
    if ($admin && password_verify($password, $admin['password_hash'])) {
        session_regenerate_id(true);
        $_SESSION['admin'] = ['id' => (int)$admin['id'], 'name' => $admin['name'], 'email' => $admin['email']];
        header('Location: index.php');
        exit;
    }
    $error = 'Invalid email or password.';
}
?>
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Admin Login - <?= e(setting('app_name')) ?></title>
<link rel="stylesheet" href="../assets/css/admin.css">
</head>
<body class="login-body">
<form class="login-card" method="post">
    <h1><?= e(setting('app_name')) ?></h1>
    <p class="muted">Admin Panel</p>
    <?php if ($error): ?><div class="error"><?= e($error) ?></div><?php endif; ?>
    <label>Email <input type="email" name="email" required autofocus></label>
    <label>Password <input type="password" name="password" required></label>
    <button class="btn primary wide" type="submit">Sign in</button>
</form>
</body>
</html>
