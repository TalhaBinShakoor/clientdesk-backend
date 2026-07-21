insert into organizations (
    id,
    name,
    slug,
    created_at,
    updated_at
)
values (
    '01010101-0101-0101-0101-010101010101',
    'ClientDesk Demo Workspace',
    'clientdesk-demo',
    now(),
    now()
)
on conflict (id) do update
set name = excluded.name,
    slug = excluded.slug,
    updated_at = now();

insert into users (
    id,
    email,
    display_name,
    password_hash,
    enabled,
    created_at,
    updated_at
)
values
    (
        '02020202-0202-0202-0202-020202020201',
        'admin@clientdesk.test',
        'Alex Morgan',
        '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
        true,
        now(),
        now()
    ),
    (
        '02020202-0202-0202-0202-020202020202',
        'team@clientdesk.test',
        'Jamie Lee',
        '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
        true,
        now(),
        now()
    ),
    (
        '02020202-0202-0202-0202-020202020203',
        'client@clientdesk.test',
        'Maya Chen',
        '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
        true,
        now(),
        now()
    )
on conflict (id) do update
set email = excluded.email,
    display_name = excluded.display_name,
    password_hash = excluded.password_hash,
    enabled = true,
    updated_at = now();

insert into organization_memberships (
    id,
    organization_id,
    user_id,
    role,
    client_id,
    created_at,
    updated_at
)
values
    (
        '03030303-0303-0303-0303-030303030301',
        '01010101-0101-0101-0101-010101010101',
        '02020202-0202-0202-0202-020202020201',
        'ADMIN',
        null,
        now(),
        now()
    ),
    (
        '03030303-0303-0303-0303-030303030302',
        '01010101-0101-0101-0101-010101010101',
        '02020202-0202-0202-0202-020202020202',
        'TEAM_MEMBER',
        null,
        now(),
        now()
    )
on conflict (id) do update
set organization_id = excluded.organization_id,
    user_id = excluded.user_id,
    role = excluded.role,
    client_id = excluded.client_id,
    updated_at = now();

insert into clients (
    id,
    organization_id,
    company_name,
    contact_name,
    email,
    phone,
    status,
    notes,
    created_at,
    updated_at
)
values
    (
        '11111111-1111-1111-1111-111111111111',
        '01010101-0101-0101-0101-010101010101',
        'Acme Studio',
        'Maya Chen',
        'maya@acmestudio.test',
        '+1 555-0101',
        'ACTIVE',
        'Design studio with recurring website and client portal work.',
        now() - interval '18 days',
        now() - interval '2 days'
    ),
    (
        '22222222-2222-2222-2222-222222222222',
        '01010101-0101-0101-0101-010101010101',
        'Northstar Legal',
        'Daniel Reeves',
        'daniel@northstarlegal.test',
        '+1 555-0102',
        'ACTIVE',
        'Boutique legal firm preparing a public site refresh and intake workflow.',
        now() - interval '15 days',
        now() - interval '1 day'
    ),
    (
        '33333333-3333-3333-3333-333333333333',
        '01010101-0101-0101-0101-010101010101',
        'Pixel Pantry',
        'Sofia Alvarez',
        'sofia@pixelpantry.test',
        '+1 555-0103',
        'LEAD',
        'E-commerce brand evaluating ongoing support and analytics improvements.',
        now() - interval '9 days',
        now() - interval '3 days'
    )
on conflict (id) do update
set organization_id = excluded.organization_id,
    company_name = excluded.company_name,
    contact_name = excluded.contact_name,
    email = excluded.email,
    phone = excluded.phone,
    status = excluded.status,
    notes = excluded.notes,
    updated_at = excluded.updated_at;

insert into organization_memberships (
    id,
    organization_id,
    user_id,
    role,
    client_id,
    created_at,
    updated_at
)
values (
    '03030303-0303-0303-0303-030303030303',
    '01010101-0101-0101-0101-010101010101',
    '02020202-0202-0202-0202-020202020203',
    'CLIENT',
    '11111111-1111-1111-1111-111111111111',
    now(),
    now()
)
on conflict (id) do update
set organization_id = excluded.organization_id,
    user_id = excluded.user_id,
    role = excluded.role,
    client_id = excluded.client_id,
    updated_at = now();

insert into work_requests (
    id, client_id, created_by_user_id, title, description, status, priority,
    requested_by, due_date, created_at, updated_at
)
values
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
        '11111111-1111-1111-1111-111111111111',
        '02020202-0202-0202-0202-020202020203',
        'Client portal onboarding flow',
        'Create a guided onboarding flow for new portal users with welcome copy, account setup steps, and first-request prompts.',
        'IN_PROGRESS', 'HIGH', 'Maya Chen',
        current_date + 8, now() - interval '12 days', now() - interval '1 day'
    ),
    (
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
        '11111111-1111-1111-1111-111111111111',
        '02020202-0202-0202-0202-020202020203',
        'Update case study page',
        'Refresh the homepage case study section with new metrics, screenshots, and calls to action.',
        'WAITING_ON_CLIENT', 'MEDIUM', 'Maya Chen',
        current_date + 4, now() - interval '7 days', now() - interval '2 days'
    ),
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
        '22222222-2222-2222-2222-222222222222',
        '02020202-0202-0202-0202-020202020201',
        'Urgent contact form fix',
        'Contact form submissions are not reaching the intake mailbox. Investigate delivery and improve confirmation messaging.',
        'NEW', 'URGENT', 'Daniel Reeves',
        current_date + 1, now() - interval '1 day', now() - interval '1 day'
    ),
    (
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
        '22222222-2222-2222-2222-222222222222',
        '02020202-0202-0202-0202-020202020201',
        'Service pages polish',
        'Tighten layout spacing, update attorney bios, and add clearer next-step buttons on practice area pages.',
        'RESOLVED', 'LOW', 'Daniel Reeves',
        current_date - 2, now() - interval '14 days', now() - interval '3 days'
    ),
    (
        'cccccccc-cccc-cccc-cccc-ccccccccccc1',
        '33333333-3333-3333-3333-333333333333',
        '02020202-0202-0202-0202-020202020201',
        'Analytics dashboard proposal',
        'Prepare a lightweight dashboard concept for tracking orders, conversion rate, and support response time.',
        'NEW', 'MEDIUM', 'Sofia Alvarez',
        current_date + 12, now() - interval '3 days', now() - interval '3 days'
    )
on conflict (id) do update
set client_id = excluded.client_id,
    created_by_user_id = excluded.created_by_user_id,
    title = excluded.title,
    description = excluded.description,
    status = excluded.status,
    priority = excluded.priority,
    requested_by = excluded.requested_by,
    due_date = excluded.due_date,
    updated_at = excluded.updated_at;

insert into project_tasks (
    id, work_request_id, title, description, status, assignee,
    due_date, created_at, updated_at
)
values
    (
        'dddddddd-dddd-dddd-dddd-ddddddddddd1',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
        'Map onboarding steps',
        'Turn the kickoff notes into a simple onboarding checklist.',
        'DONE', 'Alex Morgan', current_date - 1,
        now() - interval '11 days', now() - interval '2 days'
    ),
    (
        'dddddddd-dddd-dddd-dddd-ddddddddddd2',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
        'Build onboarding UI states',
        'Add empty, active, and completed states for the onboarding flow.',
        'IN_PROGRESS', 'Jamie Lee', current_date + 3,
        now() - interval '8 days', now() - interval '1 day'
    ),
    (
        'dddddddd-dddd-dddd-dddd-ddddddddddd3',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2',
        'Collect final screenshots',
        'Wait for approved screenshots from the client before publishing.',
        'BLOCKED', 'Alex Morgan', current_date + 2,
        now() - interval '6 days', now() - interval '2 days'
    ),
    (
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
        'Trace form submission path',
        'Check frontend validation, backend handling, and mailbox routing.',
        'TODO', 'Jamie Lee', current_date,
        now() - interval '1 day', now() - interval '1 day'
    ),
    (
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2',
        'Update practice area CTA copy',
        'Replace generic buttons with consultation-focused copy.',
        'DONE', 'Alex Morgan', current_date - 4,
        now() - interval '12 days', now() - interval '4 days'
    ),
    (
        'ffffffff-ffff-ffff-ffff-fffffffffff1',
        'cccccccc-cccc-cccc-cccc-ccccccccccc1',
        'Draft analytics dashboard quote',
        'Estimate dashboard setup, data model, and first reporting view.',
        'TODO', 'Sam Patel', current_date + 5,
        now() - interval '3 days', now() - interval '3 days'
    )
on conflict (id) do update
set work_request_id = excluded.work_request_id,
    title = excluded.title,
    description = excluded.description,
    status = excluded.status,
    assignee = excluded.assignee,
    due_date = excluded.due_date,
    updated_at = excluded.updated_at;

insert into comments (
    id, work_request_id, project_task_id, author_user_id,
    author_name, body, created_at, updated_at
)
values
    (
        '12121212-1212-1212-1212-121212121211',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
        null,
        '02020202-0202-0202-0202-020202020203',
        'Maya Chen',
        'The onboarding checklist should feel friendly but still professional for agency clients.',
        now() - interval '10 days',
        now() - interval '10 days'
    ),
    (
        '12121212-1212-1212-1212-121212121212',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
        null,
        '02020202-0202-0202-0202-020202020201',
        'Alex Morgan',
        'First pass is mapped. Next step is wiring the UI states and status copy.',
        now() - interval '2 days',
        now() - interval '2 days'
    ),
    (
        '23232323-2323-2323-2323-232323232321',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
        null,
        null,
        'Daniel Reeves',
        'This is blocking new consultations, so please treat it as urgent.',
        now() - interval '1 day',
        now() - interval '1 day'
    ),
    (
        '34343434-3434-3434-3434-343434343431',
        'cccccccc-cccc-cccc-cccc-ccccccccccc1',
        null,
        null,
        'Sofia Alvarez',
        'We want something small enough to launch quickly, but clear enough for weekly reporting.',
        now() - interval '3 days',
        now() - interval '3 days'
    )
on conflict (id) do update
set work_request_id = excluded.work_request_id,
    project_task_id = excluded.project_task_id,
    author_user_id = excluded.author_user_id,
    author_name = excluded.author_name,
    body = excluded.body,
    updated_at = excluded.updated_at;

insert into activity_events (
    id, work_request_id, project_task_id, comment_id, actor_user_id,
    event_type, actor_name, summary, created_at
)
values
    (
        'abababab-abab-abab-abab-ababababab01',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', null, null,
        '02020202-0202-0202-0202-020202020203',
        'WORK_REQUEST_CREATED', 'Maya Chen',
        'Created work request: Client portal onboarding flow',
        now() - interval '12 days'
    ),
    (
        'abababab-abab-abab-abab-ababababab02',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
        'dddddddd-dddd-dddd-dddd-ddddddddddd1', null,
        '02020202-0202-0202-0202-020202020201',
        'PROJECT_TASK_CREATED', 'Alex Morgan',
        'Created task: Map onboarding steps',
        now() - interval '11 days'
    ),
    (
        'abababab-abab-abab-abab-ababababab03',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', null,
        '12121212-1212-1212-1212-121212121211',
        '02020202-0202-0202-0202-020202020203',
        'COMMENT_ADDED', 'Maya Chen',
        'Added a comment about onboarding tone',
        now() - interval '10 days'
    ),
    (
        'abababab-abab-abab-abab-ababababab04',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1', null, null,
        '02020202-0202-0202-0202-020202020201',
        'WORK_REQUEST_STATUS_CHANGED', 'Alex Morgan',
        'Moved request to In Progress',
        now() - interval '8 days'
    ),
    (
        'bcbcbcbc-bcbc-bcbc-bcbc-bcbcbcbcbc01',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1', null, null, null,
        'WORK_REQUEST_CREATED', 'Daniel Reeves',
        'Created work request: Urgent contact form fix',
        now() - interval '1 day'
    ),
    (
        'cdcdcdcd-cdcd-cdcd-cdcd-cdcdcdcdcd01',
        'cccccccc-cccc-cccc-cccc-ccccccccccc1', null, null, null,
        'WORK_REQUEST_CREATED', 'Sofia Alvarez',
        'Created work request: Analytics dashboard proposal',
        now() - interval '3 days'
    )
on conflict (id) do update
set work_request_id = excluded.work_request_id,
    project_task_id = excluded.project_task_id,
    comment_id = excluded.comment_id,
    actor_user_id = excluded.actor_user_id,
    event_type = excluded.event_type,
    actor_name = excluded.actor_name,
    summary = excluded.summary,
    created_at = excluded.created_at;

insert into quotes (
    id, client_id, work_request_id, quote_number, title, status, currency,
    subtotal, tax_amount, total_amount, valid_until, notes, created_at, updated_at
)
values
    (
        '51515151-5151-5151-5151-515151515151',
        '11111111-1111-1111-1111-111111111111',
        'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1',
        'Q-2026-DEMO-001', 'Client portal onboarding package', 'SENT', 'USD',
        3400.00, 0.00, 3400.00, current_date + 14,
        'Includes onboarding flow design, implementation, and launch review.',
        now() - interval '5 days', now() - interval '2 days'
    ),
    (
        '62626262-6262-6262-6262-626262626262',
        '22222222-2222-2222-2222-222222222222',
        'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1',
        'Q-2026-DEMO-002', 'Contact form recovery sprint', 'DRAFT', 'USD',
        2100.00, 210.00, 2310.00, current_date + 10,
        'Urgent debugging and reliability improvements for lead intake.',
        now() - interval '1 day', now() - interval '1 day'
    ),
    (
        '73737373-7373-7373-7373-737373737373',
        '33333333-3333-3333-3333-333333333333', null,
        'Q-2026-DEMO-003', 'Analytics dashboard discovery', 'APPROVED', 'USD',
        1200.00, 0.00, 1200.00, current_date + 21,
        'Discovery workshop and dashboard implementation plan.',
        now() - interval '2 days', now() - interval '1 day'
    )
on conflict (id) do update
set client_id = excluded.client_id,
    work_request_id = excluded.work_request_id,
    quote_number = excluded.quote_number,
    title = excluded.title,
    status = excluded.status,
    currency = excluded.currency,
    subtotal = excluded.subtotal,
    tax_amount = excluded.tax_amount,
    total_amount = excluded.total_amount,
    valid_until = excluded.valid_until,
    notes = excluded.notes,
    updated_at = excluded.updated_at;

insert into quote_line_items (
    id, quote_id, description, quantity, unit_price, line_total, sort_order
)
values
    (
        '81818181-8181-8181-8181-818181818181',
        '51515151-5151-5151-5151-515151515151',
        'Onboarding flow design and implementation', 24.00, 125.00, 3000.00, 1
    ),
    (
        '81818181-8181-8181-8181-818181818182',
        '51515151-5151-5151-5151-515151515151',
        'Launch review and handoff notes', 1.00, 400.00, 400.00, 2
    ),
    (
        '92929292-9292-9292-9292-929292929291',
        '62626262-6262-6262-6262-626262626262',
        'Urgent form debugging sprint', 1.00, 1500.00, 1500.00, 1
    ),
    (
        '92929292-9292-9292-9292-929292929292',
        '62626262-6262-6262-6262-626262626262',
        'Delivery monitoring and QA', 6.00, 100.00, 600.00, 2
    ),
    (
        'a3a3a3a3-a3a3-a3a3-a3a3-a3a3a3a3a3a1',
        '73737373-7373-7373-7373-737373737373',
        'Analytics discovery workshop', 1.00, 750.00, 750.00, 1
    ),
    (
        'a3a3a3a3-a3a3-a3a3-a3a3-a3a3a3a3a3a2',
        '73737373-7373-7373-7373-737373737373',
        'Dashboard implementation plan', 1.00, 450.00, 450.00, 2
    )
on conflict (id) do update
set quote_id = excluded.quote_id,
    description = excluded.description,
    quantity = excluded.quantity,
    unit_price = excluded.unit_price,
    line_total = excluded.line_total,
    sort_order = excluded.sort_order;
