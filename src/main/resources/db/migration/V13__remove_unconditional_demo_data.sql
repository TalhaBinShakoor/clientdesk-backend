alter table clients
    alter column organization_id drop default;

delete from quotes
where client_id in (
    select id
    from clients
    where organization_id = '01010101-0101-0101-0101-010101010101'
);

delete from clients
where organization_id = '01010101-0101-0101-0101-010101010101';

delete from organizations
where id = '01010101-0101-0101-0101-010101010101';

delete from users
where id in (
    '02020202-0202-0202-0202-020202020201',
    '02020202-0202-0202-0202-020202020202',
    '02020202-0202-0202-0202-020202020203'
);
