package service.filestore;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import play.Logger;
import play.api.libs.iteratee.Concurrent.Channel;
import scala.Tuple2;
import service.EventTimeline;
import service.EventTimeline.ForgottenEventException;
import service.InMemoryEventTimeline;

public class EventManagerImpl implements EventManager {

  private final EventTimeline<String, Event> history =
      new InMemoryEventTimeline<Event>();

  private final Set<Channel<Tuple2<String, Event>>> channels =
      Collections.synchronizedSet(
          new HashSet<Channel<Tuple2<String, Event>>>());

  @Override
  public String getLastEventId() {
    return history.getLastEventId();
  }

  @Override
  public Iterable<Tuple2<String,Event>> getSince(final String lastId) {
    final ImmutableList.Builder<Tuple2<String,Event>> b =
        ImmutableList.<Tuple2<String,Event>>builder();
    Map<String, Event> missed;
    try {
      missed = lastId == null ? history.getKnown() : history.getSince(lastId);
    } catch (ForgottenEventException e1) {
      // TODO Handle forgotten history
      return ImmutableList.of(outOfDateTuple());
    }
    for (Map.Entry<String, Event> e : missed.entrySet()) {
      // Push event ID and event
      b.add(new Tuple2<String, Event>(e.getKey(), e.getValue()));
    }
    return b.build();
  }

  @Override
  public void tell(final ChannelMessage<Tuple2<String, Event>> message) {
    switch (message.type) {
    case ADD:
      Logger.debug(this+" - Adding notification channel.");
      performCatchup(message.channel, message.lastId);
      channels.add(message.channel);
      break;
    case REMOVE:
      Logger.debug(this+" - Removing notification channel.");
      channels.remove(message.channel);
      break;
    }
  }

  @Override
  public void tell(final Event event) {
    Logger.debug(this+" - Adding event to history: "+event);
    history.record(event);
    Logger.debug(String.format("%s - Pushing event to %d channels: %s",
        this, channels.size(), event));
    for (Channel<Tuple2<String, Event>> channel : channels) {
      // Push event ID and event
      channel.push(new Tuple2<String, Event>(history.getLastEventId(), event));
    }
  }

  protected void performCatchup(
      final Channel<Tuple2<String, Event>> channel,
      final String lastId) {
    Logger.debug("Catching up from "+lastId);
    if (lastId == null) {
      channel.push(outOfDateTuple());
      channel.end();
    } else {
      try {
        final Map<String, Event> missed = history.getSince(lastId);
        for (Map.Entry<String, Event> e : missed.entrySet()) {
          Logger.debug(this+"- Pushing missed event "+e+" to channel: "+channel);
          // Push event ID and event
          channel.push(new Tuple2<String, Event>(
              e.getKey(), e.getValue()));
        }
      } catch (ForgottenEventException e) {
        channel.push(outOfDateTuple());
        // Close the channel
        channel.end(e);
      }
    }
  }

  protected Tuple2<String, Event> outOfDateTuple() {
    return new Tuple2<String, Event>(
        history.getLastEventId(), Event.outOfDate());
  }

}