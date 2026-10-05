const $ = (id) => document.getElementById(id);
let active = 0;
let imageUrl;
let database;
const status = (message) => { $('status').textContent = message; };
function show(index) {
  active = index;
  $('member').hidden = index !== 0;
  $('towel').hidden = index !== 1;
  $('memberTab').setAttribute('aria-selected', String(index === 0));
  $('towelTab').setAttribute('aria-selected', String(index === 1));
  document.querySelectorAll('.pager span').forEach((dot, i) => dot.classList.toggle('active', i === index));
}
$('memberTab').onclick = () => show(0);
$('towelTab').onclick = () => show(1);
let touch;
$('deck').addEventListener('touchstart', (event) => { const p = event.changedTouches[0]; touch = { x: p.clientX, y: p.clientY }; }, { passive: true });
$('deck').addEventListener('touchend', (event) => {
  if (!touch) return;
  const p = event.changedTouches[0], dx = p.clientX - touch.x, dy = p.clientY - touch.y;
  if (Math.abs(dx) > 55 && Math.abs(dx) > Math.abs(dy) * 1.4) show(dx < 0 ? 1 : 0);
  touch = null;
}, { passive: true });
document.addEventListener('keydown', (event) => {
  if ($('settingsDialog').open || $('zoomDialog').open || !['ArrowLeft', 'ArrowRight'].includes(event.key)) return;
  show(event.key === 'ArrowRight' ? 1 : 0);
});
function openDatabase() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('nas-pass', 1);
    request.onupgradeneeded = () => request.result.createObjectStore('images');
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}
function saveImage(blob) {
  return new Promise((resolve, reject) => {
    const tx = database.transaction('images', 'readwrite');
    const store = tx.objectStore('images');
    blob ? store.put(blob, 'towel') : store.delete('towel');
    tx.oncomplete = resolve;
    tx.onerror = () => reject(tx.error);
    tx.onabort = () => reject(tx.error);
  });
}
function displayImage(blob) {
  if (imageUrl) URL.revokeObjectURL(imageUrl);
  imageUrl = blob ? URL.createObjectURL(blob) : undefined;
  if (blob) { $('towelImage').src = imageUrl; $('zoomImage').src = imageUrl; }
  else { $('towelImage').removeAttribute('src'); $('zoomImage').removeAttribute('src'); }
  $('towelEmpty').hidden = !!blob;
  $('imageView').hidden = !blob;
  $('imageLabel').textContent = blob ? '画像を変更' : '画像を選ぶ';
}
$('imageInput').onchange = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  try {
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) throw new Error('JPEG、PNG、WebPの画像を選んでください。');
    if (file.size > 15 * 1024 * 1024) throw new Error('15MB以下の画像を選んでください。');
    const decoded = await createImageBitmap(file); decoded.close();
    if (!database) throw new Error('端末への保存が利用できません。通常のブラウザで開き直してください。');
    await saveImage(file);
    displayImage(file);
    status('タオル用画像をこの端末に保存しました。');
  } catch (error) { status(error.message || '画像を保存できませんでした。'); }
  event.target.value = '';
};
$('settings').onclick = () => $('settingsDialog').showModal();
$('closeSettings').onclick = () => $('settingsDialog').close();
$('imageView').onclick = () => $('zoomDialog').showModal();
$('closeZoom').onclick = () => $('zoomDialog').close();
$('removeImage').onclick = async () => {
  try { if (!database) throw new Error(); await saveImage(null); displayImage(null); $('settingsDialog').close(); status('タオル用画像を削除しました。'); }
  catch { status('画像の削除に失敗しました。'); }
};
async function initialize() {
  try {
    database = await openDatabase();
    const request = database.transaction('images').objectStore('images').get('towel');
    request.onsuccess = () => displayImage(request.result);
    request.onerror = () => status('保存した画像を読み込めませんでした。');
  } catch { status('端末への保存が利用できません。プライベートブラウズ設定を確認してください。'); }
  if ('serviceWorker' in navigator) navigator.serviceWorker.register('./sw.js').catch(() => {});
}
initialize();
