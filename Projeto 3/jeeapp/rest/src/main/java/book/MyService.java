package book;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import data.Client;
import data.ClientInfo;
import data.Currency;
import data.Manager;
import data.ManagerInfo;
import data.Results;
import data.ResultsInfo;

@RequestScoped
@Path("/myservice")
@Produces(MediaType.APPLICATION_JSON)
public class MyService {

    @PersistenceContext(name = "proj3", type = javax.persistence.PersistenceContextType.EXTENDED)
    EntityManager em;

    @GET
    @Path("/test")
    public String test() {
        return "Success";
    }

    @GET
    @Path("/listClients")
    public List<ClientInfo> listClients() {

        TypedQuery<Client> q = em.createQuery("from Client c", Client.class);

        List<Client> l = q.getResultList();

        List<ClientInfo> clients = new ArrayList<>();

        for (Client c : l)
            clients.add(new ClientInfo(c.getName(), c.getManager().getId(), c.getBalance(), c.getPayments(),
                    c.getCredits(), c.getCreditsTimed(), c.getPaymentsTimed()));

        return clients;
    }

    @GET
    @Path("/listManagers")
    public List<ManagerInfo> listManagers() {

        TypedQuery<Manager> q = em.createQuery("from Manager m", Manager.class);

        List<Manager> l = q.getResultList();

        List<ManagerInfo> managers = new ArrayList<>();
        for (Manager m : l)
            managers.add(new ManagerInfo(m.getName(), m.getId()));

        return managers;
    }

    @GET
    @Path("/listCurrency")
    public List<Currency> listCurrency() {

        TypedQuery<Currency> q = em.createQuery("from Currency c", Currency.class);

        List<Currency> l = q.getResultList();

        return l;
    }

    @POST
    @Transactional
    @Path("/addManager")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addManager(Manager m) {

        if (m.getName().equals(""))
            return Response.status(Status.BAD_REQUEST).entity("Error - invalid name").build();

        em.persist(m);

        return Response.ok().entity(m).build();
    }

    @POST
    @Transactional
    @Path("/addClient")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addClient(ClientInfo ci) {

        if (ci.getName().equals(""))
            return Response.status(Status.BAD_REQUEST).entity("Error - invalid name").build();

        TypedQuery<Manager> q = em.createQuery("from Manager m where m.id = :managerId", Manager.class);

        q.setParameter("managerId", ci.getManager());

        Client c = new Client(ci.getName(), q.getSingleResult());

        em.persist(c);

        return Response.ok().entity(c).build();
    }

    @POST
    @Transactional
    @Path("/addCurrency")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addCurrency(Currency c) {

        if (c.getName().equals(""))
            return Response.status(Status.BAD_REQUEST).entity("Error - invalid name").build();

        em.persist(c);

        return Response.ok().entity(c).build();
    }

    @GET
    @Path("/getResults")
    public List<ResultsInfo> getResults() {

        TypedQuery<Results> q = em.createQuery("from Results r", Results.class);

        List<Results> l = q.getResultList();

        List<ResultsInfo> results = new ArrayList<>();
        for (Results r : l)
            results.add(new ResultsInfo(r.getTopic(), r.getValue()));

        return results;
    }

}
