<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Analytics');
?>
<section>
    <div class="section-head"><h1>Analytics</h1></div>
    <p class="muted">Your preparation in numbers.</p>
    <div id="msg"></div>
    <div id="analyticsBox"><p class="muted">Loading analytics...</p></div>
</section>
<script>
document.addEventListener('DOMContentLoaded', async function () {
    markNav('analytics');
    if (!requireLogin()) return;
    try {
        const d = await Api.get('analytics');
        const maxDay = Math.max(1, ...d.last_7_days.map(x => x.answered));
        let h = '<div class="stat-grid">' +
            '<div class="stat-tile"><strong>' + d.accuracy + '%</strong><span>Accuracy</span></div>' +
            '<div class="stat-tile"><strong>' + d.attempts + '</strong><span>Attempts</span></div>' +
            '<div class="stat-tile"><strong>' + d.answered + '</strong><span>Answered</span></div>' +
            '<div class="stat-tile"><strong>' + d.study_streak_days + '</strong><span>Day streak</span></div>' +
            '<div class="stat-tile"><strong>' + d.coins + '</strong><span>Coins</span></div>' +
            '</div>';
        h += '<h2 class="sub-h">Accuracy by subject</h2>';
        h += d.subjects.map(function (s) {
            return '<div class="review-item"><strong>' + esc(s.name) + ' - ' + s.percent + '%</strong>' +
                '<p class="muted">' + s.attempted + ' questions attempted</p>' +
                '<span class="bar"><span style="width:' + s.percent + '%"></span></span></div>';
        }).join('');
        h += '<h2 class="sub-h">Last 7 days</h2><div class="week-chart">';
        h += d.last_7_days.map(function (x) {
            const hgt = 8 + Math.round(72 * x.answered / maxDay);
            return '<div class="wc-col"><span class="wc-val">' + x.answered + '</span>' +
                '<span class="wc-bar" style="height:' + hgt + 'px"></span>' +
                '<span class="wc-lab">' + esc(x.label) + '</span></div>';
        }).join('');
        h += '</div>';
        document.getElementById('analyticsBox').innerHTML = h;
    } catch (err) {
        errorBox(document.getElementById('msg'), err);
        document.getElementById('analyticsBox').innerHTML = '';
    }
});
</script>
<?php web_foot(); ?>
