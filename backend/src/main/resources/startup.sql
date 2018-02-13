CREATE CLASS User IF NOT EXISTS;

CREATE PROPERTY User.username IF NOT EXISTS STRING;
CREATE INDEX username_ind On User (username) UNIQUE STRING;
CREATE PROPERTY User.password IF NOT EXISTS STRING;
CREATE PROPERTY User.role IF NOT EXISTS STRING;

INSERT INTO User(username, password, role) VALUES ('admin', 'admin', 'ADMIN');
INSERT INTO User(username, password, role) VALUES ('user', 'user', 'USER');
INSERT INTO User(username, password, role) VALUES ('powereduser', 'powereduser', 'POWERED_USER');