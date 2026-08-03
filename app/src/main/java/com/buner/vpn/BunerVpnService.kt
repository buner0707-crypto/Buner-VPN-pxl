
package com.buner.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket

class BunerVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var running = false

    private val servers = listOf(
        ServerInfo("Франция", "45.32.45.123", 443),
        ServerInfo("Узбекистан", "185.213.155.45", 443),
        ServerInfo("Эстония", "185.165.29.145", 443),
        ServerInfo("Малайзия", "103.27.238.67", 443)
    )

    data class ServerInfo(
        val name: String,
        val address: String,
        val port: Int
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra("command")) {
            "start" -> startVPN()
            "stop" -> stopVPN()
        }
        return START_STICKY
    }

    private fun startVPN() {
        val server = servers.random()
        sendState("CONNECTING", server.name)

        val builder = Builder()
        builder.setSession("Бунер VPN")
        builder.setMtu(1500)
        builder.addAddress("10.8.0.2", 32)
        builder.addRoute("0.0.0.0", 0)
        builder.addDnsServer("1.1.1.1")
        builder.addDnsServer("8.8.8.8")

        vpnInterface = builder.establish()

        if (vpnInterface != null) {
            running = true
            sendState("CONNECTED", server.name)

            val notification = Notification.Builder(this, "VPN_CHANNEL")
                .setContentTitle("Бунер VPN")
                .setContentText("Подключён к ${server.name}")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this, 0,
                        Intent(this, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .build()

            startForeground(1, notification)

            Thread {
                try {
                    val inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
                    val outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)

                    val tunnel = Socket()
                    tunnel.connect(InetSocketAddress(server.address, server.port), 5000)

                    val buffer = ByteArray(32767)
                    while (running) {
                        val read = inputStream.read(buffer)
                        if (read > 0) {
                            tunnel.getOutputStream().write(buffer, 0, read)
                            tunnel.getOutputStream().flush()
                        }

                        val tunnelRead = tunnel.getInputStream().read(buffer)
                        if (tunnelRead > 0) {
                            outputStream.write(buffer, 0, tunnelRead)
                            outputStream.flush()
                        }
                    }

                    tunnel.close()
                } catch (e: Exception) {
                    stopVPN()
                }
            }.start()
        }
    }

    private fun stopVPN() {
        running = false
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        sendState("DISCONNECTED", "")
    }

    override fun onDestroy() {
        stopVPN()
        super.onDestroy()
    }

    private fun sendState(state: String, server: String) {
        val intent = Intent("VPN_STATE").apply {
            putExtra("state", state)
            putExtra("server", server)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "VPN_CHANNEL",
                "Бунер VPN",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
