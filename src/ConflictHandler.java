import static java.lang.System.currentTimeMillis;
import static java.lang.System.out;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

public class ConflictHandler {
	
	private static final File OUTPUT_DIR = new File("output");
	
	private static final File MERGE_OURS_FILE = new File(OUTPUT_DIR, "merge_ours.txt");
	
	private static final File MERGE_OURS_DETAILS_FILE = new File(OUTPUT_DIR, "merge_ours_details.txt");
	
	private static final File CONFLICTS_DETAILS_FILE = new File(OUTPUT_DIR, "conflict.txt");
	
	private static final String ENCODING = "UTF-8";
	
	//private GitRepo cleanGitRepoOnMaster;
	
	private GitRepo gitRepoWithConflictsOnMaster;
	
	private GitRepo cleanGitRepoOn1_10;
	
	private DateFormat df;
	
	private String asOfDateStr;
	
	public ConflictHandler(GitRepo cleanGitRepoOnMaster, GitRepo gitRepoWithConflictsOnMaster, GitRepo cleanGitRepoOn1_10,
	    DateFormat df, String asOfDateStr) {
		//this.cleanGitRepoOnMaster = cleanGitRepoOnMaster;
		this.gitRepoWithConflictsOnMaster = gitRepoWithConflictsOnMaster;
		this.cleanGitRepoOn1_10 = cleanGitRepoOn1_10;
		this.df = df;
		this.asOfDateStr = asOfDateStr;
	}
	
	public void handle() throws Exception {
		out.println("Generating output.....");
		final long start = currentTimeMillis();
		
		Set<String> paths = gitRepoWithConflictsOnMaster.getFilesWithConflicts();
		final Date asOfDate = df.parse(asOfDateStr);
		Set<String> pathsWhereOursWin = new LinkedHashSet<String>();
		StringBuilder sbPathsWhereOursWinDetails = new StringBuilder();
		StringBuilder sbPathsWithUnResolvedConflicts = new StringBuilder();
		int oursCount = 0;
		int unresolvedConflictsCount = 0;
		
		for (String path : paths) {
			RevCommit commit = cleanGitRepoOn1_10.getMostRecentCommitForFile(path);
			PersonIdent authorIdent = commit.getAuthorIdent();
			PersonIdent committerIdent = commit.getCommitterIdent();
			if (committerIdent.getWhen().before(committerIdent.getWhen())) {
				Util.throwException("Found strange commit where the committer date is before the author date");
			}
			
			//If the author date is after the asOfDate, then this file has changes in the current branch
			//in the cloned repo, therefore they will need to resolve the conflicts manually
			//We use committer's date just to be more sure because it comes later than the author date
			//since we want the most recent date when changes were made to the file
			if (committerIdent.getWhen().after(asOfDate)) {
				unresolvedConflictsCount++;
				sbPathsWithUnResolvedConflicts.append("\n\n");
				sbPathsWithUnResolvedConflicts.append(path);
				sbPathsWithUnResolvedConflicts.append("\n");
				sbPathsWithUnResolvedConflicts.append("  Authored  [" + commit.getName() + "] "
				        + df.format(authorIdent.getWhen()) + " : " + authorIdent.getName() + " : "
				        + commit.getShortMessage());
				sbPathsWithUnResolvedConflicts.append("\n");
				sbPathsWithUnResolvedConflicts.append("  Committed [" + commit.getName() + "] "
				        + df.format(committerIdent.getWhen()) + " : " + committerIdent.getName() + " : "
				        + commit.getShortMessage());
				if(committerIdent.getWhen().before(df.parse("2013-11-01 00:00:00"))){
                    sbPathsWithUnResolvedConflicts.append("ISSUES:");
                }
				List<DiffEntry> diffs = cleanGitRepoOn1_10.getDiffEntryBetweenMasterAndOneTen(path);
				if (diffs.size() == 0) {
					sbPathsWithUnResolvedConflicts.append("\n");
					sbPathsWithUnResolvedConflicts.append("   This was a back port");
				} else {
					//There can only be one diff since it is one file path
					DiffEntry diff = diffs.get(0);
					if (DiffEntry.ChangeType.ADD == diff.getChangeType()) {
						sbPathsWithUnResolvedConflicts.append("\n");
						sbPathsWithUnResolvedConflicts.append("   Added in 1.10.x");
						
					} else if (DiffEntry.ChangeType.DELETE == diff.getChangeType()) {
						sbPathsWithUnResolvedConflicts.append("\n");
						sbPathsWithUnResolvedConflicts.append("   Deleted in 1.10.x");
						
					} else if (DiffEntry.ChangeType.RENAME == diff.getChangeType()) {
						sbPathsWithUnResolvedConflicts.append("\n");
						sbPathsWithUnResolvedConflicts.append("   Renamed in 1.10.x");
					}
				}
				
			} else {
				//File has no changes in the cloned repo branch, so ours/original version wins
				oursCount++;
				pathsWhereOursWin.add(path);
				sbPathsWhereOursWinDetails.append("\n\n");
				sbPathsWhereOursWinDetails.append(path);
				sbPathsWhereOursWinDetails.append("\n");
				sbPathsWhereOursWinDetails.append("  Authored  [" + commit.getName() + "] "
				        + df.format(authorIdent.getWhen()) + " : " + authorIdent.getName() + " : "
				        + commit.getShortMessage());
				sbPathsWhereOursWinDetails.append("\n");
				sbPathsWhereOursWinDetails.append("  Committed [" + commit.getName() + "] "
				        + df.format(committerIdent.getWhen()) + " : " + committerIdent.getName() + " : "
				        + commit.getShortMessage());
			}
			//if(path.equals("api/src/main/java/org/openmrs/DrugOrder.java")){break;}
		}
		
		//Write the results to the individual files
		createFilesIfNecessary();
		FileUtils.writeLines(MERGE_OURS_FILE, ENCODING, pathsWhereOursWin);
		Date date = new Date();
		FileUtils.writeStringToFile(MERGE_OURS_DETAILS_FILE, "#Date:" + date + "\n#Count:" + oursCount
		        + sbPathsWhereOursWinDetails.toString(), ENCODING);
		FileUtils.writeStringToFile(CONFLICTS_DETAILS_FILE, "#Date:" + date + "\n" + "#Count:" + unresolvedConflictsCount
		        + sbPathsWithUnResolvedConflicts.toString(), ENCODING);
		
		out.println("Time taken: " + ((currentTimeMillis() - start) / 1000) + "s");
	}
	
	private void createFilesIfNecessary() throws Exception {
		if (!OUTPUT_DIR.isDirectory()) {
			OUTPUT_DIR.mkdir();
		}
		if (!MERGE_OURS_FILE.exists()) {
			MERGE_OURS_FILE.createNewFile();
		}
		if (!MERGE_OURS_DETAILS_FILE.exists()) {
			MERGE_OURS_DETAILS_FILE.createNewFile();
		}
		if (!CONFLICTS_DETAILS_FILE.exists()) {
			CONFLICTS_DETAILS_FILE.createNewFile();
		}
	}
}
