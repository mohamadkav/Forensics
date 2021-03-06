package edu.nu.forensic.JsonFormat;

public enum EventType {
    PROCESS_START,
    PROCESS_END,
    FILE_READ,
    FILE_WRITE,
    LOAD_OWN_EXE,
    INJECTION,
    IMAGE_LOAD,
    START_SCRIPT,
    FILE_RENAME,
    FILE_CREATE,
    NETWORK_SEND,
    NETWORK_RECV,
    NETWORK_CONNECT,
    NETWORK_CLOSE,
    FORK,
    EXECUTE,
    FILE_DELETE,
    FILE_PROPERTY,
    EXIT,
    LOAD_ELF,
    FILE_OPEN,
    FILE_CLOSE,
    FORK_WITH_SHARE,
    FILE_OPEN_WITH_CLOSE_ON_EXEC;
}
