package com.sample.hotel.listener;

import com.sample.hotel.app.BookingService;
import com.sample.hotel.entity.Booking;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.core.event.EntitySavingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BookingEventListener {

	private final BookingService bookingService;

	public BookingEventListener(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@EventListener
	public void onBookingSaving(final EntitySavingEvent<Booking> event) {
		Booking booking = event.getEntity();
		booking.setDepartureDate(booking.getArrivalDate().plusDays(booking.getNightsOfStay()));
	}

	@EventListener
	public void onBookingChangedBeforeCommit(final EntityChangedEvent<Booking> event) {
		if (event.getChanges().isChanged("status")) {
			bookingService.cancelRoomReservation(event.getEntityId());
		}
	}
}