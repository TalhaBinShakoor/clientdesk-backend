create table comments (
    id uuid primary key,
    work_request_id uuid references work_requests(id) on delete cascade,
    project_task_id uuid references project_tasks(id) on delete cascade,
    author_name varchar(200) not null,
    body text not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint comments_target_check check (
        (work_request_id is not null and project_task_id is null)
        or
        (work_request_id is null and project_task_id is not null)
    )
);

create index idx_comments_work_request_id on comments(work_request_id);
create index idx_comments_project_task_id on comments(project_task_id);
create index idx_comments_created_at on comments(created_at);

create table activity_events (
    id uuid primary key,
    work_request_id uuid not null references work_requests(id) on delete cascade,
    project_task_id uuid references project_tasks(id) on delete set null,
    comment_id uuid references comments(id) on delete set null,
    event_type varchar(50) not null,
    actor_name varchar(200),
    summary varchar(300) not null,
    created_at timestamp with time zone not null
);

create index idx_activity_events_work_request_id on activity_events(work_request_id);
create index idx_activity_events_project_task_id on activity_events(project_task_id);
create index idx_activity_events_comment_id on activity_events(comment_id);
create index idx_activity_events_created_at on activity_events(created_at);
