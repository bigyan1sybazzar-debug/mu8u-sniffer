package com.streamsniffer.app.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.web.*
import com.streamsniffer.app.domain.sniffer.DetectedStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = hiltViewModel(),
    onNavigateToPlayer: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val state = rememberWebViewState(uiState.currentUrl)
    val navigator = rememberWebViewNavigator()
    var urlInput by remember { mutableStateOf(uiState.currentUrl) }
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(viewModel.navigateTo) {
        viewModel.navigateTo.collect { action ->
            if (action.startsWith("play:")) {
                onNavigateToPlayer(action.removePrefix("play:"), "Web Stream")
            } else {
                state.content = WebContent.Url(action)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Address Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navigator.navigateBack() }, enabled = uiState.canGoBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                        
                        TextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            placeholder = { Text("Search or enter URL") },
                            trailingIcon = {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                } else {
                                    IconButton(onClick = { 
                                        viewModel.onUrlEntered(urlInput)
                                        focusManager.clearFocus()
                                    }) {
                                        Icon(Icons.Default.Search, "Go")
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                viewModel.onUrlEntered(urlInput)
                                focusManager.clearFocus()
                            })
                        )

                        IconButton(onClick = { viewModel.toggleStreamPanel() }) {
                            BadgedBox(badge = {
                                if (uiState.detectedStreams.isNotEmpty()) {
                                    Badge { Text(uiState.detectedStreams.size.toString()) }
                                }
                            }) {
                                Icon(Icons.Default.FileDownload, "Streams", 
                                    tint = if (uiState.detectedStreams.isNotEmpty()) 
                                        MaterialTheme.colorScheme.primary else LocalContentColor.current)
                            }
                        }
                    }
                }

                LinearProgressIndicator(
                    progress = { state.loadingState.let { if (it is LoadingState.Loading) it.progress else 0f } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (uiState.isLoading) 2.dp else 0.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )

                WebView(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    navigator = navigator,
                    onCreated = { webView ->
                        webView.settings.javaScriptEnabled = true
                        webView.settings.domStorageEnabled = true
                        webView.settings.mediaPlaybackRequiresUserGesture = false
                    },
                    client = object : AccompanistWebViewClient() {
                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            url?.let { 
                                urlInput = it
                                viewModel.onPageStarted(it) 
                            }
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            super.onPageFinished(view, url)
                            url?.let { viewModel.onPageFinished(it, view.title ?: "") }
                            viewModel.onNavigationStateChanged(navigator.canGoBack, navigator.canGoForward)
                            
                            view.evaluateJavascript(
                                "(function() { return document.documentElement.outerHTML; })();"
                            ) { html ->
                                html?.let { viewModel.scanPageSource(it) }
                            }
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = uiState.showStreamPanel,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 16.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Detected Streams",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                TextButton(onClick = { viewModel.clearDetectedStreams() }) {
                                    Text("Clear All")
                                }
                                IconButton(onClick = { viewModel.toggleStreamPanel() }) {
                                    Icon(Icons.Default.Close, "Close")
                                }
                            }
                        }

                        if (uiState.detectedStreams.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No streams detected on this page", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.detectedStreams) { stream ->
                                    StreamDetectionCard(
                                        stream = stream,
                                        onPlay = { viewModel.playStream(it.url) },
                                        onSave = { viewModel.saveStream(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreamDetectionCard(
    stream: DetectedStream,
    onPlay: (DetectedStream) -> Unit,
    onSave: (DetectedStream) -> Unit
) {
    var isSaved by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stream.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text(stream.quality) },
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stream.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = { 
                onSave(stream)
                isSaved = true
            }) {
                Icon(
                    if (isSaved) Icons.Default.CheckCircle else Icons.Default.AddCircleOutline,
                    "Save",
                    tint = if (isSaved) Color.Green else LocalContentColor.current
                )
            }

            FilledIconButton(
                onClick = { onPlay(stream) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, "Play")
            }
        }
    }
}
