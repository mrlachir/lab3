package com.example.model;
import javax.persistence.*;
import java.util.*;

@Entity
public class Salle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String nom;
    private int capacite;
    @OneToMany(mappedBy = "salle")
    private Set<Reservation> reservations = new HashSet<>();
    // Equipment can be shared: never cascade REMOVE.
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "salle_equipement", joinColumns = @JoinColumn(name = "salle_id"),
            inverseJoinColumns = @JoinColumn(name = "equipement_id"))
    private Set<Equipement> equipements = new HashSet<>();

    public Salle() {}
    public Salle(String nom, int capacite) { this.nom = nom; this.capacite = capacite; }
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }
    public Set<Reservation> getReservations() { return Collections.unmodifiableSet(reservations); }
    public Set<Equipement> getEquipements() { return Collections.unmodifiableSet(equipements); }
    public void addReservation(Reservation r) { Objects.requireNonNull(r).setSalle(this); }
    public void removeReservation(Reservation r) {
        if (r != null && r.getSalle() == this) r.setSalle(null);
    }
    void attach(Reservation r) { reservations.add(r); }
    void detach(Reservation r) { reservations.remove(r); }
    public void addEquipement(Equipement e) {
        Objects.requireNonNull(e);
        if (equipements.add(e)) e.attach(this);
    }
    public void removeEquipement(Equipement e) {
        if (equipements.remove(e)) e.detach(this);
    }
    @Override public String toString() { return "Salle{id=" + id + ", nom='" + nom + "'}"; }
}
