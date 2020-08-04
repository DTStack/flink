package org.apache.flink.docs.rest;

import org.apache.flink.api.common.JobID;
import org.apache.flink.runtime.concurrent.FutureUtils;
import org.apache.flink.runtime.executiongraph.AccessExecutionGraph;
import org.apache.flink.runtime.rest.handler.legacy.ExecutionGraphCache;
import org.apache.flink.runtime.webmonitor.RestfulGateway;

import java.util.concurrent.CompletableFuture;

/**
 * {@link ExecutionGraphCache} which does nothing.
 */
public enum NoOpExecutionGraphCache implements ExecutionGraphCache {
	INSTANCE;

	@Override
	public int size() {
		return 0;
	}

	@Override
	public CompletableFuture<AccessExecutionGraph> getExecutionGraph(JobID jobId, RestfulGateway restfulGateway) {
		return FutureUtils.completedExceptionally(new UnsupportedOperationException("NoOpExecutionGraphCache does not support to retrieve execution graphs"));
	}

	@Override
	public void cleanup() {}

	@Override
	public void close() {}
}
