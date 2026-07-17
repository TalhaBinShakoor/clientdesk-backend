create table quotes (
    id uuid primary key,
    client_id uuid not null,
    work_request_id uuid,
    quote_number varchar(50) not null unique,
    title varchar(200) not null,
    status varchar(30) not null,
    currency varchar(3) not null default 'USD',
    subtotal numeric(12, 2) not null default 0.00,
    tax_amount numeric(12, 2) not null default 0.00,
    total_amount numeric(12, 2) not null default 0.00,
    valid_until date,
    notes text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_quotes_client foreign key (client_id) references clients (id),
    constraint fk_quotes_work_request foreign key (work_request_id) references work_requests (id),
    constraint chk_quotes_status check (status in ('DRAFT', 'SENT', 'APPROVED', 'DECLINED')),
    constraint chk_quotes_subtotal_non_negative check (subtotal >= 0),
    constraint chk_quotes_tax_amount_non_negative check (tax_amount >= 0),
    constraint chk_quotes_total_amount_non_negative check (total_amount >= 0)
);

create table quote_line_items (
    id uuid primary key,
    quote_id uuid not null,
    description varchar(300) not null,
    quantity numeric(10, 2) not null,
    unit_price numeric(12, 2) not null,
    line_total numeric(12, 2) not null,
    sort_order integer not null default 0,
    constraint fk_quote_line_items_quote foreign key (quote_id) references quotes (id) on delete cascade,
    constraint chk_quote_line_items_quantity_positive check (quantity > 0),
    constraint chk_quote_line_items_unit_price_non_negative check (unit_price >= 0),
    constraint chk_quote_line_items_line_total_non_negative check (line_total >= 0)
);

create index idx_quotes_client_id on quotes (client_id);
create index idx_quotes_work_request_id on quotes (work_request_id);
create index idx_quotes_status on quotes (status);
create index idx_quote_line_items_quote_id on quote_line_items (quote_id);
