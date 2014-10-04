import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * The contents of this file are subject to the OpenMRS Public License Version 1.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://license.openmrs.org Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License. Copyright (C) OpenMRS, LLC.
 * All Rights Reserved.
 */

public class Contributor {
	
	@JsonProperty
	private String name;
	
	@JsonProperty
	private String email;
	
	@JsonProperty
	private String date;
	
	public String getDate() {
		return date;
	}
	
	public void setDate(String date) {
		this.date = date;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDisplayName() {
		//TODO Fetch user details from GitHub
		String actualName = getName();
		try {
			Properties props = new Properties();
			props.load(new FileReader(new File("contributor_names.properties")));
			if (props.get(getName()) != null) {
				String mappedName = props.getProperty(getName());
				if (StringUtils.isNotBlank(mappedName)) {
					actualName = mappedName;
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return actualName;
	}
	
	@Override
	public String toString() {
		//return name + "[" + email + "]";
		return name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		Contributor other = (Contributor) obj;
		if (other.getEmail() == null) {
			return this.email == null;
		}
		if (this.email == null) {
			return other.getEmail() == null;
		}
		return this.email.equals(other.getEmail());
	}
	
	@Override
	public int hashCode() {
		if (email == null) {
			return super.hashCode();
		}
		return email.hashCode();
	}
	
}
