const express = require('express');
const cors = require('cors');
const { runMigrations } = require('./migrate');
const { getPool } = require('./db');

const filmsRouter = require('./routes/films');
const accountsRouter = require('./routes/accounts');
const profilesRouter = require('./routes/profiles');
const seatConfigRouter = require('./routes/seatConfig');
const bookingsRouter = require('./routes/bookings');
const reviewsRouter = require('./routes/reviews');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json({ limit: '15mb' }));

app.use('/Item', filmsRouter);
app.use('/Account', accountsRouter);
app.use('/Profile', profilesRouter);
app.use('/SeatConfig', seatConfigRouter);
app.use('/Bookings', bookingsRouter);
app.use('/Reviews', reviewsRouter);

app.get('/health', (req, res) => res.json({ status: 'ok' }));


const PROMOTE_INTERVAL_MS = 60 * 1000;
function startAutoPromoteJob() {
  setInterval(async () => {
    try {
      const pool = await getPool();
      await filmsRouter.promoteDueFilms(pool);
    } catch (err) {
      console.error('[auto-promote] Lỗi khi tự động chuyển trạng thái phim:', err);
    }
  }, PROMOTE_INTERVAL_MS);
}

async function start() {
  try {
    await runMigrations();
  } catch (err) {
    console.error('[server] Migrate database thất bại:', err);
    process.exit(1);
  }

  startAutoPromoteJob();

  app.listen(PORT, () => {
    console.log(`[server] MovieApp API đang chạy tại http://0.0.0.0:${PORT}`);
  });
}

start();
