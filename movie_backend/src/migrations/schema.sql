IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Films')
BEGIN
    CREATE TABLE Films (
        Id            NVARCHAR(50)  NOT NULL PRIMARY KEY,
        Title         NVARCHAR(255) NOT NULL,
        Description   NVARCHAR(MAX) NULL,
        Poster        NVARCHAR(500) NULL,
        [Time]        NVARCHAR(50)  NULL,
        Trailer       NVARCHAR(500) NULL,
        Imdb          FLOAT         NULL,
        [Year]        INT           NULL,
        Price         FLOAT         NULL,
        IsNowShowing  BIT           NOT NULL DEFAULT 1,
        IsUpcoming    BIT           NOT NULL DEFAULT 0,
        ReleaseAt     BIGINT        NULL
    );
END

IF NOT EXISTS (
    SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Films') AND name = 'ReleaseAt'
)
BEGIN
    ALTER TABLE Films ADD ReleaseAt BIGINT NULL;
END

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'FilmGenres')
BEGIN
    CREATE TABLE FilmGenres (
        Id      INT IDENTITY PRIMARY KEY,
        FilmId  NVARCHAR(50)  NOT NULL FOREIGN KEY REFERENCES Films(Id) ON DELETE CASCADE,
        Genre   NVARCHAR(100) NOT NULL
    );
END

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'FilmCasts')
BEGIN
    CREATE TABLE FilmCasts (
        Id      INT IDENTITY PRIMARY KEY,
        FilmId  NVARCHAR(50)  NOT NULL FOREIGN KEY REFERENCES Films(Id) ON DELETE CASCADE,
        Actor   NVARCHAR(255) NULL,
        PicUrl  NVARCHAR(500) NULL
    );
END

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Accounts')
BEGIN
    CREATE TABLE Accounts (
        Id        NVARCHAR(50)  NOT NULL PRIMARY KEY,
        UserName  NVARCHAR(100) NOT NULL UNIQUE,
        Password  NVARCHAR(255) NOT NULL,
        Role      NVARCHAR(20)  NOT NULL DEFAULT 'user'
    );
END

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Profiles')
BEGIN
    CREATE TABLE Profiles (
        Id           NVARCHAR(50)  NOT NULL PRIMARY KEY,
        AccountId    NVARCHAR(50)  NULL FOREIGN KEY REFERENCES Accounts(Id) ON DELETE SET NULL,
        Name         NVARCHAR(255) NULL,
        DayOfBirth   NVARCHAR(20)  NULL,
        Telephone    NVARCHAR(20)  NULL,
        Gmail        NVARCHAR(255) NULL,
        Avatar       NVARCHAR(MAX) NULL
    );
END

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Reviews')
BEGIN
    CREATE TABLE Reviews (
        Id         NVARCHAR(50)  NOT NULL PRIMARY KEY,
        FilmId     NVARCHAR(50)  NOT NULL FOREIGN KEY REFERENCES Films(Id) ON DELETE CASCADE,
        AccountId  NVARCHAR(50)  NULL,
        UserName   NVARCHAR(100) NULL,
        Rating     INT           NOT NULL DEFAULT 5,
        Comment    NVARCHAR(MAX) NULL,
        CreateAt   BIGINT        NOT NULL
    );
END

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'SeatConfig')
BEGIN
    CREATE TABLE SeatConfig (
        SeatType     NVARCHAR(20) NOT NULL PRIMARY KEY, -- 'normal' | 'vip'
        Rows         NVARCHAR(200) NOT NULL,            
        SeatsPerRow  INT NOT NULL,
        Price        FLOAT NOT NULL
    );
END

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'Bookings')
BEGIN
    CREATE TABLE Bookings (
        Id          NVARCHAR(50)  NOT NULL PRIMARY KEY,
        FilmId      NVARCHAR(50)  NOT NULL,
        AccountId   NVARCHAR(50)  NULL,
        FilmTitle   NVARCHAR(255) NULL,
        [Date]      NVARCHAR(20)  NOT NULL,
        [Time]      NVARCHAR(20)  NOT NULL,
        Seats       NVARCHAR(500) NOT NULL,             
        TotalPrice  FLOAT NOT NULL DEFAULT 0,
        Status      NVARCHAR(20)  NOT NULL DEFAULT 'pending',
        CreatedAt   BIGINT NOT NULL
    );
END

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Bookings_Film_Date_Time')
    CREATE INDEX IX_Bookings_Film_Date_Time ON Bookings(FilmId, [Date], [Time]);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Bookings_AccountId')
    CREATE INDEX IX_Bookings_AccountId ON Bookings(AccountId);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_Reviews_FilmId')
    CREATE INDEX IX_Reviews_FilmId ON Reviews(FilmId);
