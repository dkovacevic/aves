CREATE TABLE Connections (
 user_from UUID NOT NULL,
 user_to UUID NOT NULL,
 time TIMESTAMP DEFAULT NOW()
);