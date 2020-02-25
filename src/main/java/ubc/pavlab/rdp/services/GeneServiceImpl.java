package ubc.pavlab.rdp.services;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.search.Attribute;
import net.sf.ehcache.search.expression.Criteria;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.stereotype.Service;
import ubc.pavlab.rdp.model.Gene;
import ubc.pavlab.rdp.model.Taxon;
import ubc.pavlab.rdp.model.enums.GeneMatchType;
import ubc.pavlab.rdp.model.enums.PrivacyLevelType;
import ubc.pavlab.rdp.model.enums.TierType;
import ubc.pavlab.rdp.settings.ApplicationSettings;
import ubc.pavlab.rdp.util.GeneInfoParser;
import ubc.pavlab.rdp.util.SearchResult;
import ubc.pavlab.rdp.util.SearchableEhcache;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by mjacobson on 17/01/18.
 */
@Service("geneService")
public class GeneServiceImpl extends SearchableEhcache<Integer, Gene> implements GeneService {

    private static final String CACHE_NAME = "gene";
    private static Log log = LogFactory.getLog( GeneServiceImpl.class );
    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired
    private EhCacheCacheManager cacheManager;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ApplicationSettings applicationSettings;

    private Ehcache cache;

    private Attribute<String> name;
    private Attribute<String> symbol;
    private Attribute<Integer> taxonId;
    private Attribute<String> aliases;

    @Override
    public Ehcache getCache() {
        return cache;
    }

    @Override
    public Integer getKey( Gene gene ) {
        return gene.getGeneId();
    }

    @Override
    public Gene load( Integer id ) {
        return fetchByKey( id );
    }

    @Override
    public Collection<Gene> load( Collection<Integer> ids ) {
        return fetchByKey( ids );
    }

    @Override
    public Gene findBySymbolAndTaxon( String symbol, Taxon taxon ) {
        return fetchOneByCriteria( this.taxonId.eq( taxon.getId() ).and( this.symbol.eq( symbol ) ) );
    }

    @Override
    public Collection<Gene> findBySymbolInAndTaxon( Collection<String> symbols, Taxon taxon ) {
        return fetchByCriteria( this.taxonId.eq( taxon.getId() ).and( this.symbol.in( symbols ) ) );
    }

    @Override
    public Collection<SearchResult<Gene>> autocomplete( String query, Taxon taxon, int maxResults ) {
        Collection<SearchResult<Gene>> results = new LinkedHashSet<>();
        Criteria taxonCrit = this.taxonId.eq( taxon.getId() );

        if ( addAll( results, fetchByCriteria( taxonCrit.and( symbol.ilike( query ) ) ), GeneMatchType.EXACT_SYMBOL,
                maxResults ) ) {
            return results;
        }

        if ( addAll( results, fetchByCriteria( taxonCrit.and( symbol.ilike( query + "*" ) ) ),
                GeneMatchType.SIMILAR_SYMBOL, maxResults ) ) {
            return results;
        }

        if ( addAll( results, fetchByCriteria( taxonCrit.and( name.ilike( "*" + query + "*" ) ) ),
                GeneMatchType.SIMILAR_NAME, maxResults ) ) {
            return results;
        }

        addAll( results, fetchByCriteria( taxonCrit.and( aliases.ilike( "*" + query + "*" ) ) ),
                GeneMatchType.SIMILAR_ALIAS, maxResults );

        return results;
    }

    @Override
    public int size() {
        return this.cache.getSize();
    }

    @Override
    public Map<Gene, TierType> deserializeGenesTiers( Map<Integer, TierType> genesTierMap ) {
        return load( genesTierMap.keySet() ).stream().filter( Objects::nonNull )
                .collect( Collectors.toMap( g -> g, g -> genesTierMap.get( g.getGeneId() ) ) );
    }

    @Override
    public Map<Gene, Optional<PrivacyLevelType>> deserializeGenesPrivacyLevels( Map<Integer, PrivacyLevelType> genesPrivacyLevelMap ) {
        return load( genesPrivacyLevelMap.keySet() ).stream().filter( Objects::nonNull )
                .collect( Collectors.toMap( g -> g, g -> Optional.ofNullable( genesPrivacyLevelMap.get( g.getGeneId() ) ) ) );
    }

    @Override
    public void addAll( Collection<Gene> genes ) {
        putAll( genes );
    }

    @Override
    public void clear() {
        removeAll();
    }

    @PostConstruct
    private void initialize() {
        this.cache = this.cacheManager.getCacheManager().getEhcache( CACHE_NAME );
        name = new Attribute<>( "name" );
        symbol = new Attribute<>( "symbol" );
        taxonId = new Attribute<>( "taxonId" );
        aliases = new Attribute<>( "aliases" );

        ApplicationSettings.CacheSettings cacheSettings = applicationSettings.getCache();

        if ( cacheSettings.isEnabled() ) {
            log.info( "Loading genes" );
            for ( Taxon taxon : taxonService.findByActiveTrue() ) {

                try {
                    Set<Gene> data;
                    if ( cacheSettings.isLoadFromDisk() ) {
                        Path path = Paths.get( cacheSettings.getGeneFilesLocation(), taxon.getId() + ".gene_info.gz" );
                        log.info( "Loading genes for " + taxon.toString() + " from disk: " + path.toAbsolutePath() );
                        data = GeneInfoParser.parse( taxon, path.toFile() );
                    } else {
                        log.info( "Loading genes for " + taxon.toString() + " from URL: " + taxon.getGeneUrl() );
                        data = GeneInfoParser.parse( taxon, new URL( taxon.getGeneUrl() ) );
                    }
                    log.info( "Done parsing." );
                    addAll( data );
                } catch ( Exception e ) {
                    log.error( "Issue loading genes for: " + taxon, e );
                }
            }
            log.info( "Finished loading genes: " + size() );
        }

    }

    private <T> boolean addAll( Collection<SearchResult<T>> container, Collection<T> newValues, GeneMatchType match,
            int maxSize ) {

        for ( T newValue : newValues ) {
            if ( maxSize == -1 || container.size() < maxSize ) {
                container.add( new SearchResult<>( match, newValue ) );
            } else {
                return true;
            }
        }

        return false;
    }
}
