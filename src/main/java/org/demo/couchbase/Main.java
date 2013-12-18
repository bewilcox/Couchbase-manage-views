package org.demo.couchbase;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        CouchbaseConnectionProvider connectionProvider = new CouchbaseConnectionProvider();
        connectionProvider.initConnection();
        connectionProvider.initDatabase();
    }
}
