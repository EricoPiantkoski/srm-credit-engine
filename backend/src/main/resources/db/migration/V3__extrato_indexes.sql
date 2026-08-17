CREATE INDEX idx_liquidacao_item_recebivel ON liquidacao_item (recebivel_id);
CREATE INDEX idx_liquidacao_item_moeda_pagamento ON liquidacao_item (codigo_moeda_pagamento);
CREATE INDEX idx_liquidacao_status ON liquidacao (status);