/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright 2008 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */
package org.opensolaris.opengrok.history;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author austvik
 */
public class TeamFoundationServerHistoryParserTest {

    private TeamFoundationServerHistoryParser instance;
    
    public TeamFoundationServerHistoryParserTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        instance = new TeamFoundationServerHistoryParser();
    }

    @After
    public void tearDown() {
        instance = null;
    }

    /**
     * Test of parse method, of class SubversionHistoryParser.
     */
    @Test
    public void parseEmpty() throws Exception {
        History result = instance.parse("");
        assertNotNull(result);
        assertNotNull(result.getHistoryEntries());
        assertTrue("Should not contain any history entries", 0 == result.getHistoryEntries().size());
    }
    
    /**
     * Test of parsing output similar to that in subversions own svn repository.
     */
    @Test 
    public void ParseALaTfs() throws Exception {
        String historyOutput = "Changeset Change                     User              Date       Comment\n" +
                               "--------- -------------------------- ----------------- ---------- -------------\n" +
                               "21895     merge, edit                Joe P. Black      12/2/2012  Merge\n" +
                               "21293     merge, edit                Princess Zelda    9/4/2012   Merge\n" +
                               "21122     merge, edit                Joe P. Black      10/10/2012 Merge\n" +
                               "21106     merge, edit                Joe P. Black      10/10/2012 Merge\n" +
                               "20917     merge, edit                Joe P. Black      10/1/2012  Merge From 20\n" +
                               "20574     merge, edit                Joe P. Black      9/7/2012   \n" +
                               "20536     merge, edit                Joe P. Black      9/6/2012   Merge from 20\n" +
                               "20495     merge, edit                Princess Zelda    9/4/2012   merge js/css \n" +
                               "20429     add                        Joe P. Black      8/29/2012  Trunk";
        
        String revId1 = "21895";
        String author1 = "Joe P. Black";
        String changeType1 = "merge, edit";
        String date1= "12/2/2012";
        String revId2 = "21293";
        String author2 = "Princess Zelda";
        String date2= "9/4/2012";
        String revId3 = "21122";
        String author3 = "Joe P. Black";
        String date3= "10/10/2012";
        History result = instance.parse(historyOutput);
        assertNotNull(result);
        assertNotNull(result.getHistoryEntries());
        assertEquals(9, result.getHistoryEntries().size());
        
        HistoryEntry e1 = result.getHistoryEntries().get(0);
        assertEquals(revId1, e1.getRevision());
        assertEquals(author1, e1.getAuthor());
        assertEquals(0, e1.getFiles().size());

        HistoryEntry e2 = result.getHistoryEntries().get(1);
        assertEquals(revId2, e2.getRevision());
        assertEquals(author2, e2.getAuthor());
        assertEquals(0, e2.getFiles().size());

        HistoryEntry e3 = result.getHistoryEntries().get(2);
        assertEquals(revId3, e3.getRevision());
        assertEquals(author3, e3.getAuthor());
        assertEquals(0, e3.getFiles().size());
        assertTrue(e3.getMessage().contains("Merge"));
    }

}
