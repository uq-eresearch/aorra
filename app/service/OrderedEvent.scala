package service

import service.EventManager.Event;

case class OrderedEvent(val id: String, val event: Event)
  extends Ordered[OrderedEvent] {

  /**
   * Temporal order of events is the same as natural ID order.
   */
  override def compare(that: OrderedEvent) = id.compareTo(that.id)

}