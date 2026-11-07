<?php
// Shared helpers: settings, JSON responses, auth tokens, scoring rules.
// Every changeable value comes from the `settings` table; the DEFAULTS below
// are only an empty-database fallback and are never the live value once the
// seed is imported.
declare(strict_types=1);

require_once __DIR__ . '/db.php';

const SETTING_DEFAULTS = [
    'app_name' => 'MDCAT Prep',
    'tagline' => 'Your Path to Medical College',
    'support_email' => '',
    'social_facebook' => '',
    'social_youtube' => '',
    'social_instagram' => '',
    'daily_goal' => '50',
    'mock_question_count' => '200',
    'timer_seconds_per_question' => '60',
    'weak_threshold_percent' => '60',
    'weak_min_attempts' => '5',
    'default_design' => '1',
    'google_client_id' => '',
    'privacy_url' => 'privacy.php',
    'daily_reward_coins' => '20',
    'rewards_enabled' => '1',
    'whatsapp_channel_url' => '',
];

/** All settings as key => value (database first, fallback defaults). */
function settings_all(): array
{
    static $cache = null;
    if ($cache !== null) {
        return $cache;
    }
    $out = SETTING_DEFAULTS;
    try {
        foreach (db()->query('SELECT `key`, `value` FROM settings') as $row) {
            $out[$row['key']] = $row['value'];
        }
    } catch (Throwable $e) {
        // Table missing (install not run): fall back to defaults.
    }
    return $cache = $out;
}

function setting(string $key): string
{
    $all = settings_all();
    return (string)($all[$key] ?? SETTING_DEFAULTS[$key] ?? '');
}

function setting_int(string $key): int
{
    return (int)setting($key);
}

function set_setting(string $key, string $value): void
{
    $stmt = db()->prepare('INSERT INTO settings (`key`, `value`) VALUES (?, ?)
        ON DUPLICATE KEY UPDATE `value` = VALUES(`value`)');
    $stmt->execute([$key, $value]);
}

/* ------------------------------------------------------------ JSON output */
function json_out($data, int $status = 200): void
{
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode($data, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    exit;
}

function json_ok($data = null): void
{
    json_out(['ok' => true, 'data' => $data]);
}

function json_error(string $message, int $status = 400, string $code = 'error'): void
{
    json_out(['ok' => false, 'error' => ['code' => $code, 'message' => $message]], $status);
}

/** Decoded JSON request body as an array (empty array when none). */
function request_json(): array
{
    $raw = file_get_contents('php://input');
    if ($raw === false || trim($raw) === '') {
        return [];
    }
    $data = json_decode($raw, true);
    return is_array($data) ? $data : [];
}

/** Input value from JSON body first, then form/query params. */
function input(string $key, $default = null)
{
    static $body = null;
    if ($body === null) {
        $body = request_json();
    }
    if (array_key_exists($key, $body)) {
        return $body[$key];
    }
    return $_REQUEST[$key] ?? $default;
}

/* ------------------------------------------------------------------ auth */
function issue_token(int $userId): string
{
    $token = bin2hex(random_bytes(32));
    $stmt = db()->prepare('INSERT INTO user_tokens (user_id, token, expires_at)
        VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 90 DAY))');
    $stmt->execute([$userId, $token]);
    return $token;
}

/** Current user row from the bearer token, or null (guest). */
function current_user(): ?array
{
    $token = '';
    $auth = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (stripos($auth, 'Bearer ') === 0) {
        $token = trim(substr($auth, 7));
    } elseif (!empty($_SERVER['HTTP_X_AUTH_TOKEN'])) {
        $token = trim($_SERVER['HTTP_X_AUTH_TOKEN']);
    } elseif (!empty($_GET['token'])) {
        $token = trim((string)$_GET['token']);
    }
    if ($token === '') {
        return null;
    }
    $stmt = db()->prepare('SELECT u.* FROM user_tokens t JOIN users u ON u.id = t.user_id
        WHERE t.token = ? AND (t.expires_at IS NULL OR t.expires_at > NOW())');
    $stmt->execute([$token]);
    $user = $stmt->fetch();
    return $user ?: null;
}

/** Current user or a 401 JSON error. Guests never pass this gate. */
function require_user(): array
{
    $user = current_user();
    if ($user === null) {
        json_error('Please sign in to continue.', 401, 'auth_required');
    }
    return $user;
}

function public_user(array $user): array
{
    return [
        'id' => (int)$user['id'],
        'name' => $user['name'],
        'email' => $user['email'],
        'phone' => $user['phone'],
        'created_at' => $user['created_at'],
        'coins' => (int)($user['coins'] ?? 0),
        'personal_goal' => isset($user['personal_goal']) && $user['personal_goal'] !== null
            ? (int)$user['personal_goal'] : null,
    ];
}

/* ------------------------------------------------------------- quiz rules */
/**
 * Per-topic statistics for one user: attempted and correct counts grouped by
 * chapter. Used for progress bars and weak-topic markers.
 */
function user_chapter_stats(int $userId): array
{
    $stmt = db()->prepare(
        'SELECT q.chapter_id AS chapter_id,
                COUNT(*) AS attempted,
                SUM(aa.is_correct) AS correct
           FROM attempt_answers aa
           JOIN quiz_attempts a ON a.id = aa.attempt_id
           JOIN questions q ON q.id = aa.question_id
          WHERE a.user_id = ?
          GROUP BY q.chapter_id'
    );
    $stmt->execute([$userId]);
    $out = [];
    foreach ($stmt->fetchAll() as $row) {
        $attempted = (int)$row['attempted'];
        $correct = (int)$row['correct'];
        $out[(int)$row['chapter_id']] = [
            'attempted' => $attempted,
            'correct' => $correct,
            'percent' => $attempted > 0 ? (int)round($correct * 100 / $attempted) : 0,
        ];
    }
    return $out;
}

/** Weak-topic rule: attempted >= settings minimum AND percent < threshold. */
function is_weak(array $stat): bool
{
    return $stat['attempted'] >= setting_int('weak_min_attempts')
        && $stat['percent'] < setting_int('weak_threshold_percent');
}

/** Number of MCQs the user completed today (Daily Progress numerator). */
function daily_completed(int $userId): int
{
    $stmt = db()->prepare(
        'SELECT COUNT(*) FROM attempt_answers aa
           JOIN quiz_attempts a ON a.id = aa.attempt_id
          WHERE a.user_id = ? AND DATE(a.created_at) = CURDATE()'
    );
    $stmt->execute([$userId]);
    return (int)$stmt->fetchColumn();
}

/** Whether the user already claimed today's Daily Reward. */
function reward_claimed_today(int $userId): bool
{
    try {
        $stmt = db()->prepare('SELECT COUNT(*) FROM reward_claims WHERE user_id = ? AND claim_date = CURDATE()');
        $stmt->execute([$userId]);
        return (int)$stmt->fetchColumn() > 0;
    } catch (Throwable $e) {
        return false; // Migration not run yet: behave as unclaimed.
    }
}

/** Total Daily Reward claims ever made by the user. */
function reward_total_claims(int $userId): int
{
    try {
        $stmt = db()->prepare('SELECT COUNT(*) FROM reward_claims WHERE user_id = ?');
        $stmt->execute([$userId]);
        return (int)$stmt->fetchColumn();
    } catch (Throwable $e) {
        return 0;
    }
}

/** Consecutive claim days ending today (or yesterday if today is open). */
function reward_streak(int $userId): int
{
    try {
        $stmt = db()->prepare('SELECT claim_date FROM reward_claims WHERE user_id = ? ORDER BY claim_date DESC LIMIT 60');
        $stmt->execute([$userId]);
        $dates = [];
        foreach ($stmt->fetchAll() as $r) {
            $dates[$r['claim_date']] = true;
        }
    } catch (Throwable $e) {
        return 0;
    }
    $cursor = isset($dates[date('Y-m-d')]) ? 0 : 1; // start today, else yesterday
    $streak = 0;
    while (isset($dates[date('Y-m-d', strtotime("-$cursor days"))])) {
        $streak++;
        $cursor++;
    }
    return $streak;
}

/** Consecutive days with at least one quiz attempt, ending today/yesterday. */
function study_streak(int $userId): int
{
    $stmt = db()->prepare(
        'SELECT DISTINCT DATE(created_at) AS d FROM quiz_attempts
          WHERE user_id = ? ORDER BY d DESC LIMIT 60'
    );
    $stmt->execute([$userId]);
    $dates = [];
    foreach ($stmt->fetchAll() as $r) {
        $dates[$r['d']] = true;
    }
    $cursor = isset($dates[date('Y-m-d')]) ? 0 : 1;
    $streak = 0;
    while (isset($dates[date('Y-m-d', strtotime("-$cursor days"))])) {
        $streak++;
        $cursor++;
    }
    return $streak;
}

/** Active, in-date banners for one placement slot, in admin order. */
function banners_for(string $slot): array
{
    $stmt = db()->prepare(
        'SELECT * FROM banners
          WHERE placement = ? AND is_active = 1
            AND (start_date IS NULL OR start_date <= CURDATE())
            AND (end_date IS NULL OR end_date >= CURDATE())
          ORDER BY sort_order ASC, id ASC'
    );
    $stmt->execute([$slot]);
    $out = [];
    foreach ($stmt->fetchAll() as $b) {
        $out[] = [
            'id' => (int)$b['id'],
            'title' => $b['title'],
            'image_url' => base_url() . ltrim($b['image_path'], '/'),
            'target_link' => $b['target_link'],
            'placement' => $b['placement'],
        ];
    }
    return $out;
}

/** Question row as served BEFORE an attempt (never leaks the answer). */
function public_question(array $q, bool $saved = false): array
{
    return [
        'id' => (int)$q['id'],
        'chapter_id' => (int)$q['chapter_id'],
        'subtopic' => $q['subtopic'],
        'question' => $q['question_text'],
        'options' => [
            'A' => $q['option_a'], 'B' => $q['option_b'],
            'C' => $q['option_c'], 'D' => $q['option_d'],
        ],
        'is_saved' => $saved,
    ];
}

/** Question row as served in review/saved lists (answer included). */
function review_question(array $q, ?string $selected = null): array
{
    $out = public_question($q, true);
    $out['correct_option'] = $q['correct_option'];
    $out['explanation'] = $q['explanation'];
    if ($selected !== null) {
        $out['selected_option'] = $selected;
        $out['is_correct'] = $selected === $q['correct_option'];
    }
    return $out;
}

function saved_ids(int $userId): array
{
    $stmt = db()->prepare('SELECT question_id FROM saved_questions WHERE user_id = ?');
    $stmt->execute([$userId]);
    return array_map('intval', $stmt->fetchAll(PDO::FETCH_COLUMN));
}

/** Sanitised plain text for admin/website output. */
function e(?string $s): string
{
    return htmlspecialchars((string)$s, ENT_QUOTES, 'UTF-8');
}


/* ------------------------------------------------- sign-in throttling
   5 failed attempts inside 10 minutes lock the key for 15 minutes.
   Keys look like 'login:<email>|<ip>'. Failures are stored in the
   login_throttle table; a missing table degrades to no throttling. */
function throttle_locked(string $key): bool
{
    try {
        $stmt = db()->prepare('SELECT locked_until FROM login_throttle WHERE id_key = ?');
        $stmt->execute([$key]);
        $until = $stmt->fetchColumn();
        return $until && strtotime((string)$until) > time();
    } catch (Throwable $e) {
        return false;
    }
}

function throttle_fail(string $key): void
{
    try {
        $stmt = db()->prepare('SELECT fail_count, last_fail_at FROM login_throttle WHERE id_key = ?');
        $stmt->execute([$key]);
        $row = $stmt->fetch();
        $count = 1;
        if ($row && $row['last_fail_at'] && strtotime((string)$row['last_fail_at']) > time() - 600) {
            $count = (int)$row['fail_count'] + 1;
        }
        $lockedUntil = $count >= 5 ? date('Y-m-d H:i:s', time() + 900) : null;
        db()->prepare('INSERT INTO login_throttle (id_key, fail_count, last_fail_at, locked_until) VALUES (?, ?, NOW(), ?)
            ON DUPLICATE KEY UPDATE fail_count = VALUES(fail_count), last_fail_at = NOW(), locked_until = VALUES(locked_until)')
            ->execute([$key, $count, $lockedUntil]);
    } catch (Throwable $e) { /* throttling must never break sign-in */ }
}

function throttle_clear(string $key): void
{
    try {
        db()->prepare('DELETE FROM login_throttle WHERE id_key = ?')->execute([$key]);
    } catch (Throwable $e) { }
}


/** Map of chapter id => subject title, for result charts and filters. */
function subject_titles_for(array $chapterIds): array
{
    $chapterIds = array_values(array_unique(array_map('intval', $chapterIds)));
    if (!$chapterIds) return [];
    try {
        $place = implode(',', array_fill(0, count($chapterIds), '?'));
        $stmt = db()->prepare(
            "SELECT c.id, s.title FROM chapters c JOIN subjects s ON s.id = c.subject_id WHERE c.id IN ($place)"
        );
        $stmt->execute($chapterIds);
        $map = [];
        foreach ($stmt->fetchAll() as $r) $map[(int)$r['id']] = $r['title'];
        return $map;
    } catch (Throwable $e) {
        return [];
    }
}

/** Active notifications the user has not marked read yet. */
function unread_notification_count(int $userId): int
{
    try {
        $stmt = db()->prepare(
            'SELECT COUNT(*) FROM notifications n
              WHERE n.is_active = 1
                AND NOT EXISTS (SELECT 1 FROM notification_reads r
                                 WHERE r.user_id = ? AND r.notification_id = n.id)'
        );
        $stmt->execute([$userId]);
        return (int)$stmt->fetchColumn();
    } catch (Throwable $e) {
        return 0;
    }
}
