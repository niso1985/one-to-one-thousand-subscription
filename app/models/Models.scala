package models

import play.api.libs.json.Json

case class StripeInfo(
    publicKey: String,
    plan1:     String,
    plan2:     String,
    plan3:     String,
    plan4:     String,
    plan5:     String
)
object StripeInfo {
  implicit val jw = Json.writes[StripeInfo]
}

case class Session(sessionId: String)
object Session {
  implicit val jw = Json.writes[Session]
}

case class CustomerInfo(name: String, email: String, introducer: String, plan: String)
object CustomerInfo {
  implicit val jr = Json.reads[CustomerInfo]
}

case class ErrorResponse(msg: String)
object ErrorResponse {
  implicit val jw = Json.writes[ErrorResponse]
}
