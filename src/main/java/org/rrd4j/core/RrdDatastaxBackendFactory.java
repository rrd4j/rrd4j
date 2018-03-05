package org.rrd4j.core;



import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * {@link RrdBackendFactory} that uses
 * <a href="http://docs.datastax.com/en/developer/java-driver/3.4/manual/object_mapper/using/">Datastax mapping java driver</a>
 * to read data. Construct a Mapper {@link Mapper} object and pass it via the constructor.
 *
 * @author <a href="mailto:kasperf@asergo.com">Kasper Fock</a>
 */
public class RrdDatastaxBackendFactory extends RrdBackendFactory {
    private static final String UTF_8 = "UTF-8";
    Session session;
    MappingManager manager;
    Mapper<RrdDatastax> mapper;

    private String createKeyspace = "CREATE KEYSPACE IF NOT EXISTS rrd4j WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':1 }";
    private String createTable = "CREATE TABLE IF NOT EXISTS rrd4j.rrd (path text primary key, rrd blob)";


    private final Set<String> pathCache = new CopyOnWriteArraySet<String>();

    /**
     * <p>Constructor for RrdDatastaxBackendFactory.</p>
     *
     * @param session a {@link Session} object.
     */
    public RrdDatastaxBackendFactory(Session session) {
        this.session = session;
        ResultSet rs = session.execute(createKeyspace);
        if(rs.wasApplied()){
            System.out.println("Keyspace created for rrd");
        }else{
            System.out.println("Keyspace failed to create");
        }
        ResultSet tableCreated = session.execute(createTable);
        if(!tableCreated.wasApplied()){
            System.out.println("RRD table not created in cassandra");
        }
        manager = new MappingManager(session);
        mapper = manager.mapper(RrdDatastax.class,"rrd4j");
        /*try {
            RrdBackendFactory fact = RrdBackendFactory.getFactory(getName());
            RrdBackendFactory.setDefaultFactory(getName());
        } catch (Exception e) {

        }*/
        RrdBackendFactory.registerAndSetAsDefaultFactory(this);
    }

    /**
     * {@inheritDoc}
     *
     * Creates new RrdDatastaxBackend object for the given id (path).
     */
    protected RrdBackend open(String path, boolean readOnly) throws IOException {
            return new RrdDatastaxBackend(path, mapper);

    }

    /**
     * <p>all.</p>
     *
     */
    public List<RrdDatastax> all() {
        ResultSet results = session.execute("SELECT * FROM rrd4j.rrd");
        return mapper.map(results).all();
    }

    /**
     * <p>delete.</p>
     *
     * @param path a {@link String} object.
     */
    public void delete(String path) {
        mapper.delete(path);
    }

    /**
     * {@inheritDoc}
     *
     * Checks if the RRD with the given id (path) already exists in the database.
     */
    protected boolean exists(String path) throws IOException {
        return mapper.get(path) != null;
    }

    /** {@inheritDoc} */
    protected boolean shouldValidateHeader(String path) {
        return false;
    }

    /**
     * <p>getName.</p>
     *
     * @return The {@link String} "DATASTAX".
     */
    public String getName() {
        return "DATASTAX";
    }
}
