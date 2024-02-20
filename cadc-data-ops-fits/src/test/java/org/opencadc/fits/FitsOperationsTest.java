/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2020.                            (c) 2020.
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
************************************************************************
*/

package org.opencadc.fits;

import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;
import nom.tam.util.RandomAccessFileIO;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.soda.ExtensionSlice;
import org.opencadc.soda.server.Cutout;

/**
 *
 * @author pdowler
 */
public class FitsOperationsTest {
    private static final Logger log = Logger.getLogger(FitsOperationsTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.fits", Level.DEBUG);
    }
     
    public FitsOperationsTest() { 
    }
    
    @Test
    public void testGetPrimaryHeader() {
        try {
            // setup
            final RandomAccessFileIO randomAccessDataObject =
                    new RandomAccessStorageObject(FileUtil.getFileFromResource("sample-mef.fits",
                                                                         FitsOperationsTest.class), "r");

            FitsOperations fop = new FitsOperations(randomAccessDataObject);
            Header h = fop.getPrimaryHeader();
            //h.dumpHeader(System.out);
            Cursor<String,HeaderCard> iter = h.iterator();
            for (int i = 0; i <= 5; i++) {
                HeaderCard hc = iter.next();
                log.info(hc.getKey() + " = " + hc.getValue());
            }
            log.info("...");
            
            long nbytes = h.getDataSize();
            log.info("data size: " + nbytes);
            
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testGetHeaders() {
        try {
            // setup
            final RandomAccessFileIO randomAccessDataObject =
                    new RandomAccessStorageObject(FileUtil.getFileFromResource("sample-mef.fits",
                                                                         FitsOperationsTest.class), "r");
            
            FitsOperations fop = new FitsOperations(randomAccessDataObject);
            List<Header> hdrs = fop.getHeaders();
            
            for (int i = 0; i < hdrs.size(); i++) {
                Header h = hdrs.get(i);
                log.info("** header: " + i);
                Cursor<String,HeaderCard> iter = h.iterator();
                for (int c = 0; c <= 5; c++) {
                    HeaderCard hc = iter.next();
                    log.info(hc.getKey() + " = " + hc.getValue());
                }
                long nbytes = h.getDataSize();
                log.info("** data size: " + nbytes);
                log.info("...");
            }
            
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testHeaders() throws Exception {
        try (final RandomAccessFileIO randomAccessDataObject =
                     new RandomAccessStorageObject(FileUtil.getFileFromResource("sample-mef.fits",
                                                                                FitsOperationsTest.class), "r");
             final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            // setup
            final FitsOperations fop = new FitsOperations(randomAccessDataObject);
            fop.headersToStream(byteArrayOutputStream);

            final byte[] output = byteArrayOutputStream.toByteArray();

            final File sampleHeaderFile = FileUtil.getFileFromResource("sample-mef.txt",
                                                                       FitsOperationsTest.class);
            final List<String> expectedHeaderLines = new ArrayList<>();
            try (final FileInputStream fileInputStream = new FileInputStream(sampleHeaderFile);
                 final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    expectedHeaderLines.add(line);
                }
            }

            final List<String> resultHeaderLines = new ArrayList<>();
            try (final BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(output)))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    resultHeaderLines.add(line);
                }
            }

            for (int i = 0; i < expectedHeaderLines.size(); i++) {
                final String nextExpectedHeaderLine = expectedHeaderLines.get(i);
                final String nextResultHeaderLine = resultHeaderLines.get(i);

                final HeaderCard expectedHeaderCard = HeaderCard.create(nextExpectedHeaderLine);
                final HeaderCard resultHeaderCard = HeaderCard.create(nextResultHeaderLine);

                Assert.assertEquals("Wrong header key at '" + nextExpectedHeaderLine + "' == '"
                                    + nextResultHeaderLine + "': " + i, expectedHeaderCard.getKey(),
                                    resultHeaderCard.getKey());

                Assert.assertEquals("Wrong header value at '" + nextExpectedHeaderLine + "' == '"
                                    + nextResultHeaderLine + "': " + i, expectedHeaderCard.getValue(),
                                    resultHeaderCard.getValue());
            }
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            throw unexpected;
        }
    }

    @Test
    public void testSlice() throws Exception {
        // setup
        final RandomAccessFileIO randomAccessDataObject =
                new RandomAccessStorageObject(FileUtil.getFileFromResource("test-hst-mef.fits",
                                                                     FitsOperationsTest.class), "r");
        final FitsOperations fop = new FitsOperations(randomAccessDataObject);
        final File outputFile = Files.createTempFile(
                new File(System.getProperty("user.home")).toPath(),
                "test-hst-mef-cutout-", ".fits").toFile();
        outputFile.deleteOnExit();

        // Extension 3 contains non-image Data, but should still be included.
        try (final FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            List<ExtensionSlice> cut = new ArrayList<>();
            cut.add(new ExtensionSlice(3));
            cut.add(new ExtensionSlice(106));
            cut.add(new ExtensionSlice(126));
            final Cutout cutout = new Cutout();
            cutout.pixelCutouts = cut;
            fop.cutoutToStream(cutout, fileOutputStream);
        }

        final Fits resultsFits = new Fits(new RandomAccessStorageObject(outputFile, "r"));
        resultsFits.read();

        Assert.assertEquals("Wrong HDU count.  Extensions 3, 106, and 126 should be available.", 4,
                            resultsFits.getNumberOfHDUs());

        BasicHDU<?> hdu;
        while ((hdu = resultsFits.readHDU()) != null) {
            final Header h = hdu.getHeader();
            final Cursor<String,HeaderCard> iter = h.iterator();
            for (int c = 0; c <= 5; c++) {
                HeaderCard hc = iter.next();
                log.info(hc.getKey() + " = " + hc.getValue());
            }
            long nbytes = h.getDataSize();
            log.info("** data size: " + nbytes);
            log.info("...");
        }
    }

    @Test
    public void testNoOverlap() throws Exception {
        final RandomAccessFileIO randomAccessDataObject =
                new RandomAccessStorageObject(FileUtil.getFileFromResource("test-hst-mef.fits",
                                                                     FitsOperationsTest.class), "r");
        final FitsOperations fop = new FitsOperations(randomAccessDataObject);
        final File outputFile = Files.createTempFile(
                new File(System.getProperty("user.home")).toPath(),
                "test-hst-mef-cutout-", ".fits").toFile();
        outputFile.deleteOnExit();

        // Extension 3 contains non-image Data, but should still be included.
        try (final FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            List<ExtensionSlice> cut = new ArrayList<>();
            // No such extension
            cut.add(new ExtensionSlice(188));
            final Cutout cutout = new Cutout();
            cutout.pixelCutouts = cut;
            fop.cutoutToStream(cutout, fileOutputStream);
            Assert.fail("Should throw NoOverlapException");
        } catch (NoOverlapException noOverlapException) {
            // Good!
        }
    }
}
