package de.gsi.dataset.event.queue;

import java.lang.ref.WeakReference;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.RingBuffer;

import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.EventSource;
import de.gsi.dataset.event.EventThreadHelper;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.event.queue.EventQueue.RingEvent;
import io.micrometer.core.instrument.Metrics;

/**
 * Event Queue Listener gets periodically called and retrieves the new events since its last invocation from the event
 * queue.
 * Depending on its strategy it calls its callback function for events.
 * There is a ThreadPool which cycles over all EventQueueListeners and executes them.
 *
 * @author Alexander Krimm
 */
public class EventQueueListener implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventQueueListener.class);

    private EventListener listener;
    private EventQueueListenerStrategy strategy; // strategy how listeners should be invoked for events
    private WeakReference<EventSource> filterSource; // the source to listen to
    private Class<? extends UpdateEvent> filterEventType; // the event type to listen to
    private Predicate<RingEvent> filterPredicate; // a filter for selecting matching events

    private BatchEventProcessor<RingEvent> processor;

    private RingEvent lastEvt;

    public EventQueueListener(final RingBuffer<RingEvent> queue, final EventListener listener, final EventQueueListenerStrategy strategy,
            Class<? extends UpdateEvent> eventType, EventSource source, Predicate<RingEvent> filter) {
        this.listener = listener;
        this.strategy = strategy;
        this.filterSource = new WeakReference<>(source);
        this.filterEventType = eventType;
        this.filterPredicate = filter;

        processor = new BatchEventProcessor<>(queue, queue.newBarrier(), this::handle);
    }

    public void setListener(EventListener listener) {
        this.listener = listener;
    }

    public enum EventQueueListenerStrategy {
        // multi-threaded strategies
        M_LAST_DROP, // only run the listener on the last matching event, dropping if the listener is still running
        M_LAST_INTERRUPT, // like M_LAST_DROP, but cancel a currently running Listener
        M_LAST_RESCHEDULE, // like M_LAST_DROP, but ensure, that the last event gets rescheduled if no listener is running
        M_EVERY, // run every Event in parallel
        // single-threaded strategies
        S_ALL, // run every event on this thread
        S_LAST; // run the last event on this thread
    }

    /**
     * @param evt The RingEvent which occurred
     * @param evtId The id of the event in the buffer
     * @param endOfBatch whether the event was the last in a series of processed events.
     */
    public void handle(final RingEvent evt, final long evtId, final boolean endOfBatch) {
        if ((filterSource.get() != null && filterSource.get() != evt.evt.getSource()) 
                || !filterEventType.isAssignableFrom(evt.evt.getClass())
                || !filterPredicate.test(evt)) {
            if (strategy == EventQueueListenerStrategy.M_LAST_DROP && lastEvt != null) {
                this.listener.handle(lastEvt.evt, evt.getId());
                lastEvt = null;
            }
            return;
        }
        // measure latency as time between adding event to the buffer and now
        evt.getSubmitTimestamp().stop(Metrics.timer("chartfx.events.latency", "eventType", evt.evt.getClass().getSimpleName()));
        switch (strategy) {
        case M_EVERY:
            this.listener.handle(evt.evt, evt.getId());
            break;
        case M_LAST_DROP:
            if (endOfBatch) {
                this.listener.handle(evt.evt, evt.getId());
                lastEvt = null;
            } else {
                lastEvt = evt;
            }
            break;
        case M_LAST_INTERRUPT:
            if (endOfBatch) {
                this.listener.handle(evt.evt, evt.getId());
            }
            break;
        case M_LAST_RESCHEDULE:
            if (endOfBatch) {
                this.listener.handle(evt.evt, evt.getId());
            }
            break;
        case S_ALL:
            this.listener.handle(evt.evt, evt.getId());
            break;
        case S_LAST:
            if (endOfBatch) {
                this.listener.handle(evt.evt, evt.getId());
            }
            break;
        default:
            break;
        }
        evt.getSubmitTimestamp().stop(Metrics.timer("chartfx.events.latency.post", "eventType", evt.evt.getClass().getSimpleName()));
    }

    public void execute() {
        EventThreadHelper.getExecutorService().execute(this);
    }

    @Override
    public void run() {
        processor.run();
    }

    public void halt() {
        processor.halt();
    }
}
