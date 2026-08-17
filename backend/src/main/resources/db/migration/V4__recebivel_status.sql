ALTER TABLE recebivel ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'DISPONIVEL';

CREATE INDEX idx_recebivel_status ON recebivel (status);