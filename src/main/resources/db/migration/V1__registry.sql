create extension if not exists vector;

create table bff_service (
    name varchar(120) primary key,
    description varchar(500) not null
);

create table bff_dependency (
    bff_name varchar(120) not null references bff_service (name),
    dependency_name varchar(120) not null,
    dependency_type varchar(40) not null,
    criticality varchar(40) not null,
    description varchar(500) not null,
    position int not null,
    primary key (bff_name, dependency_name)
);
