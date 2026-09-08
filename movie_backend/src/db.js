const sql = require('mssql');

const dbConfig = {
    server: process.env.DB_SERVER || 'localhost',
    port: parseInt(process.env.DB_PORT || '1433', 10),
    user: process.env.DB_USER || 'sa',
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME || 'MovieAppDb',
    options: {
        encrypt: false,
        trustServerCertificate: true
    },
    pool: { max: 10, min: 0, idleTimeoutMillis: 30000 }
};

const masterConfig = { ...dbConfig, database: 'master' };

let poolPromise = null;

async function getPool(){
    if (!poolPromise){
        poolPromise = new sql.ConnectionPool(dbConfig).connect();
    }
    return poolPromise
}

module.exports = { sql, getPool, dbConfig, masterConfig }