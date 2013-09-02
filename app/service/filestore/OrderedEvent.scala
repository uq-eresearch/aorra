package service.filestore

import service.filestore.EventManager.Event

case class OrderedEvent(val id: String, val event: Event)