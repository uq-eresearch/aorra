package service.filestore;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import play.Logger;
import service.EventTimeline;
import service.EventTimeline.ForgottenEventException;
import service.InMemoryEventTimeline;

public class EventManagerImpl implements EventManager {

  private final EventTimeline<String, Event> history =
      new InMemoryEventTimeline<Event>();

  private final Set<EventReceiver> receivers =
      Collections.synchronizedSet(
          new HashSet<EventReceiver>());

  @Override
  public String getLastEventId() {
    return history.getLastEventId();
  }

  @Override
  public Iterable<OrderedEvent> getSince(final String lastId) {
    final ImmutableList.Builder<OrderedEvent> b = ImmutableList.builder();
    Map<String, Event> missed;
    try {
      missed = lastId == null ? history.getKnown() : history.getSince(lastId);
    } catch (ForgottenEventException e1) {
      // TODO Handle forgotten history
      return ImmutableList.of(outOfDateOrderedEvent());
    }
    for (Map.Entry<String, Event> e : missed.entrySet()) {
      // Push event ID and event
      b.add(new OrderedEvent(e.getKey(), e.getValue()));
    }
    return b.build();
  }

  @Override
  public void tell(final EventReceiverMessage message) {
    switch (message.type) {
    case ADD:
      Logger.debug("Adding event receiver to " + this);
      performCatchup(message.er, message.lastId);
      receivers.add(message.er);
      break;
    case REMOVE:
      Logger.debug("Removing event receiver from " + this);
      receivers.remove(message.er);
      break;
    }
  }

  @Override
  public void tell(final Event event) {
    history.record(event);
    Logger.debug(String.format("%s pushing event to %d receivers: %s",
        this, receivers.size(), event));
    for (EventReceiver er : receivers) {
      // Push event ID and event
      er.push(new OrderedEvent(history.getLastEventId(), event));
    }
  }

  protected void performCatchup(
      final EventReceiver er,
      final String lastId) {
    Logger.debug("Catching up from "+lastId);
    if (lastId == null) {
      er.push(outOfDateOrderedEvent());
      er.end();
    } else {
      try {
        final Map<String, Event> missed = history.getSince(lastId);
        for (Map.Entry<String, Event> e : missed.entrySet()) {
          Logger.debug(String.format(
              "%s pushing missed event %s to receiver: %s",
              this, e.getKey(), er));
          // Push event ID and event
          er.push(new OrderedEvent(e.getKey(), e.getValue()));
        }
      } catch (ForgottenEventException e) {
        er.push(outOfDateOrderedEvent());
        // Close the channel
        er.end(e);
      }
    }
  }

  protected OrderedEvent outOfDateOrderedEvent() {
    return new OrderedEvent(history.getLastEventId(), Event.outOfDate());
  }

  @Override
  public String toString() {
    return "EM#"+System.identityHashCode(this);
  }

}