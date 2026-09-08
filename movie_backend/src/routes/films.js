const express = require('express');
const { nanoid } = require('nanoid');
const { sql, getPool } = require('../db');

const router = express.Router();

async function promoteDueFilms(pool) {
  const now = Date.now();
  await pool.request()
    .input('Now', sql.BigInt, now)
    .query(`
      UPDATE Films
      SET IsUpcoming = 0, IsNowShowing = 1
      WHERE IsUpcoming = 1 AND ReleaseAt IS NOT NULL AND ReleaseAt <= @Now
    `);
}

async function fetchAllFilms(pool) {
  const [filmsResult, genresResult, castsResult] = await Promise.all([
    pool.request().query('SELECT * FROM Films'),
    pool.request().query('SELECT * FROM FilmGenres'),
    pool.request().query('SELECT * FROM FilmCasts')
  ]);

  const genresByFilm = {};
  for (const g of genresResult.recordset) {
    (genresByFilm[g.FilmId] ??= []).push(g.Genre);
  }

  const castsByFilm = {};
  for (const c of castsResult.recordset) {
    (castsByFilm[c.FilmId] ??= []).push({ PicUrl: c.PicUrl || '', Actor: c.Actor || '' });
  }

  return filmsResult.recordset.map((f) => mapFilm(f, genresByFilm[f.Id] || [], castsByFilm[f.Id] || []));
}

function mapFilm(row, genres, casts) {
  return {
    id: row.Id,
    Title: row.Title,
    Description: row.Description || '',
    Poster: row.Poster || '',
    Time: row.Time || '',
    Trailer: row.Trailer || '',
    Imdb: row.Imdb || 0,
    Year: row.Year || 0,
    price: row.Price || 0,
    Genre: genres,
    Casts: casts,
    IsNowShowing: !!row.IsNowShowing,
    IsUpcoming: !!row.IsUpcoming,
    ReleaseAt: row.ReleaseAt != null ? Number(row.ReleaseAt) : null
  };
}

async function fetchOneFilm(pool, id) {
  const filmResult = await pool.request().input('Id', sql.NVarChar, id).query('SELECT * FROM Films WHERE Id = @Id');
  const row = filmResult.recordset[0];
  if (!row) return null;

  const [genresResult, castsResult] = await Promise.all([
    pool.request().input('FilmId', sql.NVarChar, id).query('SELECT Genre FROM FilmGenres WHERE FilmId = @FilmId'),
    pool.request().input('FilmId', sql.NVarChar, id).query('SELECT Actor, PicUrl FROM FilmCasts WHERE FilmId = @FilmId')
  ]);

  return mapFilm(
    row,
    genresResult.recordset.map((g) => g.Genre),
    castsResult.recordset.map((c) => ({ PicUrl: c.PicUrl || '', Actor: c.Actor || '' }))
  );
}

router.get('/', async (req, res) => {
  try {
    const pool = await getPool();
    await promoteDueFilms(pool);
    const films = await fetchAllFilms(pool);
    res.json(films);
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

    let isNowShowing = !!body.IsNowShowing;
    let isUpcoming = !!body.IsUpcoming;
    const releaseAt = body.ReleaseAt != null && body.ReleaseAt !== '' ? Number(body.ReleaseAt) : null;
    if (isUpcoming && releaseAt != null && releaseAt <= Date.now()) {
      isUpcoming = false;
      isNowShowing = true;
    }

    await pool.request()
      .input('Id', sql.NVarChar, id)
      .input('Title', sql.NVarChar, body.Title || '')
      .input('Description', sql.NVarChar(sql.MAX), body.Description || '')
      .input('Poster', sql.NVarChar, body.Poster || '')
      .input('Time', sql.NVarChar, body.Time || '')
      .input('Trailer', sql.NVarChar, body.Trailer || '')
      .input('Imdb', sql.Float, body.Imdb || 0)
      .input('Year', sql.Int, body.Year || 0)
      .input('Price', sql.Float, body.price || 0)
      .input('IsNowShowing', sql.Bit, isNowShowing ? 1 : 0)
      .input('IsUpcoming', sql.Bit, isUpcoming ? 1 : 0)
      .input('ReleaseAt', sql.BigInt, releaseAt)
      .query(`
        INSERT INTO Films (Id, Title, Description, Poster, [Time], Trailer, Imdb, [Year], Price, IsNowShowing, IsUpcoming, ReleaseAt)
        VALUES (@Id, @Title, @Description, @Poster, @Time, @Trailer, @Imdb, @Year, @Price, @IsNowShowing, @IsUpcoming, @ReleaseAt)
      `);

    for (const genre of body.Genre || []) {
      await pool.request()
        .input('FilmId', sql.NVarChar, id)
        .input('Genre', sql.NVarChar, genre)
        .query('INSERT INTO FilmGenres (FilmId, Genre) VALUES (@FilmId, @Genre)');
    }
    for (const cast of body.Casts || []) {
      await pool.request()
        .input('FilmId', sql.NVarChar, id)
        .input('Actor', sql.NVarChar, cast.Actor || '')
        .input('PicUrl', sql.NVarChar, cast.PicUrl || '')
        .query('INSERT INTO FilmCasts (FilmId, Actor, PicUrl) VALUES (@FilmId, @Actor, @PicUrl)');
    }

    const created = await fetchOneFilm(pool, id);
    res.status(201).json(created);
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

    let isNowShowing = !!body.IsNowShowing;
    let isUpcoming = !!body.IsUpcoming;
    const releaseAt = body.ReleaseAt != null && body.ReleaseAt !== '' ? Number(body.ReleaseAt) : null;
    if (isUpcoming && releaseAt != null && releaseAt <= Date.now()) {
      isUpcoming = false;
      isNowShowing = true;
    }

    const result = await pool.request()
      .input('Id', sql.NVarChar, id)
      .input('Title', sql.NVarChar, body.Title || '')
      .input('Description', sql.NVarChar(sql.MAX), body.Description || '')
      .input('Poster', sql.NVarChar, body.Poster || '')
      .input('Time', sql.NVarChar, body.Time || '')
      .input('Trailer', sql.NVarChar, body.Trailer || '')
      .input('Imdb', sql.Float, body.Imdb || 0)
      .input('Year', sql.Int, body.Year || 0)
      .input('Price', sql.Float, body.price || 0)
      .input('IsNowShowing', sql.Bit, isNowShowing ? 1 : 0)
      .input('IsUpcoming', sql.Bit, isUpcoming ? 1 : 0)
      .input('ReleaseAt', sql.BigInt, releaseAt)
      .query(`
        UPDATE Films SET
          Title = @Title, Description = @Description, Poster = @Poster, [Time] = @Time,
          Trailer = @Trailer, Imdb = @Imdb, [Year] = @Year, Price = @Price,
          IsNowShowing = @IsNowShowing, IsUpcoming = @IsUpcoming, ReleaseAt = @ReleaseAt
        WHERE Id = @Id
      `);

    if (result.rowsAffected[0] === 0) {
      return res.status(404).json({ error: 'Không tìm thấy phim' });
    }

    await pool.request().input('FilmId', sql.NVarChar, id).query('DELETE FROM FilmGenres WHERE FilmId = @FilmId');
    await pool.request().input('FilmId', sql.NVarChar, id).query('DELETE FROM FilmCasts WHERE FilmId = @FilmId');

    for (const genre of body.Genre || []) {
      await pool.request()
        .input('FilmId', sql.NVarChar, id)
        .input('Genre', sql.NVarChar, genre)
        .query('INSERT INTO FilmGenres (FilmId, Genre) VALUES (@FilmId, @Genre)');
    }
    for (const cast of body.Casts || []) {
      await pool.request()
        .input('FilmId', sql.NVarChar, id)
        .input('Actor', sql.NVarChar, cast.Actor || '')
        .input('PicUrl', sql.NVarChar, cast.PicUrl || '')
        .query('INSERT INTO FilmCasts (FilmId, Actor, PicUrl) VALUES (@FilmId, @Actor, @PicUrl)');
    }

    const updated = await fetchOneFilm(pool, id);
    res.json(updated);
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
      .query('DELETE FROM Films WHERE Id = @Id');

    if (result.rowsAffected[0] === 0) {
      return res.status(404).json({ error: 'Không tìm thấy phim' });
    }
    res.status(200).send();
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;
module.exports.promoteDueFilms = promoteDueFilms;
