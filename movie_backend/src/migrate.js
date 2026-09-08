const fs = require('fs');
const path = require('path');
const sql = require('mssql');
const { dbConfig, masterConfig } = require('./db');

const SCHEMA_PATH = path.join(__dirname, 'migrations', 'schema.sql');
const SEED_PATH = path.join(__dirname, 'migrations', 'seed-data.json');

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function connectWithRetry(config, label, maxAttempts = 20, delayMs = 3000) {
  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      const pool = await new sql.ConnectionPool(config).connect();
      console.log(`[migrate] Kết nối "${label}" thành công (lần thử ${attempt}).`);
      return pool;
    } catch (err) {
      console.log(`[migrate] "${label}" chưa sẵn sàng (lần thử ${attempt}/${maxAttempts}): ${err.message}`);
      if (attempt === maxAttempts) throw err;
      await delay(delayMs);
    }
  }
}

async function ensureDatabaseExists() {
  const pool = await connectWithRetry(masterConfig, 'master');
  await pool.request().batch(`
    IF NOT EXISTS (SELECT * FROM sys.databases WHERE name = '${dbConfig.database}')
    BEGIN
      CREATE DATABASE [${dbConfig.database}];
    END
  `);
  await pool.close();
}

async function applySchema(pool) {
  const schemaSql = fs.readFileSync(SCHEMA_PATH, 'utf8');
  await pool.request().batch(schemaSql);
  console.log('[migrate] Đã áp dụng schema.');
}

async function isAlreadySeeded(pool) {
  const result = await pool.request().query('SELECT COUNT(*) AS total FROM Films');
  return result.recordset[0].total > 0;
}

async function seedData(pool) {
  const raw = fs.readFileSync(SEED_PATH, 'utf8');
  const data = JSON.parse(raw);

  // ---- Films + Genres + Casts ----
  for (const film of data.Item || []) {
    await pool.request()
      .input('Id', sql.NVarChar, film.id)
      .input('Title', sql.NVarChar, film.Title)
      .input('Description', sql.NVarChar(sql.MAX), film.Description || '')
      .input('Poster', sql.NVarChar, film.Poster || '')
      .input('Time', sql.NVarChar, film.Time || '')
      .input('Trailer', sql.NVarChar, film.Trailer || '')
      .input('Imdb', sql.Float, film.Imdb || 0)
      .input('Year', sql.Int, film.Year || 0)
      .input('Price', sql.Float, film.price || 0)
      .input('IsNowShowing', sql.Bit, film.IsNowShowing ? 1 : 0)
      .input('IsUpcoming', sql.Bit, film.IsUpcoming ? 1 : 0)
      .query(`
        INSERT INTO Films (Id, Title, Description, Poster, [Time], Trailer, Imdb, [Year], Price, IsNowShowing, IsUpcoming)
        VALUES (@Id, @Title, @Description, @Poster, @Time, @Trailer, @Imdb, @Year, @Price, @IsNowShowing, @IsUpcoming)
      `);

    for (const genre of film.Genre || []) {
      await pool.request()
        .input('FilmId', sql.NVarChar, film.id)
        .input('Genre', sql.NVarChar, genre)
        .query('INSERT INTO FilmGenres (FilmId, Genre) VALUES (@FilmId, @Genre)');
    }

    for (const cast of film.Casts || []) {
      await pool.request()
        .input('FilmId', sql.NVarChar, film.id)
        .input('Actor', sql.NVarChar, cast.Actor || '')
        .input('PicUrl', sql.NVarChar, cast.PicUrl || '')
        .query('INSERT INTO FilmCasts (FilmId, Actor, PicUrl) VALUES (@FilmId, @Actor, @PicUrl)');
    }
  }
  console.log(`[migrate] Đã seed ${data.Item?.length || 0} phim.`);

  // ---- Accounts ----
  for (const acc of data.Account || []) {
    await pool.request()
      .input('Id', sql.NVarChar, acc.id)
      .input('UserName', sql.NVarChar, acc.userName)
      .input('Password', sql.NVarChar, acc.password)
      .input('Role', sql.NVarChar, acc.role || 'user')
      .query('INSERT INTO Accounts (Id, UserName, Password, Role) VALUES (@Id, @UserName, @Password, @Role)');
  }
  console.log(`[migrate] Đã seed ${data.Account?.length || 0} tài khoản.`);

  // ---- Profiles ----
  for (const p of data.Profile || []) {
    await pool.request()
      .input('Id', sql.NVarChar, p.id)
      .input('AccountId', sql.NVarChar, p.accountId || null)
      .input('Name', sql.NVarChar, p.name || '')
      .input('DayOfBirth', sql.NVarChar, p.day_of_birth || '')
      .input('Telephone', sql.NVarChar, p.telephone || '')
      .input('Gmail', sql.NVarChar, p.gmail || '')
      .input('Avatar', sql.NVarChar(sql.MAX), p.avatar || '')
      .query(`
        INSERT INTO Profiles (Id, AccountId, Name, DayOfBirth, Telephone, Gmail, Avatar)
        VALUES (@Id, @AccountId, @Name, @DayOfBirth, @Telephone, @Gmail, @Avatar)
      `);
  }
  console.log(`[migrate] Đã seed ${data.Profile?.length || 0} hồ sơ.`);

  // ---- Reviews ----
  for (const r of data.Reviews || []) {
    await pool.request()
      .input('Id', sql.NVarChar, r.id)
      .input('FilmId', sql.NVarChar, r.filmId)
      .input('AccountId', sql.NVarChar, r.accountId || null)
      .input('UserName', sql.NVarChar, r.userName || '')
      .input('Rating', sql.Int, r.rating || 5)
      .input('Comment', sql.NVarChar(sql.MAX), r.comment || '')
      .input('CreateAt', sql.BigInt, r.createAt || Date.now())
      .query(`
        INSERT INTO Reviews (Id, FilmId, AccountId, UserName, Rating, Comment, CreateAt)
        VALUES (@Id, @FilmId, @AccountId, @UserName, @Rating, @Comment, @CreateAt)
      `);
  }
  console.log(`[migrate] Đã seed ${data.Reviews?.length || 0} đánh giá.`);

  // ---- SeatConfig (normal + vip) ----
  if (data.SeatConfig) {
    for (const type of ['normal', 'vip']) {
      const cfg = data.SeatConfig[type];
      if (!cfg) continue;
      await pool.request()
        .input('SeatType', sql.NVarChar, type)
        .input('Rows', sql.NVarChar, (cfg.rows || []).join(','))
        .input('SeatsPerRow', sql.Int, cfg.seatsPerRow || 0)
        .input('Price', sql.Float, cfg.price || 0)
        .query('INSERT INTO SeatConfig (SeatType, Rows, SeatsPerRow, Price) VALUES (@SeatType, @Rows, @SeatsPerRow, @Price)');
    }
    console.log('[migrate] Đã seed cấu hình ghế (normal + vip).');
  }

  // ---- Bookings ----
  for (const b of data.Bookings || []) {
    await pool.request()
      .input('Id', sql.NVarChar, b.id)
      .input('FilmId', sql.NVarChar, b.filmId)
      .input('AccountId', sql.NVarChar, b.accountId || null)
      .input('FilmTitle', sql.NVarChar, b.filmTitle || '')
      .input('Date', sql.NVarChar, b.date)
      .input('Time', sql.NVarChar, b.time)
      .input('Seats', sql.NVarChar, (b.seats || []).join(','))
      .input('TotalPrice', sql.Float, b.totalPrice || 0)
      .input('Status', sql.NVarChar, b.status || 'pending')
      .input('CreatedAt', sql.BigInt, b.createdAt || Date.now())
      .query(`
        INSERT INTO Bookings (Id, FilmId, AccountId, FilmTitle, [Date], [Time], Seats, TotalPrice, Status, CreatedAt)
        VALUES (@Id, @FilmId, @AccountId, @FilmTitle, @Date, @Time, @Seats, @TotalPrice, @Status, @CreatedAt)
      `);
  }
  console.log(`[migrate] Đã seed ${data.Bookings?.length || 0} lượt đặt vé.`);
}

async function runMigrations() {
  console.log('[migrate] Bắt đầu migrate database...');
  await ensureDatabaseExists();

  const pool = await connectWithRetry(dbConfig, dbConfig.database);
  await applySchema(pool);

  const seeded = await isAlreadySeeded(pool);
  if (seeded) {
    console.log('[migrate] Dữ liệu đã tồn tại, bỏ qua bước seed.');
  } else {
    await seedData(pool);
  }

  await pool.close();
  console.log('[migrate] Hoàn tất migrate.');
}

module.exports = { runMigrations };
