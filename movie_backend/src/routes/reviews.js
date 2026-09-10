const express = require('express');
const { nanoid } = require('nanoid');
const { getPool } = require('../db');

const router = express.Router();

function mapReview(row) {
  return {
    id: row.id,
    filmId: row.filmid,
    accountId: row.accountid || '',
    userName: row.username || '',
    rating: row.rating,
    comment: row.comment || '',
    createAt: Number(row.createat)
  };
}

router.get('/', async (req, res) => {
  try {
    const pool = await getPool();
    const { filmId } = req.query;

    let query = 'SELECT * FROM Reviews';
    let params = [];

    if (filmId) {
      query += ' WHERE FilmId = $1';
      params = [filmId];
    }

    const result = await pool.query(query, params);
    res.json(result.rows.map(mapReview));
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

    await pool.query(
      `INSERT INTO Reviews (Id, FilmId, AccountId, UserName, Rating, Comment, CreateAt)
       VALUES ($1, $2, $3, $4, $5, $6, $7)`,
      [id, body.filmId, body.accountId || null, body.userName || '', body.rating || 5, body.comment || '', createAt]
    );

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

    const result = await pool.query(
      'UPDATE Reviews SET Rating = $2, Comment = $3 WHERE Id = $1',
      [id, body.rating || 5, body.comment || '']
    );

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Không tìm thấy đánh giá' });
    }

    const updated = await pool.query('SELECT * FROM Reviews WHERE Id = $1', [id]);
    res.json(mapReview(updated.rows[0]));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

router.delete('/:id', async (req, res) => {
  try {
    const pool = await getPool();
    const result = await pool.query('DELETE FROM Reviews WHERE Id = $1', [req.params.id]);

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Không tìm thấy đánh giá' });
    }
    res.status(200).send();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
