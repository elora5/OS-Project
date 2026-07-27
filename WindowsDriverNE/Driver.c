#include <ntifs.h>
#include <wdm.h>
#include <wdf.h>

DRIVER_INITIALIZE DriverEntry;
EVT_WDF_DRIVER_UNLOAD DriverEvtUnload;

BOOLEAN g_DriverStopping = FALSE;

NTSTATUS CreateAndWriteFile() {
    HANDLE hFile = NULL;
    NTSTATUS status;
    IO_STATUS_BLOCK ioStatus = { 0 };
    UNICODE_STRING fileName;
    OBJECT_ATTRIBUTES objAttr;
    CHAR buffer[] = "Mahir wrote using Kernel!\n";

    if (g_DriverStopping) {
        return STATUS_UNSUCCESSFUL;
    }

    RtlInitUnicodeString(&fileName, L"\\??\\C:\\Temp\\kmdf_test.txt");
    InitializeObjectAttributes(&objAttr, &fileName,
        OBJ_CASE_INSENSITIVE | OBJ_KERNEL_HANDLE, NULL, NULL);

    // 1. File Creation (Kernel API)
    status = ZwCreateFile(&hFile, GENERIC_WRITE, &objAttr, &ioStatus, NULL,
        FILE_ATTRIBUTE_NORMAL, 0, FILE_OVERWRITE_IF,
        FILE_SYNCHRONOUS_IO_NONALERT, NULL, 0);

    // Print CREATE results
    DbgPrint("[KERNEL PROOF] CREATE Results:\n");
    DbgPrint("NTSTATUS: 0x%08X\n", status);
    DbgPrint("IO_STATUS_BLOCK:\n");
    DbgPrint("  Status: 0x%08X\n", ioStatus.Status);
    DbgPrint("  Information: %lld bytes\n", ioStatus.Information);

    if (!NT_SUCCESS(status)) {
        KdPrint(("Create failed: 0x%X\n", status));
        return status;
    }

    // 2. File Writing (Kernel API)
    status = ZwWriteFile(hFile, NULL, NULL, NULL, &ioStatus,
        buffer, (ULONG)strlen(buffer), NULL, NULL);

    // Print WRITE results
    DbgPrint("[KERNEL PROOF] WRITE Results:\n");
    DbgPrint("NTSTATUS: 0x%08X\n", status);
    DbgPrint("IO_STATUS_BLOCK:\n");
    DbgPrint("  Status: 0x%08X\n", ioStatus.Status);
    DbgPrint("  Information: %lld bytes written\n", ioStatus.Information);

    if (hFile) {
        ZwClose(hFile);
    }

    return status;
}

NTSTATUS DriverEntry(PDRIVER_OBJECT DriverObject, PUNICODE_STRING RegistryPath) {
    WDF_DRIVER_CONFIG config;
    NTSTATUS status;

    DbgPrint("Hello Kernel! Hasin Mahir\n");

    WDF_DRIVER_CONFIG_INIT(&config, NULL);
    config.EvtDriverUnload = DriverEvtUnload;
    config.DriverInitFlags |= WdfDriverInitNonPnpDriver;

    status = WdfDriverCreate(DriverObject, RegistryPath,
        WDF_NO_OBJECT_ATTRIBUTES, &config, WDF_NO_HANDLE);
    if (!NT_SUCCESS(status)) {
        KdPrint(("WdfDriverCreate failed: 0x%X\n", status));
        return status;
    }

    return CreateAndWriteFile();
}

VOID DriverEvtUnload(WDFDRIVER Driver) {
    UNREFERENCED_PARAMETER(Driver);
    g_DriverStopping = TRUE;
    DbgPrint("Goodbye Kernel!\n");
}