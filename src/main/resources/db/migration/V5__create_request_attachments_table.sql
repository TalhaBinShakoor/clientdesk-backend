create table request_attachments (
    id uuid primary key,
    work_request_id uuid not null references work_requests(id) on delete cascade,
    original_file_name varchar(255) not null,
    stored_file_name varchar(255) not null unique,
    content_type varchar(255),
    size_bytes bigint not null,
    uploaded_by varchar(200),
    created_at timestamp with time zone not null
);

create index idx_request_attachments_work_request_id on request_attachments(work_request_id);
create index idx_request_attachments_created_at on request_attachments(created_at);
