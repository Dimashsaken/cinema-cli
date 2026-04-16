package com.cinebook.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeatStatusNotifierTest {

    @Test
    void subscribe_receivesNotification() {
        SeatStatusNotifier notifier = new SeatStatusNotifier();
        List<String> received = new ArrayList<>();
        notifier.subscribe((id, seats) -> received.addAll(seats));

        notifier.notifySeatsReleased("ST001", List.of("A1", "B2"));

        assertEquals(List.of("A1", "B2"), received);
    }

    @Test
    void multipleListeners_allNotified() {
        SeatStatusNotifier notifier = new SeatStatusNotifier();
        List<String> r1 = new ArrayList<>();
        List<String> r2 = new ArrayList<>();
        notifier.subscribe((id, seats) -> r1.addAll(seats));
        notifier.subscribe((id, seats) -> r2.addAll(seats));

        notifier.notifySeatsReleased("ST001", List.of("A1"));

        assertEquals(1, r1.size());
        assertEquals(1, r2.size());
    }

    @Test
    void unsubscribe_noLongerReceives() {
        SeatStatusNotifier notifier = new SeatStatusNotifier();
        List<String> received = new ArrayList<>();
        SeatStatusNotifier.SeatReleaseListener listener = (id, seats) -> received.addAll(seats);

        notifier.subscribe(listener);
        notifier.notifySeatsReleased("ST001", List.of("A1"));
        assertEquals(1, received.size());

        notifier.unsubscribe(listener);
        notifier.notifySeatsReleased("ST001", List.of("B2"));
        assertEquals(1, received.size()); // no new notification
    }
}
