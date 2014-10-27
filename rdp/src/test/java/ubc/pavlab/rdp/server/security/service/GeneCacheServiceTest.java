/*
 * The rdp project
 * 
 * Copyright (c) 2014 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubc.pavlab.rdp.server.security.service;

import static org.junit.Assert.assertEquals;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubc.pavlab.rdp.server.model.Gene;
import ubc.pavlab.rdp.server.service.GeneCacheService;
import ubc.pavlab.rdp.server.service.GeneService;
import ubc.pavlab.rdp.testing.BaseSpringContextTest;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneCacheServiceTest extends BaseSpringContextTest {

    @Autowired
    GeneCacheService geneCacheService;

    @Autowired
    GeneService geneService;

    private Long[] taxonIds = new Long[] { 9606L, 562L, 10090L, 559292L, 999999999L };
    private long numberOfGenesPerTaxon = 20;
    private Long totalGenes;

    @Before
    public void setUp() {
        totalGenes = initGeneTable();
    }

    @After
    public void tearDown() {
        for ( Long count = 0L; count < totalGenes; count++ ) {
            geneService.delete( geneService.findById( count ) );
        }
        totalGenes = null;
    }

    private Long initGeneTable() {
        Long id = 0L;
        for ( Long taxonId : taxonIds ) {
            for ( int count = 0; count < numberOfGenesPerTaxon; count++ ) {
                String officialSymbol = RandomStringUtils.randomAlphabetic( 10 );
                String officialName = RandomStringUtils.randomAlphabetic( 10 );
                String aliases = RandomStringUtils.randomAlphabetic( 5 ) + "|" + RandomStringUtils.randomAlphabetic( 5 );
                Gene gene = new Gene( id++, taxonId, officialSymbol, officialName, aliases );
                geneService.create( gene );
            }
        }
        return id;
    }

    @Test
    public void testUpdateCache() {
        long numberOfElements = geneCacheService.updateCache();
        assertEquals( totalGenes - numberOfGenesPerTaxon, numberOfElements );
    }
}
