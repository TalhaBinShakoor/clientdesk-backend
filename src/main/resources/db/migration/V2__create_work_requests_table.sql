CREATE TABLE work_requests (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM',
    requested_by VARCHAR(200),
    due_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_work_requests_client
        FOREIGN KEY (client_id)
        REFERENCES clients(id)
        ON DELETE CASCADE,

    CONSTRAINT work_requests_status_check
        CHECK (status IN ('NEW', 'IN_PROGRESS', 'WAITING_ON_CLIENT', 'RESOLVED', 'CLOSED')),

    CONSTRAINT work_requests_priority_check
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT'))
);

CREATE INDEX idx_work_requests_client_id ON work_requests(client_id);
CREATE INDEX idx_work_requests_status ON work_requests(status);
CREATE INDEX idx_work_requests_priority ON work_requests(priority);
