const express = require('express');
const { getPool } = require('../db');

const router = express.Router();

router.get('/', async (req, res) => {
  try {
    const pool = await getPool();
    const result = await pool.query('SELECT * FROM SeatConfig');

    const config = { normal: { rows: [], seatsPerRow: 0, price: 0 }, vip: { rows: [], seatsPerRow: 0, price: 0 } };
    for (const row of result.rows) {
      const key = row.seattype === 'vip' ? 'vip' : 'normal';
      config[key] = {
        rows: (row.rows || '').split(',').filter((r) => r.length > 0),
        seatsPerRow: row.seatsperrow,
        price: row.price
      };
    }

    res.json(config);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
