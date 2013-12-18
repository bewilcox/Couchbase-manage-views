
package org.demo.couchbase;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.DesignDocument;
import com.couchbase.client.protocol.views.ViewDesign;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.demo.couchbase.utils.DBConstants;
import org.demo.couchbase.utils.JsonUtils;
import org.demo.couchbase.utils.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.*;


public class CouchbaseConnectionProvider {
	
	private static final Logger LOG = LoggerFactory.getLogger(CouchbaseConnectionProvider.class);
	private CouchbaseClient client;
	    
    private List<URI> uris = new ArrayList<>();
    private String bucket = null;
    private String password = null;
    private Version databaseVersion;
	
	/**
	 * init connection to the cluster couchbase
	 * @throws Exception
	 */
	public void initConnection() {
		if (this.client == null) {
			// params
			this.loadConfiguration();
			try {
				client = new CouchbaseClient(uris, bucket, password);
			} catch (IOException e) {
				throw new RuntimeException("Enabled to connect to the Couchbase cluster", e);
			}
		}
	}
	
	/**
	 * Close the connection.
	 */
	public void closeConnection() {
		if (client != null) {
			client.shutdown();
			client = null;
		}
	}
	
	/**
	 * Load configuration for the couchbase connection.
	 * @throws Exception
	 */
	private void loadConfiguration() {
        InputStream in = getClass().getClassLoader().getResourceAsStream(DBConstants.CONFIGURATION_FILE_NAME);
        Properties p = new Properties();
        try {
			p.load(in);
			
	        // load the list of URI
	        String urisAsString = (String)p.get(DBConstants.COUCHBASE_URI);
	        List<String> x = Arrays.asList(  urisAsString.split(",") );
	        for ( String u : x ) {
	            URI uri = new URI(u);
	            uris.add( uri );
	        }

	        bucket = (String)p.get(DBConstants.COUCHBASE_BUCKET);
	        password = (String)p.get(DBConstants.COUCHBASE_PASSWORD);
	        
	        this.databaseVersion = Version.parse((String)p.get(DBConstants.DATABASE_VERSION));
	        
		} catch (Exception e) {
			throw new RuntimeException("Enable to load configuration of the couchbase server : " +e.getStackTrace());
		} 
	}

	/**
	 * Get the couchbase client.
	 * @return
	 */
	private CouchbaseClient getClient() {
		if (client == null) {
			this.initConnection();
		}
		return client;
	}

	/**
	 * Create or update all generated views.
	 * @throws IOException 
	 */
	public void initDatabase() throws IOException {
		boolean updateNeeded = false;
		LOG.info("Couchbase database initialisation started");
		LOG.info("Checking version");
		String strVersionInBase = (String)this.getClient().get(DBConstants.DATABASE_VERSION_DOC_ID);
		Version versionInBase = null;
		if(strVersionInBase != null) {
			versionInBase = JsonUtils.getMapper().readValue(strVersionInBase,Version.class);
		}

		if (versionInBase == null) {
			LOG.info("No version document found. Create one for the version {}",this.databaseVersion.toString());
			this.getClient().set(DBConstants.DATABASE_VERSION_DOC_ID, 
									JsonUtils.getMapper().writeValueAsString(databaseVersion));
			updateNeeded = true;
		} else {
			updateNeeded = this.databaseVersion.isLatest(versionInBase);
		}
		
		if (updateNeeded) {
			LOG.info("Update views is needed.");
			// Get all view files
			LOG.info("Create or Update Couchbase views");
			URL viewURL = getClass().getClassLoader().getResource("couchbase/views/");
			File viewDir = FileUtils.toFile(viewURL);
			if (viewDir.isDirectory()) {
				String[] extensions = new String[] { "js" };
				Collection<File> viewFiles = FileUtils.listFiles(viewDir,
						extensions, false);
				LOG.info("----  Create or Update Couchbase views");
				LOG.info("----  {} view files found.", viewFiles.size());
				// Get all views by file
				for (File file : viewFiles) {
					String[] viewDeclarations = StringUtils.substringsBetween(
							FileUtils.readFileToString(file), "startview",
							"//endview");
					LOG.info("----  {} view declarations found on {} file.",
							viewDeclarations.length, file.getName());
					for (int i = 0; i < viewDeclarations.length; i++) {
						String[] lines = StringUtils.split(viewDeclarations[i],
								"\n");
						String viewName = lines[0].trim();
						String map = StringUtils.join(lines, "", 1,
								(lines.length));
						LOG.info("----  create or update view : {} ", viewName);
						this.createOrUpdateView(viewName, map);
					}
				}
			}
			
			// Update version in db
			this.getClient().set(DBConstants.DATABASE_VERSION_DOC_ID,
									JsonUtils.getMapper().writeValueAsString(databaseVersion));
		} else {
			LOG.info("No update view needed");
		}
		LOG.info("Couchbase database initialisation finished");
	}
	
	private void createOrUpdateView(String viewName, String map) {
		this.createOrUpdateView(viewName, map, null);
	}
	
	private void createOrUpdateView(String viewName, String map, String reduce) {
		DesignDocument designDocument;
		String designDocumentName;
		
		// Create two design document, one for the all_doc query type and one for the other.
		if(viewName.startsWith("all")) {
			designDocumentName = DBConstants.DESIGN_NAME_ALL;
		}else {
			designDocumentName = DBConstants.DESIGN_NAME_DOMAIN;
		}
		
		try {
			designDocument = this.getClient().getDesignDocument(designDocumentName);
		} catch (Exception e) {
			designDocument = new DesignDocument(designDocumentName);
		}

		ViewDesign viewDesign;
		if (reduce != null) {
			viewDesign = new ViewDesign(viewName,map,reduce);
		} else {
			viewDesign = new ViewDesign(viewName,map);
		}

		designDocument.getViews().add(viewDesign);
		this.getClient().createDesignDoc(designDocument);
	}

}