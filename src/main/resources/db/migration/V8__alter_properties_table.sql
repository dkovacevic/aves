DROP TABLE Properties;

CREATE TABLE Properties (
 user_id UUID NOT NULL,
 prop_key VARCHAR NOT NULL,
 prop_value VARCHAR NOT NULL
);

CREATE UNIQUE INDEX Properties_index ON Properties (user_id, prop_key);
