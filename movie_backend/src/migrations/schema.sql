CREATE TABLE IF NOT EXISTS Films (
    Id            VARCHAR(50)  NOT NULL PRIMARY KEY,
    Title         VARCHAR(255) NOT NULL,
    Description   TEXT          NULL,
    Poster        VARCHAR(500) NULL,
    "Time"        VARCHAR(50)   NULL,
    Trailer       VARCHAR(500) NULL,
    Imdb          FLOAT         NULL,
    "Year"        INT           NULL,
    Price         FLOAT         NULL,
    IsNowShowing  BOOLEAN       NOT NULL DEFAULT TRUE,
    IsUpcoming    BOOLEAN       NOT NULL DEFAULT FALSE,
    ReleaseAt     BIGINT        NULL
);

CREATE TABLE IF NOT EXISTS FilmGenres (
    Id      SERIAL PRIMARY KEY,
    FilmId  VARCHAR(50)  NOT NULL REFERENCES Films(Id) ON DELETE CASCADE,
    Genre   VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS FilmCasts (
    Id      SERIAL PRIMARY KEY,
    FilmId  VARCHAR(50)  NOT NULL REFERENCES Films(Id) ON DELETE CASCADE,
    Actor   VARCHAR(255) NULL,
    PicUrl  VARCHAR(500) NULL
);

CREATE TABLE IF NOT EXISTS Accounts (
    Id        VARCHAR(50)  NOT NULL PRIMARY KEY,
    UserName  VARCHAR(100) NOT NULL UNIQUE,
    Password  VARCHAR(255) NOT NULL,
    Role      VARCHAR(20)  NOT NULL DEFAULT 'user',
    GoogleId  VARCHAR(255) NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS UX_Accounts_GoogleId ON Accounts(GoogleId) WHERE GoogleId IS NOT NULL;

CREATE TABLE IF NOT EXISTS Profiles (
    Id           VARCHAR(50)  NOT NULL PRIMARY KEY,
    AccountId    VARCHAR(50)  NULL REFERENCES Accounts(Id) ON DELETE SET NULL,
    Name         VARCHAR(255) NULL,
    DayOfBirth   VARCHAR(20)  NULL,
    Telephone    VARCHAR(20)  NULL,
    Gmail        VARCHAR(255) NULL,
    Avatar       TEXT         NULL
);

CREATE TABLE IF NOT EXISTS Reviews (
    Id         VARCHAR(50)  NOT NULL PRIMARY KEY,
    FilmId     VARCHAR(50)  NOT NULL REFERENCES Films(Id) ON DELETE CASCADE,
    AccountId  VARCHAR(50)  NULL,
    UserName   VARCHAR(100) NULL,
    Rating     INT           NOT NULL DEFAULT 5,
    Comment    TEXT          NULL,
    CreateAt   BIGINT        NOT NULL
);

CREATE TABLE IF NOT EXISTS SeatConfig (
    SeatType     VARCHAR(20) NOT NULL PRIMARY KEY,
    Rows         VARCHAR(200) NOT NULL,
    SeatsPerRow  INT NOT NULL,
    Price        FLOAT NOT NULL
);

CREATE TABLE IF NOT EXISTS Bookings (
    Id          VARCHAR(50)  NOT NULL PRIMARY KEY,
    FilmId      VARCHAR(50)  NOT NULL,
    AccountId   VARCHAR(50)  NULL,
    FilmTitle   VARCHAR(255) NULL,
    "Date"      VARCHAR(20)  NOT NULL,
    "Time"      VARCHAR(20)  NOT NULL,
    Seats       VARCHAR(500) NOT NULL,
    TotalPrice  FLOAT NOT NULL DEFAULT 0,
    Status      VARCHAR(20)  NOT NULL DEFAULT 'pending',
    CreatedAt   BIGINT NOT NULL,
    Combos      TEXT         NULL
);

CREATE INDEX IF NOT EXISTS IX_Bookings_Film_Date_Time ON Bookings(FilmId, "Date", "Time");
CREATE INDEX IF NOT EXISTS IX_Bookings_AccountId ON Bookings(AccountId);

CREATE TABLE IF NOT EXISTS ComboItems (
    Id          VARCHAR(50)  NOT NULL PRIMARY KEY,
    Name        VARCHAR(255) NOT NULL,
    Description VARCHAR(500) NULL,
    Price       FLOAT         NOT NULL DEFAULT 0,
    Category    VARCHAR(20)  NOT NULL DEFAULT 'popcorn',
    ImageUrl    VARCHAR(500) NULL,
    IsActive    BOOLEAN       NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS IX_Reviews_FilmId ON Reviews(FilmId);
