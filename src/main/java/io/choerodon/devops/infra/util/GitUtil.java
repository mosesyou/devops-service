package io.choerodon.devops.infra.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.choerodon.core.exception.CommonException;
import io.choerodon.devops.api.vo.GitConfigVO;
import io.choerodon.devops.api.vo.GitEnvConfigVO;
import io.choerodon.devops.app.service.DevopsEnvironmentService;
import io.choerodon.devops.infra.dto.DevopsEnvironmentDTO;
import io.choerodon.devops.infra.dto.iam.OrganizationDTO;
import io.choerodon.devops.infra.dto.iam.ProjectDTO;
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator;

/**
 * Created by younger on 2018/3/29.
 */
@Component
public class GitUtil {
    public static final String DEV_OPS_SYNC_TAG = "devops-sync";
    public static final String TEMPLATE = "template";
    private static final String MASTER = "master";
    private static final String PATH = "/";
    private static final String GIT_SUFFIX = "/.git";
    private static final String ERROR_GIT_CLONE = "error.git.clone";
    private static final String REPO_NAME = "devops-service-repo";
    private static final Logger LOGGER = LoggerFactory.getLogger(GitUtil.class);
    private Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
    @Autowired
    private DevopsEnvironmentService devopsEnvironmentService;
    @Autowired
    private BaseServiceClientOperator baseServiceClientOperator;
    private String classPath;
    @Value("${services.gitlab.sshUrl}")
    private String gitlabSshUrl;

    /**
     * 构造方法
     */
    public GitUtil() {
        try {
            ResourceLoader resourceLoader = new DefaultResourceLoader();
            this.classPath = resourceLoader.getResource("/").getURI().getPath();
            String repositoryPath = this.classPath == null ? "" : this.classPath + REPO_NAME;
            File repo = new File(repositoryPath);
            if (!repo.exists() && repo.mkdirs()) {
                LOGGER.info("create {} success", repositoryPath);
            }
        } catch (IOException io) {
            throw new CommonException(io.getMessage(), io);
        }
    }

    /**
     * 验证无需token就可以进行访问的代码仓库的克隆地址是否有效
     *
     * @param repositoryUrl 代码仓库克隆地址
     * @param token         访问仓库所需的token（可为空）
     * @return true if the url is valid and the ls-remote result is not empty.
     */
    public static Boolean validRepositoryUrl(String repositoryUrl, String token) {
        LsRemoteCommand lsRemoteCommand = new LsRemoteCommand(null);
        lsRemoteCommand.setRemote(repositoryUrl);
        if (!StringUtils.isEmpty(token)) {
            lsRemoteCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider("", token));
        }
        try {
            int size = lsRemoteCommand.call().size();
            if (size == 0) {
                return null;
            } else {
                return Boolean.TRUE;
            }
        } catch (GitAPIException e) {
            return Boolean.FALSE;
        }
    }

    private static String getLog(String repoPath, String fileName) {
        String latestCommit = "";
        File file = new File(repoPath);
        try (Repository repository = new FileRepository(file.getAbsolutePath())) {
            try (Git git = new Git(repository)) {
                Iterable<RevCommit> logs = git.log().addPath(fileName).call();
                Iterator<RevCommit> revCommitIterator = logs.iterator();
                if (revCommitIterator.hasNext()) {
                    latestCommit = revCommitIterator.next().getName();
                }
            }
        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }
        return latestCommit;
    }

    public static String getFileLatestCommit(String path, String filePath) {
        if (filePath != null) {
            String[] fileName = filePath.split("/");
            return GitUtil.getLog(path, fileName[fileName.length - 1]);
        }
        return "";
    }

    public static String getGitlabSshUrl(Pattern pattern, String url, String orgCode, String proCode, String envCode) {
        String result = "";
        if (url.contains("@")) {
            String[] urls = url.split(":");
            if (urls.length == 1) {
                result = String.format("%s:%s-%s-gitops/%s.git",
                        url, orgCode, proCode, envCode);
            } else {
                if (pattern.matcher(urls[1]).matches()) {
                    result = String.format("ssh://%s/%s-%s-gitops/%s.git",
                            url, orgCode, proCode, envCode);
                }
            }
        } else {
            String[] urls = url.split(":");
            if (urls.length == 1) {
                result = String.format("git@%s:%s-%s-gitops/%s.git",
                        url, orgCode, proCode, envCode);
            } else {
                if (pattern.matcher(urls[1]).matches()) {
                    result = String.format("ssh://git@%s/%s-%s-gitops/%s.git",
                            url, orgCode, proCode, envCode);
                }
            }
        }
        return result;
    }

    /**
     * clone by ssh
     *
     * @param path target path
     * @param url  git repo url
     */
    public Git cloneBySsh(String path, String url, String sshKeyRsa) {
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setURI(url);
        cloneCommand.setBranch(MASTER);
        cloneCommand.setTransportConfigCallback(getTransportConfigCallback(sshKeyRsa));
        try {
            cloneCommand.setDirectory(new File(path));
            return cloneCommand.call();
        } catch (GitAPIException e) {
            throw new CommonException(e.getMessage(), e);
        }
    }

    /**
     * check git repo to commit
     *
     * @param path   git repo path
     * @param commit target commit or branch or tag
     */
    public void checkout(String path, String commit) {

        File repoGitDir = new File(path);
        try (Repository repository = new FileRepository(repoGitDir.getAbsolutePath())) {
            checkout(commit, repository);
        } catch (IOException e) {
            LOGGER.info("Get repository error", e);
        }
    }

    private void checkout(String commit, Repository repository) {
        try (Git git = new Git(repository)) {
            git.checkout().setName(commit).call();
        } catch (GitAPIException e) {
            LOGGER.info("Checkout error ", e);
        }
    }

//    /**
//     * pull git repo using ssh
//     *
//     * @param path git repo
//     */
//    public void pullBySsh(String path) {
//        File repoGitDir = new File(path);
//        try (Repository repository = new FileRepository(repoGitDir.getAbsolutePath())) {
//            pullBySsh(repository);
//        } catch (IOException e) {
//            LOGGER.info("Get repository error", e);
//        }
//    }
//
//    private void pullBySsh(Repository repository) {
//        try (Git git = new Git(repository)) {
//            git.pull()
//                    .setTransportConfigCallback(getTransportConfigCallback())
//                    .setRemoteBranchName(MASTER)
//                    .call();
//        } catch (GitAPIException e) {
//            LOGGER.info("Pull error", e);
//        }
//    }

    private TransportConfigCallback getTransportConfigCallback(String sshKeyRsa) {
        return transport -> {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactor(sshKeyRsa));
        };
    }

    private SshSessionFactory sshSessionFactor(String sshKeyRsa) {
        return new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
                session.setConfig("StrictHostKeyChecking", "no");
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);
                defaultJSch.getIdentityRepository().removeAll();
                defaultJSch.getIdentityRepository().add(sshKeyRsa.getBytes());
                return defaultJSch;
            }
        };
    }

    /**
     * clone 并checkout
     *
     * @param dirName
     * @param remoteUrl
     * @param accessToken
     * @return
     */
    public void cloneAndCheckout(String dirName, String remoteUrl, String accessToken, String commit) {
        File localPathFile = new File(dirName);
        try {
            Git git = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(localPathFile)
                    .setCredentialsProvider(StringUtils.isEmpty(accessToken) ? null : new UsernamePasswordCredentialsProvider("", accessToken))
                    .call();
            git.checkout().setName(commit).call();
            git.close();
            FileUtil.deleteDirectory(new File(localPathFile + GIT_SUFFIX));
        } catch (Exception e) {
            throw new CommonException(ERROR_GIT_CLONE, e);
        }
    }

    /**
     * Git克隆
     */
    public String cloneAppMarket(String name, String commit, String remoteUrl, String adminToken) {
        Git git = null;
        String workingDirectory = getWorkingDirectory(name);
        File localPathFile = new File(workingDirectory);
        deleteDirectory(localPathFile);
        try {
            git = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(localPathFile)
                    .setCredentialsProvider(StringUtils.isEmpty(adminToken) ? null : new UsernamePasswordCredentialsProvider("", adminToken))
                    .call();
            git.checkout().setName(commit).call();
            git.close();
            FileUtil.deleteDirectory(new File(localPathFile + GIT_SUFFIX));
        } catch (Exception e) {
            throw new CommonException(ERROR_GIT_CLONE, e);
        }
        return workingDirectory;
    }

    /**
     * 克隆公开仓库的或者根据access token克隆私库的代码所有分支
     *
     * @param dirName     directory name
     * @param remoteUrl   remote url to clone
     * @param accessToken the access token for access
     * @return the git instance of local repository
     */
    public Git cloneRepository(String dirName, String remoteUrl, String accessToken) {
        Git git;
        String workingDirectory = getWorkingDirectory(dirName);
        File localPathFile = new File(workingDirectory);
        deleteDirectory(localPathFile);
        try {
            Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setCloneAllBranches(true)
                    .setCredentialsProvider(StringUtils.isEmpty(accessToken) ? null : new UsernamePasswordCredentialsProvider("", accessToken))
                    .setDirectory(localPathFile)
                    .call();
            git = Git.open(new File(localPathFile + GIT_SUFFIX));
        } catch (GitAPIException | IOException e) {
            throw new CommonException(ERROR_GIT_CLONE, e);
        }
        return git;
    }

    /**
     * 克隆公开仓库或者根据access token克隆代码克隆特定分支
     *
     * @param dirName     directory name
     * @param remoteUrl   remote url to clone
     * @param accessToken the access token for access
     * @param branchName  branch name
     * @return the git instance of local repository
     */
    public Git cloneRepository(String dirName, String remoteUrl, String accessToken, String branchName) {
        String workingDirectory = getWorkingDirectory(dirName);
        File localPathFile = new File(workingDirectory);
        deleteDirectory(localPathFile);
        try {
            return Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setCloneAllBranches(false)
                    .setBranch(branchName)
                    .setCredentialsProvider(StringUtils.isEmpty(accessToken) ? null : new UsernamePasswordCredentialsProvider("", accessToken))
                    .setDirectory(localPathFile)
                    .call();
        } catch (GitAPIException e) {
            throw new CommonException(ERROR_GIT_CLONE, e);
        }
    }

    /**
     * 提交并push到远程代码仓库
     *
     * @param git         本地git对象
     * @param repoUrl     仓库地址
     * @param accessToken token
     * @throws CommonException 异常发生时，应捕获此异常，关闭资源
     */
    public void commitAndPush(Git git, String repoUrl, String accessToken, String refName) {
        try {
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call();
            git.commit().setMessage("Render Variables[skip ci]").call();
            List<Ref> refs = git.branchList().call();
            PushCommand pushCommand = git.push();
            for (Ref ref : refs) {
                if (ref.getName().equals(refName)) {
                    pushCommand.add(ref);
                    break;
                }
            }
            pushCommand.setRemote(repoUrl);
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                    "", accessToken));
            pushCommand.call();
        } catch (GitAPIException e) {
            throw new CommonException("error.git.push", e);
        }
    }

    /**
     * 将单个分支推送到远程仓库
     *
     * @param git         Git对象
     * @param repoUrl     仓库地址
     * @param accessToken 访问token
     * @param branchName  要推送的分支名
     */
    public void push(Git git, String repoUrl, String accessToken, String branchName) {
        try {
            // 对应分支名的本地引用名
            String localRefName = Constants.R_HEADS + branchName;
            // 找出对应分支名的本地分支引用
            Ref localRef = null;
            List<Ref> refs = git.branchList().call();
            for (Ref ref : refs) {
                if (ref.getName().equals(localRefName)) {
                    localRef = ref;
                    break;
                }
            }

            // 如果在本地分支找不到匹配branchName的Ref直接返回
            if (localRef == null) {
                return;
            }

            // 推代码
            git.push().add(localRef)
                    .setRemote(repoUrl)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("", accessToken))
                    .call();
        } catch (GitAPIException e) {
            throw new CommonException("error.git.push", e);
        }
    }

    public void commitAndPushForMaster(Git git, String repoUrl, String commitMessage, String accessToken) {
        try {
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call();
            git.commit().setMessage("The download version：" + commitMessage).call();
            PushCommand pushCommand = git.push();
            pushCommand.add(MASTER);
            pushCommand.setRemote(repoUrl);
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider("", accessToken));
            pushCommand.call();
        } catch (GitAPIException e) {
            throw new CommonException("error.git.push", e);
        }
    }

    /**
     * 获取最近一次commit
     *
     * @param git
     * @return
     */
    public String getFirstCommit(Git git) {
        String commit;
        try {
            Iterable<RevCommit> log = git.log().call();
            commit = log.iterator().next().getName();
        } catch (GitAPIException e) {
            throw new CommonException("error.get.commit");
        }
        return commit;
    }

    /**
     * 将代码推到目标库
     */
    public void push(Git git, String name, String commit, String repoUrl, String userName, String accessToken) {
        try {
            String[] url = repoUrl.split("://");
            git.add().addFilepattern(".").call();
            git.add().setUpdate(true).addFilepattern(".").call();
            git.commit().setMessage(commit).call();
            List<Ref> refs = git.branchList().call();
            PushCommand pushCommand = git.push();
            for (Ref ref : refs) {
                pushCommand.add(ref);
            }
            pushCommand.setRemote(url[0] + "://gitlab-ci-token:" + accessToken + "@" + url[1]);
            LOGGER.info("push remote is: {}", pushCommand.getRemote());
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                    userName, accessToken));
            pushCommand.call();
        } catch (GitAPIException e) {
            throw new CommonException("error.git.push", e);
        } finally {
            //删除模板
            deleteWorkingDirectory(name);
            if (git != null) {
                git.close();
            }
        }
    }

    /**
     * 获取工作目录
     */
    public String getWorkingDirectory(String name) {
        String path = this.classPath == null ? REPO_NAME + PATH + name : this.classPath + REPO_NAME + PATH + name;
        return path.replace(PATH, File.separator);
    }

    /**
     * 删除工作目录
     */
    public void deleteWorkingDirectory(String name) {
        String path = getWorkingDirectory(name);
        File file = new File(path);
        deleteDirectory(file);
    }

    /**
     * 删除文件
     */
    private void deleteDirectory(File file) {
        if (file.exists()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                throw new CommonException("error.directory.delete", e);
            }
        }
    }

    public Git initGit(File file) {
        Git git = null;
        try {
            git = Git.init().setDirectory(file).call();
        } catch (GitAPIException e) {
            throw new CommonException("error.git.init", e);
        }
        return git;
    }

    /**
     * Git克隆
     */
    public String clone(String name, String remoteUrl, String accessToken) {
        String workingDirectory = getWorkingDirectory(name);
        File oldLocalPathFile = new File(workingDirectory);
        deleteDirectory(oldLocalPathFile);
        try {
            Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(oldLocalPathFile)
                    .setCredentialsProvider(StringUtils.isEmpty(accessToken) ? null : new UsernamePasswordCredentialsProvider("", accessToken))
                    .call()
                    .close();
        } catch (GitAPIException e) {
            throw new CommonException(ERROR_GIT_CLONE, e);
        }
        return workingDirectory;
    }


    /**
     * 合并
     *
     * @param newFilePath
     * @param oldFilePath
     * @return
     */
    public Git combineAppMarket(String oldFilePath, String newFilePath) {
        Git git = null;
        FileUtil.copyDir(new File(oldFilePath + GIT_SUFFIX), new File(newFilePath + GIT_SUFFIX));
        try {
            git = Git.open(new File(newFilePath));
        } catch (IOException e) {
            throw new CommonException("error.git.open", e);
        }
        return git;
    }

//    /**
//     * push current git repo
//     *
//     * @param git git repo
//     * @throws GitAPIException push error
//     */
//    public void gitPush(Git git) throws GitAPIException {
//        git.push().setTransportConfigCallback(getTransportConfigCallback()).call();
//    }

//    /**
//     * push current git repo
//     *
//     * @param git git repo
//     * @throws GitAPIException push error
//     */
//    public void gitPushTag(Git git) throws GitAPIException {
//        List<Ref> refs = git.branchList().call();
//        PushCommand pushCommand = git.push();
//        for (Ref ref : refs) {
//            pushCommand.add(ref);
//        }
//        pushCommand.setPushTags();
//        pushCommand.setTransportConfigCallback(getTransportConfigCallback()).call();
//    }

    /**
     * create a file in git repo, and then commit it
     *
     * @param repoPath     git repo path
     * @param git          git repo
     * @param relativePath file relative path
     * @param fileContent  file content
     * @param commitMsg    commit msg, if null, commit msg will be '[ADD] add ' + file relative path
     * @throws IOException     if target repo is not found
     * @throws GitAPIException if target repo is not a git repo
     */
    public void createFileInRepo(String repoPath, Git git, String relativePath, String fileContent, String commitMsg)
            throws IOException, GitAPIException {
        FileUtil.saveDataToFile(repoPath, relativePath, fileContent);
        boolean gitProvided = git != null;
        git = gitProvided ? git : Git.open(new File(repoPath));
        addFile(git, relativePath);
        commitChanges(git, commitMsg == null || commitMsg.isEmpty() ? "[ADD] add " + relativePath : commitMsg);
        if (!gitProvided) {
            git.close();
        }
    }


    public String handDevopsEnvGitRepository(Long projectId, String envCode, String envRsa) {
        ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(projectId);
        OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
        //本地路径
        String path = String.format("gitops/%s/%s/%s",
                organizationDTO.getCode(), projectDTO.getCode(), envCode);
        //生成环境git仓库ssh地址
        String url = GitUtil.getGitlabSshUrl(pattern, gitlabSshUrl, organizationDTO.getCode(),
                projectDTO.getCode(), envCode);

        File file = new File(path);
        if (!file.exists()) {
            this.cloneBySsh(path, url, envRsa);
        }
        return path;
    }


    public GitConfigVO getGitConfig(Long clusterId) {
        List<DevopsEnvironmentDTO> devopsEnvironments = devopsEnvironmentService.baseListByClusterId(clusterId);
        GitConfigVO gitConfigVO = new GitConfigVO();
        List<GitEnvConfigVO> gitEnvConfigDTOS = new ArrayList<>();
        devopsEnvironments.stream().filter(devopsEnvironmentE -> devopsEnvironmentE.getGitlabEnvProjectId() != null).forEach(devopsEnvironmentE -> {
            ProjectDTO projectDTO = baseServiceClientOperator.queryIamProjectById(devopsEnvironmentE.getProjectId());
            OrganizationDTO organizationDTO = baseServiceClientOperator.queryOrganizationById(projectDTO.getOrganizationId());
            String repoUrl = GitUtil.getGitlabSshUrl(pattern, gitlabSshUrl, organizationDTO.getCode(), projectDTO.getCode(), devopsEnvironmentE.getCode());

            GitEnvConfigVO gitEnvConfigVO = new GitEnvConfigVO();
            gitEnvConfigVO.setEnvId(devopsEnvironmentE.getId());
            gitEnvConfigVO.setGitRsaKey(devopsEnvironmentE.getEnvIdRsa());
            gitEnvConfigVO.setGitUrl(repoUrl);
            gitEnvConfigVO.setNamespace(devopsEnvironmentE.getCode());
            gitEnvConfigDTOS.add(gitEnvConfigVO);
        });
        gitConfigVO.setEnvs(gitEnvConfigDTOS);
        gitConfigVO.setGitHost(gitlabSshUrl);
        return gitConfigVO;
    }

    private void addFile(Git git, String relativePath) throws GitAPIException {
        git.add().setUpdate(false).addFilepattern(relativePath).call();
        git.add().setUpdate(true).addFilepattern(relativePath).call();
    }

    private void commitChanges(Git git, String commitMsg) throws GitAPIException {
        git.commit().setMessage(commitMsg).call();
    }
}
