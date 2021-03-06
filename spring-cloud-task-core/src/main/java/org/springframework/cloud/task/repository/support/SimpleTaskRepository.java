/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.task.repository.support;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.util.Assert;

/**
 * Records the task execution information to the log and to TaskExecutionDao provided.
 * @author Glenn Renfro
 */
public class SimpleTaskRepository implements TaskRepository {

	public static final int MAX_EXIT_MESSAGE_SIZE = 2500;
	public static final int MAX_TASK_NAME_SIZE = 100;

	private final static Log logger = LogFactory.getLog(SimpleTaskRepository.class);

	private TaskExecutionDao taskExecutionDao;

	private FactoryBean<TaskExecutionDao> taskExecutionDaoFactoryBean;

	private boolean initialized = false;

	public SimpleTaskRepository(FactoryBean<TaskExecutionDao> taskExecutionDaoFactoryBean){
		Assert.notNull(taskExecutionDaoFactoryBean, "A FactoryBean that provides a TaskExecutionDao is required");

		this.taskExecutionDaoFactoryBean = taskExecutionDaoFactoryBean;
	}

	@Override
	public TaskExecution completeTaskExecution(long executionId, Integer exitCode, Date endTime,
			String exitMessage) {
		initialize();

		validateExitInformation(executionId, exitCode, endTime);
		exitMessage = trimExitMessage(exitMessage);
		taskExecutionDao.completeTaskExecution(executionId, exitCode, endTime, exitMessage);
		logger.debug("Updating: TaskExecution with executionId="+executionId
				+ " with the following {"
				+ "exitCode=" + exitCode
				+ ", endTime=" + endTime
				+ ", exitMessage='" + exitMessage + '\''
				+ '}');

		return taskExecutionDao.getTaskExecution(executionId);
	}

	@Override
	public TaskExecution createTaskExecution(String taskName,
			Date startTime,List<String> parameters) {
		initialize();
		validateCreateInformation(startTime, taskName);
		TaskExecution taskExecution =
				taskExecutionDao.createTaskExecution(taskName, startTime, parameters);
		logger.debug("Creating: " + taskExecution.toString());
		return taskExecution;
	}

	/**
	 * Retrieves the taskExecutionDao associated with this repository.
	 * @return the taskExecutionDao
	 */
	public TaskExecutionDao getTaskExecutionDao() {
		initialize();
		return taskExecutionDao;
	}

	private void initialize() {
		if(!initialized) {
			try {
				this.taskExecutionDao = this.taskExecutionDaoFactoryBean.getObject();
				this.initialized = true;
			}
			catch (Exception e) {
				throw new IllegalStateException("Unable to create the TaskExecutionDao", e);
			}
		}
	}

	/**
	 * Validate startTime and taskName are valid.
	 */
	private void validateCreateInformation(Date startTime, String taskName) {
		Assert.notNull(startTime, "TaskExecution start time cannot be null.");

		if (taskName != null &&
				taskName.length() > MAX_TASK_NAME_SIZE) {
			throw new IllegalArgumentException("TaskName length exceeds "
					+ MAX_TASK_NAME_SIZE + " characters");
		}
	}

	private void validateExitInformation(long executionId, Integer exitCode,  Date endTime){
		Assert.notNull(exitCode, "exitCode should not be null");
		Assert.isTrue(exitCode >= 0, "exit code must be greater than or equal to zero");
		Assert.notNull(endTime, "TaskExecution endTime cannot be null.");
	}

	private String trimExitMessage(String exitMessage){
		String result = exitMessage;
		if(exitMessage != null &&
				exitMessage.length() > MAX_EXIT_MESSAGE_SIZE) {
			result = exitMessage.substring(0, MAX_EXIT_MESSAGE_SIZE - 1);
		}
		return result;
	}
}
