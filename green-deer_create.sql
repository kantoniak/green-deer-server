-- Created by Vertabelo (http://vertabelo.com)
-- Last modification date: 2017-09-20 18:09:41.812

-- tables
-- Table: goals
CREATE TABLE goals (
    user_id int  NOT NULL,
    distance int  NOT NULL,
    weight real  NOT NULL,
    CONSTRAINT goals_pk PRIMARY KEY (user_id)
);

-- Table: runs
CREATE TABLE runs (
    id int  NOT NULL,
    user_id int  NOT NULL,
    time_created timestamp  NOT NULL,
    distance int  NOT NULL,
    time int  NOT NULL,
    weight real  NULL,
    CONSTRAINT runs_pk PRIMARY KEY (id)
);

-- Table: users
CREATE TABLE users (
    id int  NOT NULL,
    CONSTRAINT users_pk PRIMARY KEY (id)
);

-- foreign keys
-- Reference: goals_users (table: goals)
ALTER TABLE goals ADD CONSTRAINT goals_users
    FOREIGN KEY (user_id)
    REFERENCES users (id)  
    NOT DEFERRABLE 
    INITIALLY IMMEDIATE
;

-- Reference: runs_users (table: runs)
ALTER TABLE runs ADD CONSTRAINT runs_users
    FOREIGN KEY (user_id)
    REFERENCES users (id)  
    NOT DEFERRABLE 
    INITIALLY IMMEDIATE
;

-- End of file.

