package controllers

import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.checkout.Session
import com.stripe.param.{ CustomerCreateParams, CustomerUpdateParams }
import com.stripe.param.checkout.SessionCreateParams
import com.stripe.param.checkout.SessionCreateParams.{ PaymentMethodType, SubscriptionData }
import com.typesafe.config._
import dao.CustomerDAO
import models.{ CustomerInfo, ErrorResponse, StripeInfo }
import play.Logger
import play.api.libs.json.{ JsError, JsSuccess, Json }
import play.api.mvc._
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.dbio.DBIOAction
import slick.jdbc.JdbcProfile

import scala.util.control.Exception._
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.control.NonFatal

class Application @Inject() (cc: ControllerComponents, dp: DatabaseConfigProvider) extends AbstractController(cc) {
  val config: Config = ConfigFactory.load()
  val pubkey = config.getString("stripe.pubkey")
  val plan1 = config.getString("stripe.plan1")
  val plan2 = config.getString("stripe.plan2")
  val baseUrl = config.getString("baseurl")

  Stripe.apiKey = config.getString("stripe.secretkey")

  val dbConfig = dp.get[JdbcProfile]

  def setup = Action {
    val info = StripeInfo(pubkey, plan1, plan2)
    Ok(Json.toJson(info))
  }

  def createCheckoutSession = Action.async { implicit request: Request[AnyContent] ⇒
    import CustomerDAO.JdbcProfile.api._
    def update(row: CustomerDAO.Row, customerInfo: CustomerInfo): DBIO[Customer] = {
      val ubuilder = new CustomerUpdateParams.Builder
      val u = ubuilder.setName(customerInfo.name)
        .setDescription(s"紹介者: ${customerInfo.introducer}")
        .build()
      val c = Customer.retrieve(row.stripeId).update(u)
      Logger.info(s"[createCheckoutSession] retrieve and update customer: $c")
      DBIO.successful(c)
    }
    def make(customerInfo: CustomerInfo): DBIO[Customer] = {
      val cbuilder = new CustomerCreateParams.Builder
      val cparam = cbuilder.setName(customerInfo.name)
        .setDescription(s"紹介者: ${customerInfo.introducer}")
        .setEmail(customerInfo.email)
        .build()
      val c = Customer.create(cparam)
      Logger.info(s"[createCheckoutSession] created customer: $c")
      (CustomerDAO += CustomerDAO.Row(
        email = customerInfo.email,
        stripeId = c.getId,
        userName = customerInfo.name
      )).map(_ ⇒ c)
    }

    Logger.info(s"[createCheckoutSession] start")
    for {
      json ← request.body.asJson.map(_.validate[CustomerInfo]) match {
        case None ⇒
          val ex = new IllegalArgumentException("Need request body.")
          Logger.error(ex.getLocalizedMessage)
          Future.failed(ex)
        case Some(j) ⇒
          Logger.info(s"[createCheckoutSession] received json: ${j.toString}")
          Future.successful(j)
      }
      customerInfo ← json match {
        case e @ JsError(_) ⇒
          val ex = new IllegalArgumentException(s"Json Parse error. ${e.toString}")
          Logger.error(ex.getLocalizedMessage)
          Future.failed(ex)
        case JsSuccess(parsed, _) ⇒
          Logger.info(s"[createCheckoutSession] received param: $parsed")
          Future.successful(parsed)
      }
      customer ← {
        val action = for {
          row ← CustomerDAO.findByEmail(customerInfo.email)
          c ← row match {
            case None    ⇒ make(customerInfo)
            case Some(r) ⇒ update(r, customerInfo)
          }
        } yield c
        dbConfig.db.run(action.transactionally) // TODO: recoverしてEitherかTryで例外を捕捉するべき
      }
      r ← Future.successful {
        val builder = new SessionCreateParams.Builder
        builder.setSuccessUrl(s"$baseUrl/assets/checkout/success.html")
          .setCancelUrl(s"$baseUrl/assets/checkout/canceled.html")
          .addPaymentMethodType(PaymentMethodType.CARD)
          .setCustomer(customer.getId)

        val planBuild = new SubscriptionData.Item.Builder()
          .setPlan(customerInfo.plan)
          .build
        val subscriptionData = new SubscriptionData.Builder()
          .addItem(planBuild)
          .build
        builder.setSubscriptionData(subscriptionData)

        val createParams = builder.build
        val session = Session.create(createParams)
        Logger.info(s"[createCheckoutSession] created session: $session")
        Ok(Json.toJson(models.Session(session.getId)))
      }
    } yield r
  }
}
