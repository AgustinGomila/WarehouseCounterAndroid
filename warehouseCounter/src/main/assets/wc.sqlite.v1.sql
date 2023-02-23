BEGIN TRANSACTION;

CREATE TABLE IF NOT EXISTS [user] (
  [_id] bigint NOT NULL,
  [name] NVARCHAR(255) NOT NULL,
  [active] int NOT NULL,
  [password] NVARCHAR(100) NULL,
  CONSTRAINT [PK__id] PRIMARY KEY ([_id])
);
CREATE TABLE IF NOT EXISTS [item] (
  [_id] BIGINT NOT NULL,
  [description] NVARCHAR(255) NOT NULL,
  [active] INT NOT NULL,
  [price] FLOAT NULL,
  [ean] NVARCHAR(45) NOT NULL,
  [item_category_id] BIGINT NOT NULL,
  [external_id] NVARCHAR(45) NULL,
  [lot_enabled] INT NOT NULL DEFAULT 0,
  CONSTRAINT [PK__id] PRIMARY KEY ([_id])
);
CREATE TABLE IF NOT EXISTS [item_code] (
  [item_id] BIGINT NULL,
  [code] NVARCHAR(45) NULL,
  [qty] FLOAT NULL,
  [to_upload] INT NOT NULL
);
CREATE TABLE IF NOT EXISTS [item_category] (
  [_id] BIGINT NOT NULL,
  [description] NVARCHAR(255) NOT NULL,
  [active] INT NOT NULL,
  [parent_id] BIGINT NOT NULL,
  CONSTRAINT [PK__id] PRIMARY KEY ([_id])
);
CREATE TABLE IF NOT EXISTS [client] (
  [_id] BIGINT NOT NULL,
  [name] NVARCHAR(255) NOT NULL,
  [contact_name] NVARCHAR(255) NULL,
  [phone] NVARCHAR(255) NULL,
  [address] NVARCHAR(255) NULL,
  [city] NVARCHAR(255) NULL,
  [user_id] INT NULL,
  [active] INT NOT NULL,
  [latitude] FLOAT NULL,
  [longitude] FLOAT NULL,
  [country_id] INT NULL,
  [tax_number] NVARCHAR(45) NULL,
  CONSTRAINT [PK__id] PRIMARY KEY ([_id])
);
CREATE TABLE IF NOT EXISTS [lot] (
  [_id] BIGINT NOT NULL,
  [code] NVARCHAR(255) NOT NULL,
  [active] INT NOT NULL,
  CONSTRAINT [PK__id] PRIMARY KEY ([_id])
);
DROP
  INDEX IF EXISTS [IDX_name];
CREATE INDEX [IDX_name] ON [user] ([name]);
DROP
  INDEX IF EXISTS [IDX_ean];
CREATE INDEX [IDX_ean] ON [item] ([ean]);
DROP
  INDEX IF EXISTS [IDX_item_id];
DROP
  INDEX IF EXISTS [IDX_code];
CREATE INDEX [IDX_item_id] ON [item_code] ([item_id]);
CREATE INDEX [IDX_code] ON [item_code] ([code]);
DROP
  INDEX IF EXISTS [IDX_parent_id];
DROP
  INDEX IF EXISTS [IDX_description];
CREATE INDEX [IDX_parent_id] ON [item_category] ([parent_id]);
CREATE INDEX [IDX_description] ON [item_category] ([description]);
DROP
  INDEX IF EXISTS [IDX_name];
DROP
  INDEX IF EXISTS [IDX_contact_name];
CREATE INDEX [IDX_name] ON [client] ([name]);
CREATE INDEX [IDX_contact_name] ON [client] ([contact_name]);
DROP
  INDEX IF EXISTS [IDX_code];
CREATE INDEX [IDX_code] ON [lot] ([code]);

COMMIT;
