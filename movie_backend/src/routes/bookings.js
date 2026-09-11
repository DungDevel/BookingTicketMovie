const express = require('express');
const { nanoid } = require('nanoid');
const { getPool } = require('../db');
const { sendBookingConfirmationEmail } = require('../mail');

const router = express.Router();

function mapBooking(row) {
  let combos = [];
  try {
    combos = row.combos ? JSON.parse(row.combos) : [];
  } catch (e) {
    combos = [];
  }

  return {
    id: row.id,
    filmId: row.filmid,
    accountId: row.accountid || '',
    filmTitle: row.filmtitle || '',
    date: row.Date,
    time: row.Time,
    seats: (row.seats || '').split(',').filter((s) => s.length > 0),
    totalPrice: row.totalprice || 0,
    status: row.status,
    createdAt: Number(row.createdat),
    combos
  };
}

router.get('/', async (req, res) => {
  try {
    const pool = await getPool();
    const { filmId, date, time, accountId } = req.query;

    let query = 'SELECT * FROM Bookings';
    let params = [];

    if (filmId && date && time) {
      query += ' WHERE FilmId = $1 AND "Date" = $2 AND "Time" = $3';
      params = [filmId, date, time];
    } else if (accountId) {
      query += ' WHERE AccountId = $1';
      params = [accountId];
    }

    const result = await pool.query(query, params);
    res.json(result.rows.map(mapBooking));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

router.post('/', async (req, res) => {
  try {
    const pool = await getPool();
    const body = req.body;
    const id = body.id && body.id.length > 0 ? body.id : nanoid(11);
    const createdAt = body.createdAt || Date.now();
    const combosJson = JSON.stringify(body.combos || []);

    await pool.query(
      `INSERT INTO Bookings (Id, FilmId, AccountId, FilmTitle, "Date", "Time", Seats, TotalPrice, Status, CreatedAt, Combos)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)`,
      [id, body.filmId, body.accountId || null, body.filmTitle || '', body.date, body.time, (body.seats || []).join(','), body.totalPrice || 0, body.status || 'pending', createdAt, combosJson]
    );

    res.status(201).json({ ...body, id, createdAt, status: body.status || 'pending', combos: body.combos || [] });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

async function sendTicketEmailIfPossible(pool, bookingRow) {
  if (!bookingRow.accountid) {
    console.log(`[mail] Booking ${bookingRow.id} không có AccountId, bỏ qua gửi vé.`);
    return;
  }

  const profileResult = await pool.query('SELECT Gmail FROM Profiles WHERE AccountId = $1 LIMIT 1', [bookingRow.accountid]);

  const gmail = profileResult.rows[0]?.gmail;

  await sendBookingConfirmationEmail({
    to: gmail,
    booking: mapBooking(bookingRow)
  });
}

router.patch('/:id', async (req, res) => {
  try {
    const pool = await getPool();
    const { id } = req.params;
    const { status } = req.body;

    const beforeResult = await pool.query('SELECT Status FROM Bookings WHERE Id = $1', [id]);

    if (beforeResult.rows.length === 0) {
      return res.status(404).json({ error: 'Không tìm thấy booking' });
    }
    const previousStatus = beforeResult.rows[0].status;

    const result = await pool.query('UPDATE Bookings SET Status = $2 WHERE Id = $1', [id, status]);

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Không tìm thấy booking' });
    }

    const updated = await pool.query('SELECT * FROM Bookings WHERE Id = $1', [id]);
    const updatedRow = updated.rows[0];

    res.json(mapBooking(updatedRow));

    if (status === 'confirmed' && previousStatus !== 'confirmed') {
      sendTicketEmailIfPossible(pool, updatedRow).catch((err) => {
        console.error('[mail] Gửi email vé thất bại:', err);
      });
    }
  } catch (err) {
    console.error(err);
    if (!res.headersSent) {
      res.status(500).json({ error: err.message });
    }
  }
});

router.delete('/:id', async (req, res) => {
  try {
    const pool = await getPool();
    const result = await pool.query('DELETE FROM Bookings WHERE Id = $1', [req.params.id]);

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Không tìm thấy booking' });
    }
    res.status(200).send();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;