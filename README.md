# README

Open Bank Project SEPA Adpater

## LICENSE

This project is dual licensed under the AGPL V3 (see LICENSE) and commercial licenses from TESOBE GmbH.

## GETTING STARTED

- First, you'll need the develop version of the OBP-API :
https://github.com/OpenBankProject/OBP-API/tree/develop.

- In the OBP-API props file (obp-api/src/main/resources/props/default.props), be sure to have these corresponding values :
```
connector=star
starConnector_supported_types=mapped,akka
transactionRequests_supported_types=SANDBOX_TAN,COUNTERPARTY,SEPA,ACCOUNT_OTP,ACCOUNT,REFUND
akka_connector.hostname=127.0.0.1
akka_connector.port=2662
akka_connector.timeout=10
SEPA_OTP_INSTRUCTION_TRANSPORT=DUMMY
REFUND_OTP_INSTRUCTION_TRANSPORT=DUMMY
```

- Add the necessary methodRouting `makePaymentv210`, `makePaymentV400` and `notifyTransactionRequest` 
to route them through the "akka_vDec2018" connector.
You can do this by calling the OBP create method routing endpoint 
`localhost:8080/obp/v4.0.0/management/method_routings` (POST) with the following body (example for `makePaymentv210`) :
```
{
    "is_bank_id_exact_match": false,
    "method_name": "makePaymentv210",
    "connector_name": "akka_vDec2018",
    "bank_id_pattern": ".*",
    "parameters": []
}
```

- In the SEPA Adapter application.conf file (src/main/ressources/application.conf) configure your postgreSQL Database,
fill-in your OBP-API DirectLogin token and configure the akka properties if necessary. 
Fill the `sepa-adapter` fields `bank-id` with the `BankId` you created on the OBP-API (e.g. `THE_DEFAULT_BANK_ID`)
and the `bank-bic` one with your OBP-API Bank BIC (e.g. `OBPBDEB1XXX`).
```
databaseConfig = {
  dataSourceClass = "org.postgresql.ds.PGSimpleDataSource"
  properties = {
    databaseName = "FILL_ME"
    user = "FILL_ME"
    password = "FILL_ME"
  }
  numThreads = 10
}
obp-api = {
  authorization = {
    direct-login-token = "FILL_ME"
  }
}
sepa-adapter {
  bank-id = "FILL_ME"
  bank-bic = "FILL_ME"
}
```

- Then, run the `DatabaseSchema.sql` script (`src/main/scala/model/DatabaseSchema.sql`) to create the required database tables.

- Now, you should be able to start the SEPA Adapter (`src/main/scala/adapter/Adapter.scala`)

- The OBP-API user connected to the SEPA Adapter need the following entitlements. Those ones can be added
by calling the entitlements endpoint `localhost:8080/obp/v4.0.0/users/USER_ID/entitlements` (POST).
    - `CanCreateHistoricalTransaction`
    - `CanCreateAnyTransactionRequest`

### Process outgoing files

Once you have recorded some outgoing messages in your Adapter database (By using the OBP-API Transaction request endpoint) 
yoy can create the outgoing files by running the ProcessOutgoingFiles executable (src/main/scala/sepa/scheduler/ProcessOutgoingFiles.scala).
You'll find the generated files at the location `src/main/scala/sepa/sct/file/out`, don't forget to create the `out` folder.

Demonstration video : [Send a credit transfer transaction](https://vimeo.com/440011547)

### Process incoming files

You can process incoming files by specifying the files to process in ProcessIncomingFilesActorSystem at
src/main/scala/sepa/scheduler/ProcessIncomingFilesActorSystem.scala and running it.

demonstration video : [Receive a credit transfer transaction](https://vimeo.com/440020466)

## PROJECT

This SEPA adapter works with the OBP-API to provide a SEPA payment solution.

[OBP SEPA Adapter - Global overview](https://vimeo.com/440002863)

### SEPA messages support

Currently, only the Credit Transfer scheme is implemented.
Here is the differents messages supported by this application :

- Messages integrated with the OBP-API :
    - CREDIT TRANSFER (pacs.008.001.02)
        - [Send a credit transfer transaction](https://vimeo.com/440011547)
        - [Receive a credit transfer transaction](https://vimeo.com/440020466)
    - PAYMENT RETURN (pacs.004.001.02)
        - [Return a credit transfer transaction](https://vimeo.com/451053702)
    - PAYMENT REJECT (pacs.002.001.03)
    - PAYMENT RECALL (camt.056.001.01)
        - [Send a payment recall](https://drive.google.com/file/d/1Ajssk6tiZiTaerz64EQd_pvJK2Qn8PK5/view?usp=sharing)
        - [Receive a payment recall](https://drive.google.com/file/d/17n9U0RscXUh1lCs3Aynz_E4jyZ8bt0op/view?usp=sharing)
    - PAYMENT RECALL NEGATIVE ANSWER (camt.029.001.03)
    
- Messages supported by the SEPA Adpater but not integrated with the OBP-API :
    - INQUIRY CLAIM NON RECEIP (camt.027.001.06)
    - INQUIRY CLAIM VALUE DATE CORRECTION (camt.087.001.05)
    - INQUIRY CLAIM NON RECEIP POSITIVE RESPONSE (camt.029.001.08)
    - INQUIRY CLAIM NON RECEIP NEGATIVE RESPONSE (camt.029.001.08)
    - INQUIRY CLAIM VALUE DATE CORRECTION POSITIVE RESPONSE (camt.029.001.08)
    - INQUIRY CLAIM VALUE DATE CORRECTION NEGATIVE RESPONSE (camt.029.001.08)
    - REQUEST STATUS UPDATE (pacs.028.001.01)
    
Inquiry messages are special ones because they can't be processed automatically in the most of the cases. 
To deal with those messages, two options are available :
- Create new endpoints on the OBP-API to deal with those messages (not recommended)
- Create an API on this SEPA Adapter to provide a full interface to process those messages (not implemented)

### OBP-API connectors

The adapter uses the OBP-API to store transactions and transaction requests. 
This allows the system to be fully transparent.

Here are the connector methods used by the SEPA adapter : 
- makePaymentv210
- makePaymentV400
- notifyTransactionRequest

Those methods must be defined in the methods routings to be routed with the akka connector.
So the OBP-API connector need to be set to `star` in the props file.

### OBP-API endpoints

Here are the endpoints called by the SEPA Adapter to communicate with the OBP-API:
- saveHistoricalTransaction
- createTransactionRequestRefund
- getTransactionRequest
- answerTransactionRequestChallenge
- getAccountByAccountRouting
- getPrivateAccountByIdFull
- getBankById
- getExplictCounterpartiesForAccount
- createCounterparty

## DOCUMENTATION

### SEPA Credit Transfer

All the necessary documentation about how the SEPA Credit Transfer works can be found on the European Payment Council website :
https://www.europeanpaymentscouncil.eu/what-we-do/sepa-payment-schemes/sepa-credit-transfer/sepa-credit-transfer-rulebook-and

Here is the SCT scheme functional specifications (Rulebook) : 
https://www.europeanpaymentscouncil.eu/sites/default/files/kb/file/2020-04/EPC125-05%202019%20SCT%20Rulebook%20version%201.1.pdf

Technical documentation (Implementation guidelines) can be found here : 
https://www.europeanpaymentscouncil.eu/sites/default/files/kb/file/2018-11/EPC115-06%20SCT%20Interbank%20IG%202019%20V1.0.pdf

### Dependencies
- Database : [PostgreSQL](https://www.postgresql.org)
- Database mapper : [Slick](https://scala-slick.org)
- Communication and HTTP requests : [Akka](https://akka.io)
- XSD class generator : [scalaxb](http://scalaxb.org)
- Json : [Circe](https://circe.github.io/circe)