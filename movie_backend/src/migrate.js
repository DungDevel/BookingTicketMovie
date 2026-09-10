const fs = require('fs');
const path = require('path');
const { nanoid } = require('nanoid');
const { getPool, query } = require('./db');

const SCHEMA_PATH = path.join(__dirname, 'migrations', 'schema.sql');
const SEED_PATH = path.join(__dirname, 'migrations', 'seed-data.json');

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function connectWithRetry(maxAttempts = 20, delayMs = 3000) {
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      const pool = await getPool();
      await pool.query('SELECT 1');
      console.log(`[migrate] Kết nối database thành công (lần thử ${attempt}).`);
      return pool;
    } catch (err) {
      console.log(`[migrate] Database chưa sẵn sàng (lần thử ${attempt}/${maxAttempts}): ${err.message}`);
      if (attempt === maxAttempts) throw err;
      await delay(delayMs);
    }
  }
}

async function applySchema(pool) {
  const schemaSql = fs.readFileSync(SCHEMA_PATH, 'utf8');
  await pool.query(schemaSql);
  console.log('[migrate] Đã áp dụng schema.');
}

async function isAlreadySeeded(pool) {
  const result = await pool.query('SELECT COUNT(*) AS total FROM Films');
  return parseInt(result.rows[0].total) > 0;
}

const DEFAULT_COMBO_ITEMS = [
  { name: 'Bắp rang bơ (nhỏ)', description: 'Bắp rang bơ size nhỏ', price: 35000, category: 'popcorn', imageUrl: 'https://res.cloudinary.com/vuyb39ll/image/upload/v1788943214/Bap.jpg' },
  { name: 'Bắp rang bơ (lớn)', description: 'Bắp rang bơ size lớn', price: 55000, category: 'popcorn', imageUrl: 'https://res.cloudinary.com/vuyb39ll/image/upload/v1788943214/Bap.jpg' },
  { name: 'Bắp phô mai (lớn)', description: 'Bắp phô mai size lớn', price: 65000, category: 'popcorn', imageUrl: 'https://res.cloudinary.com/vuyb39ll/image/upload/v1788943214/Bap.jpg' },
  { name: 'Coca-Cola', description: 'Nước ngọt có gas 32oz', price: 25000, category: 'drink', imageUrl: 'https://res.cloudinary.com/vuyb39ll/image/upload/v1788943213/NuocBap.jpg' },
  { name: 'Pepsi', description: 'Nước ngọt có gas 32oz', price: 25000, category: 'drink', imageUrl: 'https://res.cloudinary.com/vuyb39ll/image/upload/v1788943213/NuocBap.jpg' },
  { name: 'Trà đào', description: 'Trà đào mát lạnh 32oz', price: 30000, category: 'drink', imageUrl: 'https://res.cloudinary.com/vuyb39ll/image/upload/v1788943213/NuocBap.jpg' },
  { name: 'Nước suối', description: 'Nước suối tinh khiết 500ml', price: 15000, category: 'drink', imageUrl: 'https://res.cloudinary.com/vuyb39ll/image/upload/v1788943213/NuocBap.jpg' },
  { name: 'Combo 1 người', description: '1 Bắp rang bơ (lớn) + 1 Nước ngọt (lớn)', price: 79000, category: 'combo', imageUrl: 'https://res.cloudinary.com/vuyb39ll/image/upload/v1788943214/Combobap.jpg' },
  { name: 'Combo 2 người', description: '1 Bắp rang bơ (lớn) + 2 Nước ngọt (lớn)', price: 99000, category: 'combo', imageUrl: 'https://res.cloudinary.com/vuyb39ll/image/upload/v1788943214/Combobap.jpg' },
  { name: 'Combo 4 người', description: '2 Bắp rang bơ (lớn) + 4 Nước ngọt (lớn)', price: 179000, category: 'combo', imageUrl: 'https://res.cloudinary.com/vuyb39ll/image/upload/v1788943214/Combobap.jpg' }
];

async function seedComboItemsIfEmpty(pool) {
  const countResult = await pool.query('SELECT COUNT(*) AS total FROM ComboItems');
  if (parseInt(countResult.rows[0].total) > 0) {
    return;
  }

  for (const item of DEFAULT_COMBO_ITEMS) {
    await pool.query(
      `INSERT INTO ComboItems (Id, Name, Description, Price, Category, ImageUrl, IsActive)
       VALUES ($1, $2, $3, $4, $5, $6, $7)`,
      [nanoid(11), item.name, item.description, item.price, item.category, '', true]
    );
  }
  console.log(`[migrate] Đã seed ${DEFAULT_COMBO_ITEMS.length} sản phẩm bắp/nước/combo.`);
}

async function seedData(pool) {
  const raw = fs.readFileSync(SEED_PATH, 'utf8');
  const data = JSON.parse(raw);

  for (const film of data.Item || []) {
    await pool.query(
      `INSERT INTO Films (Id, Title, Description, Poster, "Time", Trailer, Imdb, "Year", Price, IsNowShowing, IsUpcoming, ReleaseAt)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)`,
      [
        film.id,
        film.Title,
        film.Description || '',
        film.Poster || '',
        film.Time || '',
        film.Trailer || '',
        film.Imdb || 0,
        film.Year || 0,
        film.price || 0,
        film.IsNowShowing ? true : false,
        film.IsUpcoming ? true : false,
        film.ReleaseAt || null
      ]
    );

    for (const genre of film.Genre || []) {
      await pool.query(
        'INSERT INTO FilmGenres (FilmId, Genre) VALUES ($1, $2)',
        [film.id, genre]
      );
    }

    for (const cast of film.Casts || []) {
      await pool.query(
        'INSERT INTO FilmCasts (FilmId, Actor, PicUrl) VALUES ($1, $2, $3)',
        [film.id, cast.Actor || '', cast.PicUrl || '']
      );
    }
  }
  console.log(`[migrate] Đã seed ${data.Item?.length || 0} phim.`);

  for (const acc of data.Account || []) {
    await pool.query(
      'INSERT INTO Accounts (Id, UserName, Password, Role) VALUES ($1, $2, $3, $4)',
      [acc.id, acc.userName, acc.password, acc.role || 'user']
    );
  }
  console.log(`[migrate] Đã seed ${data.Account?.length || 0} tài khoản.`);

  for (const p of data.Profile || []) {
    await pool.query(
      `INSERT INTO Profiles (Id, AccountId, Name, DayOfBirth, Telephone, Gmail, Avatar)
       VALUES ($1, $2, $3, $4, $5, $6, $7)`,
      [
        p.id,
        p.accountId || null,
        p.name || '',
        p.day_of_birth || '',
        p.telephone || '',
        p.gmail || '',
        p.avatar || ''
      ]
    );
  }
  console.log(`[migrate] Đã seed ${data.Profile?.length || 0} hồ sơ.`);

  for (const r of data.Reviews || []) {
    await pool.query(
      `INSERT INTO Reviews (Id, FilmId, AccountId, UserName, Rating, Comment, CreateAt)
       VALUES ($1, $2, $3, $4, $5, $6, $7)`,
      [
        r.id,
        r.filmId,
        r.accountId || null,
        r.userName || '',
        r.rating || 5,
        r.comment || '',
        r.createAt || Date.now()
      ]
    );
  }
  console.log(`[migrate] Đã seed ${data.Reviews?.length || 0} đánh giá.`);

  if (data.SeatConfig) {
    for (const type of ['normal', 'vip']) {
      const cfg = data.SeatConfig[type];
      if (!cfg) continue;
      await pool.query(
        'INSERT INTO SeatConfig (SeatType, Rows, SeatsPerRow, Price) VALUES ($1, $2, $3, $4)',
        [type, (cfg.rows || []).join(','), cfg.seatsPerRow || 0, cfg.price || 0]
      );
    }
    console.log('[migrate] Đã seed cấu hình ghế (normal + vip).');
  }

  for (const b of data.Bookings || []) {
    await pool.query(
      `INSERT INTO Bookings (Id, FilmId, AccountId, FilmTitle, "Date", "Time", Seats, TotalPrice, Status, CreatedAt)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)`,
      [
        b.id,
        b.filmId,
        b.accountId || null,
        b.filmTitle || '',
        b.date,
        b.time,
        (b.seats || []).join(','),
        b.totalPrice || 0,
        b.status || 'pending',
        b.createdAt || Date.now()
      ]
    );
  }
  console.log(`[migrate] Đã seed ${data.Bookings?.length || 0} lượt đặt vé.`);
}

async function runMigrations() {
  console.log('[migrate] Bắt đầu migrate database...');

  const pool = await connectWithRetry();
  await applySchema(pool);

  const seeded = await isAlreadySeeded(pool);
  if (seeded) {
    console.log('[migrate] Dữ liệu đã tồn tại, bỏ qua bước seed.');
  } else {
    await seedData(pool);
  }

  await seedComboItemsIfEmpty(pool);

  console.log('[migrate] Hoàn tất migrate.');
}

module.exports = { runMigrations };
