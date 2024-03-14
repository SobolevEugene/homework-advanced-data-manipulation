package com.sample.hotel.app;

import com.sample.hotel.entity.Booking;
import com.sample.hotel.entity.Room;
import com.sample.hotel.entity.RoomReservation;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Component
public class BookingService {

	private final DataManager dataManager;
	@PersistenceContext
	private EntityManager entityManager;

	public BookingService(DataManager dataManager) {
		this.dataManager = dataManager;
	}

	/**
	 * Check if given room is suitable for the booking.
	 * 1) Check that sleeping places is enough to fit numberOfGuests.
	 * 2) Check that there are no reservations for this room at the same range of dates.
	 * Use javax.persistence.EntityManager and JPQL query for querying database.
	 *
	 * @param booking booking
	 * @param room    room
	 * @return true if checks are passed successfully
	 */
	public boolean isSuitable(Booking booking, Room room) {

		boolean sleepingPlacesCondition = room.getSleepingPlaces() >= booking.getNumberOfGuests();
		boolean dateCondition = (long) entityManager
				.createNativeQuery("select count(*) from ROOM_RESERVATION rr " +
						"left join BOOKING b on b.id = rr.booking_id " +
						"where rr.ROOM_ID = #roomId " +
						"and b.ARRIVAL_DATE <= #bookingArrivalDate " +
						"and b.DEPARTURE_DATE >= #bookingDepartureDate")
				.setParameter("roomId", room.getId())
				.setParameter("bookingArrivalDate", booking.getArrivalDate())
				.setParameter("bookingDepartureDate", booking.getDepartureDate())
				.getSingleResult() == 0;

		return sleepingPlacesCondition && dateCondition;
	}

	/**
	 * Check that room is suitable for the booking, and create a reservation for this room.
	 *
	 * @param room    room to reserve
	 * @param booking hotel booking
	 *                Wrap operation into a transaction (declarative or manual).
	 * @return created reservation object, or null if room is not suitable
	 */
	@Transactional
	public RoomReservation reserveRoom(Booking booking, Room room) {
		if (!isSuitable(booking, room)) {
			return null;
		}
		RoomReservation roomReservation = dataManager.create(RoomReservation.class);
		roomReservation.setRoom(room);
		roomReservation.setBooking(booking);
		return dataManager.save(roomReservation);
	}

	public void cancelRoomReservation(Id bookingId) {
		RoomReservation roomReservation = dataManager.loadValue("select r from RoomReservation r " +
						"where r.booking.id = :bookingId",
				RoomReservation.class).parameter("bookingId", bookingId).one();

		dataManager.remove(roomReservation);
	}
}