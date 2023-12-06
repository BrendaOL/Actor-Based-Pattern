/*case class Property(id:Int, name:String, pType:String, category:String, location:(String,String), reservations:List[String])

class DBReservation() {
  // Possible properties
  val properties = Seq(
    Property(1,"Property priveé","hotel","1 stars",("Belgium","Brussels"),List("01/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(2,"Luxury Sea","hotel","2 stars",("EEUU","Los angeles"),List("02/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(3,"White property","hotel","3 stars",("France","Paris"),List("03/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(4,"Extreme property","hotel","4 stars",("Russia","Moscu"),List("03/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(5,"Gems","hotel","5 stars",("Peru","Arequipa"),List("03/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(6,"Apartment A","apartments","1 stars",("Peru","Arequipa"),List("03/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(7,"Apartment B","apartments","2 stars",("EEUU","Texas"),List("04/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(8,"Apartment C","apartments","3 stars",("EEUU","Texas"),List("05/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(9,"Apartment D","apartments","4 stars",("Belgium","Brussels"),List("03/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(10,"Apartment E","apartments","5 stars",("Canada","Montreal"),List("08/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(11,"Resort Stay","resorts","1 stars",("Argentina","Buenos Aires"),List("09/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(12,"Resort Join","resorts","2 stars",("Colombia","Bogotá"),List("06/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(13,"Resort Peace","resorts","3 stars",("Belgium","Brussels"),List("07/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(14,"Resort Green","resorts","4 stars",("Belgium","Antwerp"),List("08/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021")),
    Property(15,"Resort White","resorts","5 stars",("Peru","Arequipa"),List("08/12/2021","24/12/2021","25/12/2021","26/12/2021","31/12/2021"))
  )

  //Brings the type of property that is available to book
  def getPropertyAvailable(typeProperty:String, date: String): List[Property] = {
    var options:List[Property] = List()
    properties.map(element => {
      if(element.pType == typeProperty)
        if(!element.reservations.contains(date)) {
          options :+= element
        }
    })
    options
  }
}
*/