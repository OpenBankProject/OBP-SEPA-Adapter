# README

Open Bank Project SEPA Adpater

## LICENSE

This project is dual licensed under the AGPL V3 (see LICENSE) and commercial licenses from TESOBE GmbH.

## PROJECT

This SEPA adapter works with the OBP-API to provide a SEPA payment solution.

[OBP SEPA Adapter - Global overview](https://vimeo.com/440002863)

The current fork of the OBP-API working with the SEPA adapter is available here :
https://github.com/GuillaumeKergreis/OBP-API/tree/AddSepaAdapter.
The features present in this branch will be gradually added to the OBP-API 
so that ultimately the OBP-API will be fully compatible with the adapater.


### SEPA messages support

Currently, only the Credit Transfer scheme is implemented.
Here is the differents messages supported by this application :

- Messages integrated with the OBP-API :
    - CREDIT TRANSFER (pacs.008.001.02)
        - [Send a credit transfer transaction](https://vimeo.com/440011547)
        - [Receive a credit transfer transaction](https://vimeo.com/440020466)
    - PAYMENT RETURN (pacs.004.001.02)
    - PAYMENT REJECT (pacs.002.001.03)
    - PAYMENT RECALL (camt.056.001.01)
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
- notifyTransactionRequest (Implemented in the OBP-API forked version)
Those methods must be defined in the methods routings to be routed with the akka connector.
So the OBP-API connector need to be set to `star` in the props file.

### OBP-API endpoints

Here are the endpoints called by the SEPA Adapter to communicate with the OBP-API:
- saveHistoricalTransaction
- createTransactionRequestRefund
- getTransactionRequest
- answerTransactionRequestChallenge
- getAccountIdByIban (Implemented in the OBP-API forked version)

## DOCUMENTATION

### SEPA Credit Transfer

All the necessary documentation about how the SEPA Credit Transfer works can be found on the European Payment Council website :
https://www.europeanpaymentscouncil.eu/what-we-do/sepa-payment-schemes/sepa-credit-transfer/sepa-credit-transfer-rulebook-and

Here is the SCT scheme functional specifications (Rulebook) : 
https://www.europeanpaymentscouncil.eu/sites/default/files/kb/file/2020-04/EPC125-05%202019%20SCT%20Rulebook%20version%201.1.pdf

Technical documentation (Implementation guidelines) can be found here : 
https://www.europeanpaymentscouncil.eu/sites/default/files/kb/file/2018-11/EPC115-06%20SCT%20Interbank%20IG%202019%20V1.0.pdf
