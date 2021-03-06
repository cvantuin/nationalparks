package com.openshift.evg.roadshow.parks.db;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.openshift.evg.roadshow.parks.model.Park;

/**
 * Created by jmorales on 11/08/16.
 */
@Component
public class MongoDBConnection {

    private static final String FILENAME = "/nationalparks.json";

    private static final String COLLECTION = "nationalparks";

    @Autowired
    private ResourceLoader resourceLoader;
    
    @Autowired
    private Environment env;

    private MongoTemplate mongoTemplate = null;

    public MongoDBConnection() {
    }

    @PostConstruct
    public void initConnection() {
        String mongoHost = env.getProperty("mongodb.server.host", "127.0.0l1"); // env var MONGODB_SERVER_HOST takes precedence
        String mongoPort = env.getProperty("mongodb.server.port", "27017"); // env var MONGODB_SERVER_PORT takes precedence
        String mongoUser = env.getProperty("mongodb.user", "mongodb"); // env var MONGODB_USER takes precedence
        String mongoPassword = env.getProperty("mongodb.password", "mongodb"); // env var MONGODB_PASSWORD takes precedence
        String mongoDBName = env.getProperty("mongodb.database", "mongodb"); // env var MONGODB_DATABASE takes precedence

        try {
            String mongoURI = "mongodb://" + mongoUser + ":" + mongoPassword + "@" + mongoHost + ":" + mongoPort + "/" + mongoDBName;
            System.out.println("[INFO] Connection string: " + mongoURI);
            mongoTemplate = new MongoTemplate(new Mongo(new MongoURI(mongoURI)), mongoDBName);
            mongoTemplate.getCollection(COLLECTION);
        } catch (Throwable e) {
            System.out.println("[ERROR] Creating the mongoTemplate. " + e.getMessage());
            mongoTemplate = null;
        }
    }

    /*
     * Load from embedded list of parks using FILENAME
     */
    public List<Document> loadParks() {
        System.out.println("[DEBUG] MongoDBConnection.loadParks()");

        try {
            return loadParks(resourceLoader.getResource(ResourceLoader.CLASSPATH_URL_PREFIX + FILENAME).getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error loading parks. Return empty list");
        }
        return new ArrayList<Document>();
    }

    public List<Document> loadParks(String fileLocation) {
        System.out.println("[DEBUG] MongoDBConnection.loadParks(" + fileLocation + ")");

        try {
            return loadParks(new FileInputStream(new File(fileLocation)));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error loading parks. Return empty list");
        }
        return new ArrayList<Document>();
    }

    public List<Document> loadParks(InputStream is) {
        System.out.println("[DEBUG] MongoDBConnection.loadParks(InputStream)");

        List<Document> docs = new ArrayList<Document>();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        try {
            String currentLine = null;
            int i = 1;
            while ((currentLine = in.readLine()) != null) {
                String s = currentLine.toString();
                // System.out.println("line "+ i++ + ": " + s);
                Document doc = Document.parse(s);
                docs.add(doc);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error loading parks. Return empty list");
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Error loading parks. Return empty list");
            }
        }
        return docs;
    }


    /**
     *
     */
    public void clear() {
        System.out.println("[DEBUG] MongoDBConnection.clear()");
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(COLLECTION);
        } else {
            System.out.println("[ERROR] mongoTemplate could not be initiallized. No operation with DB will be performed");
        }
    }


    /**
     * @param parks
     */
    public void init(List<Document> parks) {
        System.out.println("[DEBUG] MongoDBConnection.init(...)");
        if (mongoTemplate != null) {
            mongoTemplate.dropCollection(COLLECTION);
            mongoTemplate.insert(parks, COLLECTION);
            mongoTemplate.getCollection(COLLECTION).createIndex(new BasicDBObject().append("coordinates", "2d"));
        } else {
            System.out.println("[ERROR] mongoTemplate could not be initiallized. No operation with DB will be performed");
        }

    }

    /**
     * @return
     */
    public long sizeInDB() {
        if (mongoTemplate != null) {
            return mongoTemplate.count(new Query(), COLLECTION);
        } else {
            System.out.println("[ERROR] mongoTemplate could not be initiallized. No operation with DB will be performed");
            return 0;
        }

    }

    /**
     * @param parks
     */
    public void insert(List<Document> parks) {
        if (mongoTemplate != null) {
            mongoTemplate.insert(parks, COLLECTION);
        } else {
            System.out.println("[ERROR] mongoTemplate could not be initiallized. No operation with DB will be performed");
        }

    }

    /**
     * @return
     */
    public List<Park> getAll() {
        System.out.println("[DEBUG] MongoDBConnection.getAll()");

        if (mongoTemplate != null) {
            return mongoTemplate.findAll(Park.class, COLLECTION);
        } else {
            System.out.println("[ERROR] mongoTemplate could not be initiallized. No operation with DB will be performed");
            return new ArrayList<Park>();
        }
    }

    /**
     * @param query
     * @return
     */
    public List<Park> getByQuery(BasicDBObject query) {
        System.out.println("[DEBUG] MongoDBConnection.getByQuery()");
        List<Park> parks = new ArrayList<Park>();
        if (mongoTemplate != null) {
            ParkReadConverter converter = new ParkReadConverter();

            DBCursor cursor = mongoTemplate.getCollection(COLLECTION).find(query);
            int i = 0;
            while (cursor.hasNext()) {
                Park park = converter.convert(cursor.next());
                // System.out.println("Adding item " + i++ + ": " + park);
                parks.add(park);
            }
        } else {
            System.out.println("[ERROR] mongoTemplate could not be initiallized. No operation with DB will be performed");
        }
        return parks;
    }
}
