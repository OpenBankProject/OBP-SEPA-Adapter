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
    id                      uuid PRIMARY KEY,
    creation_date_time      timestamp,
    type                    varchar,
    status                  varchar,
    sepa_file_id            uuid REFERENCES sepa_file (id),
    message_id_in_sepa_file varchar,
    number_of_transactions  integer,
    total_amount            decimal,
    settlement_date         date,
    instigating_agent       varchar,
    instigated_agent        varchar,
    custom_fields           varchar
);

CREATE TABLE sepa_credit_transfer_transaction
(
    id                          uuid PRIMARY KEY,
    amount                      decimal,
    debtor                      varchar,
    debtor_account              varchar,
    debtor_agent                varchar,
    ultimate_debtor             varchar,
    creditor                    varchar,
    creditor_account            varchar,
    creditor_agent              varchar,
    ultimate_creditor           varchar,
    purpose_code                varchar,
    description                 varchar,
    creation_date_time          timestamp,
    settlement_date             date,
    transaction_id_in_sepa_file varchar,
    instruction_id              varchar,
    end_to_end_id               varchar,
    settlement_information      varchar,
    payment_type_information    varchar,
    status                      varchar,
    custom_fields               varchar
);

CREATE TABLE sepa_transaction_message
(
    sepa_credit_transfer_transaction_id uuid REFERENCES sepa_credit_transfer_transaction (id),
    sepa_message_id                     uuid REFERENCES sepa_message (id),
    transaction_status_id_in_sepa_file  varchar,
    obp_transaction_request_id          varchar,
    obp_transaction_id                  varchar
);
