BEGIN TRANSACTION;

CREATE TABLE IF NOT EXISTS [user] (
  [_id] bigint NOT NULL,
  [name] TEXT NOT NULL,
  [active] INTEGER NOT NULL,
  [password] TEXT NULL,
  CONSTRAINT [PK__id] PRIMARY KEY ([_id])
);
CREATE TABLE IF NOT EXISTS [item] (
  [_id] INTEGER NOT NULL,
  [description] TEXT NOT NULL,
  [active] INTEGER NOT NULL,
  [price] REAL NULL,
  [ean] TEXT NOT NULL,
  [item_category_id] INTEGER NOT NULL,
  [external_id] TEXT NULL,
  [lot_enabled] INTEGER NOT NULL DEFAULT 0,
  CONSTRAINT [PK__id] PRIMARY KEY ([_id])
);
CREATE TABLE IF NOT EXISTS [item_code] (
  [_id] INTEGER NOT NULL,
  [item_id] INTEGER NULL,
  [code] TEXT NULL,
  [qty] REAL NULL,
  [to_upload] INTEGER NOT NULL,
  CONSTRAINT [PK__id] PRIMARY KEY ([_id])
);
CREATE TABLE IF NOT EXISTS [item_category] (
  [_id] INTEGER NOT NULL,
  [description] TEXT NOT NULL,
  [active] INTEGER NOT NULL,
  [parent_id] INTEGER NOT NULL,
  CONSTRAINT [PK__id] PRIMARY KEY ([_id])
);
CREATE TABLE IF NOT EXISTS [client] (
  [_id] INTEGER NOT NULL,
  [name] TEXT NOT NULL,
  [contact_name] TEXT NULL,
  [phone] TEXT NULL,
  [address] TEXT NULL,
  [city] TEXT NULL,
  [user_id] INTEGER NULL,
  [active] INTEGER NOT NULL,
  [latitude] REAL NULL,
  [longitude] REAL NULL,
  [country_id] INTEGER NULL,
  [tax_number] TEXT NULL,
  CONSTRAINT [PK__id] PRIMARY KEY ([_id])
);
CREATE TABLE IF NOT EXISTS [lot] (
  [_id] INTEGER NOT NULL,
  [code] TEXT NOT NULL,
  [active] INTEGER NOT NULL,
  CONSTRAINT [PK__id] PRIMARY KEY ([_id])
);
DROP
  INDEX IF EXISTS [IDX_user_name];
CREATE INDEX [IDX_user_name] ON [user] ([name]);
DROP
  INDEX IF EXISTS [IDX_item_ean];
CREATE INDEX [IDX_item_ean] ON [item] ([ean]);
DROP
  INDEX IF EXISTS [IDX_item_code_item_id];
DROP
  INDEX IF EXISTS [IDX_item_code_code];
CREATE INDEX [IDX_item_code_item_id] ON [item_code] ([item_id]);
CREATE INDEX [IDX_item_code_code] ON [item_code] ([code]);
DROP
  INDEX IF EXISTS [IDX_item_category_parent_id];
DROP
  INDEX IF EXISTS [IDX_item_category_description];
CREATE INDEX [IDX_item_category_parent_id] ON [item_category] ([parent_id]);
CREATE INDEX [IDX_item_category_description] ON [item_category] ([description]);
DROP
  INDEX IF EXISTS [IDX_client_name];
DROP
  INDEX IF EXISTS [IDX_client_contact_name];
CREATE INDEX [IDX_client_name] ON [client] ([name]);
CREATE INDEX [IDX_client_contact_name] ON [client] ([contact_name]);
DROP
  INDEX IF EXISTS [IDX_lot_code];
CREATE INDEX [IDX_lot_code] ON [lot] ([code]);

COMMIT;
