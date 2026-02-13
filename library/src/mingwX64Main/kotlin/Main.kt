import com.ammarymn.kmp.sysutil.SystemInfo

fun main() {
    println("Hello from mingwX64!")
    println(SystemInfo.os.family)
    println(SystemInfo.os.version)
    println(SystemInfo.os.buildNumber)
    println(SystemInfo.os.uptime)
    println(SystemInfo.os.isPrivileged)
    println(SystemInfo.os.processId)
}