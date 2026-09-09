const express = require('express');
const { nanoid } = require('nanoid');
const { OAuth2Client } = require('google-auth-library');
const { sql, getPool } = require('../db');

const router = express.Router();

const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID;
const googleClient = GOOGLE_CLIENT_ID ? new OAuth2Client(GOOGLE_CLIENT_ID) : null;

function mapAccount(row) {
  return {
    id: row.Id,
    userName: row.UserName,
    password: row.Password,
    role: row.Role
  };
}

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
    if (err.message && err.message.includes('UNIQUE')) {
      return res.status(409).json({ error: 'Tên đăng nhập đã tồn tại' });
    }
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


router.post('/google', async (req, res) => {
  try {
    const { idToken } = req.body;
    if (!idToken) {
      return res.status(400).json({ error: 'Thiếu idToken' });
    }
    if (!googleClient) {
      console.error('[google-auth] Chưa cấu hình biến môi trường GOOGLE_CLIENT_ID trên server.');
      return res.status(500).json({ error: 'Server chưa cấu hình đăng nhập Google' });
    }

    let payload;
    try {
      const ticket = await googleClient.verifyIdToken({
        idToken,
        audience: GOOGLE_CLIENT_ID
      });
      payload = ticket.getPayload();
    } catch (err) {
      console.error('[google-auth] Xác thực idToken thất bại:', err.message);
      return res.status(401).json({ error: 'idToken không hợp lệ hoặc đã hết hạn' });
    }

    if (!payload || !payload.email) {
      return res.status(401).json({ error: 'Không lấy được email từ tài khoản Google' });
    }

    const googleId = payload.sub;
    const email = payload.email;
    const displayName = payload.name || '';

    const pool = await getPool();

    const byGoogleId = await pool.request()
      .input('GoogleId', sql.NVarChar, googleId)
      .query('SELECT TOP 1 * FROM Accounts WHERE GoogleId = @GoogleId');

    let accountRow = byGoogleId.recordset[0];

    if (!accountRow) {
      const byUserName = await pool.request()
        .input('UserName', sql.NVarChar, email)
        .query('SELECT TOP 1 * FROM Accounts WHERE UserName = @UserName');

      if (byUserName.recordset[0]) {
        accountRow = byUserName.recordset[0];
        await pool.request()
          .input('Id', sql.NVarChar, accountRow.Id)
          .input('GoogleId', sql.NVarChar, googleId)
          .query('UPDATE Accounts SET GoogleId = @GoogleId WHERE Id = @Id');
      } else {
        const newId = nanoid(11);
        const randomPassword = nanoid(24);
        await pool.request()
          .input('Id', sql.NVarChar, newId)
          .input('UserName', sql.NVarChar, email)
          .input('Password', sql.NVarChar, randomPassword)
          .input('GoogleId', sql.NVarChar, googleId)
          .query(`
            INSERT INTO Accounts (Id, UserName, Password, Role, GoogleId)
            VALUES (@Id, @UserName, @Password, 'user', @GoogleId)
          `);
        accountRow = { Id: newId, UserName: email, Password: randomPassword, Role: 'user', GoogleId: googleId };
      }
    }

    const profileResult = await pool.request()
      .input('AccountId', sql.NVarChar, accountRow.Id)
      .query('SELECT TOP 1 * FROM Profiles WHERE AccountId = @AccountId');

    const profileRow = profileResult.recordset[0];

    if (!profileRow) {
      const newProfileId = nanoid(11);
      await pool.request()
        .input('Id', sql.NVarChar, newProfileId)
        .input('AccountId', sql.NVarChar, accountRow.Id)
        .input('Name', sql.NVarChar, displayName)
        .input('Gmail', sql.NVarChar, email)
        .query(`
          INSERT INTO Profiles (Id, AccountId, Name, DayOfBirth, Telephone, Gmail, Avatar)
          VALUES (@Id, @AccountId, @Name, '', '', @Gmail, '')
        `);
    } else if (!profileRow.Gmail || profileRow.Gmail.trim().length === 0) {
      await pool.request()
        .input('Id', sql.NVarChar, profileRow.Id)
        .input('Gmail', sql.NVarChar, email)
        .query('UPDATE Profiles SET Gmail = @Gmail WHERE Id = @Id');
    }

    res.json(mapAccount(accountRow));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
