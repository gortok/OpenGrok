/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.opensolaris.opengrok.history;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import org.opensolaris.opengrok.OpenGrokLogger;
import org.opensolaris.opengrok.util.Executor;

/**
 *
 * @author gstocker
 */
class TeamFoundationServerHistoryParser implements Executor.StreamHandler {
    private Repository repository;
    final int columnHeaderLine = 1;
    final int separatorWidth = 1; //denotes number of spaces each column is separated by
        //set defaults, will calculate changes later.
    int changesetColumnWidth = 9;
    int changeColumnWidth = 26;
    int userColumnWidth = 17;
    int dateColumnWidth = 10;
    int commentColumnWidth = 13;
    final List<HistoryEntry> entries = new ArrayList<HistoryEntry>();

    public TeamFoundationServerHistoryParser() {
        repository = new TeamFoundationServerRepository();
    }
    public TeamFoundationServerHistoryParser(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void processStream(InputStream input) throws IOException {
        DateFormat df = repository.getDateFormat();
        BufferedReader in = new BufferedReader(new InputStreamReader(input));
        String s;
        HistoryEntry entry = null;
        while ((s = in.readLine()) != null) {
            entry = parseLine(s);
            entries.add(entry);
        }
    }

     /**
     * Parse the history for the specified file.
     *
     * @param file the file to parse history for
     * @param repos Pointer to the TeamFoundationServerRepository
     * @param sinceRevision the revision number immediately preceding the first
     * revision we want, or {@code null} to fetch the entire history
     * @return object representing the file's history
     */
    History parse(File file, TeamFoundationServerRepository repository, String sinceRevision) throws HistoryException {
        Executor executor = repository.getHistoryLogExecutor(file, sinceRevision);
        int status = executor.exec(true, this);
        
        if (status != 0) {
            throw new HistoryException("Failed to get history for: \"" + file.getAbsolutePath() + "\" Exit code: " + status);
        }
        
        Executor exec = repository.getHistoryLogExecutor(file, sinceRevision); 
        if (status != 0) {
            throw new HistoryException("Failed to get history for: \"" + file.getAbsolutePath() + "\" Exit code: " + status);
            
            
        }
        return new History(this.entries);
   }

    History parse(String string) {
        if (string.length() == 0) return new History();
        if (string.startsWith("Changeset Change"))  return parseBrief(string);
        return parseDetailed(string);
    }
    /**
    * Parse history that's in /format:brief (or no format specified) for TFS history
    * @param briefChangesetHistory
    * @return object representing file's history
    * "file" is a nebelous term; can mean a singular 'file', or a directory (YAY TFS)
    * 
    */
    History parseBrief(String changesetHistory) {
       
        
        String[] lines = changesetHistory.split("\n");
        if (lines[2].length() == 0) { //no data
            return new History(); 
        }
        String[] columnWidths = lines[columnHeaderLine].split(" ");
        if (columnWidths.length == 5)
        {
            changesetColumnWidth = columnWidths[0].length();
            changeColumnWidth = columnWidths[1].length();
            userColumnWidth = columnWidths[2].length();
            dateColumnWidth = columnWidths[3].length();
            commentColumnWidth = columnWidths[4].length();
        }
        History tfsHistory = new History();
        ArrayList<HistoryEntry> historyEntries = new ArrayList<>();
        for (int i = 2; i < lines.length; i++) { //Start at first line of data
            HistoryEntry entry = parseLine(lines[i]);
            historyEntries.add(entry);
        }
        tfsHistory.setHistoryEntries(historyEntries);
        return tfsHistory;
        
    }
    
    HistoryEntry parseLine(String line){
        String revision = line.substring(0, changesetColumnWidth - 1).trim();
        String change = line.substring(changesetColumnWidth + separatorWidth, changeColumnWidth -1).trim();
        String author = line.substring(changesetColumnWidth + separatorWidth + changeColumnWidth + separatorWidth, changesetColumnWidth + separatorWidth + changeColumnWidth + separatorWidth + userColumnWidth-1).trim();
        String dateAsString = line.substring(changesetColumnWidth + separatorWidth + changeColumnWidth + separatorWidth + userColumnWidth + separatorWidth, changesetColumnWidth + separatorWidth + changeColumnWidth + separatorWidth + userColumnWidth+ dateColumnWidth -1).trim();
        String message = line.substring(line.length() - commentColumnWidth, line.length()).trim();
        Date date = null;
        try {
            date = new SimpleDateFormat("MM/dd/yyyy").parse(dateAsString);
        } catch (ParseException ex) {
            OpenGrokLogger.getLogger().log(Level.SEVERE, "Failed to parse as date: " + dateAsString, ex);
        }

        HistoryEntry entry = new HistoryEntry(revision, date , author, null, message, true);
        return entry;
    }
    
    History parseDetailed(String changesetHistory){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
