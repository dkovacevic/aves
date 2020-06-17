CREATE TABLE PushTokens (
 token varchar PRIMARY KEY,
 user_id UUID NOT NULL,
 client varchar,
 app varchar NOT NULL,
 transport varchar NOT NULL,
 time TIMESTAMP DEFAULT NOW()
)