CREATE TABLE clients (
                         id UUID PRIMARY KEY,
                         company_name VARCHAR(200) NOT NULL,
                         contact_name VARCHAR(200),
                         email VARCHAR(320),
                         phone VARCHAR(50),
                         status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                         notes TEXT,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                         CONSTRAINT clients_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'LEAD'))
);