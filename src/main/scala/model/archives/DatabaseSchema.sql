CREATE TABLE customer
(
    id            uuid PRIMARY KEY,
    first_name    varchar,
    last_name     varchar,
    address       varchar,
    zip_code      varchar,
    city          varchar,
    country       varchar,
    birth_date    date,
    birth_city    varchar,
    birth_country varchar
);

CREATE TABLE beneficiary
(
    id          uuid PRIMARY KEY,
    customer_id uuid REFERENCES customer (id),
    iban        varchar(34),
    bic         varchar(11),
    name        varchar,
    label       varchar
);

CREATE TABLE account
(
    id                 uuid PRIMARY KEY,
    iban               varchar(34),
    customer_id        uuid REFERENCES customer (id),
    type               varchar,
    status             varchar,
    balance            decimal,
    accounting_balance decimal
);

CREATE TABLE sdd_mandate
(
    id                  uuid PRIMARY KEY,
    reference           varchar,
    customer_id         uuid REFERENCES customer (id),
    customer_account_id uuid REFERENCES account (id),
    biller_name         varchar,
    biller_reference    varchar,
    biller_iban         varchar(34),
    biller_bic          varchar(11),
    type                varchar(4),
    recurrent           boolean,
    status              varchar,
    implementation_date date
);

CREATE TABLE card
(
    id                      uuid PRIMARY KEY,
    customer_id             uuid REFERENCES customer (id),
    account_id              uuid REFERENCES account (id),
    pan                     varchar,
    cvv                     varchar,
    pin                     varchar,
    creation_date           date,
    activation_date         date,
    opposition_date         date,
    expiration_date         date,
    payment_network         varchar,
    card_type               varchar,
    range                   varchar,
    status                  varchar,
    withdrawal_status       varchar,
    internet_payment_status varchar,
    abroad_payment_status   varchar,
    nfc_payment_status      varchar,
    nfc_paid_amount         decimal,
    nfc_payment_limit       decimal,
    paid_amount             decimal,
    payment_limit           decimal,
    withdrawn_amount        decimal,
    withdrawal_limit        decimal
);

CREATE TABLE core_banking_file
(
    id                     uuid PRIMARY KEY,
    name                   varchar,
    path                   varchar,
    type                   varchar,
    status                 varchar,
    receipt_date           timestamp,
    processed_date         timestamp,
    number_of_transactions integer
);

CREATE TABLE transaction
(
    id          uuid PRIMARY KEY,
    account_id  uuid REFERENCES account (id),
    type        varchar,
    status      varchar,
    amount      decimal,
    date        timestamp,
    old_balance decimal,
    new_balance decimal
);

CREATE TABLE sepa_transaction
(
    counterpart_iban    varchar(34),
    counterpart_bic     varchar(11),
    counterpart_name    varchar,
    description         varchar(140),
    processed_date      timestamp,
    file_id             uuid REFERENCES core_banking_file (id),
    file_transaction_id varchar(35)
) INHERITS (transaction);

CREATE TABLE card_transaction
(
    card_id          uuid REFERENCES card (id),
    merchant_name    varchar,
    clearing_date    timestamp,
    clearing_file_id uuid REFERENCES core_banking_file (id)
) INHERITS (transaction);


