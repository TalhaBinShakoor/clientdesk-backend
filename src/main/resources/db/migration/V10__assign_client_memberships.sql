alter table organization_memberships
    add column client_id uuid;

update organization_memberships
set client_id = '11111111-1111-1111-1111-111111111111'
where role = 'CLIENT'
  and organization_id = '01010101-0101-0101-0101-010101010101';

alter table clients
    add constraint clients_id_organization_unique unique (id, organization_id);

alter table organization_memberships
    add constraint fk_memberships_client_organization
        foreign key (client_id, organization_id)
        references clients(id, organization_id)
        on delete cascade,
    add constraint organization_memberships_client_assignment_check check (
        (role = 'CLIENT' and client_id is not null)
        or (role <> 'CLIENT' and client_id is null)
    );

create index idx_organization_memberships_client_id
    on organization_memberships(client_id);
