package com.example;

import com.example.model.*;
import com.example.service.ReservationService;
import com.example.util.Transactions;
import javax.persistence.*;
import org.junit.*;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.Assert.*;

public class ReservationTest {
    private EntityManagerFactory emf;
    private EntityManager em;
    private Utilisateur user;
    private Salle salle;
    private Equipement equipment;
    private Reservation reservation;
    private final LocalDateTime start = LocalDateTime.of(2026, 11, 1, 9, 0);

    @Before public void setup() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("javax.persistence.jdbc.url",
                "jdbc:h2:mem:lab3_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        properties.put("hibernate.show_sql", "false");
        emf = Persistence.createEntityManagerFactory("gestion-reservations-pu", properties);
        em = emf.createEntityManager();
        user = new Utilisateur("Alice", "alice@example.com");
        salle = new Salle("Salle A", 20);
        equipment = new Equipement("Projecteur");
        salle.addEquipement(equipment);
        reservation = new Reservation(start, start.plusHours(1), salle);
        user.addReservation(reservation);
        Transactions.executer(em, context -> context.persist(user));
    }
    @After public void cleanup() {
        try {
            if (em != null && em.isOpen()) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                em.close();
            }
        } finally { if (emf != null && emf.isOpen()) emf.close(); }
    }
    @Test public void cascadePersistAndJoinTable() {
        assertNotNull(user.getId());
        assertNotNull(reservation.getId());
        assertNotNull(salle.getId());
        assertNotNull(equipment.getId());
        em.clear();
        assertEquals(1, em.find(Utilisateur.class, user.getId()).getReservations().size());
        assertEquals(1, em.find(Salle.class, salle.getId()).getReservations().size());
        assertEquals(1, em.find(Equipement.class, equipment.getId()).getSalles().size());
        assertEquals(1L, ((Number) em.createNativeQuery("SELECT COUNT(*) FROM salle_equipement").getSingleResult()).longValue());
    }
    @Test public void orphanRemovalDeletesReservation() {
        Transactions.executer(em, context -> user.removeReservation(reservation));
        em.clear();
        assertNull(em.find(Reservation.class, reservation.getId()));
        assertNotNull(em.find(Salle.class, salle.getId()));
        assertTrue(em.find(Utilisateur.class, user.getId()).getReservations().isEmpty());
    }
    @Test public void deletingUserPreservesSharedEntities() {
        Transactions.executer(em, context -> context.remove(user));
        em.clear();
        assertNull(em.find(Utilisateur.class, user.getId()));
        assertNull(em.find(Reservation.class, reservation.getId()));
        assertNotNull(em.find(Salle.class, salle.getId()));
        assertNotNull(em.find(Equipement.class, equipment.getId()));
    }
    @Test public void deletingRoomPreservesEquipmentInAnotherRoom() {
        Salle other = new Salle("Salle B", 10);
        other.addEquipement(equipment);
        Transactions.executer(em, context -> context.persist(other));
        Transactions.executer(em, context -> context.remove(user));
        Transactions.executer(em, context -> {
            salle.removeEquipement(equipment);
            context.remove(salle);
        });
        em.clear();
        assertNull(em.find(Salle.class, salle.getId()));
        assertNotNull(em.find(Equipement.class, equipment.getId()));
        assertEquals(1, em.find(Salle.class, other.getId()).getEquipements().size());
        assertEquals(1, em.find(Equipement.class, equipment.getId()).getSalles().size());
    }
    @Test public void availabilityHandlesOverlapsAndBoundaries() {
        ReservationService service = new ReservationService(em);
        assertFalse(service.salleDisponible(salle.getId(), start, start.plusHours(1)));
        assertFalse(service.salleDisponible(salle.getId(), start.minusHours(1), start.plusHours(2)));
        assertFalse(service.salleDisponible(salle.getId(), start.plusMinutes(15), start.plusMinutes(30)));
        assertTrue(service.salleDisponible(salle.getId(), start.minusHours(1), start));
        assertTrue(service.salleDisponible(salle.getId(), start.plusHours(1), start.plusHours(2)));
    }
    @Test public void bookingServiceRejectsConflictAndAcceptsAdjacent() {
        ReservationService service = new ReservationService(em);
        assertThrows(IllegalStateException.class, () -> Transactions.executer(em,
                context -> service.reserver(user.getId(), salle.getId(), start, start.plusMinutes(30))));
        Transactions.executer(em, context -> service.reserver(user.getId(), salle.getId(),
                start.plusHours(1), start.plusHours(2)));
        em.clear();
        assertEquals(2, service.reservationsUtilisateur(user.getId()).size());
    }
    @Test public void queriesFilterUserAndEquipment() {
        ReservationService service = new ReservationService(em);
        assertEquals(1, service.reservationsUtilisateur(user.getId()).size());
        assertTrue(service.reservationsUtilisateur(-1L).isEmpty());
        assertEquals(salle.getId(), service.sallesAvecEquipement("Projecteur").get(0).getId());
        assertTrue(service.sallesAvecEquipement("Absent").isEmpty());
    }
    @Test public void datesValidatedByApiAndPersistenceLifecycle() {
        assertThrows(IllegalArgumentException.class, () -> new Reservation(start, start, salle));
        assertThrows(IllegalArgumentException.class, () -> new Reservation(start, start.minusHours(1), salle));
        assertThrows(IllegalArgumentException.class, () -> new Reservation(null, start, salle));
        assertThrows(RuntimeException.class, () -> Transactions.executer(em, context -> {
            reservation.setDateFin(start.minusHours(1));
            context.flush();
        }));
        assertFalse(em.getTransaction().isActive());
        assertEquals(start.plusHours(1), em.find(Reservation.class, reservation.getId()).getDateFin());
    }
    @Test public void relationshipHelpersSynchronizeBothSides() {
        Utilisateur other = new Utilisateur("Bob", "bob@example.com");
        Reservation r = new Reservation(start.plusHours(2), start.plusHours(3), salle);
        other.addReservation(r);
        assertTrue(other.getReservations().contains(r));
        assertTrue(salle.getReservations().contains(r));
        other.removeReservation(r);
        assertNull(r.getUtilisateur());
        assertFalse(other.getReservations().contains(r));
        salle.removeReservation(r);
        assertNull(r.getSalle());
        Salle b = new Salle("B", 10);
        equipment.addSalle(b);
        assertTrue(b.getEquipements().contains(equipment));
        assertTrue(equipment.getSalles().contains(b));
        equipment.removeSalle(b);
        assertTrue(b.getEquipements().isEmpty());
        assertFalse(equipment.getSalles().contains(b));
    }
    @Test public void duplicateEmailRollsBack() {
        assertThrows(PersistenceException.class, () -> Transactions.executer(em,
                context -> context.persist(new Utilisateur("Duplicate", "alice@example.com"))));
        assertFalse(em.getTransaction().isActive());
        assertEquals(Long.valueOf(1), em.createQuery("SELECT COUNT(u) FROM Utilisateur u", Long.class).getSingleResult());
    }
}
