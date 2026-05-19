package com.streamsniffer.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.streamsniffer.app.ui.navigation.StreamSnifferNavGraph
import com.streamsniffer.app.ui.theme.StreamSnifferTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var externalStreamUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            StreamSnifferTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StreamSnifferNavGraph(externalStreamUrl = externalStreamUrl)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // Handle m3u8 URLs shared from other apps
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                val url = uri.toString()
                if (url.contains(".m3u8") || url.contains("application/x-mpegurl")) {
                    externalStreamUrl = url
                }
            }
        }
    }
}
