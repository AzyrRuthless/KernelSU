package me.weishu.kernelsu.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter

private const val MAX_OUTPUT_LINES = 2000

class TerminalViewModel : ViewModel() {
    val output = mutableStateListOf<String>()
    val history = mutableListOf<String>()
    
    private var process: Process? = null
    private var writer: BufferedWriter? = null

    init {
        startShell()
    }

    private fun appendOutput(line: String) {
        viewModelScope.launch(Dispatchers.Main) {
            if (output.size >= MAX_OUTPUT_LINES) {
                val removeCount = output.size - MAX_OUTPUT_LINES + 1
                if (removeCount > 0) {
                    output.removeRange(0, removeCount)
                }
            }
            output.add(line)
        }
    }

    private fun startShell() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                process = ProcessBuilder("su").start()
                writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
                val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process!!.errorStream))

                launch(Dispatchers.IO) {
                    try {
                        reader.forEachLine { line -> appendOutput(line) }
                    } catch (_: IOException) {
                    }
                }
                
                launch(Dispatchers.IO) {
                    try {
                        errorReader.forEachLine { line -> appendOutput("[ERR] $line") }
                    } catch (_: IOException) {
                    }
                }

                appendOutput("KernelSU Terminal Ready...")
            } catch (e: Exception) {
                appendOutput("Failed to start shell: ${e.message}")
            }
        }
    }

    fun executeCommand(command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (command.isNotBlank()) {
                    if (history.isEmpty() || history.last() != command) {
                        history.add(command)
                    }
                }
                writer?.write(command + "\n")
                writer?.flush()
                appendOutput("# $command")
            } catch (e: Exception) {
                appendOutput("Error: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        process?.destroy()
        super.onCleared()
    }
}
