import { spawn } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const node = process.execPath;

const services = [
  spawn(
    node,
    [path.join(root, 'node_modules/vite/bin/vite.js'), '--mode', 'development'],
    {
      cwd: path.join(root, 'apps/web-ele'),
      env: process.env,
      stdio: 'inherit',
    },
  ),
];

function stop() {
  for (const service of services) service.kill('SIGTERM');
}

process.on('SIGINT', stop);
process.on('SIGTERM', stop);

for (const service of services) {
  service.on('exit', (code) => {
    if (code && code !== 0) {
      stop();
      process.exitCode = code;
    }
  });
}
