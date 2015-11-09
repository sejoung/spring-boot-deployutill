package kr.co.killers.deployutil.service.impl;

import kr.co.killers.deployutil.service.SVNService;
import org.springframework.stereotype.Service;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;

/**
 * Created by ASH on 2015-11-09.
 */
@Service
public class SVNServiceImpl implements SVNService {

    @Override
    public void getLatestFileRev(String url, String destPath, String id, String passwd) throws Exception {
        SVNRepository repository = null;
        //initiate the reporitory from the url
        repository = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(url));

        //create authentication data
        ISVNAuthenticationManager authManager =
                SVNWCUtil.createDefaultAuthenticationManager(id, passwd);
        repository.setAuthenticationManager(authManager);

        //output some data to verify connection
        System.out.println("Repository Root: " + repository.getRepositoryRoot(true));
        System.out.println("Repository UUID: " + repository.getRepositoryUUID(true));

        //need to identify latest revision
        long latestRevision = repository.getLatestRevision();
        System.out.println("Repository Latest Revision: " + latestRevision);

        //create client manager and set authentication
        SVNClientManager ourClientManager = SVNClientManager.newInstance();
        ourClientManager.setAuthenticationManager(authManager);

        //use SVNUpdateClient to do the export
        SVNUpdateClient updateClient = ourClientManager.getUpdateClient();
        updateClient.setIgnoreExternals(false);

        updateClient.doExport(repository.getLocation(), new File(destPath), SVNRevision.create(latestRevision), SVNRevision.create(latestRevision), null, true, SVNDepth.INFINITY);
    }
}