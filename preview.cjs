const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const files = new Set(['index.html', 'style.css', 'app.js', 'sw.js', 'manifest.webmanifest', 'icon.svg']);
const types = { '.html': 'text/html; charset=utf-8', '.css': 'text/css', '.js': 'text/javascript', '.webmanifest': 'application/manifest+json', '.svg': 'image/svg+xml' };
http.createServer((req, res) => {
  const name = new URL(req.url, 'http://localhost').pathname.slice(1) || 'index.html';
  if (!files.has(name)) { res.writeHead(404); return res.end(); }
  fs.readFile(path.join(__dirname, name), (err, data) => {
    if (err) { res.writeHead(500); return res.end(); }
    res.writeHead(200, { 'Content-Type': types[path.extname(name)], 'Cache-Control': 'no-cache' });
    res.end(data);
  });
}).listen(8080, '127.0.0.1', () => console.log('NAS Pass preview: http://127.0.0.1:8080'));
