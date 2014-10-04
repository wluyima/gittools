/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License. Copyright (C) OpenMRS, LLC.
 * All Rights Reserved.
 */

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

public class ReferenceApplicationContributors {
	
	private final static List<String> emailsToSkip = Arrays.asList("infrastructure@openmrs.org", "info@individual-it.net");
	
	private final static String OPENMRS_MODULE_PREFIX = "openmrs-module-";
	
	public static void main(String[] args) throws Exception {
		Properties modulesAndPreviousVersion = getModulesAndPreviousVersionsMap();
		Set<Contributor> allContributors = new HashSet<Contributor>();
		for (Map.Entry<Object, Object> entry : modulesAndPreviousVersion.entrySet()) {
			String moduleId = entry.getKey().toString();
			System.out.println("\n" + moduleId);
			Set<Contributor> contributors = getContributors(OPENMRS_MODULE_PREFIX + moduleId, null, entry.getValue()
			        .toString());
			System.out.println(" > " + moduleId + " contributors: " + contributors.size());
			allContributors.addAll(contributors);
		}
		System.out.println("\ncore");
		Set<Contributor> contributors = getContributors("openmrs-core", "1.10.x",
		    "COMMIT@Setting version to 1.10.0-SNAPSHOT");
		System.out.println(" > core contributors: " + contributors.size());
		allContributors.addAll(contributors);
		
		Set<String> contributorNames = new TreeSet<String>();
		for (Contributor contributor : allContributors) {
			contributorNames.add(contributor.getDisplayName());
		}
		System.out.println("\nTotal Count:" + contributorNames.size());
		System.out.println(StringUtils.join(contributorNames, ", "));
	}
	
	public static Properties getModulesAndPreviousVersionsMap() throws Exception {
		Properties props = new Properties();
		props.load(new FileReader(new File("module_previous_versions.properties")));
		return props;
	}
	
	private static Set<Contributor> getContributors(String repositoryName, String branch, String since) throws Exception {
		
		List<Commit> commits = new GitHub(repositoryName, branch, since).getCommits();
		Set<Contributor> contributors = new HashSet<Contributor>();
		for (Commit commit : commits) {
			Contributor author = commit.getAuthor();
			Contributor committer = commit.getCommitter();
			if (!emailsToSkip.contains(author.getEmail())) {
				contributors.add(author);
			}
			if (!emailsToSkip.contains(committer.getEmail())) {
				contributors.add(committer);
			}
		}
		System.out.println("\tCount:" + contributors.size());
		System.out.println("\t" + StringUtils.join(contributors, ", "));
		return contributors;
	}
}
