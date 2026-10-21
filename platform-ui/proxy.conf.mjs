// ng serve forwards /v1 (the trade API) and /auth (auth-service) to the machine running docker compose, so the
// browser only ever talks to localhost:4200 and there is no CORS to set up.
// Point it at another backend machine with e.g. `BACKEND_HOST=10.18.64.177 npm start`; the ports match API_PORT and
// AUTH_PORT in the root .env
const host = process.env.BACKEND_HOST ?? '10.18.72.182';
const apiPort = process.env.API_PORT ?? '8081';
const authPort = process.env.AUTH_PORT ?? '8084';

export default {
  '/v1': {
    target: `http://${host}:${apiPort}`,
    secure: false,
  },
  '/auth': {
    target: `http://${host}:${authPort}`,
    secure: false,
  },
};
