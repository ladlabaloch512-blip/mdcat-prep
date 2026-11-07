<?php
// Website shared layout. The active design (1-10) is applied by JS from
// localStorage, defaulting to the admin setting; CSS lives in themes.css.
declare(strict_types=1);
require_once __DIR__ . '/helpers.php';

const DESIGN_NAMES = [
    1 => 'Classic Green Gradient', 2 => 'Bento Scholar', 3 => 'Focus List',
    4 => 'Dark Mode Focus', 5 => 'Diagonal Energy', 6 => 'Editorial Index',
    7 => 'Soft Rounded Care', 8 => 'Split Panel Pro', 9 => 'Duotone Poster',
    10 => 'Teal Clinical Grid',
];

function web_head(string $title): void
{
    $design = setting_int('default_design');
    if ($design < 1 || $design > 10) {
        $design = 1;
    }
    echo '<!doctype html><html lang="en"><head><meta charset="utf-8">';
    echo '<meta name="viewport" content="width=device-width, initial-scale=1">';
    echo '<title>' . e($title) . ' - ' . e(setting('app_name')) . '</title>';
    echo '<link rel="icon" type="image/png" href="assets/logo.png">';
    echo '<link rel="stylesheet" href="assets/css/themes.css">';
    echo '<script>document.documentElement.dataset.design = localStorage.getItem("mdcat_design") || "'
        . $design . '";document.documentElement.dataset.palette = localStorage.getItem("mdcat_palette_" + document.documentElement.dataset.design) || "0";</script>';
    echo '</head><body>';
    echo '<header class="topbar"><a class="logo" href="index.php">'
        . '<img class="logo-img" src="assets/logo.png" alt="">' . e(setting('app_name')) . '</a>';
    echo '<nav class="topnav">'
        . '<a href="index.php">Home</a>'
        . '<a href="chapters.php">Chapters</a>'
        . '<a href="analytics.php">Analytics</a>'
        . '<a href="rewards.php">Rewards</a>'
        . '<a href="channel.php">WhatsApp Channel</a>'
        . '<a href="saved.php">Saved</a>'
        . '<a href="notifications.php">Notifications</a>'
        . '<a href="profile.php">Profile</a>'
        . '</nav>';
    echo '<span class="who" id="whoBox"></span></header>';
    echo '<main class="wrap">';
}

function web_foot(): void
{
    echo '</main>';
    echo '<nav class="bottom-nav">'
        . '<a href="index.php" data-nav="home"><span class="ni">&#8962;</span>Home</a>'
        . '<a href="chapters.php" data-nav="chapters"><span class="ni">&#9776;</span>Chapters</a>'
        . '<a href="analytics.php" data-nav="analytics"><span class="ni">&#128200;</span>Stats</a>'
        . '<a href="quiz.php?type=mock" data-nav="mock" class="mock-btn"><span class="ni">&#9654;</span>Mock</a>'
        . '<a href="saved.php" data-nav="saved"><span class="ni">&#9734;</span>Saved</a>'
        . '<a href="profile.php" data-nav="profile"><span class="ni">&#9786;</span>Profile</a>'
        . '</nav>';
    echo '<footer class="foot"><span>' . e(setting('app_name')) . ' - ' . e(setting('tagline')) . '</span>'
        . '<a href="privacy.php">Privacy Policy</a></footer>';
    echo '<script src="assets/js/api.js"></script>';
    echo '</body></html>';
}
