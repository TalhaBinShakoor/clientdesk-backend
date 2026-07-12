CREATE TABLE project_tasks (
    id UUID PRIMARY KEY,
    work_request_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'TODO',
    assignee VARCHAR(200),
    due_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_project_tasks_work_request
        FOREIGN KEY (work_request_id)
        REFERENCES work_requests(id)
        ON DELETE CASCADE,

    CONSTRAINT project_tasks_status_check
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'BLOCKED', 'DONE'))
);

CREATE INDEX idx_project_tasks_work_request_id ON project_tasks(work_request_id);
CREATE INDEX idx_project_tasks_status ON project_tasks(status);
CREATE INDEX idx_project_tasks_assignee ON project_tasks(assignee);
