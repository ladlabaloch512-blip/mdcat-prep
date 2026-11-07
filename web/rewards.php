<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('Daily Reward');
?>
<section>
    <div class="section-head"><h1>Daily Reward</h1></div>
    <p class="muted">Come back every day, claim your coins and keep your streak alive.</p>
    <div id="msg"></div>
    <div class="reward-card">
        <span class="x-k">YOUR COINS</span>
        <div class="reward-balance" id="balance">0</div>
        <p id="rewardStatus">Loading your reward...</p>
        <button class="btn primary big" id="claimBtn" type="button" disabled>Claim today's reward</button>
    </div>
</section>
<script>
document.addEventListener('DOMContentLoaded', async function () {
    if (!requireLogin()) return;
    const btn = document.getElementById('claimBtn');
    const status = document.getElementById('rewardStatus');
    const balance = document.getElementById('balance');
    function countUp(el, to) {
        const start = performance.now(), dur = 800;
        function step(now) {
            const p = Math.min(1, (now - start) / dur);
            el.textContent = Math.round(to * p);
            if (p < 1) requestAnimationFrame(step);
        }
        requestAnimationFrame(step);
    }
    async function load() {
        try {
            const d = await Api.get('rewards');
            countUp(balance, d.coins);
            if (!d.rewards_enabled) {
                status.textContent = 'Daily rewards are paused right now.';
                btn.textContent = 'Rewards paused';
                btn.disabled = true;
            } else if (d.claimed_today) {
                status.textContent = 'Claimed today. Streak: ' + d.streak_days + ' day(s) - Total claims: ' + d.total_claims;
                btn.textContent = 'Come back tomorrow';
                btn.disabled = true;
            } else {
                status.textContent = "Today's reward: " + d.daily_reward_coins + ' coins - Streak: ' + d.streak_days + ' day(s)';
                btn.textContent = 'Claim ' + d.daily_reward_coins + ' coins';
                btn.disabled = false;
            }
        } catch (err) {
            errorBox(document.getElementById('msg'), err);
        }
    }
    btn.addEventListener('click', async function () {
        btn.disabled = true;
        btn.textContent = 'Claiming...';
        try {
            const d = await Api.post('rewards/claim', {});
            countUp(balance, d.coins);
            status.textContent = 'You got ' + d.added + ' coins. Streak: ' + d.streak_days + ' day(s)';
            btn.textContent = 'Come back tomorrow';
        } catch (err) {
            errorBox(document.getElementById('msg'), err);
            load();
        }
    });
    load();
});
</script>
<?php web_foot(); ?>
