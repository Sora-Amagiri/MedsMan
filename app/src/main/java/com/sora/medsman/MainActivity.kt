package com.sora.medsman

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sora.medsman.ui.MedsManApp
import com.sora.medsman.ui.theme.MedsManTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }

        setContent {
            MedsManTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val app = application as MedsManApplication
                    MedsManApp(repository = app.repository)
                }
            }
        }
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 100
    }
}
