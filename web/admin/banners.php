<?php
declare(strict_types=1);
require_once __DIR__ . '/_common.php';
require_admin();
handle_delete('banners', 'banners.php');

$slots = [
    'dashboard' => 'Dashboard (after Daily Progress)',
    'dashboard_after_subjects' => 'Dashboard (after Subjects)',
    'chapters' => 'Chapter Selection (top)',
    'result' => 'Result (below score)',
    'saved' => 'Saved MCQs (top)',
];
$edit = null;
if (isset($_GET['edit'])) {
    $stmt = db()->prepare('SELECT * FROM banners WHERE id = ?');
    $stmt->execute([(int)$_GET['edit']]);
    $edit = $stmt->fetch() ?: null;
}
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST' && isset($_POST['save'])) {
    csrf_check();
    $imagePath = (string)($edit['image_path'] ?? '');
    if (!empty($_FILES['image']['tmp_name']) && is_uploaded_file($_FILES['image']['tmp_name'])) {
        $ext = strtolower(pathinfo((string)$_FILES['image']['name'], PATHINFO_EXTENSION));
        if (!in_array($ext, ['png', 'jpg', 'jpeg', 'webp', 'gif'], true)) {
            flash('Banner image must be png, jpg, webp or gif.');
            header('Location: banners.php');
            exit;
        }
        $name = 'banner-' . date('YmdHis') . '-' . bin2hex(random_bytes(3)) . '.' . $ext;
        $dest = dirname(__DIR__) . '/uploads/banners/' . $name;
        if (!move_uploaded_file($_FILES['image']['tmp_name'], $dest)) {
            flash('Image upload failed. Check the uploads/banners folder permissions.');
            header('Location: banners.php');
            exit;
        }
        $imagePath = 'uploads/banners/' . $name;
    }
    if ($imagePath === '') {
        flash('Please upload a banner image.');
        header('Location: banners.php');
        exit;
    }
    $data = [
        trim((string)$_POST['title']), $imagePath, trim((string)($_POST['target_link'] ?? '')),
        (string)$_POST['placement'], (int)($_POST['sort_order'] ?? 0),
        isset($_POST['is_active']) ? 1 : 0,
        ($_POST['start_date'] ?? '') !== '' ? $_POST['start_date'] : null,
        ($_POST['end_date'] ?? '') !== '' ? $_POST['end_date'] : null,
    ];
    if (!empty($_POST['id'])) {
        $data[] = (int)$_POST['id'];
        db()->prepare('UPDATE banners SET title=?, image_path=?, target_link=?, placement=?, sort_order=?,
            is_active=?, start_date=?, end_date=? WHERE id=?')->execute($data);
        flash('Banner updated.');
    } else {
        db()->prepare('INSERT INTO banners (title, image_path, target_link, placement, sort_order,
            is_active, start_date, end_date) VALUES (?,?,?,?,?,?,?,?)')->execute($data);
        flash('Banner added.');
    }
    header('Location: banners.php');
    exit;
}

admin_head($edit ? 'Edit Banner' : 'Banners');
?>
<p class="muted">Banners are the client's own custom banners. They show on the app and website
only while active and inside their dates. A slot with no active banner collapses automatically.
There is never a banner inside the live quiz.</p>
<form method="post" class="form-card" enctype="multipart/form-data">
    <input type="hidden" name="csrf" value="<?= csrf_token() ?>">
    <input type="hidden" name="id" value="<?= (int)($edit['id'] ?? 0) ?>">
    <label>Title <input name="title" required value="<?= e($edit['title'] ?? '') ?>"></label>
    <label>Image <?php if ($edit): ?>(leave empty to keep current)<?php endif; ?>
        <input type="file" name="image" accept="image/*"></label>
    <?php if ($edit): ?><img class="banner-preview" src="../<?= e($edit['image_path']) ?>" alt=""><?php endif; ?>
    <label>Target link (optional) <input name="target_link" value="<?= e($edit['target_link'] ?? '') ?>" placeholder="https://"></label>
    <label>Placement slot
        <select name="placement">
            <?php foreach ($slots as $k => $label): ?>
            <option value="<?= $k ?>" <?= ($edit['placement'] ?? 'dashboard') === $k ? 'selected' : '' ?>><?= $label ?></option>
            <?php endforeach; ?>
        </select></label>
    <label>Order <input type="number" name="sort_order" value="<?= (int)($edit['sort_order'] ?? 0) ?>"></label>
    <label>Start date (optional) <input type="date" name="start_date" value="<?= e($edit['start_date'] ?? '') ?>"></label>
    <label>End date (optional) <input type="date" name="end_date" value="<?= e($edit['end_date'] ?? '') ?>"></label>
    <label class="check"><input type="checkbox" name="is_active" <?= ($edit['is_active'] ?? 1) ? 'checked' : '' ?>> Active</label>
    <button class="btn primary" name="save" type="submit">Save banner</button>
    <?php if ($edit): ?><a class="btn" href="banners.php">Cancel edit</a><?php endif; ?>
</form>

<table>
<tr><th>Preview</th><th>Title</th><th>Slot</th><th>Order</th><th>Active</th><th>Dates</th><th></th></tr>
<?php foreach (db()->query('SELECT * FROM banners ORDER BY placement, sort_order, id') as $b): ?>
<tr>
    <td><img class="banner-thumb" src="../<?= e($b['image_path']) ?>" alt=""></td>
    <td><?= e($b['title']) ?></td><td><?= e($slots[$b['placement']] ?? $b['placement']) ?></td>
    <td><?= (int)$b['sort_order'] ?></td><td><?= $b['is_active'] ? 'Yes' : 'No' ?></td>
    <td class="muted"><?= e(trim(($b['start_date'] ?? '') . ' to ' . ($b['end_date'] ?? ''), ' to')) ?: 'Always' ?></td>
    <td class="actions"><a class="btn small" href="banners.php?edit=<?= (int)$b['id'] ?>">Edit</a>
    <?= delete_button((int)$b['id']) ?></td>
</tr>
<?php endforeach; ?>
</table>
<?php admin_foot(); ?>
