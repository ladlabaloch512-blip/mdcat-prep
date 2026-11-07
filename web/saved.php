<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Saved MCQs');
?>
<div id="bannerSaved"></div>
<section>
    <div class="section-head"><h1>Saved MCQs</h1></div>
    <div id="msg"></div>
    <div id="savedList"><p class="muted">Loading saved MCQs...</p></div>
</section>
<script>
document.addEventListener('DOMContentLoaded', async function () {
    markNav('saved');
    if (!requireLogin()) return;
    try {
        const d = await Api.get('saved');
        renderBanners(document.getElementById('bannerSaved'), d.banners);
        const el = document.getElementById('savedList');
        if (!d.questions.length) {
            el.innerHTML = '<p class="muted">No saved MCQs yet. Tap the star on any question in a quiz to save it here.</p>';
            return;
        }
        el.innerHTML = d.questions.map(function (q) {
            return '<div class="review-item">' +
                '<div class="r-head"><strong>' + esc(q.question) + '</strong>' +
                '<button class="icon-btn remove-btn" data-qid="' + q.id + '" type="button" title="Remove">&#9733;</button></div>' +
                '<p class="muted">' + esc(q.chapter_title || '') + (q.subtopic ? ' - ' + esc(q.subtopic) : '') + '</p>' +
                '<p>Correct answer: <strong>' + q.correct_option + ' - ' + esc(q.options[q.correct_option]) + '</strong></p>' +
                (q.explanation ? '<p class="expl"><strong>Explanation:</strong> ' + esc(q.explanation) + '</p>' : '') +
                '</div>';
        }).join('');
        document.querySelectorAll('.remove-btn').forEach(function (b) {
            b.addEventListener('click', async function () {
                await Api.post('saved/toggle', { question_id: parseInt(b.dataset.qid, 10) });
                b.closest('.review-item').remove();
            });
        });
    } catch (err) {
        errorBox(document.getElementById('msg'), err);
        document.getElementById('savedList').innerHTML = '';
    }
});
</script>
<?php web_foot(); ?>
