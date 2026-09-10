const express = require('express');
const { nanoid } = require('nanoid');
const { getPool } = require('../db');

const router = express.Router();

function mapProfile(row) {
  return {
    id: row.id,
    accountId: row.accountid || '',
    name: row.name || '',
    day_of_birth: row.dayofbirth || '',
    telephone: row.telephone || '',
    gmail: row.gmail || '',
    avatar: row.avatar || ''
  };
}

router.get('/', async (req, res) => {
  try {
    const pool = await getPool();
    const result = await pool.query('SELECT * FROM Profiles');
    res.json(result.rows.map(mapProfile));
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

    await pool.query(
      `INSERT INTO Profiles (Id, AccountId, Name, DayOfBirth, Telephone, Gmail, Avatar)
       VALUES ($1, $2, $3, $4, $5, $6, $7)`,
      [id, body.accountId || null, body.name || '', body.day_of_birth || '', body.telephone || '', body.gmail || '', body.avatar || '']
    );

    res.status(201).json({ ...body, id });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

router.put('/:id', async (req, res) => {
  try {
    const pool = await getPool();
    const { id } = req.params;
    const body = req.body;

    const result = await pool.query(
      `UPDATE Profiles SET
        AccountId=$2, Name=$3, DayOfBirth=$4,
        Telephone=$5, Gmail=$6, Avatar=$7
      WHERE Id=$1`,
      [id, body.accountId || null, body.name || '', body.day_of_birth || '', body.telephone || '', body.gmail || '', body.avatar || '']
    );

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Không tìm thấy hồ sơ' });
    }
    res.json({ ...body, id });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
