package se.attini.logs;

import java.util.Objects;

public class ExportLogsRequest {

    private final long startTime;
    private final long endTime;

    private ExportLogsRequest(Builder builder) {
        this.startTime = Objects.requireNonNull(builder.startTime, "startTime");
        this.endTime = Objects.requireNonNull(builder.endTime, "endTime");
    }

    public static Builder builder() {
        return new Builder();
    }


    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public static class Builder {
        private Long startTime;
        private Long endTime;

        private Builder() {
        }
        public Builder setStartTime(long startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder setEndTime(long endTime) {
            this.endTime = endTime;
            return this;
        }

        public ExportLogsRequest build() {
            return new ExportLogsRequest(this);
        }
    }
}
