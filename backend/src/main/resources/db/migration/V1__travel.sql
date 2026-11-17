create table accounts (
    id uuid primary key,
    email text not null unique,
    display_name text not null,
    password_hash text not null,
    created_at timestamptz not null default now()
);
create table sessions (
    token_hash bytea primary key,
    account_id uuid not null references accounts(id) on delete cascade,
    expires_at timestamptz not null
);
create index sessions_expiry on sessions(expires_at);

create table trips (
    id uuid primary key,
    account_id uuid not null references accounts(id) on delete cascade,
    revision integer not null check (revision >= 1),
    plan jsonb not null,
    updated_at timestamptz not null default now()
);
create index trips_account_updated on trips(account_id, updated_at desc, id);
