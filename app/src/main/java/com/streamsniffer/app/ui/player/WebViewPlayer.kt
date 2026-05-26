package com.streamsniffer.app.ui.player

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewPlayer(
    url: String,
    modifier: Modifier = Modifier
) {
    val isHls = url.contains(".m3u8") || url.contains("application/x-mpegurl")
    
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.allowFileAccess = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                webChromeClient = object : WebChromeClient() {
                    // Handle fullscreen if the iframe supports it
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                    }
                }

                if (isHls) {
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
                            <script src="https://cdn.jsdelivr.net/npm/hls.js@latest"></script>
                            <style>
                                body, html { margin: 0; padding: 0; width: 100%; height: 100%; background: black; overflow: hidden; }
                                #video { width: 100%; height: 100%; object-fit: contain; }
                                .loader {
                                    position: absolute; top: 0; left: 0; right: 0; bottom: 0;
                                    display: flex; flex-direction: column; align-items: center; justify-content: center;
                                    background: rgba(0,0,0,0.8); z-index: 10; color: white; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                                }
                                .spinner {
                                    width: 48px; height: 48px; border: 4px solid rgba(255,255,255,0.1);
                                    border-top: 4px solid #cc0000; border-radius: 50%; animate: spin 1s linear infinite;
                                    margin-bottom: 20px;
                                }
                                @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
                                .error-msg {
                                    position: absolute; inset: 0; background: #111; color: white;
                                    display: flex; flex-direction: column; align-items: center; justify-content: center;
                                    text-align: center; padding: 30px; z-index: 20; font-family: sans-serif;
                                }
                                .retry-btn {
                                    background: #cc0000; color: white; border: none; padding: 12px 24px;
                                    border-radius: 12px; font-weight: 800; text-transform: uppercase; letter-spacing: 1px;
                                    margin-top: 24px; cursor: pointer; box-shadow: 0 4px 14px rgba(204, 0, 0, 0.4);
                                }
                            </style>
                        </head>
                        <body>
                            <div id="loader" class="loader">
                                <div class="spinner"></div>
                                <div style="font-size: 10px; font-weight: 900; letter-spacing: 3px; text-transform: uppercase; color: #eee;">Initializing Player</div>
                            </div>
                            <div id="error" class="error-msg" style="display: none;">
                                <div style="font-size: 48px; margin-bottom: 10px;">⚠️</div>
                                <div style="font-weight: 900; font-size: 18px; text-transform: uppercase;">Streaming Error</div>
                                <div id="err-text" style="font-size: 12px; color: #888; margin-top: 8px; max-width: 250px;"></div>
                                <button class="retry-btn" onclick="location.reload()">Retry Connection</button>
                            </div>
                            <video id="video" controls playsinline></video>
                            <script>
                                var video = document.getElementById('video');
                                var loader = document.getElementById('loader');
                                var errorDiv = document.getElementById('error');
                                var errText = document.getElementById('err-text');
                                var streamUrl = '$url';

                                function handleError(msg) {
                                    loader.style.display = 'none';
                                    errorDiv.style.display = 'flex';
                                    errText.innerText = msg;
                                }

                                if (Hls.isSupported()) {
                                    var hls = new Hls({
                                        enableWorker: true,
                                        lowLatencyMode: true,
                                        backBufferLength: 90
                                    });
                                    hls.loadSource(streamUrl);
                                    hls.attachMedia(video);
                                    hls.on(Hls.Events.MANIFEST_PARSED, function() {
                                        loader.style.display = 'none';
                                        video.play().catch(function(e) { console.log('Autoplay blocked'); });
                                    });
                                    hls.on(Hls.Events.ERROR, function(event, data) {
                                        if (data.fatal) {
                                            switch (data.type) {
                                                case Hls.ErrorTypes.NETWORK_ERROR:
                                                    handleError('Network error: The stream is unreachable or restricted.');
                                                    break;
                                                case Hls.ErrorTypes.MEDIA_ERROR:
                                                    hls.recoverMediaError();
                                                    break;
                                                default:
                                                    handleError('A critical player error occurred.');
                                                    hls.destroy();
                                                    break;
                                            }
                                        }
                                    });
                                } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                                    video.src = streamUrl;
                                    video.addEventListener('loadedmetadata', function() {
                                        loader.style.display = 'none';
                                        video.play();
                                    });
                                    video.addEventListener('error', function() {
                                        handleError('Format not supported natively on this device.');
                                    });
                                } else {
                                    handleError('No HLS support detected.');
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    loadDataWithBaseURL("https://bigyann.com.np", html, "text/html", "UTF-8", null)
                } else {
                    // Regular iFrame or URL
                    loadUrl(url)
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { webView ->
            // Update URL if it changes
            // webView.loadUrl(url) // Be careful with reloading on every recompose
        }
    )
}
