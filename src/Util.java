import static java.lang.System.out;

import java.util.Map;

import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.IndexDiff;

public class Util {
	
	static void throwException(String msg) {
		throw new RuntimeException("Some error occurred: " + msg);
	}
	
	static void resolveFilesWithNoChanges(GitRepo repo, String branchName) throws Exception {
		Status status = repo.callStatus();
		if (status.getConflicting().size() > 0) {
			out.println("Conflicts:");
			int count = 0;
			for (Map.Entry<String, IndexDiff.StageState> pathAndState : status.getConflictingStageState().entrySet()) {
				count++;
				out.println(pathAndState.getValue() + ": " + pathAndState.getKey());
				//repo.getLatestRevisionCommitForFile(pathAndState.getKey());
				if (count > 10) {
					break;
				}
			}
		}
	}
}
