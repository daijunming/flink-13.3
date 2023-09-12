package org.apache.flink.streaming.connectors.redis.common.config;

/**
 * sink options.
 * @author donghuan
 */
public class RedisSinkOptions {
    private final int maxRetryTimes;

    private final RedisValueDataStructure redisValueDataStructure;

    public RedisSinkOptions(int maxRetryTimes, RedisValueDataStructure redisValueDataStructure) {
        this.maxRetryTimes = maxRetryTimes;
        this.redisValueDataStructure = redisValueDataStructure;
    }

    public int getMaxRetryTimes() {
        return maxRetryTimes;
    }

    public RedisValueDataStructure getRedisValueDataStructure() {
        return redisValueDataStructure;
    }

    /** RedisSinkOptions.Builder. */
    public static class Builder {
        private int maxRetryTimes;

        private RedisValueDataStructure redisValueDataStructure;

        public Builder setRedisValueDataStructure(RedisValueDataStructure redisValueDataStructure) {
            this.redisValueDataStructure = redisValueDataStructure;
            return this;
        }

        public Builder setMaxRetryTimes(int maxRetryTimes) {
            this.maxRetryTimes = maxRetryTimes;
            return this;
        }

        public RedisSinkOptions build() {
            return new RedisSinkOptions(maxRetryTimes, redisValueDataStructure);
        }
    }
}
