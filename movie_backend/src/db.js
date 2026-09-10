const { Pool } = require('pg');

const dbConfig = {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '5432', 10),
    user: process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME || 'MovieAppDb',
    ssl: process.env.DB_SSL === 'true' ? { rejectUnauthorized: false } : false,
    max: 10,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 10000,
};

let pool = null;

async function getPool() {
    if (!pool) {
        pool = new Pool(dbConfig);

        pool.on('error', (err) => {
            console.error('[db] Lỗi kết nối pool:', err);
        });
    }
    return pool;
}

async function query(text, params) {
    const client = await getPool();
    return client.query(text, params);
}

module.exports = { getPool, query, dbConfig };
