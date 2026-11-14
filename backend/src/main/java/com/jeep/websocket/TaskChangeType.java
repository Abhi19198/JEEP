package com.jeep.websocket;

/** Kinds of task change, sent verbatim as the {@code type} field of live-update payloads. */
public enum TaskChangeType {
    TASK_CREATED,
    TASK_UPDATED,
    TASK_DELETED
}
