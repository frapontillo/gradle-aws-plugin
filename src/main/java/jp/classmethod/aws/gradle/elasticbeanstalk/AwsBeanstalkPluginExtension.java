/*
 * Copyright 2013-2015 Classmethod, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.classmethod.aws.gradle.elasticbeanstalk;

import groovy.lang.Closure;

import java.util.List;

import jp.classmethod.aws.gradle.AwsPluginExtension;
import lombok.Getter;
import lombok.Setter;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.EnvironmentDescription;
import com.amazonaws.services.elasticbeanstalk.model.ListAvailableSolutionStacksResult;


public class AwsBeanstalkPluginExtension {
	
	public static final String NAME = "beanstalk";

	@Getter @Setter
	private	Project project;
	
	@Getter @Setter
	private	String profileName;
	
	@Getter @Setter
	private String region;
		
	@Getter(lazy = true)
	private final AWSElasticBeanstalk client = initClient();
	
	private AWSElasticBeanstalk initClient() {
		AwsPluginExtension aws = project.getExtensions().getByType(AwsPluginExtension.class);
		AWSElasticBeanstalkClient client = aws.createClient(AWSElasticBeanstalkClient.class, profileName);
		client.setRegion(aws.getActiveRegion(region));
		return client;
	}

	
	@Getter @Setter
	private	String appName;
	
	@Getter @Setter
	private	String appDesc = "";
	
	@Getter
	private	EbAppVersionExtension version;
	public void version(Closure<?> closure) {
		closure.setResolveStrategy(Closure.DELEGATE_FIRST);
		closure.setDelegate(version);
		closure.call();
	}
	
	@Getter
	private	NamedDomainObjectContainer<EbConfigurationTemplateExtension> configurationTemplates;
	public void configurationTemplates(Closure<?> closure) {
		configurationTemplates.configure(closure);
	}
	
	@Getter
	private	EbEnvironmentExtension environment;
	public void environment(Closure<?> closure) {
		environment.configure(closure);
	}
	
	@Getter @Setter
	private Tier tier = Tier.WebServer;
	
	
	public AwsBeanstalkPluginExtension(Project project) {
		this.project = project;
		this.version = new EbAppVersionExtension();
		this.configurationTemplates = project.container(EbConfigurationTemplateExtension.class);
		this.environment = new EbEnvironmentExtension();
	}
	
	public String getEbEnvironmentCNAME(String environmentName) {
		DescribeEnvironmentsResult der = getClient().describeEnvironments(new DescribeEnvironmentsRequest()
			.withApplicationName(appName)
			.withEnvironmentNames(environmentName));
		EnvironmentDescription env = der.getEnvironments().get(0);
		return env.getCNAME();
	}
	
	public List<EnvironmentDescription> getEnvironmentDescs(List<String> environmentNames) {
		DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest().withApplicationName(appName);
		if (environmentNames.isEmpty() == false) {
			req.setEnvironmentNames(environmentNames);
		}
		DescribeEnvironmentsResult der = getClient().describeEnvironments(req);
		return der.getEnvironments();
	}
	
	public String getElbName(EnvironmentDescription env) {
		String elbName = env.getEndpointURL();
		elbName = elbName.substring(0, elbName.indexOf("."));
		elbName = elbName.substring(0, elbName.lastIndexOf("-"));
		return elbName;
	}
	
	public String latestSolutionStackName(String os, String platform) {
		ListAvailableSolutionStacksResult lassr = getClient().listAvailableSolutionStacks();
		return lassr.getSolutionStacks().stream()
				.filter(n -> n.startsWith(os) && n.contains(" running " + platform))
				.findFirst()
				.get();
	}
}
