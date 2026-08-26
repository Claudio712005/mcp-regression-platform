insert into bff_service (name, description) values
    ('fintech-bff-account', 'Backend for frontend responsible for the retail account journey');

insert into bff_dependency (bff_name, dependency_name, dependency_type, criticality, description, position) values
    ('fintech-bff-account', 'fintech-srv-account', 'HTTP_SERVICE', 'CRITICAL', 'Account domain service exposing balances and account status', 1),
    ('fintech-bff-account', 'fintech-db', 'DATABASE', 'CRITICAL', 'Relational store used by the account journey', 2);
