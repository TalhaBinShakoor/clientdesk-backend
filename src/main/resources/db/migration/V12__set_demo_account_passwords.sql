update users
set password_hash = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    updated_at = now()
where email in (
    'admin@clientdesk.test',
    'team@clientdesk.test',
    'client@clientdesk.test'
);
