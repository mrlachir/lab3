package com.example.model;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Reservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalDateTime dateDebut;
    @Column(nullable = false)
    private LocalDateTime dateFin;
    // Do not propagate REMOVE towards the user or the shared room.
    @ManyToOne(optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
    // PERSIST/MERGE complete the handout's user -> reservation -> room -> equipment chain.
    @ManyToOne(optional = false, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    public Reservation() {}
    public Reservation(LocalDateTime debut, LocalDateTime fin, Salle salle) {
        setPeriode(debut, fin); setSalle(salle);
    }
    public Long getId() { return id; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
    public void setPeriode(LocalDateTime debut, LocalDateTime fin) {
        verifierPeriode(debut, fin); dateDebut = debut; dateFin = fin;
    }
    public static void verifierPeriode(LocalDateTime debut, LocalDateTime fin) {
        if (debut == null || fin == null || !debut.isBefore(fin))
            throw new IllegalArgumentException("Start must be strictly before end.");
    }
    // Lifecycle validation also protects inserts/updates made outside the service.
    @PrePersist @PreUpdate
    private void validerDates() { verifierPeriode(dateDebut, dateFin); }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur nouveau) {
        if (utilisateur == nouveau) return;
        Utilisateur ancien = utilisateur;
        utilisateur = nouveau;
        if (ancien != null) ancien.detach(this);
        if (nouveau != null) nouveau.attach(this);
    }
    public Salle getSalle() { return salle; }
    public void setSalle(Salle nouvelle) {
        if (salle == nouvelle) return;
        Salle ancienne = salle;
        salle = nouvelle;
        if (ancienne != null) ancienne.detach(this);
        if (nouvelle != null) nouvelle.attach(this);
    }
    @Override public String toString() {
        return "Reservation{id=" + id + ", debut=" + dateDebut + ", fin=" + dateFin + "}";
    }
}
