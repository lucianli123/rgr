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

package ubc.pavlab.rdp.server.model;

import ubic.basecode.ontology.model.OntologyTerm;

/**
 * TODO Document Me
 * 
 * @author mjacobson
 * @version $Id$
 */
public class GeneOntologyTerm {
    private String geneOntologyId;
    private String geneOntologyTerm;
    private Long frequency;
    private Long size;

    public GeneOntologyTerm() {

    }

    public GeneOntologyTerm( String geneOntologyId, String geneOntologyTerm ) {
        this.geneOntologyId = geneOntologyId;
        this.geneOntologyTerm = geneOntologyTerm;
    }

    public GeneOntologyTerm( OntologyTerm term ) {
        this.geneOntologyId = term.getUri().replaceAll( ".*?/", "" ).replace( "_", ":" );
        this.geneOntologyTerm = term.getTerm();
    }

    /**
     * @return the geneOntologyId
     */
    public String getGeneOntologyId() {
        return geneOntologyId;
    }

    /**
     * @param geneOntologyId the geneOntologyId to set
     */
    public void setGeneOntologyId( String geneOntologyId ) {
        this.geneOntologyId = geneOntologyId;
    }

    /**
     * @return the geneOntologyTerm
     */
    public String getGeneOntologyTerm() {
        return geneOntologyTerm;
    }

    /**
     * @param geneOntologyTerm the geneOntologyTerm to set
     */
    public void setGeneOntologyTerm( String geneOntologyTerm ) {
        this.geneOntologyTerm = geneOntologyTerm;
    }

    /**
     * @return the frequency
     */
    public Long getFrequency() {
        return frequency;
    }

    /**
     * @param frequency the frequency to set
     */
    public void setFrequency( Long frequency ) {
        this.frequency = frequency;
    }

    /**
     * @return the size
     */
    public Long getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize( Long size ) {
        this.size = size;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( geneOntologyId == null ) ? 0 : geneOntologyId.hashCode() );
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( !( obj instanceof GeneOntologyTerm ) ) return false;
        GeneOntologyTerm other = ( GeneOntologyTerm ) obj;
        if ( geneOntologyId == null ) {
            if ( other.geneOntologyId != null ) return false;
        } else if ( !geneOntologyId.equals( other.geneOntologyId ) ) return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "GeneOntologyTerm [geneOntologyId=" + geneOntologyId + "]";
    }

}
