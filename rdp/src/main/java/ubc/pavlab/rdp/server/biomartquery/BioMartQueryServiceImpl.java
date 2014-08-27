package ubc.pavlab.rdp.server.biomartquery;

import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubc.pavlab.rdp.server.exception.BioMartServiceException;
import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.model.GeneAlias;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Simple wrapper that calls BioMart REST query service.
 * 
 * @author anton/jleong
 * @version $Id: BioMartQueryServiceImpl.java,v 1.13 2013/07/15 16:01:54 anton Exp $
 */
@Service
public class BioMartQueryServiceImpl implements BioMartQueryService {
    private static final String BIO_MART_URL_SUFFIX = "/biomart/martservice/results";
    private static final String BIO_MART_URL = "http://www.biomart.org" + BIO_MART_URL_SUFFIX;
    private static final String GENE_NAMES_BIO_MART_URL = "http://www.genenames.org" + BIO_MART_URL_SUFFIX;

    private static Log log = LogFactory.getLog( BioMartQueryServiceImpl.class.getName() );

    private static String sendRequest( String xmlQueryString, String url ) throws BioMartServiceException {
        Client client = Client.create();

        MultivaluedMap<String, String> queryData = new MultivaluedMapImpl();
        queryData.add( "query", xmlQueryString );

        WebResource resource = client.resource( url ).queryParams( queryData );

        ClientResponse response = resource.type( MediaType.APPLICATION_FORM_URLENCODED_TYPE )
                .get( ClientResponse.class );

        // Check return code
        if ( Response.Status.fromStatusCode( response.getStatus() ).getFamily() != Response.Status.Family.SUCCESSFUL ) {
            String errorMessage = "Error occurred when accessing BioMart web service: "
                    + response.getEntity( String.class );
            log.error( errorMessage );

            throw new BioMartServiceException( errorMessage );
        }

        return response.getEntity( String.class );
    }

    @Autowired
    private BioMartCache bioMartCache;

    @Override
    public Collection<Gene> fetchGenesByGeneSymbols( Collection<String> geneSymbols ) throws BioMartServiceException {
        updateCacheIfExpired();

        return bioMartCache.fetchGenesByGeneSymbols( geneSymbols );
    }

    @Override
    public Collection<Gene> fetchGenesByLocation( String chromosomeName, Long start, Long end )
            throws BioMartServiceException {
        updateCacheIfExpired();

        return bioMartCache.fetchGenesByLocation( chromosomeName, start, end );
    }

    /*
     * @Override public Collection<GenomicRange> fetchGenomicRangesByGeneSymbols( Collection<String> geneSymbols )
     * throws BioMartServiceException { Collection<Gene> genes = fetchGenesByGeneSymbols( geneSymbols );
     * Collection<GenomicRange> genomicRanges = new HashSet<GenomicRange>( genes.size() );
     * 
     * for ( Gene gene : genes ) { genomicRanges.add( gene.getGenomicRange() ); }
     * 
     * return genomicRanges; }
     */

    @Override
    public Collection<Gene> findGenes( String queryString ) throws BioMartServiceException {
        updateCacheIfExpired();

        return bioMartCache.findGenes( queryString );
    }

    /**
     * get the genes using the list of gene ids or list of gene symbols
     * 
     * @param List of gene strings
     * @return Gene value Objects associated with the given gene string list
     */
    @Override
    public List<Gene> getGenes( List<String> geneStrings ) throws BioMartServiceException {
        updateCacheIfExpired();

        return bioMartCache.getGenes( geneStrings );
    }

    @SuppressWarnings("unused")
    @PostConstruct
    private void initialize() throws BioMartServiceException {
        // updateCacheIfExpired();
    }

    private String queryDataset( Dataset dataset, String url ) throws BioMartServiceException {
        Query query = new Query();
        query.Dataset = dataset;

        StringWriter xmlQueryWriter = null;

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance( Query.class, Dataset.class, Filter.class,
                    Attribute.class );
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            xmlQueryWriter = new StringWriter();
            jaxbMarshaller.marshal( query, xmlQueryWriter );
        } catch ( JAXBException e ) {
            String errorMessage = "Cannot initialize genes from BioMart";
            log.error( errorMessage, e );

            throw new BioMartServiceException( errorMessage );
        }

        final StopWatch timer = new StopWatch();
        timer.start();

        Timer uploadCheckerTimer = new Timer( true );
        uploadCheckerTimer.scheduleAtFixedRate( new TimerTask() {
            @Override
            public void run() {
                log.info( "Waiting for BioMart response ... " + timer.getTime() + " ms" );
            }
        }, 0, 10 * 1000 );

        String response = sendRequest( xmlQueryWriter.toString(), url );
        log.info( "BioMart request to (" + url + ") took " + timer.getTime() + " ms" );

        uploadCheckerTimer.cancel();
        return response;
    }

    private Collection<Gene> parseGeneInfo( Dataset dataset ) throws BioMartServiceException {

        dataset.Attribute.add( new Attribute( "ensembl_gene_id" ) );
        dataset.Attribute.add( new Attribute( "entrezgene" ) );
        dataset.Attribute.add( new Attribute( "external_gene_id" ) );
        dataset.Attribute.add( new Attribute( "description" ) );
        dataset.Attribute.add( new Attribute( "gene_biotype" ) );
        dataset.Attribute.add( new Attribute( "chromosome_name" ) );
        dataset.Attribute.add( new Attribute( "start" ) );
        dataset.Attribute.add( new Attribute( "end" ) );

        String response = queryDataset( dataset, BIO_MART_URL );

        String[] rows = StringUtils.split( response, "\n" );

        Collection<Gene> genes = new HashSet<>();

        int rowsLength = rows.length;
        if ( rowsLength <= 1 ) {
            String errorMessage = "Error: retrieved only " + rowsLength + " row of gene data from BioMart"
                    + ( rowsLength == 1 ? "(Error message from BioMart: " + rows[0] + ")" : "" );
            log.error( errorMessage );

            throw new BioMartServiceException( errorMessage );
        }

        for ( String row : rows ) {

            // warning: split() trims off trailing whitespaces!
            String[] fields = row.split( "\t" );

            int index = 0;
            String ensemblId = fields[index++];
            String ncbiGeneId = fields[index++];
            String symbol = fields[index++];
            String name = fields[index++];
            String geneBiotype = fields[index++];
            String chromosome = fields[index++];
            String start = fields[index++];
            String end = fields[index++];

            // Ignore results that do not have required attributes.
            if ( ensemblId.equals( "" ) || chromosome.equals( "" ) || start.equals( "" ) || end.equals( "" ) ) {
                continue;
            }

            int sourceIndex = name.indexOf( " [Source:" );
            name = sourceIndex >= 0 ? name.substring( 0, sourceIndex ) : name;

            // GeneValueObject gene = new GeneValueObject( ensemblId, symbol, name, geneBiotype, "human" );
            Gene gene = new Gene();
            gene.setEnsemblId( ensemblId );
            gene.setOfficialSymbol( symbol );
            gene.setOfficialName( name );
            // TODO
            // gene.setTaxon(new Taxon());
            /*
             * int startBase = Integer.valueOf( start ); int endBase = Integer.valueOf( end ); if ( startBase < endBase
             * ) { gene.setGenomicRange( new GenomicRange( chromosome, startBase, endBase ) ); } else {
             * gene.setGenomicRange( new GenomicRange( chromosome, endBase, startBase ) ); }
             */
            gene.setNcbiGeneId( ncbiGeneId );
            genes.add( gene );
        }

        return genes;
    }

    private HashMap<String, Gene> convertToMap( Collection<Gene> genes ) {
        HashMap<String, Gene> map = new HashMap<>();
        for ( Gene g : genes ) {
            map.put( g.getEnsemblId(), g );
        }
        return map;
    }

    private void addHumanGeneSynonyms( Collection<Gene> genes ) throws BioMartServiceException {
        Dataset dataset = new Dataset( "hgnc" );

        dataset.Filter.add( new Filter( "gd_pub_chr", "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,X,Y" ) );

        dataset.Attribute.add( new Attribute( "gd_aliases" ) );
        dataset.Attribute.add( new Attribute( "md_ensembl_id" ) );

        String response = queryDataset( dataset, GENE_NAMES_BIO_MART_URL );

        String[] rows = StringUtils.split( response, "\n" );

        HashMap<String, Gene> genesMap = convertToMap( genes );

        for ( String row : rows ) {

            // warning: split() trims off trailing whitespaces!
            String[] fields = row.split( "\t" );

            try {
                int index = 0;
                String aliases = fields[index++];
                String ensemblId = fields[index++];
                // Ignore results that do not have required attributes.
                if ( ensemblId.equals( "" ) || aliases.equals( "" ) || !genesMap.containsKey( ensemblId ) ) {
                    continue;
                }

                for ( String alias : aliases.split( "," ) ) {
                    genesMap.get( ensemblId ).getAliases().add( new GeneAlias( alias.trim() ) );
                }

            } catch ( ArrayIndexOutOfBoundsException e ) {
                continue;
            }
        }

    }

    private Collection<Gene> parseHumanGeneInfo() throws BioMartServiceException {
        Dataset dataset = new Dataset( "hsapiens_gene_ensembl" );

        dataset.Filter.add( new Filter( "chromosome_name",
                "1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,X,Y" ) );

        Collection<Gene> genes = new HashSet<>();
        genes.addAll( parseGeneInfo( dataset ) );

        addHumanGeneSynonyms( genes );

        return genes;
    }

    private void updateCacheIfExpired() throws BioMartServiceException {
        if ( !this.bioMartCache.hasExpired() ) return;

        Collection<Gene> genes = new HashSet<>();
        genes.addAll( parseHumanGeneInfo() );

        this.bioMartCache.putAll( genes );

    }
}
