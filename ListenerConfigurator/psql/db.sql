-- user : gameadmin
-- usergroup : gameuser

CREATE SCHEMA listen AUTHORIZATION configurator;
GRANT ALL ON SCHEMA listen TO configurator;

CREATE TABLE listen.game_info
(
  game_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT game_id PRIMARY KEY (game_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.game_info OWNER TO configurator;

CREATE TABLE listen.quest_info
(
  quest_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT quest_id PRIMARY KEY (quest_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.quest_info OWNER TO configurator;

CREATE TABLE listen.achievement_info
(
  achievement_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT achievement_id PRIMARY KEY (achievement_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.achievement_info OWNER TO configurator;

CREATE TABLE listen.badge_info
(
  badge_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT badge_id PRIMARY KEY (badge_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.badge_info OWNER TO configurator;

CREATE TABLE listen.action_info
(
  action_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT action_id PRIMARY KEY (action_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.action_info OWNER TO configurator;

CREATE TABLE listen.point_info
(
  point_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT point_id PRIMARY KEY (point_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.point_info OWNER TO configurator;

CREATE TABLE listen.level_info
(
  level_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT level_id PRIMARY KEY (level_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.level_info OWNER TO configurator;

CREATE SCHEMA model AUTHORIZATION configurator;
GRANT ALL ON SCHEMA model TO configurator;

CREATE TABLE model.game_data
(
  game_id character varying(20) NOT NULL,
  community_type character varying(20),
  description character varying(100),
  CONSTRAINT game_id PRIMARY KEY (game_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE model.game_data OWNER TO configurator;

CREATE TABLE model.quest_data
(
  
)
WITH (
  OIDS=FALSE
);
ALTER TABLE model.quest_data OWNER TO configurator;

CREATE TABLE model.achievement_data
(
  achievement_id character varying(20) NOT NULL,
  name character varying(20) NOT NULL,
  description character varying(100),
  point_value integer NOT NULL DEFAULT 0,
  badge_id character varying(20),
  use_notification boolean,
  notif_message character varying,
  CONSTRAINT achievement_id PRIMARY KEY (achievement_id),
  CONSTRAINT badge_id FOREIGN KEY (badge_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE model.achievement_data OWNER TO configurator;

CREATE TABLE model.badge_data
(
  badge_id character varying(20) NOT NULL,
  name character varying(20) NOT NULL,
  description character varying(100),
  use_notification boolean,
  notif_message character varying,
  CONSTRAINT badge_id PRIMARY KEY (badge_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE model.badge_data OWNER TO configurator;

CREATE TABLE model.action_data
(
  
)
WITH (
  OIDS=FALSE
);
ALTER TABLE model.action_data OWNER TO configurator;

CREATE TABLE model.level_data
(
  
)
WITH (
  OIDS=FALSE
);
ALTER TABLE model.level_data OWNER TO configurator;
















































