/*++

Module Name:

    public.h

Abstract:

    This module contains the common declarations shared by driver
    and user applications.

Environment:

    user and kernel

--*/

//
// Define an Interface Guid so that apps can find the device and talk to it.
//

DEFINE_GUID (GUID_DEVINTERFACE_WindowsDriverNE,
    0xc0d467bd,0xecf7,0x4600,0xa9,0x92,0x1f,0x81,0xf1,0x25,0x93,0x52);
// {c0d467bd-ecf7-4600-a992-1f81f1259352}
