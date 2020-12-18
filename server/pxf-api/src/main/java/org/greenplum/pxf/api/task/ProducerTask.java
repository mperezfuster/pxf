package org.greenplum.pxf.api.task;

import com.google.common.collect.Lists;
import org.greenplum.pxf.api.concurrent.BoundedExecutor;
import org.greenplum.pxf.api.model.DataSplit;
import org.greenplum.pxf.api.model.DataSplitSegmentIterator;
import org.greenplum.pxf.api.model.DataSplitter;
import org.greenplum.pxf.api.model.QuerySession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class ProducerTask implements Runnable {

    private static final int DEFAULT_MAX_THREADS = 10;
    // TODO: decide a property name for the maxThreads
    private static final String PXF_PRODUCER_MAX_THREADS_PROPERTY = "pxf.producer.max-threads";

    protected Logger LOG = LoggerFactory.getLogger(this.getClass());

    private final QuerySession querySession;
    private final BoundedExecutor boundedExecutor;

    public ProducerTask(QuerySession querySession, Executor executor) {
        this.querySession = requireNonNull(querySession, "querySession cannot be null");
        // Defaults to the minimum between DEFAULT_MAX_THREADS and the number of available processors
        int maxThreads = querySession.getContext().getConfiguration()
                .getInt(PXF_PRODUCER_MAX_THREADS_PROPERTY,
                        Math.min(DEFAULT_MAX_THREADS, Runtime.getRuntime().availableProcessors()));
        this.boundedExecutor = new BoundedExecutor(executor, maxThreads);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            int taskIndex = 0; // keeps track of the number of tasks this producer created
            int totalSegments = querySession.getContext().getTotalSegments();
            Integer segmentId;

            // Materialize the list of splits
            DataSplitter splitter = querySession.getProcessor().getDataSplitter(querySession);
            List<DataSplit> splitList = Lists.newArrayList(splitter);
            // get the queue of segments IDs that have registered to this QuerySession
            BlockingDeque<Integer> registeredSegmentQueue = querySession.getRegisteredSegmentQueue();
            BlockingDeque<List<List<Object>>> outputQueue = querySession.getOutputQueue();

            while (querySession.isActive()) {
                segmentId = registeredSegmentQueue.poll(25, TimeUnit.MILLISECONDS);

                if (segmentId == null) {
                    if (querySession.getCompletedTupleReaderTaskCount() == querySession.getCreatedTupleReaderTaskCount()
                            && outputQueue.isEmpty()) {
                        // try to mark the session as inactive. If another
                        // thread is able to register itself before we mark it
                        // as inactive, this operation will be a no-op
                        querySession.tryMarkInactive();
                    }
                } else {
                    Iterator<DataSplit> iterator = new DataSplitSegmentIterator<>(segmentId, totalSegments, splitList.iterator());
                    while (iterator.hasNext() && querySession.isActive()) {
                        DataSplit split = iterator.next();
                        LOG.debug("Submitting {} to the pool for query {}", split, querySession);

                        TupleReaderTask<?> task = new TupleReaderTask<>(taskIndex, split, querySession);

                        // Registers the task and increases the number of jobs submitted to the executor
                        querySession.addTupleReaderTask(taskIndex, task);
                        boundedExecutor.execute(task);
                        taskIndex++;
                    }
                }
            }
        } catch (Exception ex) {
            querySession.errorQuery(ex);
            throw new RuntimeException(ex);
        } finally {

            // Allow segments to deregister to the query session
            while (querySession.getActiveSegmentCount() > 0) {
                // wait or until timeout
                try {
                    Thread.sleep(10L, 0);
                } catch (InterruptedException e) {
                }
            }

            if (querySession.isQueryErrored() || querySession.isQueryCancelled()) {
                // When an error occurs or the query is cancelled, we need
                // to interrupt all the running TupleReaderTasks.
                querySession.cancelAllTasks();
            }

            querySession.close();
        }
    }
}