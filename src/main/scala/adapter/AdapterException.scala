package adapter

// Exception that can be generaed by the Adapter from an OBP-API response

class ObpApiNotAvailableException(message: String) extends Exception(message)

class ObpAccountNotFoundException(message: String) extends Exception(message)

class ObpBankNotFoundException(message: String) extends Exception(message)
