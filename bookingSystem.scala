import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, ReceiveTimeout, Terminated}
import akka.event.LoggingReceive

import java.util.UUID
import scala.concurrent.duration._
import scala.util.Random

//States for the property
sealed trait PropertyAvailability
case object PropertyAvailable extends PropertyAvailability
case object PropertyNotAvailable extends PropertyAvailability
//States for the Reservation
sealed trait DetailReservation
case object BookedReservation extends DetailReservation
case object JackpotReservation extends DetailReservation
case object ErrorReservation extends DetailReservation

//Message
case class SearchMessage(PropertyType: String, date:String, replyTo:ActorRef)
case class SearchResults(possibleProperties: List[Property])
//Reservation
case class TakeReservation(corrId:UUID, client: ClientIdentity, details: Property, date: String)
case class MakeReservation(client: ClientIdentity, details: Property, date: String, replyTo:ActorRef)
// attributes of the property
case class Property(id:Int, name:String, pType:String, category:String, location:(String,String))
// attributes of the client
case class ClientIdentity(name:String, age:Int, passport:String)

// Client Actor
class Client(identity: ClientIdentity, searchMessage:SearchMessage) extends Actor with ActorLogging {

  def getRandomProperty(options: List[Property]): Property = {
    options(Random.nextInt(options.length))
  }

  // Send the Search Message to the systemService to get available properties on that date
  if(!searchMessage.date.isEmpty)
    log.info(s"Client: ${identity.name} is looking for ${searchMessage.PropertyType}")
    searchMessage.replyTo ! SearchMessage(searchMessage.PropertyType,searchMessage.date, self)

  def receive: Receive = {
    //If it receive a searchResult then the client choose a random property to make a reservation
    case searchResults: SearchResults =>
      log.info(s"Client: ${identity.name} is making a decision for ${searchMessage.date}")
      searchMessage.replyTo ! MakeReservation(identity,getRandomProperty(searchResults.possibleProperties), searchMessage.date,self)
    // Acknowledge the reservation
    case BookedReservation =>
      log.info(s"Client: ${identity.name} received a confirmation for his/her reservation.")
    case JackpotReservation =>
      log.info(s"Client: ${identity.name} received a Jackpot reservation.")
    case ErrorReservation =>
      log.info(s"Client: ${identity.name} couldn't make his/her reservation (no longer available)")
  }
}

class SystemService(reservationService: ActorRef) extends Actor with ActorLogging {

  // Possible properties
  var properties = Seq(
    Property(1,"Property priveé","hotel","1 stars",("Belgium","Brussels")),
    Property(2,"Luxury Sea","hotel","2 stars",("EEUU","Los angeles")),
    Property(3,"White property","hotel","3 stars",("France","Paris")),
    Property(4,"Extreme property","hotel","4 stars",("Russia","Moscu")),
    Property(5,"Gems","hotel","5 stars",("Peru","Arequipa")),
    Property(6,"Apartment A","apartments","1 stars",("Peru","Arequipa")),
    Property(7,"Apartment B","apartments","2 stars",("EEUU","Texas")),
    Property(8,"Apartment C","apartments","3 stars",("EEUU","Texas")),
    Property(9,"Apartment D","apartments","4 stars",("Belgium","Brussels")),
    Property(10,"Apartment E","apartments","5 stars",("Canada","Montreal")),
    Property(11,"Resort Stay","resorts","1 stars",("Argentina","Buenos Aires")),
    Property(12,"Resort Join","resorts","2 stars",("Colombia","Bogotá")),
    Property(13,"Resort Peace","resorts","3 stars",("Belgium","Brussels")),
    Property(14,"Resort Green","resorts","4 stars",("Belgium","Antwerp")),
    Property(15,"Resort White","resorts","5 stars",("Peru","Arequipa"))
  )

  //Brings the type of property that is available to book
  def getPropertyAvailable(typeProperty:String): List[Property] = {
    var options:List[Property] = List()
    properties.map(element => {
      if(element.pType == typeProperty)
        options :+= element
    })
    options
  }

  def receive: Receive = {
    // The SystemService handles the searchMessage and returns a list of options
    case SearchMessage(typeOfProperty:String, date:String, replyTo) =>
      replyTo ! SearchResults(getPropertyAvailable(typeOfProperty))

    // The SystemService creates a temporaryChild to handle reservations
    case MakeReservation(client, details, date, replyTo) =>
      val corrID = UUID.randomUUID()
      log.info(s"System: Pass the reservation N.$corrID")
      val temporaryChild = context.actorOf(Props(new TemporaryChild(corrID,self,client,details,date,reservationService,replyTo)))
      temporaryChild ! TakeReservation(corrID, client, details, date)

    // The SystemService receives the confirmation of the reservation's state
    case BookedReservation =>
      log.info("System: received booked reservation")
    case JackpotReservation =>
      log.info("System: received jackpot reservation")
    case ErrorReservation =>
      log.info("System: received a not possible reservation")

  }
}

//Temporary Child Actor
class TemporaryChild(corrID: UUID, replyParent:ActorRef, client: ClientIdentity, details: Property, date: String, reservationService: ActorRef, replyTo:ActorRef) extends Actor with ActorLogging {
  context.setReceiveTimeout(5.seconds)
  // for the business-handshake pattern
  var alreadyDone: Set[ActorRef] = Set.empty
  // Some random data to not always get free properties
  var withoutEmptySpot = Seq(1,8,13,15)

  //Check if property selected is free
  def getCheckAvailable(id:Int): Boolean = {
    if (withoutEmptySpot.contains(id)) false else true
  }

  def receive: Receive = {
    case TakeReservation(`corrID`, client, details, date) =>
      log.info(s"Temporary child: Business handshake with Reservation Service for Reservation N.$corrID")
      val saga: ActorRef = context.actorOf(Props(new HandShake(self, reservationService, client,details,date)),
        name="Saga")

    // Checks availability for the property
    case MakeReservation(client, details, date, replyTo) if alreadyDone(replyTo) =>
      if (getCheckAvailable(details.id)) {
        replyTo ! PropertyAvailable
      } else
        replyTo ! PropertyNotAvailable

    // Checks availability for the property
    case MakeReservation(client, details, date, replyTo) =>
      log.info(s"Temporary child: Checks availability for ${client.name}, property[${details.id}] ${details.name} on $date. Reservation N.$corrID")
      alreadyDone += replyTo
      context.watch(replyTo)
      if (getCheckAvailable(details.id)) {
        replyTo ! PropertyAvailable
      } else
        replyTo ! PropertyNotAvailable
    case Terminated(saga) =>
      alreadyDone -= saga
    // Receives a message from reservation service
    case BookedReservation =>
      log.info(s"Temporary child: Receives confirmation for reservation N.$corrID")
      replyParent ! BookedReservation
      replyTo ! BookedReservation
      context.stop(self)

    // Receives a message from reservation service
    case JackpotReservation =>
      log.info(s"Temporary child: Receives confirmation with upgrade for reservation N.$corrID")
      replyParent ! JackpotReservation
      replyTo ! JackpotReservation
      context.stop(self)

    case PropertyNotAvailable =>
      replyParent ! ErrorReservation
      replyTo ! ErrorReservation
      context.stop(self)

    case ErrorReservation =>
      replyParent ! ErrorReservation
      replyTo ! ErrorReservation
      context.stop(self)

    //If temporary child doesn't receive a message from reservation service then something went missing on the business handshake
    case ReceiveTimeout =>
      log.warning(s"Temporary child: Property ${details.name} for ${client.name} is not available on $date. Reservation N.$corrID")
      replyParent ! ErrorReservation
      replyTo ! ErrorReservation
      context.stop(self)
  }
}

//First Part: TemporaryChild. Check if it's available. SecondPart:ReservationService make the reservation
class HandShake(firstPart: ActorRef, secondPart:ActorRef, client: ClientIdentity, details: Property, date: String) extends Actor with ActorLogging {
  def receive: Receive = passFirst

  def passFirst: Receive = {
    log.info(s"First Part needs to check if the ${details.name} is available for ${client.name}")

    firstPart ! MakeReservation(client, details, date,self)
    context.setReceiveTimeout(2.seconds)

    LoggingReceive {
      case PropertyAvailable =>
        log.info(s"Temporary Child: Property ${details.name} is available for ${client.name}")
        context.become(passSecondPart)

      case PropertyNotAvailable =>
        log.info(s"Temporary Child: Property ${details.name} is not available for ${client.name} on the $date")
        firstPart ! PropertyNotAvailable
        context.stop(self)
      case ReceiveTimeout =>
        firstPart ! MakeReservation(client, details, date,self)
    }

  }

  def passSecondPart: Receive = {
    log.info(s"Second part needs to reserve the ${details.name} for ${client.name}")

    secondPart ! MakeReservation(client, details, date,self)

    LoggingReceive {
      case BookedReservation =>
        log.info(s"Reservation service: Reservation completed for ${client.name}")
        firstPart ! BookedReservation
        context.stop(self)
      case JackpotReservation =>
        log.info(s"Reservation service: Upgrade on Reservation for ${client.name}")
        firstPart ! JackpotReservation
        context.stop(self)
      case ErrorReservation =>
        log.info(s"Reservation service: Oops something went wrong for ${client.name} missing DATE!")
        firstPart ! ErrorReservation
        context.stop(self)
    }
  }
}

// ReservationService Actor
class ReservationService extends Actor with ActorLogging {
  def randomJackpot: Boolean = {
    val number = Random.nextInt(100)
    number match {
      case x if x < 15 => true
      case x if x > 89 => true
      case _ => false
    }
    //if (number < 10) true else false
  }

  def receive: Receive = {
    case MakeReservation(client, details, date, replyTo) =>
      if(!date.isEmpty){
        if (randomJackpot) {
          replyTo ! JackpotReservation
        } else {
          replyTo ! BookedReservation
        }
      } else {
        replyTo ! ErrorReservation
        context.watch(replyTo)
      }
  }
}

object bookingSystem extends App{

  // get a random date only for december 2021
  def getRandomDate: String = {
    var num = Random.nextInt(31)
    if (num < 10) "0" + num + "/12/2021" else num + "/12/2021"
  }

  // get a random property
  def getRandomTypeProperty: String = {
    // Possible properties from a wishlist of a client
    val TypeProperty = Seq("hotel","resorts","apartments")
    TypeProperty(Random.nextInt(3))
  }

  val SystemBookingService: ActorSystem = ActorSystem("SystemService")

  val reservationService = SystemBookingService.actorOf(Props[ReservationService], "ReservationService")
  val systemServiceActor = SystemBookingService.actorOf(Props(new SystemService(reservationService)),"SystemService")
  val clientActorA = SystemBookingService.actorOf(Props(new Client(ClientIdentity("Brenda Ordonez Lujan",27,"102451"), SearchMessage(getRandomTypeProperty,getRandomDate,systemServiceActor))), "ClientA")
  val clientActorB = SystemBookingService.actorOf(Props(new Client(ClientIdentity("Miguel Alejandro Chukiwanka",30,"102452"), SearchMessage(getRandomTypeProperty,getRandomDate,systemServiceActor))), "ClientB")
  val clientActorC = SystemBookingService.actorOf(Props(new Client(ClientIdentity("Lourdes Chukiwanka",34,"102053"), SearchMessage(getRandomTypeProperty,getRandomDate,systemServiceActor))), "ClientC")
  val clientActorD = SystemBookingService.actorOf(Props(new Client(ClientIdentity("Stephanie Lujan",28,"102454"), SearchMessage(getRandomTypeProperty,getRandomDate,systemServiceActor))), "ClientD")
  val clientActorE = SystemBookingService.actorOf(Props(new Client(ClientIdentity("Pamela Talavera",37,"102359"), SearchMessage(getRandomTypeProperty,getRandomDate,systemServiceActor))), "ClientE")
  Thread.sleep(1000)
  SystemBookingService.terminate()

}
