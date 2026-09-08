const express = require('express');
const { nanoid } = require('nanoid');
const { sql, getPool } = require('../db');

const router = express.Router();

function mapReview(row) {
  return {
    id: row.Id,
    filmId: row.FilmId,
    accountId: row.AccountId || '',
    userName: row.UserName || '',
    rating: row.Rating,
    comment: row.Comment || '',
    createAt: Number(row.CreateAt)
  };
}

router.get('/', async (req, res) => {
  try {
    const pool = await getPool();
    const { filmId } = req.query;

    const request = pool.request();
    let query = 'SELECT * FROM Reviews';
    if (filmId) {
      query += ' WHERE FilmId = @FilmId';
      request.input('FilmId', sql.NVarChar, filmId);
    }

    const result = await request.query(query);
    res.json(result.recordset.map(mapReview));
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
    const createAt = body.createAt || Date.now();

    await pool.request()
      .input('Id', sql.NVarChar, id)
      .input('FilmId', sql.NVarChar, body.filmId)
      .input('AccountId', sql.NVarChar, body.accountId || null)
      .input('UserName', sql.NVarChar, body.userName || '')
      .input('Rating', sql.Int, body.rating || 5)
      .input('Comment', sql.NVarChar(sql.MAX), body.comment || '')
      .input('CreateAt', sql.BigInt, createAt)
      .query(`
        INSERT INTO Reviews (Id, FilmId, AccountId, UserName, Rating, Comment, CreateAt)
        VALUES (@Id, @FilmId, @AccountId, @UserName, @Rating, @Comment, @CreateAt)
      `);

    res.status(201).json({ ...body, id, createAt });
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

    const result = await pool.request()
      .input('Id', sql.NVarChar, id)
      .input('Rating', sql.Int, body.rating || 5)
      .input('Comment', sql.NVarChar(sql.MAX), body.comment || '')
      .query('UPDATE Reviews SET Rating = @Rating, Comment = @Comment WHERE Id = @Id');

    if (result.rowsAffected[0] === 0) {
      return res.status(404).json({ error: 'Không tìm thấy đánh giá' });
    }

    const updated = await pool.request().input('Id', sql.NVarChar, id).query('SELECT * FROM Reviews WHERE Id = @Id');
    res.json(mapReview(updated.recordset[0]));
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
      .query('DELETE FROM Reviews WHERE Id = @Id');

    if (result.rowsAffected[0] === 0) {
      return res.status(404).json({ error: 'Không tìm thấy đánh giá' });
    }
    res.status(200).send();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
