const express = require('express');
const { nanoid } = require('nanoid');
const { sql, getPool } = require('../db');

const router = express.Router();

function mapProfile(row) {
  return {
    id: row.Id,
    accountId: row.AccountId || '',
    name: row.Name || '',
    day_of_birth: row.DayOfBirth || '',
    telephone: row.Telephone || '',
    gmail: row.Gmail || '',
    avatar: row.Avatar || ''
  };
}

// GET /Profile
router.get('/', async (req, res) => {
  try {
    const pool = await getPool();
    const result = await pool.request().query('SELECT * FROM Profiles');
    res.json(result.recordset.map(mapProfile));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

// POST /Profile
router.post('/', async (req, res) => {
  try {
    const pool = await getPool();
    const body = req.body;
    const id = body.id && body.id.length > 0 ? body.id : nanoid(11);

    await pool.request()
      .input('Id', sql.NVarChar, id)
      .input('AccountId', sql.NVarChar, body.accountId || null)
      .input('Name', sql.NVarChar, body.name || '')
      .input('DayOfBirth', sql.NVarChar, body.day_of_birth || '')
      .input('Telephone', sql.NVarChar, body.telephone || '')
      .input('Gmail', sql.NVarChar, body.gmail || '')
      .input('Avatar', sql.NVarChar(sql.MAX), body.avatar || '')
      .query(`
        INSERT INTO Profiles (Id, AccountId, Name, DayOfBirth, Telephone, Gmail, Avatar)
        VALUES (@Id, @AccountId, @Name, @DayOfBirth, @Telephone, @Gmail, @Avatar)
      `);

    res.status(201).json({ ...body, id });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

// PUT /Profile/:id
router.put('/:id', async (req, res) => {
  try {
    const pool = await getPool();
    const { id } = req.params;
    const body = req.body;

    const result = await pool.request()
      .input('Id', sql.NVarChar, id)
      .input('AccountId', sql.NVarChar, body.accountId || null)
      .input('Name', sql.NVarChar, body.name || '')
      .input('DayOfBirth', sql.NVarChar, body.day_of_birth || '')
      .input('Telephone', sql.NVarChar, body.telephone || '')
      .input('Gmail', sql.NVarChar, body.gmail || '')
      .input('Avatar', sql.NVarChar(sql.MAX), body.avatar || '')
      .query(`
        UPDATE Profiles SET
          AccountId=@AccountId, Name=@Name, DayOfBirth=@DayOfBirth,
          Telephone=@Telephone, Gmail=@Gmail, Avatar=@Avatar
        WHERE Id=@Id
      `);

    if (result.rowsAffected[0] === 0) {
      return res.status(404).json({ error: 'Không tìm thấy hồ sơ' });
    }
    res.json({ ...body, id });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
