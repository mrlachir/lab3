package com.example;
import com.example.model.*;
import com.example.service.ReservationService;
import com.example.util.Transactions;
import javax.persistence.*;
import java.time.LocalDateTime;

public class App {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion-reservations-pu");
        try {
            EntityManager em = emf.createEntityManager();
            try {
                Utilisateur alice = new Utilisateur("Alice Dupont", "alice@example.com");
                Salle a = new Salle("Salle A", 20);
                Equipement projecteur = new Equipement("Projecteur");
                a.addEquipement(projecteur);
                LocalDateTime debut = LocalDateTime.of(2026, 11, 1, 9, 0);
                Reservation r1 = new Reservation(debut, debut.plusHours(1), a);
                Reservation r2 = new Reservation(debut.plusHours(2), debut.plusHours(3), a);
                alice.addReservation(r1);
                alice.addReservation(r2);
                Transactions.executer(em, context -> context.persist(alice));
                System.out.println("CASCADE PERSIST: user=" + alice.getId() + ", room=" + a.getId()
                        + ", equipment=" + projecteur.getId() + ", reservations=2");
                Salle b = new Salle("Salle B", 10);
                b.addEquipement(projecteur);
                Transactions.executer(em, context -> context.persist(b));
                ReservationService service = new ReservationService(em);
                System.out.println("USER RESERVATIONS: " + service.reservationsUtilisateur(alice.getId()));
                System.out.println("ROOMS WITH PROJECTOR: " + service.sallesAvecEquipement("Projecteur"));
                System.out.println("AVAILABLE DURING BOOKING: " + service.salleDisponible(a.getId(), debut, debut.plusMinutes(30)));
                System.out.println("AVAILABLE AFTER BOOKING: " + service.salleDisponible(a.getId(), debut.plusHours(1), debut.plusHours(2)));
                Long orphanId = r1.getId();
                Transactions.executer(em, context -> alice.removeReservation(r1));
                em.clear();
                System.out.println("ORPHAN REMOVED: " + (em.find(Reservation.class, orphanId) == null));
                Transactions.executer(em, context -> context.remove(context.find(Utilisateur.class, alice.getId())));
                em.clear();
                System.out.println("RESERVATIONS AFTER USER DELETE: "
                        + em.createQuery("SELECT COUNT(r) FROM Reservation r", Long.class).getSingleResult());
                Transactions.executer(em, context -> {
                    Salle room = context.find(Salle.class, a.getId());
                    for (Equipement e : new java.util.HashSet<>(room.getEquipements())) room.removeEquipement(e);
                    context.remove(room);
                });
                em.clear();
                System.out.println("SHARED EQUIPMENT PRESERVED: " + (em.find(Equipement.class, projecteur.getId()) != null));
                System.out.println("REMAINING ROOMS: " + em.createQuery("SELECT s FROM Salle s", Salle.class).getResultList());
            } finally { em.close(); }
        } finally { emf.close(); }
    }
}
