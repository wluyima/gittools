/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License. Copyright (C) OpenMRS, LLC.
 * All Rights Reserved.
 */

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ReferenceApplicationContributors {
	
	final static List<String> emailsToSkip = Arrays.asList("infrastructure@openmrs.org", "info@individual-it.net");
	
	public static void main(String[] args) throws Exception {
		List<String> moduleIds = getReferenceApplicationModules();
		Date date = new SimpleDateFormat("dd/MM/yyyy").parse("07/10/2013");
		Map<String, String> refAppContributors = new HashMap<String, String>();
		for (String moduleId : moduleIds) {
			try {
				refAppContributors.putAll(getContributors(Git.open(new File("../openmrs-module-" + moduleId)), date, null,
				    null));
			}
			catch (RepositoryNotFoundException e) {
				System.out.println("Skipped because module repo can't be found for:" + moduleId);
			}
		}
		
		System.out.println("Count:" + refAppContributors.size());
		Set<String> names = new TreeSet<String>();
		names.addAll(refAppContributors.values());
		System.out.println(StringUtils.join(names, ", "));
	}
	
	private static List<String> getReferenceApplicationModules() throws Exception {
		List<String> moduleIds = new ArrayList<String>();
		File distroPomFile = new File("../openmrs-distro-referenceapplication/package", "pom.xml");
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(distroPomFile);
		//Get the module ids using the artifactItem tags nested inside the artifactItem tag
		Element artifactItems = (Element) doc.getElementsByTagName("artifactItems").item(0);
		NodeList nodeList = artifactItems.getElementsByTagName("artifactItem");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element dependency = (Element) nodeList.item(i);
			String omodArtifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent();
			if (!"openmrs-webapp".equals(omodArtifactId)) {
				moduleIds.add(omodArtifactId.substring(0, omodArtifactId.indexOf("-omod")));
			}
		}
		return moduleIds;
	}
	
	private static Map<String, String> getContributors(Git git, Date sinceDate, String sinceRevExclusive,
	                                                   String untilRevExclusive) throws Exception {
		
		LogCommand logCommand = git.log();
		if (sinceRevExclusive != null && untilRevExclusive != null) {
			logCommand.addRange(RevObject.fromString(sinceRevExclusive), RevObject.fromString(untilRevExclusive));
		} else if (sinceRevExclusive != null) {
			logCommand.not(RevObject.fromString(sinceRevExclusive));
		}
		Iterator<RevCommit> i = logCommand.call().iterator();
		RevWalk revWalk = new RevWalk(git.getRepository());
		Map<String, String> emailAndName = new HashMap<String, String>();
		//List<String> names = new ArrayList<String>();
		while (i.hasNext()) {
			RevCommit commit = revWalk.parseCommit(i.next());
			PersonIdent pi = commit.getAuthorIdent();
			PersonIdent ci = commit.getCommitterIdent();
			if (ci.getWhen().before(sinceDate)) {
				break;
			}
			//System.out.println("\t[" + commit.getName() + "] " + pi.getName() + " [" + ci.getName() + "] : " + pi.getWhen()
			//        + " : " + commit.getShortMessage());
			if (!emailsToSkip.contains(pi.getEmailAddress())) {
				emailAndName.put(pi.getEmailAddress(), pi.getName());
			}
			if (!emailsToSkip.contains(ci.getEmailAddress())) {
				emailAndName.put(ci.getEmailAddress(), ci.getName());
			}
			//names.add(pi.getName());
			//names.add(ci.getName());
		}
		//System.out.println("\tCount:" + names.size());
		//System.out.println("\t" + StringUtils.join(names, ", "));
		return emailAndName;
	}
	
}
