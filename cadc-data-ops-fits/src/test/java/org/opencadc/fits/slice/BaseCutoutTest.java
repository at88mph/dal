/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2021.                            (c) 2021.
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
 *
 ************************************************************************
 */

package org.opencadc.fits.slice;

import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;

import java.util.Arrays;

public abstract class BaseCutoutTest {
    private static final Logger LOGGER = Logger.getLogger(BaseCutoutTest.class);

    // Concrete tests can set this if desired.
    long padding = 2;

    static {
        Log4jInit.setLevel("org.opecadc.fits.slice", Level.DEBUG);
    }

    /**
     * Ensure the pixel values match with a +-padding grace.
     * @param message   The fail message.
     * @param expected  The expected values.
     * @param result    The resulting values.
     */
    void assertFuzzyPixelArrayEquals(final String message, final long[] expected, final long[] result) {
        if (result == null && expected != null) {
            Assert.fail("Result array is null but expected " + Arrays.toString(expected)
                        + "\nMessage from test: " + message);
        } else if (result != null && expected == null) {
            Assert.fail("Expected null but got " + Arrays.toString(result) + "\nMessage from test: " + message);
        } else if (result != null) {
            LOGGER.debug("\n*****\nChecking array \n" + Arrays.toString(result) + "\nagainst\n"
                         + Arrays.toString(expected) + "\n allowing a difference of " + padding + ".\n****");
            Assert.assertEquals(message, result.length, expected.length);
            for (int i = 0; i < expected.length; i++) {
                final long expectedPixelValue = expected[i];
                final long resultPixelValue = result[i];

                // Assert
                final boolean condition = (resultPixelValue <= expectedPixelValue + padding)
                                          && (resultPixelValue >= expectedPixelValue - padding);
                LOGGER.debug("Checking " + resultPixelValue + " against " + expectedPixelValue + " diff = "
                             + Math.abs(expectedPixelValue - resultPixelValue));
                Assert.assertTrue(message + "\nExpected " + expectedPixelValue + "(+/-" + padding
                                  + "), but received " + resultPixelValue + "\n", condition);
            }
        }
    }
}
