package com.tphat.alpha

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rikka.shizuku.Shizuku
import java.util.Locale

private val Bg = Color(0xFF09090D)
private val Panel = Color(0xFF191920)
private val Red = Color(0xFFFF3344)
private val Muted = Color(0xFFB8B8C2)
private const val SHIZUKU_REQUEST_CODE = 5201

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Red,
                    background = Bg,
                    surface = Panel,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                AlphaApp()
            }
        }
    }
}

private data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdk: Int,
    val totalRam: Long,
    val availableRam: Long,
    val cpuCores: Int,
    val refreshRate: Float,
    val batteryPercent: Int?,
    val thermalStatus: String
)

@Suppress("DEPRECATION")
private fun readDeviceInfo(context: Context): DeviceInfo {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
            as android.app.ActivityManager
    val memory = android.app.ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memory)

    val refresh = try {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE)
                as WindowManager
        windowManager.defaultDisplay.refreshRate
    } catch (_: Exception) {
        0f
    }

    val batteryIntent = context.registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )
    val battery = batteryIntent?.let {
        val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) level * 100 / scale else null
    }

    val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE)
                    as PowerManager
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Bình thường"
                PowerManager.THERMAL_STATUS_LIGHT -> "Ấm nhẹ"
                PowerManager.THERMAL_STATUS_MODERATE -> "Nóng vừa"
                PowerManager.THERMAL_STATUS_SEVERE -> "Nóng cao"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Quá nóng"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Nhiệt khẩn cấp"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Nguy cơ tự tắt"
                else -> "Không xác định"
            }
        } catch (_: Exception) {
            "Không đọc được"
        }
    } else {
        "Android cũ: không hỗ trợ API nhiệt"
    }

    return DeviceInfo(
        manufacturer = Build.MANUFACTURER.orEmpty(),
        model = Build.MODEL.orEmpty(),
        androidVersion = Build.VERSION.RELEASE ?: "Không rõ",
        sdk = Build.VERSION.SDK_INT,
        totalRam = memory.totalMem,
        availableRam = memory.availMem,
        cpuCores = Runtime.getRuntime().availableProcessors(),
        refreshRate = refresh,
        batteryPercent = battery,
        thermalStatus = thermal
    )
}

private fun formatGb(bytes: Long): String =
    String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))

private fun deviceAdvice(device: DeviceInfo): Pair<String, List<String>> {
    val ramGb = device.totalRam / (1024.0 * 1024.0 * 1024.0)
    val profile = when {
        ramGb < 4.0 || device.cpuCores <= 4 -> "CƠ BẢN"
        ramGb < 6.0 -> "PHỔ THÔNG"
        ramGb < 8.0 -> "KHÁ"
        else -> "TÀI NGUYÊN CAO"
    }

    val advice = mutableListOf<String>()
    when (profile) {
        "CƠ BẢN" -> {
            advice += "Ưu tiên đồ họa Mượt hoặc mức thấp trong Free Fire."
            advice += "Đóng ứng dụng nặng không cần thiết trước khi chơi."
            advice += "Đảm bảo còn đủ dung lượng lưu trữ trống."
        }
        "PHỔ THÔNG" -> {
            advice += "Bắt đầu với đồ họa Mượt, kiểm tra độ ổn định FPS."
            advice += "Giảm hiệu ứng trong game nếu FPS tụt thường xuyên."
            advice += "Tránh tải hoặc cập nhật ứng dụng khi đang chơi."
        }
        else -> {
            advice += "Thử đồ họa Mượt trước, tăng từng tùy chọn nếu ổn định."
            advice += "RAM cao không đồng nghĩa GPU mạnh."
            advice += "Đánh giá FPS thực tế trong giao tranh, không chỉ ở sảnh."
        }
    }

    advice += if (device.refreshRate >= 90f) {
        "Màn hình báo ${device.refreshRate.toInt()} Hz; game vẫn chỉ chạy ở FPS mà thiết bị và game hỗ trợ."
    } else {
        "Kiểm tra cài đặt màn hình để chọn tần số quét được máy hỗ trợ."
    }

    if (device.thermalStatus.contains("Nóng") ||
        device.thermalStatus.contains("Quá") ||
        device.thermalStatus.contains("Nguy cơ")) {
        advice += "Thiết bị báo nhiệt cao. Dừng chơi và để máy nguội."
    } else {
        advice += "Tránh chơi khi máy quá nóng để giảm nguy cơ tụt hiệu năng."
    }

    advice += "Đề xuất này dựa trên RAM và số lõi CPU, chưa phải benchmark."
    return profile to advice
}

private fun shizukuStatus(): String = try {
    if (!Shizuku.pingBinder()) {
        "Shizuku chưa chạy"
    } else if (
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    ) {
        "Đã cấp quyền"
    } else {
        "Chưa cấp quyền cho TPHAT ALPHA"
    }
} catch (e: Exception) {
    "Không kiểm tra được: ${e.message ?: "lỗi không xác định"}"
}

private fun runReadOnlyCommand(action: String): String {
    val command = when (action) {
        "device" ->
            "getprop ro.product.manufacturer; " +
            "getprop ro.product.model; " +
            "getprop ro.build.version.release; " +
            "getprop ro.hardware"
        "display" ->
            "settings get system peak_refresh_rate; " +
            "settings get system min_refresh_rate; " +
            "settings get system user_refresh_rate"
        "battery" -> "dumpsys battery"
        else -> return "Thao tác không được hỗ trợ."
    }

    val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
    return try {
        val stdout = process.inputStream.bufferedReader().use { it.readText() }
        val stderr = process.errorStream.bufferedReader().use { it.readText() }
        val code = process.waitFor()
        buildString {
            append(stdout.ifBlank { "Không có dữ liệu trả về." })
            if (stderr.isNotBlank()) append("\nThông báo:\n$stderr")
            append("\n\nMã kết thúc: $code")
        }
    } finally {
        process.destroy()
    }
}

@Composable
private fun AlphaApp() {
    val context = LocalContext.current
    var refreshKey by remember { mutableStateOf(0) }
    val device = remember(refreshKey) { readDeviceInfo(context) }
    val (profile, advice) = remember(device) { deviceAdvice(device) }

    var permission by remember { mutableStateOf(shizukuStatus()) }
    var output by remember {
        mutableStateOf("Chưa chạy kiểm tra. Hãy khởi động Shizuku trước.")
    }
    var busy by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }

    fun requestPermission() {
        try {
            if (!Shizuku.pingBinder()) {
                permission = "Shizuku chưa chạy. Hãy mở Shizuku trước."
            } else if (
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            ) {
                permission = "Đã cấp quyền"
            } else {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                permission = "Hãy xác nhận hộp thoại quyền Shizuku."
            }
        } catch (e: Exception) {
            permission = "Lỗi: ${e.message ?: "không xác định"}"
        }
    }

    fun runCheck(action: String) {
        if (busy) return
        permission = shizukuStatus()
        if (permission != "Đã cấp quyền") {
            output = "Chưa có quyền Shizuku.\n$permission"
            return
        }
        busy = true
        output = "Đang kiểm tra..."
        Thread {
            val result = try {
                runReadOnlyCommand(action)
            } catch (e: Exception) {
                "Lệnh thất bại: ${e.message ?: "lỗi không xác định"}"
            }
            Handler(Looper.getMainLooper()).post {
                output = result
                busy = false
            }
        }.start()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Bg)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("TPHAT ALPHA", color = Color.White, fontSize = 28.sp,
                fontWeight = FontWeight.Black)
            Text("ADB EDITION • MULTI-DEVICE", color = Red,
                fontSize = 11.sp, fontWeight = FontWeight.Bold)

            when (tab) {
                0 -> {
                    DeviceCard(device, profile)
                    Button(
                        onClick = { refreshKey++ },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("QUÉT LẠI THIẾT BỊ") }

                    Text("ĐỀ XUẤT CHO FREE FIRE", color = Red,
                        fontWeight = FontWeight.Bold)
                    advice.forEachIndexed { index, item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Panel),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(Modifier.padding(14.dp)) {
                                Text("%02d".format(index + 1), color = Red,
                                    fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(12.dp))
                                Text(item, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
                1 -> {
                    Text("SHIZUKU / ADB", color = Red, fontWeight = FontWeight.Bold)
                    Text("Trạng thái: $permission", color = Color.White)
                    Button(onClick = { requestPermission() },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("KẾT NỐI / CẤP QUYỀN")
                    }
                    OutlinedButton(onClick = { permission = shizukuStatus() },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("KIỂM TRA QUYỀN")
                    }
                    ToolButton("Đọc thông tin thiết bị", busy) { runCheck("device") }
                    ToolButton("Kiểm tra thiết lập màn hình", busy) { runCheck("display") }
                    ToolButton("Kiểm tra thông tin pin", busy) { runCheck("battery") }
                    Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("KẾT QUẢ", color = Red, fontWeight = FontWeight.Bold)
                            Text(output, color = Color.White, fontSize = 12.sp)
                        }
                    }
                    OutlinedButton(onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                        } catch (_: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("MỞ CÀI ĐẶT MÀN HÌNH")
                    }
                }
                else -> {
                    DeviceCard(device, profile)
                    Text(
                        "Bản 2.0 • Không root • Không sửa dữ liệu game.\n" +
                        "Các lệnh Shizuku trong bản này chỉ đọc thông tin. " +
                        "Không bảo đảm tăng FPS.",
                        color = Muted
                    )
                }
            }
        }

        NavigationBar(containerColor = Panel) {
            listOf("Trang chủ", "ADB", "Thông tin").forEachIndexed { index, title ->
                NavigationBarItem(
                    selected = tab == index,
                    onClick = { tab = index },
                    icon = {},
                    label = { Text(title, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = Red,
                        unselectedTextColor = Muted,
                        indicatorColor = Color(0xFF35151A)
                    )
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceInfo, profile: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("THIẾT BỊ ĐÃ NHẬN DIỆN", color = Red,
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("${device.manufacturer} ${device.model}", color = Color.White,
                fontSize = 20.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = Color.DarkGray)
            InfoRow("Android", "${device.androidVersion} (API ${device.sdk})")
            InfoRow("RAM tổng", formatGb(device.totalRam))
            InfoRow("RAM khả dụng", formatGb(device.availableRam))
            InfoRow("Lõi CPU báo cáo", device.cpuCores.toString())
            InfoRow("Tần số quét", if (device.refreshRate > 0)
                "${device.refreshRate.toInt()} Hz" else "Không xác định")
            InfoRow("Pin", device.batteryPercent?.let { "$it%" } ?: "Không rõ")
            InfoRow("Nhiệt", device.thermalStatus)
            InfoRow("Hồ sơ tham khảo", profile)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(value, color = Color.White, fontSize = 12.sp,
            modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ToolButton(title: String, busy: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Red),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold)
    }
}
