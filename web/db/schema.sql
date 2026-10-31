create table if not exists devices (
  device_id text primary key,
  depositor text,
  requested_plan text not null,
  pending_plan text,
  status text not null,
  expires_at timestamptz,
  agreed_at timestamptz not null,
  note text,
  app_version int,
  requested_at timestamptz not null,
  approved_at timestamptz,
  last_seen_at timestamptz,
  revoked_at timestamptz
);

create table if not exists releases (
  id serial primary key,
  version_code int not null unique,
  version_name text not null,
  blob_url text not null,
  size_bytes bigint not null,
  notes text,
  is_latest boolean not null default false,
  uploaded_at timestamptz not null
);

create table if not exists settings (
  key text primary key,
  value text not null
);

insert into settings (key, value) values
  ('price_day', '0'),
  ('price_lifetime', '0'),
  ('price_month', '0'),
  ('price_year', '0'),
  ('bank_account', ''),
  ('openchat_url', 'https://open.kakao.com/o/s1cF7MUi'),
  ('notice', ''),
  ('biz_reg_no', '')
on conflict (key) do nothing;
