import static java.lang.System.out;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Launcher {
	
	private static final File CLEAN_REPO_ON_1_10 = new File("/Users/wluyima/Documents/clean_1_10/openmrs-core");
	
	private static final File REPO_WITH_CONFLICTS_ON_MASTER = new File(
	        "/Users/wluyima/Documents/conflict_master/openmrs-core");

	private static GitRepo gitRepoWithConflictsOnMaster;
	
	private static GitRepo cleanGitRepoOn1_10;
	
	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static void main(String[] args) throws Exception {
		out.println("----------------------------------------------------");
		gitRepoWithConflictsOnMaster = new GitRepo(REPO_WITH_CONFLICTS_ON_MASTER, "conflict_master");
		gitRepoWithConflictsOnMaster.checkStatus();
		out.println();
		
		cleanGitRepoOn1_10 = new GitRepo(CLEAN_REPO_ON_1_10, "clean_1_10");
		cleanGitRepoOn1_10.checkStatus();
		out.println();
		
		new ConflictHandler(gitRepoWithConflictsOnMaster, cleanGitRepoOn1_10, df, "2013-10-22 00:00:00").handle();
		out.println("Done...");
		out.println("----------------------------------------------------");
	}
}
