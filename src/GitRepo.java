import static java.lang.System.out;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jgit.api.DiffCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.filter.PathFilter;

public class GitRepo {
	
	private Git git;
	
	private Repository repo;
	
	private String repoName;
	
	@Override
	public String toString() {
		return this.repoName;
	}
	
	public GitRepo(File repoFile, String repoName) throws Exception {
		this.repoName = repoName;
		this.git = Git.open(repoFile);
		this.repo = git.getRepository();
	}
	
	public void checkStatus() throws Exception {
		Status status = git.status().call();
		out.println(this + " is on branch  " + repo.getBranch());
		if (!status.isClean()) {
			out.println("  > repo is not clean");
			return;
		}
		out.println("  > Nothing to commit, working directory clean");
	}
	
	public Status callStatus() throws Exception {
		return git.status().call();
	}
	
	public Set<String> getFilesWithConflicts() throws Exception {
		return new TreeSet<String>(callStatus().getConflicting());
	}
	
	public RevCommit getMostRecentCommitForFile(String path) throws Exception {
		ObjectId head = repo.resolve(Constants.HEAD);
		LogCommand logCommand = git.log();
		logCommand.add(head);
		logCommand.addPath(path);
		return getCommit(logCommand.call().iterator().next());
	}
	
	private RevCommit getCommit(AnyObjectId id) throws Exception {
		RevWalk revWalk = null;
		try {
			revWalk = new RevWalk(repo);
			return revWalk.parseCommit(id);
		}
		finally {
			if (revWalk != null) {
				revWalk.dispose();
			}
		}
	}
	
	public List<DiffEntry> getDiffEntryBetweenOneTenAndOneNine(String path) throws Exception {
		AbstractTreeIterator oneNineTreeParser = prepareTreeParserForRef("refs/heads/1.9.x");
		AbstractTreeIterator oneTenTreeParser = prepareTreeParserForRef("refs/heads/1.10.x");
		
		DiffCommand diffCommand = git.diff();
		diffCommand.setPathFilter(PathFilter.create(path));
		diffCommand.setOldTree(oneNineTreeParser).setNewTree(oneTenTreeParser);
		
		return diffCommand.call();
	}
	
	private AbstractTreeIterator prepareTreeParserForRef(String ref) throws Exception {
		// from the commit we can build the tree which allows us to construct the TreeParser
		Ref head = repo.getRef(ref);
		RevWalk walk = new RevWalk(repo);
		RevCommit commit = walk.parseCommit(head.getObjectId());
		RevTree tree = walk.parseTree(commit.getTree().getId());
		
		CanonicalTreeParser treeParser = new CanonicalTreeParser();
		ObjectReader reader = repo.newObjectReader();
		try {
			treeParser.reset(reader, tree.getId());
		}
		finally {
			reader.release();
		}
		return treeParser;
	}
}
