package ubc.pavlab.rdp.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.apache.commons.lang3.ArrayUtils.indexOf;

/**
 * Read in gene info provided by the NCBI.
 *
 * @author poirigui
 */
@Component
@CommonsLog
public class GeneInfoParser {

    private static final String[] EXPECTED_HEADER_FIELDS = { "#tax_id", "GeneID", "Symbol", "Synonyms", "description", "Modification_date" };

    private static DateFormat NCBI_DATE_FORMAT = new SimpleDateFormat( "yyyyMMdd" );

    @Autowired
    FTPClient ftp;

    public List<Record> parse( URL url ) throws ParseException, IOException {
        try {
            ftp.connect( url.getHost() );
            ftp.login( "anonymous", "" );
            ftp.setFileType( FTP.BINARY_FILE_TYPE );
            ftp.enterLocalPassiveMode();
            return parse( new GZIPInputStream( ftp.retrieveFileStream( url.getPath() ) ) );
        } finally {
            if ( ftp.isConnected() ) {
                ftp.disconnect();
            }
        }
    }

    public List<Record> parse( File file ) throws ParseException, IOException {
        return parse( new GZIPInputStream( new FileInputStream( file ) ) );
    }

    public List<Record> parse( InputStream input ) throws ParseException, IOException {
        try ( BufferedReader br = new BufferedReader( new InputStreamReader( input ) ) ) {
            String headerLine = br.readLine();

            if ( headerLine == null ) {
                throw new ParseException( "Stream contains no data.", 0 );
            }

            String[] header = headerLine.split( "\t" );

            for ( String expectedField : EXPECTED_HEADER_FIELDS ) {
                if ( !ArrayUtils.contains( header, expectedField ) ) {
                    throw new ParseException( MessageFormat.format( "Gene information is missing the following field: {0}.", expectedField ), 0 );
                }
            }

            return br.lines()
                    .map( line -> {
                        try {
                            return Record.parseLine( line, header );
                        } catch ( ParseException e ) {
                            log.warn( "Failed to parse line: " + line, e );
                            return null;
                        }
                    } )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toList() );
        }
    }

    @Data
    @AllArgsConstructor
    public static class Record {

        private Integer taxonId;
        private Integer GeneId;
        private String symbol;
        private String synonyms;
        private String description;
        private Date modificationDate;

        public static Record parseLine( String line, String[] header ) throws ParseException {
            String[] values = line.split( "\t" );
            if ( values.length != header.length ) {
                throw new ParseException( "Line does not have the expected number of fields.", 0 );
            }
            Integer taxonId;
            try {
                taxonId = Integer.parseInt( values[indexOf( header, "#tax_id" )] );
            } catch ( NumberFormatException e ) {
                throw new ParseException( "Could not parse taxon id.", 0 );
            }
            Integer geneId;
            try {
                geneId = Integer.parseInt( values[indexOf( header, "GeneID" )] );
            } catch ( NumberFormatException e ) {
                throw new ParseException( "Could not parse gene id.", 0 );
            }
            String symbol = values[indexOf( header, "Symbol" )];
            String synonyms = values[indexOf( header, "Synonyms" )];
            String description = values[indexOf( header, "description" )];
            Date modificationDate = NCBI_DATE_FORMAT.parse( values[indexOf( header, "Modification_date" )] );
            return new Record( taxonId, geneId, symbol, synonyms, description, modificationDate );
        }
    }
}
