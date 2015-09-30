/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License. Copyright (C) OpenMRS, LLC.
 * All Rights Reserved.
 */
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class GitHistoryLoader {
	
	public static void main(String[] args) throws Exception {
		Properties props = new Properties();
		props.putAll(ReferenceApplicationContributors.getModulesAndPreviousVersionsMap());
		//props.put("core", "COMMIT@[maven-release-plugin] prepare for next development iteration");
		ObjectMapper mapper = new ObjectMapper();
		for (Map.Entry<Object, Object> entry : props.entrySet()) {
			String projectName = entry.getKey().toString();
			System.out.println("\n" + projectName);
			String since = entry.getValue().toString();
			if (StringUtils.isNotBlank(since)) {
				if (!since.startsWith("COMMIT@")) {
					since = GitHub.RELEASE_PLUGIN_MESSAGE_PREFIX + since;
				} else {
					since = since.substring(since.indexOf("COMMIT@") + 7);
				}
				System.out.println(" Getting commits since: " + since);
			} else {
				since = null;
				System.out.println(" Getting all commits");
			}
			
			Integer page = 1;
			String url = GitHub.OPENMRS_GIT_HUB_REST_URL + "openmrs-"
			        + (("core".equals(projectName)) ? "core" : "module-" + projectName)
			        + "/commits?page={page}&per_page=100&sha=" + (("core".equals(projectName)) ? "1.10.x" : "master");
			
			String pageUrl = StringUtils.replace(url, "{page}", page.toString());
			while (true) {
				System.out.println("\tFetching from: " + pageUrl);
				JsonNode response = mapper.readValue(new URL(pageUrl), JsonNode.class);
				
				if (response.size() == 0) {
					//We've reached the first commit in the branch or the remaining
					//commits come after the since date if specified
					break;
				}
				
				String fileName = projectName + ((page == 1) ? ".json" : "_" + page + ".json");
				mapper.writerWithDefaultPrettyPrinter().writeValue(new File("cache", fileName), response);
				if (since != null) {
					boolean foundMessage = false;
					List<JsonNode> commitNodes = response.findValues("commit", new ArrayList<JsonNode>());
					for (JsonNode commitNode : commitNodes) {
						Commit commit = mapper.convertValue(commitNode, Commit.class);
						//System.out.println("\t" + commit.getCommitter().getName() + " : " + commit.getCommitter().getDate()
						//        + " : " + commit.getMessage());
						if ((since.trim().equals(commit.getMessage().trim()))) {
							System.out.println("\tFound target commit : " + commit.getCommitter().getName() + " : "
							        + commit.getCommitter().getDate() + " : " + commit.getMessage());
							foundMessage = true;
							break;
						}
					}
					
					if (foundMessage) {
						break;
					}
					
				}
				
				page++;
				pageUrl = StringUtils.replace(url, "{page}", page.toString());
			}
		}
	}
}
