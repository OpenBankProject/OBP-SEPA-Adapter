package model.archives

import java.time.LocalDate
import java.util.UUID

case class Customer(
                     id: UUID,
                     firstName: String,
                     lastName: String,
                     address: String,
                     zipCode: String,
                     city: String,
                     country: String,
                     birthDate: LocalDate,
                     birthCity: String,
                     birthCountry: String
                   )
