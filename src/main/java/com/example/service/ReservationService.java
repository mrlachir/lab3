package com.example.service;
import com.example.model.*;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

public class ReservationService {
    private final EntityManager em;
    public ReservationService(EntityManager em) { this.em = em; }
    // Half-open intervals [start,end): consecutive bookings are allowed.
    public boolean salleDisponible(Long salleId, LocalDateTime debut, LocalDateTime fin) {
        Reservation.verifierPeriode(debut, fin);
        if (em.find(Salle.class, salleId) == null) throw new IllegalArgumentException("Unknown room");
        return em.createQuery("SELECT COUNT(r) FROM Reservation r WHERE r.salle.id = :id "
                + "AND r.dateDebut < :fin AND r.dateFin > :debut", Long.class)
                .setParameter("id", salleId).setParameter("debut", debut).setParameter("fin", fin)
                .getSingleResult() == 0L;
    }
    public Reservation reserver(Long utilisateurId, Long salleId, LocalDateTime debut, LocalDateTime fin) {
        if (!em.getTransaction().isActive()) throw new IllegalStateException("An active transaction is required");
        Reservation.verifierPeriode(debut, fin);
        // Serialize booking attempts using this service for the same room.
        Salle salle = em.find(Salle.class, salleId, LockModeType.PESSIMISTIC_WRITE);
        Utilisateur user = em.find(Utilisateur.class, utilisateurId);
        if (salle == null || user == null) throw new IllegalArgumentException("Unknown room or user");
        if (!salleDisponible(salleId, debut, fin)) throw new IllegalStateException("Room is unavailable");
        Reservation r = new Reservation(debut, fin, salle);
        user.addReservation(r);
        em.persist(r);
        return r;
    }
    public List<Reservation> reservationsUtilisateur(Long id) {
        return em.createQuery("SELECT r FROM Reservation r WHERE r.utilisateur.id = :id ORDER BY r.dateDebut",
                Reservation.class).setParameter("id", id).getResultList();
    }
    public List<Salle> sallesAvecEquipement(String nom) {
        return em.createQuery("SELECT DISTINCT s FROM Salle s JOIN s.equipements e WHERE e.nom = :nom",
                Salle.class).setParameter("nom", nom).getResultList();
    }
}
