alter table c_customer
    add is_deleted boolean not null;

alter table c_customer
    add create_ts timestamp not null;

alter table c_customer
    add update_ts timestamp not null;
