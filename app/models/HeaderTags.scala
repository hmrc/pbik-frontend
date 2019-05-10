package models


/**
  * The Header Tags are used between the PBIK gateway and NPS to control the optimistic locks.
  * Each time a call is made to NPS on an employer specific URL, NPS returns the current value of the optimistic lock for the employer record
  * We need to save that value and send it back in each time we wish to change the employer record ( i.e by updating the registered benefits or
  * excluding an individual ). If the Etag value does not match, NPS will reject the update as it indicates other changes have been made to the
  * employer record thereby invalidating ours ).
  */
object HeaderTags {
  val ETAG: String = "ETag"
  val X_TXID: String = "X-TXID"
}