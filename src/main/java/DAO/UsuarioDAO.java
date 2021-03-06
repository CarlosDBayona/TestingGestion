/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DAO;

import DAO.exceptions.IllegalOrphanException;
import DAO.exceptions.NonexistentEntityException;
import DAO.exceptions.PreexistingEntityException;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import Datos.Prestamo;
import Datos.Usuario;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 *
 * @author Carlos
 */
public class UsuarioDAO implements Serializable {
    private EntityManager em=null;
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Usuario usuario) throws PreexistingEntityException, Exception {
        if (usuario.getPrestamoCollection() == null) {
            usuario.setPrestamoCollection(new ArrayList<Prestamo>());
        }
        startOperation();
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Collection<Prestamo> attachedPrestamoCollection = new ArrayList<Prestamo>();
            for (Prestamo prestamoCollectionPrestamoToAttach : usuario.getPrestamoCollection()) {
                prestamoCollectionPrestamoToAttach = em.getReference(prestamoCollectionPrestamoToAttach.getClass(), prestamoCollectionPrestamoToAttach.getCodprestamo());
                attachedPrestamoCollection.add(prestamoCollectionPrestamoToAttach);
            }
            usuario.setPrestamoCollection(attachedPrestamoCollection);
            em.persist(usuario);
            for (Prestamo prestamoCollectionPrestamo : usuario.getPrestamoCollection()) {
                Usuario oldIdusuarioOfPrestamoCollectionPrestamo = prestamoCollectionPrestamo.getIdusuario();
                prestamoCollectionPrestamo.setIdusuario(usuario);
                prestamoCollectionPrestamo = em.merge(prestamoCollectionPrestamo);
                if (oldIdusuarioOfPrestamoCollectionPrestamo != null) {
                    oldIdusuarioOfPrestamoCollectionPrestamo.getPrestamoCollection().remove(prestamoCollectionPrestamo);
                    oldIdusuarioOfPrestamoCollectionPrestamo = em.merge(oldIdusuarioOfPrestamoCollectionPrestamo);
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            if (findUsuario(usuario.getIdusuario()) != null) {
                throw new PreexistingEntityException("Usuario " + usuario + " already exists.", ex);
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
                emf.close();
            }
        }
    }

    public void edit(Usuario usuario) throws IllegalOrphanException, NonexistentEntityException, Exception {
        startOperation();
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Usuario persistentUsuario = em.find(Usuario.class, usuario.getIdusuario());
            Collection<Prestamo> prestamoCollectionOld = persistentUsuario.getPrestamoCollection();
            Collection<Prestamo> prestamoCollectionNew = usuario.getPrestamoCollection();
            List<String> illegalOrphanMessages = null;
            for (Prestamo prestamoCollectionOldPrestamo : prestamoCollectionOld) {
                if (!prestamoCollectionNew.contains(prestamoCollectionOldPrestamo)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Prestamo " + prestamoCollectionOldPrestamo + " since its idusuario field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            Collection<Prestamo> attachedPrestamoCollectionNew = new ArrayList<Prestamo>();
            for (Prestamo prestamoCollectionNewPrestamoToAttach : prestamoCollectionNew) {
                prestamoCollectionNewPrestamoToAttach = em.getReference(prestamoCollectionNewPrestamoToAttach.getClass(), prestamoCollectionNewPrestamoToAttach.getCodprestamo());
                attachedPrestamoCollectionNew.add(prestamoCollectionNewPrestamoToAttach);
            }
            prestamoCollectionNew = attachedPrestamoCollectionNew;
            usuario.setPrestamoCollection(prestamoCollectionNew);
            usuario = em.merge(usuario);
            for (Prestamo prestamoCollectionNewPrestamo : prestamoCollectionNew) {
                if (!prestamoCollectionOld.contains(prestamoCollectionNewPrestamo)) {
                    Usuario oldIdusuarioOfPrestamoCollectionNewPrestamo = prestamoCollectionNewPrestamo.getIdusuario();
                    prestamoCollectionNewPrestamo.setIdusuario(usuario);
                    prestamoCollectionNewPrestamo = em.merge(prestamoCollectionNewPrestamo);
                    if (oldIdusuarioOfPrestamoCollectionNewPrestamo != null && !oldIdusuarioOfPrestamoCollectionNewPrestamo.equals(usuario)) {
                        oldIdusuarioOfPrestamoCollectionNewPrestamo.getPrestamoCollection().remove(prestamoCollectionNewPrestamo);
                        oldIdusuarioOfPrestamoCollectionNewPrestamo = em.merge(oldIdusuarioOfPrestamoCollectionNewPrestamo);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                String id = usuario.getIdusuario();
                if (findUsuario(id) == null) {
                    throw new NonexistentEntityException("The usuario with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
                emf.close();
            }
        }
    }

    public void destroy(String id) throws IllegalOrphanException, NonexistentEntityException {
        startOperation();
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Usuario usuario;
            try {
                usuario = em.getReference(Usuario.class, id);
                usuario.getIdusuario();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The usuario with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            Collection<Prestamo> prestamoCollectionOrphanCheck = usuario.getPrestamoCollection();
            for (Prestamo prestamoCollectionOrphanCheckPrestamo : prestamoCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Usuario (" + usuario + ") cannot be destroyed since the Prestamo " + prestamoCollectionOrphanCheckPrestamo + " in its prestamoCollection field has a non-nullable idusuario field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            em.remove(usuario);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
                emf.close();
            }
        }
    }

    public List<Usuario> findUsuarioEntities() {
        return findUsuarioEntities(true, -1, -1);
    }

    public List<Usuario> findUsuarioEntities(int maxResults, int firstResult) {
        return findUsuarioEntities(false, maxResults, firstResult);
    }

    private List<Usuario> findUsuarioEntities(boolean all, int maxResults, int firstResult) {
        startOperation();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Usuario.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
            emf.close();
        }
    }

    public Usuario findUsuario(String id) {
        startOperation();
        try {
            return em.find(Usuario.class, id);
        } finally {
            em.close();
            emf.close();
        }
    }

    public int getUsuarioCount() {
        startOperation();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Usuario> rt = cq.from(Usuario.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
            emf.close();
        }
    }
    protected void startOperation() { 
        URI dbUri = null;
        try {
            dbUri = new URI(System.getenv("DATABASE_URL")); 
            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();

            Map<String, String> properties = new HashMap<String, String>();
            properties.put("javax.persistence.jdbc.url", dbUrl);
            properties.put("javax.persistence.jdbc.user", username );
            properties.put("javax.persistence.jdbc.password", password );
            properties.put("javax.persistence.jdbc.driver", "org.postgresql.Driver");
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            this.emf = Persistence.createEntityManagerFactory("GestionInventario",properties);
            this.em = emf.createEntityManager();
        } catch (URISyntaxException ex) {
            Logger.getLogger(UsuarioDAO.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
