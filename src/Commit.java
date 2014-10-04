import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License. Copyright (C) OpenMRS, LLC.
 * All Rights Reserved.
 */

@JsonIgnoreProperties({ "tree", "url", "comment_count" })
public class Commit {
	
	@JsonProperty
	private Contributor author;
	
	@JsonProperty
	private Contributor committer;
	
	@JsonProperty
	private String message;
	
	public Contributor getAuthor() {
		return author;
	}
	
	public void setAuthor(Contributor author) {
		this.author = author;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public Contributor getCommitter() {
		return committer;
	}
	
	public void setCommitter(Contributor committer) {
		this.committer = committer;
	}
}
