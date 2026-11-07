<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Quiz');
?>
<section class="quiz-wrap">
    <div class="quiz-top">
        <div>
            <h1 id="quizTitle">Quiz</h1>
            <p class="muted" id="qCounter">Loading...</p>
        </div>
        <div class="timer" id="timer">00:00</div>
    </div>
    <div class="segbar" id="segBar"></div>
    <div id="msg"></div>
    <div class="quiz-card" id="quizCard" style="display:none">
        <div class="q-head">
            <span class="q-sub" id="qSubtopic"></span>
            <button class="icon-btn" id="bookmarkBtn" type="button" title="Save this MCQ">&#9734;</button>
        </div>
        <h2 class="q-text" id="qText"></h2>
        <div class="options" id="options"></div>
        <label class="switch-row">
            <input type="checkbox" id="explToggle"> Show Explanation
        </label>
        <div class="expl-box" id="explBox" style="display:none">
            Explanations are shown with the correct answers on the result screen, in Review Mistakes.
        </div>
        <div class="quiz-nav">
            <button class="btn" id="prevBtn" type="button">&larr; Previous</button>
            <button class="btn primary" id="nextBtn" type="button">Next &rarr;</button>
        </div>
    </div>
</section>
<script>
const OPTION_STYLES = { 1: 'rows', 2: 'grid', 3: 'divided', 4: 'rows', 5: 'cut', 6: 'divided', 7: 'soft', 8: 'rows', 9: 'blocks', 10: 'rows' };
let quiz = null, idx = 0, answers = {}, startedAt = Date.now(), timerInt = null, remaining = 0;

document.addEventListener('DOMContentLoaded', async function () {
    markNav('chapters');
    const params = new URLSearchParams(location.search);
    const type = params.get('type') || 'chapter';
    const chapterId = params.get('chapter_id') || '';
    try {
        quiz = await Api.get('quiz/start&type=' + encodeURIComponent(type) +
            (chapterId ? '&chapter_id=' + encodeURIComponent(chapterId) : ''));
    } catch (err) {
        errorBox(document.getElementById('msg'), err);
        document.getElementById('qCounter').textContent = '';
        return;
    }
    document.getElementById('quizTitle').textContent = quiz.title;
    document.getElementById('options').className = 'options style-' +
        (OPTION_STYLES[document.documentElement.dataset.design] || 'rows');
    document.getElementById('quizCard').style.display = '';
    remaining = quiz.time_limit_seconds;
    drawTimer();
    timerInt = setInterval(function () {
        remaining--;
        drawTimer();
        if (remaining <= 0) { clearInterval(timerInt); submitQuiz(true); }
    }, 1000);
    document.getElementById('prevBtn').addEventListener('click', () => { if (idx > 0) { idx--; draw(); } });
    document.getElementById('nextBtn').addEventListener('click', function () {
        if (idx < quiz.questions.length - 1) { idx++; draw(); }
        else submitQuiz(false);
    });
    document.getElementById('bookmarkBtn').addEventListener('click', async function () {
        const q = quiz.questions[idx];
        try {
            const r = await Api.post('saved/toggle', { question_id: q.id });
            q.is_saved = r.saved;
            drawBookmark();
        } catch (err) { errorBox(document.getElementById('msg'), err); }
    });
    document.getElementById('explToggle').addEventListener('change', function () {
        document.getElementById('explBox').style.display = this.checked ? '' : 'none';
    });
    draw();
});

function drawTimer() {
    const s = Math.max(0, remaining);
    document.getElementById('timer').textContent =
        String(Math.floor(s / 60)).padStart(2, '0') + ':' + String(s % 60).padStart(2, '0');
    document.getElementById('timer').classList.toggle('low', s <= 30);
}

function drawBookmark() {
    const q = quiz.questions[idx];
    const b = document.getElementById('bookmarkBtn');
    b.innerHTML = q.is_saved ? '&#9733;' : '&#9734;';
    b.classList.toggle('saved', !!q.is_saved);
}

function draw() {
    const q = quiz.questions[idx];
    document.getElementById('qCounter').textContent =
        'Question ' + (idx + 1) + ' of ' + quiz.questions.length;
    document.getElementById('qSubtopic').textContent = q.subtopic || '';
    document.getElementById('qText').textContent = q.question;
    document.getElementById('segBar').innerHTML = quiz.questions.map(function (_, i) {
        return '<span class="' + (answers[quiz.questions[i].id] ? 'done' : '') + (i === idx ? ' now' : '') + '"></span>';
    }).join('');
    document.getElementById('options').innerHTML = ['A', 'B', 'C', 'D'].map(function (L) {
        const sel = answers[q.id] === L;
        return '<button type="button" class="option' + (sel ? ' selected' : '') + '" data-letter="' + L + '">' +
            '<span class="o-letter">' + L + '</span><span class="o-text">' + esc(q.options[L]) + '</span></button>';
    }).join('');
    document.querySelectorAll('.option').forEach(function (b) {
        b.addEventListener('click', function () {
            answers[q.id] = b.dataset.letter;
            draw();
        });
    });
    document.getElementById('prevBtn').disabled = idx === 0;
    document.getElementById('nextBtn').innerHTML =
        idx === quiz.questions.length - 1 ? 'Submit' : 'Next &rarr;';
    drawBookmark();
}

async function submitQuiz(auto) {
    if (!Api.token()) {
        errorBox(document.getElementById('msg'), new Error(
            'Please sign in to submit this quiz and save your result. Your answers stay on this screen.'));
        return;
    }
    clearInterval(timerInt);
    const payload = {
        type: quiz.quiz_type,
        chapter_id: quiz.chapter_id,
        time_taken_seconds: Math.round((Date.now() - startedAt) / 1000),
        answers: quiz.questions.map(q => ({ question_id: q.id, selected_option: answers[q.id] || '' }))
    };
    try {
        const result = await Api.post('quiz/submit', payload);
        result._meta = { type: quiz.quiz_type, chapter_id: quiz.chapter_id, title: quiz.title };
        sessionStorage.setItem('mdcat_last_result', JSON.stringify(result));
        location.href = 'result.php';
    } catch (err) {
        errorBox(document.getElementById('msg'), err);
    }
}
</script>
<?php web_foot(); ?>
