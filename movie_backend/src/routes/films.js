const express = require('express');
const { nanoid } = require('nanoid');
const { getPool } = require('../db');

const router = express.Router();

async function promoteDueFilms(pool) {
  const now = Date.now();
  await pool.query(
    `UPDATE Films
     SET IsUpcoming = false, IsNowShowing = true
     WHERE IsUpcoming = true AND ReleaseAt IS NOT NULL AND ReleaseAt <= $1`,
    [now]
  );
}

async function fetchAllFilms(pool) {
  const [filmsResult, genresResult, castsResult] = await Promise.all([
    pool.query('SELECT * FROM Films'),
    pool.query('SELECT * FROM FilmGenres'),
    pool.query('SELECT * FROM FilmCasts')
  ]);

  const genresByFilm = {};
  for (const g of genresResult.rows) {
    (genresByFilm[g.filmid] ??= []).push(g.genre);
  }

  const castsByFilm = {};
  for (const c of castsResult.rows) {
    (castsByFilm[c.filmid] ??= []).push({ PicUrl: c.picurl || '', Actor: c.actor || '' });
  }

  return filmsResult.rows.map((f) => mapFilm(f, genresByFilm[f.id] || [], castsByFilm[f.id] || []));
}

function mapFilm(row, genres, casts) {
  return {
    id: row.id,
    Title: row.title,
    Description: row.description || '',
    Poster: row.poster || '',
    Time: row.Time || '',
    Trailer: row.trailer || '',
    Imdb: row.imdb || 0,
    Year: row.Year || 0,
    price: row.price || 0,
    Genre: genres,
    Casts: casts,
    IsNowShowing: !!row.isnowshowing,
    IsUpcoming: !!row.isupcoming,
    ReleaseAt: row.releaseat != null ? Number(row.releaseat) : null
  };
}

async function fetchOneFilm(pool, id) {
  const filmResult = await pool.query('SELECT * FROM Films WHERE Id = $1', [id]);
  const row = filmResult.rows[0];
  if (!row) return null;

  const [genresResult, castsResult] = await Promise.all([
    pool.query('SELECT Genre FROM FilmGenres WHERE FilmId = $1', [id]),
    pool.query('SELECT Actor, PicUrl FROM FilmCasts WHERE FilmId = $1', [id])
  ]);

  return mapFilm(
    row,
    genresResult.rows.map((g) => g.genre),
    castsResult.rows.map((c) => ({ PicUrl: c.picurl || '', Actor: c.actor || '' }))
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

    await pool.query(
      `INSERT INTO Films (Id, Title, Description, Poster, "Time", Trailer, Imdb, "Year", Price, IsNowShowing, IsUpcoming, ReleaseAt)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)`,
      [
        id,
        body.Title || '',
        body.Description || '',
        body.Poster || '',
        body.Time || '',
        body.Trailer || '',
        body.Imdb || 0,
        body.Year || 0,
        body.price || 0,
        isNowShowing,
        isUpcoming,
        releaseAt
      ]
    );

    for (const genre of body.Genre || []) {
      await pool.query('INSERT INTO FilmGenres (FilmId, Genre) VALUES ($1, $2)', [id, genre]);
    }
    for (const cast of body.Casts || []) {
      await pool.query('INSERT INTO FilmCasts (FilmId, Actor, PicUrl) VALUES ($1, $2, $3)', [id, cast.Actor || '', cast.PicUrl || '']);
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

    const result = await pool.query(
      `UPDATE Films SET
        Title = $2, Description = $3, Poster = $4, "Time" = $5,
        Trailer = $6, Imdb = $7, "Year" = $8, Price = $9,
        IsNowShowing = $10, IsUpcoming = $11, ReleaseAt = $12
        WHERE Id = $1`,
      [
        id,
        body.Title || '',
        body.Description || '',
        body.Poster || '',
        body.Time || '',
        body.Trailer || '',
        body.Imdb || 0,
        body.Year || 0,
        body.price || 0,
        isNowShowing,
        isUpcoming,
        releaseAt
      ]
    );

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Không tìm thấy phim' });
    }

    await pool.query('DELETE FROM FilmGenres WHERE FilmId = $1', [id]);
    await pool.query('DELETE FROM FilmCasts WHERE FilmId = $1', [id]);

    for (const genre of body.Genre || []) {
      await pool.query('INSERT INTO FilmGenres (FilmId, Genre) VALUES ($1, $2)', [id, genre]);
    }
    for (const cast of body.Casts || []) {
      await pool.query('INSERT INTO FilmCasts (FilmId, Actor, PicUrl) VALUES ($1, $2, $3)', [id, cast.Actor || '', cast.PicUrl || '']);
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
    const result = await pool.query('DELETE FROM Films WHERE Id = $1', [req.params.id]);

    if (result.rowCount === 0) {
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