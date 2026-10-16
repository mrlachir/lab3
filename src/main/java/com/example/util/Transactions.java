package com.example.util;
import javax.persistence.*;
import java.util.function.Consumer;
import java.util.logging.*;

public final class Transactions {
    private static final Logger LOG = Logger.getLogger(Transactions.class.getName());
    private Transactions() {}
    public static void executer(EntityManager em, Consumer<EntityManager> action) {
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            action.accept(em);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                try { tx.rollback(); } catch (RuntimeException rollbackError) { e.addSuppressed(rollbackError); }
            }
            // A failed persistence context should not be reused with stale managed entities.
            em.clear();
            LOG.log(Level.WARNING, "Transaction failed and was rolled back", e);
            throw e;
        }
    }
}
