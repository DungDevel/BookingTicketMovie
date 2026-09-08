const express = require('express');
const { nanoid } = require('nanoid');
const { sql, getPool } = require('../db');

const router = express.Router();

function mapAccount(row) {
  return {
    id: row.Id,
    userName: row.UserName,
    password: row.Password,
    role: row.Role
  };
}

// GET /Account
router.get('/', async (req, res) => {
  try {
    const pool = await getPool();
    const result = await pool.request().query('SELECT * FROM Accounts');
    res.json(result.recordset.map(mapAccount));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

// POST /Account — đăng ký
router.post('/', async (req, res) => {
  try {
    const pool = await getPool();
    const body = req.body;
    const id = body.id && body.id.length > 0 ? body.id : nanoid(11);

    await pool.request()
      .input('Id', sql.NVarChar, id)
      .input('UserName', sql.NVarChar, body.userName)
      .input('Password', sql.NVarChar, body.password)
      .input('Role', sql.NVarChar, body.role || 'user')
      .query('INSERT INTO Accounts (Id, UserName, Password, Role) VALUES (@Id, @UserName, @Password, @Role)');

    res.status(201).json({ id, userName: body.userName, password: body.password, role: body.role || 'user' });
  } catch (err) {
    console.error(err);
    // Lỗi trùng UserName (UNIQUE constraint) -> trả 409 thay vì 500 chung chung.
    if (err.message && err.message.includes('UNIQUE')) {
      return res.status(409).json({ error: 'Tên đăng nhập đã tồn tại' });
    }
    res.status(500).json({ error: err.message });
  }
});

// PUT /Account/:id
router.put('/:id', async (req, res) => {
  try {
    const pool = await getPool();
    const { id } = req.params;
    const body = req.body;

    const result = await pool.request()
      .input('Id', sql.NVarChar, id)
      .input('UserName', sql.NVarChar, body.userName)
      .input('Password', sql.NVarChar, body.password)
      .input('Role', sql.NVarChar, body.role)
      .query('UPDATE Accounts SET UserName=@UserName, Password=@Password, Role=@Role WHERE Id=@Id');

    if (result.rowsAffected[0] === 0) {
      return res.status(404).json({ error: 'Không tìm thấy tài khoản' });
    }
    res.json({ id, userName: body.userName, password: body.password, role: body.role });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

// DELETE /Account/:id
router.delete('/:id', async (req, res) => {
  try {
    const pool = await getPool();
    const result = await pool.request()
      .input('Id', sql.NVarChar, req.params.id)
      .query('DELETE FROM Accounts WHERE Id = @Id');

    if (result.rowsAffected[0] === 0) {
      return res.status(404).json({ error: 'Không tìm thấy tài khoản' });
    }
    res.status(200).send();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
