import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License. Copyright (C) OpenMRS, LLC.
 * All Rights Reserved.
 */

public class GitHub {
	
	public final static String RELEASE_PLUGIN_MESSAGE_PREFIX = "[maven-release-plugin] prepare release ";
	
	public final static String OPENMRS_GIT_HUB_REST_URL = "https://api.github.com/repos/openmrs/";
	
	private String url;
	
	private String repositoryName;
	
	private String since;
	
	private boolean isSinceDate = false;
	
	public GitHub(String repositoryName, String branch, String sinceString) {
		this.repositoryName = repositoryName;
		this.url = OPENMRS_GIT_HUB_REST_URL + repositoryName + "/commits?page={page}&per_page=100&sha="
		        + ((branch != null) ? branch : "master");
		this.since = sinceString;
		if (StringUtils.isNotBlank(since)) {
			if (since.startsWith("COMMIT@")) {
				//it is a commit message
				since = since.substring(since.indexOf("COMMIT@") + 7);
			} else {
				since = RELEASE_PLUGIN_MESSAGE_PREFIX + since;
				/* TODO support date and time
				try {
					new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(since);
					isSinceDate = true;
				}
				catch (Exception e) {
					since = RELEASE_PLUGIN_MESSAGE_PREFIX + since;
				}*/
			}
			System.out.println(" Getting contributors since: " + since);
		} else {
			System.out.println(" Getting all contributors");
		}
	}
	
	public List<Commit> getCommits() throws Exception {
		Integer page = 1;
		String pageUrl = StringUtils.replace(url, "{page}", page.toString());
		if (since != null && isSinceDate) {
			pageUrl += "&since=" + since;
		}
		List<Commit> commits = new ArrayList<Commit>();
		ObjectMapper mapper = new ObjectMapper();
		while (true) {
			System.out.println("\tFetching from: " + pageUrl);
			//JsonNode response = mapper.readValue(new URL(pageUrl), JsonNode.class);
			String projectName = repositoryName.substring(repositoryName.lastIndexOf("-") + 1);
			String filename = projectName + ((page == 1) ? ".json" : "_" + page + ".json");
			File preloadedJsonFile = new File("preloaded", filename);
			if (!preloadedJsonFile.exists()) {
				System.out.println("\t Preloaded file not found: " + filename);
				break;//There are mo more preloaded commit files for this project    
			} else {
				System.out.println("\t Preloaded file: " + filename);
			}
			JsonNode response = mapper.readValue(preloadedJsonFile, JsonNode.class);
			List<JsonNode> commitNodes = response.findValues("commit", new ArrayList<JsonNode>());
			if (commitNodes.size() == 0) {
				//We've reached the first commit in the branch or the remaining
				//commits come after the since date if specified
				break;
			}
			boolean foundMessage = false;
			for (JsonNode commitNode : commitNodes) {
				Commit commit = mapper.convertValue(commitNode, Commit.class);
				if (isSinceDate || since == null) {
					commits.add(commit);
				} else if (since != null && commit.getMessage() != null) {
					if (!(since.trim().equals(commit.getMessage().trim()))) {
						commits.add(commit);
					} else {
						System.out.println("\tFound Previous Release commit : " + commit.getCommitter().getName() + " : "
						        + commit.getCommitter().getDate() + " : " + commit.getMessage());
						foundMessage = true;
						//Exclude '[maven-release-plugin] prepare for next development iteration' commit
						if (commits.size() > 0) {
							Commit previousCommit = commits.get(commits.size() - 1);
							if (previousCommit.getMessage() != null) {
								if ("[maven-release-plugin] prepare for next development iteration".equals(previousCommit
								        .getMessage().trim())) {
									commits.remove(commits.size() - 1);
									System.out.println("\tRemoving commit : " + previousCommit.getCommitter().getName()
									        + " : " + previousCommit.getCommitter().getDate() + " : "
									        + previousCommit.getMessage() + "\n");
								}
							}
						}
						break;
					}
				}
			}
			
			if (foundMessage) {
				break;
			}
			
			page++;
			pageUrl = StringUtils.replace(url, "{page}", page.toString());
		}
		
		return commits;
	}
}
