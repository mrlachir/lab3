package com.example.model;
import javax.persistence.*;
import java.util.*;

@Entity
public class Equipement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String nom;
    @ManyToMany(mappedBy = "equipements")
    private Set<Salle> salles = new HashSet<>();
    public Equipement() {}
    public Equipement(String nom) { this.nom = nom; }
    public Long getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public Set<Salle> getSalles() { return Collections.unmodifiableSet(salles); }
    public void addSalle(Salle salle) { salle.addEquipement(this); }
    public void removeSalle(Salle salle) { salle.removeEquipement(this); }
    void attach(Salle salle) { salles.add(salle); }
    void detach(Salle salle) { salles.remove(salle); }
}
