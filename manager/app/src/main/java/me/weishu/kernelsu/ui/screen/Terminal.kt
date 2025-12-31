package me.weishu.kernelsu.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.LocalBlurEnabled
import me.weishu.kernelsu.ui.util.blurRadius
import me.weishu.kernelsu.ui.viewmodel.TerminalViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max
import kotlin.math.min

@Destination<RootGraph>
@Composable
fun TerminalScreen(navigator: DestinationsNavigator) {
    val viewModel: TerminalViewModel = viewModel()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    var inputState by remember { mutableStateOf(TextFieldValue("")) }
    var historyIndex by remember { mutableIntStateOf(-1) }

    val blurEnabled = LocalBlurEnabled.current
    
    val scrollBehavior = MiuixScrollBehavior()
    val hazeState = remember { HazeState() }
    val hazeStyle = HazeStyle(
        backgroundColor = MiuixTheme.colorScheme.surface,
        tint = HazeTint(MiuixTheme.colorScheme.surface.copy(0.8f))
    )

    LaunchedEffect(viewModel.output.size) {
        if (viewModel.output.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.output.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = if (blurEnabled) {
                    Modifier.hazeEffect(hazeState) {
                        style = hazeStyle
                        blurRadius = blurRadius(blurEnabled)
                        noiseFactor = 0f
                    }
                } else {
                    Modifier
                },
                color = if (blurEnabled) Color.Transparent else MiuixTheme.colorScheme.surface,
                title = stringResource(id = R.string.terminal),
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.padding(start = 16.dp),
                        onClick = dropUnlessResumed { navigator.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onBackground
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .imePadding()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .hazeSource(state = hazeState)
                    .padding(horizontal = 12.dp)
            ) {
                items(viewModel.output) { line ->
                    Text(
                        text = line,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (line.startsWith("#")) MiuixTheme.colorScheme.primary 
                                else MiuixTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surface)
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val keys = listOf(
                    "ESC", "TAB", "CTRL", "ALT", 
                    "PgUp", "PgDn", "Home", "End",
                    "▲", "▼", "◀", "▶"
                )
                
                items(keys) { key ->
                    TerminalKeyButton(key) {
                        when (key) {
                            "ESC" -> {
                                inputState = TextFieldValue("")
                                historyIndex = -1
                            }
                            "TAB" -> {
                                val text = inputState.text
                                val selection = inputState.selection
                                val newText = text.replaceRange(selection.start, selection.end, "\t")
                                inputState = TextFieldValue(newText, TextRange(selection.start + 1))
                            }
                            "PgUp" -> {
                                scope.launch {
                                    val firstVisible = listState.firstVisibleItemIndex
                                    listState.animateScrollToItem(max(0, firstVisible - 10))
                                }
                            }
                            "PgDn" -> {
                                scope.launch {
                                    val firstVisible = listState.firstVisibleItemIndex
                                    listState.animateScrollToItem(firstVisible + 10)
                                }
                            }
                            "Home" -> {
                                inputState = inputState.copy(selection = TextRange(0))
                            }
                            "End" -> {
                                inputState = inputState.copy(selection = TextRange(inputState.text.length))
                            }
                            "▲" -> {
                                if (viewModel.history.isNotEmpty()) {
                                    if (historyIndex == -1) {
                                        historyIndex = viewModel.history.lastIndex
                                    } else {
                                        historyIndex = max(0, historyIndex - 1)
                                    }
                                    val cmd = viewModel.history[historyIndex]
                                    inputState = TextFieldValue(cmd, TextRange(cmd.length))
                                }
                            }
                            "▼" -> {
                                if (historyIndex != -1) {
                                    if (historyIndex < viewModel.history.lastIndex) {
                                        historyIndex++
                                        val cmd = viewModel.history[historyIndex]
                                        inputState = TextFieldValue(cmd, TextRange(cmd.length))
                                    } else {
                                        historyIndex = -1
                                        inputState = TextFieldValue("")
                                    }
                                }
                            }
                            "◀" -> {
                                val current = inputState.selection.start
                                if (current > 0) {
                                    inputState = inputState.copy(selection = TextRange(current - 1))
                                }
                            }
                            "▶" -> {
                                val current = inputState.selection.start
                                if (current < inputState.text.length) {
                                    inputState = inputState.copy(selection = TextRange(current + 1))
                                }
                            }
                            "CTRL", "ALT" -> {
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(MiuixTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                BasicTextField(
                    value = inputState,
                    onValueChange = { 
                        inputState = it
                        historyIndex = -1 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = MiuixTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (inputState.text.isNotBlank()) {
                                viewModel.executeCommand(inputState.text)
                                inputState = TextFieldValue("")
                                historyIndex = -1
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$ ", color = MiuixTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TerminalKeyButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MiuixTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onSecondaryContainer,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
