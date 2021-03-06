package uk.co.eelpieconsulting.osm.nominatim.solr;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CommonsHttpSolrServer;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Splitter;

import uk.co.eelpieconsulting.osm.nominatim.model.Place;
import uk.co.eelpieconsulting.osm.nominatim.parsing.PlacesDumpParser;

@Component
public class IndexUpdater {
	
	private static final int COMMIT_SIZE = 10000;
	final Splitter onCommaSeperator = Splitter.on(",");

	@Value("#{autoComplete['solr.url']}")
	private String solrUrl;
	
	public void buildIndex(String dumpFileName) throws SolrServerException, IOException {
		final SolrServer solrServer = new CommonsHttpSolrServer(solrUrl);
		
		final URL resource = this.getClass().getClassLoader().getResource(dumpFileName);
		final PlacesDumpParser parser = new PlacesDumpParser(FileUtils.toFile(resource));
		
		int count = 0;
		int total = 0;
		
		UpdateRequest updateRequest = new UpdateRequest();
		while (parser.hasNext()) {
			Place place = parser.next();
			final SolrInputDocument inputDocument = new SolrInputDocument();
			inputDocument.addField("id", place.getOsmId() + place.getType());
			inputDocument.addField("osm_id", place.getOsmId());
			inputDocument.addField("osm_type", place.getOsmType());
			
			final String name = onCommaSeperator.split(place.getAddress()).iterator().next();			
			inputDocument.addField("name", name);
			inputDocument.addField("name_string", place.getAddress().replaceFirst("^The ", ""));
			inputDocument.addField("address_line", place.getAddress());
			inputDocument.addField("class", place.getClassification());
			inputDocument.addField("type", place.getType());

			updateRequest.add(inputDocument);
			count++;
			if (count > COMMIT_SIZE) {
				updateRequest.process(solrServer);
				solrServer.commit();
				updateRequest = new UpdateRequest();
				total = total + count;
				System.out.println(total);
				count = 0;
			}
		}

		updateRequest.process(solrServer);
		solrServer.commit();
	}
	
}
