const express = require('express');
const { nanoid } = require('nanoid');
const { OAuth2Client } = require('google-auth-library');
const { getPool } = require('../db');

const router = express.Router();

const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID;
const googleClient = GOOGLE_CLIENT_ID ? new OAuth2Client(GOOGLE_CLIENT_ID) : null;

function mapAccount(row) {
  return {
    id: row.id,
    userName: row.username,
    password: row.password,
    role: row.role
  };
}

router.get('/', async (req, res) => {
  try {
    const pool = await getPool();
    const result = await pool.query('SELECT * FROM Accounts');
    res.json(result.rows.map(mapAccount));
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
      'INSERT INTO Accounts (Id, UserName, Password, Role) VALUES ($1, $2, $3, $4)',
      [id, body.userName, body.password, body.role || 'user']
    );

    res.status(201).json({ id, userName: body.userName, password: body.password, role: body.role || 'user' });
  } catch (err) {
    console.error(err);
    if (err.message && (err.message.includes('unique') || err.message.includes('duplicate'))) {
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

    const result = await pool.query(
      'UPDATE Accounts SET UserName=$2, Password=$3, Role=$4 WHERE Id=$1',
      [id, body.userName, body.password, body.role]
    );

    if (result.rowCount === 0) {
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
    const result = await pool.query('DELETE FROM Accounts WHERE Id = $1', [req.params.id]);

    if (result.rowCount === 0) {
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

    const byGoogleId = await pool.query('SELECT * FROM Accounts WHERE GoogleId = $1 LIMIT 1', [googleId]);

    let accountRow = byGoogleId.rows[0];

    if (!accountRow) {
      const byUserName = await pool.query('SELECT * FROM Accounts WHERE UserName = $1 LIMIT 1', [email]);

      if (byUserName.rows[0]) {
        accountRow = byUserName.rows[0];
        await pool.query('UPDATE Accounts SET GoogleId = $2 WHERE Id = $1', [accountRow.id, googleId]);
      } else {
        const newId = nanoid(11);
        const randomPassword = nanoid(24);
        await pool.query(
          `INSERT INTO Accounts (Id, UserName, Password, Role, GoogleId)
           VALUES ($1, $2, $3, 'user', $4)`,
          [newId, email, randomPassword, googleId]
        );
        accountRow = { id: newId, username: email, password: randomPassword, role: 'user', googleid: googleId };
      }
    }

    const profileResult = await pool.query('SELECT * FROM Profiles WHERE AccountId = $1 LIMIT 1', [accountRow.id]);

    const profileRow = profileResult.rows[0];

    if (!profileRow) {
      const newProfileId = nanoid(11);
      await pool.query(
        `INSERT INTO Profiles (Id, AccountId, Name, DayOfBirth, Telephone, Gmail, Avatar)
         VALUES ($1, $2, $3, '', '', $4, '')`,
        [newProfileId, accountRow.id, displayName, email]
      );
    } else if (!profileRow.gmail || profileRow.gmail.trim().length === 0) {
      await pool.query('UPDATE Profiles SET Gmail = $2 WHERE Id = $1', [profileRow.id, email]);
    }

    res.json(mapAccount(accountRow));
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
