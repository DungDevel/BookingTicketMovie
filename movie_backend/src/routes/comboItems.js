const express = require('express');
const { nanoid } = require('nanoid');
const { sql, getPool } = require('../db');

const router = express.Router();

function mapCombo(row) {
  return {
    id: row.Id,
    name: row.Name,
    description: row.Description || '',
    price: row.Price || 0,
    category: row.Category,
    imageUrl: row.ImageUrl || '',
    isActive: !!row.IsActive
  };
}

router.get('/', async (req, res) => {
  try {
    const pool = await getPool();
    const result = await pool.request().query('SELECT * FROM ComboItems ORDER BY Category, Name');
    res.json(result.recordset.map(mapCombo));
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

    await pool.request()
      .input('Id', sql.NVarChar, id)
      .input('Name', sql.NVarChar, body.name || '')
      .input('Description', sql.NVarChar, body.description || '')
      .input('Price', sql.Float, body.price || 0)
      .input('Category', sql.NVarChar, body.category || 'popcorn')
      .input('ImageUrl', sql.NVarChar, body.imageUrl || '')
      .input('IsActive', sql.Bit, body.isActive === false ? 0 : 1)
      .query(`
        INSERT INTO ComboItems (Id, Name, Description, Price, Category, ImageUrl, IsActive)
        VALUES (@Id, @Name, @Description, @Price, @Category, @ImageUrl, @IsActive)
      `);

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

    const result = await pool.request()
      .input('Id', sql.NVarChar, id)
      .input('Name', sql.NVarChar, body.name || '')
      .input('Description', sql.NVarChar, body.description || '')
      .input('Price', sql.Float, body.price || 0)
      .input('Category', sql.NVarChar, body.category || 'popcorn')
      .input('ImageUrl', sql.NVarChar, body.imageUrl || '')
      .input('IsActive', sql.Bit, body.isActive === false ? 0 : 1)
      .query(`
        UPDATE ComboItems SET
          Name = @Name, Description = @Description, Price = @Price,
          Category = @Category, ImageUrl = @ImageUrl, IsActive = @IsActive
        WHERE Id = @Id
      `);

    if (result.rowsAffected[0] === 0) {
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
    const result = await pool.request()
      .input('Id', sql.NVarChar, req.params.id)
      .query('DELETE FROM ComboItems WHERE Id = @Id');

    if (result.rowsAffected[0] === 0) {
      return res.status(404).json({ error: 'Không tìm thấy sản phẩm' });
    }
    res.status(200).send();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;