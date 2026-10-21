// ng serve forwards /v1 (the trade API) and /auth (auth-service) to the machine running docker compose, so the
// browser only ever talks to localhost:4200 and there is no CORS to set up.
// Point it at another backend machine with e.g. `BACKEND_HOST=10.18.64.177 npm start`; the ports match API_PORT and
// AUTH_PORT in the root .env
import { existsSync, readFileSync } from 'node:fs';

const host = process.env.BACKEND_HOST ?? '10.18.72.182';
const apiPort = process.env.API_PORT ?? '8081';
const authPort = process.env.AUTH_PORT ?? '8084';

// /logo/ticker/AAPL -> img.logo.dev with the publishable (pk_) key added here, so the key stays in the
// gitignored root .env (LOGO_DEV_CRED_KEY) or LOGO_DEV_TOKEN, never in the code
const logoToken = process.env.LOGO_DEV_TOKEN ?? readEnvFile('../.env').LOGO_DEV_CRED_KEY ?? '';

function readEnvFile(path) {
  if (!existsSync(path)) {
    return {};
  }
  return Object.fromEntries(readFileSync(path, 'utf8').split(/\r?\n/)
    .map((line) => line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/))
    .filter(Boolean)
    .map(([, key, value]) => [key, value]));
}

export default {
  '/v1': {
    target: `http://${host}:${apiPort}`,
    secure: false,
  },
  '/auth': {
    target: `http://${host}:${authPort}`,
    secure: false,
  },
  '/logo': {
    target: 'https://img.logo.dev',
    secure: true,
    changeOrigin: true,
    pathRewrite: { '^/logo/(.*)$': `/$1?token=${logoToken}&format=png&size=64` },
  },
};
