package me.weishu.kernelsu.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class TerminalViewModel : ViewModel() {
    val output = mutableStateListOf<String>()
    private var process: Process? = null
    private var writer: BufferedWriter? = null

    init {
        startShell()
    }

    private fun startShell() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                process = ProcessBuilder("su").start()
                writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
                val reader = BufferedReader(InputStreamReader(process!!.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process!!.errorStream))

                launch(Dispatchers.IO) {
                    reader.forEachLine { line -> output.add(line) }
                }
                
                launch(Dispatchers.IO) {
                    errorReader.forEachLine { line -> output.add("[ERR] $line") }
                }

                output.add("KernelSU Terminal Ready...")
            } catch (e: Exception) {
                output.add("Failed to start shell: ${e.message}")
            }
        }
    }

    fun executeCommand(command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                writer?.write(command + "\n")
                writer?.flush()
                output.add("# $command")
            } catch (e: Exception) {
                output.add("Error: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        process?.destroy()
        super.onCleared()
    }
}
