/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.batch;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * {@link CommandLineRunner} to {@link JobLauncher launch} Spring Batch jobs. Runs all
 * jobs in the surrounding context by default. Can also be used to launch a specific job
 * by providing a jobName
 * 
 * @author Dave Syer
 */
@Component
public class JobLauncherCommandLineRunner implements CommandLineRunner,
		ApplicationEventPublisherAware {

	private static Log logger = LogFactory.getLog(JobLauncherCommandLineRunner.class);

	@Autowired(required = false)
	private final JobParametersConverter converter = new DefaultJobParametersConverter();

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired(required = false)
	private JobRegistry jobRegistry;

	@Autowired
	private JobRepository jobRepository;

	private String jobNames;

	@Autowired(required = false)
	private final Collection<Job> jobs = Collections.emptySet();

	private ApplicationEventPublisher publisher;

	public void setJobNames(String jobNames) {
		this.jobNames = jobNames;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void run(String... args) throws JobExecutionException {
		logger.info("Running default command line with: " + Arrays.asList(args));
		launchJobFromProperties(StringUtils.splitArrayElementsIntoProperties(args, "="));
	}

	protected void launchJobFromProperties(Properties properties)
			throws JobExecutionException {
		JobParameters jobParameters = this.converter.getJobParameters(properties);
		executeLocalJobs(jobParameters);
		executeRegisteredJobs(jobParameters);
	}

	private void executeRegisteredJobs(JobParameters jobParameters)
			throws JobExecutionException {
		if (this.jobRegistry != null && StringUtils.hasText(this.jobNames)) {
			String[] jobsToRun = this.jobNames.split(",");
			for (String jobName : jobsToRun) {
				try {
					Job job = this.jobRegistry.getJob(jobName);
					if (this.jobs.contains(job)) {
						continue;
					}
					execute(job, jobParameters);
				}
				catch (NoSuchJobException nsje) {
					logger.debug("No job found in registry for job name: " + jobName);
					continue;
				}
			}
		}
	}

	protected void execute(Job job, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException, JobRestartException,
			JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		String jobName = job.getName();
		JobExecution previousExecution = this.jobRepository.getLastJobExecution(jobName,
				jobParameters);
		if (previousExecution == null || previousExecution.getStatus().isUnsuccessful()) {
			JobExecution execution = this.jobLauncher.run(job, jobParameters);
			if (this.publisher != null) {
				this.publisher.publishEvent(new JobExecutionEvent(execution));
			}
		}
	}

	private void executeLocalJobs(JobParameters jobParameters)
			throws JobExecutionException {
		for (Job job : this.jobs) {
			if (StringUtils.hasText(this.jobNames)) {
				String[] jobsToRun = this.jobNames.split(",");
				if (!PatternMatchUtils.simpleMatch(jobsToRun, job.getName())) {
					logger.debug("Skipped job: " + job.getName());
					continue;
				}
			}
			execute(job, jobParameters);
		}
	}

}
