create table organizations (
    id uuid primary key,
    name varchar(200) not null,
    slug varchar(100) not null unique,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table users (
    id uuid primary key,
    email varchar(320) not null unique,
    display_name varchar(200) not null,
    password_hash varchar(255) not null,
    enabled boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table organization_memberships (
    id uuid primary key,
    organization_id uuid not null references organizations(id) on delete cascade,
    user_id uuid not null references users(id) on delete cascade,
    role varchar(30) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint organization_memberships_role_check check (role in ('ADMIN', 'TEAM_MEMBER', 'CLIENT')),
    constraint organization_memberships_unique_user_org unique (organization_id, user_id)
);

alter table clients
    add column organization_id uuid;

alter table work_requests
    add column created_by_user_id uuid;

alter table comments
    add column author_user_id uuid;

alter table request_attachments
    add column uploaded_by_user_id uuid;

insert into organizations (id, name, slug, created_at, updated_at)
values
    ('01010101-0101-0101-0101-010101010101', 'ClientDesk Demo Workspace', 'clientdesk-demo', now(), now());

insert into users (id, email, display_name, password_hash, enabled, created_at, updated_at)
values
    ('02020202-0202-0202-0202-020202020201', 'admin@clientdesk.test', 'Alex Morgan', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, now(), now()),
    ('02020202-0202-0202-0202-020202020202', 'team@clientdesk.test', 'Jamie Lee', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, now(), now()),
    ('02020202-0202-0202-0202-020202020203', 'client@clientdesk.test', 'Maya Chen', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, now(), now());

insert into organization_memberships (id, organization_id, user_id, role, created_at, updated_at)
values
    ('03030303-0303-0303-0303-030303030301', '01010101-0101-0101-0101-010101010101', '02020202-0202-0202-0202-020202020201', 'ADMIN', now(), now()),
    ('03030303-0303-0303-0303-030303030302', '01010101-0101-0101-0101-010101010101', '02020202-0202-0202-0202-020202020202', 'TEAM_MEMBER', now(), now()),
    ('03030303-0303-0303-0303-030303030303', '01010101-0101-0101-0101-010101010101', '02020202-0202-0202-0202-020202020203', 'CLIENT', now(), now());

update clients
set organization_id = '01010101-0101-0101-0101-010101010101'
where organization_id is null;

update work_requests
set created_by_user_id = case
    when requested_by = 'Maya Chen' then '02020202-0202-0202-0202-020202020203'::uuid
    else '02020202-0202-0202-0202-020202020201'::uuid
end
where created_by_user_id is null;

update comments
set author_user_id = case
    when author_name = 'Maya Chen' then '02020202-0202-0202-0202-020202020203'::uuid
    when author_name = 'Alex Morgan' then '02020202-0202-0202-0202-020202020201'::uuid
    else '02020202-0202-0202-0202-020202020202'::uuid
end
where author_user_id is null;

update request_attachments
set uploaded_by_user_id = case
    when uploaded_by = 'Maya Chen' then '02020202-0202-0202-0202-020202020203'::uuid
    when uploaded_by = 'Jamie Lee' then '02020202-0202-0202-0202-020202020202'::uuid
    else '02020202-0202-0202-0202-020202020201'::uuid
end
where uploaded_by_user_id is null;

alter table clients
    alter column organization_id set not null,
    add constraint fk_clients_organization foreign key (organization_id) references organizations(id);

alter table work_requests
    add constraint fk_work_requests_created_by_user foreign key (created_by_user_id) references users(id) on delete set null;

alter table comments
    add constraint fk_comments_author_user foreign key (author_user_id) references users(id) on delete set null;

alter table request_attachments
    add constraint fk_request_attachments_uploaded_by_user foreign key (uploaded_by_user_id) references users(id) on delete set null;

create index idx_organizations_slug on organizations(slug);
create index idx_users_email on users(email);
create index idx_organization_memberships_organization_id on organization_memberships(organization_id);
create index idx_organization_memberships_user_id on organization_memberships(user_id);
create index idx_organization_memberships_role on organization_memberships(role);
create index idx_clients_organization_id on clients(organization_id);
create index idx_work_requests_created_by_user_id on work_requests(created_by_user_id);
create index idx_comments_author_user_id on comments(author_user_id);
create index idx_request_attachments_uploaded_by_user_id on request_attachments(uploaded_by_user_id);
