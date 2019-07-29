package edu.nu.forensic.etw;

public enum EtwEventType {
    ThreadCSwitch,
    FileIoRead,
    FileIoDelete,
    FileIoCleanup,
    FileIoCreate,
    RegistryOpen,
    RegistryQuery,
    RegistryQueryValue,
    RegistryCreate,
    RegistrySetInformation,
    RegistryClose,
    ThreadStart,
    ThreadDCStart,
    ThreadEnd,
    ThreadDCEnd,
    ImageDCStart,
    ImageDCEnd,
    FileIoWrite,
    RegistrySetValue,
    FileIoSetInfo,
    FileIoFileCreate,
    FileIoName,
    FileIoRename,
    FileIoClose,
    ImageLoad,
    RegistryKCBCreate,
    RegistryKCBDelete,
    TcpIpRecvIPV4,
    TcpIpDisconnectIPV4,
    TcpIpRetransmitIPV4,
    TcpIpReconnectIPV4,
    TcpIpTCPCopyIPV4,
    TcpIpSendIPV4,
    ProcessStart,
    ProcessEnd,
    ProcessDCStart,
    ProcessDCEnd,
    UdpIpSendIPV4,
    UdpIpRecvIPV4,
    TcpIpConnectIPV4,
    TcpIpAcceptIPV4,
    FileIoQueryInfo,
    VisibleWindowInfo,
    MosueDownPositionInfo,
    KeyBoardInfo,
    Callstack,
    PowerShellCheck,
    FileMacroCheck,
    DeviceRemoveAble,
    IpConfig,
    HealthCheck,
    Unknown,
    FileIoRenamePath,
    InitSignal
}
