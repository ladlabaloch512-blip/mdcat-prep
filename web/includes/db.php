<?php
// Shared database connection (PDO, prepared statements everywhere).
declare(strict_types=1);

function db(): PDO
{
    static $pdo = null;
    if ($pdo instanceof PDO) {
        return $pdo;
    }
    $cfgFile = dirname(__DIR__) . '/config.php';
    $cfg = file_exists($cfgFile) ? require $cfgFile : [
        'db_host' => 'localhost', 'db_name' => 'mdcatprep',
        'db_user' => 'root', 'db_pass' => '', 'base_url' => '',
    ];
    $dsn = 'mysql:host=' . $cfg['db_host'] . ';dbname=' . $cfg['db_name'] . ';charset=utf8mb4';
    $pdo = new PDO($dsn, $cfg['db_user'], $cfg['db_pass'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
    return $pdo;
}

function app_config(): array
{
    static $cfg = null;
    if ($cfg === null) {
        $cfgFile = dirname(__DIR__) . '/config.php';
        $cfg = file_exists($cfgFile) ? require $cfgFile : ['base_url' => ''];
    }
    return $cfg;
}

/** Public base URL of this install, always ending with a slash. */
function base_url(): string
{
    $cfg = app_config();
    if (!empty($cfg['base_url'])) {
        return rtrim($cfg['base_url'], '/') . '/';
    }
    $https = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
        || (($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '') === 'https');
    $scheme = $https ? 'https' : 'http';
    $host = $_SERVER['HTTP_HOST'] ?? 'localhost';
    $dir = rtrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'] ?? '/')), '/');
    // API and admin live one level below the web root.
    if (basename($dir) === 'api' || basename($dir) === 'admin') {
        $dir = rtrim(dirname($dir), '/');
    }
    return $scheme . '://' . $host . ($dir === '' ? '/' : $dir . '/');
}
