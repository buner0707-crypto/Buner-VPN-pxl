package com.buner.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.delay

enum class VpnState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

class MainActivity : ComponentActivity() {

    private var vpnState by mutableStateOf(VpnState.DISCONNECTED)
    private var connectedServer by mutableStateOf("")
    private var connectionTime by mutableStateOf(0L)
    private var accessKey by mutableStateOf("")

    private val vpnStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra("state")) {
                "CONNECTING" -> {
                    vpnState = VpnState.CONNECTING
                    connectedServer = intent.getStringExtra("server") ?: ""
                }
                "CONNECTED" -> {
                    vpnState = VpnState.CONNECTED
                    connectedServer = intent.getStringExtra("server") ?: ""
                    connectionTime = 0L
                }
                "DISCONNECTED" -> {
                    vpnState = VpnState.DISCONNECTED
                    connectedServer = ""
                    connectionTime = 0L
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accessKey = AccessKeyManager.getInstance(this).getOrCreateKey()

        LocalBroadcastManager.getInstance(this).registerReceiver(
            vpnStateReceiver,
            IntentFilter("VPN_STATE")
        )

        setContent {
            BunerVPNScreen(
                vpnState = vpnState,
                connectedServer = connectedServer,
                connectionTime = connectionTime,
                accessKey = accessKey,
                onStartClick = { startVpn() },
                onStopClick = { stopVpn() },
                onShareClick = { shareKey() },
                onCopyKeyClick = { copyKey() }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(vpnStateReceiver)
    }

    private fun startVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, RESULT_OK, null)
        }
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        if (result == RESULT_OK) {
            val intent = Intent(this, BunerVpnService::class.java)
            intent.putExtra("command", "start")
            startService(intent)
        }
    }

    private fun stopVpn() {
        val intent = Intent(this, BunerVpnService::class.java)
        intent.putExtra("command", "stop")
        startService(intent)
    }

    private fun shareKey() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Мой ключ доступа к Бунер VPN: $accessKey\nСкачай приложение и введи этот ключ!")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Поделиться ключом"))
    }

    private fun copyKey() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("VPN Key", accessKey)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Ключ скопирован", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun BunerVPNScreen(
    vpnState: VpnState,
    connectedServer: String,
    connectionTime: Long,
    accessKey: String,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyKeyClick: () -> Unit
) {
    var elapsedTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(vpnState) {
        if (vpnState == VpnState.CONNECTED) {
            while (true) {
                delay(1000)
                elapsedTime++
            }
        } else {
            elapsedTime = 0
        }
    }

    val buttonColor by animateColorAsState(
        targetValue = when (vpnState) {
            VpnState.DISCONNECTED -> Color(0xFF2196F3)
            VpnState.CONNECTING -> Color(0xFFFF9800)
            VpnState.CONNECTED -> Color(0xFF4CAF50)
        },
        label = "buttonColor"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (vpnState == VpnState.CONNECTING) 1.05f else 1f,
        label = "buttonScale"
    )

    val statusText = when (vpnState) {
        VpnState.DISCONNECTED -> "Отключён"
        VpnState.CONNECTING -> "Подключаюсь..."
        VpnState.CONNECTED -> "Подключён к $connectedServer"
    }

    val statusColor = when (vpnState) {
        VpnState.DISCONNECTED -> Color.Gray
        VpnState.CONNECTING -> Color(0xFFFF9800)
        VpnState.CONNECTED -> Color(0xFF4CAF50)
    }

    val timeText = if (vpnState == VpnState.CONNECTED) {
        val min = elapsedTime / 60
        val sec = elapsedTime % 60
        String.format("%02d:%02d", min, sec)
    } else ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Status
        Text(
            text = statusText,
            color = statusColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        if (vpnState == VpnState.CONNECTED) {
            Text(
                text = timeText,
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Main button
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(buttonColor)
                .scale(buttonScale)
                .clickable {
                    when (vpnState) {
                        VpnState.DISCONNECTED -> onStartClick()
                        VpnState.CONNECTED -> onStopClick()
                        VpnState.CONNECTING -> {}
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Бунер",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Access key
        if (accessKey.isNotEmpty()) {
            Text(
                text = "Ключ доступа",
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                text = accessKey.take(8),
                color = Color(0xFF4CAF50),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onCopyKeyClick() }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Share button
        Button(
            onClick = onShareClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF333333)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .height(48.dp)
        ) {
            Text("Поделиться", color = Color.White, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
