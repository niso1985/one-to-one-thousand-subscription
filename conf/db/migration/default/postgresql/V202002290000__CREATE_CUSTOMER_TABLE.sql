create table c_customer
(
    id        serial       not null
        constraint c_customer_pk
            primary key,
    email     varchar(256) not null,
    stripe_id varchar(256) not null,
    user_name varchar(256) not null
);

create unique index c_customer_email_uindex
    on c_customer (email);

create unique index c_customer_stripe_id_uindex
    on c_customer (stripe_id);

