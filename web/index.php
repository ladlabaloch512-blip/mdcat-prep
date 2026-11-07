<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Home');
?>
<section class="hero">
    <div class="hero-text">
        <p class="greeting" id="greeting">Good morning,</p>
        <h1 id="studentName">Future Doctor</h1>
        <p class="tagline"><?= e(setting('tagline')) ?></p>
    </div>
    <div class="progress-card">
        <div class="ring" id="progressRing"><span id="progressPct">0%</span></div>
        <div>
            <h2>Daily Progress</h2>
            <p class="muted"><span id="dailyDone">0</span> of <span id="dailyGoal">50</span> MCQs completed today</p>
            <p class="keep" id="progressNote">Keep it up, future doctor</p>
        </div>
    </div>
</section>

<div class="chips-row" id="chipsRow"></div>
<div id="bannerDashboard"></div>
<div class="xrow" id="extrasRow"></div>

<section>
    <div class="section-head"><h2>Subjects</h2><span class="muted">PMDC Syllabus</span></div>
    <div class="subjects" id="subjects"><p class="muted">Loading subjects...</p></div>
</section>

<div id="bannerAfterSubjects"></div>

<a class="btn primary big block" href="quiz.php?type=mock">&#9654;&nbsp; Start Full Mock Test
    <small id="mockInfo">200 MCQs - Exam conditions</small></a>

<script>
document.addEventListener('DOMContentLoaded', async function () {
    markNav('home');
    const h = new Date().getHours();
    document.getElementById('greeting').textContent =
        h < 12 ? 'Good morning,' : (h < 17 ? 'Good afternoon,' : 'Good evening,');
    try {
        const d = await Api.get('home');
        if (d.user) document.getElementById('studentName').textContent = d.user.name;
        document.getElementById('dailyDone').textContent = d.daily_completed;
        document.getElementById('dailyGoal').textContent = d.daily_goal;
        const pct = d.daily_goal > 0 ? Math.min(100, Math.round(d.daily_completed * 100 / d.daily_goal)) : 0;
        document.getElementById('progressPct').textContent = pct + '%';
        document.getElementById('progressRing').style.setProperty('--pct', pct);
        document.getElementById('mockInfo').textContent =
            (await Api.get('settings').catch(() => ({ mock_question_count: 200 }))).mock_question_count + ' MCQs - Exam conditions';
        document.getElementById('chipsRow').innerHTML =
            '<div class="chip"><strong>' + d.daily_completed + '/' + d.daily_goal + '</strong><span>MCQs today</span></div>' +
            '<div class="chip"><strong>' + pct + '%</strong><span>Daily goal</span></div>' +
            '<div class="chip"><strong>' + d.subjects.reduce((a, s) => a + s.question_count, 0) + '</strong><span>Questions</span></div>' +
            '<div class="chip"><strong>PMDC</strong><span>Syllabus</span></div>';
        document.getElementById('subjects').innerHTML = d.subjects.map(function (s, i) {
            return '<a class="subject-tile" href="chapters.php?subject_id=' + s.id + '" style="--sc:' + esc(s.color || '#22a06b') + '">' +
                '<span class="s-num">' + String(i + 1).padStart(2, '0') + '</span>' +
                '<span class="s-icon">' + sIcon(s.icon) + '</span>' +
                '<strong>' + esc(s.name) + '</strong>' +
                '<small>' + esc(s.tagline || '') + ' - ' + s.question_count + ' MCQs</small>' +
                '<span class="bar"><span style="width:' + s.percent + '%"></span></span>' +
                '<span class="s-pct">' + s.percent + '%</span></a>';
        }).join('');
        renderBanners(document.getElementById('bannerDashboard'), d.banners_dashboard);
        renderBanners(document.getElementById('bannerAfterSubjects'), d.banners_after_subjects);
        // Daily Reward + WhatsApp Channel cards (mirror the app dashboard).
        let x = '<a class="xcard reward" href="rewards.php"><span class="x-k">DAILY REWARD</span>' +
            '<strong>' + (d.coins || 0) + ' coins</strong>' +
            '<small>' + (d.reward_claimed_today ? 'Claimed today' : 'Tap to claim ' + (d.daily_reward_coins || 0) + ' coins') + '</small></a>';
        if (d.whatsapp_channel_url) {
            x += '<a class="xcard channel" href="channel.php"><span class="x-k">WHATSAPP CHANNEL</span>' +
                '<strong>Join for daily MCQs</strong><small>Tap to open</small></a>';
        }
        document.getElementById('extrasRow').innerHTML = x;
    } catch (err) {
        document.getElementById('subjects').innerHTML = '<div class="error">' + esc(err.message) + '</div>';
    }
});
function sIcon(icon) {
    return { leaf: '&#10047;', flask: '&#9879;', atom: '&#9883;', book: '&#10002;' }[icon] || '&#10002;';
}
</script>
<?php web_foot(); ?>
