-- user : configurator
-- usergroup : configurator

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
  quest_id character varying(20) NOT NULL
  name character varying(20) NOT NULL,
  description character varying(100),
  status DEFAULT ''REVEALED'',
  achievement_id character varying(20),
  quest_flag boolean DEFAULT false,
  quest_id_completed character varying(20) NULL,
  point_flag boolean DEFAULT false,
  point_value integer DEFAULT 0,
  use_notification boolean,
  notif_message character varying,
  CONSTRAINT quest_id PRIMARY KEY (quest_id),
  CONSTRAINT achievement_id FOREIGN KEY (achievement_id)
    REFERENCES model.achievement_data (achievement_id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT quest_id_completed FOREIGN KEY (quest_id_completed)
    REFERENCES model.quest_data (quest_id) ON UPDATE CASCADE ON DELETE CASCADE,
  CHECK (point_value >= 0)
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
    REFERENCES model.badge_data (badge_id) ON UPDATE CASCADE ON DELETE CASCADE
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
  action_id character varying(20) NOT NULL
  name character varying(20) NOT NULL,
  description character varying(100),
  point_value integer NOT NULL DEFAULT 0,
  use_notification boolean,
  notif_message character varying,
  CONSTRAINT action_id PRIMARY KEY (action_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE model.action_data OWNER TO configurator;

CREATE TABLE model.level_data
(
  level_num integer NOT NULL
  name character varying(20) NOT NULL,
  point_value integer NOT NULL DEFAULT 0,
  use_notification boolean,
  notif_message character varying,
  CONSTRAINT level_num PRIMARY KEY (level_num),
  CHECK (point_value >= 0)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE model.level_data OWNER TO configurator;

CREATE TABLE model.config_data
(
  config_id character varying(20) NOT NULL,
  name character varying(20) NOT NULL,
  description character varying(100)
  CONSTRAINT config_id PRIMARY KEY (config_id)
)
WITH (
  OIDS=FALSE
);
ALTER TABLE model.config_data OWNER TO configurator;

CREATE SCHEMA listen AUTHORIZATION configurator;
GRANT ALL ON SCHEMA listen TO configurator;

CREATE TABLE listen.game_info
(
  config_id character varying(20) NOT NULL,
  game_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT config_game_pkey PRIMARY KEY (config_id, game_id),
  CONSTRAINT game_id FOREIGN KEY (game_id)
    REFERENCES model.game_data (game_id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT config_id FOREIGN KEY (config_id)
    REFERENCES model.config_data (config_id) ON UPDATE CASCADE ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.game_info OWNER TO configurator;

CREATE TABLE listen.quest_info
(
  config_id character varying(20) NOT NULL,
  quest_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT config_quest_pkey PRIMARY KEY (config_id, quest_id),
  CONSTRAINT config_id FOREIGN KEY (config_id)
    REFERENCES model.config_data (config_id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT quest_id FOREIGN KEY (quest_id)
    REFERENCES model.quest_data (quest_id) ON UPDATE CASCADE ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.quest_info OWNER TO configurator;

CREATE TABLE listen.achievement_info
(
  config_id character varying(20) NOT NULL,
  achievement_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT config_achievement_pkey PRIMARY KEY (config_id, achievement_id),
  CONSTRAINT config_id FOREIGN KEY (config_id)
    REFERENCES model.config_data (config_id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT achievement_id FOREIGN KEY (achievement_id)
    REFERENCES model.achievement_data (achievement_id) ON UPDATE CASCADE ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.achievement_info OWNER TO configurator;

CREATE TABLE listen.badge_info
(
  config_id character varying(20) NOT NULL,
  badge_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT config_badge_pkey PRIMARY KEY (config_id, badge_id),
  CONSTRAINT config_id FOREIGN KEY (config_id)
    REFERENCES model.config_data (config_id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT badge_id FOREIGN KEY (badge_id)
    REFERENCES model.badge_data (badge_id) ON UPDATE CASCADE ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.badge_info OWNER TO configurator;

CREATE TABLE listen.action_info
(
  config_id character varying(20) NOT NULL,
  action_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT config_action_pkey PRIMARY KEY (config_id, action_id),
  CONSTRAINT config_id FOREIGN KEY (config_id)
    REFERENCES model.config_data (config_id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT action_id FOREIGN KEY (action_id)
    REFERENCES model.action_data (action_id) ON UPDATE CASCADE ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.action_info OWNER TO configurator;

CREATE TABLE listen.level_info
(
  config_id character varying(20) NOT NULL,
  level_id character varying(20) NOT NULL,
  listen_to character varying(100),
  CONSTRAINT config_level_pkey PRIMARY KEY (config_id, level_id),
  CONSTRAINT config_id FOREIGN KEY (config_id)
    REFERENCES model.config_data (config_id) ON UPDATE CASCADE ON DELETE CASCADE,
  CONSTRAINT level_id FOREIGN KEY (level_id)
    REFERENCES model.level_data (level_id) ON UPDATE CASCADE ON DELETE CASCADE
)
WITH (
  OIDS=FALSE
);
ALTER TABLE listen.level_info OWNER TO configurator;

