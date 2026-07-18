alter table clients
    alter column organization_id set default '01010101-0101-0101-0101-010101010101'::uuid;
