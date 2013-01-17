/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2009.                            (c) 2009.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
*  $Revision: 4 $
*
************************************************************************
*/
package ca.nrc.cadc.stc.util;

import ca.nrc.cadc.stc.SpectralInterval;
import ca.nrc.cadc.stc.SpectralUnit;
import ca.nrc.cadc.stc.StcsParsingException;

/**
 * Class to parse a STC-S phrase to a SpectralInterval object, and format
 * a SpectralInterval object to a STC-S phrase.
 */
public class SpectralIntervalFormat implements Format<SpectralInterval>
{
    /**
     * Parses a String to a SpectralInterval.
     *
     * @param phrase the String to parse.
     * @return SpectralInterval value of the String.
     */
    public SpectralInterval parse(String phrase)
        throws StcsParsingException
    {
        if (phrase == null || phrase.length() == 0)
            return null;
        phrase = phrase.trim();

        String[] tokens = phrase.split("\\s+");
        if (tokens.length != 5)
            throw new StcsParsingException("Expected 5 words in " + phrase);

        if (!tokens[0].equalsIgnoreCase(SpectralInterval.NAME))
            throw new StcsParsingException("Expected SpectralInterval, was " + tokens[0]);

        double lolimit;
        double hilimit;
        try
        {
            lolimit = Double.parseDouble(tokens[1]);
        }
        catch (NumberFormatException e)
        {
            throw new StcsParsingException("Unable to parse loLimit " + tokens[1] +
                                           " to number because " + e.getMessage());
        }
        try
        {
            hilimit = Double.parseDouble(tokens[2]);
        }
        catch (NumberFormatException e)
        {
            throw new StcsParsingException("Unable to parse hiLimit " + tokens[2] +
                                           " to number because " + e.getMessage());
        }

        if (!tokens[3].equalsIgnoreCase("unit"))
            throw new StcsParsingException("Invalid word in phrase, expected unit, " +
                                           " found " + tokens[4]);

        if (!SpectralUnit.contains(tokens[4]))
            throw new StcsParsingException("Not a valid SpectralUnit " + tokens[4]);

        SpectralUnit unit = SpectralUnit.valueOf(tokens[4]);
        
        return new SpectralInterval(lolimit, hilimit, unit);
    }

    /**
     * Takes a SpectralInterval and returns a String representation.
     * If the SpectralInterval is null an empty String is returned.
     *
     * @param spectralInterval SpectralInterval to format
     * @return String representation of the SpectralInterval.
     */
    public String format(SpectralInterval spectralInterval)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(SpectralInterval.NAME);
        sb.append(" ");
        sb.append(spectralInterval.getLoLimit());
        sb.append(" ");
        sb.append(spectralInterval.getHiLimit());
        sb.append(" unit ");
        sb.append(spectralInterval.getUnit().name());
        return sb.toString();
    }

}