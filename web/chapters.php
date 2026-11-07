<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Chapters');
?>
<div id="bannerChapters"></div>
<section>
    <div class="section-head"><h1 id="pageTitle">Chapters</h1><span class="muted">PMDC Syllabus</span></div>
    <div class="subjects mini" id="subjectPicker"></div>
    <div class="filters">
        <button class="chip-btn active" data-filter="all">All Chapters</button>
        <button class="chip-btn" data-filter="weak">Weak Topics</button>
        <button class="chip-btn" data-filter="solved">Solved</button>
    </div>
    <div class="chapters" id="chapters"><p class="muted">Loading chapters...</p></div>
</section>
<script>
let allChapters = [];
document.addEventListener('DOMContentLoaded', async function () {
    markNav('chapters');
    const params = new URLSearchParams(location.search);
    let subjectId = parseInt(params.get('subject_id') || '0', 10);
    try {
        const subjects = await Api.get('subjects');
        if (!subjectId && subjects.length) subjectId = subjects[0].id;
        document.getElementById('subjectPicker').innerHTML = subjects.map(function (s) {
            return '<a class="subject-tile mini-tile' + (s.id === subjectId ? ' sel' : '') +
                '" href="chapters.php?subject_id=' + s.id + '"><strong>' + esc(s.name) + '</strong>' +
                '<small>' + s.chapter_count + ' chapters</small></a>';
        }).join('');
        const subj = subjects.find(s => s.id === subjectId);
        if (subj) document.getElementById('pageTitle').textContent = subj.name + ' Chapters';
        const d = await Api.get('chapters&subject_id=' + subjectId);
        allChapters = d.chapters;
        renderBanners(document.getElementById('bannerChapters'), d.banners);
        draw('all');
    } catch (err) {
        document.getElementById('chapters').innerHTML = '<div class="error">' + esc(err.message) + '</div>';
    }
    document.querySelectorAll('.chip-btn').forEach(function (b) {
        b.addEventListener('click', function () {
            document.querySelectorAll('.chip-btn').forEach(x => x.classList.remove('active'));
            b.classList.add('active');
            draw(b.dataset.filter);
        });
    });
});
function draw(filter) {
    const list = allChapters.filter(function (c) {
        if (filter === 'weak') return c.is_weak;
        if (filter === 'solved') return c.question_count > 0 && c.attempted >= c.question_count;
        return true;
    });
    const el = document.getElementById('chapters');
    if (!list.length) {
        el.innerHTML = '<p class="muted">' + (filter === 'weak'
            ? 'No weak topics right now. Weak topics appear after at least 5 attempted MCQs in a chapter with a score below the threshold.'
            : (filter === 'solved'
                ? 'No solved chapters yet. A chapter counts as solved when you have attempted every question in it.'
                : 'No chapters yet.')) + '</p>';
        return;
    }
    el.innerHTML = list.map(function (c, i) {
        return '<a class="chapter-row" href="quiz.php?type=chapter&chapter_id=' + c.id + '">' +
            '<span class="c-num">' + String(i + 1).padStart(2, '0') + '</span>' +
            '<span class="c-main"><strong>' + esc(c.title) + '</strong>' +
            '<small>' + esc(c.subtopics || '') + '</small>' +
            '<span class="bar"><span style="width:' + c.percent + '%"></span></span></span>' +
            '<span class="c-side"><span class="c-count">' + c.question_count + ' MCQs</span>' +
            (c.is_weak ? '<span class="weak-badge">Weak</span>' : '') +
            '<span class="c-pct">' + c.percent + '%</span></span></a>';
    }).join('');
}
</script>
<?php web_foot(); ?>
