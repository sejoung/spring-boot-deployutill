package kr.co.killers.deployutil.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import kr.co.killers.deployutil.constants.CommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import kr.co.killers.deployutil.service.SVNService;

import javax.tools.*;

/**
 * Created by ASH on 2015-11-09.
 */
@Service
public class SVNServiceImpl implements SVNService {
    private static final Logger log = LoggerFactory.getLogger(SVNServiceImpl.class);

    @Override
    public Map<String, String> getLatestFileCheckout(String url, String destPath, String id, String password, int startRevision, int endRevision) throws Exception {
        Map<String, String> classNameMap = new HashMap<String, String>();
        SVNRepository repository = null;
        repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));

        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(id, password);
        repository.setAuthenticationManager(authManager);

        long latestRevision = repository.getLatestRevision();
        System.out.println("Repository Latest Revision: " + latestRevision);

        SVNClientManager ourClientManager = SVNClientManager.newInstance();
        ourClientManager.setAuthenticationManager(authManager);

        // use SVNUpdateClient to do the export
        SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
        updateClient.setIgnoreExternals(false);
        updateClient.doExport(repository.getLocation(), new File(destPath), SVNRevision.create(startRevision), SVNRevision.create(latestRevision), null, true, SVNDepth.INFINITY);

        Collection logEntries = null;

        logEntries = repository.log(new String[]{""}, null, startRevision, endRevision, true, true);

        for (Iterator entries = logEntries.iterator(); entries.hasNext(); ) {
            SVNLogEntry logEntry = (SVNLogEntry) entries.next();

            if (logEntry.getChangedPaths().size() > 0) {

                Set changedPathsSet = logEntry.getChangedPaths().keySet();
                for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext(); ) {
                    SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths().get(changedPaths.next());
                    String key = entryPath.getPath();
                    String typeValueTemp = classNameMap.get(key);
                    if (typeValueTemp != null && !"".equals(typeValueTemp)) {
                        classNameMap.put(key, typeValueTemp + entryPath.getType());
                    } else {
                        classNameMap.put(key, String.valueOf(entryPath.getType()));
                    }

                }

            }
        }
        return classNameMap;
    }

    @Override
    public Map<String, String> getRepositorypaths(String url, String id, String password, int startRevision, int endRevision) throws Exception {
        Map<String, String> classNameMap = new HashMap<String, String>();
        SVNRepository repository = null;
        repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(id, password);
        repository.setAuthenticationManager(authManager);

        Collection logEntries = null;

        logEntries = repository.log(new String[]{""}, null, startRevision, endRevision, true, true);

        for (Iterator entries = logEntries.iterator(); entries.hasNext(); ) {
            SVNLogEntry logEntry = (SVNLogEntry) entries.next();

            if (logEntry.getChangedPaths().size() > 0) {

                Set changedPathsSet = logEntry.getChangedPaths().keySet();
                for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths.hasNext(); ) {
                    SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry.getChangedPaths().get(changedPaths.next());
                    String key = entryPath.getPath();
                    String typeValueTemp = classNameMap.get(key);
                    if (typeValueTemp != null && !"".equals(typeValueTemp)) {
                        classNameMap.put(key, typeValueTemp + entryPath.getType());
                    } else {
                        classNameMap.put(key, String.valueOf(entryPath.getType()));
                    }

                }

            }
        }

        return classNameMap;
    }

    @Override
    public boolean compileComplete(String soruceDir, String destDir, String libDir) throws Exception {
        // todo: parameter 수정이 필요함. JAVA_VERSION 등..
        ArrayList<String> filePath = new ArrayList<String>();

        class DirectoryContents {
            public void displayDirectoryContents(File dir) throws IOException {
                File[] files = dir.listFiles();
                for (File file : files) {
                    if (file.isDirectory()) {
                        displayDirectoryContents(file);
                    } else {
                        filePath.add(file.getCanonicalPath());
                    }
                }
            }
        }

        DirectoryContents dc = new DirectoryContents();
        dc.displayDirectoryContents(new File(soruceDir));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

        // todo: charset -> parameter 변경
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), Charset.forName(CommonConstants.CHARSET_UTF8));
        List<File> sourceFileList = new ArrayList<File>();

        for (int i = 0; i < filePath.size(); i++) {
            if(filePath.get(i).toUpperCase().contains(".JAVA")) {
                sourceFileList.add(new File(filePath.get(i)));
            }
        }

        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFileList);

        // todo: charset -> parameter 변경
        Iterable<String> compileOptions = Arrays.asList("-encoding", CommonConstants.CHARSET_UTF8, "-d", destDir, "-classpath", libDir);
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, compileOptions, null, compilationUnits);
        boolean success = task.call();

        for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
            log.debug("Error on line %d in %s%n", diagnostic.getLineNumber(), diagnostic.getSource().toString());
        }

        fileManager.close();
        log.debug("Success: " + success);

        return success;
    }
}
