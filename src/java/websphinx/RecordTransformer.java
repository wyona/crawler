/*
 * WebSphinx web-crawling toolkit
 *
 * Copyright (c) 1998-2002 Carnegie Mellon University.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package websphinx;

import java.io.*;
import java.net.URL;
import rcm.util.Str;

public class RecordTransformer extends RewritableLinkTransformer {

    String prolog = "<HTML><HEAD><TITLE>Extracted Records</TITLE></HEAD><BODY><TABLE>\n";
    String epilog = "</TABLE></BODY></HTML>\n";

    String recordStart = "<TR>\n<TD><A HREF=\"%u\">%n.</A>\n";
    String recordEnd =   "\n";
    String recordDivider = "";
    
    String fieldStart = "  <TD>";
    String fieldEnd   = "\n";
    String fieldDivider = "";

    int nRecords = 0;

    public RecordTransformer (String filename) throws IOException {
        super (filename);
    }

    public synchronized void setProlog (String prolog) {
        this.prolog = prolog;
    }
    public synchronized String getProlog () {
        return prolog;
    }

    public synchronized void setEpilog (String epilog) {
        this.epilog = epilog;
    }
    public synchronized String getEpilog () {
        return epilog;
    }

    public synchronized void setRecordStart (String recordStart) {
        this.recordStart = recordStart;
    }
    public synchronized String getRecordStart () {
        return recordStart;
    }

    public synchronized void setRecordEnd (String recordEnd) {
        this.recordEnd = recordEnd;
    }
    public synchronized String getRecordEnd () {
        return recordEnd;
    }

    public synchronized void setRecordDivider (String recordDivider) {
        this.recordDivider = recordDivider;
    }
    public synchronized String getRecordDivider () {
        return recordDivider;
    }

    public synchronized void setFieldStart (String fieldStart) {
        this.fieldStart = fieldStart;
    }
    public synchronized String getFieldStart () {
        return fieldStart;
    }

    public synchronized void setFieldEnd (String fieldEnd) {
        this.fieldEnd = fieldEnd;
    }
    public synchronized String getFieldEnd () {
        return fieldEnd;
    }

    public synchronized void setFieldDivider (String fieldDivider) {
        this.fieldDivider = fieldDivider;
    }
    public synchronized String getFieldDivider () {
        return fieldDivider;
    }

    /**
     * Flush the record page to disk.  Temporarily writes the epilog.
     */
    public synchronized void flush () throws IOException {
        long p = getFilePointer ();
        if (nRecords == 0)
            emit (prolog);
        emit (epilog);
        seek (p);
        super.flush ();
    }
        

    public synchronized int getRecordCount () {
        return nRecords;
    }

    public synchronized void writeRecord (Object[] fields, boolean asText) throws IOException {
        ++nRecords;

        emit ((nRecords == 1) ? prolog : recordDivider);
        
        URL url = urlOfFirstRegion (fields);
        
        emitTemplate (recordStart, url, nRecords);
        for (int i=0; i<fields.length; ++i) {
            if (i > 0)
                emit (fieldDivider);
            emit (fieldStart);
            
            Object f = fields[i];
            if (f instanceof Region) {
                Region r = (Region)fields[i];
                if (asText)
                    write (r.toText());
                else
                    write (r);
            }
            else
                write (f.toString ());
                
            emit (fieldEnd);
        }
        emitTemplate (recordEnd, url, nRecords);
    }
    
    private URL urlOfFirstRegion (Object[] fields) {
        for (int i=0; i<fields.length; ++i)
            if (fields[i] instanceof Region) {
                Region r = (Region)fields[i];
                return r.getSource().getURL();
            }
        return null;
    }

    private void emitTemplate (String template, URL url, int record) throws IOException {
        if (template == null || template.length() == 0)
            return;
            
        template = Str.replace (template, "%n", String.valueOf (record));
        template = Str.replace (template, "%u", url != null ? url.toString () : "");
        emit (template);
    }

    /*
     * Testing
     *
    public static void main (String[] args) throws Exception {
        Pattern p = new Tagexp (args[0].replace ('_', ' ') );
        RecordTransformer records = new RecordTransformer (args[1]);
        for (int i=2; i<args.length; ++i) {
            Page page = new Page (new Link (args[i]));
            PatternMatcher m = p.match (page);
            for (Region r = m.nextMatch(); r != null; r = m.nextMatch())
                records.writeRecord (r.getFields (Pattern.groups), false);
        }
        records.close ();
    }
     */

}
