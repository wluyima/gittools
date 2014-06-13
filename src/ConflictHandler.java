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
	
	private static final File MISSING_FORWARD_PORTS_FILE = new File(OUTPUT_DIR, "missing_forward_ports.txt");
	
	private static final File PREFER_OURS_SH = new File(OUTPUT_DIR, "prefer_ours.sh");
	
	private static final String ENCODING = "UTF-8";
	
	private GitRepo gitRepoWithConflictsOnMaster;
	
	private GitRepo cleanGitRepoOn1_10;
	
	private DateFormat df;
	
	private String asOfDateStr;
	
	public ConflictHandler(GitRepo gitRepoWithConflictsOnMaster, GitRepo cleanGitRepoOn1_10, DateFormat df,
	    String asOfDateStr) {
		this.gitRepoWithConflictsOnMaster = gitRepoWithConflictsOnMaster;
		this.cleanGitRepoOn1_10 = cleanGitRepoOn1_10;
		this.df = df;
		this.asOfDateStr = asOfDateStr;
	}
	
	public void handle() throws Exception {
		
		setupOutputFiles();
		
		out.println("Generating output.....");
		final long start = currentTimeMillis();
		
		Set<String> paths = gitRepoWithConflictsOnMaster.getFilesWithConflicts();
		final Date asOfDate = df.parse(asOfDateStr);
		Set<String> pathsWhereOursWin = new LinkedHashSet<String>();
		StringBuilder sbPathsWhereOursWinDetails = new StringBuilder();
		StringBuilder sbPathsWithUnResolvedConflicts = new StringBuilder();
		StringBuilder missingForwardPortsDetails = new StringBuilder();
		StringBuilder shellScript = new StringBuilder();
		int oursCount = 0;
		int unresolvedConflictsCount = 0;
		int hasBackportsCounts = 0;
		int hasMissingForwardPortsCount = 0;
		
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
			
			//Compare file with 1.9.x versions to check for back ports and missing back ports
			List<DiffEntry> diffs = cleanGitRepoOn1_10.getDiffEntryBetweenOneTenAndOneNine(path);
			
			if ((committerIdent.getWhen().after(asOfDate) || committerIdent.getWhen().equals(asOfDate))) {
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
				
				if (diffs.size() == 0) {
					//This file has the same contents as the 1.9.x version but has back ports that
					//happened after creating the 1.10.x but could be back ports between 1.9.x and 
					//1.10.x but not yet in master
					hasBackportsCounts++;
					sbPathsWithUnResolvedConflicts.append("\n");
					sbPathsWithUnResolvedConflicts.append("  Possibly a back port");
                    //File has no changes in the cloned repo branch, so ours/original version wins
                    /*oursCount++;
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

                    shellScript.append("\n\ngit checkout --ours -- " + path);
                    shellScript.append("\ngit add " + path);*/
				}
			} else if (committerIdent.getWhen().before(asOfDate) && diffs.size() > 0) {
				hasMissingForwardPortsCount++;
				missingForwardPortsDetails.append("\n\n");
				missingForwardPortsDetails.append(path);
				missingForwardPortsDetails.append("\n");
				missingForwardPortsDetails.append("  Authored  [" + commit.getName() + "] "
				        + df.format(authorIdent.getWhen()) + " : " + authorIdent.getName() + " : "
				        + commit.getShortMessage());
				missingForwardPortsDetails.append("\n");
				missingForwardPortsDetails.append("  Committed [" + commit.getName() + "] "
				        + df.format(committerIdent.getWhen()) + " : " + committerIdent.getName() + " : "
				        + commit.getShortMessage());
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
				
				shellScript.append("\n\ngit checkout --ours -- " + path);
				shellScript.append("\ngit add " + path);
			}
		}
		
		//Write the results to the individual files
		FileUtils.writeLines(MERGE_OURS_FILE, ENCODING, pathsWhereOursWin);
		Date date = new Date();
		
		FileUtils.writeStringToFile(MERGE_OURS_DETAILS_FILE, "#Date:" + date + "\n#Count:" + oursCount
		        + sbPathsWhereOursWinDetails.toString(), ENCODING);
		
		FileUtils.writeStringToFile(CONFLICTS_DETAILS_FILE,
		    "#Date:" + date + "\n" + "#Count:" + unresolvedConflictsCount + "\n#Files possibly with back ports only:"
		            + hasBackportsCounts + sbPathsWithUnResolvedConflicts.toString(), ENCODING);
		
		FileUtils.writeStringToFile(MISSING_FORWARD_PORTS_FILE, "#Date:" + date + "\n#Count:" + hasMissingForwardPortsCount
		        + missingForwardPortsDetails.toString(), ENCODING);
		
		FileUtils.writeStringToFile(PREFER_OURS_SH, "#!/bin/bash" + shellScript.toString(), ENCODING);
		
		out.println("Time taken: " + ((currentTimeMillis() - start) / 1000) + "s");
	}
	
	private void setupOutputFiles() throws Exception {
		if (!OUTPUT_DIR.isDirectory()) {
			OUTPUT_DIR.mkdir();
		}
		if (MERGE_OURS_FILE.exists()) {
			MERGE_OURS_FILE.delete();
		}
		if (MERGE_OURS_DETAILS_FILE.exists()) {
			MERGE_OURS_DETAILS_FILE.delete();
		}
		if (CONFLICTS_DETAILS_FILE.exists()) {
			CONFLICTS_DETAILS_FILE.delete();
		}
		if (MISSING_FORWARD_PORTS_FILE.exists()) {
			MISSING_FORWARD_PORTS_FILE.delete();
		}
		if (PREFER_OURS_SH.exists()) {
			PREFER_OURS_SH.delete();
		}
	}
}
