package org.example.weektelevenassignment;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final AppUserRepository userRepository;

    public ReservationService(RoomRepository roomRepository,
                              ReservationRepository reservationRepository,
                              AppUserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
    }

    public List<Reservation> findByRoom(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new NotFoundException("room " + roomId + " not found");
        }
        return reservationRepository.findByRoomIdOrderByStartTime(roomId);
    }

    // The owner comes from the login, never from the request
    @Transactional
    public Reservation create(Long roomId, String username,
                              LocalDateTime startTime, LocalDateTime endTime) {
        checkTimes(startTime, endTime);                                         // 400
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("room " + roomId + " not found"));  // 404
        if (reservationRepository.countOverlapping(roomId, startTime, endTime) > 0) {
            throw new ConflictException("room " + roomId + " is already booked at that time");  // 409
        }
        AppUser owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("user " + username + " not found"));
        return reservationRepository.save(new Reservation(room, owner, startTime, endTime));
    }

    public List<Reservation> findMine(String username) {
        return reservationRepository.findMine(username);
    }

    // Read one: the owner or an admin
    @Transactional(readOnly = true)
    public Reservation get(Long id, String username, boolean admin) {
        Reservation r = find(id);                     // 404 first
        requireOwnerOrAdmin(r, username, admin);      // then 403
        return r;
    }

    // Cancel: the owner or an admin
    @Transactional
    public void delete(Long id, String username, boolean admin) {
        Reservation r = find(id);
        requireOwnerOrAdmin(r, username, admin);
        reservationRepository.delete(r);
    }

    private Reservation find(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("reservation " + id + " not found"));
    }

    private void requireOwnerOrAdmin(Reservation r, String username, boolean admin) {
        if (!admin && !r.isOwnedBy(username)) {
            throw new ForbiddenException("reservation " + r.getId() + " belongs to another user");
        }
    }

    private void checkTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
    }
}
