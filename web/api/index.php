<?php
// MDCAT Prep REST API. One entry point, route passed as ?r=auth/login so it
// works on any shared hosting without rewrite rules.
declare(strict_types=1);

// CORS: reflect only an Origin from this same host. The native app sends
// no Origin at all and the website is same-origin, so nobody else needs
// cross-origin access to this API.
$reqOrigin = (string)($_SERVER['HTTP_ORIGIN'] ?? '');
if ($reqOrigin !== '') {
    $host = (string)($_SERVER['HTTP_HOST'] ?? '');
    $oHost = (string)(parse_url($reqOrigin, PHP_URL_HOST) ?? '');
    $oPort = parse_url($reqOrigin, PHP_URL_PORT);
    if ($oHost !== '' && strcasecmp($oHost . ($oPort ? ':' . $oPort : ''), $host) === 0) {
        header('Access-Control-Allow-Origin: ' . $reqOrigin);
        header('Vary: Origin');
    }
}
header('Access-Control-Allow-Headers: Authorization, Content-Type, X-Auth-Token');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
if (($_SERVER['REQUEST_METHOD'] ?? 'GET') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

require_once dirname(__DIR__) . '/includes/helpers.php';

$route = trim((string)($_GET['r'] ?? ''), '/');
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

try {
    switch ($route) {
        case 'health':
            json_ok(['status' => 'up', 'app' => setting('app_name')]);
            break;

        case 'settings':
            json_ok(settings_all());
            break;

        /* ---------------------------------------------------------- auth */
        case 'auth/register': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $name = trim((string)input('name', ''));
            $email = strtolower(trim((string)input('email', '')));
            $phone = trim((string)input('phone', ''));
            $password = (string)input('password', '');
            if ($name === '' || $email === '' || $password === '') {
                json_error('Name, email and password are required.');
            }
            if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
                json_error('Please enter a valid email address.');
            }
            if (strlen($password) < 6) {
                json_error('Password must be at least 6 characters.');
            }
            $stmt = db()->prepare('SELECT id FROM users WHERE email = ?');
            $stmt->execute([$email]);
            if ($stmt->fetch()) {
                json_error('An account with this email already exists. Please sign in.', 409, 'email_exists');
            }
            $stmt = db()->prepare('INSERT INTO users (name, email, phone, password_hash) VALUES (?, ?, ?, ?)');
            $stmt->execute([$name, $email, $phone !== '' ? $phone : null, password_hash($password, PASSWORD_DEFAULT)]);
            $userId = (int)db()->lastInsertId();
            $stmt = db()->prepare('SELECT * FROM users WHERE id = ?');
            $stmt->execute([$userId]);
            $user = $stmt->fetch();
            json_ok(['token' => issue_token($userId), 'user' => public_user($user)]);
            break;
        }

        case 'auth/login': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $email = strtolower(trim((string)input('email', '')));
            $password = (string)input('password', '');
            $tKey = 'login:' . $email . '|' . (string)($_SERVER['REMOTE_ADDR'] ?? '');
            if (throttle_locked($tKey)) {
                json_error('Too many failed sign-in attempts. Please wait 15 minutes and try again.', 429, 'too_many_attempts');
            }
            $stmt = db()->prepare('SELECT * FROM users WHERE email = ?');
            $stmt->execute([$email]);
            $user = $stmt->fetch();
            if (!$user || empty($user['password_hash']) || !password_verify($password, $user['password_hash'])) {
                throttle_fail($tKey);
                json_error('Invalid email or password.', 401, 'invalid_credentials');
            }
            throttle_clear($tKey);
            json_ok(['token' => issue_token((int)$user['id']), 'user' => public_user($user)]);
            break;
        }

        case 'auth/google': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $clientId = setting('google_client_id');
            $cfg = app_config();
            if ($clientId === '' && !empty($cfg['google_client_id'])) {
                $clientId = (string)$cfg['google_client_id'];
            }
            if ($clientId === '') {
                json_error('Google sign-in is not configured yet. The admin must set the Google client ID.', 501, 'google_not_configured');
            }
            $idToken = (string)input('id_token', '');
            if ($idToken === '') {
                json_error('Missing Google ID token.');
            }
            $verifyUrl = 'https://oauth2.googleapis.com/tokeninfo?id_token=' . urlencode($idToken);
            $resp = @file_get_contents($verifyUrl);
            $payload = $resp ? json_decode($resp, true) : null;
            if (!is_array($payload) || ($payload['aud'] ?? '') !== $clientId || empty($payload['sub'])) {
                json_error('Google sign-in could not be verified.', 401, 'google_verify_failed');
            }
            $googleId = (string)$payload['sub'];
            $email = strtolower((string)($payload['email'] ?? ''));
            $name = (string)($payload['name'] ?? 'Student');
            $stmt = db()->prepare('SELECT * FROM users WHERE google_id = ? OR email = ? LIMIT 1');
            $stmt->execute([$googleId, $email]);
            $user = $stmt->fetch();
            if ($user) {
                if (empty($user['google_id'])) {
                    db()->prepare('UPDATE users SET google_id = ? WHERE id = ?')->execute([$googleId, $user['id']]);
                }
            } else {
                if ($email === '') {
                    json_error('Google account did not provide an email address.', 401);
                }
                db()->prepare('INSERT INTO users (name, email, google_id) VALUES (?, ?, ?)')
                    ->execute([$name, $email, $googleId]);
                $stmt = db()->prepare('SELECT * FROM users WHERE id = ?');
                $stmt->execute([(int)db()->lastInsertId()]);
                $user = $stmt->fetch();
            }
            json_ok(['token' => issue_token((int)$user['id']), 'user' => public_user($user)]);
            break;
        }

        case 'auth/logout': {
            $user = require_user();
            $token = trim((string)($_SERVER['HTTP_X_AUTH_TOKEN'] ?? ''));
            $auth = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
            if (stripos($auth, 'Bearer ') === 0) {
                $token = trim(substr($auth, 7));
            }
            if ($token !== '') {
                db()->prepare('DELETE FROM user_tokens WHERE token = ? AND user_id = ?')
                    ->execute([$token, $user['id']]);
            }
            json_ok(['logged_out' => true]);
            break;
        }

        case 'me': {
            $user = require_user();
            json_ok(public_user($user));
            break;
        }

        case 'auth/forgot': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $email = strtolower(trim((string)input('email', '')));
            // Always the same answer, so the endpoint never reveals
            // whether an email is registered.
            $generic = 'If this email is registered, a reset code has been sent to it.';
            if (!filter_var($email, FILTER_VALIDATE_EMAIL)) json_ok(['message' => $generic]);
            $stmt = db()->prepare('SELECT * FROM users WHERE email = ?');
            $stmt->execute([$email]);
            $user = $stmt->fetch();
            if ($user && !empty($user['password_hash'])) {
                $recent = db()->prepare('SELECT COUNT(*) FROM password_resets WHERE user_id = ? AND created_at > (NOW() - INTERVAL 15 MINUTE)');
                $recent->execute([$user['id']]);
                if ((int)$recent->fetchColumn() < 3) {
                    $code = (string)random_int(100000, 999999);
                    db()->prepare('DELETE FROM password_resets WHERE user_id = ?')->execute([$user['id']]);
                    db()->prepare('INSERT INTO password_resets (user_id, code_hash, expires_at) VALUES (?, ?, NOW() + INTERVAL 15 MINUTE)')
                        ->execute([$user['id'], hash('sha256', $code)]);
                    $host = (string)($_SERVER['HTTP_HOST'] ?? 'localhost');
                    $appName = setting('app_name');
                    $body = "Your " . $appName . " password reset code is: " . $code . "\n\n"
                        . "The code is valid for 15 minutes. Enter it in the app or on the website together with your new password.\n"
                        . "If you did not ask for a password reset, you can ignore this email.";
                    @mail($email, $appName . ' password reset code', $body,
                        'From: no-reply@' . $host . "\r\nContent-Type: text/plain; charset=UTF-8");
                }
            }
            json_ok(['message' => $generic]);
            break;
        }

        case 'auth/reset': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $email = strtolower(trim((string)input('email', '')));
            $code = trim((string)input('code', ''));
            $password = (string)input('password', '');
            if (strlen($password) < 6) json_error('Password must be at least 6 characters.');
            $stmt = db()->prepare('SELECT * FROM users WHERE email = ?');
            $stmt->execute([$email]);
            $user = $stmt->fetch();
            if (!$user) json_error('This reset code is invalid or has expired.', 400, 'reset_invalid');
            $stmt = db()->prepare('SELECT * FROM password_resets WHERE user_id = ? ORDER BY id DESC LIMIT 1');
            $stmt->execute([$user['id']]);
            $reset = $stmt->fetch();
            if (!$reset || (int)$reset['attempts'] >= 5 || strtotime((string)$reset['expires_at']) < time()
                || !hash_equals((string)$reset['code_hash'], hash('sha256', $code))) {
                if ($reset) db()->prepare('UPDATE password_resets SET attempts = attempts + 1 WHERE id = ?')->execute([$reset['id']]);
                json_error('This reset code is invalid or has expired.', 400, 'reset_invalid');
            }
            db()->prepare('UPDATE users SET password_hash = ? WHERE id = ?')
                ->execute([password_hash($password, PASSWORD_DEFAULT), $user['id']]);
            db()->prepare('DELETE FROM password_resets WHERE user_id = ?')->execute([$user['id']]);
            // A password reset signs the account out everywhere.
            db()->prepare('DELETE FROM user_tokens WHERE user_id = ?')->execute([$user['id']]);
            json_ok(['reset' => true]);
            break;
        }

        case 'profile/update': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $user = require_user();
            $name = trim((string)input('name', ''));
            $phone = trim((string)input('phone', ''));
            if ($name === '' || mb_strlen($name) > 120) json_error('Please enter your name.');
            if (mb_strlen($phone) > 30) json_error('Phone number is too long.');
            db()->prepare('UPDATE users SET name = ?, phone = ? WHERE id = ?')
                ->execute([$name, $phone, $user['id']]);
            $stmt = db()->prepare('SELECT * FROM users WHERE id = ?');
            $stmt->execute([$user['id']]);
            json_ok(public_user($stmt->fetch()));
            break;
        }

        case 'profile/password': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $user = require_user();
            $current = (string)input('current_password', '');
            $new = (string)input('new_password', '');
            if (strlen($new) < 6) json_error('New password must be at least 6 characters.');
            if (!empty($user['password_hash']) && !password_verify($current, $user['password_hash'])) {
                json_error('Your current password is not correct.', 401, 'invalid_credentials');
            }
            db()->prepare('UPDATE users SET password_hash = ? WHERE id = ?')
                ->execute([password_hash($new, PASSWORD_DEFAULT), $user['id']]);
            // Keep this session, sign out every other device.
            $token = trim((string)($_SERVER['HTTP_X_AUTH_TOKEN'] ?? ''));
            $auth = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
            if (stripos($auth, 'Bearer ') === 0) $token = trim(substr($auth, 7));
            if ($token !== '') {
                db()->prepare('DELETE FROM user_tokens WHERE user_id = ? AND token <> ?')->execute([$user['id'], $token]);
            } else {
                db()->prepare('DELETE FROM user_tokens WHERE user_id = ?')->execute([$user['id']]);
            }
            json_ok(['changed' => true]);
            break;
        }

        case 'auth/delete': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $user = require_user();
            $password = (string)input('password', '');
            if (!empty($user['password_hash']) && !password_verify($password, $user['password_hash'])) {
                json_error('Your password is not correct.', 401, 'invalid_credentials');
            }
            // Foreign keys cascade: tokens, attempts, answers, saved
            // questions and reward claims are removed with the account.
            db()->prepare('DELETE FROM password_resets WHERE user_id = ?')->execute([$user['id']]);
            db()->prepare('DELETE FROM users WHERE id = ?')->execute([$user['id']]);
            json_ok(['deleted' => true]);
            break;
        }

        /* -------------------------------------------------------- content */
        case 'home': {
            $user = current_user();
            $subjects = fetch_subjects($user ? (int)$user['id'] : null);
            $data = [
                'app_name' => setting('app_name'),
                'tagline' => setting('tagline'),
                'user' => $user ? public_user($user) : null,
                'daily_goal' => $user && !empty($user['personal_goal'])
                    ? (int)$user['personal_goal'] : setting_int('daily_goal'),
                'daily_completed' => $user ? daily_completed((int)$user['id']) : 0,
                'coins' => $user ? (int)($user['coins'] ?? 0) : 0,
                'reward_claimed_today' => $user ? reward_claimed_today((int)$user['id']) : false,
                'daily_reward_coins' => setting_int('daily_reward_coins'),
                'whatsapp_channel_url' => setting('whatsapp_channel_url'),
                'unread_notifications' => $user ? unread_notification_count((int)$user['id']) : 0,
                'subjects' => $subjects,
                'banners_dashboard' => banners_for('dashboard'),
                'banners_after_subjects' => banners_for('dashboard_after_subjects'),
                'default_design' => setting_int('default_design'),
            ];
            json_ok($data);
            break;
        }

        case 'subjects': {
            $user = current_user();
            json_ok(fetch_subjects($user ? (int)$user['id'] : null));
            break;
        }

        case 'chapters': {
            $subjectId = (int)input('subject_id', 0);
            if ($subjectId <= 0) json_error('subject_id is required.');
            $user = current_user();
            $stats = $user ? user_chapter_stats((int)$user['id']) : [];
            $stmt = db()->prepare(
                'SELECT c.*, (SELECT COUNT(*) FROM questions q WHERE q.chapter_id = c.id AND q.is_active = 1) AS question_count
                   FROM chapters c
                  WHERE c.subject_id = ? AND c.is_active = 1
                  ORDER BY c.sort_order, c.id'
            );
            $stmt->execute([$subjectId]);
            $out = [];
            foreach ($stmt->fetchAll() as $c) {
                $stat = $stats[(int)$c['id']] ?? ['attempted' => 0, 'correct' => 0, 'percent' => 0];
                $out[] = [
                    'id' => (int)$c['id'],
                    'subject_id' => (int)$c['subject_id'],
                    'title' => $c['title'],
                    'subtopics' => $c['subtopics'],
                    'question_count' => (int)$c['question_count'],
                    'attempted' => $stat['attempted'],
                    'correct' => $stat['correct'],
                    'percent' => $stat['percent'],
                    'is_weak' => is_weak($stat),
                ];
            }
            json_ok(['chapters' => $out, 'banners' => banners_for('chapters')]);
            break;
        }

        /* ----------------------------------------------------------- quiz */
        case 'quiz/start': {
            $type = (string)input('type', 'chapter');
            $user = current_user();
            $saved = $user ? saved_ids((int)$user['id']) : [];
            if ($type === 'mock') {
                $limit = max(1, setting_int('mock_question_count'));
                $stmt = db()->prepare(
                    'SELECT q.* FROM questions q JOIN chapters c ON c.id = q.chapter_id
                      WHERE q.is_active = 1 AND c.is_active = 1
                      ORDER BY RAND() LIMIT ' . $limit
                );
                $stmt->execute();
                $title = 'Full Mock Test';
                $chapterId = null;
            } else {
                $chapterId = (int)input('chapter_id', 0);
                if ($chapterId <= 0) json_error('chapter_id is required.');
                $stmt = db()->prepare('SELECT * FROM questions WHERE chapter_id = ? AND is_active = 1 ORDER BY id');
                $stmt->execute([$chapterId]);
                $ch = db()->prepare('SELECT title FROM chapters WHERE id = ?');
                $ch->execute([$chapterId]);
                $title = (string)($ch->fetchColumn() ?: 'Chapter Quiz');
            }
            $rows = $stmt->fetchAll();
            if (!$rows) {
                json_error('No questions are available for this quiz yet.', 404, 'no_questions');
            }
            $questions = array_map(
                fn ($q) => public_question($q, in_array((int)$q['id'], $saved, true)),
                $rows
            );
            json_ok([
                'quiz_type' => $type,
                'chapter_id' => $chapterId,
                'title' => $title,
                'total' => count($questions),
                'time_limit_seconds' => count($questions) * setting_int('timer_seconds_per_question'),
                'seconds_per_question' => setting_int('timer_seconds_per_question'),
                'questions' => $questions,
            ]);
            break;
        }

        case 'quiz/submit': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $user = require_user();
            $type = (string)input('type', 'chapter');
            $chapterId = input('chapter_id');
            $chapterId = ($chapterId === null || $chapterId === '') ? null : (int)$chapterId;
            $answers = input('answers', []);
            if (!is_array($answers) || !$answers) {
                json_error('No answers were submitted.');
            }
            $timeTaken = (int)input('time_taken_seconds', 0);

            $ids = [];
            $selected = [];
            foreach ($answers as $a) {
                if (!is_array($a) || empty($a['question_id'])) continue;
                $qid = (int)$a['question_id'];
                $ids[] = $qid;
                $sel = isset($a['selected_option']) && $a['selected_option'] !== ''
                    ? strtoupper((string)$a['selected_option']) : null;
                $selected[$qid] = in_array($sel, ['A', 'B', 'C', 'D'], true) ? $sel : null;
            }
            if (!$ids) json_error('No valid answers were submitted.');

            $place = implode(',', array_fill(0, count($ids), '?'));
            $stmt = db()->prepare("SELECT * FROM questions WHERE id IN ($place) AND is_active = 1");
            $stmt->execute($ids);
            $questions = [];
            foreach ($stmt->fetchAll() as $q) {
                $questions[(int)$q['id']] = $q;
            }

            $correct = 0; $incorrect = 0; $skipped = 0;
            foreach ($ids as $qid) {
                if (!isset($questions[$qid])) continue;
                $sel = $selected[$qid];
                if ($sel === null) $skipped++;
                elseif ($sel === $questions[$qid]['correct_option']) $correct++;
                else $incorrect++;
            }
            $total = $correct + $incorrect + $skipped;

            $pdo = db();
            $pdo->beginTransaction();
            $stmt = $pdo->prepare('INSERT INTO quiz_attempts
                (user_id, quiz_type, chapter_id, total_questions, correct_count, incorrect_count, skipped_count, score, time_taken_seconds)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)');
            $stmt->execute([(int)$user['id'], $type === 'mock' ? 'mock' : 'chapter', $chapterId,
                $total, $correct, $incorrect, $skipped, $correct, $timeTaken]);
            $attemptId = (int)$pdo->lastInsertId();
            $ins = $pdo->prepare('INSERT INTO attempt_answers (attempt_id, question_id, selected_option, is_correct)
                VALUES (?, ?, ?, ?)');
            $review = [];
            $subjectMap = subject_titles_for(array_map(static fn($q) => (int)$q['chapter_id'], $questions));
            $saved = saved_ids((int)$user['id']);
            foreach ($ids as $qid) {
                if (!isset($questions[$qid])) continue;
                $q = $questions[$qid];
                $sel = $selected[$qid];
                $isCorrect = $sel !== null && $sel === $q['correct_option'] ? 1 : 0;
                $ins->execute([$attemptId, $qid, $sel, $isCorrect]);
                $row = review_question($q, $sel);
                $row['subject_title'] = $subjectMap[(int)$q['chapter_id']] ?? '';
                $row['is_saved'] = in_array($qid, $saved, true);
                $review[] = $row;
            }
            $pdo->commit();

            json_ok([
                'attempt_id' => $attemptId,
                'quiz_type' => $type,
                'chapter_id' => $chapterId,
                'score' => $correct,
                'total' => $total,
                'out_of' => $type === 'mock' ? setting_int('mock_question_count') : $total,
                'correct' => $correct,
                'incorrect' => $incorrect,
                'skipped' => $skipped,
                'percent' => $total > 0 ? (int)round($correct * 100 / $total) : 0,
                'review' => $review,
                'banners' => banners_for('result'),
            ]);
            break;
        }

        case 'attempts': {
            $user = require_user();
            $stmt = db()->prepare(
                'SELECT a.*, c.title AS chapter_title FROM quiz_attempts a
                   LEFT JOIN chapters c ON c.id = a.chapter_id
                  WHERE a.user_id = ? ORDER BY a.created_at DESC LIMIT 100'
            );
            $stmt->execute([$user['id']]);
            $out = [];
            foreach ($stmt->fetchAll() as $a) {
                $out[] = [
                    'id' => (int)$a['id'],
                    'quiz_type' => $a['quiz_type'],
                    'chapter_id' => $a['chapter_id'] ? (int)$a['chapter_id'] : null,
                    'chapter_title' => $a['chapter_title'],
                    'score' => (int)$a['score'],
                    'total' => (int)$a['total_questions'],
                    'correct' => (int)$a['correct_count'],
                    'incorrect' => (int)$a['incorrect_count'],
                    'skipped' => (int)$a['skipped_count'],
                    'time_taken_seconds' => (int)$a['time_taken_seconds'],
                    'created_at' => $a['created_at'],
                ];
            }
            json_ok($out);
            break;
        }

        /* ---------------------------------------------------------- saved */
        case 'saved': {
            $user = require_user();
            $stmt = db()->prepare(
                'SELECT q.*, c.title AS chapter_title, sub.title AS subject_title FROM saved_questions s
                   JOIN questions q ON q.id = s.question_id
                   JOIN chapters c ON c.id = q.chapter_id
                   JOIN subjects sub ON sub.id = c.subject_id
                  WHERE s.user_id = ? ORDER BY s.created_at DESC'
            );
            $stmt->execute([$user['id']]);
            $out = [];
            foreach ($stmt->fetchAll() as $q) {
                $row = review_question($q);
                $row['chapter_title'] = $q['chapter_title'];
                $row['subject_title'] = $q['subject_title'];
                $out[] = $row;
            }
            json_ok(['questions' => $out, 'banners' => banners_for('saved')]);
            break;
        }

        case 'saved/toggle': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $user = require_user(); // Guests cannot save MCQs.
            $qid = (int)input('question_id', 0);
            if ($qid <= 0) json_error('question_id is required.');
            $stmt = db()->prepare('SELECT id FROM saved_questions WHERE user_id = ? AND question_id = ?');
            $stmt->execute([$user['id'], $qid]);
            if ($stmt->fetch()) {
                db()->prepare('DELETE FROM saved_questions WHERE user_id = ? AND question_id = ?')
                    ->execute([$user['id'], $qid]);
                json_ok(['saved' => false]);
            }
            db()->prepare('INSERT INTO saved_questions (user_id, question_id) VALUES (?, ?)')
                ->execute([$user['id'], $qid]);
            json_ok(['saved' => true]);
            break;
        }

        /* --------------------------------------------------------- extras */
        case 'rewards': {
            $user = require_user();
            json_ok([
                'coins' => (int)($user['coins'] ?? 0),
                'daily_reward_coins' => setting_int('daily_reward_coins'),
                'rewards_enabled' => setting_int('rewards_enabled') === 1,
                'claimed_today' => reward_claimed_today((int)$user['id']),
                'streak_days' => reward_streak((int)$user['id']),
                'total_claims' => reward_total_claims((int)$user['id']),
            ]);
            break;
        }

        case 'rewards/claim': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $user = require_user();
            if (setting_int('rewards_enabled') !== 1) {
                json_error('Daily rewards are turned off right now.', 403, 'rewards_off');
            }
            $amount = setting_int('daily_reward_coins');
            try {
                db()->prepare('INSERT INTO reward_claims (user_id, claim_date, coins) VALUES (?, CURDATE(), ?)')
                    ->execute([(int)$user['id'], $amount]);
            } catch (Throwable $e) {
                json_error('You have already claimed today\'s reward. Come back tomorrow.', 409, 'already_claimed');
            }
            db()->prepare('UPDATE users SET coins = coins + ? WHERE id = ?')
                ->execute([$amount, (int)$user['id']]);
            $stmt = db()->prepare('SELECT coins FROM users WHERE id = ?');
            $stmt->execute([(int)$user['id']]);
            json_ok([
                'claimed' => true,
                'added' => $amount,
                'coins' => (int)$stmt->fetchColumn(),
                'streak_days' => reward_streak((int)$user['id']),
            ]);
            break;
        }

        case 'qotd': {
            // Question of the Day: the same question for everyone all day,
            // chosen deterministically from the active question bank.
            $count = (int)db()->query('SELECT COUNT(*) FROM questions WHERE is_active = 1')->fetchColumn();
            if ($count === 0) {
                json_error('No questions are available yet.', 404, 'no_questions');
            }
            $offset = (int)(crc32(date('Y-m-d')) % $count);
            $stmt = db()->prepare('SELECT q.*, c.title AS chapter_title FROM questions q
                JOIN chapters c ON c.id = q.chapter_id
                WHERE q.is_active = 1 ORDER BY q.id LIMIT 1 OFFSET ' . $offset);
            $stmt->execute();
            $q = $stmt->fetch();
            $row = review_question($q);
            $row['chapter_title'] = $q['chapter_title'];
            json_ok(['date' => date('Y-m-d'), 'question' => $row]);
            break;
        }

        case 'analytics': {
            $user = require_user();
            $uid = (int)$user['id'];
            $tot = db()->prepare(
                'SELECT COUNT(DISTINCT a.id) AS attempts,
                        COUNT(aa.id) AS answered,
                        SUM(aa.is_correct) AS correct
                   FROM quiz_attempts a
                   LEFT JOIN attempt_answers aa ON aa.attempt_id = a.id
                  WHERE a.user_id = ?'
            );
            $tot->execute([$uid]);
            $t = $tot->fetch();
            $answered = (int)($t['answered'] ?? 0);
            $correct = (int)($t['correct'] ?? 0);
            $days = [];
            $stmt = db()->prepare(
                'SELECT DATE(a.created_at) AS d, COUNT(aa.id) AS answered
                   FROM quiz_attempts a
                   JOIN attempt_answers aa ON aa.attempt_id = a.id
                  WHERE a.user_id = ? AND a.created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)
                  GROUP BY DATE(a.created_at)'
            );
            $stmt->execute([$uid]);
            $byDay = [];
            foreach ($stmt->fetchAll() as $r) {
                $byDay[$r['d']] = (int)$r['answered'];
            }
            for ($i = 6; $i >= 0; $i--) {
                $d = date('Y-m-d', strtotime("-$i days"));
                $days[] = ['date' => $d, 'label' => date('D', strtotime($d)), 'answered' => $byDay[$d] ?? 0];
            }
            json_ok([
                'attempts' => (int)($t['attempts'] ?? 0),
                'answered' => $answered,
                'correct' => $correct,
                'accuracy' => $answered > 0 ? (int)round($correct * 100 / $answered) : 0,
                'coins' => (int)($user['coins'] ?? 0),
                'study_streak_days' => study_streak($uid),
                'subjects' => fetch_subjects($uid),
                'last_7_days' => $days,
            ]);
            break;
        }

        case 'profile/goal': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $user = require_user();
            $goal = (int)input('goal', 0);
            if ($goal < 5 || $goal > 500) {
                json_error('Please choose a daily goal between 5 and 500 questions.');
            }
            db()->prepare('UPDATE users SET personal_goal = ? WHERE id = ?')
                ->execute([$goal, (int)$user['id']]);
            json_ok(['daily_goal' => $goal]);
            break;
        }

        /* --------------------------------------------------- notifications */
        case 'notifications': {
            $user = current_user();
            $stmt = db()->query('SELECT id, title, body, created_at FROM notifications
                WHERE is_active = 1 ORDER BY created_at DESC, id DESC LIMIT 100');
            $items = $stmt->fetchAll();
            $readIds = [];
            if ($user) {
                try {
                    $rs = db()->prepare('SELECT notification_id FROM notification_reads WHERE user_id = ?');
                    $rs->execute([$user['id']]);
                    $readIds = array_map('intval', $rs->fetchAll(PDO::FETCH_COLUMN));
                } catch (Throwable $e) { }
            }
            foreach ($items as &$n) {
                $n['is_read'] = $user ? in_array((int)$n['id'], $readIds, true) : true;
            }
            unset($n);
            json_ok($items);
            break;
        }

        case 'notifications/read-all': {
            if ($method !== 'POST') json_error('POST required.', 405);
            $user = require_user();
            $ids = db()->query('SELECT id FROM notifications WHERE is_active = 1')->fetchAll(PDO::FETCH_COLUMN);
            $ins = db()->prepare('INSERT IGNORE INTO notification_reads (user_id, notification_id) VALUES (?, ?)');
            foreach ($ids as $nid) $ins->execute([$user['id'], (int)$nid]);
            json_ok(['marked' => count($ids)]);
            break;
        }

        case 'banners': {
            $slot = (string)input('slot', 'dashboard');
            $allowed = ['dashboard', 'dashboard_after_subjects', 'chapters', 'result', 'saved'];
            if (!in_array($slot, $allowed, true)) json_error('Unknown banner slot.');
            json_ok(banners_for($slot));
            break;
        }

        default:
            json_error('Unknown route: ' . $route, 404, 'not_found');
    }
} catch (Throwable $ex) {
    // The rollback attempt must never throw: when the database
    // connection itself is the failure, db() throws again here and the
    // raw PHP fatal page (paths, stack trace) would leak to the user.
    try {
        if (db()->inTransaction()) {
            db()->rollBack();
        }
    } catch (Throwable $ignored) { }
    error_log('API error [' . $route . ']: ' . $ex->getMessage());
    json_error('Something went wrong on the server. Please try again in a moment.', 500, 'server_error');
}

/** Subjects with per-user progress (empty progress for guests). */
function fetch_subjects(?int $userId): array
{
    $stats = [];
    if ($userId !== null) {
        foreach (user_chapter_stats($userId) as $chapterId => $s) {
            $stats[$chapterId] = $s;
        }
    }
    $chapterSubject = [];
    foreach (db()->query('SELECT id, subject_id FROM chapters WHERE is_active = 1') as $c) {
        $chapterSubject[(int)$c['id']] = (int)$c['subject_id'];
    }
    $subjectAgg = [];
    foreach ($stats as $chapterId => $s) {
        $sid = $chapterSubject[$chapterId] ?? null;
        if ($sid === null) continue;
        $subjectAgg[$sid]['attempted'] = ($subjectAgg[$sid]['attempted'] ?? 0) + $s['attempted'];
        $subjectAgg[$sid]['correct'] = ($subjectAgg[$sid]['correct'] ?? 0) + $s['correct'];
    }
    $stmt = db()->query(
        'SELECT s.*, (SELECT COUNT(*) FROM chapters c WHERE c.subject_id = s.id AND c.is_active = 1) AS chapter_count,
                      (SELECT COUNT(*) FROM questions q JOIN chapters c ON c.id = q.chapter_id
                        WHERE c.subject_id = s.id AND q.is_active = 1 AND c.is_active = 1) AS question_count
           FROM subjects s WHERE s.is_active = 1 ORDER BY s.sort_order, s.id'
    );
    $out = [];
    foreach ($stmt->fetchAll() as $s) {
        $agg = $subjectAgg[(int)$s['id']] ?? ['attempted' => 0, 'correct' => 0];
        $out[] = [
            'id' => (int)$s['id'],
            'name' => $s['name'],
            'tagline' => $s['tagline'],
            'icon' => $s['icon'],
            'color' => $s['color'],
            'chapter_count' => (int)$s['chapter_count'],
            'question_count' => (int)$s['question_count'],
            'attempted' => $agg['attempted'],
            'correct' => $agg['correct'],
            'percent' => $agg['attempted'] > 0 ? (int)round($agg['correct'] * 100 / $agg['attempted']) : 0,
        ];
    }
    return $out;
}
