package me.weishu.kernelsu.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.LocalBlurEnabled
import me.weishu.kernelsu.ui.viewmodel.TerminalViewModel
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.useful.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Destination<RootGraph>
@Composable
fun TerminalScreen(navigator: DestinationsNavigator) {
    val viewModel: TerminalViewModel = viewModel()
    val listState = rememberLazyListState()
    var cmdText by remember { mutableStateOf("") }
    
    val blurEnabled = LocalBlurEnabled.current

    LaunchedEffect(viewModel.output.size) {
        if (viewModel.output.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.output.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(id = R.string.terminal),
                navigationIcon = {
                    IconButton(
                        onClick = { navigator.popBackStack() },
                        icon = MiuixIcons.Back
                    )
                },
                color = if (blurEnabled) Color.Transparent else MiuixTheme.colorScheme.surface
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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

            InputField(
                value = cmdText,
                onValueChange = { cmdText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                placeholder = "Enter command...",
                singleLine = true,
                onKeyboardAction = {
                    if (cmdText.isNotBlank()) {
                        viewModel.executeCommand(cmdText)
                        cmdText = ""
                    }
                }
            )
        }
    }
}
