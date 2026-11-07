<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Result');
?>
<section class="result-wrap">
    <div id="msg"></div>
    <div class="result-hero">
        <div class="ring big" id="scoreRing"><span id="scorePct">0%</span></div>
        <div class="score-big"><span id="scoreNum">0</span><span class="score-of"> / <span id="scoreOf">200</span></span></div>
        <p class="muted" id="resultTitle">Your Result</p>
    </div>
    <div class="result-counts">
        <div class="count ok"><strong id="cCorrect">0</strong><span>Correct</span></div>
        <div class="count bad"><strong id="cIncorrect">0</strong><span>Incorrect</span></div>
        <div class="count skip"><strong id="cSkipped">0</strong><span>Skipped</span></div>
    </div>
    <div class="chart" id="chart"></div>
    <div id="bannerResult"></div>
    <div class="result-actions">
        <button class="btn primary" id="reviewBtn" type="button">Review Mistakes</button>
        <a class="btn" id="retakeBtn" href="#">Retake Quiz</a>
        <a class="btn" href="index.php">Back to Dashboard</a>
    </div>
    <div id="reviewBox" style="display:none">
        <h2>Review Mistakes</h2>
        <div id="reviewList"></div>
    </div>
    <div id="fullReviewBox">
        <h2>Full Review</h2>
        <div id="fullList"></div>
    </div>
</section>
<script>
let result = null;
document.addEventListener('DOMContentLoaded', function () {
    markNav('chapters');
    try { result = JSON.parse(sessionStorage.getItem('mdcat_last_result') || 'null'); } catch (e) { }
    if (!result) {
        errorBox(document.getElementById('msg'), new Error('No result found. Take a quiz first.'));
        return;
    }
    const meta = result._meta || {};
    document.getElementById('resultTitle').textContent = (meta.title || 'Quiz') + ' - Result';
    document.getElementById('scoreNum').textContent = result.score;
    document.getElementById('scoreOf').textContent = result.out_of;
    document.getElementById('scorePct').textContent = result.percent + '%';
    document.getElementById('scoreRing').style.setProperty('--pct', result.percent);
    document.getElementById('cCorrect').textContent = result.correct;
    document.getElementById('cIncorrect').textContent = result.incorrect;
    document.getElementById('cSkipped').textContent = result.skipped;
    document.getElementById('chart').innerHTML = result.review.map(function (r, i) {
        const cls = r.selected_option == null ? 'skip' : (r.is_correct ? 'ok' : 'bad');
        return '<span class="bar-cell ' + cls + '" title="Question ' + (i + 1) + '"></span>';
    }).join('') + '<p class="muted chart-note">Each block is one question: green correct, red incorrect, grey skipped.</p>';
    renderBanners(document.getElementById('bannerResult'), result.banners);
    document.getElementById('retakeBtn').href = 'quiz.php?type=' + encodeURIComponent(meta.type || 'chapter') +
        (meta.chapter_id ? '&chapter_id=' + meta.chapter_id : '');

    const mistakes = result.review.filter(r => !r.is_correct);
    document.getElementById('reviewBtn').addEventListener('click', function () {
        const box = document.getElementById('reviewBox');
        box.style.display = box.style.display === 'none' ? '' : 'none';
    });
    document.getElementById('reviewList').innerHTML = mistakes.length
        ? mistakes.map(r => reviewItem(r)).join('')
        : '<p class="muted">No mistakes. Perfect score on the questions you attempted.</p>';
    document.getElementById('fullList').innerHTML = result.review.map(r => reviewItem(r)).join('');
    bindSaveButtons();
});

function reviewItem(r) {
    const status = r.selected_option == null ? 'Skipped' : (r.is_correct ? 'Correct' : 'Incorrect');
    const cls = r.selected_option == null ? 'skip' : (r.is_correct ? 'ok' : 'bad');
    return '<div class="review-item ' + cls + '">' +
        '<div class="r-head"><strong>' + esc(r.question) + '</strong>' +
        '<button class="icon-btn save-btn" data-qid="' + r.id + '" type="button" title="Save this MCQ">' +
        (r.is_saved ? '&#9733;' : '&#9734;') + '</button></div>' +
        '<p>Your answer: <strong>' + (r.selected_option ? r.selected_option + ' - ' + esc(r.options[r.selected_option]) : 'Skipped') + '</strong>' +
        ' &nbsp;|&nbsp; Correct answer: <strong>' + r.correct_option + ' - ' + esc(r.options[r.correct_option]) + '</strong>' +
        ' &nbsp;<span class="badge ' + cls + '">' + status + '</span></p>' +
        (r.explanation ? '<p class="expl"><strong>Explanation:</strong> ' + esc(r.explanation) + '</p>' : '') +
        '</div>';
}

function bindSaveButtons() {
    document.querySelectorAll('.save-btn').forEach(function (b) {
        b.addEventListener('click', async function () {
            try {
                const r = await Api.post('saved/toggle', { question_id: parseInt(b.dataset.qid, 10) });
                document.querySelectorAll('.save-btn[data-qid="' + b.dataset.qid + '"]').forEach(x => {
                    x.innerHTML = r.saved ? '&#9733;' : '&#9734;';
                });
            } catch (err) { alert(err.message); }
        });
    });
}
</script>
<?php web_foot(); ?>
