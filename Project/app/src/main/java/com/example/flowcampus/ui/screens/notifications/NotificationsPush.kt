package com.example.flowcampus.ui.screens.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

// Têm que ser iguais aos do script tools/notify.py e ao AndroidManifest.xml
const val PUSH_CHANNEL_ID = "flowcampus_alerts"
const val PUSH_TOPIC = "alerts"

// A partir do Android 8, toda notificação precisa de um "canal"
fun createPushChannel(context: Context) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        val channel = NotificationChannel(
            PUSH_CHANNEL_ID,
            "Alertas de lotação",
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.description = "Avisos sobre a lotação dos ambientes do campus"

        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(channel)
    }
}

// Chamar uma vez na tela principal (depois do login):
// cria o canal, pede permissão (Android 13+) e inscreve o aparelho no tópico
@Composable
fun SetupPushNotifications() {

    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    LaunchedEffect(Unit) {

        createPushChannel(context)

        FirebaseMessaging.getInstance().subscribeToTopic(PUSH_TOPIC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}