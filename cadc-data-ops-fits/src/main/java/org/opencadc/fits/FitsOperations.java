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

import ca.nrc.cadc.io.ReadException;
import ca.nrc.cadc.wcs.exceptions.NoSuchKeywordException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;
import nom.tam.util.RandomAccessFileIO;
import org.apache.log4j.Logger;
import org.opencadc.fits.slice.NDimensionalSlicer;
import org.opencadc.soda.server.Cutout;

/**
 * Operations on FITS files.
 * 
 * @author pdowler
 */
public class FitsOperations {
    private static final Logger log = Logger.getLogger(FitsOperations.class);

    private final RandomAccessFileIO src;

    public FitsOperations(RandomAccessFileIO src) {
        this.src = src;
    }

    public Header getPrimaryHeader() throws ReadException {
        try {
            Fits fits = new Fits(src);
            
            BasicHDU<?> hdu = fits.readHDU();
            
            return hdu.getHeader();
        } catch (FitsException ex) {
            throw new RuntimeException("invalid fits data: " + src);
        } catch (IOException ex) {
            throw new ReadException("failed to read " + src, ex);
        }
    }
    
    public List<Header> getHeaders() throws ReadException {
        try {
            List<Header> ret = new ArrayList<>();
            
            Fits fits = new Fits(src);
            BasicHDU<?> hdu = fits.readHDU();
            while (hdu != null) {
                Header h = hdu.getHeader();
                ret.add(h);
                hdu = fits.readHDU();
            }
            
            return ret;
        } catch (FitsException ex) {
            throw new RuntimeException("invalid fits data: " + src);
        } catch (IOException ex) {
            throw new ReadException("failed to read " + src, ex);
        }
    }
    
    /**
     * Write the headers to the given OutputStream.
     *
     * @param outputStream      The stream to write to.
     * @throws IOException      For any errors writing to the stream, or reading the Headers.
     */
    public void headersToStream(final OutputStream outputStream) throws IOException {
        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
        for (final Header header : getHeaders()) {
            for (final Cursor<String, HeaderCard> headerCardCursor = header.iterator();
                 headerCardCursor.hasNext(); ) {
                final HeaderCard headerCard = headerCardCursor.next();
                bufferedWriter.write(headerCard.toString());
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
        }
    }

    /**
     * Implement prototype SODA pixel cutout action.
     *
     * @param cutout cutout spec
     * @param outputStream  The Stream to write out to.
     * @throws NoOverlapException   A valid cutout was provided, but there is no match (overlap).
     * @throws ReadException        Any errors reading reported by the storage system.
     */
    public void cutoutToStream(final Cutout cutout, final OutputStream outputStream)
            throws NoOverlapException, ReadException {
        log.debug("cutoutToStream() start.");
        try {
            final NDimensionalSlicer slicer = new NDimensionalSlicer();
            slicer.slice(src, cutout, outputStream);
        } catch (FitsException | NoSuchKeywordException ex) {
            throw new ReadException("invalid fits data: " + src + " reason: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new ReadException("failed to read " + src + " reason: " + ex.getMessage(), ex);
        }
        log.debug("cutoutToStream() OK.");
    }
}
