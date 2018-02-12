CREATE CLASS User;
CREATE PROPERTY User.username STRING;
CREATE INDEX username_ind On User (username) UNIQUE STRING;
CREATE PROPERTY User.password STRING;
CREATE PROPERTY User.role STRING;

INSERT INTO User(username, password, role) VALUES ('admin', 'admin', 'ADMIN');
INSERT INTO User(username, password, role) VALUES ('user', 'user', 'USER');
INSERT INTO User(username, password, role) VALUES ('powereduser', 'powereduser', 'POWERED_USER');