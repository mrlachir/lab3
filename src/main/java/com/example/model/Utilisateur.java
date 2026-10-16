package com.example.model;
import javax.persistence.*;
import java.util.*;

@Entity
public class Utilisateur {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String nom;
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Reservation> reservations = new HashSet<>();

    public Utilisateur() {}
    public Utilisateur(String nom, String email) { this.nom = nom; this.email = email; }
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Set<Reservation> getReservations() { return Collections.unmodifiableSet(reservations); }
    public void addReservation(Reservation r) { Objects.requireNonNull(r).setUtilisateur(this); }
    public void removeReservation(Reservation r) {
        if (r != null && r.getUtilisateur() == this) r.setUtilisateur(null);
    }
    void attach(Reservation r) { reservations.add(r); }
    void detach(Reservation r) { reservations.remove(r); }
    @Override public String toString() { return "Utilisateur{id=" + id + ", nom='" + nom + "'}"; }
}
