const express = require('express');
const { nanoid } = require('nanoid');
const { getPool } = require('../db');

const router = express.Router();

function mapCombo(row) {
  return {
    id: row.id,
    name: row.name,
    description: row.description || '',
    price: row.price || 0,
    category: row.category,
    imageUrl: row.imageurl || '',
    isActive: !!row.isactive
  };
}

router.get('/', async (req, res) => {
  try {
    const pool = await getPool();
    const result = await pool.query('SELECT * FROM ComboItems ORDER BY Category, Name');
    res.json(result.rows.map(mapCombo));
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
      `INSERT INTO ComboItems (Id, Name, Description, Price, Category, ImageUrl, IsActive)
       VALUES ($1, $2, $3, $4, $5, $6, $7)`,
      [id, body.name || '', body.description || '', body.price || 0, body.category || 'popcorn', body.imageUrl || '', body.isActive === false ? false : true]
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
      `UPDATE ComboItems SET
        Name = $2, Description = $3, Price = $4,
        Category = $5, ImageUrl = $6, IsActive = $7
      WHERE Id = $1`,
      [id, body.name || '', body.description || '', body.price || 0, body.category || 'popcorn', body.imageUrl || '', body.isActive === false ? false : true]
    );

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Không tìm thấy sản phẩm' });
    }
    res.json({ ...body, id });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

router.delete('/:id', async (req, res) => {
  try {
    const pool = await getPool();
    const result = await pool.query('DELETE FROM ComboItems WHERE Id = $1', [req.params.id]);

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Không tìm thấy sản phẩm' });
    }
    res.status(200).send();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
