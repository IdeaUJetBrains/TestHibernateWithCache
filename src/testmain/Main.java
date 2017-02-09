package testmain;

import entity.Entitybus;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

/**
 * Created by Olga Pavlova on 1/23/2017.
 */
public class Main {
    private static final SessionFactory ourSessionFactory;

    static {
        try {
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml").setProperty("hibernate.cache.use_second_level_cache", "true");
            System.out.println("Hibernate Configuration loaded");

            ourSessionFactory = configuration.buildSessionFactory();
            System.out.println("Hibernate buildSessionFactory finished");
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static Session getSession() throws HibernateException {
        return ourSessionFactory.openSession();
    }

    public static void main(final String[] args) throws Exception {
        System.out.println("Temp Dir:"+System.getProperty("java.io.tmpdir"));

        Statistics statistics = ourSessionFactory.getStatistics();
        System.out.println("Stats enabled="+statistics.isStatisticsEnabled());
        statistics.setStatisticsEnabled(true);
        //================================================================================//
        //First level cache (or session cache)
        //================================================================================//
        //-Works in session scope
        //-It is used when the objects are loaded using their primary key
        //-It's enabled by default

       /* Session session1 = getSession();

        Entitybus entitybus1 = session1.get(Entitybus.class,1);
        //we're in the same session, the objects will be retrieved from the cache
        Entitybus entitybus2 = session1.get(Entitybus.class, 1);
        Entitybus entitybus3 = session1.get(Entitybus.class, 1);
        session1.close();

        // this is a new session, the object will be retrieved from DB
        Session session2 = getSession();
        Entitybus entitybus4 = session2.get(Entitybus.class, 1);
        session2.close();*/

        //================================================================================//
        //Second level cache
        //================================================================================//
        /*
        - The second level cache is responsible for caching objects across sessions
        - 2nd level cache is created in session factory scope and is available to be used in all sessions which are created using that particular session factory
        - Second level cache is used when the objects are loaded using their primary key
        - When searching an object by its identifier, the flow of the lookup is:
             1st level cache -> yes: return
                             -> no: 2nd level cache -> yes: store in 1st level cache; return;
                                    -> no: load from DB; store in 1st and 2nd level cache; return
         - The second level cache is not enabled by default
         */
        Session session1 = getSession();
        Session anotherSession = getSession();
        Transaction transaction = session1.beginTransaction();
        Transaction anotherTransaction = anotherSession.beginTransaction();

        printStats(statistics, 0);

        //Entity is fecthed very first time
        Entitybus entitybus1 = (Entitybus)session1.load(Entitybus.class, 1);
        printData(entitybus1, statistics, 1);

        //fetch the department entity again
        entitybus1 = (Entitybus) session1.load(Entitybus.class, 1);
        printData(entitybus1, statistics, 2);

        //Evict from first level cache
        session1.evict(entitybus1);

        entitybus1 = (Entitybus) session1.load(Entitybus.class, 1);
        printData(entitybus1, statistics, 3);

        entitybus1 = (Entitybus) session1.load(Entitybus.class, 2);
        printData(entitybus1, statistics, 4);

        entitybus1 = (Entitybus) anotherSession.load(Entitybus.class, 1);
        printData(entitybus1, statistics, 5);
        //Release resources
        transaction.commit();
        anotherTransaction.commit();

        session1.close();
        anotherSession.close();


        //================================================================================//
        //Query cache
        // It is used to cache the results of a query. Only 1 request is sent into DB by these requests
        Session session = getSession();
        Query query = session.createQuery("from Entitybus a where a.enumber = :name" );
        query.setCacheable(true); // !!!! it is necessary to add this string with the prop in the config

        Entitybus ebus = (Entitybus) query.setParameter("name", "1ENTITYBUS").uniqueResult();
        System.out.println("query cache1: " + ebus.getEnumber());

        ebus = (Entitybus) query.setParameter("name", "1ENTITYBUS").uniqueResult();
        System.out.println("query cache2: " + ebus.getEnumber());

        session.close();



    }


    private static void printStats(Statistics stats, int i) {
        System.out.println("***** " + i + " *****");
        System.out.println("Fetch Count="
                + stats.getEntityFetchCount());
        System.out.println("Second Level Hit Count="
                + stats.getSecondLevelCacheHitCount());
        System.out
                .println("Second Level Miss Count="
                        + stats
                        .getSecondLevelCacheMissCount());
        System.out.println("Second Level Put Count="
                + stats.getSecondLevelCachePutCount());
    }

    private static void printData(Entitybus emp, Statistics stats, int count) {
        System.out.println(count+":: Enumber="+emp.getEnumber() );
        printStats(stats, count);
    }
}