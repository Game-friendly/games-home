const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 4173;
const ROOT = __dirname;
const UPSTREAM = 'http://127.0.0.1:8080';

function sendJson(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json;charset=utf-8' });
  res.end(JSON.stringify(body));
}

function proxy(req, res) {
  const target = new URL(req.url, UPSTREAM);
  const bodyChunks = [];
  req.on('data', c => bodyChunks.push(c));
  req.on('end', () => {
    const body = Buffer.concat(bodyChunks);
    const upstreamReq = http.request(target, {
      method: req.method,
      headers: {
        ...req.headers,
        host: target.host
      }
    }, upstreamRes => {
      res.writeHead(upstreamRes.statusCode, upstreamRes.headers);
      upstreamRes.pipe(res);
    });
    upstreamReq.on('error', () => sendJson(res, 502, { code: 502, msg: '后端服务未就绪' }));
    upstreamReq.end(body);
  });
}

const server = http.createServer((req, res) => {
  if (req.url.startsWith('/api/') || req.url.startsWith('/uploads/')) {
    proxy(req, res);
    return;
  }
  const file = req.url === '/' ? 'index.html' : decodeURIComponent(req.url.slice(1));
  const filePath = path.join(ROOT, file);
  if (!filePath.startsWith(ROOT) || !fs.existsSync(filePath) || !fs.statSync(filePath).isFile()) {
    sendJson(res, 404, { code: 404, msg: 'Not found' });
    return;
  }
  const ext = path.extname(filePath).toLowerCase();
  const types = { '.html': 'text/html;charset=utf-8', '.css': 'text/css;charset=utf-8', '.js': 'text/javascript;charset=utf-8' };
  res.writeHead(200, { 'Content-Type': types[ext] || 'application/octet-stream' });
  fs.createReadStream(filePath).pipe(res);
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`preview server: http://0.0.0.0:${PORT}`);
});
