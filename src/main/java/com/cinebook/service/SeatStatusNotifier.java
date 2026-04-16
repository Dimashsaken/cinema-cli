package com.cinebook.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer pattern: notifies listeners when seats are released (e.g. hold expiry).
 *
 * <p>CLI sessions subscribe to refresh their seat map when a hold expires.
 * Uses CopyOnWriteArrayList for thread-safe iteration during notification.
 */
public class SeatStatusNotifier {

    /** Listener interface for seat release events. */
    @FunctionalInterface
    public interface SeatReleaseListener {
        void onSeatsReleased(String showtimeId, List<String> seatCodes);
    }

    private final List<SeatReleaseListener> listeners = new CopyOnWriteArrayList<>();

    public void subscribe(SeatReleaseListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(SeatReleaseListener listener) {
        listeners.remove(listener);
    }

    /** Notify all listeners that seats were released. */
    public void notifySeatsReleased(String showtimeId, List<String> seatCodes) {
        for (SeatReleaseListener listener : listeners) {
            listener.onSeatsReleased(showtimeId, seatCodes);
        }
    }
}
