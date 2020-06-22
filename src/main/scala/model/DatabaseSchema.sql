CREATE TABLE sepa_file
(
    id             uuid PRIMARY KEY,
    name           varchar,
    path           varchar,
    type           varchar,
    status         varchar,
    receipt_date   timestamp,
    processed_date timestamp
);

CREATE TABLE sepa_message
(
    id                 uuid PRIMARY KEY,
    creation_date_time timestamp,
    type               varchar,
    content            varchar,
    sepa_file_id       uuid REFERENCES sepa_file (id),
    id_in_sepa_file    varchar
);

CREATE TABLE sepa_credit_transfer_transaction
(
    id               uuid PRIMARY KEY,
    amount           decimal,
    debtor_name      varchar,
    debtor_account   varchar,
    debtor_agent     varchar,
    creditor_name    varchar,
    creditor_account varchar,
    creditor_agent   varchar,
    purpose_code     varchar,
    descripton       varchar,
    sepa_message_id  uuid REFERENCES sepa_message (id),
    id_in_sepa_file  varchar,
    instruction_id   varchar,
    end_to_end_id    varchar
);
