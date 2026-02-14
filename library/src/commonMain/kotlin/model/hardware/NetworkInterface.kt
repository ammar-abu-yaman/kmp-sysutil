package com.ammarymn.kmp.sysutil.model.hardware

data class NetworkInterface(
    val friendlyName: String, // "eth0"
    val name: String,
    val description: String,   // "Intel(R) Wi-Fi 6 AX200"
    val macAddress: String,    // "00:1A:2B:3C:4D:5E"
    val ipAddresses: List<String>, // ["192.168.1.50", "fe80::..."]
)