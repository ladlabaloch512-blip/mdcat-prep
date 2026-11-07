<?php
declare(strict_types=1);
require_once __DIR__ . '/includes/web.php';
web_head('WhatsApp Channel');
?>
<section>
    <div class="section-head"><h1>WhatsApp Channel</h1></div>
    <div class="channel-card">
        <h2>Join our WhatsApp Channel</h2>
        <p>Daily practice MCQs, important dates and preparation tips, straight on
           WhatsApp. One-way updates from our team, no group noise.</p>
    </div>
    <div id="channelAction"><p class="muted">Loading the join link...</p></div>
</section>
<script>
document.addEventListener('DOMContentLoaded', async function () {
    const box = document.getElementById('channelAction');
    try {
        const s = await Api.get('settings');
        const url = s.whatsapp_channel_url || '';
        if (!url) {
            box.innerHTML = '<button class="btn primary big block" type="button" disabled>Channel link coming soon</button>' +
                '<p class="muted">The join link has not been added yet. Please check again soon.</p>';
        } else {
            box.innerHTML = '<a class="btn primary big block" target="_blank" rel="noopener" href="' + esc(url) + '">Join the channel</a>';
        }
    } catch (err) {
        box.innerHTML = '<p class="muted">Could not load the channel link right now.</p>';
    }
});
</script>
<?php web_foot(); ?>
