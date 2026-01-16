package com.example.gateway.jobs;

import org.redisson.api.RedissonClient;
import org.redisson.api.RQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Service
public class JobServiceImpl implements JobService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String JOB_STATUS_PREFIX = "job:status:";
    private static final String JOB_COUNTER_PREFIX = "job:counter:";
    private static final String WORKER_HEARTBEAT_KEY = "worker:heartbeat";
    private static final long WORKER_HEARTBEAT_TIMEOUT = 30; // seconds

    @Override
    public String enqueueJob(String queueName, Serializable jobData, String jobId) {
        try {
            RQueue<Object> queue = redissonClient.getQueue(queueName);
            queue.offer(jobData);
            
            // Store job status
            redisTemplate.opsForValue().set(
                    JOB_STATUS_PREFIX + jobId,
                    JobConstants.JOB_STATUS_PENDING,
                    24,
                    TimeUnit.HOURS
            );
            
            // Increment pending counter
            incrementCounter(JOB_COUNTER_PREFIX + JobConstants.JOB_STATUS_PENDING);
            
            return jobId;
        } catch (Exception e) {
            throw new RuntimeException("Failed to enqueue job: " + jobId, e);
        }
    }

    @Override
    public String getJobStatus(String jobId) {
        Object status = redisTemplate.opsForValue().get(JOB_STATUS_PREFIX + jobId);
        return status != null ? status.toString() : JobConstants.JOB_STATUS_PENDING;
    }

    @Override
    public long getPendingJobsCount() {
        return getCounter(JOB_COUNTER_PREFIX + JobConstants.JOB_STATUS_PENDING);
    }

    @Override
    public long getProcessingJobsCount() {
        return getCounter(JOB_COUNTER_PREFIX + JobConstants.JOB_STATUS_PROCESSING);
    }

    @Override
    public long getCompletedJobsCount() {
        return getCounter(JOB_COUNTER_PREFIX + JobConstants.JOB_STATUS_COMPLETED);
    }

    @Override
    public long getFailedJobsCount() {
        return getCounter(JOB_COUNTER_PREFIX + JobConstants.JOB_STATUS_FAILED);
    }

    @Override
    public boolean isWorkerRunning() {
        Object heartbeat = redisTemplate.opsForValue().get(WORKER_HEARTBEAT_KEY);
        return heartbeat != null;
    }

    public void updateJobStatus(String jobId, String newStatus) {
        String currentStatus = getJobStatus(jobId);
        
        // Update status
        redisTemplate.opsForValue().set(
                JOB_STATUS_PREFIX + jobId,
                newStatus,
                24,
                TimeUnit.HOURS
        );
        
        // Update counters
        if (!currentStatus.equals(newStatus)) {
            decrementCounter(JOB_COUNTER_PREFIX + currentStatus);
            incrementCounter(JOB_COUNTER_PREFIX + newStatus);
        }
    }

    public void setWorkerHeartbeat() {
        redisTemplate.opsForValue().set(
                WORKER_HEARTBEAT_KEY,
                System.currentTimeMillis(),
                WORKER_HEARTBEAT_TIMEOUT,
                TimeUnit.SECONDS
        );
    }

    private void incrementCounter(String counterKey) {
        redisTemplate.opsForValue().increment(counterKey);
    }

    private void decrementCounter(String counterKey) {
        Long count = (Long) redisTemplate.opsForValue().get(counterKey);
        if (count != null && count > 0) {
            redisTemplate.opsForValue().decrement(counterKey);
        }
    }

    private long getCounter(String counterKey) {
        Object count = redisTemplate.opsForValue().get(counterKey);
        return count != null ? Long.parseLong(count.toString()) : 0;
    }
}
