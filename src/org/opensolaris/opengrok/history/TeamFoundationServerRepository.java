/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensolaris.opengrok.history;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.opensolaris.opengrok.OpenGrokLogger;
import org.opensolaris.opengrok.util.Executor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

/**
 * Access to a Team Foundation Server (TFS) Repository
 *
 * @author George Stocker (Github: @gortok)
 */
public class TeamFoundationServerRepository extends Repository {

    private static final long serialVersionUID = 1L;
    /**
     * The property name used to obtain the client command for this repository.
     */
    public static final String CMD_PROPERTY_KEY
            = "org.opensolaris.opengrok.history.TeamFoundationServer";
    /**
     * The command to use to access the repository if none was given explicitly
     */
    public static final String CMD_FALLBACK = "tf";

    protected String reposPath;

    public TeamFoundationServerRepository() {
        type = "TeamFoundationServer";
        datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    }

    @Override
    boolean fileHasHistory(File file) {
        return true;
    }

    @Override
    boolean hasHistoryForDirectories() {
        return true;
    }
    
    @Override
    public void setDirectoryName(String directoryName){
        super.setDirectoryName(directoryName);
        
        if(isWorking()) {
            Boolean rootFound = Boolean.FALSE;
            
            List<String> cmd = new ArrayList<String>();
            cmd.add(this.cmd);
            cmd.add("workfold");
            File directory = new File(getDirectoryName());
            Executor executor = new Executor(cmd, directory);
            if (executor.exec() == 0) {
                try {
                    InputStream workfoldResult = executor.getOutputStream();
                    InputStreamReader streamReader = new InputStreamReader(workfoldResult);
                    BufferedReader bufferedReader = new BufferedReader(streamReader);
                    String currentLine = bufferedReader.readLine(); //Mac? Linux? Windows? WHOSE LINE IS IT ANYWAY?
                    String remoteUrl = "";
                    String workspaceRepositoryDirectory = "";
                    while(currentLine != null) {
                        if (currentLine.contains("Collection: "))
                        {
                            int index = currentLine.indexOf("Collection: ");
                            if (index >= 0) {
                                remoteUrl = currentLine.substring(index+"Collection: ".length(), currentLine.length()).trim();
                            }
                        }
                        if (currentLine.contains("$/")) //standard character to denote that the line is a workspace mapping
                        {
                            String workspaces[] = currentLine.split("\\s+");
                            String driveLetterPattern = "[A-Za-z]:\\+";
                            Pattern pattern = Pattern.compile(driveLetterPattern);
                            
                            for (String w : workspaces)
                            {
                                Matcher matcher = pattern.matcher(w);
                                if (matcher.matches())
                                {
                                    int i = w.indexOf(":\\");
                                    this.directoryName = w.substring(i+":\\".length(), w.length()).trim();
                                }
                            }
                        }
                    }
                }
                catch(IOException ex)
                {
                    directoryName = null;
                }
            }
        }
    }
    @Override
    History getHistory(File file) throws HistoryException {
        return getHistory(file, null);
    }

    @Override
    History getHistory(File file, String sinceRevision)
            throws HistoryException {
        return new TeamFoundationServerHistoryParser().parse(file, this, sinceRevision);
    }
    Executor getHistoryLogExecutor(final File file, String sinceRevision) {
        String filePath;
        try {
            filePath = file.getCanonicalPath();
        } catch (IOException ex) {
            OpenGrokLogger.getLogger().log(Level.SEVERE,
                "Failed to get canonical path: {0}", ex.getClass().toString());
            return null;
        }
        String filename = "";
        if (filePath.length() > directoryName.length()) {
            filename = filePath.substring(directoryName.length() + 1);
        }
        
        List<String> cmd = new ArrayList<String>();
        ensureCommand(CMD_PROPERTY_KEY, CMD_FALLBACK);
        cmd.add(this.cmd);
        cmd.add("history");
        if (sinceRevision != null && sinceRevision.length() > 0) {
            cmd.add(String.format("/version:%s", sinceRevision));
        }
        cmd.add(filename);
        cmd.add("/noprompt");
        return new Executor(cmd, new File(directoryName));
        
    }
    

    @Override
    InputStream getHistoryGet(String parent, String basename, String rev) {
        InputStream ret = null;

        File directory = new File(directoryName);

        String filepath;
        try {
            filepath = (new File(parent, basename)).getCanonicalPath();
        } catch (IOException exp) {
            OpenGrokLogger.getLogger().log(Level.SEVERE,
                    "Failed to get canonical path: {0}", exp.getClass().toString());
            return null;
        }
        String filename = filepath.substring(directoryName.length() + 1);

        List<String> cmd = new ArrayList<String>();
        ensureCommand(CMD_PROPERTY_KEY, CMD_FALLBACK);
        cmd.add(this.cmd);
        cmd.add("history");
        if (rev != null && rev.length() > 0) {
            cmd.add(String.format("/version:%s", rev));
        }
        cmd.add(filename);
        cmd.add("/noprompt"); //because Windows by default pulls up a prompt. Thanks, Windows.

        Executor executor = new Executor(cmd, directory);
        if (executor.exec() == 0) {
            ret = executor.getOutputStream();
        }

        return ret;

    }

    @Override
    boolean fileHasAnnotation(File file) {
        return true;
    }

    @Override
    Annotation annotate(File file, String revision) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    void update() throws IOException {
        File directory = new File(getDirectoryName());

        List<String> cmd = new ArrayList<String>();
        ensureCommand(CMD_PROPERTY_KEY, CMD_FALLBACK);
        cmd.add(this.cmd);
        cmd.add("get");
        cmd.add("/recursive");
        Executor executor = new Executor(cmd, directory);
        if (executor.exec() != 0) {
            throw new IOException(executor.getErrorString());
        }
    }

    @Override
    boolean isRepositoryFor(File file) {
        if (file.isDirectory()) {
            File f = new File(file, ".vsscc");
            return f.exists() && f.isDirectory();
        }
        return false;
    }
    @Override
    public boolean isWorking() {
        return working != null && working.booleanValue();
    }

    public void setWorking(Boolean working) {
        this.working = working;
    }

}
