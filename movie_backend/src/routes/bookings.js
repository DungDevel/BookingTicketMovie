const express = require('express');
const { nanoid } = require('nanoid');
const { sql, getPool } = require('../db');

const router = express.Router();

function mapBooking(row) {
  return {
    id: row.Id,
    filmId: row.FilmId,
    accountId: row.AccountId || '',
    filmTitle: row.FilmTitle || '',
    date: row.Date,
    time: row.Time,
    seats: (row.Seats || '').split(',').filter((s) => s.length > 0),
    totalPrice: row.TotalPrice || 0,
    status: row.Status,
    createdAt: Number(row.CreatedAt)
  };
}


router.get('/', async (req, res) => {
  try {
    const pool = await getPool();
    const { filmId, date, time, accountId } = req.query;

    let query = 'SELECT * FROM Bookings';
    const request = pool.request();

    if (filmId && date && time) {
      query += ' WHERE FilmId = @FilmId AND [Date] = @Date AND [Time] = @Time';
      request.input('FilmId', sql.NVarChar, filmId);
      request.input('Date', sql.NVarChar, date);
      request.input('Time', sql.NVarChar, time);
    } else if (accountId) {
      query += ' WHERE AccountId = @AccountId';
      request.input('AccountId', sql.NVarChar, accountId);
    }

    const result = await request.query(query);
    res.json(result.recordset.map(mapBooking));
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

    await pool.request()
      .input('Id', sql.NVarChar, id)
      .input('FilmId', sql.NVarChar, body.filmId)
      .input('AccountId', sql.NVarChar, body.accountId || null)
      .input('FilmTitle', sql.NVarChar, body.filmTitle || '')
      .input('Date', sql.NVarChar, body.date)
      .input('Time', sql.NVarChar, body.time)
      .input('Seats', sql.NVarChar, (body.seats || []).join(','))
      .input('TotalPrice', sql.Float, body.totalPrice || 0)
      .input('Status', sql.NVarChar, body.status || 'pending')
      .input('CreatedAt', sql.BigInt, createdAt)
      .query(`
        INSERT INTO Bookings (Id, FilmId, AccountId, FilmTitle, [Date], [Time], Seats, TotalPrice, Status, CreatedAt)
        VALUES (@Id, @FilmId, @AccountId, @FilmTitle, @Date, @Time, @Seats, @TotalPrice, @Status, @CreatedAt)
      `);

    res.status(201).json({ ...body, id, createdAt, status: body.status || 'pending' });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

router.patch('/:id', async (req, res) => {
  try {
    const pool = await getPool();
    const { id } = req.params;
    const { status } = req.body;

    const result = await pool.request()
      .input('Id', sql.NVarChar, id)
      .input('Status', sql.NVarChar, status)
      .query('UPDATE Bookings SET Status = @Status WHERE Id = @Id');

    if (result.rowsAffected[0] === 0) {
      return res.status(404).json({ error: 'Không tìm thấy booking' });
    }

    const updated = await pool.request().input('Id', sql.NVarChar, id).query('SELECT * FROM Bookings WHERE Id = @Id');
    res.json(mapBooking(updated.recordset[0]));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

router.delete('/:id', async (req, res) => {
  try {
    const pool = await getPool();
    const result = await pool.request()
      .input('Id', sql.NVarChar, req.params.id)
      .query('DELETE FROM Bookings WHERE Id = @Id');

    if (result.rowsAffected[0] === 0) {
      return res.status(404).json({ error: 'Không tìm thấy booking' });
    }
    res.status(200).send();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
