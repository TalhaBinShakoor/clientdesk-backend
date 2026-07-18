alter table activity_events
    add column actor_user_id uuid;

update activity_events
set actor_user_id = case
    when actor_name = 'Maya Chen' then '02020202-0202-0202-0202-020202020203'::uuid
    when actor_name = 'Jamie Lee' then '02020202-0202-0202-0202-020202020202'::uuid
    when actor_name = 'Alex Morgan' then '02020202-0202-0202-0202-020202020201'::uuid
    else null
end
where actor_user_id is null;

alter table activity_events
    add constraint fk_activity_events_actor_user
        foreign key (actor_user_id) references users(id) on delete set null;

create index idx_activity_events_actor_user_id
    on activity_events(actor_user_id);
