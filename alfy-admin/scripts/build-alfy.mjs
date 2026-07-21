import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const result = spawnSync(
  process.execPath,
  [path.join(root, 'node_modules/vite/bin/vite.js'), 'build', '--mode', 'production'],
  { cwd: path.join(root, 'apps/web-ele'), env: process.env, stdio: 'inherit' },
);

process.exit(result.status ?? 1);
