package util;

/**
 * EventDispatcher - Central event bus for decoupled system communication
 * 
 * PURPOSE: Allows game systems (quests, achievements, combat, etc.) to communicate
 * without direct dependencies. Systems subscribe to event types and react to events.
 * 
 * IMPLEMENTATION:
 * - Uses custom DynArray to store listeners (no standard library)
 * - EventListener interface for callback pattern
 * - Event hierarchy starting with base Event class
 * - Thread-safe design not required (console game, single-threaded)
 * 
 * USAGE:
 *   EventDispatcher dispatcher = new EventDispatcher();
 *   dispatcher.subscribe("QUEST_COMPLETE", (event) -> unlockAchievement());
 *   dispatcher.dispatch(new GameEvent("QUEST_COMPLETE", questData));
 */

public class EventDispatcher {
    
    /**
     * Listener interface - implementations handle events
     */
    public interface EventListener {
        /**
         * Called when an event is dispatched
         * @param event The event that occurred
         */
        void onEvent(GameEvent event);
    }
    
    /**
     * Base event class - all events extend this
     */
    public static class GameEvent {
        private String eventType;
        private Object data;
        private long timestamp;
        
        public GameEvent(String eventType, Object data) {
            this.eventType = eventType;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getEventType() { return eventType; }
        public Object getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Internal container for listener subscriptions
     */
    private static class Subscription {
        String eventType;
        EventListener listener;
        
        Subscription(String eventType, EventListener listener) {
            this.eventType = eventType;
            this.listener = listener;
        }
    }
    
    // Storage for all listeners - uses DynArray for no external dependencies
    private DynArray<Subscription> subscriptions;
    
    /**
     * Initialize dispatcher
     */
    public EventDispatcher() {
        this.subscriptions = new DynArray<>();
    }
    
    /**
     * Subscribe a listener to a specific event type
     * @param eventType The type of event to listen for (e.g., "QUEST_COMPLETE")
     * @param listener The callback to invoke when event occurs
     */
    public void subscribe(String eventType, EventListener listener) {
        // Validation
        if (eventType == null || eventType.isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        subscriptions.add(new Subscription(eventType, listener));
    }
    
    /**
     * Unsubscribe a listener from a specific event type
     * @param eventType The event type to stop listening for
     * @param listener The listener to remove
     */
    public void unsubscribe(String eventType, EventListener listener) {
        // Iterate through subscriptions and remove matching ones
        for (int i = 0; i < subscriptions.size(); i++) {
            Subscription sub = subscriptions.get(i);
            if (sub.eventType.equals(eventType) && sub.listener == listener) {
                subscriptions.remove(i);
                i--; // Adjust index since we removed an element
            }
        }
    }
    
    /**
     * Dispatch an event to all listeners
     * @param event The event to dispatch
     */
    public void dispatch(GameEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        
        // Notify all listeners subscribed to this event type
        for (int i = 0; i < subscriptions.size(); i++) {
            Subscription sub = subscriptions.get(i);
            if (sub.eventType.equals(event.getEventType())) {
                try {
                    sub.listener.onEvent(event);
                } catch (Exception e) {
                    // Log error but continue dispatching to other listeners
                    System.err.println("Error in event listener for " + event.getEventType() + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Clear all subscriptions for a specific event type
     * @param eventType The event type to clear listeners for
     */
    public void clearEventType(String eventType) {
        for (int i = subscriptions.size() - 1; i >= 0; i--) {
            Subscription sub = subscriptions.get(i);
            if (sub.eventType.equals(eventType)) {
                subscriptions.remove(i);
            }
        }
    }
    
    /**
     * Clear all subscriptions
     */
    public void clearAll() {
        subscriptions = new DynArray<>();
    }
    
    /**
     * Get count of active subscriptions for debugging
     * @return Number of active subscriptions
     */
    public int getSubscriptionCount() {
        return subscriptions.size();
    }
}
